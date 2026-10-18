package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.ui.kit.DownloadLine
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.reader.GlyphSize
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * One request, named in full: which reciter, which surah, how much, and the
 * reciter is changeable without leaving the offer. One tap downloads the word
 * timings and the audio together, under one progress bar, and then it plays.
 *
 * The offer keeps its own single row whatever the width, and that is not an
 * oversight: it carries two controls, never four, and the one line it prints
 * is a name and a size.
 *
 * The name and the size lead and the state's answers hold the capsule's
 * trailing rail, the close outermost: the offer is as wide as the page's own
 * measure, and that width is used rather than hugged (owner report, this
 * session).
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
    val press = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .floatingLift(Radii.Pill)
            .clip(Radii.Pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag("listen-offer-bar"),
    ) {
        // The chooser hangs from the bar's own centre with its ground above,
        // exactly as the chooser on the playing pill does: one list opens the
        // same way in both of its homes, instead of the framework's menu,
        // which stood on the bar's own edge and met its shoulder (owner
        // decision).
        PillMenuAnchor { anchor ->
            PillPopup(
                open = chooser,
                anchor = anchor,
                onDismiss = { chooser = false },
            ) {
                ReciterChoices(
                    options = offer.options,
                    selected = offer.reciter,
                    onChoose = {
                        chooser = false
                        onReciter(it)
                    },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    // The click owns the 48 dp node, the ripple draws on the
                    // capsule inside it, and the mark keeps its gap from its
                    // word: the same order the playing pill's reciter door is
                    // built in, so one tap flashes one shape in the two homes
                    // of one chooser (owner decision, the theme and
                    // consistency session).
                    .clickable(
                        enabled = progress == null,
                        role = Role.Button,
                        interactionSource = press,
                        indication = null,
                    ) { chooser = true }
                    .minimumInteractiveComponentSize()
                    .clip(Radii.Pill)
                    .indication(press, ripple(color = MaterialTheme.colorScheme.primary))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reciterShortName(offer.reciter, offer.reciterName),
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
                                .size(GlyphSize.ChevronInline),
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
            // The state's answers hold the capsule's trailing rail, the close
            // outermost, and the words give way to them with an ellipsis; the
            // 12 dp lead keeps the two apart when the name is long. The offer
            // used to hug its leading corner and leave the far end of the
            // capsule empty (owner report, this session).
            Row(
                modifier = Modifier.padding(start = WordsGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                        // An offer the reader does not want is not a trap:
                        // the same close that cancels a download takes the
                        // offer away, so nothing sits over the reading
                        // unasked.
                        TransportButton(
                            Transport.Close,
                            stringResource(R.string.playback_close_offer),
                            onCancel,
                        )
                    }
                }
            }
        }
        if (progress != null) {
            // The same drawn line the playing pill wears, so a download is
            // one shape whatever pill carries it (owner decision, this
            // session).
            DownloadLine(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .height(2.dp),
            )
        }
    }
}
