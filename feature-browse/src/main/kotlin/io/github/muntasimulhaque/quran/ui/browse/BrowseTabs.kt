package io.github.muntasimulhaque.quran.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.browse.R
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash

internal enum class BrowseTab(val labelRes: Int) {
    Surahs(R.string.browse_tab_surahs),
    Juz(R.string.browse_tab_juz),
    LastRead(R.string.browse_tab_last_read),
    Saved(R.string.browse_tab_saved),
}

/**
 * One tab as a chip: the reader taps a chip, so the tab row is the same kind
 * of control as the search filters, and one choice among four is read the same
 * way wherever the app asks for it.
 */
@Composable
internal fun BrowseTabChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The chip's own shape stays as small as it reads; the click and the
    // accessibility node carry the app's 48 dp target, because a tab is a
    // control and a control that cannot be hit is not drawn at all to the
    // reader who missed it. The ripple is drawn on the pill inside, not on
    // the slab: the click node sits outside the clip, so an indication left
    // on it would flash a rectangle over the target instead of the shape
    // the reader is looking at (the same rule TextButton wears).
    val press = remember { MutableInteractionSource() }
    val ink = if (active) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            // selectable, not clickable: a chosen tab must say it is chosen
            // to TalkBack the way the app's other single choices do (owner
            // decision, the theme and consistency session).
            .selectable(
                selected = active,
                interactionSource = press,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(Radii.Pill)
                .background(
                    if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)
                    },
                )
                .indication(press, ripple(color = ink))
                .padding(horizontal = Space.Row, vertical = Space.Line),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = ink,
            )
        }
    }
}
