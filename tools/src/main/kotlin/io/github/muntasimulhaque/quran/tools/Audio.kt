package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds the per-surah recitation packages.
 *
 * One ZIP per reciter per surah holds that surah's ayah files exactly as the
 * content database names them. The manifest carries each package's size and
 * SHA-256, and it is what the app ships: a package is only accepted after its
 * hash matches, so a bad download or a tampered file can never play.
 *
 * The packages carry Ogg Opus, mono at 48 kHz, 64 kbps variable bitrate. The
 * source exports are still 44.1 kHz stereo MP3 on the CDN, so each ayah is
 * downloaded, decoded, mixed to mono with equal weights, resampled once to
 * 48 kHz because Opus has no other rate, and encoded with libopus at
 * 64 kbps constrained VBR. Unconstrained VBR averaged 79 kbps on the first
 * full pass and grew the set to 2.47 GB against the 64 kbps target, so the
 * constrained mode is what keeps the stated average while still varying the
 * frames. Nothing else touches the recitation: no normalization, no denoise,
 * and no gain. A surah whose quality check fails is encoded again at
 * 96 kbps; one that fails there stops the build instead of shipping (owner
 * decision, the Opus migration).
 *
 * The source MP3s are pinned by SHA-256 in `content/audio-sources.json` and
 * keyed by their source path, so a CDN or an upstream export cannot quietly
 * replace a recitation between two releases. The output is deterministic for
 * a given source: the Ogg muxer runs bitexact and the ZIP timestamp and entry
 * order are fixed.
 */
class Audio(private val root: File) {

    private val sampleAyahs = listOf(
        "1:1", "1:2", "1:3", "1:4", "1:5", "1:6", "1:7",
        "2:1", "2:2", "2:255",
        "18:1", "36:1", "55:1", "78:1",
        "112:1", "112:2", "112:3", "112:4",
        "114:1", "114:2", "114:3", "114:4", "114:5", "114:6",
    )

    private val githubBase = "https://github.com/muntasimulhaque/quran/releases/download"

    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()

    private val sourceRoot = File(root, "content/work/audio-source")
    private val opusRoot = File(root, "content/work/audio-opus")
    private val checkRoot = File(root, "content/work/audio-check")
    private val output = File(root, "content/work/audio-packs")
    private val manifestFile = File(root, "content/recitation-manifest.json")
    private val qualityFile = File(root, "content/work/audio-quality.json")

    /**
     * The source MP3s the packages are built from, by path and SHA-256.
     *
     * The file is keyed by the source path, which still ends in `.mp3`: that
     * is the file the CDN serves and the one whose bytes are pinned. The
     * package path ends in `.opus` and is not the pin's key (owner decision,
     * the Opus migration).
     */
    private val sourceFile = File(root, "content/audio-sources.json")
    private val sourceHashes = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val sourceChanged = AtomicBoolean(false)

    private data class ZipRecord(val bytes: Long, val sha256: String, val bitrate: Int)

    private data class Row(val reciter: String, val surah: Int, val path: String)

    private data class Source(val packagePath: String, val sourcePath: String, val file: File)

    private data class Built(val bitrate: Int, val quality: SurahQuality)

    /** Every package of one surah was built and measured at one bitrate. */
    private data class Measurement(
        val bitrate: Int,
        val quality: SurahQuality,
        val passed: Boolean,
    )

    private class QualityFailure(
        message: String,
        val quality: SurahQuality,
    ) : IllegalStateException(message)

    init {
        loadSourceHashes()
    }

    // ------------------------------------------------------------------ sample

    /**
     * The debug sample: the first ayahs of a few surahs in the reader's new
     * format, copied into the app's debug assets. It is the one recitation a
     * debug build plays without a download, so it is built by the same
     * encoder and measured by the same gate as a package.
     */
    fun sample(): Int {
        checkEncoder()?.let {
            println("audio: $it")
            return 1
        }
        val database = File(root, "content/quran.db")
        if (!database.exists()) {
            println("audio: content/quran.db is missing; run build first")
            return 1
        }
        val target = File(root, "content/work/audio-dev")
        // A sample left in the old format is not playable and would be copied
        // into the assets beside the new one.
        target.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".mp3") }
            .toList()
            .forEach { it.delete() }
        val paths = mutableListOf<String>()
        openSqlite(database).use { connection ->
            connection.each(
                "SELECT ra.audio_path FROM recitation_ayah ra " +
                    "JOIN ayah a ON a.number = ra.ayah_number " +
                    "WHERE a.verse_key IN (${sampleAyahs.joinToString(",") { "'$it'" }}) " +
                    "ORDER BY ra.recitation, a.number",
            ) { rs ->
                rs.getString(1)?.let { paths += it }
            }
        }
        if (paths.isEmpty()) {
            println("audio: the database has no recitation rows for the sample")
            return 1
        }
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        val built = AtomicInteger()
        parallel(paths.distinct(), threads = 6) { path ->
            try {
                val sourcePath = sourcePathOf(path)
                val source = sourceFileFor(sourcePath)
                downloadSource(sourcePath, source)
                val opus = SafeFiles.audioFile(target, path)
                    ?: throw IllegalStateException("unsafe sample path: $path")
                encode(source, opus, OPUS_STANDARD_KBPS)
                if (!isMonoOpus48(opus)) throw IllegalStateException("not a mono 48 kHz Opus file")
                val scratch = File(checkRoot, "sample-" + path.replace('/', '-'))
                scratch.mkdirs()
                val measured = try {
                    measureAyah(source, opus, scratch)
                } finally {
                    scratch.deleteRecursively()
                }
                if (!measured.passed) throw IllegalStateException("quality: ${measured.sentence()}")
                built.incrementAndGet()
            } catch (error: Exception) {
                failures += "$path (${error.message})"
            }
        }
        writeSourceHashes(force = true)
        val bytes = target.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        println("audio: sample at ${target.relativeTo(root)}")
        println("audio: built ${built.get()} of ${paths.distinct().size}, ${bytes / 1024} KB total")
        if (failures.isEmpty()) return 0
        println("AUDIO FAILURES (${failures.size})")
        failures.take(10).forEach { println("  - $it") }
        return 1
    }

    // --------------------------------------------------------------- measure

    /**
     * Re-measures the staged source and Opus against the current gate and
     * rewrites the quality report, without encoding or packaging again.
     *
     * The report and the code that produced it have to move together. When
     * the measurement changes after a build, this command is what keeps the
     * package set and its measured record in step instead of leaving a
     * report that describes an older gate (owner decision, the Opus
     * migration).
     */
    fun measure(
        surahFilter: Set<Int> = emptySet(),
        reciterFilter: Set<String> = emptySet(),
    ): Int {
        checkEncoder()?.let {
            println("audio: $it")
            return 1
        }
        val database = File(root, "content/quran.db")
        if (!database.exists()) {
            println("audio: content/quran.db is missing; run build first")
            return 1
        }
        val rows = mutableListOf<Row>()
        openSqlite(database).use { connection ->
            connection.each(
                "SELECT ra.recitation, a.surah, ra.audio_path FROM recitation_ayah ra " +
                    "JOIN ayah a ON a.number = ra.ayah_number ORDER BY ra.recitation, a.surah, a.ayah",
            ) { rs ->
                rows += Row(
                    reciter = rs.getString(1) ?: return@each,
                    surah = rs.getInt(2),
                    path = rs.getString(3) ?: return@each,
                )
            }
        }
        val groups = rows.groupBy { it.reciter to it.surah }
            .toSortedMap(compareBy({ it.first }, { it.second }))
        val selected = groups.filterKeys { (reciter, surah) ->
            (reciterFilter.isEmpty() || reciter in reciterFilter) &&
                (surahFilter.isEmpty() || surah in surahFilter)
        }
        if (selected.isEmpty()) {
            println("audio: no recitation rows matched")
            return 1
        }
        val manifest = readManifest(manifestFile)
        if (manifest.isEmpty()) {
            println("audio: the Opus manifest is missing; run audio packs first")
            return 1
        }
        // A full re-measure starts clean, so an interrupted run leaves an
        // obviously partial record rather than a mix of two gates. A filtered
        // re-measure keeps the surahs it did not touch.
        val full = surahFilter.isEmpty() && reciterFilter.isEmpty()
        val quality = if (full) mutableMapOf() else readQualityReport(qualityFile).toMutableMap()
        if (full) writeQuality(quality)
        checkRoot.listFiles { file -> file.name.startsWith("quality-") }
            ?.forEach { it.deleteRecursively() }
        var measured = 0
        val failures = mutableListOf<String>()
        for ((key, group) in selected) {
            val (reciter, surah) = key
            val record = manifest[key]
            if (record == null) {
                failures += "$reciter surah $surah has no manifest entry"
                continue
            }
            val sources = group.map { row ->
                val sourcePath = sourcePathOf(row.path)
                Source(row.path, sourcePath, sourceFileFor(sourcePath))
            }
            val opusDir = File(opusRoot, "$reciter/$surah")
            val missing = sources.firstOrNull { source ->
                !source.file.isFile ||
                    SafeFiles.childUnder(opusDir, source.packagePath.substringAfterLast('/'))
                        ?.isFile != true
            }
            if (missing != null) {
                failures += "$reciter surah $surah is not staged: ${missing.sourcePath}"
                continue
            }
            val result = try {
                measureSurah(reciter, surah, sources, opusDir, record.bitrate, threads = 8)
            } catch (error: Exception) {
                failures += "$reciter surah $surah: ${error.message}"
                continue
            }
            quality[key] = result.quality
            writeQuality(quality)
            measured++
            print(".")
        }
        println()
        writeQuality(quality)
        println("audio: re-measured $measured of ${selected.size} surahs")
        return report(failures)
    }

    // ------------------------------------------------------------------- packs

    /**
     * Builds one ZIP per reciter per surah and rewrites the manifest. A
     * package is recovered from a published release only when the manifest
     * says it carries Opus; the old MP3 manifest is never reused, because
     * recovering those packages would ship the format this build replaces.
     *
     * The manifest is written after every surah, so an interrupted run
     * resumes from the packages it finished. The source MP3s and the encoded
     * Opus stay staged until the whole run has passed its gates.
     */
    fun packs(surahFilter: Set<Int>, reciterFilter: Set<String>): Int {
        checkEncoder()?.let {
            println("audio: $it")
            return 1
        }
        val database = File(root, "content/quran.db")
        if (!database.exists()) {
            println("audio: content/quran.db is missing; run build first")
            return 1
        }
        val rows = mutableListOf<Row>()
        openSqlite(database).use { connection ->
            connection.each(
                "SELECT ra.recitation, a.surah, ra.audio_path FROM recitation_ayah ra " +
                    "JOIN ayah a ON a.number = ra.ayah_number ORDER BY ra.recitation, a.surah, a.ayah",
            ) { rs ->
                val reciter = rs.getString(1) ?: return@each
                val surah = rs.getInt(2)
                if (reciter !in reciterFilter) return@each
                if (surahFilter.isNotEmpty() && surah !in surahFilter) return@each
                rows += Row(reciter, surah, rs.getString(3) ?: return@each)
            }
        }
        if (rows.isEmpty()) {
            println("audio: no recitation rows matched")
            return 1
        }
        val groups = rows.groupBy { it.reciter to it.surah }
            .toSortedMap(compareBy({ it.first }, { it.second }))
        val existing = readManifest(manifestFile)
        val entries = LinkedHashMap<Pair<String, Int>, ZipRecord>()
        for ((key, record) in existing) {
            // A filtered build keeps the packages it did not touch, provided
            // they are still on disk. A missing one is recovered when its
            // surah comes up.
            if (zipFor(key.first, key.second).isFile) entries[key] = record
        }
        val quality = readQualityReport(qualityFile)
        checkRoot.listFiles { file -> file.name.startsWith("quality-") }
            ?.forEach { it.deleteRecursively() }

        var built = 0
        var reused = 0
        val failures = mutableListOf<String>()
        for ((key, group) in groups) {
            val (reciter, surah) = key
            val zip = zipFor(reciter, surah)
            val known = existing[key]
            if (known != null && zip.isFile && zip.length() == known.bytes &&
                sha256(zip).equals(known.sha256, ignoreCase = true)
            ) {
                entries[key] = known
                reused++
                continue
            }
            if (known != null) {
                // A package that has ever been published is recovered from its
                // hash-pinned release, never rebuilt from the CDN with a
                // different encoder setting.
                try {
                    downloadPublished(reciter, surah, zip, known.bytes, known.sha256)
                    entries[key] = known
                    reused++
                    print("r")
                    continue
                } catch (error: Exception) {
                    failures += "$reciter surah $surah: ${error.message}"
                    continue
                }
            }
            val paths = group.map { it.path }
            val result = try {
                buildSurah(reciter, surah, paths)
            } catch (error: Exception) {
                val message = "$reciter surah $surah: ${error.message}"
                failures += message
                if (error is QualityFailure) {
                    quality[key] = error.quality
                    println()
                    println("audio: stopping on the quality failure above")
                    writeManifest(entries)
                    writeQuality(quality)
                    writeSourceHashes(force = true)
                    return report(failures)
                }
                println()
                println("audio: $message")
                continue
            }
            quality[key] = result.quality
            entries[key] = ZipRecord(zip.length(), sha256(zip), result.bitrate)
            built++
            print(".")
            writeManifest(entries)
            writeQuality(quality)
            // Pins are written as the run goes, so an interruption never
            // loses what was already downloaded and measured.
            writeSourceHashes(force = false)
        }
        if (built + reused > 0) println()
        writeManifest(entries)
        writeQuality(quality)
        writeSourceHashes(force = true)
        println(
            "audio: packs built $built, reused $reused, " +
                "manifest at ${manifestFile.relativeTo(root)}",
        )
        return report(failures)
    }

    private fun buildSurah(reciter: String, surah: Int, paths: List<String>): Built {
        if (paths.isEmpty()) throw IllegalStateException("no ayahs")
        val names = paths.map { it.substringAfterLast('/') }
        if (names.distinct().size != names.size) throw IllegalStateException("duplicate ayah files")
        for (name in names) {
            if (!AYAH_NAME.matches(name)) throw IllegalStateException("unsafe ayah file: $name")
        }
        val sources = paths.map { path ->
            val sourcePath = sourcePathOf(path)
            Source(path, sourcePath, sourceFileFor(sourcePath))
        }
        val downloadFailures = java.util.Collections.synchronizedList(mutableListOf<String>())
        parallel(sources.map { it.sourcePath }, threads = 12) { sourcePath ->
            try {
                downloadSource(sourcePath, sourceFileFor(sourcePath))
            } catch (error: Exception) {
                downloadFailures += "$sourcePath (${error.message})"
            }
        }
        if (downloadFailures.isNotEmpty()) {
            throw IllegalStateException("${downloadFailures.size} source file(s) failed: ${downloadFailures.first()}")
        }
        for (source in sources) {
            if (!source.file.isFile || source.file.length() == 0L) {
                throw IllegalStateException("the source is missing: ${source.sourcePath}")
            }
            if (!looksLikeMp3(source.file)) {
                throw IllegalStateException("the source is not an MP3: ${source.sourcePath}")
            }
        }

        val opusDir = File(opusRoot, "$reciter/$surah")
        opusDir.deleteRecursively()
        opusDir.mkdirs()
        var bitrate = OPUS_STANDARD_KBPS
        encodeSurah(sources, opusDir, bitrate)
        var measured = measureSurah(reciter, surah, sources, opusDir, bitrate)
        if (!measured.passed) {
            println()
            println(
                "audio: $reciter surah $surah failed at $bitrate kbps " +
                    "(${measured.quality.sentence()}); raising the surah to $OPUS_RAISED_KBPS kbps",
            )
            bitrate = OPUS_RAISED_KBPS
            opusDir.deleteRecursively()
            opusDir.mkdirs()
            encodeSurah(sources, opusDir, bitrate)
            measured = measureSurah(reciter, surah, sources, opusDir, bitrate)
            if (!measured.passed) {
                val failed = measured.quality
                throw QualityFailure(
                    "still fails at $bitrate kbps (${failed.sentence()})",
                    failed,
                )
            }
        }
        zipSurah(zipFor(reciter, surah), names, opusDir)
        return Built(bitrate, measured.quality)
    }

    private fun encodeSurah(sources: List<Source>, opusDir: File, bitrate: Int) {
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        parallel(sources, threads = 8) { source ->
            try {
                val name = source.packagePath.substringAfterLast('/')
                val target = SafeFiles.childUnder(opusDir, name)
                    ?: throw IllegalStateException("unsafe ayah file: $name")
                encode(source.file, target, bitrate)
                if (!isMonoOpus48(target)) throw IllegalStateException("not a mono 48 kHz Opus file")
            } catch (error: Exception) {
                failures += "${source.sourcePath} (${error.message})"
            }
        }
        if (failures.isNotEmpty()) {
            throw IllegalStateException("${failures.size} ayah file(s) failed: ${failures.first()}")
        }
    }

    private fun measureSurah(
        reciter: String,
        surah: Int,
        sources: List<Source>,
        opusDir: File,
        bitrate: Int,
        threads: Int = 4,
    ): Measurement {
        val scratch = File(checkRoot, "quality-$reciter-$surah")
        scratch.mkdirs()
        val results = arrayOfNulls<AyahQuality>(sources.size)
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        parallel(sources.indices.toList(), threads = threads) { index ->
            val source = sources[index]
            val name = source.packagePath.substringAfterLast('/')
            val opus = SafeFiles.childUnder(opusDir, name)
            try {
                if (opus == null) throw IllegalStateException("unsafe ayah file: $name")
                val directory = File(scratch, "%04d".format(index))
                directory.mkdirs()
                results[index] = measureAyah(source.file, opus, directory)
            } catch (error: Exception) {
                failures += "$name (${error.message})"
            }
        }
        scratch.deleteRecursively()
        if (failures.isNotEmpty()) {
            throw IllegalStateException("${failures.size} ayah file(s) could not be measured: ${failures.first()}")
        }
        val measured = results.filterNotNull()
        if (measured.size != sources.size) {
            throw IllegalStateException("only ${measured.size} of ${sources.size} ayah files were measured")
        }
        val failed = sources.indices.filter { !measured[it].passed }
            .map { "${sources[it].packagePath} (${measured[it].sentence()})" }
        return Measurement(
            bitrate = bitrate,
            quality = SurahQuality(
                reciter = reciter,
                surah = surah,
                bitrate = bitrate,
                ayahs = measured.size,
                totalDurationMs = measured.sumOf { it.sourceMs },
                minSnrDb = measured.minOf { it.snrDb },
                minSegmentalSnrDb = measured.minOf { it.segmentalSnrDb },
                maxLevelDeltaDb = measured.maxOf { absDb(it.levelDeltaDb) },
                maxDurationDeltaMs = measured.maxOf { it.durationDeltaMs },
                maxDriftMs = measured.maxOf { it.driftMs },
                failed = failed,
            ),
            passed = failed.isEmpty(),
        )
    }

    /** Encodes one MP3 source to mono 48 kHz Ogg Opus with libopus. */
    private fun encode(source: File, target: File, bitrate: Int) {
        target.parentFile?.mkdirs()
        val part = File(target.parentFile, target.name + ".part")
        val command = listOf(
            "ffmpeg",
            "-hide_banner",
            "-loglevel", "error",
            "-y",
            "-fflags", "+bitexact",
            "-i", source.absolutePath,
            "-map_metadata", "-1",
            // Opus carries 8, 12, 16, 24, or 48 kHz. The source is 44.1 kHz,
            // so this one high-quality resample is the codec's requirement,
            // not a treatment of the recitation. Equal weights keep a dual
            // mono source at its own level; ffmpeg's default -ac 1 mixes at
            // 0.707 per channel and would add 3 dB and clip (owner report,
            // the Opus migration).
            "-af", "aresample=48000:resampler=soxr:precision=28,pan=mono|c0=0.5*c0+0.5*c1",
            "-c:a", "libopus",
            "-b:a", "${bitrate}k",
            // Constrained VBR: the first pass at -vbr on averaged 79 kbps
            // and grew the set to 2.47 GB against the 64 kbps target.
            // Constrained VBR still varies the frame bits but keeps the
            // average the manifest states (owner decision, the Opus
            // migration).
            "-vbr", "constrained",
            "-application", "audio",
            "-frame_duration", "20",
            "-compression_level", "10",
            "-fflags", "+bitexact",
            "-f", "ogg",
            part.absolutePath,
        )
        val process = ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
        val error = process.errorStream.bufferedReader().readText()
        val finished = process.waitFor(10, TimeUnit.MINUTES)
        if (!finished) process.destroyForcibly()
        if (!finished || process.exitValue() != 0) {
            part.delete()
            throw IllegalStateException(
                "ffmpeg could not encode ${source.name}: ${error.trim().take(400)}",
            )
        }
        if (!isMonoOpus48(part)) {
            part.delete()
            throw IllegalStateException("ffmpeg did not write a mono 48 kHz Opus file for ${source.name}")
        }
        Files.move(
            part.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun zipSurah(zip: File, names: List<String>, opusDir: File) {
        zip.parentFile?.mkdirs()
        val part = File(zip.parentFile, zip.name + ".part")
        part.delete()
        ZipOutputStream(part.outputStream().buffered()).use { out ->
            for (name in names) {
                val file = SafeFiles.childUnder(opusDir, name)
                    ?: throw IllegalStateException("unsafe ayah file: $name")
                if (!file.isFile) throw IllegalStateException("missing encoded ayah: $name")
                val entry = ZipEntry(name)
                // A fixed timestamp and the database order keep a rebuild
                // byte-identical. The Ogg muxer runs bitexact too, so the
                // same source and setting produce the same entry bytes.
                entry.time = 315_532_800_000L // 1980-01-01, the ZIP epoch
                out.putNextEntry(entry)
                file.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
        }
        Files.move(part.toPath(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun zipFor(reciter: String, surah: Int): File =
        File(output, "$reciter/surah-%03d.zip".format(surah))

    private fun sourcePathOf(packagePath: String): String {
        if (!packagePath.endsWith(".opus")) {
            throw IllegalStateException("not an Opus package path: $packagePath")
        }
        return packagePath.dropLast(5) + ".mp3"
    }

    private fun sourceFileFor(sourcePath: String): File =
        SafeFiles.sourceAudioFile(sourceRoot, sourcePath)
            ?: throw IllegalStateException("unsafe source path: $sourcePath")

    // ---------------------------------------------------------------- network

    /**
     * Downloads one source MP3, or reuses the staged one when it still
     * matches its pin. A file with no pin is trusted only after it reads as
     * an MP3: every write here is a temporary file renamed into place, so a
     * file this tool left behind is whole.
     */
    private fun downloadSource(sourcePath: String, file: File) {
        if (SafeFiles.sourceAudioFile(sourceRoot, sourcePath) == null) {
            throw IllegalStateException("unsafe source path: $sourcePath")
        }
        if (file.isFile && file.length() > 0) {
            val actual = sha256(file)
            val pinned = sourceHashes[sourcePath]
            if (pinned != null) {
                if (pinned.equals(actual, ignoreCase = true)) return
                // The staged copy is damaged; the pin is still the authority,
                // and a second download that does not match it will fail.
                file.delete()
            } else if (looksLikeMp3(file)) {
                recordHash(sourcePath, actual)
                return
            } else {
                file.delete()
            }
        }
        val response = client.send(
            getRequest("$CDN/$sourcePath"),
            HttpResponse.BodyHandlers.ofByteArray(),
        )
        if (response.statusCode() != 200 || response.body().isEmpty()) {
            throw IllegalStateException("HTTP ${response.statusCode()}")
        }
        recordSource(sourcePath, response.body())
        file.parentFile?.mkdirs()
        val part = File(file.parentFile, file.name + ".part")
        Files.write(part.toPath(), response.body())
        if (!looksLikeMp3(part)) {
            part.delete()
            throw IllegalStateException("the source is not an MP3")
        }
        Files.move(part.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     * Recovers one published package from the project's own release, checked
     * against the manifest's size and SHA-256. A missing or damaged local
     * package is replaced by this, not by a fresh CDN download.
     */
    private fun downloadPublished(
        reciter: String,
        surah: Int,
        destination: File,
        expectedBytes: Long,
        expectedSha256: String,
    ) {
        val tag = RECITER_TAGS[reciter] ?: throw IllegalStateException("unknown reciter: $reciter")
        val url = "$githubBase/$tag/surah-%03d.zip".format(surah)
        val temporary = File(destination.parentFile, destination.name + ".part")
        destination.parentFile?.mkdirs()
        var connection: java.net.HttpURLConnection? = null
        try {
            connection = ReleaseFiles.open(url)
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { out ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        read += count
                        if (expectedBytes > 0 && read > expectedBytes) {
                            throw IllegalStateException("the published package is larger than the manifest says")
                        }
                        digest.update(buffer, 0, count)
                        out.write(buffer, 0, count)
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if ((expectedBytes > 0 && read != expectedBytes) ||
                !actual.equals(expectedSha256, ignoreCase = true)
            ) {
                throw IllegalStateException("the published package does not match the manifest")
            }
            if (destination.exists() && !destination.delete()) {
                throw IllegalStateException("the old package could not be replaced")
            }
            if (!temporary.renameTo(destination)) {
                throw IllegalStateException("the published package could not be placed")
            }
        } finally {
            connection?.disconnect()
            temporary.delete()
        }
    }

    private fun loadSourceHashes() {
        if (!sourceFile.exists()) return
        val rootJson = runCatching {
            Json.parseToJsonElement(sourceFile.readText()).jsonObject
        }.getOrNull() ?: return
        for ((path, value) in rootJson) {
            val hash = (value as? JsonPrimitive)?.content ?: continue
            if (SafeFiles.sourceAudioFile(sourceRoot, path) != null) {
                sourceHashes[path] = hash
            }
        }
    }

    /**
     * Checks one downloaded source against the recorded hash, or records it
     * on the first build. A different file is a hard failure, never a quiet
     * replacement.
     */
    private fun recordSource(path: String, bytes: ByteArray) {
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        recordHash(path, actual)
    }

    private fun recordHash(path: String, actual: String) {
        val previous = sourceHashes.putIfAbsent(path, actual)
        if (previous != null && !previous.equals(actual, ignoreCase = true)) {
            throw IllegalStateException("the source audio changed: $path")
        }
        if (previous == null) sourceChanged.set(true)
    }

    private fun writeSourceHashes(force: Boolean) {
        if (!force && !sourceChanged.getAndSet(false)) return
        val json = buildJsonObject {
            for ((path, hash) in sourceHashes.toSortedMap()) put(path, hash)
        }
        sourceFile.parentFile?.mkdirs()
        sourceFile.writeText(Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json))
    }

    /** An MP3 starts with an ID3 tag or an MPEG frame sync; anything else is not audio. */
    private fun looksLikeMp3(file: File): Boolean {
        val head = ByteArray(3)
        file.inputStream().use { if (it.read(head) < 3) return false }
        val id3 = head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte()
        val sync = head[0] == 0xFF.toByte() && (head[1].toInt() and 0xE0) == 0xE0
        return id3 || sync
    }

    // --------------------------------------------------------------- manifest

    /**
     * The manifest always describes what this run built, whatever filter
     * built it, and is rewritten after every surah so a long build resumes.
     * Each ZIP is hashed once, when it is built; a reused package keeps the
     * size and hash the manifest already proves.
     */
    private fun writeManifest(entries: Map<Pair<String, Int>, ZipRecord>) {
        val json = buildJsonObject {
            put("format", OPUS_FORMAT)
            put("baseUrl", githubBase)
            put(
                "recitations",
                buildJsonArray {
                    for ((reciter, tag) in RECITER_TAGS) {
                        val surahs = entries.filterKeys { it.first == reciter }
                            .toList()
                            .sortedBy { it.first.second }
                        if (surahs.isEmpty()) continue
                        add(
                            buildJsonObject {
                                put("id", reciter)
                                put("tag", tag)
                                put(
                                    "surahs",
                                    buildJsonArray {
                                        for ((key, record) in surahs) {
                                            add(
                                                buildJsonObject {
                                                    put("surah", key.second)
                                                    put("asset", "surah-%03d.zip".format(key.second))
                                                    put("bytes", record.bytes)
                                                    put("sha256", record.sha256)
                                                    put("bitrate", record.bitrate * 1000)
                                                },
                                            )
                                        }
                                    },
                                )
                            },
                        )
                    }
                },
            )
        }
        writeAtomically(manifestFile, Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json))
    }

    /**
     * Reads the manifest this run may reuse. A manifest without the Opus
     * format marker is a manifest of MP3 packages, and recovering those
     * would defeat the migration, so none of its entries are read. It is
     * not deleted here: the owner keeps the old file until the new one is
     * complete, and the first run of this tool prints why it ignored it.
     */
    private fun readManifest(file: File): Map<Pair<String, Int>, ZipRecord> {
        if (!file.exists()) return emptyMap()
        val json = runCatching { Json.parseToJsonElement(file.readText()).jsonObject }.getOrNull()
            ?: return emptyMap()
        val format = json["format"]?.jsonPrimitive?.contentOrNull
        if (format != OPUS_FORMAT) {
            println(
                "audio: ignoring ${file.relativeTo(root)}: it has no \"format\": \"$OPUS_FORMAT\" marker. " +
                    "Its packages are MP3 and are never reused. Back it up or remove it before the first Opus build.",
            )
            return emptyMap()
        }
        val out = mutableMapOf<Pair<String, Int>, ZipRecord>()
        for (recitation in (json["recitations"] as? JsonArray).orEmpty()) {
            val objectValue = recitation.jsonObject
            val id = objectValue["id"]?.jsonPrimitive?.contentOrNull ?: continue
            for (entry in (objectValue["surahs"] as? JsonArray).orEmpty()) {
                val row = entry.jsonObject
                val surah = (row["surah"] as? JsonPrimitive)?.content?.toIntOrNull() ?: continue
                val bytes = (row["bytes"] as? JsonPrimitive)?.content?.toLongOrNull() ?: continue
                val sha = (row["sha256"] as? JsonPrimitive)?.content ?: continue
                val bitrate = (row["bitrate"] as? JsonPrimitive)?.content?.toIntOrNull()
                    ?.div(1000) ?: OPUS_STANDARD_KBPS
                out[id to surah] = ZipRecord(bytes, sha, bitrate)
            }
        }
        return out
    }

    private fun writeQuality(quality: Map<Pair<String, Int>, SurahQuality>) {
        val json = buildJsonObject {
            put("format", OPUS_FORMAT)
            put("sampleRate", OPUS_SAMPLE_RATE)
            put("channels", OPUS_CHANNELS)
            put("standardKbps", OPUS_STANDARD_KBPS)
            put("raisedKbps", OPUS_RAISED_KBPS)
            put(
                "thresholds",
                buildJsonObject {
                    put("minSnrDb", MIN_SNR_DB)
                    put("minSegmentalSnrDb", MIN_SEGMENTAL_SNR_DB)
                    put("maxLevelDeltaDb", MAX_LEVEL_DELTA_DB)
                    put("maxDurationDeltaMs", MAX_DURATION_DELTA_MS)
                    put("maxDriftMs", MAX_DRIFT_MS)
                },
            )
            put(
                "surahs",
                buildJsonArray {
                    for ((key, value) in quality.toList().sortedWith(compareBy({ it.first.first }, { it.first.second }))) {
                        add(
                            buildJsonObject {
                                put("reciter", key.first)
                                put("surah", key.second)
                                put("bitrate", value.bitrate)
                                put("ayahs", value.ayahs)
                                put("totalDurationMs", value.totalDurationMs)
                                put("minSnrDb", value.minSnrDb)
                                put("minSegmentalSnrDb", value.minSegmentalSnrDb)
                                put("maxLevelDeltaDb", value.maxLevelDeltaDb)
                                put("maxDurationDeltaMs", value.maxDurationDeltaMs)
                                put("maxDriftMs", value.maxDriftMs)
                                put("failed", buildJsonArray { value.failed.forEach { add(JsonPrimitive(it)) } })
                            },
                        )
                    }
                },
            )
        }
        writeAtomically(
            qualityFile,
            Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json),
        )
    }

    private fun writeAtomically(file: File, text: String) {
        file.parentFile?.mkdirs()
        val part = File(file.parentFile, file.name + ".part")
        part.writeText(text)
        Files.move(part.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun report(failures: List<String>): Int {
        if (failures.isEmpty()) return 0
        println("AUDIO FAILURES (${failures.size})")
        failures.forEach { println("  - $it") }
        return 1
    }

    // ---------------------------------------------------------------- publish

    /**
     * Uploads every built package to its GitHub Release.
     *
     * The recitation release tags are fixed, unlike the content packs' hash
     * tags: a re-encode keeps the same asset names, so the old MP3 zip is
     * still standing under `surah-001.zip`. An asset is reused only when its
     * remote size equals the local zip's; every other one is uploaded with
     * `--clobber`, and the old package is replaced rather than left for the
     * new manifest to disagree with (owner decision, the Opus migration).
     */
    fun publish(reciterFilter: Set<String>): Int {
        val failures = mutableListOf<String>()
        for ((reciter, tag) in RECITER_TAGS) {
            if (reciter !in reciterFilter) continue
            val zips = File(output, reciter).listFiles { file -> file.name.endsWith(".zip") }
                ?.sorted() ?: continue
            if (zips.isEmpty()) continue
            if (!releaseExists(tag)) {
                val created = runGh(
                    "release", "create", tag,
                    "--title", "Recitation: $reciter",
                    "--notes", "Per-surah recitation packages for $reciter. Downloaded by the app only when the reader asks to play a surah.",
                )
                if (!created) {
                    failures += "$tag: could not create the release"
                    continue
                }
            }
            val uploaded = uploadedAssets(tag)
            val missing = zips.filter { uploaded[it.name] != it.length() }
            if (missing.isEmpty()) {
                println("audio: $tag is up to date (${zips.size} packages)")
                continue
            }
            var done = 0
            for (batch in missing.chunked(20)) {
                val args = mutableListOf("release", "upload", tag)
                args += batch.map { it.absolutePath }
                args += "--clobber"
                if (!runGh(*args.toTypedArray())) {
                    failures += "$tag: upload failed for ${batch.first().name}"
                    break
                }
                done += batch.size
                print(".")
            }
            println()
            println("audio: $tag uploaded $done of ${zips.size} packages")
        }
        return report(failures)
    }

    private fun releaseExists(tag: String): Boolean =
        runGh("release", "view", tag, "--json", "tagName")

    private fun uploadedAssets(tag: String): Map<String, Long> {
        val process = ProcessBuilder(
            "gh", "release", "view", tag,
            "--json", "assets",
            "--jq", ".assets[] | \"\\(.name) \\(.size)\"",
        )
            .redirectErrorStream(true)
            .start()
        val text = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (code != 0) return emptyMap()
        val out = mutableMapOf<String, Long>()
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val name = trimmed.substringBeforeLast(' ', "")
            val size = trimmed.substringAfterLast(' ', "").toLongOrNull() ?: continue
            if (name.isNotEmpty()) out[name] = size
        }
        return out
    }

    private fun runGh(vararg args: String): Boolean {
        val process = ProcessBuilder(listOf("gh") + args)
            .redirectErrorStream(true)
            .start()
        val text = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (code != 0) println(text.take(400))
        return code == 0
    }

    private fun getRequest(url: String): HttpRequest =
        HttpRequest.newBuilder(URI.create(url)).GET().build()

    /**
     * The encoder is a build-time tool, not a runtime dependency, and the
     * message has to tell the owner what to install rather than fail with a
     * stack trace from a missing process.
     */
    private fun checkEncoder(): String? {
        if (!commandWorks("ffmpeg", "-version")) {
            return "ffmpeg was not found on PATH. Install ffmpeg built with libopus " +
                "(the gyan.dev full build on Windows, or your distribution's ffmpeg)."
        }
        if (!commandWorks("ffprobe", "-version")) {
            return "ffprobe was not found on PATH. It comes with ffmpeg; install the same package."
        }
        val encoders = runCatching { commandOutput("ffmpeg", "-hide_banner", "-encoders") }
            .getOrDefault("")
        if (!encoders.contains("libopus")) {
            return "this ffmpeg has no libopus encoder. Install an ffmpeg built with libopus " +
                "(the gyan.dev full build on Windows, or your distribution's ffmpeg package)."
        }
        return null
    }

    private fun commandWorks(vararg command: String): Boolean = runCatching {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().readText()
        process.waitFor(1, TimeUnit.MINUTES) && process.exitValue() == 0
    }.getOrDefault(false)

    private fun commandOutput(vararg command: String): String {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        process.waitFor(1, TimeUnit.MINUTES)
        return text
    }

    /** `audio sample`, `audio packs [surahs] [reciters]`, `audio measure`, `audio verify`, `audio publish [reciters]`. */
    fun run(args: List<String>): Int = when (args.firstOrNull()) {
        "sample" -> sample()
        "packs" -> packs(
            surahFilter = parseNumbers(args.getOrNull(1)),
            reciterFilter = parseNames(args.getOrNull(2)),
        )
        "verify" -> AudioVerify(root).verify()
        "measure" -> measure(
            surahFilter = parseNumbers(args.getOrNull(1)),
            reciterFilter = parseNames(args.getOrNull(2)),
        )
        "publish" -> publish(parseNames(args.getOrNull(1)))
        else -> {
            println("usage: tools audio <sample|packs|measure|verify|publish>")
            2
        }
    }

    private fun parseNumbers(raw: String?): Set<Int> =
        raw.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

    private fun parseNames(raw: String?): Set<String> =
        raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet().ifEmpty { RECITER_TAGS.keys }

    private companion object {
        const val CDN = "https://audio-cdn.tarteel.ai/quran"
        val AYAH_NAME = Regex("^[0-9]{6}\\.opus$")

        /**
         * Runs [work] over [items] on a fixed pool and waits for all of them.
         * The caller decides whether a failure is collected or thrown; the
         * pool is always shut down so a failed surah cannot hold a thread.
         */
        fun <T> parallel(items: List<T>, threads: Int, work: (T) -> Unit) {
            if (items.isEmpty()) return
            val pool = Executors.newFixedThreadPool(threads)
            try {
                val futures = items.map { item -> pool.submit { work(item) } }
                futures.forEach { it.get() }
            } finally {
                pool.shutdownNow()
            }
        }

        fun absDb(value: Double): Double = if (value < 0) -value else value
    }
}

/**
 * The release tags the Opus packages live under.
 *
 * The old MP3 packages keep their original tags during the transition, so a
 * reader still on 15.5 can download a surah as before. The new app's manifest
 * points here; once the new app has been adopted, the old tags can be deleted
 * and their 4 GB reclaimed (owner decision, the Opus migration).
 */
internal val RECITER_TAGS: Map<String, String> = linkedMapOf(
    "minshawi" to "recitation-minshawi-opus",
    "husary" to "recitation-husary-opus",
)

/**
 * A surah's quality result, as written to `content/work/audio-quality.json`.
 * The thresholds live beside the measurement; the verifier reads the same
 * file to prove the numbers it reports came from this build.
 */
internal data class SurahQuality(
    val reciter: String,
    val surah: Int,
    val bitrate: Int,
    val ayahs: Int,
    val totalDurationMs: Double,
    val minSnrDb: Double,
    val minSegmentalSnrDb: Double,
    val maxLevelDeltaDb: Double,
    val maxDurationDeltaMs: Double,
    val maxDriftMs: Double,
    val failed: List<String>,
) {
    val passed: Boolean get() = failed.isEmpty()

    fun sentence(): String =
        "snr %.1f dB, segmental %.1f dB, level %.2f dB, duration %.1f ms, drift %.0f ms".format(
            minSnrDb,
            minSegmentalSnrDb,
            maxLevelDeltaDb,
            maxDurationDeltaMs,
            maxDriftMs,
        )
}
