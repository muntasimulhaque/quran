package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

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
 * removes (Clear, Cancel, Remove). The touch target is 48 dp whatever the
 * pill's own height is.
 */
@Composable
fun TextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quiet: Boolean = false,
) {
    val fill = if (quiet) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    }
    val ink = if (quiet) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .background(fill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = ink,
        )
    }
}
