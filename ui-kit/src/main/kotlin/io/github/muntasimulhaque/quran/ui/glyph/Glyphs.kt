package io.github.muntasimulhaque.quran.ui.glyph

/**
 * The set's one ruler, and the only numbers a drawing may use.
 *
 * Every mark is drawn on a 24 unit square with a 2 unit margin of air around
 * it: a circle keyline of 19.5 units, a square of 18.5, a portrait
 * rectangle of 16.5 by 19.5, and a landscape rectangle of 19.5 by 16.5, all
 * measured to the drawing's outer ink. The circle is deliberately the largest:
 * a circle that matches a square's bounds reads smaller, because the eye
 * measures the widest point and a circle only touches its box at four points,
 * so it takes about a five percent overshoot to read the same size (the
 * keyline system every drawn set uses).
 *
 * The numbers are here rather than beside each drawing because they are one
 * rule, and the set had drifted into strokes between 1.14 and 1.98 dp and into
 * a live area between 44 and 85 percent of the box, one drawing at a time
 * (owner report). The keylines are what stop that from being possible again.
 *
 * ## Optical sizes
 *
 * A mark is the same line at every size. The line is 2 dp wherever it is
 * drawn: a 24 dp box draws 2 grid units and a 14 dp box draws 3.43, so the
 * unit stroke rises as the box falls. The set used to scale the unit stroke
 * with the box, and the tick in a chip carried a 1.28 dp line beside marks
 * with 2 dp; the eye saw the outlier before it saw the grid (owner report,
 * this session). The drawings are cut to survive the thickest stroke, the one
 * a 14 dp box draws: every mark keeps the two unit margin there.
 *
 * ## Screens
 *
 * None of this follows the screen. The unit is a fraction of the mark's own
 * box, the box is a dp, and a dp is a physical measure, so a phone, a seven
 * inch tablet, and a ten inch screen take the same mark. What grows on a
 * larger screen is the page around the marks, and that is the reading's
 * measure to keep (owner decision).
 */
object GlyphGrid {

    /** The square every drawing is measured on. */
    const val Size = 24f

    /** The set's one line weight, in grid units at 24 dp: 2 dp at every size. */
    const val Stroke = 2f

    /** A filled bar: the pause's two, the skips' posts, the tafsir's rolls. */
    const val Bar = 3f

    /** The live area: the room a drawing keeps at every edge of the square. */
    const val Margin = 2f

    /** A dot that is a thing: the ellipsis, and each end of a share. */
    const val Node = 2.25f

    /** A dot that is a detail: a bullet in a list. */
    const val Bullet = 1.6f

    /**
     * The set's stroke for a mark drawn in a box of [sizeDp], in grid units.
     *
     * The drawn line is one weight, 2 dp, at every size: a 24 dp box draws 2
     * units and a 14 dp box draws 3.43, so the unit stroke rises as the box
     * falls. A smaller mark is not a thinner mark. The set used to scale the
     * unit stroke with the box, and the tick in a chip carried a 1.28 dp line
     * beside marks with 2 dp, the outlier the eye could not unsee (owner
     * report, this session).
     */
    fun strokeFor(sizeDp: Float): Float = Stroke * Size / sizeDp
}

/** A point on the set's [GlyphGrid.Size] square. */
data class GlyphPoint(val x: Float, val y: Float)

/**
 * One part of one mark, in the set's own vocabulary.
 *
 * No shape carries a width: a line is the set's stroke for the box it is drawn
 * in, a bar is [GlyphGrid.Bar] wide, and a ring is a ring. A second line weight
 * cannot be drawn because the type cannot say it (owner decision).
 */
sealed interface GlyphShape {

    /** A stroked polyline, round at every turn and at both tips. */
    data class Line(val points: List<GlyphPoint>, val closed: Boolean = false) : GlyphShape

    /** A filled polygon, every corner rounded like a join. */
    data class Solid(val points: List<GlyphPoint>) : GlyphShape

    /** A stroked circle: the set's one hollow round. */
    data class Ring(val center: GlyphPoint, val radius: Float) : GlyphShape

    /** A filled circle. */
    data class Dot(val center: GlyphPoint, val radius: Float) : GlyphShape

    /** A filled line with round ends, [GlyphGrid.Bar] wide. */
    data class Bar(val from: GlyphPoint, val to: GlyphPoint) : GlyphShape

    /** A stroked rounded rectangle: a frame that is not round. */
    data class Frame(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val corner: Float,
    ) : GlyphShape
}

/**
 * The drawings, one per [Icon], on [GlyphGrid].
 *
 * Four rules hold the set together beyond the grid. Every turn is a round
 * join and every tip is round, so a mark ends the way the controls around it
 * end and two strokes meeting never leave a notch or a wedge at the joint;
 * the brand mark stays cut flat, because it is an illustration at launcher
 * scale and not a control; a joint is one polyline, never two strokes that
 * end at the same point, because two tips butted at an angle leave a wedge,
 * which was the clock hands' flaw at their centre (owner report); and a
 * filled shape carries the corner a join does, half the line, so the kept
 * ribbon, the play triangle, and the skips are round at every point too.
 *
 * Every angle is a multiple of 45 degrees, the geometry of the page's own
 * furniture, and a frame carries one corner, 2.5 units.
 *
 * Each mark fills its keyline by nature, not by force: a circle takes the
 * circle, a ribbon the portrait rectangle, a list the landscape one, and a
 * chevron keeps a narrower width because a turn drawn to 20 units reads as a
 * mountain. The three filled transport marks are measured by their mass, so
 * the play triangle's box runs right of the axis and its weight sits on it.
 *
 * The drawings are built once and shared: a mark is drawn on every frame the
 * pill moves, so a fresh list per frame would be garbage on the surface the
 * reader watches.
 */
object Glyphs {

    /** The drawing of [icon]. */
    fun of(icon: Icon): List<GlyphShape> = drawings.getValue(icon)

    private val drawings: Map<Icon, List<GlyphShape>> = mapOf(

        // A list: three marks and three lines. The line is a stroke with a
        // round tip, so the three lines stop at the landscape keyline while
        // the marks beside them keep their air.
        Icon.Browse to listOf(
            dot(4.75f, 6f, GlyphGrid.Bullet),
            dot(4.75f, 12f, GlyphGrid.Bullet),
            dot(4.75f, 18f, GlyphGrid.Bullet),
            line(9.5f, 6f, 20.75f, 6f),
            line(9.5f, 12f, 20.75f, 12f),
            line(9.5f, 18f, 20.75f, 18f),
        ),

        // The lens takes the circle keyline; the handle starts inside the
        // lens's own stroke, so no seam shows where the two meet, and runs to
        // the live area's corner.
        Icon.Search to listOf(
            ring(10.5f, 10.5f, 7.25f),
            line(16.33f, 16.33f, 20.75f, 20.75f),
        ),

        // Two rails, each with a knob to hold. A bead strung on a rail reads
        // as a dot at the sizes the app draws; a knob that crosses the rail
        // reads as the control a hand takes hold of.
        Icon.Settings to listOf(
            line(3.25f, 7f, 20.75f, 7f),
            line(3.25f, 17f, 20.75f, 17f),
            bar(8.25f, 5.5f, 8.25f, 8.5f),
            bar(15.75f, 15.5f, 15.75f, 18.5f),
        ),

        // The portrait keyline: a ribbon is a tall thing, and the notch is a
        // quarter of its height so the tail reads as a fold rather than a cut.
        Icon.Bookmark to listOf(
            closed(4.75f, 3.25f, 19.25f, 3.25f, 19.25f, 20.75f, 12f, 15.5f, 4.75f, 20.75f),
        ),

        // The same ribbon filled, drawn to the outline's outer edge so the two
        // states are one size rather than two.
        Icon.BookmarkFilled to listOf(
            solid(3.75f, 2.25f, 20.25f, 2.25f, 20.25f, 21.75f, 12f, 16f, 3.75f, 21.75f),
        ),

        Icon.Check to listOf(
            poly(3.75f, 11.9f, 9.2f, 17.4f, 20.25f, 6.6f),
        ),

        // Measured by its mass: the triangle's centroid sits on the axis, so
        // its box runs right of the middle, the place every set draws it.
        Icon.Play to listOf(
            solid(7.5f, 3.75f, 20.25f, 12f, 7.5f, 20.25f),
        ),

        Icon.Pause to listOf(
            bar(8.25f, 5.25f, 8.25f, 18.75f),
            bar(15.75f, 5.25f, 15.75f, 18.75f),
        ),

        Icon.Next to listOf(
            solid(5.5f, 3.75f, 14.5f, 12f, 5.5f, 20.25f),
            bar(17.5f, 5.25f, 17.5f, 18.75f),
        ),

        Icon.Previous to listOf(
            solid(18.5f, 3.75f, 9.5f, 12f, 18.5f, 20.25f),
            bar(6.5f, 5.25f, 6.5f, 18.75f),
        ),

        // Two strokes, round at every tip: the same end every other mark has.
        Icon.Close to listOf(
            line(5.75f, 5.75f, 18.25f, 18.25f),
            line(18.25f, 5.75f, 5.75f, 18.25f),
        ),

        // The three ends and the two lines between them. The lines start and
        // end inside the ends, so their tips are hidden and the mark reads as
        // one graph rather than three circles and two dashes.
        Icon.Share to listOf(
            dot(6.25f, 12f, GlyphGrid.Node),
            dot(17.75f, 6f, GlyphGrid.Node),
            dot(17.75f, 18f, GlyphGrid.Node),
            line(7.58f, 11.31f, 16.42f, 6.69f),
            line(7.58f, 12.69f, 16.42f, 17.31f),
        ),

        Icon.More to listOf(
            dot(4.75f, 12f, GlyphGrid.Node),
            dot(12f, 12f, GlyphGrid.Node),
            dot(19.25f, 12f, GlyphGrid.Node),
        ),

        // A gloss: a portrait page, a rule at its margin, two lines of the
        // text, and one note written beside them. It is not the reader's note
        // (a folded page) and not a bubble (comments). The page is portrait
        // because a page is a page, the rule is at two thirds so the margin is
        // the narrow column a gloss is written in, and there is one note
        // because the mark has one thing to say; two notes made it a document
        // (owner decision).
        Icon.Tafsir to listOf(
            frame(4.25f, 3.5f, 19.75f, 20.5f, 2.5f),
            line(13.5f, 6.5f, 13.5f, 17.5f),
            line(7.15f, 9f, 10.6f, 9f),
            line(7.15f, 13f, 10.6f, 13f),
            line(16.1f, 11f, 17.1f, 11f),
        ),

        // A turn, round at both tips and at the elbow. The width is the one
        // place the set departs from its live area: a chevron drawn to 20
        // units reads as a mountain rather than a turn.
        Icon.Chevron to listOf(
            poly(5.5f, 8.6f, 12f, 15.1f, 18.5f, 8.6f),
        ),

        // The sheet, its turned corner, and one line of writing.
        Icon.Note to listOf(
            closed(4.75f, 3.25f, 14.75f, 3.25f, 19.25f, 7.75f, 19.25f, 20.75f, 4.75f, 20.75f),
            poly(14.75f, 3.25f, 14.75f, 7.75f, 19.25f, 7.75f),
            line(8.25f, 13.75f, 16.25f, 13.75f),
        ),

        // Four o'clock: the minute at twelve and the hour at four, both short
        // of the ring so the dial keeps its air. Ten past ten is the face a
        // watch is photographed in, but at 24 dp its two hands in the upper
        // half read as a tick inside a ring before they read as a clock, and
        // the mark is asked what time it is, not whether the day is done
        // (owner report).
        Icon.Clock to listOf(
            ring(12f, 12f, 8.75f),
            poly(12f, 6.75f, 12f, 12f, 15.75f, 13.75f),
        ),

        // The frame, two rows of keys, and the space bar under them. A key is
        // a short round-capped dash, so it reads as a keycap, and the whole
        // mark is drawn with the set's one stroke and needs no third dot size.
        Icon.Keyboard to listOf(
            frame(3.25f, 4.75f, 20.75f, 19.25f, 2.5f),
            line(6.4f, 8.5f, 7.4f, 8.5f),
            line(11.5f, 8.5f, 12.5f, 8.5f),
            line(16.6f, 8.5f, 17.6f, 8.5f),
            line(6.4f, 11.8f, 7.4f, 11.8f),
            line(11.5f, 11.8f, 12.5f, 11.8f),
            line(16.6f, 11.8f, 17.6f, 11.8f),
            line(9.25f, 15f, 14.75f, 15f),
        ),
    )
}

private fun point(x: Float, y: Float) = GlyphPoint(x, y)

/** Pairs of coordinates, in the order the line visits them. */
private fun points(vararg values: Float): List<GlyphPoint> {
    require(values.size % 2 == 0) { "a point needs two coordinates, ${values.size} were given" }
    return List(values.size / 2) { index -> point(values[index * 2], values[index * 2 + 1]) }
}

private fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
    GlyphShape.Line(listOf(point(x1, y1), point(x2, y2)))

private fun poly(vararg values: Float) = GlyphShape.Line(points(*values))

private fun closed(vararg values: Float) = GlyphShape.Line(points(*values), closed = true)

private fun solid(vararg values: Float) = GlyphShape.Solid(points(*values))

private fun ring(x: Float, y: Float, radius: Float) =
    GlyphShape.Ring(point(x, y), radius)

private fun dot(x: Float, y: Float, radius: Float) =
    GlyphShape.Dot(point(x, y), radius)

private fun bar(x1: Float, y1: Float, x2: Float, y2: Float) =
    GlyphShape.Bar(point(x1, y1), point(x2, y2))

private fun frame(left: Float, top: Float, right: Float, bottom: Float, corner: Float) =
    GlyphShape.Frame(left, top, right, bottom, corner)
