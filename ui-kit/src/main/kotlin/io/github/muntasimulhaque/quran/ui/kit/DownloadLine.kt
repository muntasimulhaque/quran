package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * The line a package's download is drawn on: a 2 dp hairline in the theme's
 * accent, filling from the start edge as the bytes land, drawn by hand like
 * every other shape in the app.
 *
 * The playing pill and the offer bar both wear it, so one download reads the
 * same whichever pill asked for it (owner decision, this session: the pill's
 * line had been drawn by hand and the offer bar still wore Material's own
 * progress, two shapes for one event).
 *
 * The caller owns the line's measure and place, so each pill keeps its own
 * inset from the shape's curve.
 */
@Composable
fun DownloadLine(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val filled = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .clip(Radii.Pill)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = Wash.Control)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(filled)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
