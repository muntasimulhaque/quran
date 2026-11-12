package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The room an action head owes a sheet's scroll, measured without a screen.
 *
 * The action's 48 dp target is centered on the name's line; the part above the
 * line is what a scroll clips if the content starts at zero. The numbers are
 * the whole rule: 11 dp at the default 26 dp line, half the difference at any
 * other line, and nothing once the name is as tall as the target.
 */
class SheetHeadTest {

    @Test
    fun theDefaultNameLineOwesTheTargetElevenDp() {
        assertEquals(11.dp, sheetHeadInsetFor(26.dp))
    }

    @Test
    fun aLargerNameLineOwesLess() {
        assertEquals(4.5f, sheetHeadInsetFor(39.dp).value, Tolerance)
    }

    @Test
    fun aNameLineAsTallAsTheTargetOwesNothing() {
        assertEquals(0.dp, sheetHeadInsetFor(48.dp))
        assertEquals(0.dp, sheetHeadInsetFor(52.dp))
    }

    private companion object {
        const val Tolerance = 0.001f
    }
}
