package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The palette's second rule, enforced by arithmetic: the seven families are
 * far enough apart to be told apart, and each one is far enough from the ink
 * to read as a marked letter.
 *
 * The contrast test proves that a family can be seen on its ground. It cannot
 * prove that two families can be told apart from each other. The first
 * palette passed contrast on every ground and still read as one muddy tone,
 * because four families sat within 0.1 of each other in OKLab and all seven
 * lived in a 0.09 lightness band (owner report, the palette session).
 *
 * A pair at 0.085 is the floor this app accepts. The paper palette's closest
 * pair is the quiet gray and the teal at 0.089; the night palette's closest
 * is 0.111. Every family is at least 0.10 from the ink, which on the night
 * page is the lightest tone and therefore the closest one to the colors by
 * construction.
 *
 * OKLab is used rather than a hue difference because it is the space where a
 * unit of distance is roughly a just-noticeable difference, so the numbers
 * mean something a reader's eye would agree with. The test runs on the JVM,
 * where the theme's own Color values are available.
 */
class TajweedPaletteTest {

    @Test
    fun paperFamiliesSeparate() = assertSeparated(PaperTajweed, PaperInk)

    @Test
    fun nightFamiliesSeparate() = assertSeparated(NightTajweed, NightInk)

    private fun assertSeparated(colors: TajweedColors, ink: Color) {
        val entries = listOf(
            "silent" to colors.silent,
            "ghunnah" to colors.ghunnah,
            "qalaqah" to colors.qalaqah,
            "ikhafa" to colors.ikhafa,
            "iqlab" to colors.iqlab,
            "idgham" to colors.idgham,
            "madda" to colors.madda,
        )
        for (index in entries.indices) {
            for (other in index + 1 until entries.size) {
                val delta = distance(entries[index].second, entries[other].second)
                assertTrue(
                    "${entries[index].first} and ${entries[other].first} are " +
                        "${"%.3f".format(delta)} apart, under the 0.085 floor",
                    delta >= PAIR_FLOOR,
                )
            }
        }
        for ((name, color) in entries) {
            val delta = distance(color, ink)
            assertTrue(
                "$name is ${"%.3f".format(delta)} from the ink, under the 0.10 floor",
                delta >= INK_FLOOR,
            )
        }
    }

    private fun distance(first: Color, second: Color): Double {
        val a = oklab(first)
        val b = oklab(second)
        return sqrt(
            (a[0] - b[0]).pow(2) + (a[1] - b[1]).pow(2) + (a[2] - b[2]).pow(2),
        )
    }

    private fun oklab(color: Color): DoubleArray {
        val red = linear(color.red.toDouble())
        val green = linear(color.green.toDouble())
        val blue = linear(color.blue.toDouble())
        val l = cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue)
        val m = cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue)
        val s = cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue)
        return doubleArrayOf(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    private fun linear(channel: Double): Double =
        if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

    private companion object {
        /** The smallest OKLab distance two families may sit at. */
        const val PAIR_FLOOR = 0.085

        /** The smallest distance a family may sit from the reading's own ink. */
        const val INK_FLOOR = 0.10
    }
}
