package io.github.muntasimulhaque.quran.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The theme a stored install reads as, once System became the third value.
 *
 * The theme was two grounds and a separate automatic switch; it is now one of
 * three values. An install that had the switch on lands on System, the value
 * that does what the switch did, and one that left it off keeps the ground it
 * chose: the old Paper as Light and Night as Dark. The removed "sepia" and
 * "black" pages land on the ground that replaced them. A reader who updated
 * must never find their page moved for a reason they cannot see (owner
 * decision, this session).
 *
 * This pins the read the launch mirror and the settings store share, so a
 * migration that drops a reader's choice fails here rather than in the shade.
 */
class ThemeMigrationTest {

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    private val prefs by lazy {
        context.getSharedPreferences("ui-theme", Context.MODE_PRIVATE)
    }

    @Before
    fun setUp() {
        prefs.edit().clear().apply()
    }

    @After
    fun tearDown() {
        prefs.edit().clear().apply()
    }

    private fun write(theme: String?, auto: Boolean?) {
        prefs.edit().apply {
            if (theme == null) remove("theme") else putString("theme", theme)
            if (auto == null) remove("auto_night") else putBoolean("auto_night", auto)
        }.apply()
    }

    @Test
    fun theNewValuesReadAsThemselves() {
        write("light", false)
        assertEquals(AppTheme.Light, ThemePreference(context).theme())
        write("dark", false)
        assertEquals(AppTheme.Dark, ThemePreference(context).theme())
        write("system", false)
        assertEquals(AppTheme.System, ThemePreference(context).theme())
    }

    @Test
    fun theSwitchOnBecomesSystem() {
        write("paper", true)
        assertEquals(AppTheme.System, ThemePreference(context).theme())
        write("night", true)
        assertEquals(AppTheme.System, ThemePreference(context).theme())
    }

    @Test
    fun theSwitchOffKeepsTheGroundChosen() {
        write("paper", false)
        assertEquals(AppTheme.Light, ThemePreference(context).theme())
        write("night", false)
        assertEquals(AppTheme.Dark, ThemePreference(context).theme())
    }

    @Test
    fun aFreshInstallFollowsTheSystem() {
        write(null, null)
        assertEquals(AppTheme.System, ThemePreference(context).theme())
    }

    @Test
    fun theRemovedPagesLandOnTheGroundThatReplacedThem() {
        write("sepia", false)
        assertEquals(AppTheme.Light, ThemePreference(context).theme())
        write("black", false)
        assertEquals(AppTheme.Dark, ThemePreference(context).theme())
        write("black", true)
        assertEquals(AppTheme.System, ThemePreference(context).theme())
    }
}
