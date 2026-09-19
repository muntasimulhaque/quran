package io.github.muntasimulhaque.quran.playback

/**
 * One reciter a reader could hear, with what that reciter would cost for the
 * surah at hand: the word timings and the surah's audio together, in bytes.
 */
data class ListenOption(
    val reciter: String,
    val name: String,
    val bytes: Long,
)

/**
 * An audio request waiting for the reader's word. Listening needs two things
 * that may both be missing, the reciter's word timings and the surah's audio,
 * and they are offered as one: one name, one size, one download, one progress
 * bar, and the reciter is changeable right here, because the reader who is
 * about to spend a megabyte should be able to spend it on the reciter they
 * actually want to hear.
 */
data class ListenOffer(
    val reciter: String,
    val reciterName: String,
    val surah: Int,
    val surahName: String,
    val ayah: Int,
    val bytes: Long,
    val options: List<ListenOption>,
    /** 0..1 while downloading, null when waiting for the reader. */
    val progress: Float? = null,
    val failed: Boolean = false,
)
