package io.github.muntasimulhaque.quran.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
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
 *
 * A cancel is the reader's own word, so it has to stop the transfer itself.
 * The read loop below is blocking, and a blocking read is not a suspension
 * point: cancelling the caller's job did not end it, the loop ran to its end,
 * verified its hash, and unpacked the surah the reader had just dropped
 * (owner report, this session). The job's own state is therefore read with
 * every chunk, and the cancellation is passed on instead of being turned into
 * a failure result. The loop has no suspension point, so this is the only
 * place that can end it.
 *
 * A surah lands whole or not at all. The package is read into a staging folder
 * that no reader ever sees, and its files are moved into place only after the
 * last entry has been read and the job is still on, so a cancelled transfer
 * installs nothing and a process that dies while the package is being read
 * leaves no half surah that the app would count as downloaded.
 */
class RecitationDownloader(private val context: Context) {

    suspend fun download(
        packageToFetch: RecitationPackage,
        folder: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // One transfer per attempt: two downloads of one surah would otherwise
        // stream into the same part file. A part left by a process that died
        // is the cache's to clear, and the next attempt never reuses it.
        val temporary = File(
            context.cacheDir,
            "recitation-${packageToFetch.recitation}-${packageToFetch.surah}-${System.nanoTime()}.part",
        )
        // Kept outside the try so every way out closes the socket: a cancel
        // throws from the read loop, and the connection is what holds the
        // transfer open.
        var connection: java.net.HttpURLConnection? = null
        try {
            // The one recitation host is HTTPS, and a redirect that leaves it
            // is refused: this is the app's only other network use.
            if (!packageToFetch.url.startsWith("https://")) {
                return@withContext Result.failure(IllegalStateException("the recitation URL is not https"))
            }
            connection = java.net.URL(packageToFetch.url).openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/octet-stream")
            connection.connect()
            if (connection.url.protocol != "https") {
                return@withContext Result.failure(IllegalStateException("the recitation redirect left https"))
            }
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
                        // The reader's Cancel, read between chunks. A chunk is
                        // 64 KB, so the transfer stops within one read of it.
                        coroutineContext.ensureActive()
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
            connection = null
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(packageToFetch.sha256, ignoreCase = true)) {
                return@withContext Result.failure(IllegalStateException("checksum mismatch"))
            }
            // The hash is checked before anything becomes an ayah file, and
            // the job is checked again here: a cancel that landed during the
            // last chunk must not unpack what it cancelled.
            coroutineContext.ensureActive()
            unpack(temporary, folder, packageToFetch.surah)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            // A cancel is not a failure the pill should report: it is the
            // reader's word, and it travels as itself.
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            connection?.disconnect()
            temporary.delete()
        }
    }

    /**
     * Reads the verified package into a staging folder and moves it in, one
     * file at a time, once it is whole.
     *
     * The gate is what lets the staging root be swept: an unpack is short and
     * no two of them ever run together, so anything standing in the root when
     * one starts was left by a process that is gone.
     */
    private suspend fun unpack(zip: File, folder: String, surah: Int) = unpackGate.withLock {
        val imported = File(context.filesDir, "recitations")
        val target = File(imported, folder)
        val staging = File(imported, STAGING)
        staging.mkdirs()
        staging.listFiles()?.forEach { it.deleteRecursively() }
        val unpacking = File(staging, "$folder-$surah").apply { mkdirs() }
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                coroutineContext.ensureActive()
                val entry = input.nextEntry ?: break
                val name = entry.name
                // Packages hold flat ayah files; anything that tries to escape is refused.
                if (name.contains('/') || name.contains('\\') || name.startsWith("..")) {
                    input.closeEntry()
                    continue
                }
                if (!entry.isDirectory) {
                    File(unpacking, name).outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }
                input.closeEntry()
            }
        }
        coroutineContext.ensureActive()
        target.mkdirs()
        // The move is the commit: it is a rename on one filesystem, so it is
        // short, and it is not interrupted halfway. A surah is whole on the
        // disk or it is not there (owner report, this session).
        val staged = unpacking.listFiles().orEmpty()
        if (staged.isEmpty()) throw IllegalStateException("the package held no ayah files")
        staged.forEach { stagedFile ->
            val landed = File(target, stagedFile.name)
            if (!stagedFile.renameTo(landed)) {
                stagedFile.copyTo(landed, overwrite = true)
                stagedFile.delete()
            }
        }
        unpacking.deleteRecursively()
        staging.delete()
        Unit
    }

    private companion object {
        /** Where a package is read before it becomes ayah files. */
        const val STAGING = ".staging"

        /** One unpack at a time, so the staging root can be swept whole. */
        val unpackGate = Mutex()
    }
}
