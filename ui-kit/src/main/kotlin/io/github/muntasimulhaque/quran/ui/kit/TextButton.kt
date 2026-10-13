package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Wash
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The app's one text-only button: a word that acts, worn in a rounded shape
 * of the theme's own quiet fill.
 *
 * The shape is the distinction the app keeps. A heading, a title, or a name
 * is bare type; a control is a shape the finger can see, and it says so
 * before it is tapped. Without it, "Save" and "Note" read as the same kind of
 * word on the same line, and the reader has to guess which one answers a
 * touch; with it, the reader never has to.
 *
 * Two tones, one shape: the primary tone carries what the reader came to do
 * (Save, Add, Retry, Download), and the quiet tone carries what ends or
 * removes (Clear, Cancel, Remove).
 *
 * The click and the node a screen reader focuses own a full 48 dp slab,
 * while the shape the eye reads is drawn on the pill inside it. The ripple
 * draws on the pill too, not on the slab, so the press never flashes outside
 * the shape the reader is looking at. The touch target is the slab, not a
 * modifier outside the click: the accessibility test measures the node the
 * platform exposes, and that node has to be the whole target, not a wrapper
 * around it (owner decision, this session).
 */
@Composable
fun TextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quiet: Boolean = false,
) {
    val fill = if (quiet) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = Wash.Button)
    }
    val ink = if (quiet) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clickable(
                interactionSource = press,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(Radii.Pill)
                .background(fill)
                .indication(press, ripple(color = ink))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = ink,
            )
        }
    }
}
