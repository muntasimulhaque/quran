package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.kit.formatBytes

/**
 * The reciters, as the list both the playing pill and the download offer
 * carry.
 *
 * One list, in one shape, wherever the choice is offered: the playing pill
 * carries it beside the reciter it is hearing, and the offer carries it above
 * the size it is asking for, and a reader who learned one has learned both.
 * A reciter who is already chosen wears the app's one leading radio mark, so
 * the pill and the Reciters page draw the same control and a screen reader
 * hears the chosen row as the chosen one; every other row says what that
 * reciter would still need for the surah at hand, so the choice is made with
 * the price in view rather than by trying it (owner decision, forty-eighth
 * session).
 *
 * The rows are drawn at the pill's own [PillMenuMeasure], which is also the
 * measure of the anchor the playing pill hangs them from, so the list stands
 * on the capsule's centre rather than beside the word that opened it (owner
 * report).
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
            title = option.name,
            subtitle = if (option.bytes > 0L) {
                formatBytes(option.bytes)
            } else {
                stringResource(R.string.playback_reciter_ready)
            },
            selected = option.reciter == selected,
            radio = true,
            onClick = { onChoose(option.reciter) },
            // The pill's own cloth, as the listening menu wears it: the row
            // clips into the popup's rounded shape.
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
    }
}
