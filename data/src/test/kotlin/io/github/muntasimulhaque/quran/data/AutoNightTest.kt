package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The page the reader actually reads on, once the system's own day and night
 * has a say. The rule is small and it is the whole of the automatic night
 * mode: with the switch off nothing changes, with it on the system's dark
 * mode means Night, and the day means the reader's own page (or Paper, when
 * that page was itself a night page and would otherwise never change).
 */
class AutoNightTest {

    @Test
    fun theSwitchOffNeverMovesTheChoice() {
        for (theme in AppTheme.entries) {
            for (dark in listOf(true, false)) {
                assertEquals(theme, theme.resolved(autoNight = false, systemDark = dark))
            }
        }
    }

    @Test
    fun aDarkSystemReadsAsNight() {
        for (theme in AppTheme.entries) {
            assertEquals(AppTheme.Night, theme.resolved(autoNight = true, systemDark = true))
        }
    }

    @Test
    fun aLightSystemKeepsThePageTheReaderChose() {
        assertEquals(AppTheme.Paper, AppTheme.Paper.resolved(autoNight = true, systemDark = false))
        assertEquals(AppTheme.Sepia, AppTheme.Sepia.resolved(autoNight = true, systemDark = false))
    }

    @Test
    fun aNightChoiceStillSaysSomethingByDay() {
        assertEquals(AppTheme.Paper, AppTheme.Night.resolved(autoNight = true, systemDark = false))
        assertEquals(AppTheme.Paper, AppTheme.Black.resolved(autoNight = true, systemDark = false))
    }

    @Test
    fun darkAndLightAreTheTwoHalves() {
        assertTrue(AppTheme.Night.isDark())
        assertTrue(AppTheme.Black.isDark())
        assertFalse(AppTheme.Paper.isDark())
        assertFalse(AppTheme.Sepia.isDark())
    }
}
