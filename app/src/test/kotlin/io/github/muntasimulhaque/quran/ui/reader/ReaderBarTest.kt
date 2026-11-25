package io.github.muntasimulhaque.quran.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bar's arithmetic, pinned without a screen.
 *
 * The rule is the browser's: the first part of a scroll is the bar's, the
 * rest is the page's; a backward scroll brings the bar back before the page
 * moves; a released bar finishes the nearer end. The reading's text is never
 * behind the bar, which is why the page waits for it (owner decision, 19.4).
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

    @Test
    fun aReleaseCarriesTheFingersOwnSpeed() {
        // Three deltas over forty milliseconds: sixty pixels of travel across a
        // fifty-six pixel span, so the release reads -60 / 0.04 / 56 of the
        // bar's travel per second and the settle starts at that speed
        // (owner decision, 22.8).
        val at = 1_000_000_000L
        val samples = listOf(
            TimedDelta(-20f, at),
            TimedDelta(-20f, at + 16_000_000L),
            TimedDelta(-20f, at + 32_000_000L),
        )
        assertEquals(
            "the finger's own speed",
            -60f / 40f * 1000f / 56f,
            ReaderBar.releaseVelocity(samples, at + 40_000_000L, 56),
            0.001f,
        )
    }

    @Test
    fun aStaleReleaseHasNoSpeed() {
        // A finger that stopped a fifth of a second before it lifted is a tap,
        // and a tap finishes the nearer end at rest.
        val at = 1_000_000_000L
        val samples = listOf(TimedDelta(-20f, at))
        assertEquals(0f, ReaderBar.releaseVelocity(samples, at + 200_000_000L, 56), 0f)
    }

    @Test
    fun aBarWithNoHeightHasNoSpeed() {
        // There is no travel to be a share of, so nothing may be divided by it.
        val at = 1_000_000_000L
        val samples = listOf(TimedDelta(-20f, at))
        assertEquals(0f, ReaderBar.releaseVelocity(samples, at + 8_000_000L, 0), 0f)
    }

    @Test
    fun stopClearsTheFingersTrail() {
        var clock = 1_000_000_000L
        val bar = ReaderBarState(now = { clock }).apply { ownHeightPx = 56 }
        bar.consume(-20f)
        clock += 8_000_000L
        assertTrue("the release before the stop: " + bar.releaseVelocity(clock), bar.releaseVelocity(clock) < 0f)
        bar.stop()
        assertEquals("a new gesture starts with no speed", 0f, bar.releaseVelocity(clock), 0f)
    }
}
