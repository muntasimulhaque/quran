package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * oversight: its words are a name and a size, and its answer is one word.
 *
 * **An offer has no close** (owner decision, this session). Its X answered
 * "not now" with exactly what a tap on the page answers, and the same X then
 * turned into the transfer's cancel one tap later, so one mark in one place
 * said two different things. A page tap puts the offer away, and Back answers
 * it with the same "not now"; the only X the bar draws is the cancel of a
 * package already on its way, which is the one thing a stray tap must not do
 * (owner decision, this session).
 *
 * The bar hugs its words for the same reason the pill does: the download is
 * a name, a size, and one answer, and a full-width shape spent the reader's
 * page on ground nothing stood on. Its row keeps [PillEndAir] at the words'
 * end, because a capsule's end is a sphere and at the name's line an [PillAir]
 * measured from its widest point reads as five dp: in Bangla the name looked
 * cut, where its matra reaches the top of its line (owner report, this
 * session). The answer's end keeps [PillAir], so the Download's own press
 * shape never meets the capsule's edge and the transfer's X keeps the air it
 * has always had, while the progress line stands in the room the bar keeps
 * under its row and never crosses a control's shape.
 */
@Composable
internal fun ListenOfferBar(
    offer: ListenOffer,
    title: String,
    onConfirm: () -> Unit,
    /** The one X the app keeps: abort a package on its way. */
    onCancelDownload: () -> Unit,
    onReciter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var chooser by remember { mutableStateOf(false) }
    val progress = offer.progress
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .floatingLift(Radii.Pill)
            .clip(Radii.Pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag("listen-offer-bar"),
        // The bar hugs its words, and its content stands on its own middle
        // whatever measure it is given.
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = if (progress != null) ProgressRoom else 0.dp),
        ) {
            Row(
                modifier = Modifier.padding(
                    start = PillEndAir,
                    end = PillAir,
                    top = PillAir,
                    bottom = PillAir,
                ),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        // The click owns the 48 dp node, the ripple draws on
                        // the capsule inside it, and the mark keeps its gap
                        // from its word: the same order the playing pill's
                        // reciter door is built in, so one tap flashes one
                        // shape in the two homes of one chooser (owner
                        // decision, the theme and consistency session).
                        .clickable(
                            enabled = progress == null,
                            role = Role.Button,
                            interactionSource = press,
                            indication = null,
                        ) { chooser = true }
                        .minimumInteractiveComponentSize()
                        .testTag("offer-words")
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
                // The state's one answer, and the words give way to it with an
                // ellipsis: the 12 dp lead keeps the name and the button apart
                // when the name is long. A request and a failure answer with a
                // word; a package on its way answers with the app's one X,
                // because a stray tap must never throw the transfer away.
                Row(
                    modifier = Modifier.padding(start = WordsGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        progress != null -> TransportButton(
                            Transport.Close,
                            stringResource(R.string.playback_cancel_download),
                            onCancelDownload,
                        )
                        offer.failed -> TextButton(
                            label = stringResource(R.string.playback_retry),
                            onClick = onConfirm,
                        )
                        else -> TextButton(
                            label = stringResource(R.string.playback_download),
                            onClick = onConfirm,
                        )
                    }
                }
            }
        }
        // The same drawn line the playing pill wears, so a download is one
        // shape whatever pill carries it (owner decision, this session). It
        // stands in the room kept under the row, on the bar's own measure,
        // which the words decide and the percent never does.
        if (progress != null) {
            Box(Modifier.matchParentSize()) {
                DownloadLine(
                    progress = progress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(2.dp)
                        .testTag("download-line"),
                )
            }
        }
        // The chooser hangs from the bar's own centre with its ground above,
        // exactly as the chooser on the playing pill does: one list opens the
        // same way in both of its homes, instead of the framework's menu,
        // which stood on the bar's own edge and met its shoulder (owner
        // decision).
        Box(Modifier.matchParentSize()) {
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
        }
    }
}
