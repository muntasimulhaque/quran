package io.github.muntasimulhaque.quran.ui.glyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The set's ruler, measured by arithmetic on a machine with no screen.
 *
 * A drawing is one of the set only if it keeps the grid's margin, fills the
 * live area, sits on the axis, and uses the set's stroke, bar, and two dot
 * sizes. Every one of those was two answers before the grid: the top bar drew a
 * magnifier half again the area of the list beside it, the strokes ran from
 * 0.06 to 0.13 of the box, and the same Play was drawn at 19 dp and 20 dp
 * (owner report). The numbers live in [Glyphs], so this is the only place they
 * can be read without a screen.
 *
 * The ink box is the drawing's geometry with the stroke on it: a round tip and
 * a round join both add their half stroke, and a filled shape ends at its
 * points. It is the same model the renderer draws with, which is why a mark's
 * margin is a fact here rather than a hope.
 */
class GlyphsTest {

    @Test
    fun everyIconHasADrawing() {
        Icon.entries.forEach { icon ->
            assertTrue("$icon has no drawing", Glyphs.of(icon).isNotEmpty())
        }
    }

    @Test
    fun everyMarkInksInsideTheMargin() {
        // A regular mark stands in a 20 dp or a 24 dp box; the two compact
        // marks, the door's chevron and the chip's check, stand in a 14 dp one
        // and carry the thickest line in the set. Each is asked for the margin
        // at its own job's line.
        val regular = GlyphGrid.strokeFor(20f)
        val compact = GlyphGrid.strokeFor(14f)
        Icon.entries.forEach { icon ->
            val stroke = if (icon == Icon.Chevron || icon == Icon.Check) compact else regular
            val ink = inkOf(icon, stroke)
            assertTrue(
                "$icon inks outside the set's ${GlyphGrid.Margin} unit margin at $stroke units: $ink",
                ink.left >= GlyphGrid.Margin - Tolerance &&
                    ink.top >= GlyphGrid.Margin - Tolerance &&
                    ink.right <= GlyphGrid.Size - GlyphGrid.Margin + Tolerance &&
                    ink.bottom <= GlyphGrid.Size - GlyphGrid.Margin + Tolerance,
            )
        }
    }

    @Test
    fun everyMarkReadsAtOneSize() {
        Icon.entries.forEach { icon ->
            val ink = inkOf(icon, GlyphGrid.Stroke)
            val long = maxOf(ink.width, ink.height)
            assertTrue("$icon is a speck: $ink", long >= lowestLiveArea - Tolerance)
            assertTrue("$icon fills past the live area: $ink", long <= 20f + Tolerance)
        }
    }

    @Test
    fun everyMarkSitsOnItsAxis() {
        Icon.entries.forEach { icon ->
            val ink = inkOf(icon, GlyphGrid.Stroke)
            assertTrue(
                "$icon is not on the set's axis: $ink",
                abs(ink.centerX - axis) <= axisRoom + Tolerance &&
                    abs(ink.centerY - axis) <= axisRoom + Tolerance,
            )
        }
    }

    @Test
    fun thePlayTriangleIsMeasuredByItsMass() {
        val triangle = Glyphs.of(Icon.Play).single() as GlyphShape.Solid
        // A triangle's mass sits left of its box, so a triangle centered by its
        // box reads a step right of the mark beside it. Every set draws the
        // mass on the axis and lets the box run right (owner decision).
        val mass = triangle.points.sumOf { it.x.toDouble() } / triangle.points.size
        assertTrue(
            "the play triangle's mass is at $mass, not on the axis",
            abs(mass - axis.toDouble()) <= 0.5,
        )
    }

    @Test
    fun everyRingReadsHollow() {
        Icon.entries.flatMap { Glyphs.of(it) }
            .filterIsInstance<GlyphShape.Ring>()
            .forEach { ring ->
                // The thickest stroke is the one a 14 dp box draws.
                val inner = ring.radius - GlyphGrid.strokeFor(14f) / 2f
                assertTrue(
                    "a ring of radius ${ring.radius} leaves only $inner units of hole",
                    inner >= 1.5f - Tolerance,
                )
            }
    }

    @Test
    fun everyDotIsTheSetsOwnSize() {
        Icon.entries.flatMap { Glyphs.of(it) }
            .filterIsInstance<GlyphShape.Dot>()
            .forEach { dot ->
                assertTrue(
                    "a dot of radius ${dot.radius} is neither a node nor a bullet",
                    abs(dot.radius - GlyphGrid.Node) <= Tolerance ||
                        abs(dot.radius - GlyphGrid.Bullet) <= Tolerance,
                )
            }
    }

    @Test
    fun theSizesAreTheSetsOwn() {
        // Four numbers over five names: a mark inside the app's round control,
        // a mark above a labelled action, a row's tail, and the two 14 dp marks
        // that sit inside a smaller shape.
        assertEquals("the control's mark", 24f, GlyphSize.Control.value, Tolerance)
        assertEquals("the action's mark", 20f, GlyphSize.Action.value, Tolerance)
        assertEquals("a row's tail", 18f, GlyphSize.ChevronRow.value, Tolerance)
        assertEquals("a door's chevron", 14f, GlyphSize.ChevronInline.value, Tolerance)
        assertEquals("a chip's check", 14f, GlyphSize.Check.value, Tolerance)
    }

    @Test
    fun theStrokeHoldsItsWeightAcrossSizes() {
        // One line, 2 dp, wherever a mark is drawn: the unit stroke rises as
        // the box falls, so a 14 dp tick is not a thinner line than a 24 dp
        // door. This is the rule the check's bar used to break (owner report).
        val sizes = listOf(24f, 22f, 20f, 18f, 14f)
        val strokes = sizes.map { GlyphGrid.strokeFor(it) }
        strokes.zipWithNext().forEach { (big, small) ->
            assertTrue("a $small unit stroke is not heavier than a $big unit one", small > big)
        }
        sizes.forEach { size ->
            val line = GlyphGrid.strokeFor(size) / GlyphGrid.Size * size
            assertEquals("a mark of $size dp carries a $line dp line", 2f, line, Tolerance)
        }
    }

    /** The middle of the square, in grid units. */
    private val axis = GlyphGrid.Size / 2f

    /** How far a mark's box may sit from the axis and still read as placed. */
    private val axisRoom = 2f

    /** The shortest a mark's long side may be, and still read as a mark. */
    private val lowestLiveArea = 13.5f

    private fun inkOf(icon: Icon, stroke: Float): Ink = inkOf(Glyphs.of(icon), stroke)

    private fun inkOf(shapes: List<GlyphShape>, stroke: Float): Ink {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        fun add(x0: Float, y0: Float, x1: Float, y1: Float) {
            left = minOf(left, x0)
            top = minOf(top, y0)
            right = maxOf(right, x1)
            bottom = maxOf(bottom, y1)
        }
        fun addPoint(x: Float, y: Float, half: Float) =
            add(x - half, y - half, x + half, y + half)
        shapes.forEach { shape ->
            val half = stroke / 2f
            when (shape) {
                is GlyphShape.Line -> {
                    val points = shape.points
                    // Each segment is a quad offset by the half stroke.
                    for (index in 0 until points.size - 1) {
                        addSegment(add = ::add, from = points[index], to = points[index + 1], half = half)
                    }
                    if (shape.closed && points.size > 1) {
                        addSegment(add = ::add, from = points.last(), to = points.first(), half = half)
                    }
                    // A round tip and a round join are both a disk at the
                    // vertex, and the set has no other kind of vertex.
                    points.forEach { addPoint(it.x, it.y, half) }
                }
                is GlyphShape.Solid ->
                    shape.points.forEach { add(it.x, it.y, it.x, it.y) }
                is GlyphShape.Ring -> addPoint(shape.center.x, shape.center.y, shape.radius + half)
                is GlyphShape.Dot -> addPoint(shape.center.x, shape.center.y, shape.radius)
                is GlyphShape.Bar -> {
                    val bar = GlyphGrid.Bar / 2f
                    addPoint(shape.from.x, shape.from.y, bar)
                    addPoint(shape.to.x, shape.to.y, bar)
                }
                is GlyphShape.Frame -> add(
                    shape.left - half,
                    shape.top - half,
                    shape.right + half,
                    shape.bottom + half,
                )
            }
        }
        return Ink(left, top, right, bottom)
    }

    private fun addSegment(
        add: (Float, Float, Float, Float) -> Unit,
        from: GlyphPoint,
        to: GlyphPoint,
        half: Float,
    ) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = hypot(dx, dy)
        if (length < 0.0001f) {
            add(from.x - half, from.y - half, from.x + half, from.y + half)
            return
        }
        val nx = -dy / length * half
        val ny = dx / length * half
        add(
            minOf(from.x, to.x) - abs(nx),
            minOf(from.y, to.y) - abs(ny),
            maxOf(from.x, to.x) + abs(nx),
            maxOf(from.y, to.y) + abs(ny),
        )
    }

    private data class Ink(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val width get() = right - left
        val height get() = bottom - top
        val centerX get() = (left + right) / 2f
        val centerY get() = (top + bottom) / 2f
        override fun toString() =
            "ink " + width + " x " + height + ", center (" + centerX + ", " + centerY + ")"
    }

    private companion object {
        const val Tolerance = 0.01f
    }
}
