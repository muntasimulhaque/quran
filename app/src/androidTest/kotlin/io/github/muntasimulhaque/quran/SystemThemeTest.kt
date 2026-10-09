package io.github.muntasimulhaque.quran

import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app follows the phone's own day and night, at the moment it changes.
 *
 * The reader turned on the one switch that says so, and the app carried
 * `uiMode` in its `configChanges`, which is a promise to the platform to
 * handle a theme change itself. It did not: the interface language is applied
 * in `attachBaseContext` through a context derived with
 * `createConfigurationContext`, a snapshot taken when the window is built, and
 * nothing recomposed the page when the phone's own colours turned over
 * underneath it. So the phone in its day colours and the app on the night page
 * it was opened on, at ten in the morning, with the switch on (owner report,
 * 37th session).
 *
 * Two halves are pinned. The manifest one is cheap and always true: the app
 * does not claim a change it does not handle. The behavioral one is what a
 * reader would see: the ground the reading is drawn on turns over with the
 * phone, and it turns over again on the way back, while the activity is
 * alive.
 */
@RunWith(AndroidJUnit4::class)
class SystemThemeTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val uiMode: UiModeManager =
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(UiModeManager::class.java)

    @Before
    fun setUp() {
        LanguagePreference(context).set("en")
        PackStore(context).install("translation-saheeh-en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setAyah(1)
                setTheme(AppTheme.Paper)
                setAutoNight(true)
            }
        }
    }

    @After
    fun tearDown() {
        // The app's own night override goes back to the day, and the switch
        // goes off with it: a suite that leaves the app on the night page
        // photographs a night page in whatever runs next. The "follow the
        // system" value is a hidden constant on the platform, so the test
        // pins the day instead, which is the state every other test wants.
        runCatching { uiMode.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO) }
        runCatching { InstrumentationRegistry.getInstrumentation().waitForIdleSync() }
        runBlocking { SettingsStore(context).setAutoNight(false) }
    }

    /**
     * The app must not claim the theme change. A window that is recreated by
     * the platform reads a fresh configuration, which is the whole fix; a
     * window that handles the change itself has to be told, and this app has
     * no path that tells it.
     */
    @Test
    fun theAppDoesNotClaimTheThemeChange() {
        val info = context.packageManager.getActivityInfo(
            android.content.ComponentName(context, MainActivity::class.java),
            0,
        )
        assertEquals(
            "the app must not claim a theme change it does not handle itself",
            0,
            info.configChanges and ActivityInfo.CONFIG_UI_MODE,
        )
    }

    /**
     * The reading's ground turns over with the phone, in both directions,
     * while the app is open. The phone's own night mode is set twice and the
     * drawn ground is read after each, from a real screen capture, because a
     * configuration that says night and a page that is still white is exactly
     * the defect this pins.
     */
    @Test
    fun theReadingFollowsThePhonesDayAndNight() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            uiMode.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
            awaitGround(scenario, dark = true)

            uiMode.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
            awaitGround(scenario, dark = false)
        }
    }

    /**
     * Waits for the page the reader would be looking at. The window is
     * recreated by the platform when the theme turns over, so the capture is
     * retaken until it settles, and the waits are minutes rather than seconds:
     * a slow emulator is not a failing reading aid.
     */
    private fun awaitGround(scenario: ActivityScenario<MainActivity>, dark: Boolean) {
        val deadline = System.currentTimeMillis() + 180_000L
        var reason = "the reading never turned over"
        while (System.currentTimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            // The window is rebuilt when the theme turns over, so the answer
            // is asked of whichever instance is on screen now and the old one
            // is never held on to.
            var night: Boolean? = null
            runCatching {
                scenario.onActivity { activity -> night = nightOf(activity) }
            }
            val drawn = runCatching { groundIsDark() }.getOrNull()
            if (night == dark && drawn == dark) return
            reason = "the app's own night mode is $night and the drawn ground is dark=$drawn"
            Thread.sleep(1_000)
        }
        throw AssertionError("the reading did not follow the phone ($reason)")
    }

    private fun nightOf(activity: MainActivity): Boolean =
        (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * Whether the page on screen is a night page, read from a pixel in the
     * lower part of the reading, where the page is paper and neither the chrome
     * above nor the phone's own bars below reach. The four grounds are chosen
     * far apart (the paper grounds are near white, the night grounds are near
     * black), so one measured sum against the middle of the range tells them
     * apart without naming a color.
     */
    private fun groundIsDark(): Boolean {
        val bitmap = Screenshot.capture().bitmap
        val y = (bitmap.height * 0.80f).toInt().coerceIn(0, bitmap.height - 1)
        val x = (bitmap.width * 0.5f).toInt().coerceIn(0, bitmap.width - 1)
        val pixel = bitmap.getPixel(x, y)
        val sum = Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)
        bitmap.recycle()
        return sum < 384
    }
}
