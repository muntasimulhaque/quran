package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.settings.R as SettingsR
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The daily reminder's own page: the whole row is the switch, the page has
 * its own quiet door below, and the moment is chosen on a clock that reaches
 * every minute.
 *
 * The test follows the shape a reader meets, in the order a reader meets it.
 * The hub row turns the reminder on; the door below opens the page and does
 * not toggle; the page carries the moment; and the time row opens the picker,
 * whose Set writes the moment where the alarm reads it. A row that only
 * answered at its switch was the old shape, and so was a picker that could
 * only reach the top of the hour.
 */
@RunWith(AndroidJUnit4::class)
class DailyAyahToggleTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            // Turning the reminder on, and moving its moment, both ask for the
            // notification permission, which is the real behavior a reader
            // meets. The dialog would stop the activity and take the compose
            // hierarchy with it, so the test grants the permission up front and
            // checks the switch, the page, the picker, and the stored minute
            // instead of the system dialog.
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                android.Manifest.permission.POST_NOTIFICATIONS,
            )
            // The same two acts also ask, in the phone's own screen, for the
            // exact alarm, which Android 14 and later withhold until the
            // reader allows it. That screen would cover the app and take the
            // compose hierarchy with it, so the test answers it the way a
            // reader who wants the reminder would: once, up front.
            instrumentation.uiAutomation.executeShellCommand(
                "appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow",
            ).close()
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setDailyAyah(false)
                    setDailyAyahTime(8 * 60)
                }
            }
        }

        override fun after() {
            runBlocking {
                SettingsStore(context).apply {
                    setDailyAyah(false)
                    setDailyAyahTime(8 * 60)
                }
            }
            // The reminder's own alarm goes when its switch does, so no exact
            // alarm is left standing. The exact grant itself is left as the
            // reader gave it: taking it back while this process is alive makes
            // the system kill it, and the connected run uninstalls the app,
            // which clears the grant anyway.
            io.github.muntasimulhaque.quran.daily.DailyAyahScheduler.apply(
                context,
                enabled = false,
                minuteOfDay = 8 * 60,
            )
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theRowIsTheSwitchAndItsDoorIsSeparate() {
        openSettings()

        // The whole row turns the reminder on. A row that only answered at
        // its switch was the old shape, and this is the new one.
        compose.onNodeWithTag("row-daily").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            runBlocking { readSettings().dailyAyah }
        }

        // The page has its own quiet door below the row, and opening it does
        // not change the setting the row just made.
        compose.onNodeWithTag("open-daily").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("daily-page", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "opening the page must not change the setting",
            runBlocking { readSettings().dailyAyah },
        )

        // The page carries the moment, in the reader's own time format. The
        // digits are the interface's, so the assertion is on the numbers and
        // not on a 12 or 24 hour choice the emulator may have made.
        compose.onNodeWithText(compose.activity.getString(SettingsR.string.settings_daily_time_title))
            .performScrollTo()
            .performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(
                compose.activity.getString(SettingsR.string.settings_daily_picker_title),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(compose.activity.getString(SettingsR.string.settings_set))
            .performClick()
        assertEquals(
            "Set must write the chosen moment, minute for minute",
            8 * 60,
            runBlocking { readSettings().dailyAyahMinute },
        )
    }

    @Test
    fun theHubRowSaysTheMomentItWillArrive() {
        openSettings()
        val title = compose.activity.getString(SettingsR.string.settings_daily_title)
        compose.onNodeWithTag("row-daily").performClick()
        // The subtitle is the time itself, so the reader can see the moment
        // without opening the page for it. The digits are the interface's, so
        // the assertion is on the numbers and not on a 12 or 24 hour choice.
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("8:00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(title).assertIsDisplayed()
    }

    private suspend fun readSettings() = SettingsStore(context).settings.first()

    private fun openSettings() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(readingPage).onFirst()
            .tapThePaper()
        val settings = compose.activity.getString(R.string.action_settings)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(settings).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(settings).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("settings-hub", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // The daily row sits below the fold on the phone profile, so it is
        // scrolled to before it is read or tapped.
        compose.onNodeWithTag("row-daily").performScrollTo()
        Thread.sleep(400)
    }
}
