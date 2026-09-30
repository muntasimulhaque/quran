package io.github.muntasimulhaque.quran.core

/**
 * How a settings row with a name and a value divides the width it has.
 *
 * The name is the half a reader reads first, so it holds its room and no more
 * than its share. The name's column is its own text and a gap, capped at the
 * share that keeps the value a column of its own:
 *
 * 1. a name that fits in its share takes exactly its own width and the gap,
 *    and the value takes everything else, which is the case almost every row
 *    is;
 * 2. a name longer than its share takes the share and no more, so a long
 *    name costs the name a second line and never costs the value its column.
 *
 * That is the whole rule, and it needs no measurement of the value: the value
 * is given whatever the name did not take. Before this the row split on fixed
 * weights, so the value got 45 percent of the row whatever the name needed,
 * and on a 360 dp phone that was 125 dp, which is less than both values the
 * sheet really prints ("Night · day page Paper", "Arabic 25, translation
 * 14"): each broke to a second line under a name with room to give (owner
 * report, D-130, on the weights D-116 put in). The weights also squeezed
 * "Theme" to a column so narrow that the word broke one letter to a line and
 * the row became four lines tall (owner report, 37th session).
 *
 * The name's width arrives measured, in pixels, from the text measurer at the
 * row's own size; the rule itself is arithmetic and lives here so the suite
 * can hold it. The gap is the name's own clearance to the value beside it and
 * lives inside the name's column, so the value's right edge never moves.
 */
object SettingsRow {

    /** The name's share of the row, and so the value's share is the rest of it. */
    const val NAME_PERCENT = 55

    /** The width of the name's own column in pixels: its text plus [gapPx]. */
    fun nameColumn(availablePx: Int, nameWidthPx: Int, gapPx: Int): Int {
        if (availablePx <= 0) return 0
        val share = availablePx * NAME_PERCENT / 100
        return minOf(nameWidthPx + gapPx, share).coerceIn(0, availablePx)
    }
}