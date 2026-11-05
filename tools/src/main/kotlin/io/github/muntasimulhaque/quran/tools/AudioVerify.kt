package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipInputStream

/**
 * Proves the recitation packages before anything is published.
 *
 * The database, the manifest, and the files on disk are three descriptions of
 * the same set, so this command reads all three and refuses a disagreement:
 * every ayah path in the database is a safe Opus path, every surah has one
 * package whose size and SHA-256 match the manifest, every ZIP entry is the
 * file that ayah names and comes in the database's order, and every file
 * decodes as Opus, mono, 48 kHz.
 *
 * The duration is checked twice. The build's decoded comparison is recorded
 * in `content/work/audio-quality.json`, and this command probes the container
 * duration of the file and of its pinned MP3 source: a package whose audio
 * does not run for the length the source ran would move every word segment
 * after its cut (owner decision, the Opus migration).
 */
class AudioVerify(private val root: File) {

    private val database = File(root, "content/quran.db")
    private val packs = File(root, "content/work/audio-packs")
    private val sources = File(root, "content/work/audio-source")
    private val manifestFile = File(root, "content/recitation-manifest.json")
    private val qualityFile = File(root, "content/work/audio-quality.json")
    private val sourcePins = File(root, "content/audio-sources.json")
    private val legacyManifest = File(root, "content/work/legacy-mp3/recitation-manifest.json")
    private val checkRoot = File(root, "content/work/audio-check")

    private val failures = java.util.Collections.synchronizedList(mutableListOf<String>())

    private data class Row(val reciter: String, val surah: Int, val path: String)

    private data class ZipRecord(val bytes: Long, val sha256: String, val bitrate: Int)

    private data class Group(
        val reciter: String,
        val folder: String,
        val surah: Int,
        val paths: List<String>,
    )

    private data class GroupResult(
        val reciter: String,
        val surah: Int,
        val files: Int,
        val bytes: Long,
        val maxDurationDeltaMs: Double,
        val sumDurationDeltaMs: Double,
        val totalDurationMs: Double,
        val maxDriftMs: Double,
    )

    fun verify(): Int {
        if (!commandWorks("ffprobe", "-version")) {
            println("audio verify: ffprobe was not found on PATH; install ffmpeg, which provides it")
            return 2
        }
        if (!database.exists()) {
            println("audio verify: content/quran.db is missing; run build first")
            return 2
        }
        val rows = mutableListOf<Row>()
        openSqlite(database).use { connection ->
            connection.each(
                "SELECT ra.recitation, a.surah, ra.audio_path FROM recitation_ayah ra " +
                    "JOIN ayah a ON a.number = ra.ayah_number " +
                    "ORDER BY ra.recitation, a.surah, a.ayah",
            ) { rs ->
                rows += Row(
                    reciter = rs.getString(1) ?: "",
                    surah = rs.getInt(2),
                    path = rs.getString(3) ?: "",
                )
            }
        }
        if (rows.size != 12_472) {
            failures += "the database has ${rows.size} recitation rows, expected 12,472"
        }
        val groups = linkedMapOf<Pair<String, Int>, MutableList<Row>>()
        for (row in rows) {
            if (row.reciter.isBlank() || row.surah !in 1..114) {
                failures += "a recitation row has no reciter or no surah"
                continue
            }
            groups.getOrPut(row.reciter to row.surah) { mutableListOf() } += row
        }
        val reciters = groups.keys.map { it.first }.distinct().sorted()
        if (reciters.size != 2) {
            failures += "the database names ${reciters.size} reciters, expected 2"
        }
        for (reciter in reciters) {
            val surahs = groups.keys.filter { it.first == reciter }.map { it.second }.sorted()
            if (surahs != (1..114).toList()) {
                failures += "$reciter has ${surahs.size} surahs, expected 114"
            }
        }

        val manifest = readManifest()
        val quality = readQualityReport(qualityFile)
        if (quality.size != groups.size) {
            failures += "the quality report covers ${quality.size} surahs, expected ${groups.size}"
        }
        checkSourcePins(rows)
        if (!sources.isDirectory) {
            failures += "the source MP3 cache is missing at ${sources.relativeTo(root)}; run audio packs first"
        }

        val prepared = mutableListOf<Group>()
        for ((key, groupRows) in groups.toSortedMap(compareBy({ it.first }, { it.second }))) {
            val (reciter, surah) = key
            val folder = groupRows.firstOrNull()?.path?.substringBefore('/').orEmpty()
            if (groupRows.any { it.path.substringBefore('/') != folder }) {
                failures += "$reciter surah $surah mixes reciter folders"
            }
            for (row in groupRows) {
                if (SafeFiles.audioFile(packs, row.path) == null) {
                    failures += "$reciter surah $surah has an unsafe or non-Opus path: ${row.path}"
                    continue
                }
                val name = row.path.substringAfterLast('/')
                val namedSurah = name.take(3).toIntOrNull()
                if (namedSurah != surah) {
                    failures += "$reciter surah $surah names the file $name"
                }
            }
            prepared += Group(reciter, folder, surah, groupRows.map { it.path })
        }

        val results = java.util.Collections.synchronizedList(mutableListOf<GroupResult>())
        val pool = Executors.newFixedThreadPool(4)
        try {
            prepared.map { group ->
                pool.submit { results += verifyGroup(group, manifest, quality) }
            }.forEach { it.get() }
        } finally {
            pool.shutdownNow()
        }

        for (group in prepared) {
            if (manifest[group.reciter to group.surah] == null) {
                failures += "${group.reciter} surah ${group.surah} has no manifest entry"
            }
        }
        val expectedKeys = prepared.map { it.reciter to it.surah }.toSet()
        for (key in manifest.keys) {
            if (key !in expectedKeys) {
                failures += "the manifest names ${key.first} surah ${key.second}, which the database does not have"
            }
        }

        val sorted = results.sortedWith(compareBy({ it.reciter }, { it.surah }))
        val maxDuration = sorted.maxOfOrNull { it.maxDurationDeltaMs } ?: 0.0
        val totalFiles = sorted.sumOf { it.files }
        val meanDuration = if (totalFiles == 0) 0.0 else
            sorted.sumOf { it.sumDurationDeltaMs } / totalFiles
        val maxDrift = sorted.maxOfOrNull { it.maxDriftMs } ?: 0.0
        val hours = sorted.sumOf { it.totalDurationMs } / 3_600_000.0
        if (maxDuration > MAX_DURATION_DELTA_MS) {
            failures += "a package differs from its source by %.1f ms".format(maxDuration)
        }
        if (maxDrift > MAX_DRIFT_MS) {
            failures += "a package drifts %.0f ms from its source envelope".format(maxDrift)
        }

        for (reciter in reciters) {
            val files = sorted.filter { it.reciter == reciter }.sumOf { it.files }
            val bytes = sorted.filter { it.reciter == reciter }.sumOf { it.bytes }
            println(
                "audio verify: $reciter ${sorted.count { it.reciter == reciter }} packages, " +
                    "$files files, ${size(bytes)}",
            )
        }
        println("audio verify: ${sorted.size} packages, ${sorted.sumOf { it.files }} files, ${size(sorted.sumOf { it.bytes })}")
        println(
            "audio verify: every file decodes as Ogg Opus, mono, 48000 Hz; " +
                "duration delta max %.1f ms, mean %.1f ms; waveform drift max %.0f ms; %.1f hours".format(
                    maxDuration,
                    meanDuration,
                    maxDrift,
                    hours,
                ),
        )
        val raised = quality.filterValues { it.bitrate == OPUS_RAISED_KBPS }
        if (raised.isEmpty()) {
            println("audio verify: every surah is $OPUS_STANDARD_KBPS kbps Opus")
        } else {
            println(
                "audio verify: ${raised.size} surah(s) raised to $OPUS_RAISED_KBPS kbps: " +
                    raised.values.joinToString(", ") { "${it.reciter} ${it.surah}" },
            )
        }
        val old = oldSizes()
        if (old != null) {
            println(
                "audio verify: old MP3 packages ${size(old.first)}, new ${size(sorted.sumOf { it.bytes })} " +
                    "(%.1f%%)".format(100.0 * sorted.sumOf { it.bytes } / old.first),
            )
        }
        if (failures.isEmpty()) {
            println("audio verify: all gates passed")
            return 0
        }
        println()
        println("AUDIO VERIFY FAILURES (${failures.size})")
        failures.forEach { println("  - $it") }
        return 1
    }

    private fun verifyGroup(
        group: Group,
        manifest: Map<Pair<String, Int>, ZipRecord>,
        quality: Map<Pair<String, Int>, SurahQuality>,
    ): GroupResult {
        var files = 0
        var bytes = 0L
        var maxDelta = 0.0
        var sumDelta = 0.0
        var totalDuration = 0.0
        val key = group.reciter to group.surah
        val zip = File(packs, "${group.reciter}/surah-%03d.zip".format(group.surah))
        val record = manifest[key]
        if (record == null) {
            failures += "${group.reciter} surah ${group.surah} has no manifest entry"
            return GroupResult(group.reciter, group.surah, 0, 0, 0.0, 0.0, 0.0, 0.0)
        }
        if (!zip.isFile) {
            failures += "${group.reciter} surah ${group.surah} package is missing: ${zip.relativeTo(root)}"
            return GroupResult(group.reciter, group.surah, 0, 0, 0.0, 0.0, 0.0, 0.0)
        }
        if (zip.length() != record.bytes) {
            failures += "${group.reciter} surah ${group.surah} size ${zip.length()} does not match ${record.bytes}"
        }
        val hash = sha256(zip)
        if (!hash.equals(record.sha256, ignoreCase = true)) {
            failures += "${group.reciter} surah ${group.surah} sha256 does not match the manifest"
        }
        val measured = quality[key]
        if (measured == null) {
            failures += "${group.reciter} surah ${group.surah} has no quality record"
        } else {
            if (!measured.passed) {
                failures += "${group.reciter} surah ${group.surah} failed its quality gate: ${measured.sentence()}"
            }
            if (measured.ayahs != group.paths.size) {
                failures += "${group.reciter} surah ${group.surah} quality covers ${measured.ayahs} ayahs, expected ${group.paths.size}"
            }
            if (measured.bitrate != record.bitrate) {
                failures += "${group.reciter} surah ${group.surah} bitrate ${record.bitrate} does not match its quality record ${measured.bitrate}"
            }
        }
        val expectedNames = group.paths.map { it.substringAfterLast('/') }
        val sourceByName = group.paths.associateBy { it.substringAfterLast('/') }
        val scratch = File(checkRoot, "verify-${group.reciter}-${group.surah}")
        scratch.deleteRecursively()
        scratch.mkdirs()
        try {
            val entries = mutableListOf<File>()
            ZipInputStream(zip.inputStream().buffered()).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (entry.isDirectory) {
                        failures += "${group.reciter} surah ${group.surah} package holds a directory: ${entry.name}"
                        input.closeEntry()
                        continue
                    }
                    if (!AYAH_NAME.matches(entry.name)) {
                        failures += "${group.reciter} surah ${group.surah} package holds an unsafe name: ${entry.name}"
                        input.closeEntry()
                        continue
                    }
                    val target = SafeFiles.childUnder(scratch, entry.name)
                    if (target == null) {
                        failures += "${group.reciter} surah ${group.surah} package escapes its folder: ${entry.name}"
                        input.closeEntry()
                        continue
                    }
                    target.outputStream().buffered().use { out ->
                        val buffer = ByteArray(1 shl 16)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            out.write(buffer, 0, count)
                        }
                    }
                    entries += target
                    input.closeEntry()
                }
            }
            val names = entries.map { it.name }
            if (names != expectedNames) {
                val missing = expectedNames.filterNot { it in names }
                val extra = names.filterNot { it in expectedNames }
                failures += "${group.reciter} surah ${group.surah} entries do not match the database" +
                    (if (missing.isEmpty()) "" else " (missing ${missing.take(3)})") +
                    (if (extra.isEmpty()) "" else " (extra ${extra.take(3)})")
            }
            for ((index, file) in entries.withIndex()) {
                val name = file.name
                val source = sourceByName[name]?.let { path ->
                    SafeFiles.sourceAudioFile(sources, path.dropLast(5) + ".mp3")
                }
                if (source == null || !source.isFile) {
                    failures += "${group.reciter} surah ${group.surah} has no pinned source for $name"
                    continue
                }
                val head = readOpusHead(file)
                if (head == null) {
                    failures += "${group.reciter} surah ${group.surah} $name is not an Ogg Opus file"
                    continue
                }
                if (head.channels != OPUS_CHANNELS || head.inputRate != OPUS_SAMPLE_RATE) {
                    failures +=
                        "${group.reciter} surah ${group.surah} $name is ${head.channels} channel(s) " +
                            "at ${head.inputRate} Hz"
                    continue
                }
                val opusProbe = runCatching { probe(file) }.getOrElse { error ->
                    failures += "${group.reciter} surah ${group.surah} $name cannot be probed: ${error.message}"
                    continue
                }
                val sourceProbe = runCatching { probe(source) }.getOrElse { error ->
                    failures += "${group.reciter} surah ${group.surah} $name source cannot be probed: ${error.message}"
                    continue
                }
                if (opusProbe["codec_name"] != "opus" ||
                    opusProbe["sample_rate"] != "48000" ||
                    opusProbe["channels"] != "1"
                ) {
                    failures += "${group.reciter} surah ${group.surah} $name is not Opus mono 48 kHz"
                    continue
                }
                val opusMs = (opusProbe["duration"]?.toDoubleOrNull() ?: 0.0) * 1000.0
                val sourceMs = (sourceProbe["duration"]?.toDoubleOrNull() ?: 0.0) * 1000.0
                if (opusMs <= 0.0 || sourceMs <= 0.0) {
                    failures += "${group.reciter} surah ${group.surah} $name has no readable duration"
                    continue
                }
                val delta = kotlin.math.abs(opusMs - sourceMs)
                maxDelta = kotlin.math.max(maxDelta, delta)
                sumDelta += delta
                totalDuration += opusMs
                if (delta > MAX_DURATION_DELTA_MS) {
                    failures +=
                        "${group.reciter} surah ${group.surah} $name lasts %.1f ms against %.1f ms".format(
                            opusMs,
                            sourceMs,
                        )
                }
                files++
                bytes += file.length()
            }
        } catch (error: Exception) {
            failures += "${group.reciter} surah ${group.surah} package could not be read: ${error.message}"
        } finally {
            scratch.deleteRecursively()
        }
        return GroupResult(
            reciter = group.reciter,
            surah = group.surah,
            files = files,
            bytes = bytes,
            maxDurationDeltaMs = maxDelta,
            sumDurationDeltaMs = sumDelta,
            totalDurationMs = totalDuration,
            maxDriftMs = measured?.maxDriftMs ?: 0.0,
        )
    }

    private fun probe(file: File): Map<String, String> {
        val process = ProcessBuilder(
            "ffprobe",
            "-v", "error",
            "-show_entries", "stream=codec_name,sample_rate,channels:format=duration",
            "-of", "default=noprint_wrappers=1",
            file.absolutePath,
        ).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        val finished = process.waitFor(1, TimeUnit.MINUTES)
        if (!finished) process.destroyForcibly()
        if (!finished || process.exitValue() != 0) {
            throw IllegalStateException(text.trim().take(200))
        }
        val out = mutableMapOf<String, String>()
        for (line in text.lines()) {
            val index = line.indexOf('=')
            if (index > 0) out[line.substring(0, index)] = line.substring(index + 1).trim()
        }
        return out
    }

    private fun readManifest(): Map<Pair<String, Int>, ZipRecord> {
        if (!manifestFile.exists()) {
            failures += "content/recitation-manifest.json is missing"
            return emptyMap()
        }
        val json = runCatching {
            Json.parseToJsonElement(manifestFile.readText()).jsonObject
        }.getOrNull()
        if (json == null) {
            failures += "content/recitation-manifest.json is not readable JSON"
            return emptyMap()
        }
        if (json["format"]?.jsonPrimitive?.content != OPUS_FORMAT) {
            failures += "content/recitation-manifest.json has no \"format\": \"$OPUS_FORMAT\" marker"
            return emptyMap()
        }
        val out = mutableMapOf<Pair<String, Int>, ZipRecord>()
        for (recitation in (json["recitations"] as? JsonArray).orEmpty()) {
            val row = recitation.jsonObject
            val id = row["id"]?.jsonPrimitive?.content ?: continue
            val tag = row["tag"]?.jsonPrimitive?.content
            val expectedTag = RECITER_TAGS[id]
            if (expectedTag == null || tag != expectedTag) {
                failures += "the manifest's tag for $id is ${tag ?: "missing"}, expected $expectedTag"
            }
            for (entry in (row["surahs"] as? JsonArray).orEmpty()) {
                val surah = entry.jsonObject
                val number = surah["surah"]?.jsonPrimitive?.content?.toIntOrNull() ?: continue
                val bytes = surah["bytes"]?.jsonPrimitive?.content?.toLongOrNull() ?: continue
                val sha = surah["sha256"]?.jsonPrimitive?.content ?: continue
                val bitrate = surah["bitrate"]?.jsonPrimitive?.content?.toIntOrNull()
                    ?.div(1000) ?: OPUS_STANDARD_KBPS
                out[id to number] = ZipRecord(bytes, sha, bitrate)
            }
        }
        if (out.size != 228) {
            failures += "the manifest holds ${out.size} packages, expected 228"
        }
        return out
    }

    private fun checkSourcePins(rows: List<Row>) {
        if (!sourcePins.exists()) {
            failures += "content/audio-sources.json is missing"
            return
        }
        val json = runCatching {
            Json.parseToJsonElement(sourcePins.readText()).jsonObject
        }.getOrNull()
        if (json == null) {
            failures += "content/audio-sources.json is not readable JSON"
            return
        }
        var missing = 0
        for (row in rows) {
            val source = row.path.dropLast(5) + ".mp3"
            val pin = json[source]?.jsonPrimitive?.content
            if (pin == null || !PIN.matches(pin)) missing++
        }
        if (missing > 0) {
            failures += "$missing recitation row(s) have no source hash pin"
        }
        if (json.size != rows.size) {
            failures += "content/audio-sources.json holds ${json.size} pins, expected ${rows.size}"
        }
    }

    /** The old manifest, when the owner kept it, so the report can name the saving. */
    private fun oldSizes(): Pair<Long, Map<String, Long>>? {
        if (!legacyManifest.exists()) return null
        val json = runCatching {
            Json.parseToJsonElement(legacyManifest.readText()).jsonObject
        }.getOrNull() ?: return null
        val byReciter = mutableMapOf<String, Long>()
        var total = 0L
        for (recitation in (json["recitations"] as? JsonArray).orEmpty()) {
            val row = recitation.jsonObject
            val id = row["id"]?.jsonPrimitive?.content ?: continue
            var bytes = 0L
            for (entry in (row["surahs"] as? JsonArray).orEmpty()) {
                bytes += entry.jsonObject["bytes"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            }
            byReciter[id] = bytes
            total += bytes
        }
        return total to byReciter
    }

    private fun size(bytes: Long): String {
        val gb = bytes / 1_000_000_000.0
        if (gb >= 1.0) return String.format(Locale.ROOT, "%.2f GB", gb)
        return String.format(Locale.ROOT, "%.0f MB", bytes / 1_000_000.0)
    }

    private fun commandWorks(vararg command: String): Boolean = runCatching {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().readText()
        process.waitFor(1, TimeUnit.MINUTES) && process.exitValue() == 0
    }.getOrDefault(false)

    private companion object {
        val AYAH_NAME = Regex("^[0-9]{6}\\.opus$")
        val PIN = Regex("^[0-9a-fA-F]{64}$")
    }
}
