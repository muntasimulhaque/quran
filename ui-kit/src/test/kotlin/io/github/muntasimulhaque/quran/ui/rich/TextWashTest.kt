package io.github.muntasimulhaque.quran.ui.rich

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The one wash recipe: the box every matched run wears, and the ranges it
 * is asked to wear it on. The arithmetic is pinned here because no compose
 * test can read a shape off the glass, and the wash's shape is exactly such
 * arithmetic.
 */
class TextWashTest {

    @Test
    fun `the wash grows a pad each side and trims half one above and below`() {
        val box = Rect(left = 10f, top = 20f, right = 50f, bottom = 40f)
        val washed = washBox(box)
        // The pad is 8 percent of the ink box's 20 of height: 1.6.
        assertEquals(8.4f, washed.left, 0.0001f)
        assertEquals(20.8f, washed.top, 0.0001f)
        assertEquals(51.6f, washed.right, 0.0001f)
        assertEquals(39.2f, washed.bottom, 0.0001f)
    }

    @Test
    fun `the corner rounds at a share of the ink box's height`() {
        assertEquals(5.2f, washCorner(20f), 0.0001f)
        assertEquals(0.08f, WashPadShare, 0f)
        assertEquals(0.26f, WashCornerShare, 0f)
    }

    @Test
    fun `a range that runs past the end folds back to the last letter`() {
        assertEquals(
            listOf(IntRange(3, 4)),
            foldedRanges("abcde", listOf(IntRange(3, 10))),
        )
    }

    @Test
    fun `a range that reaches back into the one before it is trimmed`() {
        assertEquals(
            listOf(IntRange(0, 2), IntRange(3, 4)),
            foldedRanges("abcde", listOf(IntRange(0, 2), IntRange(1, 4))),
        )
    }

    @Test
    fun `a range outside the text washes nothing`() {
        assertEquals(emptyList<IntRange>(), foldedRanges("abc", listOf(IntRange(5, 9))))
        assertEquals(emptyList<IntRange>(), foldedRanges("abc", emptyList()))
    }
}
