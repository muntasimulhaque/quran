package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.kit.SpeedSteps
import io.github.muntasimulhaque.quran.ui.kit.StepRow
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The pace, and what happens at the end of the audio, as the list the pill
 * opens from its own middle, in the pill's own cloth: the same floating tone
 * and rounded shape the reciter chooser wears, so it reads as the pill opening
 * rather than a foreign sheet laid over it.
 *
 * The pace keeps the app's own step row: the label is a bare line at the
 * menu's left inset and the five paces stand centred under it, as the text
 * sizes do on their own page (owner decision, forty-seventh session). The
 * row itself is [StepRow], the app's one step row, so one control is learned
 * once (owner decision, this session: the menu had carried a second copy and
 * the two had drifted).
 *
 * The answers are one value and one list (owner decision, 4.5): the five
 * answers are rows of the app's own [ChoiceRow], one radio mark leading each
 * name, so the exclusivity is the shape of the control rather than a rule the
 * reader has to learn, and the stop has its own row rather than living at the
 * off end of another. The order is the reader's own: what happens when the
 * ayah ends, then what happens when the surah ends. The five keep one gap and
 * the same leading mark the settings lists draw, so the same answer wears one
 * shape whichever door opened its list (owner decision, forty-eighth session).
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
        modifier = Modifier.padding(start = Space.Gutter, top = 12.dp, end = 16.dp, bottom = 6.dp),
    )
    // One row, five paces, the chosen one filled: the app's own step row, the
    // same the text sizes wear, so one control is learned once. It stands
    // centred on the menu rather than against its left edge, where it left
    // 10 dp of ground on one side and 28 on the other and read as a row that
    // had slipped (owner decision, forty-seventh session). The row itself
    // came from the settings page's old copy and the two had drifted, one
    // filling its whole cell and this one hugging its text (owner decision,
    // this session: one StepRow, one shape).
    StepRow(
        steps = SpeedSteps,
        value = speed,
        onSelect = onSpeed,
        cell = { index, active ->
            Text(
                text = speedText(SpeedSteps[index]),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        description = { step ->
            stringResource(
                R.string.playback_speed_option,
                speedText(step),
                stringResource(R.string.playback_speed_label),
            )
        },
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(horizontal = 10.dp)
            .testTag("playback-speed"),
    )
    Text(
        text = stringResource(R.string.playback_end_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Space.Gutter, top = 14.dp, end = 16.dp, bottom = 6.dp),
    )
    // When the ayah ends: the normal continuation, saying it again, and the
    // stop, in the pill the reader is already holding.
    EndChoice(
        label = stringResource(R.string.playback_continue_ayah),
        choice = EndOfAudio.CONTINUE_AYAH,
        chosen = end,
        onEndOfAudio = onEndOfAudio,
    )
    EndChoice(
        label = stringResource(R.string.playback_repeat_ayah),
        choice = EndOfAudio.REPEAT_AYAH,
        chosen = end,
        onEndOfAudio = onEndOfAudio,
    )
    EndChoice(
        label = stringResource(R.string.playback_stop_after_ayah),
        choice = EndOfAudio.STOP_AFTER_AYAH,
        chosen = end,
        onEndOfAudio = onEndOfAudio,
    )
    // When the surah ends: the same two answers one unit larger. The next
    // surah is fetched with the reciter being heard and plays on. The five
    // stand in one list with one gap; the words carry the difference between
    // the contexts, not the spacing (owner decision, forty-eighth session).
    EndChoice(
        label = stringResource(R.string.playback_continue_surah),
        choice = EndOfAudio.CONTINUE_SURAH,
        chosen = end,
        onEndOfAudio = onEndOfAudio,
    )
    EndChoice(
        label = stringResource(R.string.playback_repeat_surah),
        choice = EndOfAudio.REPEAT_SURAH,
        chosen = end,
        onEndOfAudio = onEndOfAudio,
    )
}

/**
 * One answer in the pill's one list: the same row the settings lists draw,
 * so the pill and the pages keep one shape, one leading mark, and one gap. A
 * radio row rather than a switch, because one answer is kept at a time; the
 * stop has its own row so the reader never learns it as the off position of
 * another.
 */
@Composable
private fun EndChoice(
    label: String,
    choice: EndOfAudio,
    chosen: EndOfAudio,
    onEndOfAudio: (EndOfAudio) -> Unit,
) {
    ChoiceRow(
        title = label,
        subtitle = null,
        selected = chosen == choice,
        radio = true,
        onClick = { onEndOfAudio(choice) },
    )
}
