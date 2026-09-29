package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The word-by-word grid's width rule, held.
 *
 * The rule has one job: a grid, and never a heap. Every case below is a way
 * the aid used to go wrong, with the arithmetic that keeps it right.
 */
class WordGridTest {

    @Test
    fun `a tile is as wide as the widest thing it holds`() {
        // 1000 px of measure, a 200 px word, a 260 px meaning
        val tile = WordGrid.tileWidth(availablePx = 1000, wordWidthPx = 200, meaningWidthPx = 260, padPx = 24)
        assertEquals(284, tile)
    }

    @Test
    fun `a tile is never narrower than the widest word`() {
        // a very long word with a short meaning: the word decides, so it never
        // wraps
        val tile = WordGrid.tileWidth(availablePx = 1000, wordWidthPx = 420, meaningWidthPx = 80, padPx = 24)
        assertTrue("the word must fit", tile >= 420)
    }

    @Test
    fun `one long gloss cannot widen the grid to a single column`() {
        // a 900 px gloss in a 1000 px measure would make every tile 924 wide,
        // and every row one word: the heap the grid exists to close
        val tile = WordGrid.tileWidth(availablePx = 1000, wordWidthPx = 120, meaningWidthPx = 900, padPx = 24)
        assertTrue("the tile must leave room for two", tile <= 1000 / 2)
        assertEquals(2, WordGrid.columns(1000, tile))
    }

    @Test
    fun `a narrow measure still gives a tile that fits its word`() {
        // a 300 px measure, a 260 px word: the word wins over the half-measure
        val tile = WordGrid.tileWidth(availablePx = 300, wordWidthPx = 260, meaningWidthPx = 40, padPx = 24)
        assertTrue("the word must still fit", tile >= 260)
    }

    @Test
    fun `a tile never exceeds the measure`() {
        val tile = WordGrid.tileWidth(availablePx = 500, wordWidthPx = 200, meaningWidthPx = 2000, padPx = 24)
        assertTrue(tile <= 500)
    }

    @Test
    fun `columns is at least one`() {
        assertEquals(1, WordGrid.columns(availablePx = 300, tilePx = 900))
        assertEquals(1, WordGrid.columns(availablePx = 300, tilePx = 0))
    }

    @Test
    fun `a phone shows fewer tiles than a tablet at the same widths`() {
        // the tile is decided by the ayah, so it is the same on both; the
        // measure is what differs, and the number of columns follows it
        val widths = WordGrid.tileWidth(availablePx = 1000, wordWidthPx = 120, meaningWidthPx = 140, padPx = 24)
        val phone = WordGrid.columns(availablePx = 360, tilePx = widths)
        val tablet = WordGrid.columns(availablePx = 1200, tilePx = widths)
        assertTrue("a phone shows at least one", phone >= 1)
        assertTrue("a tablet shows more of the verse than a phone", tablet > phone)
    }
}
