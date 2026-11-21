package io.github.muntasimulhaque.quran.ui.rich

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.muntasimulhaque.quran.core.TajweedCodec
import io.github.muntasimulhaque.quran.ui.theme.TajweedColors
import kotlin.math.max
import kotlin.math.min

/**
 * One run of one color over the ayah's own characters.
 *
 * The code is the QUL annotation, one character per Quran character. Adjacent
 * characters with the same rule are one run; the color is the family the
 * theme gives that rule.
 */
data class TajweedColorRun(val start: Int, val end: Int, val color: Color)

/**
 * The color runs a code line asks for, in text order. A code that is off or
 * empty asks for nothing; a caller never has to build an AnnotatedString to
 * know where a color belongs.
 */
fun tajweedColorRuns(
    code: String,
    colors: TajweedColors,
    enabled: Boolean = true,
): List<TajweedColorRun> {
    if (!enabled || code.isEmpty()) return emptyList()
    return TajweedCodec.spans(code).map { span ->
        TajweedColorRun(span.start, span.end, colors.of(span.rule))
    }
}

/**
 * Draws the reading's tajweed colors over a line that was shaped once.
 *
 * A color span is not a paint decision to Android's text engine: a
 * `SpanStyle` boundary makes the line shape the run again. A fathah that
 * starts the new run loses the letter it belongs to and is drawn at the
 * bottom of the line; a lam-alef ligature is split; and the whole line
 * reflows when the Tajweed switch moves. The reading therefore shapes the
 * line once, draws it once in ink, and paints each family color here, over
 * the already shaped layout, clipped to the characters the code names (owner
 * decision, the QPC rendering audit).
 *
 * The clip is the character's own box. A rule that falls entirely inside a
 * shaped ligature has no box of its own, so it paints the ligature's box:
 * one glyph cannot be split, and a missing color is worse than a wider one.
 * The mark's own position is always the shaped one, because this function
 * never touches shaping.
 *
 * A character's box is the line's height at its advance, so a vowel whose own
 * code is 0 takes its letter's color when the letter is in the run. The
 * platform exposes no way to clip a shaped glyph between a letter and its
 * mark, and the mark's position is exact; the color follows the letter it
 * belongs to.
 */
fun DrawScope.drawTajweedColors(
    layout: TextLayoutResult,
    code: String,
    colors: TajweedColors,
    enabled: Boolean,
) {
    if (!enabled) return
    val paint = tajweedPaint(layout, code, colors) ?: return
    drawTajweed(layout, paint)
}

/**
 * One line's color geometry: the runs the code names, and one path per family
 * holding every box they cover.
 *
 * This is the expensive half of the paint and the only half that depends on
 * the shape of the line. It is a plain function and not a draw, so a caller
 * can compute it once, keep it, and only paint with it. The reading used to
 * recompute it on every redraw of every ayah, which is a
 * `layout.getBoundingBox` for every character of the verse (owner decision,
 * 19.8).
 */
fun tajweedPaint(
    layout: TextLayoutResult,
    code: String,
    colors: TajweedColors,
): TajweedPaint? {
    val text = layout.layoutInput.text.text
    if (code.isEmpty() || code.length > text.length) return null
    val runs = tajweedColorRuns(code, colors)
    if (runs.isEmpty()) return null
    val paths = LinkedHashMap<Color, Path>()
    for (run in runs) {
        val from = run.start.coerceIn(0, code.length)
        val to = run.end.coerceIn(from, code.length)
        if (to <= from) continue
        var line = -1
        var left = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        var hasBox = false

        fun flush() {
            if (line >= 0 && right > left && bottom > top) {
                paths.getOrPut(run.color) { Path() }.addRect(Rect(left, top, right, bottom))
            }
            line = -1
            left = Float.MAX_VALUE
            right = -Float.MAX_VALUE
            top = Float.MAX_VALUE
            bottom = -Float.MAX_VALUE
        }

        for (offset in from until to) {
            if (offset !in text.indices) continue
            val box = layout.getBoundingBox(offset)
            if (box.right <= box.left) continue
            val boxLine = layout.getLineForOffset(offset)
            if (boxLine != line) {
                flush()
                line = boxLine
            }
            left = min(left, box.left)
            right = max(right, box.right)
            top = min(top, box.top)
            bottom = max(bottom, box.bottom)
            hasBox = true
        }
        flush()
        if (!hasBox) {
            val base = clusterBase(text, from)
            if (base >= 0) {
                val box = layout.getBoundingBox(base)
                if (box.right > box.left) {
                    paths.getOrPut(run.color) { Path() }.addRect(box)
                }
            }
        }
    }
    if (paths.isEmpty()) return null
    return TajweedPaint(paths.entries.map { it.key to it.value })
}

/** One shaped line's color geometry, ready to paint. */
class TajweedPaint internal constructor(
    /** One path per family, in the order the runs named them. */
    internal val paths: List<Pair<Color, Path>>,
)

/**
 * Paints [paint] over [layout]: one clipped copy of the line per family.
 *
 * The line was shaped once, so nothing here can move a letter or a mark; each
 * family is the same text drawn again through its own clip.
 */
fun DrawScope.drawTajweed(layout: TextLayoutResult, paint: TajweedPaint) {
    for ((color, path) in paint.paths) {
        clipPath(path) { drawText(textLayoutResult = layout, color = color) }
    }
}

/**
 * One ayah's paint, kept between draws.
 *
 * A line's geometry depends on exactly three things: the shaped layout, the
 * code, and the palette. The layout lands once when the line is measured, the
 * palette changes when the reader changes the theme, and the code never
 * changes after the content is read. A wash that moves while the reciter
 * reads, the search arrival's mark, and a scroll all redraw the line without
 * changing any of the three, so they are answered by the paint already in
 * hand. One entry is the whole cache: a block paints its own line and nothing
 * else (owner decision, 19.8).
 */
class TajweedPainter {

    private var layout: TextLayoutResult? = null
    private var code: String = ""
    private var colors: TajweedColors? = null
    private var paint: TajweedPaint? = null

    /** [layout]'s paint in [colors], computed only when one of the three changed. */
    fun paint(layout: TextLayoutResult, code: String, colors: TajweedColors): TajweedPaint? {
        if (this.layout === layout && this.code == code && this.colors == colors) return paint
        val next = tajweedPaint(layout, code, colors)
        this.layout = layout
        this.code = code
        this.colors = colors
        this.paint = next
        return next
    }
}

/**
 * Draws a wash behind a shaped line, one rounded box per line of each range.
 *
 * A background span is a `CharacterStyle`, so it re-shapes the line the same
 * way a color span does, and a match can change the width of the result under
 * the reader as they type. The wash is geometry from the layout instead, so
 * the words never move (owner decision, the QPC rendering audit).
 *
 * The box is the reading's own word mark: a pad wider each side, half a pad
 * shorter above and below, and a corner at a share of the ink box's height.
 * One recipe serves every matched run, the search sheet's Arabic lines, its
 * prose snippets, and the reading's own marks, so the same word cannot be
 * rounded in one place and square in another (owner decision, 21.1: the
 * search sheet wore rectangles while the reading rounded the same match).
 * The washes are opaque tones, so two boxes that touch draw as one; a
 * translucent wash would need the union of the boxes restored first.
 *
 * An ellipsized line washes only the text it draws: a match past the cut has
 * no box on the glass and draws nothing.
 */
fun DrawScope.drawWash(
    layout: TextLayoutResult,
    ranges: List<IntRange>,
    color: Color,
) {
    if (ranges.isEmpty() || color.alpha == 0f) return
    if (layout.lineCount == 0) return
    val visibleEnd = layout.getLineEnd(layout.lineCount - 1, visibleEnd = true)
    val last = min(layout.layoutInput.text.length, visibleEnd) - 1
    for (range in ranges) {
        val start = range.first.coerceAtLeast(0)
        val end = range.last.coerceAtMost(last)
        if (end < start) continue
        var line = layout.getLineForOffset(start)
        var box: Rect? = null
        for (offset in start..end) {
            val nextLine = layout.getLineForOffset(offset)
            if (nextLine != line) {
                box?.let { drawWashBox(it, color) }
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
        box?.let { drawWashBox(it, color) }
    }
}

/** The share of an ink box's height a wash grows each side. */
internal const val WashPadShare = 0.08f

/** The share of an ink box's height a wash's corner rounds at. */
internal const val WashCornerShare = 0.26f

/**
 * The box a wash wears around one line's ink: a pad wider each side and
 * half a pad shorter above and below, so the mark clears the letters beside
 * it without eating the line it stands on.
 */
internal fun washBox(box: Rect): Rect {
    val pad = box.height * WashPadShare
    return Rect(
        left = box.left - pad,
        top = box.top + pad * 0.5f,
        right = box.right + pad,
        bottom = box.bottom - pad * 0.5f,
    )
}

/** The corner a wash rounds at, a share of the ink box's own height. */
internal fun washCorner(height: Float): Float = height * WashCornerShare

/** One line's wash: the rounded box, drawn behind the shaped line. */
private fun DrawScope.drawWashBox(box: Rect, color: Color) {
    val rect = washBox(box)
    if (rect.width <= 0f || rect.height <= 0f) return
    drawRoundRect(
        color = color,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(washCorner(box.height)),
    )
}

/**
 * The one Arabic text view that carries the reading's colors.
 *
 * The line is shaped once. The wash is drawn behind it, the text in its own
 * ink, and the colors over it, so the letters, the joins, the ligatures, the
 * marks, the line breaks, and the reader's place are the same whether the
 * Tajweed switch is on or off (owner decision, the QPC rendering audit).
 *
 * The caller passes the text as it wants it drawn. The only span this view
 * expects is the ayah number's own ornament span, which sits after the ayah
 * and is never part of the code.
 */
@Composable
fun TajweedText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    code: String = "",
    colors: TajweedColors? = null,
    enabled: Boolean = false,
    wash: List<IntRange> = emptyList(),
    washColor: Color = Color.Transparent,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        modifier = modifier.drawWithContent {
            val result = layout
            if (result == null) {
                drawContent()
                return@drawWithContent
            }
            drawWash(result, wash, washColor)
            drawContent()
            if (colors != null) drawTajweedColors(result, code, colors, enabled)
        },
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { value ->
            layout = value
            onTextLayout?.invoke(value)
        },
    )
}

/**
 * The start of the grapheme cluster an offset belongs to.
 *
 * A shaped ligature gives its first character the total width and its later
 * characters zero width, so a rule that covers only a later character has no
 * box of its own. Walking back through marks to the base finds the box that
 * carries the glyph. A format character such as ZWNJ stays with the base it
 * follows.
 */
private fun clusterBase(text: String, offset: Int): Int {
    if (text.isEmpty()) return -1
    var index = offset.coerceIn(0, text.length - 1)
    if (isCombining(text.codePointAt(index))) {
        while (index > 0 && isCombining(text.codePointAt(index))) {
            index -= Character.charCount(text.codePointBefore(index))
        }
        return index
    }
    var back = index
    while (back > 0 && isCombining(text.codePointBefore(back))) {
        back -= Character.charCount(text.codePointBefore(back))
    }
    if (back < index && back > 0) {
        back -= Character.charCount(text.codePointBefore(back))
    }
    return back
}

private fun isCombining(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
    Character.NON_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt(),
    Character.FORMAT.toInt(),
    -> true

    else -> false
}
