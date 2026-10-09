package io.github.muntasimulhaque.quran.core

/**
 * The page's own size on a given glass: how wide the page stands, how far
 * apart its lines stand, and the frame that holds them.
 *
 * A page of the Book is fifteen lines of the print's own type, and the print's
 * own type is set by the Book's fullest line, so a page of fifteen lines is
 * narrower than a phone is tall. Two answers exist and only one is the Book's:
 * draw the page small and float it on the glass, or open the page out until it
 * fills the glass. The second is what is done here, and the opening all goes
 * into the pitch, because the measure is the print's and the type is the
 * print's and the pitch is the one thing the app has any say in.
 *
 * So the width is solved: the widest the page can be and still stand its own
 * height inside the glass, at its own pitch. On a phone that is usually the
 * glass's own width and the pitch grows to fill the height; on a wide tablet
 * the height binds, the pitch is the page's own, and the paper stands around
 * the page the way a book stands around a page. Either way the page fills one
 * dimension of the glass exactly, and never the other one by clipping it.
 */
class PageGeometry private constructor(
    /** The page's own width in pixels, the width it is drawn at. */
    val widthPx: Int,
    /** The pitch of two lines, in ems of the measure. */
    val pitchEm: Float,
    /** The rule and the slots the page is drawn in. */
    val frame: PageFrame,
) {
    companion object {
        /**
         * The page of [lines] lines at [scale] of the measure, carrying [inkEm]
         * of ink, on a glass [glassWidthPx] by [glassHeightPx].
         *
         * The pitch is the page's own leading or whatever fills the glass,
         * whichever is larger: a page is never set tighter than its own
         * leading, because a line whose ink touches the line under it is not a
         * line of the Book.
         */
        fun of(
            glassWidthPx: Float,
            glassHeightPx: Float,
            lines: Int,
            scale: Float,
            inkEm: Float = PageFrame.INK_EM,
        ): PageGeometry {
            val widthPx = PageFrame.widestPagePx(glassWidthPx, glassHeightPx, lines, inkEm, scale)
            val em = PageFrame.em(widthPx)
            val pitchFloorEm = scale * PageFrame.LINE_HEIGHT_RATIO
            // What the page's height is besides its lines: one margin, the air,
            // the ink and the foot. They are all shares of the width, so they
            // are known once the width is, and the pitch is what is left.
            val fixed = widthPx * (1f - PageFrame.TEXT_WIDTH_RATIO) / 2f +
                em * (PageFrame.AIR_EM + inkEm * scale + PageFrame.FOOT_EM)
            val gaps = (lines - 1).coerceAtLeast(1)
            val pitchEm = maxOf(
                pitchFloorEm,
                (glassHeightPx - fixed) / gaps / em,
            )
            return PageGeometry(
                widthPx = widthPx,
                pitchEm = pitchEm,
                frame = PageFrame.of(widthPx, lines, scale, pitchEm, inkEm),
            )
        }
    }
}
