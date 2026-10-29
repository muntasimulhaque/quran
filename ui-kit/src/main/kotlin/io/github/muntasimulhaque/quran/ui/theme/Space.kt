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

    /** One row's own head and foot: the room a list row keeps around its lines. */
    val Row = 12.dp

    /**
     * Two actions side by side: the room between the shapes the reader sees.
     *
     * The gap is measured pill to pill, not target to target, because the two
     * answers are read as two shapes and the invisible 48 dp each one keeps is
     * not part of what the eye separates. Every pair in the app stands on this
     * one number: the share card's two doors, the reminder dialog's Set and
     * Cancel, the remove question, the note's Clear and Save, the pill's Retry
     * and Close. It was 12 dp, and the reminder dialog's pair drew 20 (the
     * platform lays its own action row 8 dp apart and the app added the 12 on
     * top), which is the one the owner's eye approved (owner decision, this
     * session). The shapes are about 33 dp tall, so 20 dp between them reads as
     * two answers; 12 read as one crowded pair, and a pair that changes width
     * with a platform constant is not a decision at all.
     */
    val Action = 20.dp

    /** Two thoughts in one block: the ayah and its word list. */
    val Block = 16.dp

    /** Two blocks: one ayah and the next, a page and its heading. */
    val Section = 24.dp

    /** The room a sheet leaves under its last row. */
    val SheetFoot = 28.dp

    /**
     * The one edge every word stops at, on every surface and every glass.
     *
     * The reading's verse, its translation, the word list under it, a search
     * result, a settings row, a footnote, a tafsir passage, and the welcome
     * screen all begin on this line and end on its mirror, whatever the
     * window is. The number does not look at the glass, so the seven inch
     * tablet and the ten inch screen read the page the phone reads, and a
     * line runs as far as the paper allows, which is what a printed page does
     * with the paper it has. What a line of reading does not do is become as
     * long as a count of characters decided it would be: the 65 character
     * measure that used to cap every column in the app is gone, and this is
     * the one rule that decides where a line stops (owner decision, this
     * session).
     *
     * A shape that answers a touch may reach outside the line (a row's press
     * card, the wash of a chosen ayah), and a card holds its own words inside
     * its own padding. The words that stand on the page are these.
     */
    val Gutter = 28.dp

    /**
     * How far the wash of a chosen or playing ayah reaches past its words.
     *
     * A tint whose edge fell on the letters would read as a highlight painted
     * on the Quran rather than as the paper under it, so the reading's list
     * keeps [Gutter] less this on each side and the wash reaches this far past
     * the words it stands behind.
     */
    val WashInset = 8.dp
}
