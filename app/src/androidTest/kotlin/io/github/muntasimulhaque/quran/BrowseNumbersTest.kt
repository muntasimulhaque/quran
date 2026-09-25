package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.study.R as StudyR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Browse's surah numbers are one column of whole numbers. Digits are not all
 * one width, which is how "100" once lost its last digit: the column was
 * sized on the width of "114", and in the interface face "100" is wider than
 * "114". The column now measures every number the list draws, so a whole
 * three-digit number is always wider on the screen than a narrower one. In a
 * Bangla interface the same column writes the numbers in Bangla digits, not
 * the Latin ones.
 *
 * The library is prepared before the activity starts, the order a reader
 * creates it in, and the numbers are the only thing the test touches.
 */
@RunWith(AndroidJUnit4::class)
class BrowseNumbersTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    private val library = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setMode(ReadingMode.Study)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun aWholeNumberIsDrawnAndSpeaksTheReadersLanguage() {
        openBrowse()
        val hundred = surahNumberWidth(99, "100")
        val hundredFourteen = surahNumberWidth(113, "114")
        assertTrue(
            "the whole 100 must be drawn, not clipped to 10 by a column sized on 114",
            hundred > hundredFourteen,
        )

        // The same column in a Bangla interface writes Bangla digits. The
        // language choice recreates the Activity, exactly as the app does,
        // and the open Browse sheet comes back with it.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        LanguagePreference(context).set("bn")
        runBlocking { SettingsStore(context).setUiLanguage("bn") }
        compose.activityRule.scenario.recreate()
        ensureBrowse()
        compose.onNodeWithTag("browse-surahs").performScrollToIndex(99)
        compose.waitForIdle()
        compose.onNodeWithText("\u09E7\u09E6\u09E6", useUnmergedTree = true).assertExists()
    }

    /** The width the column gives one surah's number, with the row on screen. */
    private fun surahNumberWidth(index: Int, label: String): Int {
        compose.onNodeWithTag("browse-surahs").performScrollToIndex(index)
        compose.waitForIdle()
        return compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().size.width
    }

    private fun openBrowse() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
        // The chrome comes up on a tap on the paper; the reading below it
        // keeps its own gestures, so the tap is the same one a reader makes.
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
            .tapThePaper()
        val browse = compose.activity.getString(R.string.action_browse)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(browse).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(browse).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("browse-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    /** The sheet is open already after a recreation, or opened the usual way. */
    private fun ensureBrowse() {
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithTag("browse-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithContentDescription(
                compose.activity.getString(StudyR.string.study_page_description),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val open = compose.onAllNodesWithTag("browse-sheet", useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        if (!open) openBrowse()
    }
}
