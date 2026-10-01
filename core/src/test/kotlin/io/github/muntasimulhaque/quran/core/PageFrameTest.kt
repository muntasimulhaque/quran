package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The page's own proportions, pinned here because the frame's four margins
 * are what a reader sees and nothing in a build can see them.
 *
 * The margins were four numbers and one of them was wrong: the sides and the
 * head stood a few dp from the glyphs while the foot stood a band away, so
 * the rule read as a wire pressed against the text. The rule is one number
 * now, and these are the measurements that say so.
 */
class PageFrameTest {

    /** The page in the report: 1080 px wide, the width a phone renders at. */
    private val width = 1080

    /** A line's ink above and below its baseline, in ems, as these fonts carry it. */
    private val ink = 1.72f

    private val frame = PageFrame.of(width, ink)
    private val em = PageFrame.em(width)
    private val line = em * PageFrame.LINE_HEIGHT_RATIO
    private val halfLeading = (line - ink * em) / 2f

    /** Where the text's own ink begins and ends, in page pixels. */
    private val inkTop = frame.slotTop + halfLeading
    private val inkBottom = frame.slotTop + PageFrame.LINES * line - halfLeading

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
        // The roundel stands in the middle of the room under the foot's rule,
        // and a roundel that crossed the page's own edge would be cut off by
        // the bitmap that draws it.
        val diameter = 2f * PageFrame.ROUNDEL_EM * em
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
    fun thePagerReservesTheTallestPageAnyFontCanDraw() {
        // The pager picks the page's width before it knows which page's font
        // it will draw, so the share it reserves has to hold every font in
        // the pack and not the one it happened to measure.
        val reserved = PageFrame.aspect(PageFrame.INK_EM_TALLEST)
        for (tallest in listOf(1.644f, 1.72f, 1.8f, PageFrame.INK_EM_TALLEST)) {
            assertTrue(
                "a page of $tallest em of ink must fit the room the pager gave it",
                PageFrame.aspect(tallest) <= reserved,
            )
        }
        assertEquals(
            "the reserved share is the geometry's own, not a remembered number",
            1.591f,
            reserved,
            0.002f,
        )
    }

    @Test
    fun theFrameIsTheSamePageEveryTime() {
        // The page is a cache key on its width and nothing else, so the same
        // width must draw the same page every time it is asked for.
        for (inPixels in intArrayOf(1, 320, 1080, 2560, 4096)) {
            for (fontInk in listOf(1.644f, 1.72f, 1.8f)) {
                val first = PageFrame.of(inPixels, fontInk)
                val second = PageFrame.of(inPixels, fontInk)
                assertEquals(first.heightPx, second.heightPx)
                assertEquals(first.left, second.left, 0f)
                assertEquals(first.foot, second.foot, 0f)
            }
        }
    }

    @Test
    fun aTinyPageIsStillAPage() {
        val tiny = PageFrame.of(1, ink)
        assertTrue("a page of one pixel is still a page", tiny.heightPx >= 1)
        assertTrue("and its rule is on it", tiny.left >= 0f)
    }
}