package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.kit.PopupMeasure

/**
 * The anchor hairline: a menu is placed from the bounds of the box it hangs
 * on, and a box with no height has no bounds to read, so a zero-height anchor
 * stands every popup at the window edge instead of the capsule centre. One dp
 * is the thinnest box that has a place on the page, and it draws nothing.
 */
private val AnchorLine = 1.dp

/**
 * The anchor the pill's own popup hangs from, on both of the bars it wears:
 * the capsule's own width, with the popup's measure centred on it and its top
 * edge as the line they drop from.
 *
 * The anchor and the popup are one measure on purpose, and the anchor is read
 * back to the caller so the popup is placed from the anchor's own place and
 * not from whatever the library would take from a box it cannot see the
 * bounds of (owner report). It draws nothing and takes one hairline of room,
 * because a box with no height has no bounds to read: a zero-height anchor
 * stands every popup at the window's own edge instead of the capsule's centre.
 * The playing capsule and the offer bar both stand it, so the menu opens from
 * the same place wherever it is asked.
 */
@Composable
internal fun PillMenuAnchor(content: @Composable (anchor: IntRect) -> Unit) {
    var anchor by remember { mutableStateOf(IntRect.Zero) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(PopupMeasure)
                .height(AnchorLine)
                .testTag("pill-menu-anchor")
                .onGloballyPositioned {
                    val box = it.boundsInWindow()
                    anchor = IntRect(box.left.toInt(), box.top.toInt(), box.right.toInt(), box.bottom.toInt())
                },
        ) { content(anchor) }
    }
}
