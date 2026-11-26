package io.github.muntasimulhaque.quran.playback

import io.github.muntasimulhaque.quran.data.DownloadError

data class PlaybackUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    /**
     * The reader's pace, the same value the player was given. The reading
     * turns a media-time wait into wall time from it without asking the
     * player, so a scheduled word change stays on time at 0.5x and 1.5x.
     */
    val speed: Float = 1f,
    val recitation: String? = null,
    /**
     * The playing ayah's running number, 1 to 6236: the content database's
     * key and the reading's place. It is not the ayah's number inside its
     * surah (16:28 is running 1929); [ayahInSurah] is that one, and it is the
     * number a reader knows (owner report, 19.2).
     */
    val ayahNumber: Int? = null,
    val surah: Int? = null,
    /**
     * The playing ayah's number inside its surah, read from the ayah when the
     * playlist was built. The pill's key and the panel's spoken place are its
     * only readers; everything that compares ayahs compares [ayahNumber]
     * (owner report, 19.2).
     */
    val ayahInSurah: Int? = null,
    /**
     * The playing ayah's key, surah:ayah, for the pill's own line. It is the
     * app's reference in its data form (2:255), the verse's own number and
     * never the running one, and the pill localizes its digits where it draws
     * them.
     */
    val reference: String? = null,
    /**
     * The surah's name in the reader's language, read from the playlist the
     * media session was built with. The pill leads with it; the notification's
     * title keeps its own sentence.
     */
    val placeName: String? = null,
    /** True when the chosen recitation has no published package for this surah. */
    val unavailable: Boolean = false,
    /** The surah waiting for the reader's download approval. */
    val pendingDownloadSurah: Int? = null,
    val pendingDownloadBytes: Long = 0,
    /** True when the offer comes from the end of the previous surah. */
    val pendingIsContinuation: Boolean = false,
    /**
     * The reader asked for a package on mobile data, and the question is
     * still waiting for their answer. The transfer has not started; the
     * confirm sheet is the only thing between the tap and the first byte
     * (owner decision, 23.1).
     */
    val pendingMetered: Boolean = false,
    /** 0..1 while downloading, null when idle. */
    val downloadProgress: Float? = null,
    val downloadFailed: Boolean = false,
    /** Why the download failed, when it did; null while it still might succeed. */
    val downloadFailure: DownloadError? = null,
    /** True when the player itself could not play: a missing file, a dead service. */
    val error: Boolean = false,
    /**
     * The word whose own voice is sounding, or null while no sample is.
     *
     * A word is a sample, not a session: it is one word file on its own
     * player, it raises no capsule of its own, and it leaves no place to come
     * back to. The reading washes that word on the sheet while it sounds. A
     * recitation that was playing waits at the word and is given back when the
     * word ends; the sample itself takes nothing away from the session (owner
     * decision, 17.9).
     */
    val sample: WordSample? = null,
)

/** The one word the reader asked to hear: its ayah and its position in it. */
data class WordSample(
    val ayahNumber: Int,
    val position: Int,
)
