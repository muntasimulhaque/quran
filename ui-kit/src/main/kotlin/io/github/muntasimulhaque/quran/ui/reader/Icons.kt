package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Every glyph the interface needs, drawn by hand so nothing is borrowed. */
enum class Icon {
    Browse,
    Search,
    Settings,
    Bookmark,
    BookmarkFilled,
    Check,
    Play,
    Pause,
    Next,
    Previous,
    Close,
    Share,
    More,
    Tafsir,
    Chevron,
    Note,

    /** The two halves of the time choice: the clock, and typing on a keyboard. */
    Clock,
    Keyboard,
}

/**
 * The sizes the app's marks are drawn at, named once: a chevron beside a word
 * inside a compact control, a chevron that ends a row or opens a door, a
 * chevron that is a way back, the mark of one action in the ayah bar, and the
 * check a chosen chip or a choice's ring carries.
 *
 * The chevron had grown six sizes, 12 through 22, and most of them were
 * drift: the same mark ending two rows read two sizes, and the number beside
 * each call site was chosen by the hand that last touched it. The sizes here
 * are the jobs the mark does, so a row's tail is a row's tail on every
 * surface (owner decision, this session).
 */
object GlyphSize {

    /** A chevron beside a word inside a compact control, like the pill's doors. */
    val ChevronInline = 14.dp

    /** A chevron that ends a row or says a door opens. */
    val ChevronRow = 18.dp

    /** A chevron that is a way back or out, the size of the icon buttons beside it. */
    val ChevronNav = 22.dp

    /** The mark above one action in the ayah bar. */
    val Action = 19.dp

    /** A transport mark in the playback row. */
    val Transport = 20.dp

    /** The check inside a chip or a choice's ring. */
    val Check = 14.dp
}

@Composable
fun IconGlyph(
    icon: Icon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (icon) {
            Icon.Browse -> {
                val rows = listOf(0.30f, 0.5f, 0.70f)
                for (y in rows) {
                    drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.2f, h * y))
                    drawLine(
                        tint,
                        Offset(w * 0.36f, h * y),
                        Offset(w * 0.82f, h * y),
                        strokeWidth = w * 0.075f,
                    )
                }
            }
            Icon.Search -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.26f,
                    center = Offset(w * 0.44f, h * 0.44f),
                    style = Stroke(width = w * 0.085f),
                )
                drawLine(
                    tint,
                    Offset(w * 0.63f, h * 0.63f),
                    Offset(w * 0.84f, h * 0.84f),
                    strokeWidth = w * 0.09f,
                )
            }
            Icon.Settings -> {
                for (y in listOf(0.32f, 0.68f)) {
                    drawLine(tint, Offset(w * 0.18f, h * y), Offset(w * 0.82f, h * y), w * 0.075f)
                }
                drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.36f, h * 0.32f))
                drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.66f, h * 0.68f))
            }
            Icon.Bookmark, Icon.BookmarkFilled -> {
                val path = Path().apply {
                    moveTo(w * 0.28f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.84f)
                    lineTo(w * 0.5f, h * 0.68f)
                    lineTo(w * 0.28f, h * 0.84f)
                    close()
                }
                if (icon == Icon.BookmarkFilled) {
                    drawPath(path, tint)
                } else {
                    drawPath(path, tint, style = Stroke(width = w * 0.08f))
                }
            }
            // One polyline rather than two lines: two flat caps met at the
            // elbow and left a notch on its underside, which at the 18 dp
            // the chooser draws read as a clipped corner (owner report). The
            // round join carries the stroke through the turn, and the tips
            // stay butt-cut, the way every other glyph in the set ends.
            Icon.Check -> {
                drawPath(
                    Path().apply {
                        moveTo(w * 0.16f, h * 0.52f)
                        lineTo(w * 0.42f, h * 0.78f)
                        lineTo(w * 0.84f, h * 0.22f)
                    },
                    tint,
                    style = Stroke(width = w * 0.13f, join = StrokeJoin.Round),
                )
            }
            Icon.Play -> drawPath(playPath(w, h), tint)
            Icon.Pause -> {
                drawRoundRect(tint, Offset(w * 0.28f, h * 0.2f), Size(w * 0.16f, h * 0.6f), CornerRadius(w * 0.05f))
                drawRoundRect(tint, Offset(w * 0.56f, h * 0.2f), Size(w * 0.16f, h * 0.6f), CornerRadius(w * 0.05f))
            }
            Icon.Next -> {
                drawPath(playPath(w * 0.62f, h * 0.72f, offset = Offset(w * 0.1f, h * 0.14f)), tint)
                drawRoundRect(tint, Offset(w * 0.74f, h * 0.16f), Size(w * 0.11f, h * 0.68f), CornerRadius(w * 0.04f))
            }
            Icon.Previous -> {
                drawPath(playPath(w * 0.62f, h * 0.72f, offset = Offset(w * 0.28f, h * 0.14f), flip = true), tint)
                drawRoundRect(tint, Offset(w * 0.15f, h * 0.16f), Size(w * 0.11f, h * 0.68f), CornerRadius(w * 0.04f))
            }
            Icon.Close -> {
                drawLine(tint, Offset(w * 0.26f, h * 0.26f), Offset(w * 0.74f, h * 0.74f), w * 0.09f)
                drawLine(tint, Offset(w * 0.74f, h * 0.26f), Offset(w * 0.26f, h * 0.74f), w * 0.09f)
            }
            // The connectors stop at each ring's outer edge rather than at
            // its centre: a line run through a hollow ring left a stub
            // hanging inside it, which at the 19 dp the action bar draws
            // read as a clipped mark (owner report). The dot stays whole,
            // the rings stay hollow, and each line meets its ring where the
            // ring begins.
            Icon.Share -> {
                val top = Offset(w * 0.62f, h * 0.22f)
                val left = Offset(w * 0.28f, h * 0.5f)
                val bottom = Offset(w * 0.62f, h * 0.78f)
                val ringEdge = w * 0.145f
                drawLine(tint, left, stopBefore(top, left, ringEdge), w * 0.06f)
                drawLine(tint, left, stopBefore(bottom, left, ringEdge), w * 0.06f)
                drawCircle(tint, radius = w * 0.11f, center = top, style = Stroke(w * 0.07f))
                drawCircle(tint, radius = w * 0.11f, center = bottom, style = Stroke(w * 0.07f))
                drawCircle(tint, radius = w * 0.09f, center = left)
            }
            Icon.More -> {
                for (x in listOf(0.28f, 0.5f, 0.72f)) {
                    drawCircle(tint, radius = w * 0.07f, center = Offset(w * x, h * 0.5f))
                }
            }
            // A tafsir: a scroll, the commentary a reader unrolls beside the
            // page. It is deliberately not a book, so it never reads as the
            // reading itself. A scroll is one sheet between two rolls,
            // with two lines of explanation on it, drawn to the weight of
            // the rest of the set.
            Icon.Tafsir -> {
                val stroke = w * 0.07f
                val rollRadius = CornerRadius(h * 0.07f)
                // The two rolled ends, above and below the sheet.
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.2f, h * 0.15f),
                    size = Size(w * 0.6f, h * 0.14f),
                    cornerRadius = rollRadius,
                    style = Stroke(width = stroke),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.2f, h * 0.71f),
                    size = Size(w * 0.6f, h * 0.14f),
                    cornerRadius = rollRadius,
                    style = Stroke(width = stroke),
                )
                // The sheet the rolls hold, its edges inside them.
                drawLine(tint, Offset(w * 0.29f, h * 0.29f), Offset(w * 0.29f, h * 0.71f), stroke)
                drawLine(tint, Offset(w * 0.71f, h * 0.29f), Offset(w * 0.71f, h * 0.71f), stroke)
                // Two lines of commentary on the sheet.
                drawLine(tint, Offset(w * 0.4f, h * 0.43f), Offset(w * 0.6f, h * 0.43f), stroke)
                drawLine(tint, Offset(w * 0.4f, h * 0.57f), Offset(w * 0.6f, h * 0.57f), stroke)
            }
            // One polyline rather than two lines, the same rule the check
            // follows: two flat caps met at the elbow and left a notch on
            // the outside of the turn, which at the 18 dp a settings row
            // draws read as a clipped corner (owner report). The round join
            // carries the stroke through the turn, and the tips stay
            // butt-cut, the way every other glyph in the set ends.
            Icon.Chevron -> {
                drawPath(
                    Path().apply {
                        moveTo(w * 0.28f, h * 0.4f)
                        lineTo(w * 0.5f, h * 0.62f)
                        lineTo(w * 0.72f, h * 0.4f)
                    },
                    tint,
                    style = Stroke(width = w * 0.085f, join = StrokeJoin.Round),
                )
            }
            // A clock with the hour hand at ten and the minute hand at two,
            // the reading position a clock face is drawn in everywhere, so the
            // mark is a clock even at 18 dp. It is the door to the time of the
            // daily reminder, which is the only clock this app asks for.
            Icon.Clock -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.33f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = w * 0.075f),
                )
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.27f), w * 0.075f)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.66f, h * 0.57f), w * 0.075f)
            }
            // A keyboard: the frame, two rows of keys, and the space bar under
            // them. It is the same message as the clock's opposite half: the
            // time can be typed instead of turned.
            Icon.Keyboard -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.11f, h * 0.3f),
                    size = Size(w * 0.78f, h * 0.4f),
                    cornerRadius = CornerRadius(w * 0.09f),
                    style = Stroke(width = w * 0.07f),
                )
                for (y in listOf(0.42f, 0.55f)) {
                    for (x in listOf(0.26f, 0.4f, 0.54f, 0.68f, 0.79f)) {
                        drawCircle(tint, radius = w * 0.028f, center = Offset(w * x, h * y))
                    }
                }
                drawLine(tint, Offset(w * 0.36f, h * 0.63f), Offset(w * 0.64f, h * 0.63f), w * 0.055f)
            }
            // A note: a sheet with its turned corner and a line of writing,
            // drawn to the same weight as the glyphs beside it, so the pill's
            // note action reads as part of the set rather than a stock mark.
            Icon.Note -> {
                val stroke = w * 0.075f
                val corner = w * 0.62f
                drawPath(
                    Path().apply {
                        moveTo(w * 0.22f, h * 0.14f)
                        lineTo(corner, h * 0.14f)
                        lineTo(w * 0.78f, h * 0.32f)
                        lineTo(w * 0.78f, h * 0.86f)
                        lineTo(w * 0.22f, h * 0.86f)
                        close()
                    },
                    tint,
                    style = Stroke(width = stroke),
                )
                // The turned corner, so the sheet reads as a note rather than
                // a plain page.
                drawLine(tint, Offset(corner, h * 0.14f), Offset(corner, h * 0.32f), stroke)
                drawLine(tint, Offset(corner, h * 0.32f), Offset(w * 0.78f, h * 0.32f), stroke)
                // One line of writing.
                drawLine(tint, Offset(w * 0.33f, h * 0.58f), Offset(w * 0.67f, h * 0.58f), stroke)
            }
        }
    }
}


/**
 * The point [distance] from [center] on the way back to [from], the point a
 * stroke meets a ring of that outer radius instead of running into it.
 */
private fun stopBefore(center: Offset, from: Offset, distance: Float): Offset {
    val dx = center.x - from.x
    val dy = center.y - from.y
    val length = kotlin.math.hypot(dx, dy)
    if (length < 0.0001f) return center
    return Offset(center.x - dx / length * distance, center.y - dy / length * distance)
}

private fun playPath(w: Float, h: Float, offset: Offset = Offset.Zero, flip: Boolean = false): Path =
    Path().apply {
        val x0 = if (flip) 0.78f else 0.22f
        val x1 = if (flip) 0.22f else 0.78f
        moveTo(offset.x + w * x0, offset.y + h * 0.1f)
        lineTo(offset.x + w * x1, offset.y + h * 0.5f)
        lineTo(offset.x + w * x0, offset.y + h * 0.9f)
        close()
    }
