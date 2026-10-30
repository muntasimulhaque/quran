package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The measure of every popup in the app, and of the anchor the pill hangs one
 * from.
 *
 * A popup takes its own edge from the anchor it was given, so an anchor of
 * another measure would stand the menu beside the control's middle rather
 * than on it. The pill's anchor is this wide and centered, and the menu is
 * this wide, so the two land on one center whatever the pill's own width is
 * (owner report). It is the reciter menu's old maximum and the listening
 * menu's own need: the five paces at their 48 dp target with the padding
 * around them, which is the measure the listener has to get and the widest
 * the two ever were.
 *
 * The reading's ayah menu is this measure too. It carries five rows rather
 * than a panel, so it is wider than its words need; that air is the price of
 * one menu in the app instead of two, and the rows keep their names on the
 * one leading rail the pill's rows already stand on (owner decision, this
 * session).
 */
val PopupMeasure = 288.dp

/**
 * The room a popup keeps from its own edge at the top and the foot.
 *
 * The popup's corner is 20 dp, so content left at its edge sat hard against
 * the curve and the first and last highlights read as touching the menu they
 * hang in (owner report). Ten dp keeps the first and last rows off the curve
 * above and below them; the rows keep their own 22 dp gutter at the sides.
 */
val PopupGutter = 10.dp

/**
 * The ground a popup keeps between its own foot and the anchor's edge.
 *
 * The popup wears the control's own cloth, carries no border, and wears its
 * own lift as well (owner decision, forty-seventh session). The ground is
 * what keeps the two silhouettes apart: with the popup's foot on the
 * capsule's own top edge the flat edges met along the middle, where no seam
 * can be seen, and the popup's foot corners and the capsule's shoulders
 * kissed at each end, so the two read as one shape with a waist. Ten dp keeps
 * the capsule's own lift from reaching the popup's foot and sits well inside
 * the popup's 20 dp corner, so the two silhouettes never meet.
 */
val PopupGap = 10.dp

/**
 * The least air between a popup and the edge of the glass.
 *
 * It is the app's one floating gutter, the same 16 dp a bar keeps from the
 * glass, so a menu over the page reads as a control over it rather than as a
 * sheet the app forgot to inset. It is a floor, not a measure: a window wide
 * enough keeps the popup on its anchor, and a window too narrow moves it in
 * until the gutter holds.
 */
val PopupEdge = 16.dp

/**
 * How a popup stands over its anchor.
 *
 * [Center] is a capsule's answer: the pill is a centered thing, and its menu
 * takes the same middle. [Start] is a door's answer: the ayah's number is a
 * leading mark, and its menu hangs from that edge the way a list under a
 * field does, so the menu's first line lands on the door's own line instead
 * of being pushed half off the glass by a center it never had (owner
 * decision, this session).
 */
enum class PopupAlignment { Center, Start }

/**
 * The app's one popup: a menu over the page, in the control's own cloth and
 * hung from the control it answers.
 *
 * Material's own dropdown takes the left edge of the box it is given and puts
 * itself where that box's left edge stands, so on the pill the reciter chooser
 * stood at one end of the capsule and the listening answers at the other, and
 * a reader's eye went to two ends of one control for two questions (owner
 * report). The placement is therefore the app's own, and it is arithmetic:
 * [AppPopupPosition] is the one rule, and a JVM test pins it because a
 * popup's place on the glass cannot be read back through a compose test.
 *
 * **It opens on the side with more room** (owner decision, this session). A
 * pill at the foot of the page has no room below it, so its menu still rises
 * exactly as it always has; a door in the middle of the page opens downward,
 * the way a menu under a field does, and one near the foot flips above. The
 * room is the larger side's, and the popup never grows past it: what does not
 * fit scrolls inside, so the position rule never has to clamp and the ground
 * between the popup and the anchor holds on the shortest window as it does on
 * the tallest.
 *
 * The popup takes [PopupMeasure] wide, the popup's own corners and lift, the
 * surface tone the app floats on, and [PopupGutter] at the head and foot, so
 * the first and last rows stay clear of the curve. [name] is its pane title,
 * spoken and never drawn, the same shape the share sheet's announcement
 * already wears: a reader who cannot see the menu hears what it belongs to
 * before its first row. The panel takes the focus once it has settled, so the
 * first row never opens looking chosen, and a tap outside, Back, or a scroll
 * of the page puts it away (owner decision, this session: the popup goes
 * with the page, because an anchor that moves under a hanging menu leaves it
 * pointing at nothing).
 */
@Composable
fun AppPopup(
    /** The anchor's own place on the screen, measured by the box it hangs in. */
    anchor: IntRect,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: PopupAlignment = PopupAlignment.Center,
    /** What the menu belongs to, for a screen reader. Never drawn. */
    name: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // The rule is arithmetic in pixels, so the app's dp are converted once,
    // here, where the density and the window are known.
    val density = LocalDensity.current
    val gap = with(density) { PopupGap.roundToPx() }
    val edge = with(density) { PopupEdge.roundToPx() }
    // The window is the app's own, edge to edge, so the system's bars are
    // part of it and the popup must keep out of them itself.
    val status = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().roundToPx()
    }
    val navigation = with(density) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().roundToPx()
    }
    val windowHeight = LocalView.current.height.coerceAtLeast(1)
    val above = popupRoomAbove(anchor, status, gap)
    val below = popupRoomBelow(anchor, windowHeight, navigation, gap)
    val side = popupSide(above = above, below = below)
    val room = with(density) {
        (if (side == PopupSide.Below) below else above).coerceAtLeast(0).toDp()
    }
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        popupPositionProvider = AppPopupPosition(
            anchor = anchor,
            gap = gap,
            edge = edge,
            alignment = alignment,
            side = side,
        ),
    ) {
        // The popup's window hands the focus to its first focusable child, and
        // Material draws that focus as a state layer over the first row: the
        // panel opened with the first row looking chosen before the reader had
        // touched it (owner report, this session). The panel itself is the
        // first focus target and takes the focus once it has settled, so it
        // opens quiet; the rows keep their focus, reached with Tab, and the
        // panel's target draws nothing and carries no semantics.
        val openingFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            repeat(3) { withFrameNanos { } }
            openingFocus.requestFocus()
        }
        Column(
            modifier = modifier
                .width(PopupMeasure)
                .heightIn(max = room)
                .floatingLift(Radii.Popup)
                .clip(Radii.Popup)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = PopupGutter)
                .testTag("app-popup")
                .semantics { if (name != null) paneTitle = name }
                .focusRequester(openingFocus)
                .focusTarget()
                .sheetVerticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

/** The side of its anchor a popup stands on. */
internal enum class PopupSide { Below, Above }

/**
 * The room above an anchor: the ground between the window's top and the line
 * the popup keeps above the anchor, with the system's own bar cleared first.
 */
internal fun popupRoomAbove(anchor: IntRect, reserveTop: Int, gap: Int): Int =
    (anchor.top - reserveTop - gap).coerceAtLeast(0)

/**
 * The room below an anchor: the ground between the anchor's foot and the
 * window's foot, with the system's own bar cleared and the gap kept.
 */
internal fun popupRoomBelow(
    anchor: IntRect,
    windowHeight: Int,
    reserveBottom: Int,
    gap: Int,
): Int = (windowHeight - reserveBottom - anchor.bottom - gap).coerceAtLeast(0)

/**
 * The side a popup takes: the roomier one, because the larger ground is the
 * one that can hold the menu. A tie goes below, which is the direction a menu
 * under a mark already reads in. The pill's capsule sits at the foot of the
 * page, so below is no room at all and its menu rises, exactly as it always
 * has (owner decision, this session).
 */
internal fun popupSide(above: Int, below: Int): PopupSide =
    if (below >= above) PopupSide.Below else PopupSide.Above

/**
 * Where the app's popup stands: on the roomier side of its anchor, with
 * [gap] of the page's ground between, aligned the way its anchor asks, and
 * never off the glass.
 *
 * The anchor is a box, and the box's own bounds are read here rather than
 * left to the library: this is the one place in the app that says where a
 * popup goes, and it says it about the control. The arithmetic is the whole
 * of it, which is why `AppPopupPositionTest` can pin it on a machine with no
 * screen.
 */
internal class AppPopupPosition(
    private val anchor: IntRect,
    /** [PopupGap] in pixels, converted where the density is known. */
    private val gap: Int,
    /** [PopupEdge] in pixels, converted where the density is known. */
    private val edge: Int,
    private val alignment: PopupAlignment,
    private val side: PopupSide,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val width = popupContentSize.width
        val aligned = when (alignment) {
            // A capsule's answer: the menu takes the capsule's own middle.
            PopupAlignment.Center -> anchor.center.x - width / 2
            // A door's answer: the menu hangs from the door's leading edge,
            // which is its left edge in a left-to-right interface and its
            // right edge in a right-to-left one, so a menu under an Arabic
            // door cannot open off the side it grew from.
            PopupAlignment.Start -> if (layoutDirection == LayoutDirection.Ltr) {
                anchor.left
            } else {
                anchor.right - width
            }
        }
        // The room a narrower window leaves, and the app's own gutter inside
        // it. A popup wider than the window (a fixed measure on a tiny one)
        // keeps the window's own edge instead of a negative gutter, which is
        // the floor the rule had before the gutter was named.
        val lastLeft = (windowSize.width - width).coerceAtLeast(0)
        val x = if (lastLeft >= edge * 2) {
            aligned.coerceIn(edge, lastLeft - edge)
        } else {
            aligned.coerceIn(0, lastLeft)
        }
        // The room on the chosen side was handed to the popup's own measure,
        // so this is never a clamp: it is the placement the ground allows.
        val y = when (side) {
            PopupSide.Below -> anchor.bottom + gap
            PopupSide.Above -> anchor.top - gap - popupContentSize.height
        }
        return IntOffset(x, y)
    }
}
