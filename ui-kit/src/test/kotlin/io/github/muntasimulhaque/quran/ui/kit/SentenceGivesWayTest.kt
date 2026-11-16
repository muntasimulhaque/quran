package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sentence's ground: a touch on its own paper is the sentence's, and a
 * touch anywhere else puts it away.
 *
 * A popup's place on the glass cannot be read back through a compose test,
 * and a sentence's ground is the same kind of value, so the rule that decides
 * a touch is arithmetic here and the paper's place is measured where it is
 * drawn. The two halves meet in this one function (owner decision, this
 * session).
 */
class SentenceGivesWayTest {

    /** The paper, near the foot of a 1080 by 2400 glass, 32 dp in from each side. */
    private val paper = Rect(left = 32f, top = 2200f, right = 1048f, bottom = 2344f)

    @Test
    fun aTouchOnThePaperKeepsTheSentence() {
        assertFalse(sentenceGivesWay(paper, Offset(540f, 2272f)))
        // The action pill's own ground is inside the paper, so the answer to
        // the sentence is never taken by the touch that gives it.
        assertFalse(sentenceGivesWay(paper, Offset(960f, 2272f)))
        // The very corner counts as the paper: a finger on the edge is the
        // edge of the sentence, not the page beside it.
        assertFalse(sentenceGivesWay(paper, Offset(32f, 2200f)))
    }

    @Test
    fun aTouchAnywhereElsePutsItAway() {
        // The page above the sentence, the glass beside it, and the ground
        // below its foot.
        assertTrue(sentenceGivesWay(paper, Offset(540f, 400f)))
        assertTrue(sentenceGivesWay(paper, Offset(8f, 2272f)))
        assertTrue(sentenceGivesWay(paper, Offset(540f, 2392f)))
        // The paper's own right and bottom edges are the page's side of the
        // line, the way a rectangle of pixels ends.
        assertTrue(sentenceGivesWay(paper, Offset(1048f, 2272f)))
        assertTrue(sentenceGivesWay(paper, Offset(540f, 2344f)))
    }

    @Test
    fun aTouchWithNoSentenceUpPutsAwayNothing() {
        // Nothing is drawn, so the ground is unknown, and the dismissal that
        // follows is a dismissal of nothing.
        assertTrue(sentenceGivesWay(null, Offset(540f, 2272f)))
    }
}
