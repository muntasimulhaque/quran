package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The share sheet: the card as the reader will send it, and the two doors.
 *
 * The card used to leave the app sight unseen, with its text welded to the
 * picture as a caption; a reader could not know what they were sending or
 * choose the plain words instead (owner report). Here the real card
 * is drawn at its own scale as a preview, and the doors are named: **Share
 * image** (the default) and **Share text**. The preview is the same
 * composable the capture draws, so what the reader sees is what the picture
 * will be, not a second drawing that can drift from it.
 *
 * The sheet carries no heading. It used to open with the ayah's reference in
 * a title above the card, which the card already prints under the surah's
 * name, in a type size no other sheet uses: the surface named the ayah a
 * third time and said nothing about what it was for (owner report). The two
 * doors say what it is for, and a screen reader is told the surface's name
 * through the pane title without a line being drawn (owner decision, this
 * session).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AyahShareSheet(
    card: ShareCard,
    onShareImage: () -> Unit,
    onShareText: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberExpandedSheetState()
    // The one name the sheet has: spoken when the surface opens, never drawn.
    val announcement = stringResource(R.string.share_sheet_announcement)
    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The gate rides with the scroll, so an upward flick anywhere
                // on the sheet never bounces it on its top anchor, while the
                // preview keeps its own scroll for a tall card.
                .sheetVerticalScroll(rememberScrollState())
                .padding(bottom = Space.SheetFoot)
                .semantics { paneTitle = announcement }
                .testTag("share-sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The preview keeps the card's own measure and corners, so the
            // reader looks at the artifact rather than at a description of
            // it. It scrolls with the sheet if an ayah makes it tall.
            ShareCardPreviewBox(card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Section),
                // The two doors are a centered pair on the app's one action
                // gap: they are two standalone actions, not one group, so the
                // gap is the same 20 dp the reminder dialog's Set and Cancel
                // stand on. Weighted buttons made the gap a side effect of the
                // stretch, wide on the phone and wider on a ten inch screen,
                // and 12 dp read as one crowded pair under two wide pills
                // (owner report, this session).
                horizontalArrangement = Arrangement.spacedBy(Space.Action, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    label = stringResource(R.string.share_image),
                    onClick = onShareImage,
                )
                TextButton(
                    label = stringResource(R.string.share_text),
                    onClick = onShareText,
                    quiet = true,
                )
            }
        }
    }
}

/** The card, drawn as it will be sent: the preview's own measure, no capture. */
@Composable
internal fun ShareCardPreview(card: ShareCard) {
    // The picture is drawn at one fixed text scale, whatever the phone is set
    // to: a card is one artifact sent to everyone. The capture pins the scale
    // and the preview pins it too, or a reader whose phone is at 130% would
    // approve a card larger and taller than the one they send (owner report,
    // this session).
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .width(ShareCardWidth)
            .clip(Radii.Tile),
    ) {
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            AyahShareCardContent(card)
        }
    }
}

/**
 * The preview as the share sheet shows it: the card's own measure behind a
 * height cap, with the cap on the viewport rather than on the card.
 *
 * Modifier order is the whole of it. `heightIn` before `verticalScroll`
 * caps the viewport, so a taller card still measures at its real height and
 * the reader scrolls to the foot of it; the other order caps the card
 * itself, and the clip cut a long ayah off at the cap with no scroll to
 * reach the rest (owner report). The corner clip stays outside the
 * cap so the sheet's own shape is cut once.
 */
@Composable
internal fun ShareCardPreviewBox(card: ShareCard) {
    val preview = stringResource(R.string.share_preview)
    Box(
        modifier = Modifier
            .padding(horizontal = Space.Gutter)
            .clip(Radii.Tile)
            .heightIn(max = 340.dp)
            .verticalScroll(rememberScrollState())
            // The card is a picture of the ayah the reader just read, not a
            // second reading. A screen reader hears that the preview is here
            // and moves to the two doors; it does not hear the whole ayah
            // again before it can choose (owner decision, this session).
            .clearAndSetSemantics { contentDescription = preview },
    ) {
        ShareCardPreview(card)
    }
}
