package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds and publishes the per-surah recitation packages.
 *
 * One ZIP per reciter per surah holds that surah's ayah files exactly as the
 * content database names them. The manifest carries each package's size and
 * SHA-256, and it is what the app ships: a package is only accepted after its
 * hash matches, so a bad download or a tampered file can never play.
 */
class Audio(private val root: File) {

    private val sampleAyahs = listOf(
        "1:1", "1:2", "1:3", "1:4", "1:5", "1:6", "1:7",
        "2:1", "2:2", "2:255",
        "18:1", "36:1", "55:1", "78:1",
        "112:1", "112:2", "112:3", "112:4",
        "114:1", "114:2", "114:3", "114:4", "114:5", "114:6",
    )

    private val reciterTags = mapOf(
        "minshawi" to "recitation-minshawi",
        "husary" to "recitation-husary",
    )

    private val githubBase = "https://github.com/muntasimulhaque/quran/releases/download"

    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()

    // ------------------------------------------------------------------ sample

    fun sample(): Int {
        val database = File(root, "content/quran.db")
        if (!database.exists()) {
            println("audio: content/quran.db is missing; run build first")
            return 1
        }
        val target = File(root, "content/work/audio-dev")
        val semaphore = Semaphore(6)
        val downloaded = AtomicInteger()
        val skipped = AtomicInteger()
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        val jobs = mutableListOf<CompletableFuture<*>>()

        openSqlite(database).use { connection ->
            connection.each(
                "SELECT ra.audio_path FROM recitation_ayah ra " +
                    "JOIN ayah a ON a.number = ra.ayah_number " +
                    "WHERE a.verse_key IN (${sampleAyahs.joinToString(",") { "'$it'" }}) " +
                    "ORDER BY ra.recitation, a.number",
            ) { rs ->
                val audioPath = rs.getString(1) ?: return@each
                val file = File(target, audioPath)
                if (file.length() > 0) {
                    skipped.incrementAndGet()
                    return@each
                }
                semaphore.acquire()
                jobs += client.sendAsync(getRequest("https://audio-cdn.tarteel.ai/quran/$audioPath"), HttpResponse.BodyHandlers.ofByteArray())
                    .thenAccept { response ->
                        if (response.statusCode() == 200 && response.body().isNotEmpty()) {
                            file.parentFile?.mkdirs()
                            Files.write(file.toPath(), response.body())
                            downloaded.incrementAndGet()
                        } else {
                            failures += "$audioPath (HTTP ${response.statusCode()})"
                        }
                    }.whenComplete { _, _ -> semaphore.release() }
            }
        }
        CompletableFuture.allOf(*jobs.toTypedArray()).join()

        val bytes = target.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        println("audio: sample at ${target.relativeTo(root)}")
        println("audio: downloaded ${downloaded.get()}, already present ${skipped.get()}, ${bytes / 1024} KB total")
        if (failures.isEmpty()) return 0
        println("AUDIO FAILURES (${failures.size})")
        failures.take(10).forEach { println("  - $it") }
        return 1
    }

    // ------------------------------------------------------------------- packs

    /**
     * Builds one ZIP per reciter per surah and rewrites the manifest. Existing
     * packages are trusted only when their size matches the manifest, so the
     * command can resume after an interruption without touching good work.
     */
    fun packs(surahFilter: Set<Int>, reciterFilter: Set<String>): Int {
        val database = File(root, "content/quran.db")
        if (!database.exists()) {
            println("audio: content/quran.db is missing; run build first")
            return 1
        }
        val output = File(root, "content/work/audio-packs")
        val manifestFile = File(root, "content/recitation-manifest.json")
        val existing = readManifest(manifestFile)
        val failures = mutableListOf<String>()
        var built = 0
        var skipped = 0
        var jobs = 0

        val rows = mutableListOf<Triple<String, Int, String>>() // reciter, surah, audio_path
        openSqlite(database).use { connection ->
            connection.each(
                "SELECT ra.recitation, a.surah, ra.audio_path FROM recitation_ayah ra " +
                    "JOIN ayah a ON a.number = ra.ayah_number ORDER BY ra.recitation, a.surah, a.ayah",
            ) { rs ->
                val reciter = rs.getString(1)
                val surah = rs.getInt(2)
                if (reciter !in reciterFilter) return@each
                if (surahFilter.isNotEmpty() && surah !in surahFilter) return@each
                rows += Triple(reciter, surah, rs.getString(3))
            }
        }

        for ((reciter, surah) in rows.map { it.first to it.second }.distinct()) {
            val paths = rows.filter { it.first == reciter && it.second == surah }.map { it.third }
            val zip = File(output, "$reciter/surah-%03d.zip".format(surah))
            val known = existing[reciter]?.get(surah)
            if (zip.isFile && known != null && zip.length() == known.first) {
                skipped++
                continue
            }
            try {
                buildSurahZip(reciter, surah, paths, zip)
                built++
                print(".")
            } catch (error: Exception) {
                failures += "$reciter surah $surah: ${error.message}"
            }
            jobs++
        }
        if (jobs > 0) println()

        val manifest = buildManifest(output, existing)
        manifestFile.writeText(manifest)
        println("audio: packs built $built, reused $skipped, manifest at ${manifestFile.relativeTo(root)}")
        if (failures.isEmpty()) return 0
        println("AUDIO FAILURES (${failures.size})")
        failures.forEach { println("  - $it") }
        return 1
    }

    private fun buildSurahZip(reciter: String, surah: Int, paths: List<String>, zip: File) {
        val raw = File(root, "content/work/audio-packs/$reciter/raw")
        raw.mkdirs()
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        val semaphore = Semaphore(12)
        val jobs = mutableListOf<CompletableFuture<*>>()
        for (path in paths) {
            val file = File(raw, path.substringAfterLast('/'))
            if (file.length() > 0) continue
            semaphore.acquire()
            jobs += client
                .sendAsync(getRequest("https://audio-cdn.tarteel.ai/quran/$path"), HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept { response ->
                    if (response.statusCode() == 200 && response.body().isNotEmpty()) {
                        Files.write(file.toPath(), response.body())
                    } else {
                        failures += "$path (HTTP ${response.statusCode()})"
                    }
                }.whenComplete { _, _ -> semaphore.release() }
        }
        CompletableFuture.allOf(*jobs.toTypedArray()).join()
        if (failures.isNotEmpty()) throw IllegalStateException(failures.first())

        val names = paths.map { it.substringAfterLast('/') }
        for (name in names) {
            val file = File(raw, name)
            if (file.length() == 0L) throw IllegalStateException("missing $name")
            if (!looksLikeAudio(file)) throw IllegalStateException("not an mp3: $name")
        }
        zip.parentFile?.mkdirs()
        ZipOutputStream(zip.outputStream().buffered()).use { out ->
            for (name in names) {
                out.putNextEntry(ZipEntry(name))
                File(raw, name).inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
        }
        // The raw ayah files have served their purpose; the ZIP is the product.
        names.forEach { File(raw, it).delete() }
    }

    /** An MP3 starts with an ID3 tag or an MPEG frame sync; anything else is not audio. */
    private fun looksLikeAudio(file: File): Boolean {
        val head = ByteArray(3)
        file.inputStream().use { if (it.read(head) < 3) return false }
        val id3 = head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte()
        val sync = head[0] == 0xFF.toByte() && (head[1].toInt() and 0xE0) == 0xE0
        return id3 || sync
    }

    /**
     * The manifest always describes what is on disk, whatever filter built it,
     * and reuses recorded hashes so a resumed run does not re-hash gigabytes.
     */
    private fun buildManifest(
        output: File,
        previous: Map<String, Map<Int, Pair<Long, String>>>,
    ): String {
        val json = buildJsonObject {
            put("baseUrl", githubBase)
            put(
                "recitations",
                buildJsonArray {
                    for ((reciter, tag) in reciterTags) {
                        val zips = File(output, reciter)
                            .listFiles { file -> file.name.endsWith(".zip") }
                            ?.sortedBy { it.name }
                            ?: continue
                        if (zips.isEmpty()) continue
                        add(
                            buildJsonObject {
                                put("id", reciter)
                                put("tag", tag)
                                put(
                                    "surahs",
                                    buildJsonArray {
                                        for (zip in zips) {
                                            val surah = zip.name.removePrefix("surah-").removeSuffix(".zip").toIntOrNull() ?: continue
                                            val known = previous[reciter]?.get(surah)
                                            val sha = known?.takeIf { it.first == zip.length() }?.second
                                                ?: sha256(zip)
                                            add(
                                                buildJsonObject {
                                                    put("surah", surah)
                                                    put("asset", zip.name)
                                                    put("bytes", zip.length())
                                                    put("sha256", sha)
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
        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json)
    }

    private fun readManifest(file: File): Map<String, Map<Int, Pair<Long, String>>> {
        if (!file.exists()) return emptyMap()
        val root = runCatching { Json.parseToJsonElement(file.readText()).jsonObject }.getOrNull() ?: return emptyMap()
        val out = mutableMapOf<String, MutableMap<Int, Pair<Long, String>>>()
        for (recitation in (root["recitations"] as? JsonArray).orEmpty()) {
            val objectValue = recitation.jsonObject
            val id = objectValue["id"]?.let { (it as? JsonPrimitive)?.content } ?: continue
            val surahs = out.getOrPut(id) { mutableMapOf() }
            for (entry in (objectValue["surahs"] as? JsonArray).orEmpty()) {
                val row = entry.jsonObject
                val surah = (row["surah"] as? JsonPrimitive)?.content?.toIntOrNull() ?: continue
                val bytes = (row["bytes"] as? JsonPrimitive)?.content?.toLongOrNull() ?: continue
                val sha = (row["sha256"] as? JsonPrimitive)?.content ?: continue
                surahs[surah] = bytes to sha
            }
        }
        return out
    }

    // ----------------------------------------------------------------- publish

    /** Uploads every built package to its GitHub Release, skipping what is there. */
    fun publish(reciterFilter: Set<String>): Int {
        val output = File(root, "content/work/audio-packs")
        val failures = mutableListOf<String>()
        for ((reciter, tag) in reciterTags) {
            if (reciter !in reciterFilter) continue
            val zips = File(output, reciter).listFiles { file -> file.name.endsWith(".zip") }?.sorted() ?: continue
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
            val missing = zips.filterNot { it.name in uploaded }
            if (missing.isEmpty()) {
                println("audio: $tag is up to date (${zips.size} packages)")
                continue
            }
            var done = 0
            for (batch in missing.chunked(20)) {
                val args = mutableListOf("release", "upload", tag)
                args += batch.map { it.absolutePath }
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
        if (failures.isEmpty()) return 0
        println("AUDIO FAILURES (${failures.size})")
        failures.forEach { println("  - $it") }
        return 1
    }

    private fun releaseExists(tag: String): Boolean =
        runGh("release", "view", tag, "--json", "tagName")

    private fun uploadedAssets(tag: String): Set<String> {
        val process = ProcessBuilder("gh", "release", "view", tag, "--json", "assets", "--jq", ".assets[].name")
            .redirectErrorStream(true)
            .start()
        val text = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return text.lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
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

    /** `audio sample`, `audio packs [surahs] [reciters]`, `audio publish [reciters]`. */
    fun run(args: List<String>): Int = when (args.firstOrNull()) {
        "sample" -> sample()
        "packs" -> packs(
            surahFilter = parseNumbers(args.getOrNull(1)),
            reciterFilter = parseNames(args.getOrNull(2)),
        )
        "publish" -> publish(parseNames(args.getOrNull(1)))
        else -> {
            println("usage: tools audio <sample|packs|publish>")
            2
        }
    }

    private fun parseNumbers(raw: String?): Set<Int> =
        raw.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

    private fun parseNames(raw: String?): Set<String> =
        raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet().ifEmpty { reciterTags.keys }
}
