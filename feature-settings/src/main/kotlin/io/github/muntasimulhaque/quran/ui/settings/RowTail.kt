package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.AppSwitch
import io.github.muntasimulhaque.quran.ui.kit.SheetTitle
import io.github.muntasimulhaque.quran.ui.kit.AppChevron
import io.github.muntasimulhaque.quran.ui.kit.ChevronRole
import io.github.muntasimulhaque.quran.ui.kit.PageHead
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space

/** The head of a page the reader opened: one way back, one name, and the
 * page's own switch when it has one. */
@Composable
fun PageHeader(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    PageHead(
        onBack = onBack,
        backDescription = stringResource(R.string.settings_back),
    ) {
        SheetTitle(
            text = title,
            // The page's own name, in the app's one title voice; the air
            // beside the back mark belongs to this head, and the name takes
            // the room a trailing switch leaves.
            modifier = Modifier.padding(start = Space.Tight).weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * A switch in a page's head, for the page whose first row used to be its own
 * title with the switch beside it.
 *
 * The switch is a drawing (see [AppSwitch]); the target is this box, the same
 * 48 dp every control owes the finger, and the box is named because the page's
 * title beside it is a name and not a label for the switch. A round press, not
 * the sheet's row card: a head is not a row (owner decision, this build).
 */
@Composable
fun HeadToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    description: String,
) {
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .padding(start = Space.Line)
            .minimumInteractiveComponentSize()
            .clip(Radii.Pill)
            .focusRing(press, Radii.Pill)
            .toggleable(
                value = checked,
                interactionSource = press,
                indication = ripple(bounded = false),
                role = Role.Switch,
                onValueChange = onChange,
            )
            .semantics { contentDescription = description }
            .padding(horizontal = Space.Line),
        contentAlignment = Alignment.Center,
    ) {
        AppSwitch(checked = checked)
    }
}

/**
 * The mark a settings row ends with: the chevron, when the row opens a page.
 *
 * The chevron is a picture of where a tap goes and not a control of its own.
 * The whole row is the door, so the row is one stop to a screen reader and the
 * mark takes no name. A row that also carried a switch used to read as a pair
 * of marks; the hub carries no switch now, so a row's last mark is either the
 * chevron or the switch, never both (owner decision, this session).
 *
 * The slot is 48 dp wide and a width and not a height: a square tail is taller
 * than the line of text beside it, and a Row takes the height of its tallest
 * child, so a square stretched every plain row and turned the hub into a
 * ladder (owner report). The row owes the finger its 48 dp from its own target.
 *
 * The mark stands at the end of its slot, so its right edge is the row's own
 * margin: that is exactly where a switch's right edge stands in the row below
 * it, and the two kinds of tail keep one line down the sheet.
 */
@Composable
internal fun RowTail(chevron: Boolean) {
    if (!chevron) return
    Box(
        modifier = Modifier
            .width(48.dp)
            // The slot, not the mark: a mark is centred in its slot, so the
            // slot's edges are what a row's alignment is about. The tag is on
            // a row that draws a chevron only, so a count of them is a count
            // of the rows that have a door (owner report).
            .testTag("chevron-slot"),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AppChevron(
            role = ChevronRole.Next,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("row-chevron"),
        )
    }
}
