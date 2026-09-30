package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The settings row's own arithmetic, pinned here because both of its ways of
 * going wrong are visible on a screen and invisible in a build.
 */
class SettingsRowTest {

    private val gap = 24

    @Test
    fun aNameThatFitsTakesItsOwnWidthAndTheValueTakesTheRest() {
        // 278 px of row, a 48 dp "Theme" and a value that needs 130 dp: the
        // name takes its own width and the value takes the other 206, which is
        // what the fixed share used to deny it. On a 360 dp phone the value's
        // share was 125 px and both values the sheet prints are longer than
        // that, so the theme and font rows each broke to a second line under a
        // name with room to give (owner report, D-130).
        assertEquals(
            48 + gap,
            SettingsRow.nameColumn(availablePx = 278, nameWidthPx = 48, gapPx = gap),
        )
    }

    @Test
    fun aLongNameNeverTakesTheValuesColumn() {
        // A name longer than its share stops at the share, so the value keeps
        // the other 45 percent and the name is the one that wraps.
        val available = 278
        val column = SettingsRow.nameColumn(availablePx = available, nameWidthPx = 400, gapPx = gap)
        assertEquals("the name never takes more than its share", available * 55 / 100, column)
        assertTrue(
            "the value keeps at least the other 45 percent",
            available - column >= available * 45 / 100,
        )
    }

    @Test
    fun aLongValueCostsTheValueItsSecondLineAndNotTheNameItsWord() {
        // Nothing about the value enters the rule: the name takes its own
        // width, and a value too long for what is left takes a second line
        // rather than squeezing the name (D-116).
        assertEquals(
            60 + gap,
            SettingsRow.nameColumn(availablePx = 278, nameWidthPx = 60, gapPx = gap),
        )
    }

    @Test
    fun theColumnIsNeverWiderThanTheRow() {
        for (available in intArrayOf(0, 1, 40, 200, 278, 1200)) {
            for (name in intArrayOf(0, 10, 300, 5000)) {
                for (clearance in intArrayOf(0, 24, 200)) {
                    val column = SettingsRow.nameColumn(available, name, clearance)
                    assertTrue(
                        "the column ($column) must stay inside the row ($available)",
                        column in 0..available,
                    )
                }
            }
        }
    }

    @Test
    fun anEmptyNameLeavesTheValueTheRow() {
        assertEquals(
            "a row with no name gives its width to the value",
            gap,
            SettingsRow.nameColumn(availablePx = 278, nameWidthPx = 0, gapPx = gap),
        )
    }
}