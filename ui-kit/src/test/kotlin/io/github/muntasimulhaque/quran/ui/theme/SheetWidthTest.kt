package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one page every sheet takes, and the one window that takes the whole
 * window instead. The rule is arithmetic, so it is pinned on a machine with
 * no screen: the widths here are the store's own three profiles, the phone at
 * 411 dp, the seven inch tablet at 601 dp, and the ten inch screen at
 * 1280 dp.
 */
class SheetWidthTest {

    @Test
    fun thePhoneTakesItsWholeWindow() {
        assertEquals(411.dp, Reading.sheetWidth(411.dp))
    }

    @Test
    fun theSevenInchTabletFillsItsWindow() {
        // 800 px at 213 dpi, the profile's own density. The page would leave
        // only 18 dp a side, so the window is taken instead.
        assertEquals(600.94.dp, Reading.sheetWidth(600.94.dp))
    }

    @Test
    fun theTenInchScreenKeepsTheAppPage() {
        assertEquals(Reading.SheetMeasure, Reading.sheetWidth(1280.dp))
    }

    @Test
    fun aWindowInsideTheSliverFills() {
        val window = Reading.SheetMeasure + Space.Section * 2 - 1.dp
        assertEquals(window, Reading.sheetWidth(window))
    }

    @Test
    fun aWindowAtTheSliverKeepsThePage() {
        val window = Reading.SheetMeasure + Space.Section * 2
        assertEquals(Reading.SheetMeasure, Reading.sheetWidth(window))
    }

    @Test
    fun thePageNeverLeavesASliver() {
        // Every window either takes the window (no margin at all) or keeps
        // the page with at least Space.Section of air on each side. A margin
        // between the two is the mistake this rule exists to prevent.
        for (width in 320..1280) {
            val sheet = Reading.sheetWidth(width.dp)
            val air = width.dp - sheet
            assertTrue(
                "a $width dp window left $air dp around the page",
                air == 0.dp || air >= Space.Section * 2,
            )
        }
    }
}
