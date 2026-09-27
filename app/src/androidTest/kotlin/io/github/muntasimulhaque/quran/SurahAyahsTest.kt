package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
 * A surah row is the door to its own ayahs. Browse holds one list of surahs,
 * and a tap on one opens its numbers with the reader's own place marked and
 * in view, so a jump inside a long surah is a tap on a number instead of a
 * scroll (owner decision, D-101). The test opens Al-Baqarah, jumps to 2:12,
 * and comes back to the list through the grid's own back mark.
 *
 * The library is prepared before the activity starts, the order a reader
 * creates it in, and the jump is the only thing the test touches.
 */
@RunWith(AndroidJUnit4::class)
class SurahAyahsTest {

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
    fun theSurahRowOpensItsAyahsAndAJumpLands() {
        openBrowse()
        // The Surahs list is the only list of surahs: a tap on one opens its
        // own numbers.
        compose.onNodeWithTag("surah-row-2").performClick()
        waitForTag("surah-ayahs")

        // Ayah 12 of Al-Baqarah, and the reading lands on 2:12.
        compose.onNodeWithTag("surah-ayahs").performScrollToIndex(11)
        compose.onNodeWithContentDescription("Ayah 12").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("2:12", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun theBackMarkReturnsToTheSurahList() {
        openBrowse()
        compose.onNodeWithTag("surah-row-2").performClick()
        waitForTag("surah-ayahs")
        compose.onNodeWithTag("surah-ayah-back").performClick()
        waitForTag("browse-surahs")
        compose.onNodeWithTag("surah-row-2").assertExists()
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
        waitForTag("browse-sheet")
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
