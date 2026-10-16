package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.reader.GlyphSize
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * One control in the pill's own row: a glyph the app drew by hand, or a word
 * that answers a tap.
 *
 * They are one list because the row's shape is a function of how many of
 * them there are (owner report): the words either share the row with
 * the controls or take a line of their own, and a count read from a second
 * `when` beside this one is a number that drifts from the row it measures.
 */
internal sealed interface PillControl

/** A glyph control: the shape, what TalkBack says, and what it does. */
internal class GlyphControl(
    val kind: Transport,
    val description: String,
    val onClick: () -> Unit,
) : PillControl

/** A word control: the app's own `TextButton`, for the answers that are words. */
internal class WordControl(
    val label: String,
    val onClick: () -> Unit,
) : PillControl

/**
 * The two popups the pill's own words open, named so the capsule can say
 * which one is standing: the reciter's name opens the chooser, and the place
 * under it opens the pace and the end of the audio.
 *
 * The capsule draws both, from one anchor on its own centre, so the reader's
 * eye is sent to one place on the control rather than to two (owner report).
 */
internal enum class PillMenu { Reciters, Listening }

/**
 * What the pill offers in the state it is in.
 *
 * Downloading only ever starts from the reader's own tap, the offer states
 * answer with a word and a close, and only a recitation being heard carries
 * the four transport glyphs. That count is what the words have to share the
 * row with, so it is read from here rather than counted a second time.
 */
@Composable
internal fun pillControls(
    state: PlaybackUiState,
    downloading: Boolean,
    needsDownload: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDownload: () -> Unit,
    onClose: () -> Unit,
): List<PillControl> = when {
    // A player that could not play and a download that could not land are two
    // failures of the same shape: one sentence and the one action that can
    // help. The reader is never left with a silent pill.
    state.error -> listOf(
        WordControl(stringResource(R.string.playback_retry), onDownload),
        GlyphControl(Transport.Close, stringResource(R.string.playback_close), onClose),
    )
    state.downloadFailed -> listOf(
        WordControl(stringResource(R.string.playback_retry), onDownload),
    )
    downloading -> listOf(
        GlyphControl(Transport.Close, stringResource(R.string.playback_cancel_download), onClose),
    )
    needsDownload -> listOf(
        WordControl(
            stringResource(
                if (state.pendingIsContinuation) {
                    R.string.playback_continue
                } else {
                    R.string.playback_download
                },
            ),
            onDownload,
        ),
        // A request the reader has not answered is not a trap: the same
        // close that ends playback takes the offer away, so nothing sits
        // over the reading until it is answered.
        GlyphControl(Transport.Close, stringResource(R.string.playback_close_offer), onClose),
    )
    state.unavailable -> listOf(
        GlyphControl(Transport.Close, stringResource(R.string.playback_close), onClose),
    )
    else -> listOf(
        GlyphControl(Transport.Previous, stringResource(R.string.playback_previous_ayah), onPrevious),
        GlyphControl(
            if (state.isPlaying) Transport.Pause else Transport.Play,
            stringResource(R.string.playback_play_pause),
            onToggle,
        ),
        GlyphControl(Transport.Next, stringResource(R.string.playback_next_ayah), onNext),
        GlyphControl(Transport.Close, stringResource(R.string.playback_stop), onClose),
    )
}

/** The pill's controls, in the order a reader's hand expects them. */
@Composable
internal fun PillControls(
    controls: List<PillControl>,
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.Start,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        controls.forEach { control ->
            when (control) {
                is GlyphControl -> TransportButton(control.kind, control.description, control.onClick)
                is WordControl -> TextButton(label = control.label, onClick = control.onClick)
            }
        }
    }
}

/** The glyphs the transport row draws: one shape each, the app's own weight. */
internal enum class Transport { Play, Pause, Next, Previous, Close }

/**
 * The app's one drawing for each transport mark. The pill used to carry a
 * second hand-drawn set beside `IconGlyph`, so the same Play was two
 * triangles and the Close was two strokes; every mark now comes from the one
 * set the whole app draws (owner decision, the theme and consistency
 * session).
 */
private val Transport.icon: Icon
    get() = when (this) {
        Transport.Play -> Icon.Play
        Transport.Pause -> Icon.Pause
        Transport.Next -> Icon.Next
        Transport.Previous -> Icon.Previous
        Transport.Close -> Icon.Close
    }

/** Hand-drawn transport controls, one shape each, at the app's 48 dp target. */
@Composable
internal fun TransportButton(kind: Transport, description: String, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            // The click owns the 48 dp node; the glyph inside it stays 20 dp
            // (owner decision, the accessibility test). The ripple is drawn
            // on the pill inside the click node, not on the node itself: an
            // indication left on the click would flash a rectangle over the
            // shape the reader is looking at (the same rule TextButton
            // wears).
            .clickable(
                interactionSource = press,
                indication = null,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize()
            .clip(Radii.Pill)
            .indication(press, ripple(color = tint))
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        IconGlyph(
            icon = kind.icon,
            tint = tint,
            modifier = Modifier.size(GlyphSize.Transport),
        )
    }
}
