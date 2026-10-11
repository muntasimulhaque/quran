package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 */
object Reading {

    /** The widest a line of reading may be. */
    private const val MAX_CHARACTERS = 65

    /** The gutter a sheet keeps beside its reading column, on each side. */
    val Gutter: Dp = 22.dp

    /** The default prose measure, for a surface with no size of its own. */
    val MaxMeasure: Dp = measureFor(17f, LITERATA_ADVANCE)

    /** The cap for a sheet's own content: the measure plus its two gutters. */
    val SheetMeasure: Dp = MaxMeasure + Gutter * 2

    /**
     * The search results' cap. Their snippets are drawn in the interface
     * face at 14 sp, so that is the advance the number comes from.
     */
    val SearchMeasure: Dp = measureFor(14f, INTERFACE_ADVANCE) + Gutter * 2

    /**
     * The width a column of prose drawn at [sizeSp] may have in a voice whose
     * average advance is [advance], in em.
     */
    fun measureFor(sizeSp: Float, advance: Float): Dp = (MAX_CHARACTERS * advance * sizeSp).dp

    /** The measure in the reader's own reading voice. */
    fun proseMeasure(voice: ReadingVoice, sizeSp: Float): Dp = measureFor(sizeSp, voice.advance)

    /** The measure of the Quran's own line, drawn in the Hafs face. */
    fun quranMeasure(arabicSp: Float): Dp = measureFor(arabicSp, QURAN_ADVANCE)

    // Measured over the shipped text; see the class KDoc.
    private const val LITERATA_ADVANCE = 0.471f
    private const val INTERFACE_ADVANCE = 0.471f
    private const val QURAN_ADVANCE = 0.246f
}
