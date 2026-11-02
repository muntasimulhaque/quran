package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
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
import java.io.File

/**
 * The daily reminder's own page: the switch is the switch, the row is the
 * door, and the moment is chosen on the app's own hour and minute tapes,
 * which reach every minute.
 *
 * The test follows the shape a reader meets, in the order a reader meets it.
 * The hub row's switch turns the reminder on and nothing else does; the rest
 * of the row opens the page; the page carries the same switch at its top and
 * the moment below it; and the time row opens the picker, whose Set writes the
 * moment where the alarm reads it. A tap on the row body that flipped the
 * switch would be the bug this pins, and so would a picker that could only
 * reach the top of the hour or a page switch that answered a different value
 * from the hub's.
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
        // The page carries the moment and the same switch at its top, and its
        // own mark is what the wait holds on to.
        compose.onNodeWithText(title).performClick()
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
        compose.onNodeWithTag("switch-daily").performClick()
        // The subtitle is the time itself, so the reader can see the moment
        // without opening the page for it. The digits are the interface's, so
        // the assertion is on the numbers and not on a 12 or 24 hour choice.
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("8:00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(title).assertIsDisplayed()
    }

    /**
     * The page's switch is the same reminder as the hub's.
     *
     * One value in two places: the row that opened the page carries a switch,
     * and the page carries the same one at its top. A reader who tapped the
     * words to flip it lands here and finds the switch they meant, and turning
     * it off here turns it off on the hub too (owner decision, this session).
     */
    @Test
    fun thePageSwitchIsTheSameReminder() {
        openSettings()
        val title = compose.activity.getString(SettingsR.string.settings_daily_title)
        // The row is a door, so this opens the page rather than flipping it.
        compose.onNodeWithText(title).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("daily-page", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("switch-daily-page", useUnmergedTree = true)
            .performTouchInput { click() }
        compose.waitUntil(timeoutMillis = 15_000) {
            runBlocking { readSettings().dailyAyah }
        }
        // Back on the hub, the row's own switch reads the same answer.
        back()
        compose.onNodeWithTag("switch-daily", useUnmergedTree = true)
            .performScrollTo()
            .assertIsOn()
    }

    /**
     * The moment can be typed, and the typing has a visible door.
     *
     * The big hour and minute answer a tap or a long press, but a gesture that
     * has to be taught is not a signifier; the keyboard mark under the tapes
     * is. This opens the picker, photographs it before the keyboard covers the
     * page, then types 09:30 through the mark, the hour field, the IME's Next,
     * and the minute field, and reads the stored minute back.
     */
    @Test
    fun theMomentCanBeTypedAndTheKeyboardIsAVisibleDoor() {
        openSettings()
        val pageTitle = compose.activity.getString(SettingsR.string.settings_daily_title)
        compose.onNodeWithText(pageTitle).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("daily-page", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(compose.activity.getString(SettingsR.string.settings_daily_time_title))
            .performScrollTo()
            .performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("daily-time-picker", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        val frame = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotOutputDirectory(), "daily-time-picker.png").outputStream().use { output ->
            frame.compress(Bitmap.CompressFormat.PNG, 100, output)
        }

        val typeName = compose.activity.getString(SettingsR.string.settings_daily_picker_type)
        compose.onNodeWithContentDescription(typeName).performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).performTextInput("09")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).performTextInput("30")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText(compose.activity.getString(SettingsR.string.settings_set))
            .performClick()
        assertEquals(
            "the typed 09:30 must be the minute the alarm reads",
            9 * 60 + 30,
            runBlocking { readSettings().dailyAyahMinute },
        )
    }

    /**
     * The tape and the field are one value while the field is open.
     *
     * The owner long-pressed the hour, moved the tape, and the tape did
     * nothing: the draft the field held was written back on the next commit,
     * so only the keyboard could change the time. This opens the editor,
     * moves the hour tape and then the minute tape, and reads the stored
     * moment after Set, which is the value the alarm actually keeps.
     */
    @Test
    fun theTapeAndTheTypedFieldKeepOneValue() {
        openSettings()
        val pageTitle = compose.activity.getString(SettingsR.string.settings_daily_title)
        compose.onNodeWithText(pageTitle).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("daily-page", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(compose.activity.getString(SettingsR.string.settings_daily_time_title))
            .performScrollTo()
            .performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("daily-time-picker", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        val hourName = compose.activity.getString(SettingsR.string.settings_daily_picker_hour)
        val minuteName = compose.activity.getString(SettingsR.string.settings_daily_picker_minute)
        val twentyFour = DateFormat.is24HourFormat(context)

        // Long-press the big hour to open the field, then move the hour tape
        // without touching the keyboard.
        compose.onAllNodesWithContentDescription(hourName).onFirst()
            .performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(hourName).onLast()
            .performSemanticsAction(SemanticsActions.SetProgress) {
                it(if (twentyFour) 9f else 8f)
            }
        compose.onNode(hasSetTextAction()).assertTextContains("9")

        // The minute's field is a second door to the same tape.
        compose.onAllNodesWithContentDescription(minuteName).onFirst()
            .performTouchInput { click() }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(minuteName).onLast()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        compose.onNode(hasSetTextAction()).assertTextContains("30")

        compose.onNodeWithText(compose.activity.getString(SettingsR.string.settings_set))
            .performClick()
        assertEquals(
            "the tape's hour and minute must be the moment Set writes",
            9 * 60 + 30,
            runBlocking { readSettings().dailyAyahMinute },
        )
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        Thread.sleep(700)
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
        compose.onNodeWithTag("switch-daily").performScrollTo()
        Thread.sleep(400)
    }
}
