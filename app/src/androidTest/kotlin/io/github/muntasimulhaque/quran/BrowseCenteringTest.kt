package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlin.math.abs

/**
 * The Surahs and Juz tabs open on the reader's own division of the Book,
 * put in the middle of the list the way the ayah grid puts their ayah in
 * the middle of the numbers. The reader is standing at Maryam 19:1, the
 * Quran's own number 2251, which sits in Juz 16: neither list may open at
 * its first row, and the reader's row must sit at the viewport's center
 * rather than at its top edge. A division too near either end of the Book
 * cannot be centered and clamps instead; the top of the Book is pinned by
 * BrowseNumbersTest, which opens at Al-Fatiha and finds it at the top.
 */
@RunWith(AndroidJUnit4::class)
class BrowseCenteringTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Maryam 19:1 is the Quran's own number 2251. */
    private val place = 2251

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(place)
                    setMode(ReadingMode.Study)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theSurahListOpensOnTheReadersSurahInTheMiddle() {
        openBrowse()
        // The centering is two scrolls, so the row exists at the top for a
        // moment before it is moved to the middle: wait for the place, not
        // only for the row.
        compose.waitUntil(timeoutMillis = 15_000) {
            val rows = compose.onAllNodesWithTag("surah-row-19", useUnmergedTree = true)
                .fetchSemanticsNodes()
            if (rows.isEmpty()) return@waitUntil false
            val list = compose.onNodeWithTag("browse-surahs", useUnmergedTree = true)
                .fetchSemanticsNode()
            val listCenter = (list.boundsInRoot.top + list.boundsInRoot.bottom) / 2f
            val rowCenter = (rows.first().boundsInRoot.top + rows.first().boundsInRoot.bottom) / 2f
            abs(listCenter - rowCenter) < rows.first().boundsInRoot.height
        }
        val list = compose.onNodeWithTag("browse-surahs", useUnmergedTree = true)
            .fetchSemanticsNode()
        val row = compose.onNodeWithTag("surah-row-19", useUnmergedTree = true)
            .fetchSemanticsNode()
        val listCenter = (list.boundsInRoot.top + list.boundsInRoot.bottom) / 2f
        val rowCenter = (row.boundsInRoot.top + row.boundsInRoot.bottom) / 2f
        assertTrue(
            "Maryam must sit at the list's center, not its top edge: " +
                "list center $listCenter, row center $rowCenter",
            abs(listCenter - rowCenter) < row.boundsInRoot.height,
        )
        assertTrue(
            "the list must not open at Al-Fatiha",
            compose.onAllNodesWithTag("surah-row-1", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun theJuzListOpensOnTheReadersJuzInTheMiddle() {
        openBrowse()
        // Maryam 19:1 sits in Juz 16 (which begins at Al-Kahf 18:75), so the
        // Juz tab opens the same way the Surahs tab does (owner decision).
        compose.onNodeWithText("Juz").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            val rows = compose.onAllNodesWithTag("juz-row-16", useUnmergedTree = true)
                .fetchSemanticsNodes()
            if (rows.isEmpty()) return@waitUntil false
            val list = compose.onNodeWithTag("browse-juz", useUnmergedTree = true)
                .fetchSemanticsNode()
            val listCenter = (list.boundsInRoot.top + list.boundsInRoot.bottom) / 2f
            val rowCenter = (rows.first().boundsInRoot.top + rows.first().boundsInRoot.bottom) / 2f
            abs(listCenter - rowCenter) < rows.first().boundsInRoot.height
        }
        val list = compose.onNodeWithTag("browse-juz", useUnmergedTree = true)
            .fetchSemanticsNode()
        val row = compose.onNodeWithTag("juz-row-16", useUnmergedTree = true)
            .fetchSemanticsNode()
        val listCenter = (list.boundsInRoot.top + list.boundsInRoot.bottom) / 2f
        val rowCenter = (row.boundsInRoot.top + row.boundsInRoot.bottom) / 2f
        assertTrue(
            "Juz 16 must sit at the list's center, not its top edge: " +
                "list center $listCenter, row center $rowCenter",
            abs(listCenter - rowCenter) < row.boundsInRoot.height,
        )
        assertTrue(
            "the list must not open at the first juz",
            compose.onAllNodesWithTag("juz-row-1", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun openBrowse() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
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
}
