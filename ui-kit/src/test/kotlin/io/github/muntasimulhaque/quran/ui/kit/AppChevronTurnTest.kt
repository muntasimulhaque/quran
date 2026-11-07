package io.github.muntasimulhaque.quran.ui.kit

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The app's one chevron, pinned as arithmetic because a mark's turn cannot be
 * read back through a compose test: a node reports its bounds and its spoken
 * state, never the angle its drawing was turned to.
 *
 * The compose suite pins the other half of the promise, that a door's open
 * state reaches its mark and is spoken: `PlaybackPillTest` reads the pill's
 * three doors, and `AyahActionsTest` reads the ayah's. The two suites together
 * say that a door which opens a popup turns and says so.
 *
 * The rule is [chevronTurn]'s, and it is the same one the drawing reads, so
 * this test cannot drift from the mark.
 */
class AppChevronTurnTest {

    @Test
    fun expandPointsDownUntilTheContentIsShown() {
        assertEquals(
            "hidden content points down",
            0f,
            chevronTurn(ChevronRole.Expand, expanded = false),
            0f,
        )
        assertEquals(
            "shown content points up",
            180f,
            chevronTurn(ChevronRole.Expand, expanded = true),
            0f,
        )
    }

    @Test
    fun theWayMarksKeepTheirWay() {
        assertEquals(
            "a way back points left",
            90f,
            chevronTurn(ChevronRole.Back, expanded = false),
            0f,
        )
        assertEquals(
            "the way on points right",
            -90f,
            chevronTurn(ChevronRole.Next, expanded = false),
            0f,
        )
    }
}
