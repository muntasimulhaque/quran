package io.github.muntasimulhaque.quran.ui.reading

/**
 * What a search result asks the reading to reveal when it lands.
 *
 * The reveal is transient: it is not a setting, it does not change the
 * reader's saved choices, and the reading clears it on the next touch or
 * after a short while. It exists for the one moment a search result becomes a
 * verse on the page.
 */
data class ReadingReveal(
    /** The ayah the reveal belongs to. */
    val ayah: Int,
    /** The word positions the Arabic search matched. */
    val words: Set<Int> = emptySet(),
    /** The Arabic words whose meanings matched, compared by their text. */
    val meanings: Set<String> = emptySet(),
    /** True when the matched translation should be shown even if it is hidden. */
    val showTranslation: Boolean = false,
    /** True when the word-by-word aid should be shown for the arrival. */
    val wordByWord: Boolean = false,
)
