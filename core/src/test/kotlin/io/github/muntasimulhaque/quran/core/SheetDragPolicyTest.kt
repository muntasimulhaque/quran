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
    fun theNextGestureIsJudgedFromItsOwnBeginning() {
        val policy = SheetDragPolicy()
        policy.onUserInput(atTop = false)
        policy.endGesture(100f)
        policy.onUserInput(atTop = true)
        assertEquals(0f, policy.leftoverScroll(100f), 0.001f)
        assertEquals(0f, policy.endGesture(100f), 0.001f)
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
