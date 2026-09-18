package io.github.muntasimulhaque.quran.ui.mushaf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PageFontStore
import io.github.muntasimulhaque.quran.ui.theme.MushafHighlight
import io.github.muntasimulhaque.quran.ui.theme.MushafSelection
import kotlin.math.min

/**
 * One page of the Mushaf, drawn whole: the paper, the glyphs, the page's own
 * furniture, and the washes that mark a selected or recited ayah.
 *
 * The page is rendered at the exact pixel width it will be shown at, and the
 * touch math works in page pixels, so a tap lands on the word under the
 * finger however the screen is sized. The page never scales with the reader's
 * text size: its lines are justified to the page, not the screen.
 */
@Composable
fun MushafPage(
    content: ContentDatabase,
    fonts: PageFontStore,
    renderer: PageRenderer,
    page: Int,
    palette: PagePalette,
    themeKey: String,
    selectedAyah: Int?,
    playingAyah: Int?,
    playingWord: Int?,
    onAyah: (Ayah) -> Unit,
    onLongPressAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val availableHeight = with(density) { maxHeight.toPx() }
        // The page's height follows from its width; render at the width it
        // will actually be drawn at, never larger.
        val pageWidth = min(availableWidth, availableHeight / PAGE_ASPECT).toInt().coerceAtLeast(1)
        val key = PageKey(page, pageWidth, themeKey)

        val rendered by produceState(initialValue = renderer.peek(key), key, palette) {
            value = renderer.get(key, content, fonts, palette)
        }
        val onAyahState = rememberUpdatedState(onAyah)
        val onLongState = rememberUpdatedState(onLongPressAyah)
        val onBackgroundState = rememberUpdatedState(onBackgroundTap)
        val slop = with(density) { 6.dp.toPx() }

        Canvas(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Mushaf page" }
                .pointerInput(rendered, key) {
                    val page = rendered ?: return@pointerInput
                    val scale = size.width.toFloat() / page.widthPx
                    val top = ((size.height - page.heightPx * scale) / 2f).coerceAtLeast(0f)
                    detectTapGestures(
                        onTap = {
                            // A tap anywhere belongs to the reading: it brings
                            // the chrome, or puts it away.
                            onBackgroundState.value()
                        },
                        onLongPress = { offset ->
                            // A long press asks about the ayah under the finger.
                            val ayah = page.ayahAt(
                                x = offset.x / scale,
                                y = (offset.y - top) / scale,
                                slop = slop / scale,
                            )
                            if (ayah != null) onLongState.value(ayah) else onBackgroundState.value()
                        },
                    )
                },
        ) {
            val page = rendered ?: return@Canvas
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
                color = MushafSelection,
                topLeft = Offset(box.left, box.top),
                size = Size(box.width(), box.height()),
                cornerRadius = CornerRadius(box.height() * 0.18f),
            )
        }
    } else if (playingAyah != null) {
        for (box in page.ayahLineBoxes(playingAyah)) {
            drawRoundRect(
                color = MushafHighlight.copy(alpha = 0.09f),
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
                color = MushafHighlight.copy(alpha = 0.22f),
                topLeft = Offset(word.left - pad, word.top + pad * 0.5f),
                size = Size(word.right - word.left + pad * 2, word.bottom - word.top - pad),
                cornerRadius = CornerRadius(pad * 2.4f),
            )
        }
    }
}

/** Height divided by width of a rendered page, from the renderer's metrics. */
const val PAGE_ASPECT: Float = 1.586f
