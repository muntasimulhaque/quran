package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * The card comparison separates a platform's rasterizer from a change of the
 * card.
 *
 * The calibration that sets [CardDiff.TOLERANCE] is recorded on that object:
 * an antialiasing and fractional-metrics change moves the quarter-scale mean
 * by 0.55, one changed word by 3.30, and the other headline face by 2.14.
 * These tests hold the two sides of that line.
 */
class CardDiffTest {

    @Test
    fun `a rasterizer's edges are not a change`() {
        assertTrue(CardDiff.sameEnough(aCard(), shifted(aCard(), 1)))
    }

    @Test
    fun `a changed word is a change`() {
        val changed = aCard()
        val graphics = changed.createGraphics()
        try {
            graphics.color = Color(0xFB, 0xF9, 0xF4)
            graphics.fillRect(96, 150, 460, 110)
        } finally {
            graphics.dispose()
        }
        assertFalse(CardDiff.sameEnough(aCard(), changed))
    }

    @Test
    fun `a card of another size is a change`() {
        val other = BufferedImage(540, 960, BufferedImage.TYPE_INT_RGB)
        assertFalse(CardDiff.sameEnough(aCard(), other))
    }

    /** The phone card's canvas, its paper, and one band of ink for a word. */
    private fun aCard(): BufferedImage {
        val image = BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color(0xFB, 0xF9, 0xF4)
            graphics.fillRect(0, 0, 1080, 1920)
            graphics.color = Color(0x29, 0x26, 0x22)
            graphics.fillRect(96, 150, 700, 110)
        } finally {
            graphics.dispose()
        }
        return image
    }

    /** Every channel one step apart: a stand-in for a rasterizer's edges. */
    private fun shifted(image: BufferedImage, by: Int): BufferedImage {
        val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val p = image.getRGB(x, y)
                val r = (((p shr 16) and 0xFF) + by).coerceIn(0, 255)
                val g = (((p shr 8) and 0xFF) + by).coerceIn(0, 255)
                val b = ((p and 0xFF) + by).coerceIn(0, 255)
                out.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
        }
        return out
    }
}
