package io.github.muntasimulhaque.quran.playback

import io.github.muntasimulhaque.quran.data.DownloadError

/**
 * One reciter a reader could hear, with what that reciter would cost for the
 * surah at hand: the word timings and the surah's audio together, in bytes.
 */
data class ListenOption(
    val reciter: String,
    val name: String,
    val bytes: Long,
    /**
     * The same voice's Bangla name, from the catalog; empty when it has none.
     * It rides beside the English name because the pill and the offer are
     * built away from the catalog, and the list picks at draw time so a
     * language change is answered without rebuilding the pill
     * (owner decision, 20.2).
     */
    val nameBn: String = "",
)

/**
 * An audio request waiting for the reader's word. Listening needs two things
 * that may both be missing, the reciter's word timings and the surah's audio,
 * and they are offered as one: one name, one size, one download, one progress
 * bar. The reciter is changeable right here, because the reader who is about
 * to spend a megabyte should be able to spend it on the reciter they actually
 * want to hear (owner decision, 16.2).
 */
data class ListenOffer(
    val reciter: String,
    val reciterName: String,
    /**
     * The same voice's Bangla name, read from the catalog when the offer is
     * built. The pill and the offer are built away from the catalog and
     * survive a language change, so the pair travels and the surface picks,
     * rather than a name frozen in the language that made the offer
     * (owner decision, 20.2).
     */
    val reciterNameBn: String = "",
    val surah: Int,
    val surahName: String,
    val ayah: Int,
    val bytes: Long,
    /**
     * The voices the offer's own chooser lists, with what each would still
     * need for this surah. Read for this surah and not for the playing pill's,
     * because the offer may be about a surah the player has never seen.
     */
    val options: List<ListenOption> = emptyList(),
    /** 0..1 while downloading, null when waiting for the reader. */
    val progress: Float? = null,
    val failed: Boolean = false,
    /** Why the transfer failed, when it did. */
    val failure: DownloadError? = null,
)
