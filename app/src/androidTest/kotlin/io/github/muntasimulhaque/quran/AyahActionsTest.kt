package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsProperties
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
 * The deeper door names what is behind it, and each mark says the tap that
 * made it.
 *
 * The ayah card holds only the tafsir doors, so the
 * pill carries a Tafsir action with its scroll mark, standing beside Play at
 * the front of the bar. That is the first
 * assertion here. The second is the pair of marks on the same pill: a note
 * keeps its ayah in Saved and never presses Save, so a note written before
 * the activity starts lights Note and leaves Save dark, one tap of Save then
 * lights both because the reader has now pressed Save, and a second tap asks
 * before it takes the mark and the note with it (owner report).
 *
 * The library is prepared before the activity starts, the order a reader
 * creates it in, and a long press on the ayah is the only thing the test
 * touches.
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
                    // translation column, so the reference line at the foot
                    // of the first ayah is on screen. The tests share one
                    // install, and inheriting a taller reading from the test
                    // before this one pushed the line off the fold and the
                    // wait timed out (owner report, this session).
                    setTranslationPacks(emptySet())
                    setTafsirPacks(emptySet())
                    setShowTranslation(true)
                    setShowTafsir(true)
                    setWordByWord(false)
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
        openReadingAyah()
        compose.onNodeWithContentDescription("Tafsir").assertExists()
    }

        @Test
    fun anAyahsNoteIsNotItsSave() {
        // The app's own words for a mark, so the test reads what the pill
        // says rather than a copy of it that a rename would leave behind.
        val on = compose.activity.getString(R.string.action_state_on)
        val off = compose.activity.getString(R.string.action_state_off)
        waitForReading()
        openReadingAyah()

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

    /**
     * The press fills the action's own capsule, not the 44 dp box the mark
     * and its word make: the state layer and the ripple draw on the clipped
     * control, so the shape the reader sees is the shape their finger gets,
     * and no circle is cut flat by a shorter node inside it (owner report,
     * this session).
     */
    @Test
    fun thePressFillsTheActionItsOwnCapsule() {
        waitForReading()
        openReadingAyah()
        // The shape is a child of the clickable, which merges its
        // descendants, so the tag is read from the unmerged tree.
        val shapes = compose.onAllNodesWithTag("ayah-action-shape", useUnmergedTree = true)
        // Play, Tafsir, Save, Note, Share.
        shapes.assertCountEquals(5)
        repeat(5) { index ->
            shapes[index].assertHeightIsEqualTo(48.dp)
        }
        // The bar hands its one measured width to all five, so the five
        // press shapes match rather than five ovals of the words' own making.
        val first = shapes[0].getUnclippedBoundsInRoot()
        val last = shapes[4].getUnclippedBoundsInRoot()
        // The widths are read back through the density, and two nodes placed
        // on the same pixel edge can come back a hair apart in dp (48.0
        // against 47.999992 with Compose 1.12's rounding). A hundredth of a
        // dp is under any drawn difference and keeps the assertion about the
        // one measured width instead of about float noise.
        assertEquals(
            "the five actions press in one width",
            (first.right - first.left).value,
            (last.right - last.left).value,
            0.01f,
        )
    }

    /** What the pill's action says about its own mark, in the app's words. */
    private fun markOf(label: String): String? = compose
        .onNodeWithContentDescription(label)
        .fetchSemanticsNode()
        .config
        .getOrElseNullable(SemanticsProperties.StateDescription) { null }

    private fun waitForReading() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The chrome comes up on a tap on the ayah or the paper around it, and
     * the ayah's own actions on a long press of the ayah itself, the one
     * gesture the reading keeps.
     */
    private fun openReadingAyah() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.onAllNodesWithContentDescription(readingPage).onFirst()
            .tapThePaper()
        // Seconds, not fractions: a slow emulator is not a failing reading
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
