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
import io.github.muntasimulhaque.quran.feature.study.R as StudyR
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The daily reminder is off until the reader asks, and its hour appears when
 * it is on.
 *
 * Nothing in this app schedules itself, so the switch is the whole door: with
 * it off there is no alarm, no notification, and no hour row; with it on the
 * reader picks the hour and the row says which one. The setting is written
 * through the same store the alarm reads, so this pins the switch's work.
 */
@RunWith(AndroidJUnit4::class)
class DailyAyahToggleTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            // Turning the reminder on asks for the notification permission,
            // which is the real behavior a reader meets. The dialog would
            // stop the activity and take the compose hierarchy with it, so
            // the test grants the permission up front and checks the switch,
            // the hour row, and the setting instead of the system dialog.
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
                    setDailyAyahHour(8)
                }
            }
        }

        override fun after() {
            runBlocking {
                SettingsStore(context).apply {
                    setDailyAyah(false)
                    setDailyAyahHour(8)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theReminderIsOffUntilTheSwitchIsTurnedOn() {
        openSettings()
        val title = compose.activity.getString(
            io.github.muntasimulhaque.quran.feature.settings.R.string.settings_daily_title,
        )
        compose.onNodeWithText(title).assertIsDisplayed()

        // Off: the hour row is not offered, because there is no reminder to
        // arrive at any hour.
        val hourTitle = compose.activity.getString(
            io.github.muntasimulhaque.quran.feature.settings.R.string.settings_daily_hour_title,
        )
        assertTrue(
            "the hour must not be asked for while the reminder is off",
            compose.onAllNodesWithText(hourTitle).fetchSemanticsNodes().isEmpty(),
        )

        // On: the hour row appears.
        compose.onNodeWithText(title).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(hourTitle).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("8:00").performScrollTo()
        compose.onNodeWithText("8:00").assertIsDisplayed()

        // The choice is written where the alarm reads it.
        val stored = runBlocking { readSettings() }
        assertTrue("the switch must be remembered", stored)
    }

    private suspend fun readSettings(): Boolean =
        SettingsStore(context).settings.first().dailyAyah

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
        // The daily switch sits below the fold on the phone profile, so it
        // is scrolled to before it is read or tapped.
        compose.onNodeWithText(
            compose.activity.getString(
                io.github.muntasimulhaque.quran.feature.settings.R.string.settings_daily_title,
            ),
        ).performScrollTo()
        Thread.sleep(400)
    }
}
