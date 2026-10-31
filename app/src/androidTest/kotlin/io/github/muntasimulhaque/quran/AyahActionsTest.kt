package io.github.muntasimulhaque.quran

import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SavedStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The ayah's own door, and the one menu it opens.
 *
 * The reading's signifier is the number at each ayah's head: quiet at rest, a
 * capsule with a caret while its menu is open, and the same door whether the
 * reader is sighted or using a screen reader (owner decision, this session:
 * the one-tap ayah of 6.5 and the long press that followed it are both gone,
 * because a signifier that has to be taught is not a signifier). The menu is
 * the app's one popup, the same one the playback pill opens, and every answer
 * closes it and then does its work.
 *
 * The deeper door stands in the menu only while there is a tafsir behind it.
 * A pack the reader removed is a row that is not there rather than a card
 * with nothing behind it (owner decision, this session). The two marked rows
 * say the truth of their own mark: a note keeps its ayah in Saved and never
 * presses Save, one tap of Save then lights both because the reader has now
 * pressed Save, and the second tap asks before it takes the note with it
 * (owner report).
 *
 * The library is prepared before the activity starts, the order a reader
 * creates it in, and the door is the only thing the test touches.
 */
@RunWith(AndroidJUnit4::class)
class AyahActionsTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    // The reading this test drives is the short one: no
                    // translation column, so the first ayah's head is on
                    // screen. The tests share one install, and inheriting a
                    // taller reading from the test before this one pushed the
                    // head off the fold and the wait timed out (owner report,
                    // this session).
                    setTranslationPacks(emptySet())
                    setTafsirPacks(emptySet())
                    setShowTranslation(true)
                    setWordByWord(false)
                }
                // The note the marks are read against, on the ayah whose door
                // the tests open.
                SavedStore(context).apply {
                    setNote(1, NOTE)
                    close()
                }
            }
        }

        override fun after() {
            runBlocking {
                SavedStore(context).apply {
                    unsave(1)
                    close()
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theDeeperDoorIsAbsentWhileNoTafsirIsOnTheDevice() {
        waitForReading()
        openTheDoor()

        // This library carries no tafsir, so there is nothing behind the row
        // and the menu does not offer it.
        compose.onNodeWithText(compose.activity.getString(R.string.action_tafsir))
            .assertDoesNotExist()
    }

    @Test
    fun anAyahsNoteIsNotItsSave() {
        val on = compose.activity.getString(R.string.action_state_on)
        val off = compose.activity.getString(R.string.action_state_off)
        waitForReading()
        openTheDoor()

        // The note is the reader's own writing and the Save is their own tap,
        // so a note alone lights one and not the other.
        assertEquals(
            "the note the reader wrote is the note's own mark",
            on,
            markOf(compose.activity.getString(R.string.action_note)),
        )
        assertEquals(
            "a note keeps the ayah in Saved, and never presses Save",
            off,
            markOf(compose.activity.getString(R.string.action_save)),
        )

        // One tap of Save on an ayah a note kept marks it rather than asking to
        // take away a keep the reader never made. The menu closes on the
        // answer, and the door is opened again to read the new truth.
        compose.onNodeWithText(compose.activity.getString(R.string.action_save))
            .performClick()
        waitForMenuClosed()
        openTheDoor()
        val saveLabel = compose.activity.getString(R.string.action_save)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(saveLabel).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(
            "the reader's own tap marks the ayah",
            on,
            markOf(saveLabel),
        )
        assertEquals(
            "the note is still there, and is still the note's own mark",
            on,
            markOf(compose.activity.getString(R.string.action_note)),
        )

        // The second tap is a save of the reader's own, so it asks first: the
        // note goes with it, and the reader's own words are not taken by one
        // tap of a button. The row keeps the action word Save and lets the
        // filled bookmark and its state say that the save is on.
        compose.onNodeWithText(saveLabel).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("remove-saved-confirm", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun theDoorAndItsRowsAreAWholeTargetAndTheDoorSaysItsState() {
        waitForReading()
        compose.onNodeWithContentDescription(doorName()).assertHeightIsEqualTo(48.dp)

        openTheDoor()
        val play = compose.activity.getString(R.string.action_play)
        compose.onNodeWithText(play).assertHeightIsEqualTo(48.dp)

        // A tap on the page puts the menu away, and the door says it is
        // closed again while it is still on the glass.
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.onNodeWithContentDescription(page).performClick()
        waitForMenuClosed()
        assertEquals(
            "the door is closed again once its menu is away",
            compose.activity.getString(ReadingR.string.reading_door_collapsed),
            doorState(),
        )

        // And a scroll of the reader's own puts it away too, because its door
        // moves under it (owner decision, this session).
        openTheDoor()
        compose.onNodeWithContentDescription(page).performTouchInput {
            swipe(
                start = Offset(center.x, height * 0.82f),
                end = Offset(center.x, height * 0.55f),
                durationMillis = 300,
            )
        }
        waitForMenuClosed()
    }

    @Test
    fun theAyahMenuTakesItsOwnNarrowerMeasure() {
        waitForReading()
        openTheDoor()

        // The five rows are one word each, so the menu no longer borrows the
        // pill panel's 288 dp. The popup's own column carries the measure,
        // and the widest row still has room on the glass.
        compose.onNodeWithTag("app-popup").assertWidthIsEqualTo(216.dp)
        compose.onNodeWithText(compose.activity.getString(R.string.action_share))
            .assertIsDisplayed()
    }

    @Test
    fun theWholeMenuFitsALandscapeWindow() {
        // The activity's requested orientation is not enough on this runner:
        // the display stays where the user left it. The shell command is the
        // same one the capture tour uses. The phone is left in portrait and
        // rotation-locked, which is the orientation the rest of the suite
        // expects and the state the capture tour leaves its own AVD in.
        shell("settings put system accelerometer_rotation 0")
        try {
            shell("settings put system user_rotation 0")
            compose.waitUntil(timeoutMillis = 15_000) {
                val root = composeRoot() ?: return@waitUntil false
                root.boundsInWindow.height > root.boundsInWindow.width
            }
            waitForReading()

            shell("settings put system user_rotation 1")
            compose.waitUntil(timeoutMillis = 30_000) {
                val root = composeRoot() ?: return@waitUntil false
                root.boundsInWindow.width > root.boundsInWindow.height
            }
            waitForReading()
            openTheDoor()

            // The reading's window is short on its side, so the menu used to
            // stop at the room above the door and hide the foot behind a
            // scroll. Every action now fits the safe band.
            compose.onNodeWithText(compose.activity.getString(R.string.action_note))
                .assertIsDisplayed()
            compose.onNodeWithText(compose.activity.getString(R.string.action_share))
                .assertIsDisplayed()
        } finally {
            shell("settings put system user_rotation 0")
            shell("settings put system accelerometer_rotation 0")
        }
    }

    /** The window's own root, for the tests that ask about its shape. */
    private fun composeRoot() = compose
        .onAllNodes(isRoot(), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .firstOrNull()

    /** A shell command through the instrumentation's own shell, as the tour does. */
    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.bufferedReader().readText()
        }
    }

    /** What the door says about its own state, in the app's words. */
    private fun doorState(): String? = compose
        .onNodeWithContentDescription(doorName())
        .fetchSemanticsNode()
        .config
        .getOrElseNullable(SemanticsProperties.StateDescription) { null }

    /** What a row of the menu says about its own mark, in the app's words. */
    private fun markOf(label: String): String? = compose
        .onNodeWithText(label)
        .fetchSemanticsNode()
        .config
        .getOrElseNullable(SemanticsProperties.StateDescription) { null }

    private fun doorName(): String =
        compose.activity.getString(ReadingR.string.reading_ayah_door, "1:1")

    private fun waitForReading() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithContentDescription(doorName()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens the first ayah's own menu from its door and waits for its rows. */
    private fun openTheDoor() {
        compose.onAllNodesWithContentDescription(doorName()).onFirst().performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodes(hasTestTag("app-popup")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText(compose.activity.getString(R.string.action_play))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForMenuClosed() {
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithTag("app-popup").fetchSemanticsNodes().isEmpty()
        }
    }

    private companion object {
        /** The note written on 1:1 before the activity starts. */
        const val NOTE = "The opening verse, read slowly"
    }
}
