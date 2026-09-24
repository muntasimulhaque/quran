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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.SpeedSteps
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph

/**
 * The gutter a floating control keeps from the glass. A pill that reaches
 * the screen's own edge stops reading as a control over the page and starts
 * reading as a sheet the app forgot to inset (owner report, D-090).
 */
private val BarGutter = 16.dp

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
    /** The reader's pace, shown only when it is not the ordinary one. */
    speed: Float = 1f,
    /** True while the playing ayah repeats; the pill says so. */
    repeating: Boolean = false,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReciter: () -> Unit,
    onDownload: () -> Unit,
    onClose: () -> Unit,
    onOfferConfirm: () -> Unit = {},
    onOfferCancel: () -> Unit = {},
    onOfferReciter: (String) -> Unit = {},
    /** The pace and the repeat, set from the pill exactly as from settings. */
    onSpeed: (Float) -> Unit = {},
    onRepeat: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (offer != null) {
        ListenOfferBar(
            offer = offer,
            title = offerTitle,
            onConfirm = onOfferConfirm,
            onCancel = onOfferCancel,
            onReciter = onOfferReciter,
            modifier = modifier.padding(horizontal = BarGutter),
        )
        return
    }
    val downloading = state.downloadProgress != null
    val needsDownload = state.pendingDownloadSurah != null && !downloading
    val playing = !downloading && !needsDownload && !state.downloadFailed && !state.unavailable
    Column(
        modifier = modifier
            .padding(horizontal = BarGutter)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            // The playback bar floats over the reading, so it wears the
            // floating tone and a soft lift: the page is visible around it
            // and under it, and it has to read as above the page.
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag("playback-bar"),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The words are what give when the line is long, never the
            // gutter and never the controls: the reciter's name is one
            // line, the state under it at most two, and both ellipsize. A
            // surah's name may be shortened on the narrowest phone; the
            // size the reader is approving and the doors they answer with
            // may not.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onReciter)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text(
                    text = reciterName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PlaybackStatusLine(
                    text = when {
                        state.downloadFailed -> stringResource(R.string.playback_download_failed)
                        downloading -> stringResource(
                            R.string.playback_downloading,
                            ((state.downloadProgress ?: 0f) * 100).toInt(),
                        )
                        needsDownload -> pendingLabel.orEmpty()
                        state.unavailable -> stringResource(R.string.playback_unavailable)
                        else -> playbackStatus(reference.orEmpty(), speed, repeating)
                    },
                    // The line is the door to the pace and the repeat while
                    // an ayah is playing, and only then: the download states
                    // have nothing to hear yet. The same two values the
                    // Settings page owns, set from the moment they matter
                    // (owner decision, 28).
                    onListening = if (playing) onSpeed to onRepeat else null,
                    speed = speed,
                    repeating = repeating,
                )
            }
            Spacer(Modifier.padding(horizontal = 6.dp))
            when {
                state.downloadFailed -> TextButton(
                    label = stringResource(R.string.playback_retry),
                    onClick = onDownload,
                )
                downloading -> TransportButton(
                    Transport.Close,
                    stringResource(R.string.playback_cancel_download),
                    onClose,
                )
                needsDownload -> {
                    TextButton(
                        label = stringResource(
                            if (state.pendingIsContinuation) R.string.playback_continue else R.string.playback_download,
                        ),
                        onClick = onDownload,
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

/**
 * The pill's second line: what the reader is hearing, and, while a recitation
 * plays, the door to its pace and repeat. The door is the line itself and it
 * wears a chevron so it can be found without a guess; the chevron is gone
 * when there is nothing behind it.
 */
@Composable
private fun PlaybackStatusLine(
    text: String,
    onListening: Pair<(Float) -> Unit, (Boolean) -> Unit>?,
    speed: Float,
    repeating: Boolean,
) {
    // Read here, in the composable's own scope: a semantics lambda is not a
    // composable, and a stringResource inside one is a compile error.
    val repetition = stringResource(R.string.playback_repeating)
    var open by remember { mutableStateOf(false) }
    if (onListening == null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button) { open = true }
                .padding(end = 2.dp)
                .testTag("playback-listening")
                .semantics {
                    contentDescription = text
                    val spoken = buildList {
                        if (kotlin.math.abs(speed - 1f) > 0.01f) add(speedText(speed))
                        if (repeating) add(repetition)
                    }.joinToString(", ")
                    if (spoken.isNotEmpty()) stateDescription = spoken
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(12.dp),
            )
        }
        ListeningMenu(
            open = open,
            onDismiss = { open = false },
            speed = speed,
            repeating = repeating,
            onSpeed = onListening.first,
            onRepeat = onListening.second,
        )
    }
}

/**
 * The pace and the repeat, in the pill's own cloth: the same floating tone
 * and rounded shape the reciter chooser wears, so it reads as the pill
 * opening rather than a foreign sheet laid over it.
 */
@Composable
private fun ListeningMenu(
    open: Boolean,
    onDismiss: () -> Unit,
    speed: Float,
    repeating: Boolean,
    onSpeed: (Float) -> Unit,
    onRepeat: (Boolean) -> Unit,
) {
    DropdownMenu(
        expanded = open,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.widthIn(min = 232.dp),
    ) {
        Text(
            text = stringResource(R.string.playback_speed_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
        )
        // One row, five paces, the chosen one filled: the same shape the
        // Listening page draws, so one control is learned once.
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(3.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(1.dp),
        ) {
            SpeedSteps.forEach { step ->
                val active = kotlin.math.abs(step - speed) < 0.01f
                val description = stringResource(
                    R.string.playback_speed_option,
                    speedText(step),
                    stringResource(R.string.playback_speed_label),
                )
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            },
                        )
                        .selectable(selected = active, role = Role.RadioButton) { onSpeed(step) }
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = speedText(step),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .toggleable(value = repeating, role = Role.Switch, onValueChange = onRepeat)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.playback_repeat_ayah),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            SwitchMark(checked = repeating)
        }
    }
}

/** The app's own switch mark: a rounded track and a knob, drawn by hand. */
@Composable
private fun SwitchMark(checked: Boolean) {
    val track = if (checked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    }
    val knob = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface
    androidx.compose.foundation.Canvas(Modifier.size(width = 38.dp, height = 22.dp)) {
        val corner = size.height / 2f
        drawRoundRect(
            color = track,
            cornerRadius = CornerRadius(corner),
        )
        val radius = size.height / 2f - 2.dp.toPx()
        val cx = if (checked) size.width - corner else corner
        drawCircle(color = knob, radius = radius, center = Offset(cx, corner))
    }
}

/** The segmented row hands out steps; the pill edits the same float setting. */
private enum class Transport { Play, Pause, Next, Previous, Close }

/**
 * What the pill says under the reciter's name while an ayah plays: where the
 * reader is, and, only when they are not the ordinary ones, the pace and the
 * repeat. A reader who set 1.5x a week ago and forgot, or who turned repeat
 * on and then wondered why the reading would not move on, reads the answer
 * here instead of hunting through settings for it.
 */
@Composable
private fun playbackStatus(reference: String, speed: Float, repeating: Boolean): String {
    val extras = buildList {
        if (kotlin.math.abs(speed - 1f) > 0.01f) {
            add(speedText(speed))
        }
        if (repeating) {
            add(stringResource(R.string.playback_repeating))
        }
    }
    return (listOf(reference) + extras).filter { it.isNotBlank() }.joinToString(" · ")
}

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
 * One reciter in the offer's chooser: the name, what still needs fetching,
 * and a check on the one already chosen. It is rounded and it is the pill's
 * own surface, so the choice reads as part of the pill it opened from.
 */
@Composable
private fun ReciterChoiceRow(
    option: ListenOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = option.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
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
        if (selected) {
            IconGlyph(
                icon = Icon.Check,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(18.dp),
            )
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
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f, fill = false)) {
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // The chooser wears the pill's own cloth: the same surface
                // color, the same rounded shape of its own, and no depth the
                // pill does not have. The pill carries no border and casts no
                // shadow, so a menu that did read as a foreign sheet laid over
                // it; matching both exactly is what makes it read as the pill
                // opening. The reciter in use carries the check.
                DropdownMenu(
                    expanded = chooser,
                    onDismissRequest = { chooser = false },
                    shape = RoundedCornerShape(20.dp),
                    // The chooser wears the pill's own cloth: the same floating
                    // tone, the same rounded shape, and no depth the pill does
                    // not have, so it reads as the pill opening rather than a
                    // foreign sheet laid over it.
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.widthIn(min = 216.dp, max = 288.dp),
                ) {
                    offer.options.forEach { option ->
                        ReciterChoiceRow(
                            option = option,
                            selected = option.reciter == offer.reciter,
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
                offer.failed -> TextButton(
                    label = stringResource(R.string.playback_retry),
                    onClick = onConfirm,
                )
                else -> {
                    TextButton(
                        label = stringResource(R.string.playback_download),
                        onClick = onConfirm,
                    )
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
