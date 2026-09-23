package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The Go to ayah picker: Browse, the chip in the tab row, then a number. The
 * reader's own surah is already chosen so a jump within it is one tap, and
 * another surah is one more tap through the same list Browse draws. The test
 * follows the whole path and lands on 2:12, which is a place in a long surah
 * the reader could not have reached without scrolling to it.
 *
 * The library is prepared before the activity starts, the order a reader
 * creates it in, and the jump is the only thing the test touches.
 */
@RunWith(AndroidJUnit4::class)
class GoToAyahTest {

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
    fun thePickerJumpsToAnyAyahOfAnySurah() {
        openBrowse()
        // The chip stands in the tab row: one tap from anywhere in Browse, not
        // a row over the surah list the reader has to be on first.
        compose.onNodeWithTag("go-to-ayah").performClick()
        waitForTag("go-to-ayahs")

        // The picker opens on the surah the reader is in; the ayah grid is
        // its second step, not a first choice they have to make. The selector
        // is the one node with that name: the reading behind the sheet names
        // the surah too.
        compose.onNodeWithTag("go-to-surah").assertTextContains("Al-Fatihah")

        // Another surah is one tap through the same list Browse draws.
        compose.onNodeWithTag("go-to-surah").performClick()
        waitForTag("go-to-surahs")
        compose.onNodeWithText("Al-Baqarah").performClick()
        waitForTag("go-to-ayahs")

        // Ayah 12 of Al-Baqarah, and the reading lands on 2:12.
        compose.onNodeWithTag("go-to-ayahs").performScrollToIndex(11)
        compose.onNodeWithContentDescription("Ayah 12").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("2:12", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openBrowse() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
        // The chrome comes up on a tap on the paper; the reading below it
        // keeps its own gestures, so the tap is the same one a reader makes.
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
            .performTouchInput { click(center) }
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
}
