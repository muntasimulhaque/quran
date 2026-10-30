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
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.SpeedSteps
import io.github.muntasimulhaque.quran.ui.kit.StepRow
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The whole of what a reader can change about the recitation, in one panel:
 * who is reading, how fast, and what happens when the audio ends.
 *
 * The voices lead, because the block that opens the panel leads with the
 * reciter's name; the pace and the answers follow, in the order the listening
 * menu has always kept. One panel rather than two menus (owner decision, this
 * session: the block is one door, and the three questions are one subject).
 * The panel is only opened by a recitation that exists: an offer, a failure,
 * and an unavailable answer show the voice list alone, because the pace and
 * the end are answers to a recitation, and theirs has not begun (owner
 * decision, this session).
 *
 * It may stand taller than the two menus did apart, so the popup caps it to
 * the ground above the capsule and what does not fit scrolls, as every popup
 * of the pill's already does. The voices keep the app's choice row, with the
 * leading mark and what that voice would still need for the surah at hand, so
 * a reciter is chosen with its price in view.
 *
 * **Every answer puts the panel away** (owner decision, this session). The
 * voice list has always closed on its choice, and the pace and the ending
 * follow it. One panel asked one question, and the tap answered it, so the
 * reader is back on the reading rather than in a panel they have to dismiss.
 * The cost is that a reader changing two things opens the door twice, and
 * that the ending has no read-back on the pill, which by a settled rule does
 * not write the ending down; the row's own mark was on screen when the tap
 * landed.
 */
@Composable
internal fun ColumnScope.ListeningPanel(
    options: List<ListenOption>,
    /** The reciter in use, by id; null where none of them is. */
    selected: String?,
    onChoose: (String) -> Unit,
    speed: Float,
    end: EndOfAudio,
    onSpeed: (Float) -> Unit,
    onEndOfAudio: (EndOfAudio) -> Unit,
) {
    if (options.isNotEmpty()) {
        MenuLabel(stringResource(R.string.playback_reciter_label))
        ReciterChoices(options = options, selected = selected, onChoose = onChoose)
    }
    ListeningMenu(speed = speed, end = end, onSpeed = onSpeed, onEndOfAudio = onEndOfAudio)
}

/**
 * One section's name inside a pill popup: the panel's first line, the label
 * over the pace, and the label over the answers.
 *
 * It wears the app's one head over a list, [SectionHead], so the panel's
 * sections read as groups of choices rather than as rows of their own: a label
 * at the size of the rows under it made the merged panel twelve lines of one
 * voice, with no hierarchy to lean the air between them on (owner report, this
 * session). It is a heading to a screen reader as well, so a reader who cannot
 * see the panel can jump from the voices to the pace to the ending.
 *
 * The air is this popup's own, not the settings sheet's: the panel keeps the
 * popup's 22 dp gutters and a line's room above it, where the hub opens a
 * group with a block's. The head still stands nearer the control it names than
 * the space above it.
 */
@Composable
private fun MenuLabel(text: String) {
    SectionHead(
        text = text,
        modifier = Modifier.padding(
            start = Space.Gutter,
            top = Space.Row,
            end = Space.Gutter,
            bottom = Space.Tight,
        ),
    )
}

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
    // reciter chooser's first row already opens with. The room sits over the
    // leading, and the label still stands nearer the paces it names than the
    // top edge.
    MenuLabel(stringResource(R.string.playback_speed_label))
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
                // The theme's own small label, without a one-off 12 sp
                // override, so a theme change reaches it (owner decision,
                // the theme and consistency session).
                style = MaterialTheme.typography.labelSmall,
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
    MenuLabel(stringResource(R.string.playback_end_label))
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
