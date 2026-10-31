package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The room a full-width row in a sheet keeps from the sheet's own edge.
 *
 * A row that answered a tap with a rectangle drawn from edge to edge touched
 * the boundary it stood on, the same report the pill's own menus drew (owner
 * report, this session). The row's press is a card inset by this much, so the
 * highlight never reaches the sheet's left or right edge.
 */
val SheetRowGutter = 8.dp

/** The room between two full-width rows, so two cards never meet. */
private val SheetRowGap = 2.dp

/**
 * A full-width row in a sheet that answers a tap.
 *
 * The click owns the whole row, the way every other control in the app does,
 * and the press is drawn on a rounded card inside it, inset from the sheet's
 * own edge by [SheetRowGutter], so the highlight never reaches the boundary
 * (owner report, this session: the plain clickable drew a rectangle from edge
 * to edge). The row's own content padding is applied by the caller, after
 * this, so the words and marks stay where they were and only the shape the
 * press paints moves.
 *
 * The card is inset by [SheetRowGutter] on each side, so a caller that wants
 * its words to keep the app's own gutter passes `Space.Gutter -
 * SheetRowGutter` for the horizontal padding: the gutter and the padding add
 * up to the one the rest of the sheet stands on.
 *
 * A row that answers a tap is a button unless it says otherwise. The role
 * used to be the caller's to remember, so some rows announced themselves as
 * buttons and some as bare clickable text; a door should sound like a door
 * wherever it stands (owner decision, this session).
 */
@Composable
fun Modifier.sheetRow(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button,
): Modifier {
    val press = remember { MutableInteractionSource() }
    return this
        .clickable(
            interactionSource = press,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
        .sheetRowSurface(press)
}

/**
 * The same row, as a switch: the whole row turns the setting on or off. One
 * shape with [sheetRow], so a settings row reads the same whether it opens a
 * page or carries a switch.
 */
@Composable
fun Modifier.sheetToggleRow(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true,
): Modifier {
    val press = remember { MutableInteractionSource() }
    return this
        .toggleable(
            value = value,
            interactionSource = press,
            indication = null,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onValueChange,
        )
        .sheetRowSurface(press)
}

/** The inset card and its ripple, shared by the two click shapes above. */
@Composable
private fun Modifier.sheetRowSurface(press: MutableInteractionSource): Modifier =
    this
        .padding(horizontal = SheetRowGutter, vertical = SheetRowGap)
        .clip(Radii.Card)
        .indication(press, ripple())
