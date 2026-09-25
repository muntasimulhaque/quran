package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.ScriptMix
import io.github.muntasimulhaque.quran.core.scriptMix
import io.github.muntasimulhaque.quran.ui.rich.RichBlocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An Arabic paragraph in a tafsir wraps from the right.
 *
 * The owner reported that an Arabic passage came back with every line after
 * the first starting at the left edge. The cause was Compose's default text
 * direction: with no direction on the style, `TextDirection.Unspecified`
 * resolves to the interface's own LTR, so an all-Arabic paragraph was broken
 * left to right and only its first, full line looked right. The rich text
 * views now state `TextDirection.Content`, which takes the block's own first
 * strong character, and this test reads the drawn pixels back: the last line
 * of the paragraph must end at the paragraph's right edge.
 *
 * The block is measured through the real `RichBlocks` composable and its real
 * fonts, so the claim is about the surface the reader reads, not a mirror of
 * it. Reading pixels is the only way to observe a Compose text layout from
 * the outside; the emulator draws the same anti-aliased glyphs the device
 * does.
 */
@RunWith(AndroidJUnit4::class)
class TafsirDirectionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun anArabicParagraphWrapsFromTheRight() {
        val blocks = RichText.parseHtml("<p>$ARABIC</p>")
        assertEquals("the passage must be one Arabic paragraph", ScriptMix.ARABIC, scriptMix(blocks.single().runs))

        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Box(Modifier.width(320.dp).testTag(PROBE)) {
                    RichBlocks(blocks = blocks, sizeSp = 16f, lineSp = 26f, arabicSp = 22f)
                }
            }
        }
        compose.waitForIdle()

        val bitmap = compose.onNodeWithTag(PROBE).captureToImage().asAndroidBitmap()
        val lines = inkBands(bitmap)
        assertTrue("the passage must wrap onto more than one line", lines.size >= 2)
        val lastLine = lines.last()
        assertTrue(
            "the last line ($lastLine of ${bitmap.width}) must reach the right edge",
            lastLine.last >= bitmap.width - EDGE_SLACK_PX,
        )
        assertTrue(
            "the last line ($lastLine of ${bitmap.width}) must start from the right half",
            lastLine.first >= bitmap.width / 3,
        )
    }

    /**
     * The x range of every vertical band of dark pixels, top to bottom. A
     * text line can split into two bands (the diacritics and the letter
     * bodies) when no row carries both; the last band is the last line's
     * glyphs, which is the one that says where the paragraph's ragged edge
     * falls.
     */
    private fun inkBands(bitmap: android.graphics.Bitmap): List<IntRange> {
        val bands = ArrayList<IntRange>()
        var start = -1
        var min = 0
        var max = 0
        for (y in 0 until bitmap.height) {
            var left = -1
            var right = -1
            for (x in 0 until bitmap.width) {
                if (isInk(bitmap.getPixel(x, y))) {
                    if (left < 0) left = x
                    right = x
                }
            }
            if (left < 0) {
                if (start >= 0) bands += min..max
                start = -1
            } else {
                if (start < 0) {
                    start = y
                    min = left
                    max = right
                } else {
                    min = minOf(min, left)
                    max = maxOf(max, right)
                }
            }
        }
        if (start >= 0) bands += min..max
        return bands
    }

    private fun isInk(color: Int): Boolean =
        ((color shr 16) and 0xFF) < 160 &&
            ((color shr 8) and 0xFF) < 160 &&
            (color and 0xFF) < 160

    private companion object {
        const val PROBE = "tafsir-direction-probe"

        /** Roughly 6 dp of glyph bearing at the emulator's own density. */
        const val EDGE_SLACK_PX = 16

        const val ARABIC = "إِنَّ اللهَ لا يَنَامُ، وَلَا يَنْبَغِي لَهُ أَنْ يَنَامَ، يَخْفِضُ الْقِسْطَ " +
            "وَيَرْفَعُهُ، يُرْفَعُ إِلَيْهِ عَمَلُ النَّهَارِ قَبْلَ عَمَلِ اللَّيْلِ، وَعَمَلُ اللَّيْلِ " +
            "قَبْلَ عَمَلِ النَّهَارِ، حِجَابُهُ النُّورُ - أَوِ النَّارُ - لَوْ كَشَفَهُ لَأَحْرَقَتْ " +
            "سُبُحَاتُ وَجْهِهِ مَا انْتَهَى إِلَيْهِ بَصَرُهُ مِنْ خَلْقِهِ"
    }
}
