package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The room every surface keeps, named instead of typed: two surfaces that
 * carry the same kind of break leave the same room for it, so the app reads as
 * one hand from the reading to the settings.
 *
 * The reading draws no rules between its parts, so these gaps are the
 * separators: a word list under its ayah, a translation under the words, a
 * reference under the translation. A gap that carries meaning is chosen once
 * here rather than nudged at each call site.
 *
 * The ladder is vertical. The three numbers under it are the page: how far a
 * word stands from the edge of the glass, how wide the page it stands on is
 * allowed to be, and how far a wash reaches past its words.
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
     * one number: the share sheet's two doors, the time page's Cancel and Set,
     * the remove question, the note's Clear and Save, the reminder card's Not
     * now and Turn on, and the download band's Close and Retry. It was 12 dp,
     * and the time page's pair drew 20 (the platform lays its own action row 8
     * dp apart and the app added the 12 on top), which is the one the owner's
     * eye approved (owner decision, this session). The shapes are 36 dp tall
     * at the default type, so 20 dp between them reads as two answers; 12 read
     * as one crowded pair, and a pair that changes width with a platform
     * constant is not a decision at all. The order of the pair is the button's
     * own rule: the quiet word leads and the filled answer trails.
     */
    val Action = 20.dp

    /** Two thoughts in one block: the ayah and its word list. */
    val Block = 16.dp

    /**
     * Two blocks: one ayah and the next, a page and its heading, and every
     * fact on the word meaning sheet, each a labeled block of its own.
     */
    val Section = 24.dp

    /** The room a sheet leaves under its last row. */
    val SheetFoot = 28.dp

    /**
     * The least air between a word and the edge of the glass.
     *
     * The reading's verse, its translation, the word list under it, a search
     * result, a settings row, a footnote, a tafsir passage, and the reminder
     * offer all begin on this line and end on its mirror.
     *
     * It is the floor, not the width. A glass narrow enough to hold a page
     * takes this much on each side and no more, which is every phone and the
     * seven inch tablet standing up, so a line runs as far as its paper
     * allows. A glass wider than a page keeps this much air inside the page
     * and puts the rest of its width outside it, and that half of the rule
     * belongs to [Measure] (owner decision, an earlier session).
     *
     * It was 28, and 28 was a page's own air rather than the app's: the
     * platform's own compact margin is 16, so every phone ran 12 dp inside
     * its own paper at every edge, from the search field's head to the last
     * footnote. On the store's phone the line then held about 44 characters
     * of the translation at the shipped size, under the 45 to 75 a line of
     * reading is drawn for, and the sheet's words stood so far in from the
     * sheet's edge that a tafsir read as a page inside a page. 20 is the cut
     * that leaves the paper's own strip: the wash of a chosen ayah still
     * stands 12 dp from the glass, a 48 dp control still keeps 8, and every
     * line moved into the range it is read in. The width of a page did not
     * move with it: [Measure] still holds a wide glass to 600 dp, so the ten
     * inch screen reads the line it read before and only a sheet's own edge
     * moved out (owner decision, this session: 28 to 20).
     *
     * A shape that answers a touch may reach outside the line (a row's press
     * card, the wash of a chosen ayah), and a card holds its own words inside
     * its own padding. The words that stand on the page are these.
     */
    val Gutter = 20.dp

    /**
     * The widest a line of reading may run: the page's measure.
     *
     * A window holding no more than this is the page. Its words stand
     * [Gutter] inside the glass and the line runs to the other side, which is
     * every phone and the seven inch tablet in its portrait. A window wider
     * than a page is a page on a desk: the words keep this width, centered,
     * and the glass keeps what is left as margin, so the ten inch screen reads
     * the page the seven inch screen already reads.
     *
     * 600 is the width Android itself calls a tablet (`sw600dp`), the width at
     * which a phone's window stops being a phone's window, and it is also,
     * today, the seven inch tablet's own page: edge to edge at 600. So the
     * number is the platform's, not a guess, and it changes no screen but the
     * ones that had grown past it.
     *
     * It is a length of paper and never a count of characters. The app used to
     * stop every column at 65 characters, and a line that shortened when a
     * reader turned the text down is the shape of that mistake (owner
     * decision: the character measure stays gone). At 600 dp the five text
     * sizes change how many words a line holds and never how wide the page is,
     * which is what a printed page does (owner decision, this session).
     */
    val Measure = 600.dp

    /**
     * How far the wash of a chosen or playing ayah reaches past its words.
     *
     * A tint whose edge fell on the letters would read as a highlight painted
     * on the Quran rather than as the paper under it, so the reading's list
     * keeps its page's margin less this on each side and the wash reaches this
     * far past the words it stands behind.
     */
    val WashInset = 8.dp
}
