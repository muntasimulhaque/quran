package io.github.muntasimulhaque.quran.core

/**
 * The Mushaf page's own text size, and the one number it is built on.
 *
 * The page's measure is [EM_PER_LINE] ems of its text, and the printed page
 * justifies every line to fill it exactly. The typeface the page is drawn
 * with, the KFGQPC Uthmanic Hafs face, sets its words wider than the print's
 * own calligraphy does: measured over all 8,820 lines of the Book, a line of
 * the print sums to a median of 19.4 of the face's ems, its fullest line to
 * 28.3, where the print's line holds 15.6. So the same words set at the
 * print's own em no longer fit their lines, and one of two things has to
 * give: the text is set a little smaller than the print's, or the page runs
 * past fifteen lines.
 *
 * [DEFAULT_SCALE] is the answer the page is built on: the size at which the
 * median line of the Book fills the measure as the print fills it, and at
 * which every page in the Book still stands on its own fifteen lines within
 * the room the pager gives it. The reader's own steps multiply it, so the
 * step the reader reads as their usual size is the page as the print stands,
 * and the larger steps re-set the page's lines and the reader pans the
 * taller page, because a page that is clipped is a page whose last lines
 * cannot be read or touched.
 *
 * The number was measured with the face itself over every line of the Book,
 * and it is pinned by a gate (tools fonts) that re-measures it: a face whose
 * metrics move moves this number with it, by decision and not by accident.
 */
object MushafText {

    /**
     * The page's text at the reader's usual step, as a share of the
     * measure's own em. The median line of the Book is 19.4 of the face's
     * ems against the print's 15.6, and 15.6 over 19.4 is 0.80: the extra
     * slack under 0.80 is the page's own furniture and the fullest lines,
     * because a page is never set from its median line alone.
     */
    const val DEFAULT_SCALE = 0.72f

    /**
     * The size of the page's text for one of the reader's steps, as a share
     * of the measure's em. The steps are the five every sized text uses; the
     * page's own stand is [DEFAULT_SCALE].
     */
    fun scale(step: Float): Float = step * DEFAULT_SCALE

    /**
     * What a line of the page holds at that size, in ems of the text: the
     * measure divided by the size, because the measure is the page's and the
     * text is the reader's.
     */
    fun capacityEm(step: Float): Float = PageFrame.EM_PER_LINE / scale(step)
}
