package io.github.muntasimulhaque.quran.core

/**
 * One ayah as words, for the share text a message is sent as.
 *
 * Plain text carries no alignment of its own. A bidi-aware receiver reads a
 * message's direction off its first strong character, and an ayah that opens
 * with Arabic turns every line after it to the right: the translation ends
 * at the right edge, and the final period, a neutral character at the line's
 * end, is swept to the left of the sentence it closes. One U+200E at the
 * very start makes the first strong character Latin, so the block reads left
 * to right and every punctuation mark stays where it was written, while the
 * Arabic itself still shapes from right to left inside its own line (owner
 * report, D-103).
 */
object ShareText {

    /**
     * U+200E, the left-to-right mark: invisible, strong, and the whole of
     * the direction fix. It is written once, as the text's first character;
     * a second copy would say nothing the first does not.
     */
    const val LTR_MARK = '\u200E'

    /**
     * The ayah, its translation, and its reference, one blank line apart. A
     * missing translation is left out rather than drawn as an empty
     * paragraph.
     */
    fun ayah(arabic: String, translation: String?, reference: String): String = buildString {
        append(LTR_MARK)
        append(arabic)
        translation?.takeIf { it.isNotBlank() }?.let {
            append("\n\n")
            append(it)
        }
        append("\n\n")
        append(reference)
    }
}
