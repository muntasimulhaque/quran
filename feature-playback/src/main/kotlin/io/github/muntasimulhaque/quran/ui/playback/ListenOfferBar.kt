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
import io.github.muntasimulhaque.quran.ui.kit.DownloadLine
import io.github.muntasimulhaque.quran.ui.kit.PopupAlignment
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.uikit.R as UiKitR

/**
 * One request, named in full: which reciter, which surah, how much. One tap
 * downloads the word timings and the audio together, under one progress bar,
 * and then it plays.
 *
 * **The offer is a two-row capsule, 96 dp tall** (owner decision, this
 * session). Its first row is a 48 dp door around the whole line: the
 * reciter's name, the chevron that says the line opens, then the surah and
 * its size. Its second row is the two answers, Not now and Download, side by
 * side. The line sits 14 dp under the crown, the answer row is the capsule's
 * 48 dp foot, and the visible pills keep their 7 dp of air. Before this the
 * line was a 20 dp notice, because a 20 dp line cannot also be a 48 dp
 * target; the door is the 20 dp the capsule gained, and it gives the name its
 * chooser back.
 *
 * **The line is the door to the voice list** (owner decision, this session).
 * It opens the same reciter list the playing pill carries, hung from the
 * capsule's centre: the chosen voice's mark, and what each voice would still
 * need for this surah, so the choice is made with its price in view. A chosen
 * voice is the app's reciter, and the offer is asked again with that voice's
 * name and size, so the Download spends the size the reader just saw. An
 * offer with no published voice to offer keeps the line as a notice, because
 * a door with nothing behind it is a door that lies.
 *
 * **An offer has no X** (owner decision, an earlier session). A page tap and
 * Back put it away; Not now is the same answer made visible. The only X the
 * bar draws is the cancel of a package already on its way, which is the one
 * thing a stray tap must not do.
 *
 * The bar stands at the pill's own [PillMeasure] in every state, so an offer
 * and the transfer that follows it are one width. The transfer keeps its one
 * row, with the X at its end and the progress line under it. The capsule's
 * width never follows its words, and the progress line stands in the room
 * kept under the transfer's row, never across a control's press shape.
 */
@Composable
internal fun ListenOfferBar(
    offer: ListenOffer,
    title: String,
    onConfirm: () -> Unit,
    /** The one X the app keeps: abort a package on its way. */
    onCancelDownload: () -> Unit,
    /** The voice the reader chose from the offer's own list. */
    onReciter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The chooser belongs to this request. A new voice or a new surah is
    // another request, so the list that belonged to the old one closes with
    // it, the same way the playing pill's panel closes when its pill becomes
    // another pill.
    var chooser by remember(offer.surah, offer.reciter) { mutableStateOf(false) }
    val progress = offer.progress
    val failure = if (offer.failed) {
        stringResource(
            if (offer.failure == DownloadError.Storage) {
                UiKitR.string.failure_storage
            } else {
                UiKitR.string.failure_download
            },
        )
    } else {
        null
    }
    Box(
        modifier = modifier
            // The caller's gutters come first, then the pill's one measure:
            // the offer is the same width as the playing pill and takes it
            // before its shape is drawn (owner decision, this session).
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
        if (progress == null) {
            // The ask: one 48 dp door over the 48 dp answer row. The door's
            // target is the line's own content, so the flash hugs the words
            // the reader is choosing, and the question is not the whole
            // capsule's width.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.Section),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OfferLine(
                        name = reciterShortName(offer.reciter, offer.reciterName),
                        title = failure ?: title,
                        onOpen = if (offer.options.isEmpty()) {
                            null
                        } else {
                            { chooser = true }
                        },
                        announce = offer.failed,
                        modifier = Modifier.testTag("offer-words"),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        Space.Action,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        label = stringResource(R.string.playback_not_now),
                        onClick = onCancelDownload,
                        quiet = true,
                        pillTag = "offer-not-now",
                    )
                    TextButton(
                        label = stringResource(
                            if (offer.failed) {
                                R.string.playback_retry
                            } else {
                                R.string.playback_download
                            },
                        ),
                        onClick = onConfirm,
                        // The bar exists for this one answer, so it is the
                        // prominent shape on both paths (owner decision, an
                        // earlier session).
                        prominent = true,
                        pillTag = "offer-answer",
                    )
                }
            }
        } else {
            // The transfer keeps its one row: the words, the percent, and the
            // app's one X at the end. It is a state, not a question, so it
            // does not carry Not now; the X is the one thing a stray tap must
            // never do (owner decision, an earlier session).
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = ProgressRoom),
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
                    TransferWordsBlock(
                        name = reciterShortName(offer.reciter, offer.reciterName),
                        title = stringResource(
                            R.string.playback_downloading,
                            (progress * 100).toInt(),
                        ),
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .testTag("offer-words"),
                    )
                    Row(
                        modifier = Modifier.padding(start = GlyphLead),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TransportButton(
                            Transport.Close,
                            stringResource(R.string.playback_cancel_download),
                            onCancelDownload,
                        )
                    }
                }
            }
        }
        // The same drawn line the playing pill wears, so a download is one
        // shape whatever pill carries it (owner decision). It stands in the
        // room kept under the transfer's row, on the bar's own measure, which
        // the words decide and the percent never does.
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
        // The chooser hangs from the capsule's own centre, exactly as it does
        // on the playing pill: one list opens the same way in both of its
        // homes. It is composed only while it is asked for, and only while
        // there is still an ask to change; a transfer has no voice to choose.
        if (progress == null) {
            Box(Modifier.matchParentSize()) {
                PillMenuAnchor { anchor ->
                    if (chooser && offer.options.isNotEmpty()) {
                        AppPopup(
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
    }
}
