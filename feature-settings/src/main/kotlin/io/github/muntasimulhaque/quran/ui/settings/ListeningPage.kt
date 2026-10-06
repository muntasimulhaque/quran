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
import io.github.muntasimulhaque.quran.core.toggled
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The listening page: how fast the recitation plays, and what happens as the
 * ayah or the surah being heard moves on. Both are about hearing, not about
 * the page, so they sit together under the reciter whose voice they shape.
 *
 * The answers are four switches over one value (owner decision). Each row is
 * the answer it names, checked only when it is the answer, and turning one on
 * leaves the others off, because two of them on at once is a promise the
 * player cannot keep: a surah that repeats never ends, so the continuation
 * would never come. The default, continuing to the next ayah, is the one
 * switch that is on with nothing else, and turning it off is the stop, which
 * needs no switch of its own.
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
        Group(stringResource(R.string.settings_group_continue))
        // The default, and the stop: turning this one off is the only way to
        // a reading that stops when the ayah ends (owner decision).
        EndRow(
            title = stringResource(R.string.settings_continue_ayah_title),
            choice = EndOfAudio.CONTINUE_AYAH,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
            switchTag = "switch-continue-ayah",
        )
        // Continue to the next surah: the packages that follow need no second
        // approval, and the next surah still announces itself on the pill
        // with its size and a cancel while it downloads (owner decision).
        EndRow(
            title = stringResource(R.string.settings_continue_surah_title),
            choice = EndOfAudio.CONTINUE_SURAH,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
            switchTag = "switch-continue",
        )
        Spacer(Modifier.height(Space.Section))
        Group(stringResource(R.string.settings_group_repeat))
        EndRow(
            title = stringResource(R.string.settings_repeat_title),
            choice = EndOfAudio.REPEAT_AYAH,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
        )
        // The surah, the same answer one unit larger: the whole surah begins
        // again at its first ayah on the device, and nothing is fetched to do
        // it, so it is available wherever the surah is.
        EndRow(
            title = stringResource(R.string.settings_repeat_surah_title),
            choice = EndOfAudio.REPEAT_SURAH,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
        )
        Spacer(Modifier.height(Space.Section))
    }
}

/** One of the answers, as a switch row that reports only its own. */
@Composable
private fun EndRow(
    title: String,
    choice: EndOfAudio,
    end: EndOfAudio,
    onEndOfAudio: (EndOfAudio) -> Unit,
    switchTag: String? = null,
) {
    ToggleRow(
        title = title,
        subtitle = null,
        checked = end == choice,
        onChange = { onEndOfAudio(toggled(it, choice)) },
        switchTag = switchTag,
    )
}
