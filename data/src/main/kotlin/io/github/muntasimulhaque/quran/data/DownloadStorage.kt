package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.os.StatFs

/**
 * Whether the next download fits.
 *
 * A reader can hold several translations, two tafsirs, a word list, two
 * reciters' surahs, and a pronunciation pack. A download that needed 186 MB
 * could start on a phone with 100 MB left and fail only at the end, so the
 * check happens before the first byte and the row that offered the transfer
 * says the answer (owner decision, 23.1).
 *
 * **The settings hub keeps no storage page** (owner decision, 23.4). A page
 * that added the library up could not free a byte, and every act that takes a
 * download back already stands where the download is named: a pack on its own
 * page, a surah on its reciter's list, a pronunciation surah on the words
 * page. The one thing the page could do that no row can was to clear a whole
 * library in one tap, and a reader who filled the phone knows the reciter they
 * filled it with. The sum, the free-space number, the partial-transfer
 * reading, and the two "remove all" doors left with the page.
 */
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
}
