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
        if (!SafePaths.validId(packageToFetch.recitation) ||
            packageToFetch.surah !in 1..114 ||
            !SafePaths.validHash(packageToFetch.sha256) ||
            packageToFetch.bytes < 0 ||
            !SafePaths.validFolder(folder)
        ) {
            return@withContext Result.failure(
                IllegalStateException("the recitation package has no valid identity"),
            )
        }
        // A unique temporary name per attempt: a part left by a process that
        // died is the cache's to clear, and no two attempts share it.
        val temporary = runCatching {
            File.createTempFile("recitation-${packageToFetch.recitation}-", ".part", context.cacheDir)
        }.getOrNull() ?: return@withContext Result.failure(
            IllegalStateException("the recitation part could not be created"),
        )
        // Kept outside the try so every way out closes the socket: a cancel
        // throws from the read loop, and the connection is what holds the
        // transfer open.
        var connection: java.net.HttpURLConnection? = null
        try {
            // Every redirect is checked before the connection that would use
            // it is opened, not after the request has already been sent.
            connection = ReleaseHttp.open(packageToFetch.url)
            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP ${connection.responseCode}"))
            }
            val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            if (!type.isNullOrEmpty() && type !in ALLOWED_CONTENT_TYPES) {
                return@withContext Result.failure(IllegalStateException("the recitation is not a zip"))
            }
            val maxBytes = if (packageToFetch.bytes > 0) {
                packageToFetch.bytes
            } else {
                MAX_RECITATION_BYTES
            }
            val contentLength = connection.contentLengthLong
            if (contentLength >= 0 && contentLength > maxBytes) {
                return@withContext Result.failure(
                    IllegalStateException("the recitation is larger than the manifest says"),
                )
            }
            val total = if (packageToFetch.bytes > 0) packageToFetch.bytes else contentLength
            val digest = MessageDigest.getInstance("SHA-256")
            val deadline = System.nanoTime() + MAX_DOWNLOAD_MILLIS * 1_000_000L
            var read = 0L
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        // The reader's Cancel, read between chunks. A chunk is
                        // 64 KB, so the transfer stops within one read of it.
                        coroutineContext.ensureActive()
                        if (System.nanoTime() > deadline) {
                            return@withContext Result.failure(
                                IllegalStateException("the recitation took too long"),
                            )
                        }
                        val count = input.read(buffer)
                        if (count < 0) break
                        read += count
                        if (read > maxBytes) {
                            return@withContext Result.failure(
                                IllegalStateException("the recitation is larger than the manifest says"),
                            )
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        onProgress(read, total)
                    }
                }
            }
            connection.disconnect()
            connection = null
            if (packageToFetch.bytes > 0 && read != packageToFetch.bytes) {
                return@withContext Result.failure(
                    IllegalStateException("the recitation ended at $read bytes"),
                )
            }
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
        if (!SafePaths.validFolder(folder) || surah !in 1..114) {
            throw IllegalStateException("the surah folder is not a safe name")
        }
        val imported = File(context.filesDir, "recitations")
        val target = SafePaths.childUnder(imported, folder)
            ?: throw IllegalStateException("the surah folder is not a safe name")
        val staging = File(imported, STAGING)
        staging.mkdirs()
        staging.listFiles()?.forEach { it.deleteRecursively() }
        val unpacking = File(staging, "$folder-$surah").apply { mkdirs() }
        var entries = 0
        var total = 0L
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                coroutineContext.ensureActive()
                val entry = input.nextEntry ?: break
                entries++
                if (entries > MAX_ENTRIES) {
                    input.closeEntry()
                    throw IllegalStateException("the package holds too many files")
                }
                val name = entry.name
                // Packages hold flat ayah files; a directory or a name that
                // is not one is skipped. The canonical check is the second
                // gate, so a future package shape cannot escape the folder.
                if (entry.isDirectory || !AYAH_FILE.matches(name)) {
                    input.closeEntry()
                    continue
                }
                val file = SafePaths.childUnder(unpacking, name)
                    ?: throw IllegalStateException("the package names an unsafe file")
                var fileBytes = 0L
                file.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        fileBytes += count
                        total += count
                        if (fileBytes > MAX_FILE_BYTES || total > MAX_UNCOMPRESSED_BYTES) {
                            throw IllegalStateException("the package is larger than a surah")
                        }
                        output.write(buffer, 0, count)
                    }
                }
                input.closeEntry()
            }
        }
        coroutineContext.ensureActive()
        if (unpacking.listFiles().orEmpty().isEmpty()) {
            throw IllegalStateException("the package held no ayah files")
        }
        // The folder rename is the commit. Moving the files one by one left
        // a surah that the app counted as downloaded after a process death
        // halfway through the list. A directory rename is one filesystem
        // operation, and the old folder is kept until the new one is in
        // place, so a failed commit restores the previous surah.
        val landing = SafePaths.childUnder(imported, "$folder-$surah.new")
            ?: throw IllegalStateException("the surah landing is not a safe name")
        val backup = SafePaths.childUnder(imported, "$folder-$surah.old")
            ?: throw IllegalStateException("the surah backup is not a safe name")
        if (landing.exists()) landing.deleteRecursively()
        if (!target.exists() && backup.exists()) backup.renameTo(target)
        if (!unpacking.renameTo(landing)) {
            throw IllegalStateException("the surah folder could not be completed")
        }
        if (target.exists()) {
            if (backup.exists()) backup.deleteRecursively()
            if (!target.renameTo(backup)) {
                landing.renameTo(unpacking)
                throw IllegalStateException("the previous surah folder could not be moved aside")
            }
        }
        if (!landing.renameTo(target)) {
            if (backup.exists()) backup.renameTo(target)
            throw IllegalStateException("the surah folder could not be committed")
        }
        backup.deleteRecursively()
        unpacking.deleteRecursively()
        staging.delete()
        Unit
    }

    private companion object {
        /** Where a package is read before it becomes ayah files. */
        const val STAGING = ".staging"

        /** One unpack at a time, so the staging root can be swept whole. */
        val unpackGate = Mutex()

        /** One ayah file, flat under its reciter folder. */
        val AYAH_FILE = Regex("""^[0-9]{6}\.mp3\z""")

        /** A surah package larger than this is not one the manifest offers. */
        const val MAX_RECITATION_BYTES = 512L * 1024 * 1024

        const val MAX_UNCOMPRESSED_BYTES = 512L * 1024 * 1024

        const val MAX_FILE_BYTES = 256L * 1024 * 1024

        const val MAX_ENTRIES = 10_000

        const val MAX_DOWNLOAD_MILLIS = 30L * 60 * 1000

        val ALLOWED_CONTENT_TYPES = setOf(
            "application/octet-stream",
            "binary/octet-stream",
            "application/zip",
            "application/x-zip-compressed",
        )
    }
}
