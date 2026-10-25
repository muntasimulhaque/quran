package io.github.muntasimulhaque.quran.ui.playback

import io.github.muntasimulhaque.quran.ui.glyph.GlyphGrid
import io.github.muntasimulhaque.quran.ui.glyph.GlyphShape
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
 * unit square, and this test reads the files against the set. The two shapes
 * had already drifted once, and nothing could see it (owner report, this
 * session).
 */
class NotificationGlyphsTest {

    @Test
    fun eachTransportVectorCarriesTheSetsDrawing() {
        transports.forEach { (icon, file) ->
            val parts = partsOf(file)
            val drawn = drawnParts(icon)
            assertEquals("$file carries ${parts.size} parts for $icon", drawn.size, parts.size)
            drawn.forEach { expected ->
                assertTrue(
                    "$file does not carry the $icon drawing: $expected is not among $parts",
                    parts.any { it == expected },
                )
            }
        }
    }

    @Test
    fun aBarInTheShadeIsTheSetsOwnBar() {
        transports.forEach { (icon, file) ->
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

        val transports = listOf(
            Icon.Play to "ic_transport_play.xml",
            Icon.Pause to "ic_transport_pause.xml",
            Icon.Next to "ic_transport_next.xml",
            Icon.Previous to "ic_transport_previous.xml",
        )
    }
}
