package io.github.muntasimulhaque.quran.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * One resumable transfer, shared by the two packages that used to restart from
 * zero.
 *
 * The content packs have resumed since 9.3: the part on disk is kept, the next
 * attempt asks for the rest with a Range header, and a server that ignores the
 * range is restarted from the beginning. The recitation and word-audio
 * downloaders used a fresh temporary file on every attempt, so a dropped
 * connection on a 186 MB surah threw the whole transfer away. This is that
 * same rule, in one place, for both (owner decision, 23.1).
 *
 * A cancellation is the reader's own word and travels as itself. Every other
 * failure keeps the part so the next attempt can resume; a part whose hash
 * does not match is thrown away and fetched once from the beginning.
 */
internal object ResumableDownload {

    data class Spec(
        val url: String,
        val bytes: Long,
        val sha256: String,
        /** The ceiling when the catalog does not know a size. */
        val maxBytes: Long = 512L * 1024 * 1024,
        /** The longest a transfer may take from first byte to last. */
        val maxMillis: Long = 60L * 60 * 1000,
    )

    suspend fun download(
        part: File,
        spec: Spec,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        /** The production door, or a test's own HTTP server. */
        open: (String, (HttpURLConnection) -> Unit) -> HttpURLConnection = { url, configure ->
            ReleaseHttp.open(url, configure)
        },
    ): Result<Unit> {
        if (spec.bytes <= 0 || !SafePaths.validHash(spec.sha256)) {
            return Result.failure(IllegalStateException("the package has no valid identity"))
        }
        part.parentFile?.mkdirs()
        var startAt = part.length()
        if (startAt > spec.bytes) {
            part.delete()
            startAt = 0L
        }
        while (true) {
            val fetched = fetch(part, spec, startAt, onProgress, open)
            val failure = fetched.exceptionOrNull()
            if (failure != null) {
                // A part that already holds the whole file is a completed
                // transfer whose rename never happened. The server answers
                // the Range request with 416, and retrying the same range
                // would stay stuck forever. Throw the part away and start
                // once from the beginning; the hash check then proves it.
                if (startAt > 0 && failure.message == "HTTP 416") {
                    part.delete()
                    startAt = 0L
                    continue
                }
                return Result.failure(failure)
            }
            if (matches(part, spec.sha256)) return Result.success(Unit)
            part.delete()
            if (startAt == 0L) {
                return Result.failure(IllegalStateException("checksum mismatch"))
            }
            startAt = 0L
        }
    }

    /** One HTTP pass, appending to [part] from [startAt]. */
    private suspend fun fetch(
        part: File,
        spec: Spec,
        startAt: Long,
        onProgress: (Long, Long) -> Unit,
        open: (String, (HttpURLConnection) -> Unit) -> HttpURLConnection,
    ): Result<Unit> {
        var connection: HttpURLConnection? = null
        return try {
            // Every redirect is checked before the connection that would use
            // it is opened, not after the request has already been sent.
            connection = open(spec.url) { request ->
                if (startAt > 0) request.setRequestProperty("Range", "bytes=$startAt-")
            }
            val code = connection.responseCode
            val resumed = startAt > 0 && code == HttpURLConnection.HTTP_PARTIAL
            if (startAt > 0 && code == HTTP_RANGE_NOT_SATISFIABLE) {
                return Result.failure(IllegalStateException("HTTP $code"))
            }
            if (!resumed && code !in 200..299) {
                return Result.failure(IllegalStateException("HTTP $code"))
            }
            if (resumed) validateContentRange(connection, startAt, spec.bytes)
            val from = if (resumed) startAt else 0L
            val maxBytes = if (spec.bytes > 0) spec.bytes else spec.maxBytes
            val contentLength = connection.contentLengthLong
            if (contentLength >= 0 && from + contentLength > maxBytes) {
                return Result.failure(
                    IllegalStateException("the download is larger than the catalog says"),
                )
            }
            val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            if (!type.isNullOrEmpty() && type !in ALLOWED_CONTENT_TYPES) {
                return Result.failure(IllegalStateException("the download is not an octet stream"))
            }
            val total = when {
                spec.bytes > 0 -> spec.bytes
                contentLength >= 0 -> contentLength + from
                else -> 0L
            }
            val deadline = System.nanoTime() + spec.maxMillis * 1_000_000L
            var read = from
            connection.inputStream.use { input ->
                FileOutputStream(part, resumed).buffered().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        coroutineContext.ensureActive()
                        if (System.nanoTime() > deadline) {
                            return Result.failure(IllegalStateException("the download took too long"))
                        }
                        val count = input.read(buffer)
                        if (count < 0) break
                        read += count
                        if (read > maxBytes) {
                            return Result.failure(
                                IllegalStateException("the download is larger than the catalog says"),
                            )
                        }
                        output.write(buffer, 0, count)
                        onProgress(read, total)
                    }
                }
            }
            if (spec.bytes > 0 && read != spec.bytes) {
                return Result.failure(IllegalStateException("the download ended at $read bytes"))
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

    /**
     * A resumed response must start where the part ends and, when the catalog
     * knows the size, end at the catalog's size. A server that answers a
     * different range is not appended to.
     */
    private fun validateContentRange(
        connection: HttpURLConnection,
        startAt: Long,
        expectedBytes: Long,
    ) {
        val header = connection.getHeaderField("Content-Range")
            ?: throw IllegalStateException("the resumed download has no range")
        val match = CONTENT_RANGE.find(header.trim())
            ?: throw IllegalStateException("the resumed range cannot be read")
        val first = match.groupValues[1].toLongOrNull()
            ?: throw IllegalStateException("the resumed range cannot be read")
        val total = match.groupValues[3].takeIf { it != "*" }?.toLongOrNull()
        if (first != startAt || (expectedBytes > 0 && total != null && total != expectedBytes)) {
            throw IllegalStateException("the resumed range is not this package")
        }
    }

    private suspend fun matches(file: File, expected: String): Boolean {
        if (!file.isFile) return false
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

    private const val HTTP_RANGE_NOT_SATISFIABLE = 416

    private val CONTENT_RANGE = Regex("""^bytes (\d+)-(\d+)/(\d+|\*)\z""")

    private val ALLOWED_CONTENT_TYPES = setOf(
        "application/octet-stream",
        "binary/octet-stream",
        "application/zip",
        "application/x-zip-compressed",
    )
}
