package io.github.muntasimulhaque.quran.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Downloads one content pack and puts it in the reader's library.
 *
 * The file streams to a temporary name, is verified whole against the
 * catalog's SHA-256, and only then is moved into place: a pack is a whole
 * file or it is not there, and a corrupted or interrupted download never
 * joins the library.
 *
 * A dropped connection leaves the part on disk, and the next attempt asks for
 * the rest with a Range header instead of fetching the whole file again. The
 * tafsir is tens of megabytes, and a reader whose train goes into a tunnel
 * must not pay for the whole of it twice. A server that ignores the range
 * restarts the file in the same attempt; a part whose hash does not match the
 * catalog (a pack that was replaced between two attempts) is thrown away and
 * fetched once from the beginning.
 *
 * This is the app's only network use, and it happens for a pack the reader
 * asked for, by a language choice or a tap.
 *
 * A cancel has to stop the transfer itself. The read loop is blocking, and a
 * blocking read is not a suspension point, so the job's own state is read
 * with every chunk; otherwise the pack was still fetched after the reader
 * closed its line, and the next progress chunk put the line back (owner
 * report, this session). The part on disk is kept: a pack that resumes is
 * data the reader does not pay for twice.
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
        // The first attempt resumes whatever a dropped connection left. An
        // attempt after a failed verification starts a whole file, because
        // the only reason to make one is a part that is not this pack.
        var startAt = temporary.length()
        while (true) {
            coroutineContext.ensureActive()
            val fetched = fetch(url, temporary, startAt, pack.bytes, onProgress)
            val failure = fetched.exceptionOrNull()
            if (failure != null) {
                // A part that already holds the whole file is a completed
                // transfer whose rename never happened. The server answers
                // the Range request with 416, and retrying the same range
                // would stay stuck forever. Throw the part away and start
                // once from the beginning; the hash check then proves it.
                if (startAt > 0 && failure.message == "HTTP 416") {
                    temporary.delete()
                    startAt = 0L
                    continue
                }
                return@withContext Result.failure(failure)
            }
            if (pack.sha256.isEmpty() || matches(temporary, pack.sha256)) {
                return@withContext move(temporary, directory, pack.id)
            }
            temporary.delete()
            if (startAt == 0L) {
                return@withContext Result.failure(IllegalStateException("checksum mismatch"))
            }
            startAt = 0L
        }
        @Suppress("UNREACHABLE_CODE")
        Result.failure(IllegalStateException("unreachable"))
    }

    /** One HTTP pass, appending to [temporary] from [startAt]. */
    private suspend fun fetch(
        url: String,
        temporary: File,
        startAt: Long,
        packBytes: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<Unit> {
        var connection: java.net.HttpURLConnection? = null
        return try {
            // The app's one network use is HTTPS to the project's own release
            // page: a URL of another shape, or a redirect that lands on one,
            // is refused rather than fetched in the clear.
            if (!url.startsWith("https://") || !ReleaseHosts.isAllowed(url)) {
                return Result.failure(IllegalStateException("the pack URL is not the project's release host"))
            }
            connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/octet-stream")
            if (startAt > 0) connection.setRequestProperty("Range", "bytes=$startAt-")
            connection.connect()
            if (connection.url.protocol != "https" ||
                !ReleaseHosts.isAllowed(connection.url.toString())
            ) {
                return Result.failure(
                    IllegalStateException("the pack redirect left the project's release host"),
                )
            }
            val resumed =
                startAt > 0 && connection.responseCode == java.net.HttpURLConnection.HTTP_PARTIAL
            if (!resumed && connection.responseCode !in 200..299) {
                return Result.failure(IllegalStateException("HTTP ${connection.responseCode}"))
            }
            // A server that ignored the Range sends the whole file, and the
            // part on disk is the wrong thing to append to.
            val from = if (resumed) startAt else 0L
            val total = if (packBytes > 0) packBytes else connection.contentLengthLong + from
            var read = from
            connection.inputStream.use { input ->
                FileOutputStream(temporary, resumed).buffered().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        // The reader's Cancel, read between chunks, so the
                        // transfer stops within one read of it.
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        read += count
                        onProgress(read, total)
                    }
                }
            }
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            // A cancel is the reader's word, not a failure to report.
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun matches(file: File, expected: String): Boolean {
        if (!file.exists()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                coroutineContext.ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
            .equals(expected, ignoreCase = true)
    }

    private fun move(temporary: File, directory: File, id: String): Result<Unit> {
        val target = File(directory, "$id.db")
        return runCatching {
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
        }
    }
}
