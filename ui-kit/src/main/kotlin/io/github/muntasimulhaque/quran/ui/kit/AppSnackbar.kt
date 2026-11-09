package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The app's one Snackbar: the sentence a surface owes the reader when an act
 * has no visible result of its own.
 *
 * The reading and Browse each hold their own host state, because a Snackbar
 * belongs to the surface that raised it and the reading's is behind the
 * Browse sheet. What they do not each hold is the drawing: the shape, the
 * fill, and the ink were typed twice, so the two could drift the way the
 * app's failure sentences already had. There is one shape now, and the
 * surfaces differ only in where they hang it.
 *
 * The action is the app's own [TextButton], not Material's bare action text:
 * a snackbar is not the one place a word that acts stops wearing a shape.
 * The pill sits inside the same sentence, and Undo or Settings reads as the
 * one thing the reader can do without becoming a second voice.
 */
@Composable
fun AppSnackbar(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            shape = Radii.Card,
            // The app's one floating tone, the same the playback pill and the
            // offer wear: the sentence stands on the page it interrupts, one
            // step under its paper, rather than a shade of its own (owner
            // decision, this session).
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            action = data.visuals.actionLabel?.let { label ->
                {
                    TextButton(
                        label = label,
                        onClick = { data.performAction() },
                        pillTag = "snackbar-action-pill",
                    )
                }
            },
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
