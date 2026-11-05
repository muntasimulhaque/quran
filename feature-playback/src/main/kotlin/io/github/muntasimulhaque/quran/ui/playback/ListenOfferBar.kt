package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.DownloadError
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.ui.kit.DownloadLine
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
 * **The offer is a compact centered capsule** (owner decision, this session).
 * Its first line names the request, the reciter then the surah and its size,
 * and its second row is the two answers: Not now and Download, side by side.
 * The capsule is 76 dp tall: the 8 dp top air, one 20 dp line, and the 48 dp
 * targets the two buttons own. The visible pills are 34 dp inside those
 * targets, so the capsule's foot keeps its 7 dp of air without a second empty
 * row.
 *
 * The line is a name and a fact, not a door. A 20 dp line cannot also be the
 * 48 dp target a finger needs, and a target that reached into the buttons'
 * row would steal the air above them. The reciter is changed in Settings or
 * on the playing pill's own panel; the offer's one question is the download
 * (owner decision, this session).
 *
 * **An offer has no X** (owner decision, an earlier session). A page tap and
 * Back put it away; Not now is the same answer made visible. The only X the
 * bar draws is the cancel of a package already on its way, which is the one
 * thing a stray tap must not do.
 *
 * The bar stands at the pill's own [PillMeasure] in every state, so an offer
 * and the transfer that follows it are one width. The transfer keeps its one
 * row, with the X at its end and the progress line under it; the offer's two
 * centered rows are the ask alone. The capsule's width never follows its
 * words, and the progress line stands in the room kept under the transfer's
 * row, never across a control's press shape.
 */
@Composable
internal fun ListenOfferBar(
    offer: ListenOffer,
    title: String,
    onConfirm: () -> Unit,
    /** The one X the app keeps: abort a package on its way. */
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            // The ask: one centered line over one centered pair. The line
            // keeps its 8 dp from the capsule's top, and the buttons' own
            // 48 dp targets are the capsule's foot, so the capsule is 76 dp
            // tall at the default step and grows with the reader's type.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OfferLine(
                    name = reciterShortName(offer.reciter, offer.reciterName),
                    title = failure ?: title,
                    announce = offer.failed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Space.Section,
                            end = Space.Section,
                            top = PillAir,
                        )
                        // The line keeps a 20 dp floor, one compact line,
                        // so the capsule is 76 at the default step. A larger
                        // system scale lets the line grow instead of clipping
                        // its type (owner decision, this session).
                        .heightIn(min = 20.dp)
                        .testTag("offer-words"),
                )
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
    }
}
