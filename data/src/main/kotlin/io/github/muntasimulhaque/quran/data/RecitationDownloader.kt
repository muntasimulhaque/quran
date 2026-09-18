package io.github.muntasimulhaque.quran.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Downloads one recitation package and unpacks it into the app's own storage.
 *
 * The file is streamed to a temporary name while its SHA-256 is computed, and
 * it is only unpacked when the hash matches the manifest. The temporary file
 * never becomes an ayah file, and a cancelled download leaves nothing behind.
 * This is the app's only network use, and it only happens when the reader asks
 * for a surah.
 */
class RecitationDownloader(private val context: Context) {

    suspend fun download(
        packageToFetch: RecitationPackage,
        folder: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val temporary = File(context.cacheDir, "recitation-${packageToFetch.recitation}-${packageToFetch.surah}.part")
        try {
            val connection = java.net.URL(packageToFetch.url).openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/octet-stream")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP ${connection.responseCode}"))
            }
            val total = if (packageToFetch.bytes > 0) packageToFetch.bytes else connection.contentLengthLong
            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        read += count
                        onProgress(read, total)
                    }
                }
            }
            connection.disconnect()
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(packageToFetch.sha256, ignoreCase = true)) {
                return@withContext Result.failure(IllegalStateException("checksum mismatch"))
            }
            unpack(temporary, folder)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            temporary.delete()
        }
    }

    private fun unpack(zip: File, folder: String) {
        val target = File(context.filesDir, "recitations/$folder")
        target.mkdirs()
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val name = entry.name
                // Packages hold flat ayah files; anything that tries to escape is refused.
                if (name.contains('/') || name.contains('\\') || name.startsWith("..")) {
                    input.closeEntry()
                    continue
                }
                if (!entry.isDirectory) {
                    File(target, name).outputStream().buffered().use { output -> input.copyTo(output) }
                }
                input.closeEntry()
            }
        }
    }
}
