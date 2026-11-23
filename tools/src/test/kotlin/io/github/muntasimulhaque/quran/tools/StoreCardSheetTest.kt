package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Font
import java.io.File

/**
 * The cards' words, palette, and geometry, held to the app and to the listing.
 *
 * The compositor can draw a card that is beautiful and says something the
 * store does not, or wears a color the theme does not, or asks a caption to
 * wrap further than its band allows. These are the cases a caption change has
 * to pass before it reaches `play-store/screenshots/`.
 */
class StoreCardSheetTest {

    @Test
    fun `every card copy is a real sentence`() {
        assertEquals(8, StoreCardSheet.cards.size)
        assertEquals(
            "no two cards share a headline",
            StoreCardSheet.cards.size,
            StoreCardSheet.cards.map { it.headline }.toSet().size,
        )
        for (card in StoreCardSheet.cards) {
            val headline = card.headline
            assertTrue("${card.frame} is blank", headline.isNotBlank())
            assertTrue("$headline is over eight words", headline.split(" ").size <= 8)
            assertTrue("$headline does not end a sentence", headline.trim().endsWith("."))
            assertFalse("$headline carries an em dash", headline.contains('\u2014'))
            assertFalse(
                "$headline carries a version",
                Regex("""\d+\.\d+""").containsMatchIn(headline),
            )
        }
        val promise = StoreCardSheet.cards.first().sub
        assertTrue("the first card carries the promise", promise != null)
        assertTrue("the promise is over six words", promise.orEmpty().split(" ").size <= 6)
        assertFalse("the promise carries an em dash", promise.orEmpty().contains('\u2014'))
        assertTrue(
            "only the first card carries a sub-line",
            StoreCardSheet.cards.drop(1).all { it.sub == null },
        )
    }

    @Test
    fun `the card copy matches the listing`() {
        val listing = File(repositoryRoot(), "play-store/listing.md").readText()
        for (card in StoreCardSheet.cards) {
            assertTrue(
                "the listing does not say \"${card.headline}\"",
                listing.contains(card.headline),
            )
            card.sub?.let { sub ->
                assertTrue("the listing does not say \"$sub\"", listing.contains(sub))
            }
        }
    }

    @Test
    fun `the card palette lives in the theme`() {
        val theme = File(
            repositoryRoot(),
            "ui-kit/src/main/kotlin/io/github/muntasimulhaque/quran/ui/theme/Color.kt",
        ).readText()
        for (face in StoreCardSheet.faces) {
            for (color in face.colors) {
                val literal = String.format("Color(0x%08X)", color.toLong() and 0xFFFFFFFFL)
                assertTrue(
                    "${face.name} wears $literal and the theme does not",
                    theme.contains(literal),
                )
            }
        }
    }

    @Test
    fun `the card geometry fits every canvas`() {
        val type = StoreCardPaint.typefaces(repositoryRoot())
        for (layout in StoreCardSheet.layouts) {
            val display = type.display.deriveFont(Font.PLAIN, layout.displaySize.toFloat())
            val label = type.label.deriveFont(Font.PLAIN, layout.labelSize.toFloat())
            for (card in StoreCardSheet.cards) {
                val lines = StoreCardPaint.wrap(card.headline, display, layout.textWidth.toDouble())
                assertTrue("${card.frame} is empty on ${layout.form}", lines.isNotEmpty())
                assertTrue(
                    "${card.headline} needs ${lines.size} lines on ${layout.form}",
                    lines.size <= layout.displayLines,
                )
                for (line in lines) {
                    assertTrue(
                        "\"$line\" is wider than ${layout.textWidth}",
                        line.width(display) <= layout.textWidth + 1,
                    )
                }
                card.sub?.let { sub ->
                    val subLines = StoreCardPaint.wrap(sub, label, layout.textWidth.toDouble())
                    assertEquals("the sub-line is one line on ${layout.form}", 1, subLines.size)
                }
            }
            assertTrue(
                "the headline band touches the sub-line on ${layout.form}",
                layout.displayTop + layout.displayHeight <= layout.labelTop,
            )
            assertTrue(
                "the sub-line touches the rule on ${layout.form}",
                layout.labelTop + layout.labelHeight <= layout.ruleY,
            )
            assertTrue(
                "the rule touches the frame on ${layout.form}",
                layout.ruleY + layout.ruleHeight <= layout.frame.y - layout.pad - layout.stroke,
            )
            val box = StoreCardPaint.fitted(layout.frame, layout.form.width, layout.form.height)
            assertTrue("the frame is outside its slot on ${layout.form}", layout.frame.contains(box))
            val outline = StoreCardPaint.outlineBounds(box, layout)
            assertTrue(
                "the outline leaves the canvas on ${layout.form}: $outline",
                outline.x >= 0 && outline.y >= 0 &&
                    outline.maxX <= layout.form.width && outline.maxY <= layout.form.height,
            )
        }
    }

    private fun String.width(font: Font): Float =
        java.awt.font.TextLayout(this, font, StoreCardPaint.context).advance
}

/**
 * The repository the tests read from: the nearest ancestor of the working
 * directory that carries `settings.gradle.kts`. A test task runs from its
 * module's directory, so the answer is never the working directory itself.
 */
internal fun repositoryRoot(): File {
    var directory: File? = File(System.getProperty("user.dir")).canonicalFile
    while (directory != null) {
        if (File(directory, "settings.gradle.kts").isFile) return directory
        directory = directory.parentFile
    }
    throw IllegalStateException(
        "no settings.gradle.kts above ${System.getProperty("user.dir")}",
    )
}
