package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Brings large raw sources to a machine that does not have them yet.
 *
 * Only datasets that carry an `assetUrl` in the manifest are downloaded, and
 * every download is checked against the pinned SHA-256 before it is moved
 * into place. A file that is already present and correct is left alone, so a
 * machine that received the file by other means never touches the network.
 *
 * The 604 page fonts are the only such dataset. The small QUL exports stay
 * local to the maintainer and are listed for manual download when missing.
 */
class Fetch(private val root: File) {

    /**
     * Large datasets that are unpacked once they are verified, so the app and
     * the asset pack can read plain files. The key is the manifest id and the
     * value is the directory that receives the zip's contents.
     */
    private val unpackTargets = mapOf(
        "mushaf-fonts-v2" to "content/work/fonts-v2/fonts/pages",
        "quran-font-hafs" to "content/work/fonts-hafs",
    )

    fun run(): Int {
        val manifest = loadManifest(root)
        var failures = 0
        var manual = 0
        failures += fetchContentDatabase()
        failures += fetchPacks()
        for (dataset in manifest.datasets) {
            val file = File(root, dataset.path)
            if (file.exists() && sha256(file) == dataset.sha256) {
                println("fetch: have ${dataset.id}")
                unpack(dataset.id, file)
                continue
            }
            val assetUrl = dataset.assetUrl
            if (assetUrl == null) {
                manual++
                println("fetch: manual ${dataset.id}: get it from ${dataset.url} and place it at ${dataset.path}")
                continue
            }
            println("fetch: downloading ${dataset.id} from $assetUrl")
            try {
                try {
                    download(assetUrl, file)
                } catch (direct: Exception) {
                    // This machine's JVM cannot always reach the release CDN host;
                    // the GitHub CLI can, and it is already part of the pipeline.
                    println("fetch: ${dataset.id} direct download failed (${direct.message ?: direct::class.java.simpleName}); trying gh")
                    downloadWithGh(assetUrl, file)
                }
                val actual = sha256(file)
                if (actual != dataset.sha256) {
                    println("fetch: ${dataset.id} hash $actual does not match ${dataset.sha256}; deleted")
                    file.delete()
                    failures++
                } else {
                    println("fetch: ${dataset.id} verified (${file.length()} bytes)")
                    unpack(dataset.id, file)
                }
            } catch (error: Exception) {
                println("fetch: ${dataset.id} failed: ${error.message ?: error::class.java.simpleName}")
                failures++
            }
        }
        if (failures != 0) {
            println("fetch: $failures asset(s) failed")
            return 1
        }
        if (manual != 0) {
            println("fetch: $manual dataset(s) need a manual download; the app build needs only the assets above")
        }
        return 0
    }

    private fun unpack(id: String, archive: File) {
        val targetPath = unpackTargets[id] ?: return
        if (!isZip(archive)) return
        val target = File(root, targetPath)
        val expected = when (id) {
            "mushaf-fonts-v2" -> 604
            "quran-font-hafs" -> 1
            else -> return
        }
        val present = target.walkTopDown().count { it.isFile && it.name.endsWith(".ttf") }
        if (present == expected) {
            println("fetch: $id already unpacked ($present fonts)")
            return
        }
        println("fetch: unpacking $id to $targetPath")
        extract(archive, target)
    }

    /**
     * Every pack the catalog names, so a fresh clone (and CI) can build the
     * app with the exact pack files the catalog pins, including the core pack
     * the app ships.
     */
    private fun fetchPacks(): Int {
        val catalogFile = File(root, "content/catalog.json")
        if (!catalogFile.exists()) {
            println("fetch: content/catalog.json is missing; cannot know which packs to fetch")
            return 1
        }
        // A plain text walk keeps this independent of the app's JSON shapes.
        val text = catalogFile.readText()
        val entries = Regex("\\{[^{}]*\"id\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}").findAll(text)
        var failures = 0
        for (match in entries) {
            val entry = match.value
            val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(entry)?.groupValues?.get(1) ?: continue
            val hash = Regex("\"sha256\"\\s*:\\s*\"([0-9a-f]{64})\"").find(entry)?.groupValues?.get(1) ?: continue
            val url = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"").find(entry)?.groupValues?.get(1) ?: continue
            val target = File(root, "content/packs/$id.db")
            if (target.exists() && sha256(target) == hash) {
                println("fetch: have pack $id")
                continue
            }
            println("fetch: downloading pack $id")
            failures += try {
                try {
                    download(url, target)
                } catch (direct: Exception) {
                    println("fetch: pack $id direct download failed (${direct.message ?: "error"}); trying gh")
                    downloadWithGh(url, target)
                }
                val actual = sha256(target)
                if (actual != hash) {
                    println("fetch: pack $id hash $actual does not match $hash; deleted")
                    target.delete()
                    1
                } else {
                    println("fetch: pack $id verified (${target.length()} bytes)")
                    0
                }
            } catch (error: Exception) {
                println("fetch: pack $id failed: ${error.message ?: error::class.java.simpleName}")
                1
            }
        }
        return failures
    }

    /**
     * The built content database is a derived artifact, so it lives in the
     * project's Releases, addressed by its own hash, and never in git. The
     * build report is the pin: whatever hash it names is what must be here.
     */
    private fun fetchContentDatabase(): Int {
        val report = File(root, "content/build-report.json")
        if (!report.exists()) {
            println("fetch: content/build-report.json is missing; cannot know which database to fetch")
            return 1
        }
        val text = report.readText()
        val hash = Regex("\"databaseSha256\"\\s*:\\s*\"([0-9a-f]{64})\"")
            .find(text)?.groupValues?.get(1) ?: return 1
        val bytes = Regex("\"databaseBytes\"\\s*:\\s*(\\d+)")
            .find(text)?.groupValues?.get(1)?.toLongOrNull()
        val file = File(root, "content/quran.db")
        if (file.exists() && sha256(file) == hash) {
            println("fetch: have content-db $hash")
            return 0
        }
        val tag = "content-db-" + hash.substring(0, 8)
        val url = "https://github.com/muntasimulhaque/quran/releases/download/$tag/quran.db"
        println("fetch: downloading content-db from $url")
        return try {
            try {
                download(url, file)
            } catch (direct: Exception) {
                println("fetch: content-db direct download failed (${direct.message ?: direct::class.java.simpleName}); trying gh")
                downloadWithGh(url, file)
            }
            val actual = sha256(file)
            if (actual != hash) {
                println("fetch: content-db hash $actual does not match $hash; deleted")
                file.delete()
                1
            } else {
                println("fetch: content-db verified (${file.length()} bytes" + (bytes?.let { ", expected $it" } ?: "") + ")")
                0
            }
        } catch (error: Exception) {
            println("fetch: content-db failed: ${error.message ?: error::class.java.simpleName}")
            1
        }
    }

    /**
     * Downloads a Release asset with the GitHub CLI, which reaches hosts this
     * JVM cannot. The URL carries the tag and the asset name; gh needs no
     * credentials for a public release when GH_TOKEN is present.
     */
    private fun downloadWithGh(url: String, destination: File) {
        val marker = "/releases/download/"
        val index = url.indexOf(marker)
        if (index < 0) throw IllegalStateException("no gh path for $url")
        val rest = url.substring(index + marker.length)
        val tag = rest.substringBefore('/')
        val asset = rest.substringAfter('/')
        val directory = destination.parentFile ?: throw IllegalStateException("no directory for $destination")
        directory.mkdirs()
        val process = ProcessBuilder(
            "gh", "release", "download", tag,
            "--pattern", asset,
            "--dir", directory.path,
            "--clobber",
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() != 0) {
            throw IllegalStateException("gh failed: ${output.take(200)}")
        }
        if (!destination.exists()) throw IllegalStateException("gh left no file at $destination")
    }

    private fun download(url: String, destination: File) {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, destination.name + ".part")
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 120_000
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("HTTP ${connection.responseCode} for $url")
        }
        connection.inputStream.use { input ->
            temporary.outputStream().buffered().use { output -> input.copyTo(output) }
        }
        connection.disconnect()
        Files.move(
            temporary.toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }
}
