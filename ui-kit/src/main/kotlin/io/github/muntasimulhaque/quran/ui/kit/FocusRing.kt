package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The ring a control wears while a keyboard or a switch has it.
 *
 * Every control in the app draws its press on the shape the reader sees, and
 * the app turns Material's own indication off on the click node so the ripple
 * cannot paint a rectangle over a pill. That also turned off the platform's
 * focus state: a Tab on a tablet moved from one control to the next without a
 * mark on either. This modifier reads the focus interactions from the same
 * [InteractionSource] the click already owns and draws the accent's 2 dp ring
 * on the shape, so the control a keyboard lands on is the control the eye
 * sees.
 *
 * No touch, no ring: focus interactions are keyboard and accessibility
 * focus, not a finger's press, so the app looks exactly as it did for a
 * reader with no keyboard attached (owner decision, this session).
 */
@Composable
fun Modifier.focusRing(
    source: InteractionSource,
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 2.dp,
): Modifier {
    val focused by source.collectIsFocusedAsState()
    return if (focused) this.border(width, color, shape) else this
}
