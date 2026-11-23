package io.github.muntasimulhaque.quran.tools

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * The compositor at its edges: the store's exact sizes, a frame that is
 * absent, a frame of the wrong size, and a committed set that has fallen
 * behind the source.
 *
 * The frames here are solid rectangles, not the app: this class proves the
 * command's contract, and `StoreCardSheetTest` proves the drawing. The real
 * captures are the release's own, and `storecards --check` reads them.
 */
class StoreCardsTest {

    private val root = Files.createTempDirectory("store-cards").toFile()

    @Before
    fun prepareTheStore() {
        val fonts = File(root, "content-assets/src/main/res/font")
        fonts.mkdirs()
        for (name in TYPEFACES) {
            File(repositoryRoot(), "content-assets/src/main/res/font/$name")
                .copyTo(File(fonts, name), overwrite = true)
        }
        for (layout in StoreCardSheet.layouts) {
            val folder = File(root, "play-store/frames/${layout.form.folder}")
            for (card in StoreCardSheet.cards) {
                write(folder, card.frame, layout.form.width, layout.form.height)
            }
        }
    }

    @After
    fun removeTheStore() {
        root.deleteRecursively()
    }

    @Test
    fun `the cards render at the stores exact sizes`() {
        assertEquals(0, run(checkOnly = false).first)
        for (layout in StoreCardSheet.layouts) {
            for (card in StoreCardSheet.cards) {
                val file = File(root, "play-store/screenshots/${layout.form.folder}/${card.frame}.png")
                assertTrue("${file.path} is missing", file.isFile)
                val image = ImageIO.read(file)
                assertEquals(layout.form.width, image.width)
                assertEquals(layout.form.height, image.height)
                assertFalse("${file.path} carries alpha", image.colorModel.hasAlpha())
                assertEquals(
                    "${file.path} does not stand on its face's ground",
                    StoreCardSheet.faceOf(card).ground and 0xFFFFFF,
                    image.getRGB(0, 0) and 0xFFFFFF,
                )
            }
        }
    }

    @Test
    fun `a missing frame fails by name`() {
        assertTrue(File(root, "play-store/frames/phone/03-search.png").delete())
        val (exit, output) = run(checkOnly = false)
        assertEquals(1, exit)
        assertTrue(output, output.contains("play-store/frames/phone"))
        assertTrue(output, output.contains("03-search.png"))
    }

    @Test
    fun `a wrong sized frame is refused`() {
        write(File(root, "play-store/frames/tablet7"), "04-saved", 999, 999)
        val (exit, output) = run(checkOnly = false)
        assertEquals(1, exit)
        assertTrue(output, output.contains("999x999"))
        assertTrue(output, output.contains("expected 1200x1920"))
    }

    @Test
    fun `check names a stale card`() {
        assertEquals(0, run(checkOnly = false).first)
        assertEquals(0, run(checkOnly = true).first)
        val card = File(root, "play-store/screenshots/tablet10/07-share-card.png")
        assertTrue(card.delete())
        val (exit, output) = run(checkOnly = true)
        assertEquals(1, exit)
        assertTrue(
            output,
            output.contains("play-store/screenshots/tablet10/07-share-card.png"),
        )
    }

    @Test
    fun `check names an edited card`() {
        assertEquals(0, run(checkOnly = false).first)
        val card = File(root, "play-store/screenshots/phone/01-reading.png")
        val image = ImageIO.read(card)
        val graphics = image.createGraphics()
        try {
            graphics.color = java.awt.Color(0x29, 0x26, 0x22)
            graphics.fillRect(96, 150, 460, 110)
        } finally {
            graphics.dispose()
        }
        ImageIO.write(image, "png", card)
        val (exit, output) = run(checkOnly = true)
        assertEquals(1, exit)
        assertTrue(output, output.contains("play-store/screenshots/phone/01-reading.png"))
    }

    private fun run(checkOnly: Boolean): Pair<Int, String> {
        val buffer = ByteArrayOutputStream()
        val original = System.out
        System.setOut(PrintStream(buffer, true))
        return try {
            StoreCards(root).run(checkOnly) to buffer.toString()
        } finally {
            System.setOut(original)
        }
    }

    private fun write(folder: File, name: String, width: Int, height: Int) {
        folder.mkdirs()
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color(0x33, 0x66, 0x99)
            graphics.fillRect(0, 0, width, height)
        } finally {
            graphics.dispose()
        }
        ImageIO.write(image, "png", File(folder, "$name.png"))
    }

    private companion object {
        val TYPEFACES = listOf("literata_variable.ttf", "inter_variable.ttf")
    }
}
