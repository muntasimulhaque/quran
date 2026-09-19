package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The vertical rhythm every surface keeps, named instead of typed: two
 * surfaces that carry the same kind of break leave the same room for it, so
 * the app reads as one hand from the reading to the settings.
 *
 * The reading draws no rules between its parts, so these gaps are the
 * separators: a word list under its ayah, a translation under the words, a
 * reference under the translation. A gap that carries meaning is chosen once
 * here rather than nudged at each call site.
 */
object Space {

    /** A label and the thing it names. */
    val Tight = 4.dp

    /** Two lines of one thought. */
    val Line = 8.dp

    /** Two thoughts in one block: the ayah and its word list. */
    val Block = 16.dp

    /** Two blocks: one ayah and the next, a page and its heading. */
    val Section = 24.dp
}
