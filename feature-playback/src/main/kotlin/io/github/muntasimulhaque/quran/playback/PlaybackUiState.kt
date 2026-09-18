package io.github.muntasimulhaque.quran.playback

data class PlaybackUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val recitation: String? = null,
    val ayahNumber: Int? = null,
    val surah: Int? = null,
    /** The reference of the playing ayah, as its media metadata carries it. */
    val reference: String? = null,
    /** The word being recited, as the word table numbers it; null when unknown. */
    val wordPosition: Int? = null,
    val positionMs: Long = 0,
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
)
