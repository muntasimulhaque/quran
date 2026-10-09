package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SavedStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.study.R as StudyR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The deeper door names what is behind it, and each mark says the tap that
 * made it.
 *
 * The ayah card holds only the tafsir doors, so the
 * pill's last action reads Tafsir with its scroll mark. That is the first
 * assertion here. The second is the pair of marks on the same pill: a note
 * keeps its ayah in Saved and never presses Save, so a note written before
 * the activity starts lights Note and leaves Save dark, one tap of Save then
 * lights both because the reader has now pressed Save, and a second tap asks
 * before it takes the mark and the note with it (owner report).
 *
 * The library is prepared before the activity starts, the order a reader
 * creates it in, and the long presses are the only thing the test touches.
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
                }
                // The note the marks are read against, on the ayah the press
                // below raises the pill over.
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
    fun theDeeperDoorNamesTheReadingItWasRaisedOver() {
        waitForReading()

        // The card behind the door is only the tafsirs.
        longPressReadingAyah()
        compose.onNodeWithContentDescription("Tafsir").assertExists()
    }

        @Test
    fun anAyahsNoteIsNotItsSave() {
        // The app's own words for a mark, so the test reads what the pill
        // says rather than a copy of it that a rename would leave behind.
        val on = compose.activity.getString(R.string.action_state_on)
        val off = compose.activity.getString(R.string.action_state_off)
        waitForReading()
        longPressReadingAyah()

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

        // One tap of Save on an ayah a note kept marks it rather than asking
        // to take away a keep the reader never made.
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.action_save))
            .performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            markOf(compose.activity.getString(R.string.action_save)) == on
        }
        assertEquals(
            "the note is still there, and is still the note's own mark",
            on,
            markOf(compose.activity.getString(R.string.action_note)),
        )

        // The second tap is a save of the reader's own, so it asks first: the
        // note goes with it, and the reader's own words are not taken by one
        // tap of a button.
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.action_save))
            .performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("remove-saved-confirm", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** What the pill's action says about its own mark, in the app's words. */
    private fun markOf(label: String): String? = compose
        .onNodeWithContentDescription(label)
        .fetchSemanticsNode()
        .config
        .getOrElseNullable(SemanticsProperties.StateDescription) { null }

    private fun waitForReading() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The chrome comes up on a tap on the paper, and the pill on a long press
     * of the ayah's own reference line, which the block owns.
     */
    private fun longPressReadingAyah() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
            .tapThePaper()
        // Minutes, not seconds: a slow emulator is not a failing reading
        // aid, and the house rule for a wait that watches text is the one
        // [waitForReading] already follows. Fifteen seconds timed out on a
        // cold first run before the block had drawn, twice.
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText("1:1", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("1:1", substring = true).onFirst()
            .performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithContentDescription("Tafsir").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        /** The note written on 1:1 before the activity starts. */
        const val NOTE = "The opening verse, read slowly"
    }
}
