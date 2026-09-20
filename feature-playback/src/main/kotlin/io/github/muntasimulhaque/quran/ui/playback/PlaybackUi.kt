package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph

/**
 * The playback pill. It speaks in four voices: asking to download a surah,
 * reporting progress, reporting a failure, and playing. Downloading only ever
 * starts from the reader's own tap on Download, and only for one surah.
 */
@Composable
fun PlaybackBar(
    state: PlaybackUiState,
    offer: ListenOffer?,
    offerTitle: String,
    reciterName: String,
    reference: String?,
    pendingLabel: String?,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReciter: () -> Unit,
    onDownload: () -> Unit,
    onClose: () -> Unit,
    onOfferConfirm: () -> Unit = {},
    onOfferCancel: () -> Unit = {},
    onOfferReciter: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (offer != null) {
        ListenOfferBar(
            offer = offer,
            title = offerTitle,
            onConfirm = onOfferConfirm,
            onCancel = onOfferCancel,
            onReciter = onOfferReciter,
            modifier = modifier,
        )
        return
    }
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
                        state.downloadFailed -> stringResource(R.string.playback_download_failed)
                        downloading -> stringResource(
                            R.string.playback_downloading,
                            ((state.downloadProgress ?: 0f) * 100).toInt(),
                        )
                        needsDownload -> pendingLabel.orEmpty()
                        state.unavailable -> stringResource(R.string.playback_unavailable)
                        else -> reference.orEmpty()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.padding(horizontal = 6.dp))
            when {
                state.downloadFailed -> PillText(stringResource(R.string.playback_retry), onDownload)
                downloading -> TransportButton(
                    Transport.Close,
                    stringResource(R.string.playback_cancel_download),
                    onClose,
                )
                needsDownload -> {
                    PillText(
                        stringResource(
                            if (state.pendingIsContinuation) R.string.playback_continue else R.string.playback_download,
                        ),
                        onDownload,
                    )
                    // A request the reader has not answered is not a trap:
                    // the same close that ends playback takes the offer away,
                    // so nothing sits over the reading until it is answered.
                    TransportButton(
                        Transport.Close,
                        stringResource(R.string.playback_close_offer),
                        onClose,
                    )
                }
                state.unavailable -> TransportButton(
                    Transport.Close,
                    stringResource(R.string.playback_close),
                    onClose,
                )
                else -> {
                    TransportButton(
                        Transport.Previous,
                        stringResource(R.string.playback_previous_ayah),
                        onPrevious,
                    )
                    TransportButton(
                        if (state.isPlaying) Transport.Pause else Transport.Play,
                        stringResource(R.string.playback_play_pause),
                        onToggle,
                    )
                    TransportButton(
                        Transport.Next,
                        stringResource(R.string.playback_next_ayah),
                        onNext,
                    )
                    TransportButton(
                        Transport.Close,
                        stringResource(R.string.playback_stop),
                        onClose,
                    )
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
            .minimumInteractiveComponentSize()
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
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
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


/**
 * One request, named in full: which reciter, which surah, how much, and the
 * reciter is changeable without leaving the offer. One tap downloads the word
 * timings and the audio together, under one progress bar, and then it plays.
 */
@Composable
private fun ListenOfferBar(
    offer: ListenOffer,
    title: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onReciter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var chooser by remember { mutableStateOf(false) }
    val progress = offer.progress
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = progress == null, role = Role.Button) { chooser = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = offer.reciterName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        // The name is the door to the other reciters; the
                        // mark says so, so the choice is found without a
                        // guess, and it goes away once the download starts.
                        if (progress == null) {
                            IconGlyph(
                                icon = Icon.Chevron,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(14.dp),
                            )
                        }
                    }
                    Text(
                        text = when {
                            offer.failed -> stringResource(R.string.playback_download_failed)
                            progress != null -> stringResource(
                                R.string.playback_downloading,
                                (progress * 100).toInt(),
                            )
                            else -> title
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = chooser, onDismissRequest = { chooser = false }) {
                    offer.options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = if (option.bytes > 0L) {
                                            formatBytes(option.bytes)
                                        } else {
                                            stringResource(R.string.playback_reciter_ready)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                chooser = false
                                onReciter(option.reciter)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.padding(horizontal = 6.dp))
            when {
                progress != null -> TransportButton(
                    Transport.Close,
                    stringResource(R.string.playback_cancel_download),
                    onCancel,
                )
                offer.failed -> PillText(stringResource(R.string.playback_retry), onConfirm)
                else -> {
                    PillText(stringResource(R.string.playback_download), onConfirm)
                    // An offer the reader does not want is not a trap: the
                    // same close that cancels a download takes the offer
                    // away, so nothing sits over the reading unasked.
                    TransportButton(
                        Transport.Close,
                        stringResource(R.string.playback_close_offer),
                        onCancel,
                    )
                }
            }
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
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
