package io.github.muntasimulhaque.quran.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bar's arithmetic, pinned without a screen.
 *
 * The rule is the browser's: the first part of a scroll is the bar's, the
 * rest is the page's; a backward scroll brings the bar back before the page
 * moves; a released bar finishes the nearer end. The reading's text is never
 * behind the bar, which is why the page waits for it (owner decision, this
 * session).
 */
class ReaderBarTest {

    private fun bar(height: Int = 56): ReaderBarState =
        ReaderBarState().apply { ownHeightPx = height }

    @Test
    fun aForwardDragSpendsItselfOnTheBarBeforeThePageMoves() {
        val bar = bar()
        val consumed = bar.consume(-20f)
        assertEquals("the bar takes the whole drag while it has room", -20f, consumed, 0.001f)
        assertEquals("the bar is 36 of 56 shown", 36f / 56f, bar.shown, 0.001f)
    }

    @Test
    fun aForwardDragPastTheBarGivesTheRestToThePage() {
        val bar = bar()
        val consumed = bar.consume(-80f)
        assertEquals("the bar takes only its own height", -56f, consumed, 0.001f)
        assertEquals("the bar is away", 0f, bar.shown, 0.001f)
    }

    @Test
    fun aBackwardDragShowsTheBarBeforeThePageMoves() {
        val bar = bar().apply { consume(-56f) }
        val consumed = bar.consume(20f)
        assertEquals("the bar takes the whole backward drag", 20f, consumed, 0.001f)
        assertEquals("the bar is 20 of 56 back", 20f / 56f, bar.shown, 0.001f)
    }

    @Test
    fun theBarCannotOverrunEitherEnd() {
        val bar = bar()
        assertEquals("the bar takes no more than its own height", -56f, bar.consume(-999f), 0.001f)
        assertEquals(0f, bar.shown, 0.001f)
        assertEquals("a backward drag at the top shows only the bar", 56f, bar.consume(999f), 0.001f)
        assertEquals(1f, bar.shown, 0.001f)
        assertEquals("a standing bar has no room for a backward drag", 0f, bar.consume(40f), 0.001f)
    }

    @Test
    fun aReleasedBarFinishesTheNearerEnd() {
        assertEquals("half is the split", 1f, ReaderBar.settleTarget(0.5f), 0f)
        assertEquals("past the middle opens", 1f, ReaderBar.settleTarget(0.51f), 0f)
        assertEquals("short of the middle closes", 0f, ReaderBar.settleTarget(0.49f), 0f)
        assertEquals("a standing bar stays standing", 1f, ReaderBar.settleTarget(1f), 0f)
        assertEquals("an away bar stays away", 0f, ReaderBar.settleTarget(0f), 0f)
    }

    @Test
    fun aBarWithNoMeasuredHeightTakesNothing() {
        val bar = ReaderBarState()
        assertEquals("the page keeps its whole drag", 0f, bar.consume(-40f), 0.001f)
        assertEquals("the bar stays standing", 1f, bar.shown, 0.001f)
    }
}
