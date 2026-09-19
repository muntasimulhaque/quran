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
 * finger however the screen is sized. The page never scales with the reader's
 * text size: its lines are justified to the page, not the screen.
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
                        val scale = size.width.toFloat() / page.widthPx
                        val top = ((size.height - page.heightPx * scale) / 2f).coerceAtLeast(0f)
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
                                    x = offset.x / scale,
                                    y = (offset.y - top) / scale,
                                    slop = slop / scale,
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
                    // launch is already the page the reader left.
                    if (placeholder != null) {
                        val scale = size.width / placeholder.width
                        val top = ((size.height - placeholder.height * scale) / 2f).coerceAtLeast(0f)
                        withTransform({
                            translate(0f, top)
                            scale(scale, scale, pivot = Offset.Zero)
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
                val scale = size.width / page.widthPx
                val top = ((size.height - page.heightPx * scale) / 2f).coerceAtLeast(0f)
                withTransform({
                    translate(0f, top)
                    scale(scale, scale, pivot = Offset.Zero)
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
        // carry no pointer input, so a touch still belongs to the page.
        if (active && rendered != null && availableWidth > 0f && availableHeight > 0f) {
            val read = rendered ?: return@BoxWithConstraints
            val scale = availableWidth / read.widthPx
            val top = ((availableHeight - read.heightPx * scale) / 2f).coerceAtLeast(0f)
            for (ayah in read.ayahOrder) {
                val box = read.ayahBox(ayah.number) ?: continue
                val width = box.width() * scale
                val height = box.height() * scale
                if (width <= 0f || height <= 0f) continue
                val x = (box.left * scale).roundToInt()
                val y = (top + box.top * scale).roundToInt()
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
                        .semantics {
                            contentDescription = description
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
            val pad = (word.bottom - word.top) * 0.08f
            drawRoundRect(
                color = page.palette.highlight,
                topLeft = Offset(word.left - pad, word.top + pad * 0.5f),
                size = Size(word.right - word.left + pad * 2, word.bottom - word.top - pad),
                cornerRadius = CornerRadius(pad * 2.4f),
            )
        }
    }
}

/** Height divided by width of a rendered page, from the renderer's metrics. */
const val PAGE_ASPECT: Float = 1.586f
