package io.github.muntasimulhaque.quran.ui.search

import io.github.muntasimulhaque.quran.data.Ayah

/**
 * What a search result asks the reading to show when the row opens.
 *
 * A row that matched a translation, a word meaning, or a tafsir passage has
 * evidence the reading should meet, not only a place to land. The arrival
 * carries the evidence so the reading can reveal it without changing the
 * reader's saved settings.
 */
sealed interface SearchArrival {
    /** The ayah the result lands on. */
    val ayah: Ayah

    /**
     * A verse hit: the words the Arabic matched, the words whose meanings
     * matched, and the translation pack that carried the match, when any.
     */
    data class AyahMatch(
        override val ayah: Ayah,
        val arabicWords: Set<Int> = emptySet(),
        val meaningWords: List<String> = emptyList(),
        val translationPack: String? = null,
    ) : SearchArrival

    /** A tafsir hit: the passage to open in the ayah card, by pack. */
    data class TafsirMatch(
        override val ayah: Ayah,
        val pack: String,
    ) : SearchArrival
}
