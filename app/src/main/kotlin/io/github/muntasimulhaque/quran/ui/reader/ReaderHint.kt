package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.theme.Radii
import kotlinx.coroutines.delay

/**
 * The long press is the richest gesture in the app and the least visible, so
 * it is said once, quietly, and then never again. The note floats over the
 * reading and leaves on its own; a tap puts it away early, and either way the
 * day is written down (owner decision, this session: restored with the long
 * press).
 */
@Composable
internal fun BoxScope.LongPressHint(
    reducedMotion: Boolean,
    onSeen: () -> Unit,
) {
    var hintVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1400)
        hintVisible = true
        delay(8000)
        hintVisible = false
        onSeen()
    }
    AnimatedVisibility(
        visible = hintVisible,
        enter = if (reducedMotion) fadeIn() else fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 96.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .floatingLift(Radii.Card)
                .clip(Radii.Card)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable {
                    hintVisible = false
                    onSeen()
                }
                .minimumInteractiveComponentSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconGlyph(
                icon = Icon.More,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(GlyphSize.Action),
            )
            Text(
                text = stringResource(R.string.hint_long_press_ayah),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
