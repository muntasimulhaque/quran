package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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

    fun run(): Int {
        val manifest = loadManifest(root)
        var failures = 0
        var manual = 0
        for (dataset in manifest.datasets) {
            val file = File(root, dataset.path)
            if (file.exists() && sha256(file) == dataset.sha256) {
                println("fetch: have ${dataset.id}")
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
                download(assetUrl, file)
                val actual = sha256(file)
                if (actual != dataset.sha256) {
                    println("fetch: ${dataset.id} hash $actual does not match ${dataset.sha256}; deleted")
                    file.delete()
                    failures++
                } else {
                    println("fetch: ${dataset.id} verified (${file.length()} bytes)")
                }
            } catch (error: Exception) {
                println("fetch: ${dataset.id} failed: ${error.message}")
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

    private fun download(url: String, destination: File) {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, destination.name + ".part")
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val request = HttpRequest.newBuilder(URI(url)).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofFile(temporary.toPath()))
        if (response.statusCode() != 200) {
            throw IllegalStateException("HTTP ${response.statusCode()} for $url")
        }
        Files.move(
            temporary.toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }
}
