package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.core.toggled
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.ui.kit.SpeedSteps
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.theme.appSwitchColors

/**
 * The pace, and what happens at the end of the audio, as the list the pill
 * opens from its own middle, in the pill's own cloth: the same floating tone
 * and rounded shape the reciter chooser wears, so it reads as the pill opening
 * rather than a foreign sheet laid over it.
 *
 * The pace keeps the settings page's own shape: the label is a bare line at
 * the menu's left inset and the five paces stand centred under it, as they
 * stand on the Listening page (owner decision, forty-seventh session).
 *
 * The four answers are four switches over one value (owner decision): each
 * reports the answer it carries, and the plan the app keeps is that answer
 * alone, so the reader can never be in a state where the ayah repeats and the
 * surah also repeats. The default, continuing to the next ayah, is the one
 * switch that is on with nothing else, and turning it off is the stop, which
 * needs no switch of its own.
 */
@Composable
internal fun ColumnScope.ListeningMenu(
    speed: Float,
    end: EndOfAudio,
    onSpeed: (Float) -> Unit,
    onEndOfAudio: (EndOfAudio) -> Unit,
) {
    // The label is the popup's first line, so it keeps the top inset the
    // reciter chooser's first row already opens with: with no room over the
    // leading the label sat against the popup's own top edge at a small
    // fraction of that inset (owner report). The room sits over the leading,
    // and the label still stands nearer the paces it names than the top edge.
    Text(
        text = stringResource(R.string.playback_speed_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 6.dp),
    )
    // One row, five paces, the chosen one filled: the same shape the
    // Listening page draws, so one control is learned once. It stands
    // centred on the menu rather than against its left edge, where it left
    // 10 dp of ground on one side and 28 on the other and read as a row that
    // had slipped (owner decision, forty-seventh session).
    Row(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .testTag("playback-speed")
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        SpeedSteps.forEach { step ->
            val active = kotlin.math.abs(step - speed) < 0.01f
            val description = stringResource(
                R.string.playback_speed_option,
                speedText(step),
                stringResource(R.string.playback_speed_label),
            )
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .selectable(selected = active, role = Role.RadioButton) { onSpeed(step) }
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = speedText(step),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
    EndSwitch(
        label = stringResource(R.string.playback_continue_ayah),
        choice = EndOfAudio.CONTINUE_AYAH,
        chosen = end,
        onEndOfAudio = onEndOfAudio,
    )
    // The surah, the same answer one unit larger: the next surah is fetched
    // with the reciter being heard and plays on. Off, the end of the surah is
    // offered with its size instead of fetched.
    EndSwitch(
        label = stringResource(R.string.playback_continue_surah),
        choice = EndOfAudio.CONTINUE_SURAH,
        chosen = end,
        onEndOfAudio = onEndOfAudio,
    )
    // The ayah again: the player's own loop, the answer that never advances.
    EndSwitch(
        label = stringResource(R.string.playback_repeat_ayah),
        choice = EndOfAudio.REPEAT_AYAH,
        chosen = end,
        onEndOfAudio = onEndOfAudio,
    )
    // The surah, the same answer one unit larger: the whole surah begins
    // again at its first ayah that is on the device (owner decision).
    EndSwitch(
        label = stringResource(R.string.playback_repeat_surah),
        choice = EndOfAudio.REPEAT_SURAH,
        chosen = end,
        onEndOfAudio = onEndOfAudio,
    )
}

/**
 * One of the answers the pill's menu carries: the label takes the room, the
 * switch is the app's own control in the theme's colors for this ground, and
 * the whole row is the target.
 *
 * The switch draws the state and takes no clicks of its own, because the row
 * is the control and a second target inside it would answer one tap twice. It
 * is Material's switch rather than a mark drawn here, so that every switch in
 * the app is one shape in one set of colors: the pill's menu and the settings
 * sheet ask the same question in two places, and they answer it with the
 * same control (owner decision, forty-sixth session). The menu keeps the
 * settings page's rhythm too: the row's own 10 dp over 2 dp of outer room
 * leaves 24 dp from one switch track to the next, the same band the four keep
 * on the Listening page, instead of the 32 dp the menu used to spend (owner
 * decision, forty-seventh session).
 */
@Composable
private fun EndSwitch(
    label: String,
    choice: EndOfAudio,
    chosen: EndOfAudio,
    onEndOfAudio: (EndOfAudio) -> Unit,
) {
    val on = chosen == choice
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .toggleable(value = on, role = Role.Switch) { onEndOfAudio(toggled(it, choice)) }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = on,
            onCheckedChange = null,
            colors = appSwitchColors(),
        )
    }
}
