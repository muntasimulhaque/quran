package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.PaperTajweed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.min

/**
 * The one guarantee the QPC rendering audit rests on: the Tajweed colors are
 * painted over the shaped line, never shaped into it.
 *
 * A color span makes Android's text engine shape the run again. The test
 * renders the same word twice, once with the colors and once without, and
 * requires the two layouts to be identical down to every character's box. It
 * then requires the ink masks to match, which is what proves a fathah is still
 * on top of its letter and a lam-alef is still one ligature (owner decision,
 * the QPC rendering audit).
 */
class TajweedRenderingTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val hafs = FontFamily(
        Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets),
    )

    @Test
    fun walaKeepsItsWawFathahAboveTheWaw() = assertParity("وَلَا", "90000")

    @Test
    fun lahuKeepsItsLamShaddaAndFathahOnTheLam() = assertParity("لَّهُۥ", "a0000e")

    @Test
    fun indahuKeepsItsDalFathahAboveTheDal() = assertParity("عِندَهُۥ", "0066000gg")

    @Test
    fun theMaddaOfLaKeepsTheLamAlefLigature() = assertParity("لَآ", "0gg")

    @Test
    fun theBasmalahShapesTheKashidaAndTheTwoMaddas() =
        assertParity(
            "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
            "000000010000000120000000ee000120000ff00",
        )

    private fun assertParity(text: String, code: String) {
        var onLayout: TextLayoutResult? = null
        var offLayout: TextLayoutResult? = null
        val style = TextStyle(
            fontFamily = hafs,
            fontSize = 36.sp,
            lineHeight = 68.sp,
            color = androidx.compose.ui.graphics.Color.Black,
        )
        compose.setContent {
            Column(Modifier.width(360.dp)) {
                TajweedText(
                    text = AnnotatedString(text),
                    style = style,
                    modifier = Modifier.testTag("on"),
                    code = code,
                    colors = PaperTajweed,
                    enabled = true,
                    onTextLayout = { onLayout = it },
                )
                TajweedText(
                    text = AnnotatedString(text),
                    style = style,
                    modifier = Modifier.testTag("off"),
                    code = code,
                    colors = PaperTajweed,
                    enabled = false,
                    onTextLayout = { offLayout = it },
                )
            }
        }
        compose.waitForIdle()
        val on = requireNotNull(onLayout) { "the colored line did not lay out" }
        val off = requireNotNull(offLayout) { "the plain line did not lay out" }
        assertSameLayout(on, off, text)
        assertSameInk(
            compose.onNodeWithTag("on").captureToImage(),
            compose.onNodeWithTag("off").captureToImage(),
            text,
        )
    }

    private fun assertSameLayout(on: TextLayoutResult, off: TextLayoutResult, label: String) {
        assertEquals(label, on.size, off.size)
        assertEquals(label, on.lineCount, off.lineCount)
        val length = min(on.layoutInput.text.length, off.layoutInput.text.length)
        for (offset in 0 until length) {
            assertEquals(label, on.getBoundingBox(offset), off.getBoundingBox(offset))
            assertEquals(
                label,
                on.getHorizontalPosition(offset, usePrimaryDirection = true),
                off.getHorizontalPosition(offset, usePrimaryDirection = true),
                0.01f,
            )
        }
    }

    private fun assertSameInk(on: ImageBitmap, off: ImageBitmap, label: String) {
        assertEquals(label, on.width, off.width)
        assertEquals(label, on.height, off.height)
        val onPixels = on.toPixelMap()
        val offPixels = off.toPixelMap()
        var different = 0
        for (y in 0 until on.height) {
            for (x in 0 until on.width) {
                val darkOn = luminance(onPixels[x, y]) < 0.82f
                val darkOff = luminance(offPixels[x, y]) < 0.82f
                if (darkOn != darkOff) different++
            }
        }
        val pixels = on.width * on.height
        assertTrue("$label ink differs at $different of $pixels pixels", different <= pixels / 100)
    }

    private fun luminance(pixel: androidx.compose.ui.graphics.Color): Float =
        0.299f * pixel.red + 0.587f * pixel.green + 0.114f * pixel.blue
}
