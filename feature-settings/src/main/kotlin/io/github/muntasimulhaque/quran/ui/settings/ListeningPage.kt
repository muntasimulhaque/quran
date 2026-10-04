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
import io.github.muntasimulhaque.quran.core.chosenBy
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The listening page: how fast the recitation plays, and what happens when
 * the ayah or the surah being heard ends. Both are about hearing, not about
 * the page, so they sit together under the reciter whose voice they shape.
 *
 * The three ends are three switches over one value (owner decision).
 * Each row is the answer it names, checked only when it is the answer, and
 * turning one on leaves the other two off, because two of them on at once is
 * a promise the player cannot keep: a surah that repeats never ends, so the
 * continuation would never come.
 *
 * The pill opens the same two answers, from its own listening menu, and both
 * doors call the one value kept here.
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
        // Continue: what happens at the end of the surah, where repeat is
        // what happens at the end of the ayah. Turning it on is the reader's
        // word for the packages that follow, so the next surah needs no
        // second approval; it still announces itself on the pill with its
        // size and a cancel while it downloads (owner decision).
        EndRow(
            title = stringResource(R.string.settings_continue_title),
            choice = EndOfAudio.CONTINUE,
            end = settings.endOfAudio,
            onEndOfAudio = onEndOfAudio,
            switchTag = "switch-continue",
        )
        Spacer(Modifier.height(Space.Section))
    }
}

/** One of the three ends, as a switch row that reports only its own answer. */
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
        onChange = { onEndOfAudio(chosenBy(it, choice)) },
        switchTag = switchTag,
    )
}
