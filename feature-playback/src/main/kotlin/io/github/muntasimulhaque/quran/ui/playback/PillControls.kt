package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.kit.TextButton

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

/** Hand-drawn transport controls, one shape each, at the app's 48 dp target. */
@Composable
internal fun TransportButton(kind: Transport, description: String, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            when (kind) {
                Transport.Play -> drawPath(
                    Path().apply {
                        moveTo(w * 0.24f, h * 0.14f)
                        lineTo(w * 0.86f, h * 0.5f)
                        lineTo(w * 0.24f, h * 0.86f)
                        close()
                    },
                    tint,
                )
                Transport.Pause -> {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.24f, h * 0.14f),
                        size = Size(w * 0.18f, h * 0.72f),
                        cornerRadius = CornerRadius(w * 0.06f),
                    )
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.58f, h * 0.14f),
                        size = Size(w * 0.18f, h * 0.72f),
                        cornerRadius = CornerRadius(w * 0.06f),
                    )
                }
                Transport.Next -> {
                    drawPath(
                        Path().apply {
                            moveTo(w * 0.14f, h * 0.16f)
                            lineTo(w * 0.62f, h * 0.5f)
                            lineTo(w * 0.14f, h * 0.84f)
                            close()
                        },
                        tint,
                    )
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.66f, h * 0.16f),
                        size = Size(w * 0.14f, h * 0.68f),
                        cornerRadius = CornerRadius(w * 0.05f),
                    )
                }
                Transport.Previous -> {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.20f, h * 0.16f),
                        size = Size(w * 0.14f, h * 0.68f),
                        cornerRadius = CornerRadius(w * 0.05f),
                    )
                    drawPath(
                        Path().apply {
                            moveTo(w * 0.86f, h * 0.16f)
                            lineTo(w * 0.38f, h * 0.5f)
                            lineTo(w * 0.86f, h * 0.84f)
                            close()
                        },
                        tint,
                    )
                }
                Transport.Close -> {
                    val stroke = w * 0.11f
                    drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), stroke)
                    drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), stroke)
                }
            }
        }
    }
}
