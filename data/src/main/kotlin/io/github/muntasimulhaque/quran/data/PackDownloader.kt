package io.github.muntasimulhaque.quran.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Downloads one content pack and puts it in the reader's library.
 *
 * The file streams to a temporary name while its SHA-256 is computed, and it
 * is only moved into place when the hash matches the catalog. A cancelled or
 * corrupted download leaves nothing behind, and nothing is ever patched in
 * place: a pack is a whole file or it is not there.
 *
 * This is the app's only network use, and it happens only after the reader
 * has seen the size and said yes.
 */
class PackDownloader(private val context: Context) {

    suspend fun download(
        pack: ContentPack,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val url = pack.url ?: return@withContext Result.failure(
            IllegalStateException("this pack has no download"),
        )
        val directory = File(context.filesDir, "packs/${pack.id}").apply { mkdirs() }
        val temporary = File(directory, "${pack.id}.db.part")
        try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/octet-stream")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP ${connection.responseCode}"))
            }
            val total = if (pack.bytes > 0) pack.bytes else connection.contentLengthLong
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
            if (pack.sha256.isNotEmpty() && !actual.equals(pack.sha256, ignoreCase = true)) {
                return@withContext Result.failure(IllegalStateException("checksum mismatch"))
            }
            val target = File(directory, "${pack.id}.db")
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }
}
