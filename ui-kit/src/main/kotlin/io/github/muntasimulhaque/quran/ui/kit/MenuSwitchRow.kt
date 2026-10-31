package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.appSwitchColors

/**
 * A row in the app's one popup that turns a reading setting on or off.
 *
 * It is [MenuActionRow]'s sibling and shares its rhythm to the dp: the same
 * [RowGutter] to the name's rail, the same [Space.Row] inside the capsule,
 * the same 48 dp target, the same [CapsuleGutter] and [CapsulePad], and the
 * same 12 dp foot, so the display panel's switches and its All settings row
 * stand on one rail (owner decision, this session).
 *
 * The whole row is the switch, and the switch itself is a picture of the
 * row's state: a finger that lands anywhere on the name flips it, and the
 * control is one TalkBack stop rather than a name and a switch beside it. It
 * is the same shape the settings hub's switch rows use when they have no page
 * behind them, so a reader who learned one has learned the other.
 *
 * A switch the reader cannot reach is dimmed rather than hidden: the display
 * panel has no pack list to send them to, so the row stays where it is and
 * the All settings row at its foot is the way to add what is missing.
 *
 * The switch's 48 dp target takes no height of its own: it is laid out on the
 * row's middle and reaches into the row's own padding, the same geometry the
 * settings hub's switch rows use, so a switch row is 48 dp and not the 74 a
 * stacked target would make it (owner decision, this session).
 */

/** The 48 dp a switch owes the finger; the row's height does not pay it. */
private val SwitchTarget = 48.dp
@Composable
fun MenuSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** False while the pack the switch draws has nothing on the device. */
    enabled: Boolean = true,
    /** The room under the row; a row that ends a list keeps the popup's own. */
    bottomPadding: Dp = 12.dp,
    /** The anchor a test taps when it means this row. */
    tag: String? = null,
) {
    val press = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = press,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            // Inside the click, so the target the finger gets is the full
            // 48 dp the minimum holds and not the row's own shorter box.
            .minimumInteractiveComponentSize()
            .padding(
                start = RowGutter,
                end = RowGutter,
                top = CapsuleGutter,
                bottom = (bottomPadding - CapsulePad).coerceAtLeast(0.dp),
            )
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clip(Radii.Pill)
            .indication(press, ripple())
            .padding(
                start = Space.Row,
                end = Space.Row,
                top = CapsulePad,
                bottom = CapsulePad,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(end = Space.Row),
        )
        Box(
            modifier = Modifier.layout { measurable, constraints ->
                val target = SwitchTarget.roundToPx().coerceAtMost(constraints.maxHeight)
                val placeable = measurable.measure(
                    constraints.copy(minHeight = maxOf(constraints.minHeight, target)),
                )
                layout(placeable.width, 0) {
                    placeable.place(0, -placeable.height / 2)
                }
            },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = appSwitchColors(),
            )
        }
    }
}
