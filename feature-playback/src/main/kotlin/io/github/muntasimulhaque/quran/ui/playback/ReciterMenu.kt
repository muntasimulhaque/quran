package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.ui.kit.PopupMeasure
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName

/**
 * The reciters, as the list the playing pill carries.
 *
 * One list, in one shape: the playing pill carries it at the head of its
 * listening panel, and the Reciters page draws the same choice in its rows. A
 * reciter who is already chosen wears the app's one leading radio mark, so
 * the pill and the page draw the same control and a screen reader hears the
 * chosen row as the chosen one; every other row says what that reciter would
 * still need for the surah at hand, so the choice is made with the price in
 * view rather than by trying it (owner decision, forty-eighth session).
 *
 * The download offer does not carry this list: its one line names the chosen
 * voice, and its height belongs to the two answers (owner decision, this
 * session).
 *
 * The rows fill the popup, which is drawn at the pill's own
 * [PopupMeasure], also the measure of the anchor the playing pill hangs it
 * from, so the list stands on the capsule's centre rather than beside the
 * word that opened it (owner report).
 */
@Composable
internal fun ReciterChoices(
    options: List<ListenOption>,
    /** The reciter in use, by id; null where none of them is. */
    selected: String?,
    onChoose: (String) -> Unit,
) {
    options.forEach { option ->
        ChoiceRow(
            title = reciterShortName(option.reciter, option.name),
            subtitle = if (option.bytes > 0L) {
                formatBytes(option.bytes)
            } else {
                stringResource(R.string.playback_reciter_ready)
            },
            selected = option.reciter == selected,
            radio = true,
            onClick = { onChoose(option.reciter) },
        )
    }
}
