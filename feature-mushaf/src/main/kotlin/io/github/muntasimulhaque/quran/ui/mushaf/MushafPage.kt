package io.github.muntasimulhaque.quran.ui.mushaf

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.PageFrame
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.feature.mushaf.R
import io.github.muntasimulhaque.quran.ui.theme.PagePalette
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * One page of the Mushaf, drawn whole: the paper, the words, the page's own
 * furniture, and the washes that mark a selected or recited ayah.
 *
 * The page is rendered at the exact pixel width it will be shown at, and the
 * touch math works in page pixels, so a tap lands on the word under the
 * finger however the screen is sized. It is therefore drawn at its own size
 * and centered, never stretched to fill the screen: the page's own width is
 * solved for the glass it is given ([PageGeometry]), and stretching a page to
 * the glass drew it two and a quarter times too large with two thirds of it
 * off the bottom of the screen (owner report). A page the glass cannot hold
 * is panned, not clipped.
 *
 * The page's width and its pitch both come from the glass, so the glass is
 * part of the page's key: the same page on a phone and on a tablet, or the
 * same page on a phone turned round, are two pages.
 *
 * A page taller than the glass is panned from where the reader left off: the
 * page's last lines are still the Book's, and the only thing that has moved
 * is where the page stands.
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
    renderer: PageRenderer,
    page: Int,
    /** The reader's text size for this page, as a share of the page's own type. */
    textScale: Float,
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
        val key = PageKey(
            page = page,
            glassWidthPx = availableWidth.roundToInt(),
            glassHeightPx = availableHeight.roundToInt(),
            theme = themeKey,
            step = textScale,
        )

        val rendered by produceState(initialValue = renderer.peek(key), key, palette) {
            value = renderer.get(key, content, palette)
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

        // A page set larger than the glass is taller than it, and its last
        // lines are still the Book's: the reader pans it, the way any page
        // set large is panned. The pan belongs to this page and this text
        // size, and it is forgotten when either moves on.
        var pan by remember(key) { mutableFloatStateOf(0f) }
        val pageHeight = rendered?.heightPx ?: placeholder?.height ?: 0
        val panLowest = (availableHeight - pageHeight).coerceAtMost(0f)
        val panY = pan.coerceIn(panLowest, 0f)

        Box(Modifier.fillMaxSize()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = described ?: plainDescription
                    }
                    .pointerInput(rendered, key, panLowest) {
                        val page = rendered ?: return@pointerInput
                        val left = pageLeft(size.width.toFloat(), page.widthPx)
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val start = down.position
                            val pageLeftNow = left
                            // The finger before it has done anything: it
                            // either lifts (a tap), rests (a long press), or
                            // drags (a pan, or a turn for the pager).
                            val outcome = withTimeoutOrNull(
                                viewConfiguration.longPressTimeoutMillis,
                            ) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: return@withTimeoutOrNull Outcome.Lifted
                                    if (!change.pressed) return@withTimeoutOrNull Outcome.Lifted
                                    if (dragDistance(start, change.position) > touchSlop) {
                                        return@withTimeoutOrNull Outcome.Moved
                                    }
                                }
                                Outcome.Lifted
                            } ?: Outcome.Rested
                            when (outcome) {
                                Outcome.Lifted -> onBackgroundState.value()
                                Outcome.Rested -> {
                                    // A long press asks about the ayah under
                                    // the finger: the ayah washes and the
                                    // phone hums.
                                    // The pan is read live: a long press
                                    // asks about the ayah under the finger
                                    // where the page stands now, panned and
                                    // all, and not where the drawing was
                                    // composed.
                                    val top = pageTop(
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                        page.widthPx,
                                        page.heightPx,
                                    ) + pan
                                    val ayah = page.ayahAt(
                                        x = start.x - pageLeftNow,
                                        y = start.y - top,
                                        slop = slop,
                                    )
                                    if (ayah != null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongState.value(ayah)
                                    } else {
                                        onBackgroundState.value()
                                    }
                                    // The finger may now drag: a long press
                                    // that becomes a drag is a pan, not a
                                    // scroll nobody asked for.
                                    panLoop(down.id, { panLowest }, { pan }, { value -> pan = value })
                                }
                                Outcome.Moved -> {
                                    // A mostly horizontal drag is the
                                    // pager's: it turns the page, and the
                                    // events were never consumed. A mostly
                                    // vertical drag pans a page taller than
                                    // the glass, and is consumed so the pager
                                    // never sees it.
                                    var vertical = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: break
                                        if (!change.pressed) break
                                        if (!vertical) {
                                            val dx = abs(change.position.x - start.x)
                                            val dy = abs(change.position.y - start.y)
                                            if (dy > touchSlop && dy >= dx) vertical = true
                                            else if (dx > touchSlop) break
                                            else continue
                                        }
                                        if (vertical) {
                                            if (panLowest < 0f) {
                                                val drag = change.positionChange().y
                                                if (drag != 0f) {
                                                    pan = (pan + drag).coerceIn(panLowest, 0f)
                                                }
                                            }
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
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
                                dstSize = IntSize(placeholder.width, placeholder.height),
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
                ) + panY
                withTransform({
                    translate(left, top)
                }) {
                    drawImage(
                        image = page.bitmap.asImageBitmap(),
                        dstSize = IntSize(page.widthPx, page.heightPx),
                    )
                    drawWashes(page, selectedAyah, playingAyah, playingWord)
                }
            }
        }

        // One node per ayah, in page order, over the words they name. They
        // carry no pointer input, so a touch still belongs to the page. Each
        // is placed where the page's own box is drawn, which is the box at
        // the page's own size, centered: a node placed by a scale the drawing
        // does not use is a node in the wrong place, and one wrong by enough
        // is a node no finger can reach.
        if (active && rendered != null && availableWidth > 0f && availableHeight > 0f) {
            val read = rendered ?: return@BoxWithConstraints
            val left = pageLeft(availableWidth, read.widthPx)
            val top = pageTop(availableWidth, availableHeight, read.widthPx, read.heightPx) + panY
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
                        .semantics {
                            contentDescription = description
                            onClick(label = ayahActions) {
                                onLongState.value(ayah)
                                true
                            }
                        },
                ) {}
            }
        }
    }
}

/** What a finger on the page has done, so far. */
private enum class Outcome { Lifted, Rested, Moved }

/** The distance between two points, as the touch slop is measured. */
private fun dragDistance(from: Offset, to: Offset): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    return sqrt(dx * dx + dy * dy)
}

/**
 * Watches the finger until it lifts, panning the page for as long as it
 * drags. Every move is consumed, so a pan never becomes a page turn.
 */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.panLoop(
    pointerId: androidx.compose.ui.input.pointer.PointerId,
    lowest: () -> Float,
    current: () -> Float,
    move: (Float) -> Unit,
) {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
        if (!change.pressed) break
        if (lowest() < 0f) {
            val drag = change.positionChange().y
            if (drag != 0f) move((current() + drag).coerceIn(lowest(), 0f))
        }
        change.consume()
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
 * Where a page of the reader's own pixel size sits inside the glass: its left
 * edge on a surface [availableWidth] pixels wide.
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
