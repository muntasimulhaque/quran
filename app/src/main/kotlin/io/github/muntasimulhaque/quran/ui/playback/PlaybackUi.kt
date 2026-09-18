package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

/** The reciters, by the names readers use for them. */
fun shortReciterName(id: String, fallback: String): String = when (id) {
    "minshawi" -> "Minshawi"
    "husary" -> "Husary"
    "husary-muallim" -> "Husary Muallim"
    "husary-mujawwad" -> "Husary Mujawwad"
    else -> fallback
}

/** Human sizes for a download the reader is about to approve. */
fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 * 1024 -> "${(bytes + 512) / 1024} KB"
    else -> "%.0f MB".format(bytes / 1048576.0)
}

/**
 * The playback pill. It speaks in four voices: asking to download a surah,
 * reporting progress, reporting a failure, and playing. Downloading only ever
 * starts from the reader's own tap on Download, and only for one surah.
 */
@Composable
fun PlaybackBar(
    state: PlaybackUiState,
    reciterName: String,
    reference: String?,
    pendingLabel: String?,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReciter: () -> Unit,
    onDownload: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloading = state.downloadProgress != null
    val needsDownload = state.pendingDownloadSurah != null && !downloading
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
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
                    text = when {
                        state.downloadFailed -> "Download failed"
                        downloading -> "Downloading ${((state.downloadProgress ?: 0f) * 100).toInt()}%"
                        needsDownload -> pendingLabel.orEmpty()
                        state.unavailable -> "Not available yet"
                        else -> reference.orEmpty()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.padding(horizontal = 6.dp))
            when {
                state.downloadFailed -> PillText("Retry", onDownload)
                downloading -> TransportButton(Transport.Close, "Cancel download", onClose)
                needsDownload -> PillText("Download", onDownload)
                state.unavailable -> TransportButton(Transport.Close, "Close", onClose)
                else -> {
                    TransportButton(Transport.Previous, "Previous ayah", onPrevious)
                    TransportButton(if (state.isPlaying) Transport.Pause else Transport.Play, "Play or pause", onToggle)
                    TransportButton(Transport.Next, "Next ayah", onNext)
                    TransportButton(Transport.Close, "Stop", onClose)
                }
            }
        }
        if (downloading) {
            LinearProgressIndicator(
                progress = { state.downloadProgress ?: 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            )
        }
    }
}

@Composable
private fun PillText(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
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
        androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
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

/** One reciter, and how much of the Quran is already on the device for it. */
data class ReciterSummary(
    val recitation: Recitation,
    val downloadedSurahs: Int,
    val downloadedBytes: Long,
)

/** One downloaded surah, offered for removal. */
data class DownloadedSurah(val surah: Int, val name: String, val bytes: Long)

/**
 * The recitation sheet: the reciters, and the surahs already downloaded for
 * the selected one. Removing a surah deletes only its files and only for this
 * reciter; nothing else on the device is touched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationSheet(
    recitations: List<Recitation>,
    selected: String,
    summaries: suspend () -> List<ReciterSummary>,
    downloads: suspend (String) -> List<DownloadedSurah>,
    onSelect: (String) -> Unit,
    onRemove: suspend (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { sheetState.expand() }
    var refresher by remember { mutableIntStateOf(0) }
    val reciterRows by produceState(initialValue = emptyList<ReciterSummary>(), refresher) {
        value = summaries()
    }
    val downloadedRows by produceState(initialValue = emptyList<DownloadedSurah>(), selected, refresher) {
        value = downloads(selected)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = "Reciter",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
            )
            reciterRows.forEach { summary ->
                val recitation = summary.recitation
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
                            text = if (summary.downloadedSurahs == 0) {
                                "Nothing downloaded yet"
                            } else {
                                "${summary.downloadedSurahs} surahs  ·  ${formatBytes(summary.downloadedBytes)}"
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
            if (downloadedRows.isNotEmpty()) {
                Text(
                    text = "Downloaded",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 4.dp),
                )
                downloadedRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatBytes(row.bytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.padding(horizontal = 6.dp))
                        Text(
                            text = "Remove",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    scope.launch {
                                        onRemove(row.surah)
                                        refresher++
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
