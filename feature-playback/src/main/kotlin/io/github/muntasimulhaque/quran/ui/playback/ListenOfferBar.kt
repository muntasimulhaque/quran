package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.DownloadError
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.ui.kit.AppPopup
import io.github.muntasimulhaque.quran.ui.kit.PopupAlignment
import io.github.muntasimulhaque.quran.ui.kit.DownloadLine
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * One request, named in full: which reciter, which surah, how much, and the
 * reciter is changeable without leaving the offer. One tap downloads the word
 * timings and the audio together, under one progress bar, and then it plays.
 *
 * The offer keeps its own single row whatever the width, and that is not an
 * oversight: its words are a name and a size, and its answer is one word. Its
 * words are the pill's own [WordsBlock], so the offer and the playing capsule
 * wear one shape: a line of type over a line, with one door and one answer.
 *
 * **Its door is the reciter chooser, alone** (owner decision, this session).
 * The playing pill's block opens the merged panel, with the pace and the end
 * of the audio, because a recitation exists to answer those questions. An
 * offer has no recitation yet, so the only question it can answer is which
 * voice to fetch, and a pace and an ending over a download that has not
 * started are answers before the question. The pill made the same choice
 * before it was played.
 *
 * **An offer has no close** (owner decision, another session). Its X answered
 * "not now" with exactly what a tap on the page answers, and the same X then
 * turned into the transfer's cancel one tap later, so one mark in one place
 * said two different things. A page tap puts the offer away, and Back answers
 * it with the same "not now"; the only X the bar draws is the cancel of a
 * package already on its way, which is the one thing a stray tap must not do.
 *
 * The bar stands at the pill's own [PillMeasure], in every state of the
 * request (owner decision, this session). It used to hug its words, which made
 * an offer and the transfer that followed it two different widths for one
 * download. Its row keeps [PillEndAir] at the words' end,
 * because a capsule's end is a sphere and at the name's line an [PillAir]
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
    Box(
        modifier = modifier
            // The caller's gutters come first, then the pill's one measure:
            // the offer is the same capsule as the playing pill and takes the
            // same width before its shape is drawn (owner decision, this
            // session).
            .widthIn(max = PillMeasure)
            .fillMaxWidth()
            .floatingLift(Radii.Pill)
            .clip(Radii.Pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag("listen-offer-bar"),
        // The bar stands at the pill's one measure, and its content stands on
        // its own middle whatever measure it is given. The measure is taken
        // before the shape so a narrower glass narrows the capsule and never
        // its words alone (owner decision, this session).
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = if (progress != null) ProgressRoom else 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PillEndAir,
                        end = PillAir,
                        top = PillAir,
                        bottom = PillAir,
                    ),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The words and the answer, one shape each: the block gives way
                // with an ellipsis, and the answer keeps its whole word.
                WordsBlock(
                    name = reciterShortName(offer.reciter, offer.reciterName),
                    title = when {
                        offer.failed -> stringResource(
                            if (offer.failure == DownloadError.Storage) {
                                R.string.playback_download_failed_storage
                            } else {
                                R.string.playback_download_failed
                            },
                        )
                        progress != null -> stringResource(
                            R.string.playback_downloading,
                            (progress * 100).toInt(),
                        )
                        else -> title
                    },
                    // The block is a door while the request can still be
                    // changed, and the chevron goes with the door once the
                    // package is on its way.
                    onOpen = if (progress == null) {
                        { chooser = true }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .testTag("offer-words"),
                    announce = offer.failed,
                )
                // The state's one answer, and the words give way to it with an
                // ellipsis: the lead keeps the block and the answer apart when
                // the name and the size are long. A request and a failure
                // answer with a word; a package on its way answers with the
                // app's one X, because a stray tap must never throw the
                // transfer away. The X's circle is inset into its own target,
                // so its target takes the smaller lead and the reader sees the
                // app's air either way (owner decision, this session).
                Row(
                    modifier = Modifier.padding(
                        start = if (progress != null) GlyphLead else WordsGap,
                    ),
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
                            // The bar exists for this one answer, so it is
                            // the prominent shape on both paths (owner
                            // decision, this session).
                            prominent = true,
                        )
                        else -> TextButton(
                            label = stringResource(R.string.playback_download),
                            onClick = onConfirm,
                            prominent = true,
                        )
                    }
                }
            }
        }
        // The same drawn line the playing pill wears, so a download is one
        // shape whatever pill carries it (owner decision). It stands in the
        // room kept under the row, on the bar's own measure, which the words
        // decide and the percent never does.
        if (progress != null) {
            Box(Modifier.matchParentSize()) {
                DownloadLine(
                    progress = progress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = Space.Block)
                        .padding(bottom = Space.Line)
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
                if (chooser) AppPopup(
                    anchor = anchor,
                    onDismiss = { chooser = false },
                    alignment = PopupAlignment.Center,
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
