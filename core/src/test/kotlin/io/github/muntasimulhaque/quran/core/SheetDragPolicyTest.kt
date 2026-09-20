package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The browse sheet's pull: a gesture that starts with the list scrolled is
 * the list's to finish, and a gesture that starts at the top is the sheet's.
 * "Kept" is what the policy swallows so the sheet never sees it.
 */
class SheetDragPolicyTest {

    @Test
    fun aGestureFromTheTopLeavesEverythingToTheSheet() {
        val policy = SheetDragPolicy()
        policy.onUserInput(atTop = true)
        assertEquals(0f, policy.leftoverScroll(120f), 0.001f)
        assertEquals(0f, policy.endGesture(400f), 0.001f)
    }

    @Test
    fun aGestureFromAScrolledListKeepsItsLeftovers() {
        val policy = SheetDragPolicy()
        policy.onUserInput(atTop = false)
        assertEquals(120f, policy.leftoverScroll(120f), 0.001f)
        assertEquals(400f, policy.endGesture(400f), 0.001f)
    }

    @Test
    fun reachingTheTopMidGestureDoesNotTurnItIntoAPull() {
        val policy = SheetDragPolicy()
        // The gesture began with the list scrolled, then the list reached its
        // top and its leftovers kept coming. They all belong to the list.
        policy.onUserInput(atTop = false)
        policy.onUserInput(atTop = true)
        assertEquals(80f, policy.leftoverScroll(80f), 0.001f)
        assertEquals(250f, policy.endGesture(250f), 0.001f)
    }

    @Test
    fun aGestureThatMovesTheListKeepsItsLaterLeftovers() {
        val policy = SheetDragPolicy()
        // The gesture began at the top and then scrolled the list; the pull
        // that follows the scroll home is still the list's. This is also the
        // stale record of an interrupted fling: its beginning was at the top,
        // but the next touch lands on a scrolled list, so the sheet stays out
        // of it even though no end was ever reported.
        policy.onUserInput(atTop = true)
        policy.onUserInput(atTop = false)
        assertEquals(90f, policy.leftoverScroll(90f), 0.001f)
        assertEquals(300f, policy.endGesture(300f), 0.001f)
    }

    @Test
    fun theNextGestureIsJudgedFromItsOwnBeginning() {
        val policy = SheetDragPolicy()
        policy.onUserInput(atTop = false)
        policy.endGesture(100f)
        policy.onUserInput(atTop = true)
        assertEquals(0f, policy.leftoverScroll(100f), 0.001f)
        assertEquals(0f, policy.endGesture(100f), 0.001f)
    }

    @Test
    fun aFingerLandingStartsAFreshGesture() {
        val policy = SheetDragPolicy()
        // The last gesture began with the list scrolled and never reported
        // its end, because an interrupted fling does not. A finger lands at
        // the top, and the pull that follows belongs to the sheet.
        policy.onUserInput(atTop = false)
        policy.onPointerDown()
        policy.onUserInput(atTop = true)
        assertEquals(0f, policy.leftoverScroll(120f), 0.001f)
        assertEquals(0f, policy.endGesture(400f), 0.001f)
    }

    @Test
    fun inputAfterTheGestureEndedIsNotKept() {
        val policy = SheetDragPolicy()
        policy.onUserInput(atTop = false)
        policy.endGesture(100f)
        // A programmatic scroll with no new gesture must not be swallowed.
        assertEquals(0f, policy.leftoverScroll(50f), 0.001f)
    }
}
