package io.github.muntasimulhaque.quran.ui.rich

import io.github.muntasimulhaque.quran.ui.theme.PaperTajweed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The color runs the reading paints over its one shaped line.
 *
 * The code is the QUL annotation, one character per Quran character. The
 * switch removes every run; a run carries its rule family's color; and a code
 * that asks for nothing asks for nothing, so the line keeps its ink.
 */
class TajweedTextTest {

    @Test
    fun `the switch removes every run`() {
        val off = tajweedColorRuns("99999", PaperTajweed, enabled = false)
        assertTrue(off.isEmpty())
    }

    @Test
    fun `an empty code asks for nothing`() {
        assertTrue(tajweedColorRuns("", PaperTajweed, enabled = true).isEmpty())
    }

    @Test
    fun `a run carries its rule family's color`() {
        val runs = tajweedColorRuns("90000", PaperTajweed, enabled = true)
        assertEquals(1, runs.size)
        assertEquals(0, runs.single().start)
        assertEquals(1, runs.single().end)
        assertEquals(PaperTajweed.idgham, runs.single().color)
    }

    @Test
    fun `two rules of one ayah keep their own ranges`() {
        val runs = tajweedColorRuns("0066000gg", PaperTajweed, enabled = true)
        assertEquals(2, runs.size)
        assertEquals(2, runs[0].start)
        assertEquals(4, runs[0].end)
        assertEquals(PaperTajweed.ikhafa, runs[0].color)
        assertEquals(7, runs[1].start)
        assertEquals(9, runs[1].end)
        assertEquals(PaperTajweed.madda, runs[1].color)
    }

    @Test
    fun `a run may be one mark inside a shaped word`() {
        // The fathah and alef of "لَا" carry the madda rule; the lam before
        // them is not in the run. The painter has to find the ligature's box
        // for a run this narrow.
        val runs = tajweedColorRuns("0gg", PaperTajweed, enabled = true)
        assertEquals(1, runs.size)
        assertEquals(1, runs.single().start)
        assertEquals(3, runs.single().end)
        assertEquals(PaperTajweed.madda, runs.single().color)
    }
}
