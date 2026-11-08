package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.playback.ListenOption

/** The body of the pill's one popup: the full listening panel for a
 * recitation, and the voice list alone for a pill that has none. */
@Composable
internal fun ColumnScope.PillMenuBody(
    openMenu: PillMenu?,
    options: List<ListenOption>,
    selected: String?,
    speed: Float,
    end: EndOfAudio,
    onDismiss: () -> Unit,
    onReciter: (String) -> Unit,
    onSpeed: (Float) -> Unit,
    onEndOfAudio: (EndOfAudio) -> Unit,
) {
    when (openMenu) {
        // The whole of the listening, for a recitation
        // that exists.
        PillMenu.Listening -> ListeningPanel(
            options = options,
            selected = selected,
            // Every answer puts the panel away: one
            // question was asked, and the tap answered it
            // (owner decision, this session). The voice
            // list has always closed on its choice, and
            // the pace and the end of the audio follow it
            // now, so the reader is back on the reading
            // rather than in a panel they have to dismiss.
            onChoose = {
                onDismiss()
                onReciter(it)
            },
            speed = speed,
            end = end,
            onSpeed = {
                onDismiss()
                onSpeed(it)
            },
            onEndOfAudio = {
                onDismiss()
                onEndOfAudio(it)
            },
        )
        // The voices alone, for a voice that has no
        // recitation to set a pace for.
        PillMenu.Reciters -> ReciterChoices(
            options = options,
            selected = selected,
            onChoose = {
                onDismiss()
                onReciter(it)
            },
        )
        null -> Unit
    }
}
