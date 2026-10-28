package io.github.muntasimulhaque.quran

import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.reader.readingFoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reading is drawn edge to edge, so its foot is the one place on the page
 * that has to know the system's navigation bar exists. The rule is arithmetic
 * over two lengths, so it is pinned here, where a change fails the build
 * instead of waiting for a store frame:
 *
 * 1. The page's own room stands above the bar, never under it, so the air
 *    under a surah's last line is the same on a phone with three buttons (a
 *    bar of about 50 dp), a phone with a gesture strip (about 24 dp), and a
 *    tablet whose taskbar takes no room at all.
 * 2. A control raised at the foot already carries the inset inside its own
 *    measured height, so it is taken as it stands: the bar is one piece of
 *    ground and the page pays for it once.
 */
class ReadingFootTest {

    @Test
    fun theFootStandsOnTopOfAThreeButtonBar() {
        // 32 dp of the page's own room, then the 50 dp the bar takes.
        assertEquals(82.dp, readingFoot(bottomStack = 0.dp, navigationBar = 50.dp))
    }

    @Test
    fun theFootFollowsTheBarItFinds() {
        assertEquals(56.dp, readingFoot(bottomStack = 0.dp, navigationBar = 24.dp))
        assertEquals(32.dp, readingFoot(bottomStack = 0.dp, navigationBar = 0.dp))
    }

    @Test
    fun aRaisedControlIsTakenAsItStands() {
        // 132 dp is a pill's own measured height with the bar inside it. The
        // inset must not be added again behind the pill.
        assertEquals(132.dp, readingFoot(bottomStack = 132.dp, navigationBar = 50.dp))
    }

    @Test
    fun theDeeperOfTheTwoWins() {
        // A control that stands shorter than the page's own room still gets
        // the room, so the bar is never half paid for.
        assertEquals(32.dp, readingFoot(bottomStack = 30.dp, navigationBar = 0.dp))
    }

    @Test
    fun theFeetAirNeverFallsBelowThePageRoom() {
        for (bar in 0..64) {
            val foot = readingFoot(bottomStack = 0.dp, navigationBar = bar.dp)
            val air = foot - bar.dp
            assertTrue("a $bar dp bar left $air dp of air", air == 32.dp)
        }
    }
}
