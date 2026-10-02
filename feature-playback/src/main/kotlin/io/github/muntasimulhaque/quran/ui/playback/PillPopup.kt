package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * and rises above the capsule's top edge, the shape a menu opening out of a
 * capsule at the foot of a page is expected to have.
 *
 * It wears the pill's own surface and the pill's own rounded shape, and casts
 * no shadow, because the pill casts none and a popup that did read as a
 * foreign sheet laid over it. A tap outside puts it away, as a dropdown's does.
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
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        popupPositionProvider = PillMenuPosition(anchor),
    ) {
        Column(
            modifier = Modifier
                .width(PillMenuMeasure)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

/**
 * Where the pill's popup stands: centred on the anchor, rising above the
 * capsule's own top edge, and never off the screen on either side.
 *
 * The anchor is a box, and the box's own bounds are read here rather than left
 * to the library: this is the one place in the app that says where a popup
 * goes, and it says it about the capsule. The arithmetic is the whole of it,
 * which is why [PillMenuPositionTest] can pin it on a machine with no screen:
 * a popup draws in a window of its own, and that window's place on the glass
 * cannot be read back through a compose test, so the rule itself is what
 * carries the promise (owner report).
 */
internal class PillMenuPosition(private val anchor: IntRect) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchor.center.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        // The capsule floats at the foot of the reading, so its popup rises
        // out of the capsule's top edge rather than dropping over the capsule
        // and the bar below it. A popup too tall for the room above starts at
        // the top of the screen and scrolls, which is where the window's own
        // edge is the honest place to stop.
        val y = (anchor.top - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}