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
import java.util.zip.ZipInputStream

/**
 * Downloads one recitation package and unpacks it into the app's own storage.
 *
 * The file is streamed to a stable part name while its SHA-256 is computed,
 * and it is only unpacked when the hash matches the manifest. The part is
 * kept across a dropped connection or a process death, so the next attempt
 * asks for the rest instead of the whole package (owner decision, 23.1). A
 * completed package is deleted after it is unpacked; a cancelled unpack keeps
 * it, because the bytes were already verified. This is the app's only network
 * use, and it only happens when the reader asks for a surah.
 *
 * A cancel is the reader's own word, so it has to stop the transfer itself.
 * The read loop below is blocking, and a blocking read is not a suspension
 * point: cancelling the caller's job did not end it, the loop ran to its end,
 * verified its hash, and unpacked the surah the reader had just dropped
 * (owner report, 9.3). The job's own state is therefore read with
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
            packageToFetch.bytes <= 0 ||
            !SafePaths.validFolder(folder)
        ) {
            return@withContext Result.failure(
                IllegalStateException("the recitation package has no valid identity"),
            )
        }
        val part = DownloadParts.part(
            context,
            "recitation-${packageToFetch.recitation}-${packageToFetch.surah}.zip",
        ) ?: return@withContext Result.failure(
            IllegalStateException("the recitation part could not be created"),
        )
        val fetched = ResumableDownload.download(
            part = part,
            spec = ResumableDownload.Spec(
                url = packageToFetch.url,
                bytes = packageToFetch.bytes,
                sha256 = packageToFetch.sha256,
                maxBytes = MAX_RECITATION_BYTES,
                maxMillis = MAX_DOWNLOAD_MILLIS,
            ),
            onProgress = onProgress,
        )
        if (fetched.isFailure) {
            return@withContext Result.failure(
                fetched.exceptionOrNull() ?: IllegalStateException("the recitation did not arrive"),
            )
        }
        // The hash is checked before anything becomes an ayah file, and the
        // job is checked again here: a cancel that landed during the last
        // chunk must not unpack what it cancelled.
        coroutineContext.ensureActive()
        try {
            unpack(part, folder, packageToFetch.surah)
            part.delete()
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            // The part is verified, so a cancel during unpack keeps it for
            // the next tap rather than making the reader fetch it again.
            throw cancelled
        } catch (error: Exception) {
            // A package that cannot be read is not made better by keeping it.
            part.delete()
            Result.failure(error)
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
        // The reciter's folder holds every surah of that voice, so the
        // commit moves this surah's files into it; it never renames the
        // folder over the reciter's own, which deleted every surah already
        // downloaded (owner report, the audit).
        RecitationCommit.commit(imported, folder, unpacking)
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
