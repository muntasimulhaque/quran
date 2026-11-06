package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.ControlMetrics
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * What a chip is choosing.
 *
 * A tab changes the view of a surface, a toggle turns one source on or off,
 * and a radio picks one of two or three answers. The role is the reader's,
 * not the drawing's: a screen reader hears a tab, a checkbox, or a radio
 * button, and the chip keeps one shape for all three.
 */
enum class ChipKind { Tab, Toggle, Radio }

/**
 * How a chosen chip says it is chosen.
 *
 * [Place] is the solid accent: the tab the reader is on, or the one answer a
 * small set keeps. [Toggle] is the accent's own tint with the check beside
 * the word: a source turned on, where several may be on and seven solid pills
 * would read as seven pressed buttons.
 */
enum class ChipStyle { Place, Toggle }

/**
 * The app's one chip, wherever a word is chosen from a row of words.
 *
 * The pill is [chipMeasure]'s own: the label's line plus [ControlMetrics.PillPad]
 * above and below, so it is 36 dp at the default type and grows with the
 * system font scale. The click and the node a screen reader focuses carry the
 * app's 48 dp floor, so the eye gets a chip and the finger gets a target.
 *
 * The shape is a capsule and the press is drawn on it, not on the invisible
 * target: the same rule every control in the app wears. [ChipKind] decides
 * whether the chip is a tab, a checkbox, or a radio; [ChipStyle] decides
 * whether the chosen state is a place or a toggle. The two are separate so a
 * tab and a filter cannot drift into two different heights or two different
 * press shapes while keeping a deliberate difference in tone.
 *
 * The flow that holds a row of chips must use [chipMeasure]'s `rowGap`, not a
 * fixed number: at 200 percent the pill and the target are the same height,
 * and a zero gap lets two rows touch (owner decision, the consistency audit).
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ChipKind = ChipKind.Toggle,
    style: ChipStyle = if (kind == ChipKind.Toggle) ChipStyle.Toggle else ChipStyle.Place,
) {
    val press = remember { MutableInteractionSource() }
    val ink = when {
        selected && style == ChipStyle.Place -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val fill = when {
        selected && style == ChipStyle.Place -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = Wash.Control)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)
    }
    val selection = when (kind) {
        ChipKind.Tab -> Modifier.selectable(
            selected = selected,
            interactionSource = press,
            indication = null,
            role = Role.Tab,
            onClick = onClick,
        )
        ChipKind.Toggle -> Modifier.toggleable(
            value = selected,
            interactionSource = press,
            indication = null,
            role = Role.Checkbox,
            onValueChange = { onClick() },
        )
        ChipKind.Radio -> Modifier.selectable(
            selected = selected,
            interactionSource = press,
            indication = null,
            role = Role.RadioButton,
            onClick = onClick,
        )
    }
    Row(
        modifier = modifier
            .then(selection)
            .minimumInteractiveComponentSize()
            .clip(Radii.Pill)
            .focusRing(press, Radii.Pill)
            .background(fill)
            .indication(press, ripple(color = ink))
            .padding(horizontal = Space.Row, vertical = ControlMetrics.PillPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Line),
    ) {
        if (selected && style == ChipStyle.Toggle) {
            IconGlyph(
                icon = Icon.Check,
                tint = ink,
                modifier = Modifier.size(GlyphSize.Check),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = ink,
        )
    }
}
