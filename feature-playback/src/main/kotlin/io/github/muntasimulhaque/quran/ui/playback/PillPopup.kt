package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * A popup the pill opens, in the pill's own cloth and hung from the capsule's
 * own middle.
 *
 * Material's own dropdown takes the left edge of the box it is given and puts
 * itself where that box's left edge stands, so on this pill the reciter chooser
 * stood at one end of the capsule and the listening answers at the other, and a
 * reader's eye went to two ends of one control for two questions (owner
 * report). The placement is therefore the app's own: the anchor is the
 * capsule's centre at the popup's own measure, and the popup is centred on it
 * and rises above the capsule's top edge with [PopupGap] of the page's own
 * ground between, the shape a menu opening out of a capsule at the foot of a
 * page is expected to have.
 *
 * It wears the pill's own surface, the pill's own rounded shape, and the
 * pill's own lift: a menu that floats over the reading is the same kind of
 * thing the capsule is, so it reads as a height above the page and not as a
 * patch on it (owner decision, forty-seventh session). The ground is still
 * what keeps the two silhouettes apart: the popup rises above the capsule's
 * top edge with [PopupGap] of the page's own ground between, and a popup
 * standing on the capsule's own edge met the capsule's curve with nothing
 * between (owner report). The popup takes no taller a measure than the
 * ground above the gap, so the ground is kept on a window too short for the
 * content, and what does not fit scrolls inside it (owner decision). A tap
 * outside puts it away, as a dropdown's does.
 */
@Composable
internal fun PillPopup(
    open: Boolean,
    /** The anchor's own place on the screen, measured by the box it hangs in. */
    anchor: IntRect,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!open) return
    // The rule is arithmetic in pixels, so the gap is converted once, here,
    // where the density is known.
    val density = LocalDensity.current
    val gap = with(density) { PopupGap.roundToPx() }
    // The popup may take no more than the ground above the capsule, less the
    // gap, so the position rule below never has to clamp: a popup free to
    // grow on a short window was pushed down until the ground was gone
    // (owner decision). What does not fit scrolls.
    val room = with(density) { popupRoom(anchor.top, gap).toDp() }
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        popupPositionProvider = PillMenuPosition(anchor, gap),
    ) {
        Column(
            modifier = Modifier
                .width(PillMenuMeasure)
                .heightIn(max = room)
                .shadow(elevation = PillLift, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .testTag("pill-popup")
                .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

/**
 * Where the pill's popup stands: centred on the anchor, rising above the
 * capsule's own top edge with [gap] of ground between, and never off the
 * screen on either side.
 *
 * The anchor is a box, and the box's own bounds are read here rather than left
 * to the library: this is the one place in the app that says where a popup
 * goes, and it says it about the capsule. The arithmetic is the whole of it,
 * which is why [PillMenuPositionTest] can pin it on a machine with no screen:
 * a popup draws in a window of its own, and that window's place on the glass
 * cannot be read back through a compose test, so the rule itself is what
 * carries the promise (owner report).
 */
internal class PillMenuPosition(
    private val anchor: IntRect,
    /** [PopupGap] in pixels, converted where the density is known. */
    private val gap: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchor.center.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        // The capsule floats at the foot of the reading, so its popup rises
        // above the capsule's top edge with the gap between rather than
        // dropping over the capsule and the bar below it. The clamp is the
        // rule's own floor: [PillPopup] already keeps the popup within the
        // room above the gap, so this only answers a caller that did not.
        val y = (anchor.top - gap - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}