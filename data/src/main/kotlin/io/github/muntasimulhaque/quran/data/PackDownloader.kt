package io.github.muntasimulhaque.quran.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
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
    ): Result<Unit> = downloadGate.withLock {
        withContext(Dispatchers.IO) {
            val url = pack.url ?: return@withContext Result.failure(
                IllegalStateException("this pack has no download"),
            )
            if (!SafePaths.validId(pack.id) || !SafePaths.validHash(pack.sha256) || pack.bytes < 0) {
                return@withContext Result.failure(
                    IllegalStateException("the pack has no valid id or checksum"),
                )
            }
            val root = File(context.filesDir, "packs").apply { mkdirs() }
            val directory = SafePaths.childUnder(root, pack.id)
                ?: return@withContext Result.failure(
                    IllegalStateException("the pack id is not a safe folder name"),
                )
            directory.mkdirs()
            val temporary = SafePaths.childUnder(directory, "${pack.id}.db.part")
                ?: return@withContext Result.failure(
                    IllegalStateException("the pack part is not a safe file name"),
                )
            // The first attempt resumes whatever a dropped connection left. An
            // attempt after a failed verification starts a whole file, because
            // the only reason to make one is a part that is not this pack.
            var startAt = temporary.length()
            if (pack.bytes > 0 && startAt > pack.bytes) {
                temporary.delete()
                startAt = 0L
            }
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
                if (matches(temporary, pack.sha256)) {
                    val target = SafePaths.childUnder(directory, "${pack.id}.db")
                        ?: return@withContext Result.failure(
                            IllegalStateException("the pack file is not a safe name"),
                        )
                    val moved = move(temporary, target)
                    if (moved.isSuccess) PackVerifier(context).remember(pack, target)
                    return@withContext moved
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
    }

    /** One HTTP pass, appending to [temporary] from [startAt]. */
    private suspend fun fetch(
        url: String,
        temporary: File,
        startAt: Long,
        packBytes: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<Unit> {
        var connection: HttpURLConnection? = null
        return try {
            // Every redirect is checked before the connection that would use
            // it is opened, not after the request has already been sent.
            connection = ReleaseHttp.open(url) { open ->
                if (startAt > 0) open.setRequestProperty("Range", "bytes=$startAt-")
            }
            val code = connection.responseCode
            val resumed = startAt > 0 && code == HttpURLConnection.HTTP_PARTIAL
            if (!resumed && code !in 200..299) {
                return Result.failure(IllegalStateException("HTTP $code"))
            }
            if (resumed) validateContentRange(connection, startAt, packBytes)
            val from = if (resumed) startAt else 0L
            val expected = packBytes
            val maxBytes = if (expected > 0) expected else MAX_PACK_BYTES
            val contentLength = connection.contentLengthLong
            if (contentLength >= 0 && from + contentLength > maxBytes) {
                return Result.failure(IllegalStateException("the download is larger than the catalog says"))
            }
            val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            if (!type.isNullOrEmpty() && type !in ALLOWED_CONTENT_TYPES) {
                return Result.failure(IllegalStateException("the download is not an octet stream"))
            }
            val total = if (expected > 0) expected else contentLength + from
            val deadline = System.nanoTime() + MAX_DOWNLOAD_MILLIS * 1_000_000L
            var read = from
            connection.inputStream.use { input ->
                FileOutputStream(temporary, resumed).buffered().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        // The reader's Cancel, read between chunks, so the
                        // transfer stops within one read of it.
                        coroutineContext.ensureActive()
                        if (System.nanoTime() > deadline) {
                            return Result.failure(IllegalStateException("the download took too long"))
                        }
                        val count = input.read(buffer)
                        if (count < 0) break
                        read += count
                        if (read > maxBytes) {
                            return Result.failure(IllegalStateException("the download is larger than the catalog says"))
                        }
                        output.write(buffer, 0, count)
                        onProgress(read, total)
                    }
                }
            }
            if (expected > 0 && read != expected) {
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
        packBytes: Long,
    ) {
        val header = connection.getHeaderField("Content-Range")
            ?: throw IllegalStateException("the resumed download has no range")
        val match = CONTENT_RANGE.find(header.trim())
            ?: throw IllegalStateException("the resumed range cannot be read")
        val first = match.groupValues[1].toLongOrNull()
            ?: throw IllegalStateException("the resumed range cannot be read")
        val total = match.groupValues[3].takeIf { it != "*" }?.toLongOrNull()
        if (first != startAt || (packBytes > 0 && total != null && total != packBytes)) {
            throw IllegalStateException("the resumed range is not this pack")
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

    private fun move(temporary: File, target: File): Result<Unit> = runCatching {
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw IllegalStateException("the pack could not be moved into place")
        }
    }

    private companion object {
        /** One pack transfer at a time, across the library and the updater. */
        val downloadGate = kotlinx.coroutines.sync.Mutex()

        /** A pack larger than this is not one the catalog offers. */
        const val MAX_PACK_BYTES = 256L * 1024 * 1024

        /** The longest a transfer may take from first byte to last. */
        const val MAX_DOWNLOAD_MILLIS = 30L * 60 * 1000

        val ALLOWED_CONTENT_TYPES = setOf(
            "application/octet-stream",
            "binary/octet-stream",
            "application/zip",
            "application/x-zip-compressed",
        )

        val CONTENT_RANGE = Regex("""^bytes (\d+)-(\d+)/(\d+|\*)\z""")
    }
}
