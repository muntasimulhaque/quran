package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.max
import kotlin.math.min

/**
 * A word's wash, one rounded shape per line it spans. The geometry comes from
 * the text layout itself, so the wash sits exactly on the word, in the
 * reading direction too. Used by a search match, by the reciter's word, and
 * by the word sheet while the word's own voice sounds: the page's one place
 * mark, under the letters, at the strongest tint the seven rule colors keep
 * their contrast on.
 */
internal fun DrawScope.drawWordWash(
    layout: TextLayoutResult,
    range: IntRange,
    wash: Color,
) {
    forEachWordBox(layout, range) { box -> drawWashBox(box, wash) }
}

/** Every line's box of one word, in text order, handed to the caller. */
private inline fun forEachWordBox(
    layout: TextLayoutResult,
    range: IntRange,
    action: (Rect) -> Unit,
) {
    val start = range.first.coerceAtLeast(0)
    val end = range.last.coerceAtMost(layout.layoutInput.text.length - 1)
    if (end < start) return
    var line = layout.getLineForOffset(start)
    var box: Rect? = null
    for (offset in start..end) {
        val nextLine = layout.getLineForOffset(offset)
        if (nextLine != line) {
            box?.let(action)
            box = null
            line = nextLine
        }
        val charBox = layout.getBoundingBox(offset)
        box = box?.let { union ->
            Rect(
                left = min(union.left, charBox.left),
                top = min(union.top, charBox.top),
                right = max(union.right, charBox.right),
                bottom = max(union.bottom, charBox.bottom),
            )
        } ?: charBox
    }
    box?.let(action)
}

/** One word's wash box: rounded, with the room a mark needs around the glyphs. */
private fun DrawScope.drawWashBox(box: Rect, wash: Color) {
    val pad = box.height * 0.08f
    drawRoundRect(
        color = wash,
        topLeft = Offset(box.left - pad, box.top + pad * 0.5f),
        size = Size(box.width + pad * 2, box.height - pad),
        cornerRadius = CornerRadius(box.height * 0.26f),
    )
}
