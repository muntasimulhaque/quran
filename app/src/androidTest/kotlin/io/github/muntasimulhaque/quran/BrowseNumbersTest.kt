package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
 * Browse's surah numbers are one column of whole numbers, and the interface
 * writes them in tabular figures: every digit is one width, so a number's
 * width is its count of digits and never the particular digits it draws. The
 * column is still measured on every number the list draws, and the two rules
 * together are what keep "100" whole: it is the width of "114" and cannot
 * overflow a column sized on it. In a Bangla interface the same column writes
 * the numbers in Bangla digits, not the Latin ones.
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
        // Tabular figures give the column one width per digit, so a
        // three-digit number is as wide as the three-digit label the column
        // was measured on and cannot be the one that overflows (owner
        // decision, this session: the table's figures do the work the wider
        // column used to).
        assertEquals(
            "every three-digit number must take the column's own width",
            hundredFourteen,
            hundred,
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
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        // The chrome comes up on a tap on the paper; the reading below it
        // keeps its own gestures, so the tap is the same one a reader makes.
        compose.onAllNodesWithContentDescription(readingPage).onFirst()
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
                compose.activity.getString(ReadingR.string.reading_page_description),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val open = compose.onAllNodesWithTag("browse-sheet", useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        if (!open) openBrowse()
    }
}
