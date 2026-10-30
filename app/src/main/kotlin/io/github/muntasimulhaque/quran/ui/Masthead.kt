package io.github.muntasimulhaque.quran.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.theme.amiriDisplay
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The frozen masthead: the name and its line, drawn once for the boot frame,
 * the language screen, and the reminder screen. It had three copies with two
 * sizes, two alphas, and two gaps, so the title visibly moved between the
 * first frame and the screen that replaced it (owner decision, the theme and
 * consistency session).
 */
@Composable
fun Masthead(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.first_paint_title),
            style = amiriDisplay(
                54f,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
            ),
        )
        Text(
            text = stringResource(R.string.first_paint_subtitle),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Line),
        )
    }
}
