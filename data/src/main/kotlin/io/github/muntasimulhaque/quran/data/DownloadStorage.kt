package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * What the downloads weigh, and whether the next one fits.
 *
 * A reader can hold several translations, two tafsirs, a word list, two
 * reciters' surahs, and a pronunciation pack. The settings rows say what each
 * one weighs, but no surface ever added them up, and a download that needed
 * 186 MB could start on a phone with 100 MB left and fail only at the end.
 * The check happens before the first byte, and the storage page shows the
 * same arithmetic to the reader (owner decision, 23.1).
 */
data class StorageSummary(
    /** The Quran text and the content packs on the device. */
    val contentBytes: Long = 0,
    /** Every downloaded surah of every reciter. */
    val recitationBytes: Long = 0,
    /** Every downloaded word pronunciation surah. */
    val pronunciationBytes: Long = 0,
    /** Parts kept for a transfer that stopped and can resume. */
    val partialBytes: Long = 0,
    val freeBytes: Long = 0,
    val totalBytes: Long = 0,
) {
    /** Everything the app is holding on this phone, apart from free space. */
    val usedBytes: Long
        get() = contentBytes + recitationBytes + pronunciationBytes + partialBytes
}

object DownloadStorage {

    /** Room left over after a transfer, so the phone is not filled to the edge. */
    const val HEADROOM_BYTES: Long = 50L * 1024 * 1024

    /**
     * Whether the phone can hold this job.
     *
     * A pack is downloaded to a part and renamed over the target, so its peak
     * is one copy. A recitation or a word package is a ZIP that must also be
     * unpacked, so its peak is the package plus the files inside it. The check
     * asks for both copies before the transfer starts, which is the answer
     * that keeps a 186 MB surah from failing after the last byte.
     */
    fun hasRoom(context: Context, job: DownloadJob): Boolean {
        val needed = when (job) {
            is DownloadJob.Pack -> job.bytes + HEADROOM_BYTES
            is DownloadJob.Recitation,
            is DownloadJob.WordAudio,
            -> job.bytes * 2 + HEADROOM_BYTES
        }
        return freeBytes(context) >= needed
    }

    fun freeBytes(context: Context): Long = runCatching {
        StatFs(context.filesDir.absolutePath).availableBytes
    }.getOrDefault(0L)

    fun totalBytes(context: Context): Long = runCatching {
        StatFs(context.filesDir.absolutePath).totalBytes
    }.getOrDefault(0L)

    /**
     * The whole picture, read on a worker: the page that shows it is not the
     * reading, and the sums walk folders that can hold thousands of files.
     */
    fun summary(context: Context, installedPacks: Collection<ContentPack>): StorageSummary {
        val store = PackStore(context)
        val content = installedPacks.sumOf { pack ->
            if (pack.id == PackCatalog.CORE_ID) {
                coreBytes(context)
            } else {
                store.fileFor(pack.id).length()
            }
        }
        return StorageSummary(
            contentBytes = content,
            recitationBytes = folderBytes(File(context.filesDir, "recitations")),
            pronunciationBytes = folderBytes(File(context.filesDir, "word-audio")),
            partialBytes = DownloadParts.bytes(context),
            freeBytes = freeBytes(context),
            totalBytes = totalBytes(context),
        )
    }

    private fun coreBytes(context: Context): Long =
        folderBytes(File(context.filesDir, "content"))

    private fun folderBytes(root: File): Long {
        if (!root.isDirectory) return 0L
        return root.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
}
