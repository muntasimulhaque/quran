package io.github.muntasimulhaque.quran

import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
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
import io.github.muntasimulhaque.quran.data.PackStore
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
 * The reading's signifier is the number at each ayah's head: the app's action
 * color, no capsule, and the same door whether the reader is sighted or using
 * a screen reader. The whole verse is not a button: a tap on the Quran is the
 * page's own tap, and the menu the number opens is the app's one popup, so a
 * tap outside it puts it away instead of choosing another ayah (owner
 * decision, this session: 16.6's whole-verse tap was half a solution, because
 * the page has almost no room left for a tap that means something else).
 *
 * The deeper door always stands in the menu. Without a pack it opens the
 * offer that names the language, the pack, and the size, rather than hiding
 * the reader's question. The two marked rows say the truth of their own mark:
 * a note keeps its ayah in Saved and never presses Save, one tap of Save then
 * lights both because the reader has now pressed Save, and the second tap asks
 * before it takes the note with it (owner report).
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
            // The offer leg needs the English tafsir off the device, so the
            // card has a pack to offer. The library is prepared before the
            // activity starts.
            PackStore(context).remove("tafsir-ibn-kathir-en")
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
    fun theDeeperDoorOffersATafsirWhileNoneIsOnTheDevice() {
        waitForReading()
        openTheDoor()

        // The question is always answered: without a pack, the door opens the
        // offer that names the language, the pack, and the size.
        val tafsir = compose.activity.getString(R.string.action_tafsir)
        compose.onNodeWithText(tafsir).assertExists().performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Ibn Kathir", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
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
    fun theDoorIsAWholeTargetAndThePagePutsItsMenuAway() {
        waitForReading()
        // The door is the number alone, but its target is the 48 dp every
        // control owes a finger.
        compose.onNodeWithTag(doorName()).assertIsDisplayed().assertHeightIsEqualTo(48.dp)

        openTheDoor()

        // A tap on the page puts the menu away, and it cannot land on another
        // verse: the popup is a window of its own, and a tap outside it only
        // dismisses it (owner decision, this session).
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.onAllNodesWithContentDescription(page).onFirst().tapThePaper()
        waitForMenuClosed()

        // And a scroll of the reader's own puts it away too, because its door
        // moves under it.
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
    fun theMenuCarriesEveryAct() {
        waitForReading()
        openTheDoor()

        // The menu is the app's one popup, and it carries every act.
        listOf(
            R.string.action_play,
            R.string.action_tafsir,
            R.string.action_save,
            R.string.action_note,
            R.string.action_share,
        ).forEach { act ->
            compose.onNodeWithText(compose.activity.getString(act)).assertIsDisplayed()
        }
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

    /** What a row of the menu says about its own mark, in the app's words. */
    private fun markOf(label: String): String? = compose
        .onNodeWithText(label)
        .fetchSemanticsNode()
        .config
        .getOrElseNullable(SemanticsProperties.StateDescription) { null }

    /** The first verse's own number, and the tap it answers. */
    private fun doorName(): String = "door-1:1"

    private fun waitForReading() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag(doorName()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens the first verse's own menu and waits for its rows. */
    private fun openTheDoor() {
        compose.onNodeWithTag(doorName()).performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText(compose.activity.getString(R.string.action_play))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForMenuClosed() {
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(compose.activity.getString(R.string.action_play))
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private companion object {
        /** The note written on 1:1 before the activity starts. */
        const val NOTE = "The opening verse, read slowly"
    }
}
