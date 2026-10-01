package io.github.muntasimulhaque.quran.ui.mushaf

import androidx.compose.runtime.getValue
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.PageFrame
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PageFontStore
import io.github.muntasimulhaque.quran.feature.mushaf.R
import io.github.muntasimulhaque.quran.ui.theme.PagePalette
import kotlin.math.roundToInt

/**
 * One page of the Mushaf, drawn whole: the paper, the glyphs, the page's own
 * furniture, and the washes that mark a selected or recited ayah.
 *
 * The page is rendered at the exact pixel width it will be shown at, and the
 * touch math works in page pixels, so a tap lands on the word under the
 * finger however the screen is sized. It is therefore drawn at its own size
 * and centered, never stretched to fill the screen: the reader picks the
 * page's width so the whole page fits, which on a landscape tablet is a page
 * narrower than the glass, and stretching that page to the glass drew it two
 * and a quarter times too large with two thirds of it off the bottom of the
 * screen (owner report, D-132). The page never scales with the reader's text
 * size either: its lines are justified to the page, not the screen.
 *
 * A turn is a plain horizontal slide with no lift and no cast shadow: the
 * page is the Book, not a sheet being picked up, and an edge lifted off the
 * paper draws the eye away from the text it is carrying.
 *
 * The page is a picture of text, so it also carries a reading of itself for
 * TalkBack: one invisible node per ayah, in page order, each with its
 * reference and its text.
 */
@Composable
fun MushafPage(
    content: ContentDatabase,
    fonts: PageFontStore,
    renderer: PageRenderer,
    page: Int,
    /** The pixel width the page is drawn at, decided once for the whole pager. */
    pageWidth: Int,
    palette: PagePalette,
    themeKey: String,
    selectedAyah: Int?,
    playingAyah: Int?,
    playingWord: Int?,
    onLongPressAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    modifier: Modifier = Modifier,
    /** True for the page the reader is on, whose ayah nodes are exposed. */
    active: Boolean = true,
    /** The picture of this page from the last session, until it is rendered. */
    placeholder: Bitmap? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val availableHeight = with(density) { maxHeight.toPx() }
        val key = PageKey(page, pageWidth, themeKey)

        val rendered by produceState(initialValue = renderer.peek(key), key, palette) {
            value = renderer.get(key, content, fonts, palette)
        }
        val onLongState = rememberUpdatedState(onLongPressAyah)
        val onBackgroundState = rememberUpdatedState(onBackgroundTap)
        val haptics = LocalHapticFeedback.current
        val slop = with(density) { 6.dp.toPx() }
        val ayahActions = stringResource(R.string.mushaf_ayah_actions)
        val plainDescription = stringResource(R.string.mushaf_page_description, page)
        val described = rendered?.let {
            stringResource(R.string.mushaf_page_description_ayahs, page, it.ayahOrder.size)
        }

        Box(Modifier.fillMaxSize()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = described ?: plainDescription
                    }
                    .pointerInput(rendered, key) {
                        val page = rendered ?: return@pointerInput
                        val left = pageLeft(size.width.toFloat(), page.widthPx)
                        val top = pageTop(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            page.widthPx,
                            page.heightPx,
                        )
                        detectTapGestures(
                            onTap = {
                                // A tap anywhere belongs to the reading: it
                                // brings the chrome, or puts it away.
                                onBackgroundState.value()
                            },
                            onLongPress = { offset ->
                                // A long press asks about the ayah under the
                                // finger: the ayah washes and the phone hums.
                                val ayah = page.ayahAt(
                                    x = offset.x - left,
                                    y = offset.y - top,
                                    slop = slop,
                                )
                                if (ayah != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongState.value(ayah)
                                } else {
                                    onBackgroundState.value()
                                }
                            },
                        )
                    },
            ) {
                val page = rendered
                if (page == null) {
                    // The page picture from the last session, at the same
                    // width and in the same colors, so the first frame of a
                    // launch is already the page the reader left. It is drawn
                    // at its own size and centered, like the real page.
                    if (placeholder != null) {
                        val left = pageLeft(size.width.toFloat(), placeholder.width)
                        val top = pageTop(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            placeholder.width,
                            placeholder.height,
                        )
                        withTransform({
                            translate(left, top)
                        }) {
                            drawImage(
                                image = placeholder.asImageBitmap(),
                                dstSize = androidx.compose.ui.unit.IntSize(
                                    placeholder.width,
                                    placeholder.height,
                                ),
                            )
                        }
                    }
                    return@Canvas
                }
                val left = pageLeft(size.width.toFloat(), page.widthPx)
                val top = pageTop(
                    size.width.toFloat(),
                    size.height.toFloat(),
                    page.widthPx,
                    page.heightPx,
                )
                withTransform({
                    translate(left, top)
                }) {
                    drawImage(
                        image = page.bitmap.asImageBitmap(),
                        dstSize = androidx.compose.ui.unit.IntSize(page.widthPx, page.heightPx),
                    )
                    drawWashes(page, selectedAyah, playingAyah, playingWord)
                }
            }
        }

        // One node per ayah, in page order, over the words they name. They
        // carry no pointer input, so a touch still belongs to the page. Each
        // is placed where the page's own box is drawn, which is the box at the
        // page's own size, centered: a node placed by a scale the drawing does
        // not use is a node in the wrong place, and one wrong by enough is a
        // node no finger can reach (D-132).
        if (active && rendered != null && availableWidth > 0f && availableHeight > 0f) {
            val read = rendered ?: return@BoxWithConstraints
            val left = pageLeft(availableWidth, read.widthPx)
            val top = pageTop(availableWidth, availableHeight, read.widthPx, read.heightPx)
            for (ayah in read.ayahOrder) {
                val box = read.ayahBox(ayah.number) ?: continue
                val width = box.width()
                val height = box.height()
                if (width <= 0f || height <= 0f) continue
                val x = (left + box.left).roundToInt()
                val y = (top + box.top).roundToInt()
                val description = stringResource(
                    R.string.mushaf_ayah_node,
                    ayah.verseKey,
                    ayah.text,
                )
                Box(
                    Modifier
                        .offset { IntOffset(x, y) }
                        .size(
                            width = with(density) { width.toDp() },
                            height = with(density) { height.toDp() },
                        )
                        .semantics {                            contentDescription = description
                            onClick(label = ayahActions) {
                                onLongState.value(ayah)
                                true
                            }
                        },
                )
            }
        }
    }
}

private fun DrawScope.drawWashes(
    page: RenderedPage,
    selectedAyah: Int?,
    playingAyah: Int?,
    playingWord: Int?,
) {
    if (selectedAyah != null) {
        for (box in page.ayahLineBoxes(selectedAyah)) {
            drawRoundRect(
                color = page.palette.selection,
                topLeft = Offset(box.left, box.top),
                size = Size(box.width(), box.height()),
                cornerRadius = CornerRadius(box.height() * 0.18f),
            )
        }
    } else if (playingAyah != null) {
        for (box in page.ayahLineBoxes(playingAyah)) {
            drawRoundRect(
                color = page.palette.highlight.copy(alpha = page.palette.highlight.alpha * 0.42f),
                topLeft = Offset(box.left, box.top),
                size = Size(box.width(), box.height()),
                cornerRadius = CornerRadius(box.height() * 0.18f),
            )
        }
    }
    if (playingAyah != null && playingWord != null) {
        page.wordBox(playingAyah, playingWord)?.let { word ->
            // The same rounded wash the study reading draws under its word:
            // one word mark in one shape, in both modes.
            val height = word.bottom - word.top
            val pad = height * 0.08f
            drawRoundRect(
                color = page.palette.highlight,
                topLeft = Offset(word.left - pad, word.top + pad * 0.5f),
                size = Size(word.right - word.left + pad * 2, height - pad),
                cornerRadius = CornerRadius(height * 0.26f),
            )
        }
    }
}

/**
 * Height divided by width of a rendered page, taken from the page's own
 * geometry rather than remembered beside it: it is the share of the tallest
 * page the rule can draw, so a page is never taller than the room the pager
 * gave it, whatever font it turns out to be carrying (PageFrame).
 */
val PAGE_ASPECT: Float = PageFrame.aspect(PageFrame.INK_EM_TALLEST)

/**
 * Where a page of the reader's own pixel size sits inside the glass: its left
 * edge on a surface [availableWidth] pixels wide.
 *
 * The page is rendered at the width it will be shown at and is never scaled
 * afterwards, so this is the one place a page's position is decided, and the
 * drawing, the touch math, and the ayah nodes all read it. A page wider than
 * the surface (a tall phone) starts at the glass's own edge.
 */
internal fun pageLeft(availableWidth: Float, pageWidthPx: Int): Float =
    ((availableWidth - pageWidthPx) / 2f).coerceAtLeast(0f)

/** The same for the page's top edge, on a surface [availableHeight] pixels tall. */
internal fun pageTop(
    availableWidth: Float,
    availableHeight: Float,
    pageWidthPx: Int,
    pageHeightPx: Int,
): Float = ((availableHeight - pageHeightPx) / 2f).coerceAtLeast(0f)
