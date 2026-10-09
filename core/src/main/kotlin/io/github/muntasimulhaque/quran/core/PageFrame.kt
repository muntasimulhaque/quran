package io.github.muntasimulhaque.quran.core

import kotlin.math.roundToInt

/**
 * The rectangle a Mushaf page's rule stands in, and the page's own
 * proportions, in ems of the page's own measure.
 *
 * The measure is the page's and the type is the page's own too: it is the
 * largest at which that page's fullest line still stands inside the measure,
 * because a page whose line runs past the measure is a page whose words have
 * been re-set, and the arrangement of the Book is not the app's to re-set.
 * Every page therefore carries its own scale, [PageTextLayout] computes it
 * from the Book's own words, and [of] draws the page at it.
 *
 * Everything here is in ems of the measure because the page is rendered at
 * the width it will be shown at: a share of the width is the same gap on a
 * phone and on a tablet, and a number of pixels would be a hairline on one
 * and a moat on the other. The one number that is not the page's is the
 * pitch: it belongs to the glass, and it is the only thing about a page that
 * the glass decides, because a page of fifteen lines at the print's own type
 * is narrower than a phone is tall and must be opened out to fill it.
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
    /** The pitch of two lines, in page pixels. */
    val pitchPx: Float,
    /** The page's own height, in pixels, which its aspect is measured against. */
    val heightPx: Int,
) {
    companion object {
        /** A full line of the page's glyphs sums to this many em. */
        const val EM_PER_LINE = 15.6f

        /** The measure, as a share of the page's own width. */
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

        /** An ayah's own number in its roundel, as a share of the em, as a radius. */
        const val MARKER_EM = 0.30f

        /** The page number own roundel, as a share of the em, as a radius. */
        const val ROUNDEL_EM_PAGE = 0.34f

        /** The room an ayah roundel keeps inside itself for its number. */
        const val ROUNDEL_PAD_EM = 0.14f

        /** The digits inside an ayah's roundel, as a share of the em. */
        const val MARKER_DIGITS_EM = 0.32f

        /**
         * One Arabic-Indic digit of the ornament face, as a share of its em,
         * measured from the face itself: the roundel is as wide as the number
         * it carries, and a number the app draws is a number the app must
         * also reserve room for.
         */
        const val ORNAMENT_DIGIT_EM = 0.585f

        /**
         * The shipped text face's own ink above and below its baseline, in
         * ems of that face, measured from the face itself: its hhea ascent
         * and descent over its units per em.
         */
        const val INK_EM = 1.7578f

        /**
         * The tallest ink any face the page might be drawn with is allowed to
         * carry, in ems. The gate reads this, so a face whose metrics move
         * fails the build rather than drawing a page whose marks cross the
         * rule.
         */
        const val INK_EM_TALLEST = 2.0f

        /**
         * The pitch as a share of the type: the page's own leading. It is the
         * floor the pitch never goes under, and the glass raises it above it
         * to fill whatever room the fifteen lines are given.
         */
        const val LINE_HEIGHT_RATIO = 1.644f

        /** The lines of the page. */
        const val LINES = 15

        /** The page's own em in pixels: the measure at a page of [pageWidthPx]. */
        fun em(pageWidthPx: Int): Float = pageWidthPx * TEXT_WIDTH_RATIO / EM_PER_LINE

        /**
         * An ayah number's roundel, as wide as the drawer draws it, in ems of
         * the measure. The printed page ends an ayah with its number in a
         * small oval whose width is its own height or the width of its
         * digits, whichever is wider, and the engine reserves exactly that: a
         * flat reservation sets every three-digit number on the word before
         * it, which is a touch target in the wrong place.
         */
        fun roundelWidthEm(ayah: Int): Float {
            val digits = if (ayah <= 0) 1 else ayah.toString().length
            val digitsWidth = digits * ORNAMENT_DIGIT_EM * MARKER_DIGITS_EM
            return maxOf(2f * MARKER_EM, digitsWidth + 2f * ROUNDEL_PAD_EM)
        }

        /**
         * The widest a page of [lines] lines carrying [inkEm] of ink at
         * [scale] of the measure can be and still stand, at its own pitch,
         * inside a glass [glassHeightPx] tall.
         *
         * The page's height at its own pitch is a straight line in its width:
         * the margins, the air and the foot are shares of the width, and the
         * pitch and the ink are shares of the type, which is itself a share
         * of the width. So the width is solved rather than guessed, and every
         * page is drawn at the width its own words allow it.
         */
        fun widestPagePx(
            glassWidthPx: Float,
            glassHeightPx: Float,
            lines: Int,
            inkEm: Float,
            scale: Float,
        ): Int {
            val widthEm = EM_PER_LINE / TEXT_WIDTH_RATIO
            // Every share of the page's height is a share of its width: the
            // margin is, the air is, the foot is, and the pitch and the ink
            // are shares of the type, which is a share of the width too. The
            // air is counted once, because the margin gives it back.
            val perWidth = (1f - TEXT_WIDTH_RATIO) / 2f +
                TEXT_WIDTH_RATIO / EM_PER_LINE * (
                AIR_EM + (lines - 1) * scale * LINE_HEIGHT_RATIO + inkEm * scale + FOOT_EM
                )
            if (perWidth <= 0f) return glassWidthPx.roundToInt().coerceAtLeast(1)
            val solved = glassHeightPx / perWidth
            return minOf(glassWidthPx, solved).roundToInt().coerceAtLeast(1)
        }

        /**
         * How tall a page of this geometry is, as a share of its width, for
         * [lines] lines of [inkEm] at [scale] at a pitch of [pitchEm] ems.
         */
        fun aspect(lines: Int, inkEm: Float, scale: Float, pitchEm: Float): Float {
            val widthEm = EM_PER_LINE / TEXT_WIDTH_RATIO
            val heightEm = widthEm * (1f - TEXT_WIDTH_RATIO) / 2f +
                AIR_EM + (lines - 1) * pitchEm + inkEm * scale + FOOT_EM
            return heightEm / widthEm
        }

        /**
         * The frame of a page [pageWidthPx] wide, [lines] lines long, drawn at
         * [scale] of the measure with a pitch of [pitchEm] ems of the measure.
         *
         * [inkEm] is the face's own ink in its ems: the rule is measured from
         * the text's ink and not from its slots, because these faces carry
         * their marks above their letters and a frame ruled off the slots
         * stands on them.
         */
        fun of(
            pageWidthPx: Int,
            lines: Int,
            scale: Float,
            pitchEm: Float,
            inkEm: Float = INK_EM,
        ): PageFrame {
            val em = em(pageWidthPx)
            val pitch = pitchEm * em
            val air = em * AIR_EM
            // The rule stands the air inside the measure, and whatever margin
            // is left outside it is the same margin at the head, so the frame
            // is one rectangle drawn inside the page on every side.
            val margin = pageWidthPx * (1f - TEXT_WIDTH_RATIO) / 2f - air
            val ink = inkEm * em * scale
            val halfLeading = (pitch - ink) / 2f
            val head = margin
            val slotTop = head + air - halfLeading
            val foot = head + air + (lines - 1) * pitch + ink + air
            return PageFrame(
                left = margin,
                right = pageWidthPx - margin,
                head = head,
                foot = foot,
                slotTop = slotTop,
                pitchPx = pitch,
                heightPx = (foot + FOOT_EM * em).roundToInt().coerceAtLeast(1),
            )
        }
    }
}
