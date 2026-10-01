package io.github.muntasimulhaque.quran.ui.playback

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph

/**
 * One request, named in full: which reciter, which surah, how much, and the
 * reciter is changeable without leaving the offer. One tap downloads the word
 * timings and the audio together, under one progress bar, and then it plays.
 *
 * The offer keeps its own single row whatever the width, and that is not an
 * oversight: it carries two controls, never four, and the one line it prints
 * is a name and a size.
 */
@Composable
internal fun ListenOfferBar(
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
                        maxLines = 1,
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
