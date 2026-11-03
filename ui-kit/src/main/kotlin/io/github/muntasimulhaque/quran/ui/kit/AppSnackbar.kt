package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
 */
@Composable
fun AppSnackbar(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = Radii.Card,
            // The one step above the sheet, so the sentence stands on the page
            // it interrupts rather than a shade of its own.
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            actionColor = MaterialTheme.colorScheme.primary,
        )
    }
}
