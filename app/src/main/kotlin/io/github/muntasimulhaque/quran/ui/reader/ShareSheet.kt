package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The share sheet: the card as the reader will send it, and the two doors.
 *
 * The card used to leave the app sight unseen, with its text welded to the
 * picture as a caption; a reader could not know what they were sending or
 * choose the plain words instead (owner report). Here the real card
 * is drawn at its own scale as a preview, and the doors are named: **Share
 * text** first, and **Share image** (the default) last, the app's one order
 * for a pair, so the eye ends on the default rather than meeting it first.
 * The preview is the same composable the capture draws, so what the reader
 * sees is what the picture will be, not a second drawing that can drift
 * from it.
 *
 * The sheet carries no heading. It used to open with the ayah's reference in
 * a title above the card, which the card already prints under the surah's
 * name, in a type size no other sheet uses: the surface named the ayah a
 * third time and said nothing about what it was for (owner report). The two
 * doors say what it is for, and a screen reader is told the surface's name
 * through the pane title without a line being drawn (owner decision, this
 * session).
 *
 * **The preview is the whole card, at the width the card chose, scaled to the
 * sheet** (owner report, an earlier session). The old preview kept the card
 * behind a 340 dp viewport and scrolled, so the reader saw the first lines and
 * a cut. The preview now measures the card and scales it down just enough to
 * fit the sheet's remaining height and width. The two doors stay below it, so
 * the whole artifact is in front of the reader before they choose. The card's
 * width is the ladder's answer for this ayah ([ShareCardWidths]), and the
 * door hands that same width to the capture, so the reader approves the card
 * they will send and not a 1:9 strip of it (owner decision, this version).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AyahShareSheet(
    card: ShareCard,
    onShareImage: (Dp) -> Unit,
    onShareText: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberExpandedSheetState()
    // The preview measures the card and picks its width; the door hands that
    // one answer to the capture, so the picture and the preview cannot be two
    // different cards (owner decision, this version).
    var cardWidth by remember(card) { mutableStateOf(ShareCardWidths.first()) }
    // The one name the sheet has: spoken when the surface opens, never drawn.
    val announcement = stringResource(R.string.share_sheet_announcement)
    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // The sheet's remaining height after the handle. The doors keep
            // their 48 dp target, the section gap above them, and the sheet's
            // foot; the preview gets the rest. A sheet measured without a
            // bound (a test frame, a future host) takes a screen's share
            // rather than asking for infinite air.
            val room = if (maxHeight.value.isFinite()) {
                maxHeight
            } else {
                480.dp
            }
            val previewMaxHeight = (room - ButtonsRoom).coerceAtLeast(120.dp)
            val previewMaxWidth = if (maxWidth.value.isFinite()) {
                (maxWidth - Space.Gutter * 2).coerceAtLeast(1.dp)
            } else {
                ShareCardWidths.last()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Space.SheetFoot)
                    .semantics { paneTitle = announcement }
                    .testTag("share-sheet"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The preview is the card's own measure, scaled only enough to
                // stand whole between the handle and the doors. It never
                // scrolls: the reader is looking at the artifact, not reading
                // it, and the Share image door takes them to the full size.
                ShareCardPreviewBox(
                    card = card,
                    maxWidth = previewMaxWidth,
                    maxHeight = previewMaxHeight,
                    onWidth = { cardWidth = it },
                )
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
                        label = stringResource(R.string.share_text),
                        onClick = onShareText,
                        quiet = true,
                    )
                    TextButton(
                        label = stringResource(R.string.share_image),
                        onClick = { onShareImage(cardWidth) },
                    )
                }
            }
        }
    }
}

/**
 * The card, drawn as it will be sent: one of the ladder's widths, no capture.
 *
 * The picture is drawn at one fixed text scale, whatever the phone is set
 * to: a card is one artifact sent to everyone. The capture pins the scale and
 * the preview pins it too, or a reader whose phone is at 130% would approve a
 * card larger and taller than the one they send (owner report, an earlier
 * session).
 */
@Composable
internal fun ShareCardPreview(card: ShareCard, width: Dp = ShareCardWidths.first()) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .width(width)
            .clip(Radii.Tile),
    ) {
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            AyahShareCardContent(card)
        }
    }
}

/**
 * The preview as the share sheet shows it: the card at the width the ladder
 * gives it, scaled down just enough to stand whole in the room the sheet
 * leaves for it.
 *
 * The old preview put a 340 dp cap on the viewport and scrolled the card, so
 * an ordinary ayah was cut; the version before this one scaled the whole card
 * at a fixed 320 dp, which showed a long ayah as a 1:3 strip and the longest
 * as a 1:9 one. The preview now measures the card at each of
 * [ShareCardWidths] and keeps the one closest to [ShareCardPosterRatio], then
 * says that width through [onWidth] so the capture draws the same card. The
 * layer carries the scale to the drawing, so the type is set at the card's
 * own size and then reduced as one picture, which is exactly what the shared
 * image is. The card never scales up past 1, because a preview of a 320 dp
 * artifact should not be larger than the artifact.
 *
 * The measure stops at the first width at or under the poster ratio: every
 * wider card is shorter still, so nothing wider can stand closer to it. The
 * writes to the chosen width are guarded, so the first frame settles on one
 * answer and the measure does not run again.
 *
 * The card is a picture of the ayah the reader just read, not a second
 * reading. A screen reader hears that the preview is here and moves to the
 * two doors; it does not hear the whole ayah again before it can choose
 * (owner decision, an earlier session).
 */
@Composable
internal fun ShareCardPreviewBox(
    card: ShareCard,
    maxWidth: Dp = ShareCardWidths.last(),
    maxHeight: Dp = 340.dp,
    /** The width the preview settled on, for the capture to draw the same card. */
    onWidth: (Dp) -> Unit = {},
) {
    val preview = stringResource(R.string.share_preview)
    val density = LocalDensity.current
    val report = rememberUpdatedState(onWidth)
    var chosen by remember(card) { mutableStateOf(ShareCardWidths.first()) }
    LaunchedEffect(chosen) { report.value(chosen) }
    val roomWidth = with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
    val roomHeight = with(density) { maxHeight.roundToPx().coerceAtLeast(1) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = preview
            },
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeLayout { _ ->
            val measured = mutableListOf<ShareCardMeasure>()
            val cards = mutableListOf<Pair<Dp, Placeable>>()
            for (width in ShareCardWidths) {
                val wide = with(density) { width.roundToPx() }
                val placeable = subcompose(width) { ShareCardPreview(card, width) }
                    .first()
                    .measure(Constraints.fixedWidth(wide))
                cards += width to placeable
                measured += ShareCardMeasure(width, with(density) { placeable.height.toDp() })
                if (measured.last().ratio <= ShareCardPosterRatio) break
            }
            val width = shareCardWidth(measured)
            if (width != chosen) chosen = width
            val placeable = cards.first { it.first == width }.second
            val scale = min(
                1f,
                min(
                    roomWidth / placeable.width.toFloat().coerceAtLeast(1f),
                    roomHeight / placeable.height.toFloat().coerceAtLeast(1f),
                ),
            )
            val drawnWidth = (placeable.width * scale).roundToInt()
            val drawnHeight = (placeable.height * scale).roundToInt()
            layout(drawnWidth, drawnHeight) {
                placeable.placeWithLayer(0, 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}

/** The room the sheet's two doors and its foot need under the preview. */
private val ButtonsRoom = 48.dp + Space.Section + Space.SheetFoot
