package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The listening page: how fast the recitation plays, and what happens as the
 * ayah or the surah being heard moves on. Both are about hearing, not about
 * the page, so they sit together under the reciter whose voice they shape.
 *
 * The answers are one value and one list (owner decision, 4.5): the page
 * draws the five answers as rows with one radio mark, so the exclusivity is
 * the shape of the control rather than a rule the reader has to learn. The
 * stop is its own row, because the state with no answer at all is an answer
 * the reader should be able to name. The order is the reader's own: what
 * happens when the ayah ends, then what happens when the surah ends.
 *
 * The two rows about the ayah and the two about the surah are the two groups
 * the reader thinks in, and the pill opens the same four in the same order,
 * from its own listening menu, calling the one value kept here.
 */
@Composable
fun ListeningPage(
    settings: AppSettings,
    onSpeed: (Float) -> Unit,
    onEndOfAudio: (EndOfAudio) -> Unit,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_speed))
        SpeedRow(value = settings.playbackSpeed, onChange = onSpeed)
        Spacer(Modifier.height(Space.Section))
        Group(stringResource(R.string.settings_group_end_of_audio))
        // When the ayah ends: the normal continuation, saying it again, and
        // the stop, with the stop its own row rather than the off state of
        // another. The three are what happens in front of the reader.
        EndChoice(
            title = stringResource(R.string.settings_continue_ayah_title),
            choice = EndOfAudio.CONTINUE_AYAH,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
        )
        EndChoice(
            title = stringResource(R.string.settings_repeat_title),
            choice = EndOfAudio.REPEAT_AYAH,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
        )
        EndChoice(
            title = stringResource(R.string.settings_stop_after_ayah_title),
            choice = EndOfAudio.STOP_AFTER_AYAH,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
        )
        Spacer(Modifier.height(Space.Block))
        // When the surah ends: the same two answers one unit larger. The
        // next surah is fetched with the reciter being heard and plays on.
        EndChoice(
            title = stringResource(R.string.settings_continue_surah_title),
            choice = EndOfAudio.CONTINUE_SURAH,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
        )
        EndChoice(
            title = stringResource(R.string.settings_repeat_surah_title),
            choice = EndOfAudio.REPEAT_SURAH,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
        )
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * One answer in the one list: a radio, because one answer is kept at a time.
 * The stop is a row of its own, so the reader never has to learn that the
 * default switch's off position was the stop.
 */
@Composable
private fun EndChoice(
    title: String,
    choice: EndOfAudio,
    end: EndOfAudio,
    onEndOfAudio: (EndOfAudio) -> Unit,
) {
    ChoiceRow(
        title = title,
        subtitle = null,
        selected = end == choice,
        onClick = { onEndOfAudio(choice) },
    )
}
