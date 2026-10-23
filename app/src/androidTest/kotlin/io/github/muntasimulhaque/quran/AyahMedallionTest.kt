package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The print's own ayah number, on the glass.
 *
 * The reading closes a verse with Arabic-Indic digits in a span of their own,
 * at 0.9 of the ayah's size and in the ornament color. The KFGQPC Hafs face
 * turns that run into a single required-ligature glyph, the print's roundel,
 * so the number and its ring are one shape and the app never draws the ring
 * itself. The ligature is the whole trick, and it is fragile: a bidi mark
 * between the digits, a letter spacing, a per-digit span, or a font swap
 * would leave ordinary digits on the page and no one would know until a
 * reader looked at an Arabic-only reading.
 *
 * The test lays out one word and a space twice, once alone and once with the
 * number in a smaller span at its end, and reads the width the number added.
 * The roundel's advance is 0.779 em of the span's size; plain digits add
 * 0.152 em (1) or 0.579 em (255). The thresholds sit between the two
 * outcomes, so a broken ligature fails rather than merely changing the
 * picture.
 */
class AyahMedallionTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val hafs = FontFamily(
        Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets),
    )

    @Test
    fun oneDigitDrawsThePrintedRoundel() = assertRoundel("\u0661", minimum = 0.55f)

    @Test
    fun threeDigitsDrawThePrintedRoundel() = assertRoundel("\u0662\u0665\u0665", minimum = 0.68f)

    private fun assertRoundel(number: String, minimum: Float) {
        val ornamentSp = 36f
        val style = TextStyle(fontFamily = hafs, fontSize = 40.sp, color = Color.Black)
        val opening = "\u0633\u064E\u0644\u064E\u0670\u0645\u064C "
        var base: TextLayoutResult? = null
        var withNumber: TextLayoutResult? = null
        compose.setContent {
            Column {
                Text(
                    text = opening,
                    style = style,
                    onTextLayout = { base = it },
                )
                Text(
                    text = buildAnnotatedString {
                        append(opening)
                        withStyle(SpanStyle(fontSize = ornamentSp.sp)) { append(number) }
                    },
                    style = style,
                    onTextLayout = { withNumber = it },
                )
            }
        }
        compose.waitForIdle()
        val baseLine = requireNotNull(base) { "the plain line did not lay out" }
        val numberedLine = requireNotNull(withNumber) { "the ornament line did not lay out" }
        val em = with(numberedLine.layoutInput.density) { ornamentSp.sp.toPx() }
        val added = (numberedLine.size.width - baseLine.size.width).toFloat()
        assertTrue(
            "$number added $added px, under the ${em * minimum} px the roundel needs; " +
                "the digits did not take the print's ligature",
            added >= em * minimum,
        )
    }
}
