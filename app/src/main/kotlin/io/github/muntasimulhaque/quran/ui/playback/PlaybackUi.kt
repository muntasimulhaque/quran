package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.playback.PlaybackUiState

/** The reciters, by the names readers use for them. */
fun shortReciterName(id: String, fallback: String): String = when (id) {
    "minshawi" -> "Minshawi"
    "husary" -> "Husary"
    "husary-muallim" -> "Husary Muallim"
    "husary-mujawwad" -> "Husary Mujawwad"
    else -> fallback
}

/**
 * The playback pill: who is reciting, where the reader is, and the three
 * controls that matter. Tapping the reciter opens the picker; the close
 * control stops and clears.
 */
@Composable
fun PlaybackBar(
    state: PlaybackUiState,
    reciterName: String,
    reference: String?,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReciter: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onReciter)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(
                text = reciterName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (state.unavailable) "Not on this device" else reference.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.padding(horizontal = 6.dp))
        TransportButton(Transport.Previous, "Previous ayah", onPrevious)
        TransportButton(if (state.isPlaying) Transport.Pause else Transport.Play, "Play or pause", onToggle)
        TransportButton(Transport.Next, "Next ayah", onNext)
        TransportButton(Transport.Close, "Stop", onClose)
    }
}

private enum class Transport { Play, Pause, Next, Previous, Close }

/** Hand-drawn transport controls: two shapes each, the app's own weight. */
@Composable
private fun TransportButton(kind: Transport, description: String, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
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

/** The four recitations, with the one playing marked and the missing named. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationSheet(
    recitations: List<Recitation>,
    selected: String,
    availability: suspend () -> Map<String, Boolean>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val available by produceState(initialValue = emptyMap<String, Boolean>(), recitations) {
        value = availability()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = "Reciter",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
            )
            recitations.forEach { recitation ->
                val isAvailable = available[recitation.id] == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(recitation.id) }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = shortReciterName(recitation.id, recitation.name),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = when {
                                !isAvailable -> "Not on this device"
                                recitation.credit.isNotBlank() -> recitation.credit
                                else -> recitation.name
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (recitation.id == selected) {
                        Text(
                            text = "Playing",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
