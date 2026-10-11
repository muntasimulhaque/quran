package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The page the reader actually reads on, once the system's own day and night
 * has a say. The rule is small and it is the whole of the System value: a
 * dark system is Dark and a light one is Light, while Light and Dark are the
 * reader's own and never move, whichever way the phone is turned (owner
 * decision, this session: System is the third value that replaced the
 * automatic switch, and a fixed choice is finally a fixed choice).
 */
class ThemeResolveTest {

    @Test
    fun aDarkSystemReadsAsDark() {
        assertEquals(AppTheme.Dark, AppTheme.System.resolved(systemDark = true))
    }

    @Test
    fun aLightSystemReadsAsLight() {
        assertEquals(AppTheme.Light, AppTheme.System.resolved(systemDark = false))
    }

    @Test
    fun aFixedChoiceNeverMovesWithTheSystem() {
        for (theme in listOf(AppTheme.Light, AppTheme.Dark)) {
            for (dark in listOf(true, false)) {
                assertEquals(theme, theme.resolved(systemDark = dark))
            }
        }
    }

    @Test
    fun onlyDarkIsTheDarkHalf() {
        assertTrue(AppTheme.Dark.isDark())
        assertFalse(AppTheme.Light.isDark())
        assertFalse(AppTheme.System.isDark())
    }
}
