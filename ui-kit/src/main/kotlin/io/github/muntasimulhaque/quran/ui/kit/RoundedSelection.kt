package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * The app's one text selection: a rounded wash behind the selected words,
 * with the platform's own cursor, handles, magnifier, and copy menu still
 * doing the work underneath.
 *
 * Compose draws a text selection as a rectangle and exposes its color, not
 * its corners. The app has one shape language, so the fields that hold text
 * draw their own wash and leave the platform's background transparent. A
 * field holds one text layout and one range, so its wash is arithmetic.
 *
 * The reading's copy selection is the platform's own surface, and it keeps
 * the platform's shape: its ranges can span several text blocks, and Compose
 * marks the selection internals that would be needed to draw them internal.
 * Replacing it would mean replacing the platform's copy selection, not
 * recoloring it (owner decision, this session).
 *
 * The wash is [Wash.Match], the loudest of the quiet fills, the same tone a
 * search match wears. It is not the platform's 40 percent tint: a reader
 * selects words to read them again or copy them, and the words must stay
 * readable through their own selection.
 */
@Composable
fun RoundedSelectionColors(content: @Composable () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val colors = remember(primary) {
        TextSelectionColors(handleColor = primary, backgroundColor = Color.Transparent)
    }
    CompositionLocalProvider(LocalTextSelectionColors provides colors, content = content)
}

/**
 * The rounded wash under a field's selection, drawn behind the field's own
 * text. The field gives its laid-out text and its range; a null layout, a
 * collapsed range, or a range outside the text draws nothing.
 */
@Composable
fun Modifier.roundedSelection(
    layout: TextLayoutResult?,
    selection: TextRange,
): Modifier {
    val wash = MaterialTheme.colorScheme.primary.copy(alpha = Wash.Match)
    return drawBehind {
        if (layout != null) drawRoundedTextSelection(layout, selection, Offset.Zero, wash)
    }
}

/**
 * The rounded wash for one text range. The range is drawn line by line so a
 * wrapped selection follows the words instead of becoming one rectangle
 * around the whole block.
 */
private fun DrawScope.drawRoundedTextSelection(
    layout: TextLayoutResult,
    range: TextRange,
    origin: Offset,
    color: Color,
) {
    val length = layout.layoutInput.text.length
    if (length == 0) return
    val start = minOf(range.start, range.end).coerceIn(0, length)
    val end = maxOf(range.start, range.end).coerceIn(0, length)
    if (start >= end) return
    val firstLine = layout.getLineForOffset(start)
    val lastLine = layout.getLineForOffset(end)
    val radius = Radii.SelectionRadius.toPx()
    for (line in firstLine..lastLine) {
        // A range that ends exactly at the start of a later line does not
        // paint that empty line; the line before it already ends the range.
        if (line == lastLine && lastLine > firstLine && end == layout.getLineStart(line)) continue
        val lineStart = if (line == firstLine) start else layout.getLineStart(line)
        val lineEnd = if (line == lastLine) end else layout.getLineEnd(line, visibleEnd = true)
        if (lineStart >= lineEnd) continue
        val startBox = layout.getBoundingBox(lineStart.coerceAtMost(length - 1))
        val endBox = layout.getBoundingBox((lineEnd - 1).coerceAtLeast(0))
        val left = minOf(startBox.left, endBox.left)
        val right = maxOf(startBox.right, endBox.right)
        if (right <= left) continue
        val top = layout.getLineTop(line)
        val bottom = layout.getLineBottom(line)
        drawRoundRect(
            color = color,
            topLeft = Offset(origin.x + left, origin.y + top),
            size = Size(right - left, (bottom - top).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(radius),
        )
    }
}
