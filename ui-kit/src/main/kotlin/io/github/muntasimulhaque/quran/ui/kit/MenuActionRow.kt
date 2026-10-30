package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * One action a menu offers: a mark, its word, and the whole row as the tap.
 *
 * It is [ChoiceRow]'s sibling and shares its rhythm to the dp: the same
 * [RowGutter] to the mark's column, the same 22 dp mark at the app's 22 dp
 * gutter, the same [Space.Block] from the mark to the name, the same 48 dp
 * target, the same [CapsuleGutter] and [CapsulePad] around the capsule the
 * press draws, and the same 12 dp foot. A menu of answers and a menu of
 * actions can therefore stand in one app without the two lists' names
 * starting on two different lines (owner decision, this session). The
 * numbers are read from `ChoiceRow` rather than copied, so a later change to
 * the row's rhythm reaches both.
 *
 * The difference is the meaning of the leading column. A choice says which
 * one is kept, with a ring; an action says what the tap will do, with the
 * app's own mark, and it carries no state of its own except for the two
 * actions that are switches. Save and Note are those two: their mark and
 * their word take the accent while they are on, and [state] says the same
 * thing to a screen reader, so a lit mark is never the only way a reader
 * learns that an ayah is kept or noted (owner report).
 *
 * The press is drawn on the capsule inside the target, never on the target,
 * for the reason every other control in the app gives: the shape the reader
 * sees is the shape their finger gets, and a ripple left on the row would
 * flash a rectangle from one edge of the menu to the other.
 */
@Composable
fun MenuActionRow(
    icon: Icon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** True while the action is in its on state: Save on a kept ayah. */
    active: Boolean = false,
    /** What the mark says about itself, in the words of the module that owns it. */
    state: String? = null,
    /** The room under the row; a row that ends a list keeps the popup's own. */
    bottomPadding: Dp = 12.dp,
) {
    val press = remember { MutableInteractionSource() }
    val ink = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = press,
                indication = null,
                role = Role.Button,
                onClick = onClick,
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
            .then(
                if (state != null) {
                    Modifier.semantics { stateDescription = state }
                } else {
                    Modifier
                },
            )
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
        // The mark's column is the choice row's own 22 dp, so the two lists'
        // names stand on one leading rail.
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            IconGlyph(
                icon = icon,
                tint = ink,
                modifier = Modifier.size(GlyphSize.Action),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(start = Space.Block),
        )
    }
}
