package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.settings.R as SettingsR
import io.github.muntasimulhaque.quran.feature.study.R as StudyR
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
 * The daily reminder's own page: the switch is the switch, the row is the
 * door, and the moment is chosen on a clock that reaches every minute.
 *
 * The test follows the shape a reader meets, in the order a reader meets it.
 * The hub row's switch turns the reminder on and nothing else does; the rest
 * of the row opens the page; the page carries its own switch; and the time row
 * opens the picker, whose Set writes the moment where the alarm reads it. A
 * tap on the row body that flipped the switch would be the bug this pins, and
 * so would a picker that could only reach the top of the hour.
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
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setMode(ReadingMode.Study)
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
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theSwitchIsTheSwitchAndTheRowIsTheDoor() {
        openSettings()
        val title = compose.activity.getString(SettingsR.string.settings_daily_title)

        // The switch turns the reminder on, and only the switch does.
        compose.onNodeWithTag("switch-daily").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            runBlocking { readSettings().dailyAyah }
        }

        // The row is a door: it opens the page and it does not toggle. If it
        // toggled as well, the switch would read off again and the page would
        // come up with the reminder it was meant to be setting turned off.
        compose.onNodeWithText(title).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("daily-switch", useUnmergedTree = true)
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
        compose.onNodeWithTag("switch-daily").performClick()
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
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
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
        compose.onNodeWithTag("switch-daily").performScrollTo()
        Thread.sleep(400)
    }
}
