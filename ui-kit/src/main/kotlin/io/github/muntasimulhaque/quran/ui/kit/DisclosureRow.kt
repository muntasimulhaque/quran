package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.github.muntasimulhaque.quran.ui.theme.ControlMetrics
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.uikit.R

/** The shape a disclosure row's press is drawn on. */
enum class DisclosurePress {
    /** A sheet row's rounded card, inset from the sheet's edge. */
    Card,

    /** The filter header's capsule, which hangs to the left of its words. */
    Pill,
}

/**
 * One row that shows or hides content in place: a word, its state said out
 * loud, and the app's one chevron.
 *
 * The row owns the whole control: one click, one focus ring, one press shape,
 * and one spoken state. The chevron is [AppChevron]'s [ChevronRole.Expand],
 * so it points down while the content is hidden and up once it is shown. The
 * state is spoken in the words every door shares, not left to the turn of a
 * drawing.
 *
 * The content slot is a [RowScope], so a caller can give its words the row's
 * remaining width and push the chevron to the edge, or leave the chevron
 * beside a short label. The tint is the app's one rule: quiet at rest, the
 * action accent while the content is open.
 *
 * [Card] is a sheet row's press: the same 14 dp card, 8 dp inset, and 48 dp
 * target every other row wears. [Pill] is the filter header's press: a
 * capsule inside the target, with the words on the page's own line. The
 * caller owns the outer padding and the words; this component owns the
 * control.
 */
@Composable
fun DisclosureRow(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    press: DisclosurePress = DisclosurePress.Card,
    contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val expandedText = stringResource(R.string.door_expanded)
    val collapsedText = stringResource(R.string.door_collapsed)
    val base = when (press) {
        DisclosurePress.Card -> modifier
            .fillMaxWidth()
            .sheetRow(onClick = onToggle)
            .minimumInteractiveComponentSize()
        DisclosurePress.Pill -> modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onToggle,
            )
            .minimumInteractiveComponentSize()
            .clip(Radii.Pill)
            .focusRing(source, Radii.Pill)
            .indication(
                source,
                ripple(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
    }
    val pad = contentPadding ?: when (press) {
        DisclosurePress.Card -> PaddingValues(
            start = Space.Gutter - SheetRowGutter,
            end = Space.Gutter - SheetRowGutter,
            top = Space.Row,
            bottom = Space.Row,
        )
        DisclosurePress.Pill -> PaddingValues(
            horizontal = Space.Row,
            vertical = Space.Line,
        )
    }
    Row(
        modifier = base
            .semantics { stateDescription = if (expanded) expandedText else collapsedText }
            .padding(pad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        AppChevron(
            role = ChevronRole.Expand,
            expanded = expanded,
            tint = if (expanded) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(start = ControlMetrics.ChevronGap),
        )
    }
}
