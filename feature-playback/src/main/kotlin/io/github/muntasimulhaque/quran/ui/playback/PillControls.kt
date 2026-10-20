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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
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

/**
 * A glyph control: the shape, what TalkBack says, what it does, and the
 * actions TalkBack offers beside the tap.
 *
 * [actions] exists for the one thing the pill does that has no shape any
 * more: stopping the recitation. The close that used to carry it was a mark
 * the capsule paid for with its own width, and it has been taken off the
 * glass, so the stop now lives where a screen reader already is when it
 * reaches the transport (owner decision, this session).
 */
internal class GlyphControl(
    val kind: Transport,
    val description: String,
    val onClick: () -> Unit,
    val actions: List<CustomAccessibilityAction> = emptyList(),
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
 * answer with a word, and only a recitation being heard carries the
 * transport. That count is what the words have to share the row with, so it
 * is read from here rather than counted a second time.
 *
 * **The transport carries no close** (owner decision, this session). The
 * close was one mark with four meanings in one place, Stop, Close, Close,
 * and Cancel download, and the three that were not the transfer were already
 * answered by a tap on the page. What is left is one X on the one surface
 * that aborts something in flight: a package on its way, where a stray tap
 * on the page must never throw away the reader's data. So an X in this app
 * means "abort what is in flight", never "put this panel away", and the
 * panel is put away by the page, by Back, or by the state answering itself.
 *
 * The recitation's own stop is a TalkBack action on Play or pause: it is the
 * one thing the close did that nothing else on the glass does, and a reader
 * who cannot use the page tap must not be left without it. A reader who can
 * use the page tap pauses, then taps the page, which is the app's one
 * dismissal for everything it raises.
 *
 * The words' two doors and the transfer's one X stand in their row with the
 * app's air between them, so no press shape touches another or the capsule
 * (owner decision, this session).
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
    onStop: () -> Unit,
    onCancelDownload: () -> Unit,
): List<PillControl> = when {
    // A player that could not play and a download that could not land are two
    // failures of the same shape: one sentence and the one action that can
    // help. The reader is never left with a silent pill, and neither state
    // wears a close: a page tap, or Back, puts a failure away (owner
    // decision, this session).
    state.error -> listOf(
        WordControl(stringResource(R.string.playback_retry), onDownload),
    )
    state.downloadFailed -> listOf(
        WordControl(stringResource(R.string.playback_retry), onDownload),
    )
    // The one X the app keeps: a transfer in flight is the one thing a stray
    // tap must not end, so its abort keeps a shape of its own (owner
    // decision, this session).
    downloading -> listOf(
        GlyphControl(
            Transport.Close,
            stringResource(R.string.playback_cancel_download),
            onCancelDownload,
        ),
    )
    // A request the reader has not answered is not a trap: a page tap, or
    // Back, answers it with "not now", so the offer needs no mark of its own
    // (owner decision, this session).
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
    )
    state.unavailable -> emptyList()
    else -> listOf(
        GlyphControl(Transport.Previous, stringResource(R.string.playback_previous_ayah), onPrevious),
        GlyphControl(
            kind = if (state.isPlaying) Transport.Pause else Transport.Play,
            description = stringResource(R.string.playback_play_pause),
            onClick = onToggle,
            // The pill's own stop, offered to a screen reader where the mark
            // used to stand: the same word, the same act, and no shape for a
            // reader who has the page (owner decision, this session).
            actions = listOf(
                CustomAccessibilityAction(stringResource(R.string.playback_stop)) {
                    onStop()
                    true
                },
            ),
        ),
        GlyphControl(Transport.Next, stringResource(R.string.playback_next_ayah), onNext),
    )
}

/**
 * The pill's controls, in the order a reader's hand expects them, with the
 * app's air between the shapes it draws: the transport is a cluster of
 * same-kind small controls, so it stands `Space.Line` apart (owner decision,
 * this session).
 */
@Composable
internal fun PillControls(
    controls: List<PillControl>,
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.spacedBy(PillAir),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        controls.forEach { control ->
            when (control) {
                is GlyphControl -> TransportButton(
                    kind = control.kind,
                    description = control.description,
                    onClick = control.onClick,
                    actions = control.actions,
                )
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
internal fun TransportButton(
    kind: Transport,
    description: String,
    onClick: () -> Unit,
    /** What TalkBack offers beside the tap, where a gesture has no shape. */
    actions: List<CustomAccessibilityAction> = emptyList(),
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            // The click owns the 48 dp node; the glyph inside it stays 20 dp
            // (owner decision, the accessibility test). The ripple is drawn
            // on a shape of the node's own size, not on the 20 dp glyph: an
            // indication left on the glyph flashed a dot inside the target
            // the finger actually hit, which read as too small (owner
            // report, this session). The shape is the app's one control
            // capsule, which on this square target is the circle an icon
            // button wears, and it fills the target the way the node does.
            .clickable(
                interactionSource = press,
                indication = null,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize()
            .semantics {
                contentDescription = description
                role = Role.Button
                if (actions.isNotEmpty()) customActions = actions
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(TransportSize)
                .clip(Radii.Pill)
                .indication(press, ripple(color = tint))
                .testTag("transport-shape"),
            contentAlignment = Alignment.Center,
        ) {
            IconGlyph(
                icon = kind.icon,
                tint = tint,
                modifier = Modifier.size(GlyphSize.Transport),
            )
        }
    }
}
