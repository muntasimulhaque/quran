package io.github.muntasimulhaque.quran.tools

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Whether two cards are the same card.
 *
 * The committed cards are made on the owner's machine and `--check` also runs
 * on a Linux CI runner, and Java2D rasterizes the same font differently on
 * each: the glyph outlines are the fonts', but the edges and the hinting are
 * the platform's. Comparing the PNG bytes therefore fails on a card that is
 * correct, which is a gate that lies. So the comparison decodes both pictures,
 * shrinks them to a quarter, and asks for the mean difference of the three
 * channels.
 *
 * The floor is measured, not guessed. Rendering the same card with its text
 * context at antialiasing and fractional metrics off, which is a heavier
 * change than any two platforms make, moves the quarter-scale mean by 0.55 out
 * of 255. Changing one word of a headline moves it 3.30, and setting the
 * headline in the other face moves it 2.14. A ceiling of 1.5 sits between the
 * two by a wide margin on both sides, so a real change of the type, the
 * colors, the rules, or the capture is named, and a rasterizer's own edges
 * are not (owner decision, 22.4).
 */
object CardDiff {

    /** The ceiling at quarter scale: above any rasterizer, under any change. */
    const val TOLERANCE = 1.5

    fun sameEnough(a: BufferedImage, b: BufferedImage): Boolean {
        if (a.width != b.width || a.height != b.height) return false
        return meanDifference(shrink(a, 4), shrink(b, 4)) <= TOLERANCE
    }

    private fun shrink(image: BufferedImage, by: Int): BufferedImage {
        val width = (image.width / by).coerceAtLeast(1)
        val height = (image.height / by).coerceAtLeast(1)
        val out = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = out.createGraphics()
        try {
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR,
            )
            graphics.drawImage(image, 0, 0, width, height, null)
        } finally {
            graphics.dispose()
        }
        return out
    }

    private fun meanDifference(a: BufferedImage, b: BufferedImage): Double {
        var sum = 0L
        var count = 0L
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val p = a.getRGB(x, y)
                val q = b.getRGB(x, y)
                sum += abs(((p shr 16) and 0xFF) - ((q shr 16) and 0xFF)).toLong()
                sum += abs(((p shr 8) and 0xFF) - ((q shr 8) and 0xFF)).toLong()
                sum += abs((p and 0xFF) - (q and 0xFF)).toLong()
                count += 3
            }
        }
        return sum.toDouble() / count
    }
}
