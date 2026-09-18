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
    /** True when the chosen recitation's audio is not on this device. */
    val unavailable: Boolean = false,
)
