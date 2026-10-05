package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Where the pill's popup stands, pinned as arithmetic rather than as a
 * screenshot.
 *
 * A popup draws in a window of its own, and that window's place on the glass
 * cannot be read back through a compose test: every node inside it reports
 * coordinates measured from the popup's own corner, so a test can see how big
 * the popup is and never where it hangs. The rule is therefore pinned here,
 * where it can be run on any machine, and the anchor's own place on the
 * capsule is pinned in `PlaybackPillTest`, which can see that.
 *
 * The two together are the promise: the anchor stands on the capsule's centre,
 * and the popup stands above the anchor's own top edge with [PopupGap] of
 * ground between the two: with one cloth and no shadow, that ground is the
 * only thing that says the popup and the capsule are two surfaces. The first
 * owner report was a reciter chooser at one end of the pill and the listening
 * answers at the other, two popups in two places on one control; the second
 * was the popup standing on the capsule's own edge, where the flat edges met
 * and the corners kissed (owner report).
 */
class PillMenuPositionTest {

    /**
     * The ground the rule keeps between the popup's foot and the capsule, in
     * pixels. The rule is arithmetic and knows no density, so any number
     * here pins it; 40 is easy to follow through the sums.
     */
    private val gap = 40

    @Test
    fun thePopupIsCentredOnTheAnchor() {
        // A phone's capsule: the pill is 992 px wide inside a 1080 px window,
        // and the anchor is the popup's own measure in the middle of it.
        val anchor = IntRect(left = 144, top = 1_800, right = 936, bottom = 1_803)
        val at = place(anchor = anchor, content = IntSize(792, 304), window = IntSize(1080, 2_340))
        assertEquals(
            "the popup's own left edge is the anchor's left edge, so both stand on one centre",
            anchor.left,
            at.x,
        )
        assertEquals(
            "the popup's own middle is the anchor's middle",
            anchor.center.x,
            at.x + 792 / 2,
        )
    }

    @Test
    fun thePopupRisesOutOfTheCapsulesTopEdgeWithGroundBetween() {
        val anchor = IntRect(left = 144, top = 1_800, right = 936, bottom = 1_803)
        val at = place(anchor = anchor, content = IntSize(792, 304), window = IntSize(1080, 2_340))
        assertEquals(
            "the popup stands above the capsule with the gap kept, not over it",
            anchor.top - gap - 304,
            at.y,
        )
        assertEquals(
            "the popup's foot keeps the gap above the capsule's top edge",
            gap,
            anchor.top - (at.y + 304),
        )
    }

    /**
     * The ground is absolute: the popup takes no more room than the ground
     * above the capsule, less the gap, so the position rule never clamps and
     * the two shapes cannot meet on any window. What does not fit scrolls
     * inside the popup (owner decision).
     */
    @Test
    fun thePopupNeverGrowsPastTheGroundAboveTheCapsule() {
        assertEquals(
            "the room is the capsule's own top edge less the gap",
            1_800 - 40,
            popupRoom(1_800, 40),
        )
        assertEquals(
            "a window too short for the gap has no room rather than less than none",
            0,
            popupRoom(10, 40),
        )
    }

    /**
     * The wide pill, a tablet or a phone in landscape: the anchor is the same
     * measure and the same middle of a much wider capsule, and the popup does
     * not follow the capsule's own width (owner report).
     */
    @Test
    fun thePopupDoesNotFollowAWideCapsule() {
        val anchor = IntRect(left = 1_224, top = 600, right = 2_016, bottom = 603)
        val at = place(anchor = anchor, content = IntSize(792, 304), window = IntSize(2_400, 1_600))
        assertEquals(anchor.left, at.x)
        assertEquals(anchor.top - gap - 304, at.y)
    }

    /**
     * A popup wider than the window, or an anchor pushed against one edge, is
     * never half off the glass: it stands at the window's own margin.
     */
    @Test
    fun thePopupIsNeverOffTheWindow() {
        val againstTheLeftEdge = IntRect(left = 0, top = 400, right = 200, bottom = 403)
        val left = place(
            anchor = againstTheLeftEdge,
            content = IntSize(1_100, 304),
            window = IntSize(1_080, 2_340),
        )
        assertEquals("a popup wider than the window starts at the window's edge", 0, left.x)

        val tooTallForTheRoomAbove = IntRect(left = 144, top = 120, right = 936, bottom = 123)
        val high = place(
            anchor = tooTallForTheRoomAbove,
            content = IntSize(792, 900),
            window = IntSize(1_080, 2_340),
        )
        assertEquals("a popup taller than the room above starts at the top", 0, high.y)
    }

    private fun place(anchor: IntRect, content: IntSize, window: IntSize): IntOffset =
        PillMenuPosition(anchor, gap).calculatePosition(
            anchorBounds = anchor,
            windowSize = window,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = content,
        )
}