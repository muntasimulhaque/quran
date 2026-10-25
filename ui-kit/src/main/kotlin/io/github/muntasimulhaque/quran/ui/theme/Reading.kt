package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ReadingMetrics

/**
 * How long a line of reading is allowed to grow.
 *
 * A line of text has a length at which the eye stops returning to the margin
 * on its own. The measure is therefore counted in characters, not in pixels:
 * a column of reading is capped at [MAX_CHARACTERS], and the width that number
 * comes to depends on the size the text is drawn at and on the average
 * advance of the face drawing it. A reader who turns the text down gets more
 * lines on the screen, never a longer line, and the smallest step reads as
 * easily as the largest (owner decision, the audio and type audit session).
 *
 * Before this, one fixed 620 dp cap served every size and every voice: a
 * tablet line ran about 77 characters at the default size and about 119 at
 * the smallest, and the ayah card's cap never took effect at all, running a
 * tafsir line to about 150 characters across a ten inch screen.
 *
 * The advances are measured over the shipped text, not guessed: Literata sets
 * Saheeh International at 0.471 em per character, the Hafs face sets the
 * Quran at 0.246 em, and the interface face at its 14 sp search size at
 * 0.471 em. A face or a reading size whose advance has not been measured
 * cannot have its column trusted; measure it first.
 *
 * The sheets are the app's paper: one page, [SheetMeasure], on every surface
 * and every form factor, so the search sheet and the settings sheet stand on
 * the same column. [sheetWidth] is the one rule that decides when the page
 * gives way to the window it is drawn in.
 */
object Reading {

    /** The widest a line of reading may be. */
    private const val MAX_CHARACTERS = 65

    /** The default prose measure, for a surface with no size of its own. */
    val MaxMeasure: Dp = measureFor(17f, LITERATA_ADVANCE)

    /**
     * The one page every sheet takes: the measure plus its two gutters.
     *
     * Search, Browse, Settings, the ayah card, the note, the footnote, the
     * share, and the remove question all stand on this column, so a reader
     * meets the same page from the reading to the settings (owner decision,
     * this session). A sheet whose own voice is not the 17 sp the number
     * comes from keeps its own measure inside this page; the search results
     * are the one case today.
     */
    val SheetMeasure: Dp = MaxMeasure + Space.Gutter * 2

    /**
     * The search results' own column inside the one page. Their snippets are
     * drawn in the interface face at 14 sp, so that is the advance the number
     * comes from, not the page's own.
     */
    val SearchMeasure: Dp = measureFor(14f, INTERFACE_ADVANCE) + Space.Gutter * 2

    /**
     * The width a sheet takes on a window of [windowWidth].
     *
     * Every sheet is the app's one page, [SheetMeasure]. A page that would
     * leave a sliver of margin (under [Space.Section] a side) is not read as
     * a page with air around it but as a page with a mistake beside it, so a
     * window that close to the page takes the whole window instead: the
     * seven inch tablet fills edge to edge, and the ten inch screen keeps
     * the page centered the way the reading keeps its column (owner
     * decision, this session).
     */
    fun sheetWidth(windowWidth: Dp): Dp =
        if (windowWidth - SheetMeasure < Space.Section * 2) windowWidth else SheetMeasure

    /**
     * The width a column of prose drawn at [sizeSp] may have in a voice whose
     * average advance is [advance], in em.
     */
    fun measureFor(sizeSp: Float, advance: Float): Dp = (MAX_CHARACTERS * advance * sizeSp).dp

    /** The measure in the reader's own reading voice. */
    fun proseMeasure(voice: ReadingVoice, sizeSp: Float): Dp = measureFor(sizeSp, voice.advance)

    /** The measure of the Quran's own line, drawn in the Hafs face. */
    fun quranMeasure(arabicSp: Float): Dp = measureFor(arabicSp, QURAN_ADVANCE)

    /**
     * The one column the reading is drawn in: the narrower of the prose's
     * measure and the Quran's, so neither text runs past a line the eye can
     * follow back to its margin on its own.
     *
     * It lives here because two surfaces need the same answer: the reading
     * that sets the column, and the chrome that stands at its edges on a
     * screen wider than the page. A second copy of the minimum would be a
     * second column the first one could drift away from (owner decision, this
     * session).
     */
    fun column(voice: ReadingVoice, metrics: ReadingMetrics): Dp = minOf(
        proseMeasure(voice, metrics.translationSp),
        quranMeasure(metrics.arabicSp),
    )

    // Measured over the shipped text; see the class KDoc.
    private const val LITERATA_ADVANCE = 0.471f
    private const val INTERFACE_ADVANCE = 0.471f
    private const val QURAN_ADVANCE = 0.246f
}
