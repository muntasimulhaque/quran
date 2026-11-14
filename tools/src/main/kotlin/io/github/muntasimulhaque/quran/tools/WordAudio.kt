package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.time.LocalDateTime
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The word pronunciation pack: one Opus file for each drawn word, cut from
 * the QUL Alafasy ayah audio at the quran-align boundaries.
 *
 * The source audio is QUL resource 118, Mishari Rashid al-Afasy, Ayah by
 * Ayah. The timings are quran-align's `Alafasy_128kbps.json`, the same 2016
 * release that timed the EveryAyah Alafasy recording QUL carries. Both are
 * recorded in `content/manifest.json`, and the app credits QUL and
 * quran-align on its About page. The pack is a derived work; the ayah files
 * are never shipped, only the word clips.
 *
 * The reader's word positions are the QPC positions. The quran-align file is
 * timed against the word list, so its numbers move through [QpcWords] the
 * same way the recitation pack's do. A QPC merge is two source regions that
 * name one drawn word; their times are joined into one clip so the joined
 * word is heard whole. A region the aligner could not split is not guessed
 * at: every word it covers is left without a file, and the app's per-word
 * speaker is absent for it. A word with no region is left out the same way.
 *
 * The pack is addressed by surah: one ZIP per surah, plus the Al-Fatiha
 * files bundled in the app. The source audio and the JSON are not committed;
 * the manifest, the per-file hashes, and the report are.
 */
class WordAudio(private val root: File) {

    private val content = File(root, "content")
    private val database = File(content, "quran.db")
    private val alignmentFile = File(content, "raw/quran-align/Alafasy_128kbps.json")
    private val audioZip = File(content, "raw/qul/recitation-alafasy.zip")
    private val audioDir = File(content, "work/word-audio/source")
    private val work = File(content, "work/word-audio")
    private val buildDir = File(work, "build")
    private val packsDir = File(work, "packs")
    private val manifestFile = File(content, "word-audio/manifest.json")
    private val reportFile = File(content, "word-audio/report.json")
    private val bundled = File(content, "bundled/word-audio")

    data class Segment(val from: Int, val to: Int, val startMs: Long, val endMs: Long)

    data class Clip(val position: Int, val startMs: Long, val endMs: Long)

    data class AyahPlan(
        val number: Int,
        val surah: Int,
        val ayah: Int,
        val words: Int,
        val clips: List<Clip>,
        val skipped: Set<Int>,
    )

    data class Stats(
        val ayahs: Int,
        val words: Int,
        val files: Int,
        val skipped: Int,
        val merged: Int,
        val joined: Int,
        val missing: Int,
        val extra: Int,
        val zero: Int,
        val short: Int,
    )

    data class Plan(val ayahs: List<AyahPlan>, val stats: Stats)

    fun run(args: List<String>): Int = try {
        when (args.firstOrNull()) {
            "plan" -> {
                printPlan(plan())
                0
            }
            "build" -> build(parseSurahs(args.getOrNull(1)))
            "publish" -> publish()
            else -> {
                println("usage: tools word-audio <plan|build [surahs]|publish>")
                2
            }
        }
    } catch (error: Exception) {
        println("word-audio: ${error.message}")
        1
    }

    private fun parseSurahs(raw: String?): Set<Int> =
        raw.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..114 }.toSet()

    private fun plan(): Plan {
        if (!database.isFile) error("content/quran.db is missing; run tools build first")
        if (!alignmentFile.isFile) {
            error("${alignmentFile.relativeTo(root)} is missing; fetch the quran-align release first")
        }
        if (!audioZip.isFile) {
            error("${audioZip.relativeTo(root)} is missing; fetch the QUL Alafasy ayah package first")
        }
        val ayahs = mutableListOf<Triple<Int, Int, Int>>()
        val words = HashMap<Int, Int>()
        openSqlite(database).use { connection ->
            connection.each("SELECT number, surah, ayah FROM ayah ORDER BY number") { rs ->
                ayahs += Triple(rs.getInt(1), rs.getInt(2), rs.getInt(3))
            }
            connection.each(
                "SELECT ayah_number, COUNT(*) FROM word WHERE marker = 0 GROUP BY ayah_number",
            ) { rs ->
                words[rs.getInt(1)] = rs.getInt(2)
            }
        }
        if (ayahs.size != 6236) error("the content holds ${ayahs.size} ayahs, expected 6236")
        val alignment = readAlignment()
        val plans = ArrayList<AyahPlan>(ayahs.size)
        var files = 0
        var skippedTotal = 0
        var merged = 0
        var joined = 0
        var missingTotal = 0
        var extra = 0
        var zero = 0
        var short = 0
        var totalWords = 0
        for ((number, surah, ayah) in ayahs) {
            val count = words[number] ?: 0
            if (count <= 0) error("ayah $surah:$ayah has no drawn words")
            totalWords += count
            val segments = alignment[surah to ayah].orEmpty()
            val groups = LinkedHashMap<Int, LongArray>()
            val skipped = sortedSetOf<Int>()
            var ayahMerged = 0
            var ayahJoined = 0
            var ayahMissing = 0
            var ayahExtra = 0
            var ayahZero = 0
            var ayahShort = 0
            for (segment in segments) {
                if (segment.to - segment.from > 1) {
                    // The aligner could not find the boundary inside this
                    // region. Guessing one would put two words in one file and
                    // leave the reader with a clip that is not the word.
                    ayahMerged++
                    for (source in segment.from + 1..segment.to) {
                        val target = QpcWords.position(surah, ayah, source)
                        if (target in 1..count) skipped += target else ayahExtra++
                    }
                    continue
                }
                if (segment.to - segment.from < 1) {
                    ayahZero++
                    continue
                }
                val source = segment.from + 1
                val target = QpcWords.position(surah, ayah, source)
                if (target !in 1..count) {
                    ayahExtra++
                    continue
                }
                if (segment.endMs <= segment.startMs) {
                    ayahZero++
                    skipped += target
                    continue
                }
                val existing = groups[target]
                if (existing == null) {
                    groups[target] = longArrayOf(segment.startMs, segment.endMs)
                } else {
                    // A QPC merge: two source words are one drawn word, so
                    // the file runs from the first sound to the second's end.
                    ayahJoined++
                    if (segment.startMs < existing[0]) existing[0] = segment.startMs
                    if (segment.endMs > existing[1]) existing[1] = segment.endMs
                }
            }
            for (position in 1..count) {
                if (position !in groups && position !in skipped) {
                    ayahMissing++
                    skipped += position
                }
            }
            val clips = ArrayList<Clip>(groups.size)
            for ((position, span) in groups) {
                val duration = span[1] - span[0]
                if (duration < MIN_CLIP_MS) {
                    ayahShort++
                    skipped += position
                } else {
                    clips += Clip(position, span[0], span[1])
                }
            }
            clips.sortBy { it.position }
            files += clips.size
            skippedTotal += skipped.size
            merged += ayahMerged
            joined += ayahJoined
            missingTotal += ayahMissing
            extra += ayahExtra
            zero += ayahZero
            short += ayahShort
            plans += AyahPlan(number, surah, ayah, count, clips, skipped)
        }
        return Plan(
            plans,
            Stats(
                ayahs = plans.size,
                words = totalWords,
                files = files,
                skipped = skippedTotal,
                merged = merged,
                joined = joined,
                missing = missingTotal,
                extra = extra,
                zero = zero,
                short = short,
            ),
        )
    }

    private fun readAlignment(): Map<Pair<Int, Int>, List<Segment>> {
        val root = runCatching { Json.parseToJsonElement(alignmentFile.readText()).jsonArray }
            .getOrElse { error("${alignmentFile.name} is not a JSON array: ${it.message}") }
        val out = HashMap<Pair<Int, Int>, List<Segment>>()
        for (element in root) {
            val row = element.jsonObject
            val surah = row["surah"]?.jsonPrimitive?.longOrNull?.toInt() ?: continue
            val ayah = row["ayah"]?.jsonPrimitive?.longOrNull?.toInt() ?: continue
            val segments = row["segments"]?.jsonArray?.mapNotNull { segment ->
                val parts = runCatching { segment.jsonArray }.getOrNull() ?: return@mapNotNull null
                if (parts.size < 4) return@mapNotNull null
                val from = parts[0].jsonPrimitive.longOrNull?.toInt() ?: return@mapNotNull null
                val to = parts[1].jsonPrimitive.longOrNull?.toInt() ?: return@mapNotNull null
                val start = parts[2].jsonPrimitive.longOrNull ?: return@mapNotNull null
                val end = parts[3].jsonPrimitive.longOrNull ?: return@mapNotNull null
                Segment(from, to, start, end)
            }.orEmpty()
            out[surah to ayah] = segments
        }
        return out
    }

    private fun printPlan(plan: Plan) {
        val stats = plan.stats
        println(
            "word-audio: ${stats.ayahs} ayahs, ${stats.words} words, " +
                "${stats.files} files, ${stats.skipped} skipped",
        )
        println(
            "word-audio: merged ${stats.merged}, joined ${stats.joined}, " +
                "missing ${stats.missing}, extra ${stats.extra}, " +
                "zero ${stats.zero}, short ${stats.short}",
        )
        val missingSurahs = plan.ayahs
            .filter { it.skipped.isNotEmpty() }
            .groupingBy { it.surah }
            .eachCount()
            .toSortedMap()
        if (missingSurahs.isNotEmpty()) {
            println(
                "word-audio: surahs with a skipped word: " +
                    missingSurahs.entries.joinToString(", ") { "${it.key} (${it.value})" },
            )
        }
    }

    private fun build(filter: Set<Int>): Int {
        val plan = plan()
        printPlan(plan)
        if (filter.isEmpty() && plan.ayahs.size != 6236) {
            error("the plan holds ${plan.ayahs.size} ayahs, expected 6236")
        }
        val selected = if (filter.isEmpty()) plan.ayahs else plan.ayahs.filter { it.surah in filter }
        if (selected.isEmpty()) error("the surah filter names no ayah")
        val bySurah = selected.groupBy { it.surah }.toSortedMap()
        buildDir.mkdirs()
        packsDir.mkdirs()
        manifestFile.parentFile?.mkdirs()
        val entries = ArrayList<kotlinx.serialization.json.JsonObject>(bySurah.size)
        val failedWords = ArrayList<String>()
        var totalBytes = 0L
        var builtFiles = 0
        for ((surah, ayahPlans) in bySurah) {
            val surahName = String.format(Locale.ROOT, "%03d", surah)
            val surahDir = File(buildDir, surahName)
            val expected = ayahPlans.flatMap { ayahPlan ->
                ayahPlan.clips.map { clip -> wordName(surah, ayahPlan.ayah, clip.position) }
            }.toSet()
            val existing = surahDir.listFiles { file ->
                file.isFile && file.name.endsWith(".opus") && file.length() > 0
            }?.map { it.name }?.toSet().orEmpty()
            if (existing != expected) {
                surahDir.deleteRecursively()
                surahDir.mkdirs()
                for (ayahPlan in ayahPlans) {
                    for (position in buildAyah(surahDir, ayahPlan)) {
                        failedWords += "$surah:${ayahPlan.ayah}:$position"
                    }
                }
            }
            val files = surahDir.listFiles { file -> file.isFile && file.name.endsWith(".opus") }
                ?.sortedBy { it.name }
                .orEmpty()
            if (files.isEmpty()) error("surah $surah produced no word file")
            builtFiles += files.size
            val asset = "words-$surahName.zip"
            val (bytes, hash) = zip(files, File(packsDir, asset))
            entries += buildJsonObject {
                put("surah", surah)
                put("asset", asset)
                put("bytes", bytes)
                put("sha256", hash)
                put("words", files.size)
            }
            totalBytes += bytes
            if (surah == 1) {
                val bundleDir = File(bundled, "001")
                bundleDir.deleteRecursively()
                bundleDir.mkdirs()
                files.forEach { file -> file.copyTo(File(bundleDir, file.name), overwrite = true) }
            }
            val failedHere = failedWords.count { it.startsWith("$surah:") }
            println(
                "word-audio: $surahName ${files.size} words ${bytes / 1024} KB " +
                    "sha256 ${hash.substring(0, 12)}" +
                    if (failedHere > 0) " ($failedHere word(s) left out)" else "",
            )
        }
        // A filtered build is for iterating on one surah: it writes that
        // surah's pack and the bundled Al-Fatiha, and leaves the manifest and
        // the report to the one full build that describes the whole Book.
        if (filter.isNotEmpty()) {
            println("word-audio: filtered build; the manifest and the report are untouched")
            return 0
        }
        val manifest = buildJsonObject {
            put("version", 1)
            put("baseUrl", "https://github.com/muntasimulhaque/quran/releases/download")
            put("tag", TAG)
            put("totalBytes", totalBytes)
            put("packs", JsonArray(entries))
        }
        manifestFile.writeText(Json { prettyPrint = true }.encodeToString(
            kotlinx.serialization.json.JsonElement.serializer(),
            manifest,
        ))
        val stats = plan.stats
        val report = buildJsonObject {
            put("version", 1)
            put("tag", TAG)
            put("source", "QUL resource 118, Mishari Rashid al-Afasy, Ayah by Ayah")
            put("alignment", "quran-align 2016-11-24, Alafasy_128kbps.json")
            put("ayahs", stats.ayahs)
            put("words", stats.words)
            put("plannedFiles", stats.files)
            put("files", builtFiles)
            put("skipped", stats.skipped)
            put("failed", failedWords.size)
            put("merged", stats.merged)
            put("joined", stats.joined)
            put("missing", stats.missing)
            put("extra", stats.extra)
            put("zero", stats.zero)
            put("short", stats.short)
            put("packs", entries.size)
            put("bytes", totalBytes)
            put(
                "failedWords",
                buildJsonArray { failedWords.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } },
            )
        }
        reportFile.writeText(Json { prettyPrint = true }.encodeToString(
            kotlinx.serialization.json.JsonElement.serializer(),
            report,
        ))
        println(
            "word-audio: ${entries.size} pack(s), $builtFiles files, $totalBytes bytes at " +
                "${manifestFile.relativeTo(root)}" +
                if (failedWords.isEmpty()) "" else ", ${failedWords.size} failed: ${failedWords.joinToString(", ")}",
        )
        return 0
    }

    /**
     * Builds one ayah's word files. A failed ffmpeg pass is retried one word
     * at a time, so a single damaged frame leaves one word without a file
     * instead of taking the ayah's other words with it. The words that still
     * fail are returned and named in the report; the app says such a word has
     * no recording of its own.
     */
    private fun buildAyah(out: File, ayahPlan: AyahPlan): List<Int> {
        if (ayahPlan.clips.isEmpty()) return emptyList()
        val name = String.format(Locale.ROOT, "%03d%03d", ayahPlan.surah, ayahPlan.ayah)
        val input = File(source(), "$name.mp3")
        if (!input.isFile) error("the Alafasy ayah audio is missing: ${input.name}")
        val failed = mutableListOf<Int>()
        for (chunk in ayahPlan.clips.chunked(MAX_OUTPUTS)) {
            if (runFfmpeg(input, out, ayahPlan, chunk)) continue
            for (clip in chunk) {
                if (!runFfmpeg(input, out, ayahPlan, listOf(clip))) {
                    failed += clip.position
                    File(out, wordName(ayahPlan.surah, ayahPlan.ayah, clip.position)).delete()
                }
            }
        }
        return failed
    }

    private fun runFfmpeg(input: File, out: File, ayahPlan: AyahPlan, clips: List<Clip>): Boolean {
        val filters = clips.mapIndexed { index, clip ->
            val start = clip.startMs / 1000.0
            val end = clip.endMs / 1000.0
            val duration = (clip.endMs - clip.startMs) / 1000.0
            val fadeOut = (duration - FADE_SECONDS).coerceAtLeast(0.0)
            "[0:a]atrim=start=${seconds(start)}:end=${seconds(end)}," +
                "asetpts=PTS-STARTPTS,aresample=16000," +
                "aformat=sample_fmts=s16:channel_layouts=mono," +
                "afade=t=in:st=0:d=${seconds(FADE_SECONDS)}," +
                "afade=t=out:st=${seconds(fadeOut)}:d=${seconds(FADE_SECONDS)}[w$index]"
        }.joinToString(";")
        val command = mutableListOf(
            "ffmpeg", "-y", "-v", "error",
            "-fflags", "+discardcorrupt",
            "-err_detect", "ignore_err",
            "-i", input.absolutePath,
            "-filter_complex", filters,
        )
        clips.forEachIndexed { index, clip ->
            command += listOf(
                "-map", "[w$index]",
                "-c:a", "libopus",
                "-b:a", "24k",
                "-application", "voip",
                "-vbr", "on",
                "-ar", "16000",
                "-ac", "1",
            )
            val file = File(out, wordName(ayahPlan.surah, ayahPlan.ayah, clip.position))
            command += file.absolutePath
        }
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (code == 0) return true
        if (output.isNotBlank()) println("word-audio: ${ayahPlan.surah}:${ayahPlan.ayah} ffmpeg ${output.take(200)}")
        return false
    }


    /**
     * The Alafasy ayah files, unpacked once into the work folder.
     *
     * The raw folder holds the published ZIP and nothing else, the way every
     * QUL source is kept: one hashed file is the provenance, and the 6,236
     * files it holds are a build artifact. The first build pays the unpack and
     * every one after it reads the folder.
     */
    private fun source(): File {
        val marker = File(audioDir, ".unpacked")
        if (marker.isFile) return audioDir
        audioDir.deleteRecursively()
        audioDir.mkdirs()
        val root = audioDir.canonicalFile
        java.util.zip.ZipInputStream(audioZip.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.substringAfterLast('/')
                if (entry.isDirectory || !Regex("""^[0-9]{6}\.mp3$""").matches(name)) {
                    zip.closeEntry()
                    continue
                }
                val target = File(root, name).canonicalFile
                if (target.parentFile != root) {
                    zip.closeEntry()
                    throw IllegalStateException("the Alafasy package names an unsafe file: $name")
                }
                target.outputStream().buffered().use { out -> zip.copyTo(out) }
                zip.closeEntry()
            }
        }
        marker.writeText("")
        return audioDir
    }

    private fun zip(files: List<File>, target: File): Pair<Long, String> {
        target.parentFile?.mkdirs()
        target.delete()
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            for (file in files) {
                // A fixed stamp, so the same words make the same bytes on
                // every machine: the manifest addresses the pack by its
                // hash, and a rebuild that changed only the clock would
                // publish a new pack for content that did not move.
                zip.putNextEntry(
                    ZipEntry(file.name).apply { timeLocal = ZIP_TIME },
                )
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return target.length() to sha256(target)
    }

    private fun publish(): Int {
        if (!packsDir.isDirectory) {
            println("word-audio: no built packs; run tools word-audio build first")
            return 2
        }
        val zips = packsDir.listFiles { file -> file.isFile && file.name.endsWith(".zip") }
            ?.sortedBy { it.name }
            .orEmpty()
        if (zips.isEmpty()) {
            println("word-audio: no built packs; run tools word-audio build first")
            return 2
        }
        if (!runGh("release", "view", TAG)) {
            if (!runGh(
                    "release", "create", TAG,
                    "--title", "Word pronunciation",
                    "--notes", "One Opus file for every drawn word, cut from the QUL Alafasy " +
                        "ayah audio at the quran-align boundaries. Downloaded by the app one " +
                        "surah at a time when the reader turns the word aid on.",
                )
            ) {
                println("word-audio: could not create $TAG")
                return 1
            }
        }
        val uploaded = uploadedAssets(TAG)
        val missing = zips.filterNot { it.name in uploaded }
        if (missing.isEmpty()) {
            println("word-audio: $TAG is up to date (${zips.size} packages)")
            return 0
        }
        var done = 0
        for (batch in missing.chunked(20)) {
            val args = mutableListOf("release", "upload", TAG)
            args += batch.map { it.absolutePath }
            if (!runGh(*args.toTypedArray())) {
                println("word-audio: upload failed for ${batch.first().name}")
                return 1
            }
            done += batch.size
            print(".")
        }
        println()
        println("word-audio: $TAG uploaded $done of ${zips.size} packages")
        return 0
    }

    private fun uploadedAssets(tag: String): Set<String> {
        val process = ProcessBuilder(
            "gh", "release", "view", tag, "--json", "assets", "--jq", ".assets[].name",
        ).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return text.lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun runGh(vararg args: String): Boolean {
        val process = ProcessBuilder(listOf("gh") + args).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (code != 0) println(text.take(400))
        return code == 0
    }

    private fun wordName(surah: Int, ayah: Int, position: Int): String =
        String.format(Locale.ROOT, "%03d_%03d_%03d.opus", surah, ayah, position)

    private fun seconds(value: Double): String = String.format(Locale.ROOT, "%.3f", value)

    private companion object {
        const val TAG = "word-audio-alafasy"

        /** A clip shorter than this is a click, not a word; the aligner's shortest real word is longer. */
        const val MIN_CLIP_MS = 20L

        /** The fade at each end of a clip, so the cut is not heard as a click. */
        const val FADE_SECONDS = 0.005

        /** One ffmpeg call per ayah is chunked so the command stays under the platform's limit. */
        const val MAX_OUTPUTS = 40

        /** The stamp every entry in a pack carries, so a rebuild is the same bytes. */
        val ZIP_TIME = LocalDateTime.of(2020, 1, 1, 0, 0)
    }
}
