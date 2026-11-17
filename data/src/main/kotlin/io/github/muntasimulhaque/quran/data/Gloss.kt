package io.github.muntasimulhaque.quran.data

/**
 * A word list's gloss, or null when the row carries no meaning.
 *
 * The Bengali word list splits a sentence across its words, so a row can be a
 * lone quote or a parenthesis: punctuation that belongs to the word beside
 * it. A gloss with no letter is not a meaning, and the sheet says nothing
 * rather than printing a quote under a word (owner report, 19.7). No byte of
 * the source is changed; this is the rule the app draws by.
 */
internal fun meaningfulGloss(raw: String?): String? {
    val trimmed = raw?.trim() ?: return null
    if (trimmed.isEmpty()) return null
    return trimmed.takeIf { gloss -> gloss.any(Char::isLetter) }
}
