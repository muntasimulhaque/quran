package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The reading's colors have to survive the common forms of color vision
 * deficiency, not only normal vision.
 *
 * [TajweedPaletteTest] proves that two families are far enough apart for an
 * eye with all three cone types. The earlier palette passed that test and
 * still collapsed under deuteranopia and tritanopia: ghunnah and qalaqah
 * measured 0.003, and iqlab and idgham measured 0.012 on paper. The owner
 * does not want a second, non-color channel, so the palette itself has to
 * keep the pairs apart.
 *
 * The simulation is the standard linear-RGB to LMS projection for protanopia,
 * deuteranopia, and tritanopia, followed by the same OKLab distance the
 * normal palette test uses. The floor is 0.02, a tenth of the normal floor:
 * it is not a promise that every reader sees seven distinct hues, but it is a
 * real, measurable separation where the old palette had none (owner decision,
 * this session).
 */
class TajweedColorVisionTest {

    @Test
    fun paperFamiliesSeparateUnderEveryDeficiency() = assertSeparated(PaperTajweed)

    @Test
    fun nightFamiliesSeparateUnderEveryDeficiency() = assertSeparated(NightTajweed)

    private fun assertSeparated(colors: TajweedColors) {
        val entries = listOf(
            "silent" to colors.silent,
            "ghunnah" to colors.ghunnah,
            "qalaqah" to colors.qalaqah,
            "ikhafa" to colors.ikhafa,
            "iqlab" to colors.iqlab,
            "idgham" to colors.idgham,
            "madda" to colors.madda,
        )
        for (deficiency in Deficiency.entries) {
            for (index in entries.indices) {
                for (other in index + 1 until entries.size) {
                    val delta = distance(
                        simulate(entries[index].second, deficiency),
                        simulate(entries[other].second, deficiency),
                    )
                    assertTrue(
                        "${entries[index].first} and ${entries[other].first} are " +
                            "${"%.3f".format(delta)} apart under ${deficiency.name.lowercase()}, " +
                            "under the $PAIR_FLOOR floor",
                        delta >= PAIR_FLOOR,
                    )
                }
            }
        }
    }

    private fun simulate(color: Color, deficiency: Deficiency): Color {
        val linear = doubleArrayOf(linear(color.red), linear(color.green), linear(color.blue))
        val lms = doubleArrayOf(
            17.8824 * linear[0] + 43.5161 * linear[1] + 4.11935 * linear[2],
            3.45565 * linear[0] + 27.1554 * linear[1] + 3.86714 * linear[2],
            0.0299566 * linear[0] + 0.184309 * linear[1] + 1.46709 * linear[2],
        )
        val matrix = deficiency.matrix
        val projected = doubleArrayOf(
            matrix[0][0] * lms[0] + matrix[0][1] * lms[1] + matrix[0][2] * lms[2],
            matrix[1][0] * lms[0] + matrix[1][1] * lms[1] + matrix[1][2] * lms[2],
            matrix[2][0] * lms[0] + matrix[2][1] * lms[1] + matrix[2][2] * lms[2],
        )
        val rgb = doubleArrayOf(
            0.0809444479 * projected[0] - 0.130504409 * projected[1] + 0.116721066 * projected[2],
            -0.0102485335 * projected[0] + 0.0540193266 * projected[1] - 0.113614708 * projected[2],
            -0.000365296938 * projected[0] - 0.00412161469 * projected[1] +
                0.693511405 * projected[2],
        ).map { delinear(it).coerceIn(0.0, 1.0) }
        return Color(rgb[0].toFloat(), rgb[1].toFloat(), rgb[2].toFloat())
    }

    private fun distance(first: Color, second: Color): Double {
        val a = oklab(first)
        val b = oklab(second)
        return sqrt(
            (a[0] - b[0]).pow(2) + (a[1] - b[1]).pow(2) + (a[2] - b[2]).pow(2),
        )
    }

    private fun oklab(color: Color): DoubleArray {
        val red = linear(color.red)
        val green = linear(color.green)
        val blue = linear(color.blue)
        val l = cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue)
        val m = cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue)
        val s = cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue)
        return doubleArrayOf(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    private fun linear(channel: Float): Double =
        if (channel <= 0.04045f) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

    private fun delinear(channel: Double): Double =
        if (channel <= 0.0031308) 12.92 * channel else 1.055 * channel.pow(1 / 2.4) - 0.055

    private enum class Deficiency(val matrix: Array<DoubleArray>) {
        Protan(
            arrayOf(
                doubleArrayOf(0.0, 1.05118294, -0.05116099),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0),
            ),
        ),
        Deutan(
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.9513092, 0.0, 0.04866992),
                doubleArrayOf(0.0, 0.0, 1.0),
            ),
        ),
        Tritan(
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(-0.86744736, 1.86727089, 0.0),
            ),
        ),
    }

    private companion object {
        /** The smallest OKLab distance two families may sit at under a deficiency. */
        const val PAIR_FLOOR = 0.02
    }
}
