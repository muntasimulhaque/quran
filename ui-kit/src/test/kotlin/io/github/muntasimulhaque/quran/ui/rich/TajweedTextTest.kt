package io.github.muntasimulhaque.quran.ui.rich

import androidx.compose.ui.text.buildAnnotatedString
import io.github.muntasimulhaque.quran.ui.theme.PaperTajweed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one place the reading's colors are applied. The switch must remove every
 * span; the span must carry the rule's family color; a code that does not
 * describe the text must paint nothing; and the ornament a caller appends
 * after the ayah keeps its own style.
 */
class TajweedTextTest {

    @Test
    fun `the switch removes every span`() {
        val off = tajweedText("abc", "111", PaperTajweed, enabled = false)
        assertEquals("abc", off.text)
        assertTrue(off.spanStyles.isEmpty())
    }

    @Test
    fun `the code paints the rule's family`() {
        val on = tajweedText("abc", "111", PaperTajweed, enabled = true)
        assertEquals("abc", on.text)
        assertEquals(1, on.spanStyles.size)
        assertEquals(PaperTajweed.silent, on.spanStyles.single().item.color)
        assertEquals(0, on.spanStyles.single().start)
        assertEquals(3, on.spanStyles.single().end)
    }

    @Test
    fun `a code that does not describe the text paints nothing`() {
        val short = tajweedText("abc", "11", PaperTajweed, enabled = true)
        assertEquals("abc", short.text)
        assertTrue(short.spanStyles.isEmpty())
    }

    @Test
    fun `the code stops before an ornament the caller appends`() {
        val line = buildAnnotatedString {
            append("abc")
            tajweed("111", 3, PaperTajweed, enabled = true)
            append(" 12")
        }
        assertEquals("abc 12", line.text)
        assertEquals(3, line.spanStyles.single().end)
    }
}
