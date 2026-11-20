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
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Radii

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
 * recoloring it (owner decision, 14.3).
 *
 * The wash is [PagePalette.match], the loudest of the quiet grounds, the same
 * tone a search match wears. It is not the platform's 40 percent tint: a reader
 * selects words to read them again or copy them, and the words must stay
 * readable through their own selection.
 *
 * **The ground and the grips are the accent's** (owner decision, 21.0,
 * turning 19.1's split around). A selection is two
 * things at once: a ground the words are read through, and two handles the
 * reader drags, with the caret marking where the next letter goes. The
 * handles and the caret belong to the accent, because blue is this app's mark
 * for the one thing that answers
 * a finger: every button, chevron, and tappable word is blue, so a reader has
 * already learned it before they meet a handle. The ground is the accent's own
 * quiet shade, so the grip and the words it drags read as one family, and the
 * sand-colored smudge 19.1 named cannot return.
 *
 * So the three parts are drawn by two owners on purpose. [RoundedSelectionColors]
 * colors the handles and turns the platform's own background off, so the wash
 * behind the words is the only block drawn; the caret is asked for at each
 * field that has one, with `cursorBrush` on the accent (the search field and the
 * note). Neither is measured against the other: a handle is a control and is
 * held to the contrast a control owes, and the wash is held to the ink it
 * carries, which is what `ContrastTest` reads.
 *
 * The one surface that did not follow that rule was the reading itself, which
 * kept Material's own selection: the accent at forty percent. Forty percent is
 * above every ceiling the page keeps, and on the ground it produced six of the
 * seven rule colors fell under 4.5:1 on paper and all seven on night, on the
 * one surface where a rule color matters most. The reading carries no
 * selection any more (owner decision, 19.8), so the pair that replaced
 * Material's is no longer a surface's: [readingSelectionColors] is kept
 * because the mark it names is the one the reciter's word and a search
 * arrival draw behind the Quran's own letters, and the contrast test measures
 * the seven families on it there.
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
 * The reading's mark pair, named so the contrast test can measure it.
 *
 * It was the reading's copy selection until the reading stopped carrying one
 * (owner decision, 19.8). The mark is [PagePalette.highlight], the tone the
 * reciter's word and a search arrival draw behind the words, and it is the
 * strongest tint the seven rule colors keep their 4.5:1 on. The day page's
 * mark is 1.30 against the paper, the night's 1.40, and every family clears
 * the floor on both.
 */
internal fun readingSelectionColors(handle: Color, mark: Color): TextSelectionColors =
    TextSelectionColors(handleColor = handle, backgroundColor = mark)

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
    val wash = LocalPagePalette.current.match
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
