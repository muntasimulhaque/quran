package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.core.chosenBy
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.ui.kit.SpeedSteps
import io.github.muntasimulhaque.quran.ui.kit.speedText

/**
 * The pace, and what happens at the end of the audio, in the pill's own
 * cloth: the same floating tone and rounded shape the reciter chooser wears,
 * so it reads as the pill opening rather than a foreign sheet laid over it.
 *
 * It is the pill's own [PillMenuMeasure], which is also the measure of the
 * anchor the playing pill hangs it from, so the menu stands on the capsule's
 * centre rather than beside the word that opened it (owner report).
 *
 * The three end answers are three switches over one value
 * (owner decision): each reports the answer it carries, and the plan
 * the app keeps is that answer alone, so the reader can never be in a state
 * where the ayah repeats and the surah also repeats.
 */
@Composable
internal fun ListeningMenu(
    open: Boolean,
    onDismiss: () -> Unit,
    speed: Float,
    end: EndOfAudio,
    onSpeed: (Float) -> Unit,
    onEndOfAudio: (EndOfAudio) -> Unit,
) {
    DropdownMenu(
        expanded = open,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.width(PillMenuMeasure),
    ) {
        Text(
            text = stringResource(R.string.playback_speed_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
        )
        // One row, five paces, the chosen one filled: the same shape the
        // Listening page draws, so one control is learned once.
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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
            label = stringResource(R.string.playback_repeat_ayah),
            choice = EndOfAudio.REPEAT_AYAH,
            chosen = end,
            onEndOfAudio = onEndOfAudio,
        )
        // The surah, the same answer one unit larger: the ayah that has just
        // ended begins again, and so does the whole surah, at its first ayah
        // that is on the device (owner decision).
        EndSwitch(
            label = stringResource(R.string.playback_repeat_surah),
            choice = EndOfAudio.REPEAT_SURAH,
            chosen = end,
            onEndOfAudio = onEndOfAudio,
        )
        // The end of the surah, where the two above are the end of the ayah.
        // On, the next surah is fetched with the reciter being heard and
        // plays on; off, the pill offers it with its size.
        EndSwitch(
            label = stringResource(R.string.playback_continue_next),
            choice = EndOfAudio.CONTINUE,
            chosen = end,
            onEndOfAudio = onEndOfAudio,
        )
    }
}

/**
 * One of the three end answers, as a switch the pill drew itself: the label
 * takes the room, the app's own mark says the state, and the whole row is
 * the target.
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
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .toggleable(value = on, role = Role.Switch) { onEndOfAudio(chosenBy(it, choice)) }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        SwitchMark(checked = on)
    }
}

/** The app's own switch mark: a rounded track and a knob, drawn by hand. */
@Composable
private fun SwitchMark(checked: Boolean) {
    val track = if (checked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    }
    val knob = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface
    Canvas(Modifier.size(width = 38.dp, height = 22.dp)) {
        val corner = size.height / 2f
        drawRoundRect(
            color = track,
            cornerRadius = CornerRadius(corner),
        )
        val radius = size.height / 2f - 2.dp.toPx()
        val cx = if (checked) size.width - corner else corner
        drawCircle(color = knob, radius = radius, center = Offset(cx, corner))
    }
}
