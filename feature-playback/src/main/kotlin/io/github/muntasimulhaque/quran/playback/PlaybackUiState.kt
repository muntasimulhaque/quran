package io.github.muntasimulhaque.quran.playback

import io.github.muntasimulhaque.quran.data.DownloadError

data class PlaybackUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val recitation: String? = null,
    val ayahNumber: Int? = null,
    val surah: Int? = null,
    /**
     * The playing ayah's key, surah:ayah, for the pill's own line. It is the
     * app's reference in its data form (2:255), not the notification's
     * sentence; the pill localizes its digits where it draws them.
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
    /** 0..1 while downloading, null when idle. */
    val downloadProgress: Float? = null,
    val downloadFailed: Boolean = false,
    /** Why the download failed, when it did; null while it still might succeed. */
    val downloadFailure: DownloadError? = null,
    /** True when the player itself could not play: a missing file, a dead service. */
    val error: Boolean = false,
)
