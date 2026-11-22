package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Not now, measured on the glass (owner decision, 21.9): the first tap
 * takes the ask off the boundary it was tapped on, the moved ask stands
 * at the end of the next surah the reader finishes, and the second tap
 * is the final answer.
 *
 * The reader starts at Al-Fatiha's last ayah with the answer unwritten
 * and the reminder off, the state a fresh install holds at the first
 * boundary. The second boundary is Al-Baqarah's last ayah, reached the
 * way a reader reaches it: the browse sheet, the surah's row, its own
 * numbers, one tap. Every instrumented class in a run shares one process
 * and one set of settings, so the class hands the app back answered,
 * the settled state every existing install keeps.
 *
 * The tile is brought fully onto the glass before either answer is
 * tapped. The end of a long surah first shows the tile's top edge, and a
 * tap aimed at a word below the window is a tap the system swallows, so
 * the test scrolls the tile itself into view rather than trusting the
 * first frame that is partly visible.
 */
@RunWith(AndroidJUnit4::class)
class ReminderOfferSnoozeTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(7)
                    setDailyAyah(false)
                    setReminderAnswered(false)
                    setReminderDismissedSurah(0)
                }
            }
        }

        override fun after() {
            // The settled state every existing install keeps, so a class
            // that walks the reading after this one never meets the tile.
            runBlocking {
                SettingsStore(context).apply {
                    setDailyAyah(false)
                    setReminderAnswered(true)
                    setReminderDismissedSurah(0)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun notNowMovesTheAskAndTheSecondTapIsFinal() {
        // The first boundary: the offer stands under Al-Fatiha's closing
        // line, and Not now moves the ask off this boundary.
        waitUntilTheReadingIsUp()
        swipeUntilTheOfferStands()
        assertTrue(
            "the offer must stand at the first boundary before it is answered",
            theOfferStands(),
        )
        tapNotNow()
        assertFalse(
            "the first Not now must take the offer off the boundary it was tapped on",
            theOfferStands(),
        )

        // The second boundary: the moved ask stands at the end of the next
        // surah the reader finishes, and the second Not now is final.
        landOn(2, 286)
        swipeUntilTheOfferStands()
        assertTrue(
            "the moved ask must stand at the end of the next surah the reader finishes",
            theOfferStands(),
        )
        tapNotNow()
        assertFalse(stateAfterTheTap(), theOfferStands())
    }

    /**
     * What the second answer wrote, read for the failure message so a red
     * leg says whether the tap landed or the tile outlived it.
     */
    private fun stateAfterTheTap(): String {
        val settings = runBlocking {
            runCatching { SettingsStore(context).settings.first() }.getOrNull()
        }
        val tile = runCatching {
            compose.onNodeWithTag("reading-reminder-offer").getUnclippedBoundsInRoot()
        }.getOrNull()
        return "the second Not now must answer the offer for good: " +
            "answered=${settings?.reminderAnswered} " +
            "dismissed=${settings?.reminderDismissedSurah} tile=$tile"
    }

    /** The reading is up, with the reader at Al-Fatiha's last ayah. */
    private fun waitUntilTheReadingIsUp() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The reader's own swipe, stopped the moment the offer stands. */
    private fun swipeUntilTheOfferStands() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        var drawn = theOfferStands()
        repeat(9) {
            if (!drawn) {
                compose.onAllNodesWithContentDescription(readingPage).onFirst()
                    .performTouchInput { swipeUp() }
                compose.waitForIdle()
                drawn = theOfferStands()
            }
        }
    }

    /**
     * Not now, the quiet word the offer carries. The tile is scrolled fully
     * onto the glass first: at the end of a long surah the first frame that
     * answers "displayed" can be the tile's top edge alone, with the answers
     * under the window.
     */
    private fun tapNotNow() {
        runCatching {
            compose.onNodeWithTag("reading-reminder-offer").performScrollTo()
        }
        compose.waitForIdle()
        compose.onAllNodesWithText(
            compose.activity.getString(ReadingR.string.reading_reminder_not_now),
        ).onFirst().performClick()
        compose.waitForIdle()
    }

    /**
     * The reading lands on one ayah of one surah: the browse sheet, the
     * surah's row, its own numbers, one tap.
     */
    private fun landOn(surah: Int, ayah: Int) {
        openBrowse()
        compose.onNodeWithTag("surah-row-$surah").performClick()
        waitForTag("surah-ayahs")
        compose.onNodeWithTag("surah-ayahs").performScrollToIndex(ayah - 1)
        compose.onNodeWithContentDescription("Ayah $ayah").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithTag("ayah-$surah:$ayah").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The chrome comes up on a tap on the paper; the reading below it
     * keeps its own gestures, so the tap is the same one a reader makes.
     */
    private fun openBrowse() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(readingPage).onFirst().tapThePaper()
        val browse = compose.activity.getString(R.string.action_browse)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(browse).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(browse).performClick()
        waitForTag("browse-sheet")
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Whether the tile is on the glass. */
    private fun theOfferStands(): Boolean {
        compose.waitForIdle()
        if (compose.onAllNodesWithTag("reading-reminder-offer").fetchSemanticsNodes().isEmpty()) {
            return false
        }
        return runCatching {
            compose.onNodeWithTag("reading-reminder-offer").assertIsDisplayed()
        }.isSuccess
    }
}
