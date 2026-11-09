package io.github.muntasimulhaque.quran.ui.playback

import io.github.muntasimulhaque.quran.ui.glyph.GlyphGrid
import io.github.muntasimulhaque.quran.ui.glyph.GlyphShape
import io.github.muntasimulhaque.quran.playback.NotificationMark
import io.github.muntasimulhaque.quran.ui.glyph.Glyphs
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The shade draws the same marks the pill draws, and the drawing lives once.
 *
 * The notification is the one surface the app cannot draw with Compose, so its
 * four transport marks are vector files beside the media session. They are not
 * a second hand: each file carries the numbers [Glyphs] draws, on the same 24
 * unit square, and this test reads the files against the set through
 * [NotificationMark], which names the pair in the app's own code. The two
 * shapes had already drifted once, and nothing could see it (owner report, an
 * earlier session).
 *
 * Two of the four are the shade's alone now: the pill became one row with one
 * mark (owner decision, this session), and its skips are the actions that mark
 * offers to a screen reader. The drawings are still the app's, and this test
 * still holds them to it.
 */
class NotificationGlyphsTest {

    @Test
    fun eachTransportVectorCarriesTheSetsDrawing() {
        NotificationMark.entries.forEach { mark ->
            val parts = partsOf(mark.fileName)
            val drawn = drawnParts(mark.icon)
            assertEquals(
                "${mark.fileName} carries ${parts.size} parts for ${mark.icon}",
                drawn.size,
                parts.size,
            )
            drawn.forEach { expected ->
                assertTrue(
                    "${mark.fileName} does not carry the ${mark.icon} drawing: " +
                        "$expected is not among $parts",
                    parts.any { it == expected },
                )
            }
        }
    }

    @Test
    fun aBarInTheShadeIsTheSetsOwnBar() {
        NotificationMark.entries.forEach { mark ->
            val icon = mark.icon
            val file = mark.fileName
            val bars = Glyphs.of(icon).filterIsInstance<GlyphShape.Bar>()
            if (bars.isEmpty()) return@forEach
            val text = vector(file).readText()
            val widths = Regex("""android:strokeWidth="([\d.]+)"""")
                .findAll(text)
                .map { it.groupValues[1].toFloat() }
                .toList()
            assertEquals("$file strokes ${widths.size} bars for ${bars.size} bars", bars.size, widths.size)
            widths.forEach { width ->
                assertEquals("$file draws a bar of $width units", GlyphGrid.Bar, width, Tolerance)
            }
            assertTrue(
                "$file cuts the bar's ends flat, where the set rounds them",
                text.contains("""android:strokeLineCap="round""""),
            )
        }
    }

    /** A part of a drawing, in grid units, in the order it is drawn. */
    private data class Part(val points: List<Pair<Float, Float>>)

    private fun drawnParts(icon: Icon): List<Part> = Glyphs.of(icon).mapNotNull { shape ->
        when (shape) {
            is GlyphShape.Solid -> Part(shape.points.map { it.x to it.y })
            is GlyphShape.Bar -> Part(listOf(shape.from, shape.to).map { it.x to it.y })
            else -> null
        }
    }

    private fun partsOf(file: String): List<Part> = PATH_DATA
        .findAll(vector(file).readText())
        .map { match ->
            val numbers = NUMBER.findAll(match.groupValues[1]).map { it.value.toFloat() }.toList()
            Part(numbers.chunked(2).map { it[0] to it[1] })
        }
        .toList()

    private fun vector(file: String): File = Roots
        .map { File(it, "src/main/res/drawable/$file") }
        .firstOrNull { it.isFile }
        ?: error("$file was not found from ${File(".").absolutePath}")

    private companion object {
        const val Tolerance = 0.01f

        /** The working directory is the module's, and the repository root's in some runs. */
        val Roots = listOf(File("."), File("../feature-playback"), File("feature-playback"))

        val PATH_DATA = Regex("""android:pathData="([^"]*)"""")
        val NUMBER = Regex("""-?\d+(?:\.\d+)?""")

    }
}
