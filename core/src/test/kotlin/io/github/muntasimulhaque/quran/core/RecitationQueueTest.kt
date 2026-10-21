package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The playlist's growth, pinned in the one order it is allowed to happen.
 *
 * A new playlist is reported to its listeners before the call that set it
 * returns. A start that recorded its surah after that call was measured from
 * the old surah, so the first advance appended the next surah twice, or, when
 * the start was on a surah's last ayah, appended nothing and offered a
 * package already on the device (owner report, this session).
 */
class RecitationQueueTest {

    @Test
    fun aStartIsLoadedThroughItsOwnSurah() {
        val queue = RecitationQueue()
        queue.load(108)
        assertEquals(108, queue.loadedThrough)
    }

    @Test
    fun aStartThenTheReportThenTheFirstBoundaryAppendsOnce() {
        val queue = RecitationQueue()
        // The controller records the start before the player sees the
        // playlist, so both the player's playlist report and the first ayah
        // boundary find the same answer, and only one of them appends.
        queue.load(108)
        assertEquals(109, queue.appendAt(108))
        assertNull(queue.appendAt(108))
    }

    @Test
    fun aReportBeforeTheStartIsMeasuredFromTheOldSurah() {
        // The order the controller used: the player's report arrived before
        // the start was recorded. A replay of the same surah appended the
        // next one at the report, and the late start then moved the queue
        // back so the first boundary appended it again.
        val queue = RecitationQueue(loadedThrough = 108)
        assertEquals(109, queue.appendAt(108))
        queue.load(108)
        assertEquals(109, queue.appendAt(108))
    }

    @Test
    fun theAdvanceFromTheNextSurahAppendsTheOneAfterIt() {
        val queue = RecitationQueue()
        queue.load(108)
        assertEquals(109, queue.appendAt(108))
        assertEquals(110, queue.appendAt(109))
    }

    @Test
    fun nothingFollowsTheLastSurah() {
        val queue = RecitationQueue()
        queue.load(114)
        assertNull(queue.appendAt(114))
    }

    @Test
    fun aSurahTheQueueIsNotLoadedThroughAppendsNothing() {
        val queue = RecitationQueue()
        queue.load(108)
        assertNull(queue.appendAt(2))
    }
}
