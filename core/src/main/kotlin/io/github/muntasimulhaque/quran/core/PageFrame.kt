package io.github.muntasimulhaque.quran.core

import kotlin.math.roundToInt

/**
 * The rectangle a Mushaf page's rule stands in, and the page's own
 * proportions, in ems of the page's own text.
 *
 * The rule is the page's furniture (D-122) and it is also what gives the sheet
 * of paper an edge on a wide ground, where the page and the app's ground are
 * the same tone. It is one rectangle with one margin on all four sides, and
 * the margin *inside* it is one number, [AIR_EM], measured from the text's ink
 * and not from its slots: these fonts carry their marks above the letters, so
 * a line's ink reaches further up than its slot says, and a frame ruled off
 * the slots stands on the marks.
 *
 * Before this the four margins were four numbers. The sides were a share of
 * the page's width (1.4 percent), the head was the top of the first slot, and
 * the foot was a whole band away: on the page in that report the rule stood
 * 5 dp from the glyphs at the sides, 4 dp above them, and 11 dp below the last
 * line, so one rectangle read as a wire pressed against the text at the top
 * and left and as a page at the foot (owner report, the forty-fourth session).
 * The measure gives up the two percent of the page's width it takes to hold
 * the rule where a page holds it, which is 2 percent of the mushaf's own type
 * and nothing else, and the paper outside the rule is the margin that is left
 * over, so the rule is the same distance inside the page on every side.
 *
 * Everything is in ems because the page is rendered at the width it will be
 * shown at: a share of the width is the same gap on a phone and on a tablet,
 * and a number of pixels would be a hairline on one and a moat on the other.
 */
class PageFrame private constructor(
    /** The rule's own left edge, in page pixels. */
    val left: Float,
    /** The rule's own right edge, in page pixels. */
    val right: Float,
    /** The rule's head, in page pixels from the page's top edge. */
    val head: Float,
    /** The rule's foot, in page pixels from the page's top edge. */
    val foot: Float,
    /** Where the page's first line begins: the top of its own slot. */
    val slotTop: Float,
    /** The page's own height, in pixels, which its aspect is measured against. */
    val heightPx: Int,
) {
    companion object {
        /** A full line of the page's glyphs sums to this many em. */
        const val EM_PER_LINE = 15.6f

        /** The pitch of two lines, as a share of the em. */
        const val LINE_HEIGHT_RATIO = 1.644f

        /** The lines of the page. */
        const val LINES = 15

        /**
         * The measure, as a share of the page's own width: what is left over
         * is the page's margin, and [AIR_EM] of it is the air inside the rule.
         */
        const val TEXT_WIDTH_RATIO = 0.88f

        /**
         * The air between the rule and the text, in ems, on all four sides.
         * It is about the height of the marks a line carries above its
         * letters, so the rule clears the tallest ink rather than touching
         * it, and a line under it is not read as a rule through the text.
         */
        const val AIR_EM = 0.75f

        /**
         * The room under the foot's rule, where the page number and the juz
         * sit: the roundel is two of its radii across and keeps a third of an
         * em of paper above and below itself.
         */
        const val FOOT_EM = 1.38f

        /** The page number's roundel, as a share of the em, as a radius. */
        const val ROUNDEL_EM = 0.34f

        /**
         * The tallest ink any of the 604 page fonts carries, in ems, which is
         * what the pager reserves room for: the tall fonts measure 1.8, and a
         * future pack is given a tenth of an em of slack over that.
         */
        const val INK_EM_TALLEST = 2.0f

        /**
         * The frame of a page [pageWidthPx] pixels wide whose lines carry
         * [inkEm] of ink above and below their baseline, which is the font's
         * own ascent plus its descent in ems.
         */
        fun of(pageWidthPx: Int, inkEm: Float): PageFrame {
            val em = em(pageWidthPx)
            val line = em * LINE_HEIGHT_RATIO
            val air = em * AIR_EM
            // The rule stands the air inside the measure, and whatever margin
            // is left outside it is the same margin at the head, so the frame
            // is one rectangle drawn inside the page on every side.
            val margin = (pageWidthPx - pageWidthPx * TEXT_WIDTH_RATIO) / 2f - air
            val ink = inkEm * em
            val halfLeading = (line - ink) / 2f
            val head = margin
            val slotTop = head + air - halfLeading
            val foot = head + air + (LINES - 1) * line + ink + air
            return PageFrame(
                left = margin,
                right = pageWidthPx - margin,
                head = head,
                foot = foot,
                slotTop = slotTop,
                heightPx = (foot + FOOT_EM * em).roundToInt().coerceAtLeast(1),
            )
        }

        /** The page's own em in pixels: the measure at a page of [pageWidthPx]. */
        fun em(pageWidthPx: Int): Float = pageWidthPx * TEXT_WIDTH_RATIO / EM_PER_LINE

        /**
         * How tall a page of this geometry is, as a share of its width, for
         * lines carrying [inkEm] of ink. The pager reads this before it has
         * chosen a page and so before it knows the font, and reserves
         * [aspect] of [INK_EM_TALLEST]: a page is then never taller than the
         * room it was given, on any font in the pack.
         */
        fun aspect(inkEm: Float): Float {
            val widthEm = EM_PER_LINE / TEXT_WIDTH_RATIO
            val heightEm = widthEm * (1f - TEXT_WIDTH_RATIO) / 2f + AIR_EM +
                (LINES - 1) * LINE_HEIGHT_RATIO + inkEm + FOOT_EM
            return heightEm / widthEm
        }
    }
}