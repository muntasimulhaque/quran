package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Where the app's one popup stands, pinned as arithmetic rather than as a
 * screenshot.
 *
 * A popup draws in a window of its own, and that window's place on the glass
 * cannot be read back through a compose test: every node inside it reports
 * coordinates measured from the popup's own corner, so a test can see how big
 * the popup is and never where it hangs. The rule is therefore pinned here,
 * where it can be run on any machine, and each anchor's own place on the
 * control it hangs from is pinned in the compose suite, which can see that.
 *
 * The two together are the promise. The pill's anchor stands on the capsule's
 * middle, and its menu is centred on that anchor and rises above it with
 * [PopupGap] of ground between: the capsule floats at the foot of the page, so
 * below it is no room at all. The reading's ayah door stands at the head of an
 * ayah, hangs its menu from its own leading edge, and opens downward, the way
 * a list under a field does, until the glass runs out below and the menu flips
 * above.
 *
 * The ground is kept when the menu fits on the side it prefers. When the
 * window is shorter than that side's room, the menu slides inside the safe
 * band, even over the anchor, so the rule never hides an action behind a
 * scroll it did not need. The ground yields before an action does. A menu
 * taller than the whole band still scrolls inside the popup.
 */
class AppPopupPositionTest {

    /** [PopupGap] in pixels. The rule knows no density, so any number pins it. */
    private val gap = 40

    /** [PopupEdge] in pixels: the app's floating gutter, at the 3x the app draws. */
    private val edge = 48

    @Test
    fun aCentredPopupTakesTheAnchorsMiddle() {
        // A phone's capsule: the pill is 992 px wide inside a 1080 px window,
        // and the anchor is the popup's own measure in the middle of it.
        val anchor = IntRect(left = 144, top = 1_800, right = 936, bottom = 1_803)
        val at = place(
            anchor = anchor,
            content = IntSize(792, 304),
            window = IntSize(1080, 2_340),
            alignment = PopupAlignment.Center,
            side = PopupSide.Above,
        )
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
    fun aDoorHangsItsMenuFromItsLeadingEdge() {
        // The ayah door at the reading's own 28 dp gutter, in a 360 dp window.
        val anchor = IntRect(left = 84, top = 700, right = 260, bottom = 806)
        val at = place(
            anchor = anchor,
            content = IntSize(864, 480),
            window = IntSize(1080, 1_920),
            alignment = PopupAlignment.Start,
            side = PopupSide.Below,
        )
        assertEquals(
            "the menu's first line lands on the door's own line",
            anchor.left,
            at.x,
        )
        assertEquals(
            "the menu opens downward with the ground kept between",
            anchor.bottom + gap,
            at.y,
        )
    }

    @Test
    fun aDoorInARightToLeftInterfaceHangsFromItsRightEdge() {
        val anchor = IntRect(left = 820, top = 700, right = 996, bottom = 806)
        val at = AppPopupPosition(
            anchor = anchor,
            gap = gap,
            edge = edge,
            alignment = PopupAlignment.Start,
            side = PopupSide.Below,
            topLimit = 0,
            bottomLimit = 1_920,
        ).calculatePosition(
            anchorBounds = anchor,
            windowSize = IntSize(1_080, 1_920),
            layoutDirection = LayoutDirection.Rtl,
            popupContentSize = IntSize(864, 480),
        )
        assertEquals(
            "a menu under an Arabic door opens inward, not off the side it grew from",
            anchor.right - 864,
            at.x,
        )
    }

    @Test
    fun aPopupAboveLeavesTheGroundBetweenItAndTheAnchor() {
        val anchor = IntRect(left = 144, top = 1_800, right = 936, bottom = 1_803)
        val at = place(
            anchor = anchor,
            content = IntSize(792, 304),
            window = IntSize(1080, 2_340),
            alignment = PopupAlignment.Center,
            side = PopupSide.Above,
        )
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

    @Test
    fun theSideIsTheRoomierOneAndATieGoesBelow() {
        val anchor = IntRect(left = 84, top = 900, right = 260, bottom = 1_060)
        val window = 1_920
        val status = 72
        val navigation = 72

        val above = popupRoomAbove(anchor, reserveTop = status, gap = gap)
        val below = popupRoomBelow(anchor, windowHeight = window, reserveBottom = navigation, gap = gap)

        assertEquals("the room above clears the status bar and the gap", 900 - 72 - 40, above)
        assertEquals("the room below clears the navigation bar and the gap", 1_920 - 72 - 1_060 - 40, below)
        assertEquals("a tie goes below, the direction a menu under a mark reads", PopupSide.Below, popupSide(300, 300))
        assertEquals("the larger ground wins", PopupSide.Above, popupSide(800, 400))
        assertEquals("the larger ground wins the other way too", PopupSide.Below, popupSide(400, 800))
    }

    @Test
    fun theRoomNeverGoesBelowZero() {
        assertEquals(
            "a window too short for the gap has no room rather than less than none",
            0,
            popupRoomAbove(IntRect(left = 0, top = 10, right = 0, bottom = 12), reserveTop = 0, gap = 40),
        )
        assertEquals(
            "an anchor at the window's foot has no room under it",
            0,
            popupRoomBelow(
                IntRect(left = 0, top = 1_900, right = 0, bottom = 1_920),
                windowHeight = 1_920,
                reserveBottom = 0,
                gap = 40,
            ),
        )
    }

    @Test
    fun aPopupIsNeverOffTheGlass() {
        // A door against the glass: the app's own gutter moves the menu in.
        val atTheEdge = IntRect(left = 0, top = 700, right = 120, bottom = 806)
        val at = place(
            anchor = atTheEdge,
            content = IntSize(864, 480),
            window = IntSize(1_080, 1_920),
            alignment = PopupAlignment.Start,
            side = PopupSide.Below,
        )
        assertEquals("a menu against the glass keeps the app's floating gutter", edge, at.x)

        // A popup wider than the window keeps the window's own edge.
        val wider = place(
            anchor = IntRect(left = 0, top = 400, right = 200, bottom = 403),
            content = IntSize(1_100, 304),
            window = IntSize(1_080, 2_340),
            alignment = PopupAlignment.Center,
            side = PopupSide.Above,
        )
        assertEquals("a popup wider than the window starts at the window's edge", 0, wider.x)
    }

    @Test
    fun aMenuThatFitsNeitherSideTakesTheSafeBand() {
        // A door in the middle of a landscape phone: the menu is taller than
        // either side's room, but it fits the window. It takes the band and
        // may cover the door, because an action is worth more than the ground.
        val anchor = IntRect(left = 84, top = 440, right = 260, bottom = 546)
        val window = 960
        val status = 72
        val navigation = 72
        val content = IntSize(480, 480)
        val bandTop = status
        val bandBottom = window - navigation

        val below = place(
            anchor = anchor,
            content = content,
            window = IntSize(1_080, window),
            alignment = PopupAlignment.Start,
            side = PopupSide.Below,
            topLimit = bandTop,
            bottomLimit = bandBottom,
        )
        assertEquals(
            "a menu that cannot fit below slides up so its foot stays on the glass",
            bandBottom - content.height,
            below.y,
        )
        assertEquals(
            "its foot keeps the window's own line",
            bandBottom,
            below.y + content.height,
        )

        val above = place(
            anchor = anchor,
            content = content,
            window = IntSize(1_080, window),
            alignment = PopupAlignment.Start,
            side = PopupSide.Above,
            topLimit = bandTop,
            bottomLimit = bandBottom,
        )
        assertEquals(
            "a menu that cannot fit above slides down so its head stays on the glass",
            bandTop,
            above.y,
        )
    }

    private fun place(
        anchor: IntRect,
        content: IntSize,
        window: IntSize,
        alignment: PopupAlignment,
        side: PopupSide,
        topLimit: Int = 0,
        bottomLimit: Int = window.height,
    ): IntOffset = AppPopupPosition(
        anchor = anchor,
        gap = gap,
        edge = edge,
        alignment = alignment,
        side = side,
        topLimit = topLimit,
        bottomLimit = bottomLimit,
    ).calculatePosition(
        anchorBounds = anchor,
        windowSize = window,
        layoutDirection = LayoutDirection.Ltr,
        popupContentSize = content,
    )
}
