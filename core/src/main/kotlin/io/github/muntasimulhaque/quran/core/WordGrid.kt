package io.github.muntasimulhaque.quran.core

/**
 * The width of one tile in the word-by-word grid, as a pure rule.
 *
 * Word by word is a grid, not a heap: every tile is the same width, so the
 * words stand in columns, each word sits over the meaning that is its own,
 * and the meanings of a row share one baseline. A flow of tiles each as wide
 * as its own meaning reads as a heap instead, and the reader hunts for the
 * pair.
 *
 * The rule has two floors and one ceiling, and the order matters:
 *
 * 1. a tile is never narrower than the widest word in it, because a word that
 *    wrapped would stop being a word;
 * 2. a tile is never wider than half the measure, so one long gloss cannot
 *    widen every tile and put the whole verse down the page as a single
 *    column, which is the heap this grid exists to close;
 * 3. between those, a tile is as wide as the widest thing it holds, so a
 *    short ayah's meanings are not padded out to a tablet's measure.
 *
 * The widths arrive measured, in pixels, from the text measurer at the
 * reader's own sizes; the rule itself is arithmetic and lives here so the
 * suite can hold it.
 */
object WordGrid {

    /** The tile's width in pixels. */
    fun tileWidth(availablePx: Int, wordWidthPx: Int, meaningWidthPx: Int, padPx: Int): Int {
        val floor = wordWidthPx + padPx
        val half = (availablePx / 2).coerceAtLeast(floor)
        val ideal = maxOf(wordWidthPx, meaningWidthPx) + padPx
        return ideal.coerceIn(floor, minOf(half, availablePx))
    }

    /** How many tiles of that width the measure holds, and never fewer than one. */
    fun columns(availablePx: Int, tilePx: Int): Int =
        if (tilePx <= 0) 1 else maxOf(1, availablePx / tilePx)
}
