package io.github.muntasimulhaque.quran.ui.glyph

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.min

/**
 * The sizes the app's marks are drawn at, named by the job each one does.
 *
 * Four numbers, and one rule: a mark is sized by the control it stands in,
 * never by the screen. A phone, a seven inch tablet, and a ten inch screen take
 * the same number, because dp is a physical measure and the finger that reaches
 * for the control does not grow with the glass. What grows on a larger screen is
 * the page around the marks, and that is the reading's measure to keep (owner
 * decision, this session).
 *
 * The set had six names over four jobs, and two of them were one job wearing
 * two numbers: the pill's transport and the search field's clear are both a mark
 * in the app's one round control, and they read 20 dp and 22 dp on the same
 * screen. A mark is the control's size now, so the two cannot drift apart (owner
 * decision, this session).
 *
 * The set does not follow the system font scale, for the reason it does not
 * follow the screen: a mark is a shape, not a word, and the control it stands
 * in is measured in dp. The label beside a mark grows with the reader's text,
 * and every mark that is a cue on its own is spoken as well, so the mark keeps
 * the size the drawing and the finger were made for (owner decision, this
 * session).
 */
object GlyphSize {

    /** A mark inside the app's one round control, [GlyphButton]'s target. */
    val Control = 24.dp

    /** The mark above the word of a labelled action, like the ayah bar's five. */
    val Action = 20.dp

    /** A chevron that ends a row or says a door opens. */
    val ChevronRow = 18.dp

    /** A chevron beside a word inside a compact control, like the pill's doors. */
    val ChevronInline = 14.dp

    /** The check inside a chip or a choice's ring. */
    val Check = 14.dp
}

/**
 * One mark, drawn from [Glyphs] on the set's grid, in the caller's ink.
 *
 * The drawing fills its box: the box is the job's size from [GlyphSize], the
 * mark spans the grid's live area inside it, and the two are the same on every
 * surface. A box that is not square keeps the drawing square and centered
 * rather than stretching it, because a mark drawn to a rectangle is a second
 * drawing. A filled shape takes the corner a join takes, half the line, so a
 * ribbon's edge and a triangle's point are as round as a stroke's elbow.
 */
@Composable
fun IconGlyph(
    icon: Icon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val shapes = Glyphs.of(icon)
    Canvas(modifier) {
        val unit = min(size.width, size.height) / GlyphGrid.Size
        val left = (size.width - GlyphGrid.Size * unit) / 2f
        val top = (size.height - GlyphGrid.Size * unit) / 2f
        // The set's one line weight for a mark this size, and its one end:
        // every turn and every tip is round, so a mark ends the way the
        // rounded controls around it end, and two strokes that meet never
        // leave a notch or a wedge at the joint.
        val weight = GlyphGrid.strokeFor(min(size.width, size.height) / density)
        val stroke = Stroke(
            width = weight * unit,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        fun at(point: GlyphPoint) = Offset(left + point.x * unit, top + point.y * unit)
        fun path(points: List<GlyphPoint>, closed: Boolean): Path = Path().apply {
            points.forEachIndexed { index, point ->
                val offset = at(point)
                if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
            }
            if (closed) close()
        }
        shapes.forEach { shape ->
            when (shape) {
                is GlyphShape.Line ->
                    drawPath(path(shape.points, shape.closed), tint, style = stroke)
                is GlyphShape.Solid ->
                    drawPath(solidPath(shape.points, weight / 2f * unit, { at(it) }), tint)
                is GlyphShape.Ring ->
                    drawCircle(tint, shape.radius * unit, at(shape.center), style = stroke)
                is GlyphShape.Dot ->
                    drawCircle(tint, shape.radius * unit, at(shape.center))
                is GlyphShape.Bar ->
                    drawLine(tint, at(shape.from), at(shape.to), GlyphGrid.Bar * unit, StrokeCap.Round)
                is GlyphShape.Frame -> drawRoundRect(
                    color = tint,
                    topLeft = Offset(left + shape.left * unit, top + shape.top * unit),
                    size = Size(
                        (shape.right - shape.left) * unit,
                        (shape.bottom - shape.top) * unit,
                    ),
                    cornerRadius = CornerRadius(shape.corner * unit),
                    style = stroke,
                )
            }
        }
    }
}

/**
 * A filled polygon with every corner rounded by [corner] pixels: the corner is
 * cut back along both edges and carried by one quadratic, which is the same
 * round the stroke's join paints at the same radius.
 */
private fun solidPath(points: List<GlyphPoint>, corner: Float, at: (GlyphPoint) -> Offset): Path {
    val path = Path()
    if (points.size < 3) return path
    val offsets = points.map(at)
    for (index in offsets.indices) {
        val previous = offsets[(index + offsets.size - 1) % offsets.size]
        val current = offsets[index]
        val next = offsets[(index + 1) % offsets.size]
        val inX = current.x - previous.x
        val inY = current.y - previous.y
        val outX = next.x - current.x
        val outY = next.y - current.y
        val inLength = hypot(inX, inY)
        val outLength = hypot(outX, outY)
        if (inLength < 0.0001f || outLength < 0.0001f) continue
        val cut = min(corner, min(inLength, outLength) / 2f)
        val startX = current.x - inX / inLength * cut
        val startY = current.y - inY / inLength * cut
        val endX = current.x + outX / outLength * cut
        val endY = current.y + outY / outLength * cut
        if (index == 0) path.moveTo(startX, startY) else path.lineTo(startX, startY)
        path.quadraticBezierTo(current.x, current.y, endX, endY)
    }
    path.close()
    return path
}
