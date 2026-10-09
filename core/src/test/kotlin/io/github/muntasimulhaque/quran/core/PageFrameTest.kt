package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The page's own proportions, pinned here because the frame's four margins
 * are what a reader sees and nothing in a build can see them.
 *
 * The margins are one number and the pitch is the page's own: the glass raises
 * the pitch to fill whatever room fifteen lines are given and never lowers it
 * under the page's own leading, so these are the measurements that say so.
 */
class PageFrameTest {

    /** The page in the report: 1080 px wide, the width a phone renders at. */
    private val width = 1080

    /** A line's ink above and below its baseline, in ems, as the shipped face carries it. */
    private val ink = PageFrame.INK_EM

    private val scale = 0.7f

    private val frame = PageFrame.of(width, lines = PageFrame.LINES, scale = scale, pitchEm = scale * PageFrame.LINE_HEIGHT_RATIO)
    private val em = PageFrame.em(width)
    private val line = frame.pitchPx
    private val inkPx = ink * em * scale
    private val halfLeading = (line - inkPx) / 2f

    /** Where the text's own ink begins and ends, in page pixels. */
    private val inkTop = frame.slotTop + halfLeading
    private val inkBottom = frame.slotTop + (PageFrame.LINES - 1) * line + halfLeading + inkPx

    private val measure = (width - width * PageFrame.TEXT_WIDTH_RATIO) / 2f

    @Test
    fun theAirIsTheSameNumberOnAllFourSides() {
        val air = PageFrame.AIR_EM * em
        assertEquals(
            "the rule stands off the measure by the one air",
            air,
            measure - frame.left,
            0.01f,
        )
        assertEquals(
            "the rule stands off the first line's ink by the one air",
            air,
            inkTop - frame.head,
            0.01f,
        )
        assertEquals(
            "the rule stands off the last line's ink by the one air",
            air,
            frame.foot - inkBottom,
            0.01f,
        )
        assertEquals(
            "the right side is the left side's own mirror",
            frame.left,
            width - frame.right,
            0.01f,
        )
    }

    @Test
    fun theRuleIsInsideThePageOnEverySideWithPaperLeftOutsideIt() {
        assertTrue("the rule is on the page", frame.left > 0f)
        assertTrue("the rule is on the page", frame.right < width)
        assertTrue("the head is on the page", frame.head > 0f)
        assertTrue("the foot is on the page", frame.foot < frame.heightPx)
        // The paper outside the rule is what makes it a rule on a sheet rather
        // than the screen's own edge, so it is never allowed to vanish.
        val trim = em * 0.25f
        assertTrue(
            "the paper outside the rule is a margin and not a hair: ${frame.left}",
            frame.left >= trim,
        )
    }

    @Test
    fun everyLineSitsInsideTheFrame() {
        for (index in 0 until PageFrame.LINES) {
            val top = frame.slotTop + index * line
            assertTrue("line $index begins below the head", top + halfLeading >= frame.head)
            assertTrue("line $index ends above the foot", top + line - halfLeading <= frame.foot)
        }
    }

    @Test
    fun theFootHoldsThePageNumber() {
        // The roundel stands in the middle of the room the foot's rule leaves,
        // and a roundel that crossed the page's own edge would be cut off by
        // the bitmap that draws it.
        val diameter = 2f * PageFrame.ROUNDEL_EM_PAGE * em
        val centre = frame.foot + (frame.heightPx - frame.foot) / 2f
        assertTrue(
            "the roundel is drawn whole inside the foot's own room",
            centre - diameter / 2f > frame.foot && centre + diameter / 2f < frame.heightPx,
        )
        assertTrue(
            "the roundel keeps paper above and below it",
            centre - diameter / 2f - frame.foot >= em * 0.2f,
        )
    }

    @Test
    fun theTypeIsNotSpentOnTheMargin() {
        // The measure is 88 percent of the page: the two percent it gives up
        // is the air the rule stands in, and the mushaf's own size is not
        // spent again on furniture.
        assertTrue(
            "the mushaf keeps its size",
            PageFrame.TEXT_WIDTH_RATIO >= 0.86f,
        )
        assertEquals(
            "a full line of glyphs sums to the measure it is drawn at",
            width * PageFrame.TEXT_WIDTH_RATIO,
            em * PageFrame.EM_PER_LINE,
            0.01f,
        )
    }

    @Test
    fun theShippedFacesInkIsUnderTheCeiling() {
        // The pager reads the face's own ink, and a face that grew taller than
        // the ceiling would draw a page whose marks crossed the rule.
        assertTrue(
            "the shipped face's ink is under the ceiling",
            PageFrame.INK_EM <= PageFrame.INK_EM_TALLEST,
        )
        assertTrue(
            "and it is measured, not guessed: the face carries marks above its letters",
            PageFrame.INK_EM > 1.7f,
        )
    }

    @Test
    fun theRoundelIsAsWideAsTheNumberItCarries() {
        val one = PageFrame.roundelWidthEm(7)
        val two = PageFrame.roundelWidthEm(42)
        val three = PageFrame.roundelWidthEm(286)
        assertEquals("a one digit number fills the roundel's own height", 2f * PageFrame.MARKER_EM, one, 0.001f)
        assertTrue("two digits are wider than the height", two > one)
        assertTrue("three digits are wider still", three > two)
        // The width the engine reserves is the width the drawer draws: the
        // ornament's digits at the roundel's own size, plus its padding.
        assertEquals(
            "three digits at the ornament's own advance, padded",
            3 * PageFrame.ORNAMENT_DIGIT_EM * PageFrame.MARKER_DIGITS_EM + 2 * PageFrame.ROUNDEL_PAD_EM,
            three,
            0.001f,
        )
    }

    @Test
    fun theWidestPageOfTheBookFillsTheGlassHeight() {
        // A dense page is set small and a sparse one large, and a wide glass is
        // filled from whichever of them the page is: the narrower of the two is
        // the page whose own lines are the longer.
        val dense = PageGeometry.of(2560f, 900f, lines = PageFrame.LINES, scale = 0.6f)
        val sparse = PageGeometry.of(2560f, 900f, lines = PageFrame.LINES, scale = 1.1f)
        assertTrue("a dense page is wider than a sparse one", dense.widthPx > sparse.widthPx)
        for (geometry in listOf(dense, sparse)) {
            assertEquals(
                "the page fills the glass it was given",
                900,
                geometry.frame.heightPx,
            )
        }
    }

    @Test
    fun aPhoneFillsBothOfItsSides() {
        // A phone is taller than a page of fifteen lines, so the page takes the
        // glass's own width and is opened out to fill its height.
        for (scale in listOf(0.6f, 1.1f)) {
            val geometry = PageGeometry.of(1080f, 1920f, lines = PageFrame.LINES, scale = scale)
            assertEquals("the page is as wide as the glass", 1080, geometry.widthPx)
            assertEquals("and as tall as the glass", 1920, geometry.frame.heightPx)
        }
    }

    @Test
    fun thePitchNeverGoesUnderThePagesOwnLeading() {
        // A wide glass could hold fifteen lines with a tighter pitch than the
        // page's own; the page refuses, because a line whose ink touches the
        // line under it is not a line of the Book.
        val wide = PageGeometry.of(2560f, 900f, lines = PageFrame.LINES, scale = 1.1f)
        val leading = wide.frame.pitchPx / (PageFrame.em(wide.widthPx) * 1.1f)
        assertEquals(
            "the pitch is the page's own leading",
            PageFrame.LINE_HEIGHT_RATIO.toDouble(),
            leading.toDouble(),
            0.001,
        )
        assertEquals("the page fills the glass it was given", 900, wide.frame.heightPx)
    }

    @Test
    fun theFrameIsTheSamePageEveryTime() {
        // The page is a cache key on its glass and nothing else, so the same
        // glass must draw the same page every time it is asked for.
        for (inPixels in intArrayOf(1, 320, 1080, 2560, 4096)) {
            for (fontInk in listOf(1.644f, 1.72f, PageFrame.INK_EM)) {
                val first = PageFrame.of(inPixels, PageFrame.LINES, scale, pitchEm = 1.2f, inkEm = fontInk)
                val second = PageFrame.of(inPixels, PageFrame.LINES, scale, pitchEm = 1.2f, inkEm = fontInk)
                assertEquals(first.heightPx, second.heightPx)
                assertEquals(first.left, second.left, 0f)
                assertEquals(first.foot, second.foot, 0f)
            }
        }
    }

    @Test
    fun aTinyPageIsStillAPage() {
        val tiny = PageFrame.of(1, PageFrame.LINES, scale, pitchEm = 1.2f, inkEm = ink)
        assertTrue("a page of one pixel is still a page", tiny.heightPx >= 1)
        assertTrue("and its rule is on it", tiny.left >= 0f)
    }
}
