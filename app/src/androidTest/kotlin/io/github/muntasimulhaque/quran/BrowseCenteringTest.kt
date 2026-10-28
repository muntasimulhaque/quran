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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * The Surahs and Juz tabs open on the reader's own division of the Book, put
 * in the middle of the list the way the ayah grid puts their ayah in the
 * middle of the numbers. The reader is standing at Maryam 19:1, the Quran's
 * own number 2251, which sits in Juz 16: neither list may open at its first
 * row, and the reader's row must sit at the viewport's center rather than at
 * its top edge. A division too near either end of the Book cannot be centered
 * and clamps instead; the top of the Book is pinned by BrowseNumbersTest,
 * which opens at Al-Fatiha and finds it at the top.
 *
 * The same answer is given every time the sheet is raised, because Browse's
 * place is the reading's place: a reader who jumps through the list and then
 * presses Back must find the surah they are reading in the middle, not the
 * row they tapped. The last two legs pin that by measuring the distance of
 * both rows from the middle, so the row they left is the farther one, which
 * holds on any device without depending on a pixel.
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

    @Test
    fun theSurahListCentersAgainWhenTheSheetIsRaisedAgain() {
        openBrowse()
        // Al-Kahf is one row above the reader's own surah, so it is on the
        // glass and the tap is the one a reader makes. The jump moves the
        // reading to Al-Kahf, which is the point: the row they left and the
        // surah they are in are now two different rows.
        compose.onNodeWithTag("surah-row-18").performClick()
        waitForTag("surah-ayahs")
        compose.onNodeWithTag("surah-ayahs").performScrollToIndex(0)
        compose.onNodeWithContentDescription("Ayah 1").performClick()
        waitForSheetGone()

        // Back returns the reader to the level they came from, the grid, and
        // the list is behind it. Closing the grid is the moment the list is
        // raised again, and it must center on Al-Kahf and not on the row the
        // reader tapped.
        back()
        waitForTag("surah-ayahs")
        compose.onNodeWithTag("surah-ayah-back").performClick()
        waitForTag("browse-surahs")
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("surah-row-18", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertNearerTheMiddle(
            listTag = "browse-surahs",
            readerRow = "surah-row-18",
            leftRow = "surah-row-19",
            message = "the reading's surah must take the middle again",
        )
    }

    @Test
    fun theJuzListCentersAgainWhenTheSheetIsRaisedAgain() {
        openBrowse()
        compose.onNodeWithText("Juz").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("juz-row-15", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Juz 15 is one row above the reader's own juz, and its row opens the
        // juz, so the reading lands in a different division of the Book.
        compose.onNodeWithTag("juz-row-15").performClick()
        waitForSheetGone()

        back()
        waitForTag("browse-juz")
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("juz-row-15", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertNearerTheMiddle(
            listTag = "browse-juz",
            readerRow = "juz-row-15",
            leftRow = "juz-row-16",
            message = "the reading's juz must take the middle again",
        )
    }

    /**
     * The reader's row is nearer the middle than the row they left. Measuring
     * both shows the sheet centered again without depending on how many
     * pixels the centering itself lands from the center, which is the one
     * thing a compose test on a sheet cannot hold still.
     */
    private fun assertNearerTheMiddle(
        listTag: String,
        readerRow: String,
        leftRow: String,
        message: String,
    ) {
        val list = compose.onNodeWithTag(listTag, useUnmergedTree = true).fetchSemanticsNode()
        val middle = (list.boundsInRoot.top + list.boundsInRoot.bottom) / 2f
        fun away(tag: String): Float {
            val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            return abs(middle - (bounds.top + bounds.bottom) / 2f)
        }
        val reader = away(readerRow)
        val left = away(leftRow)
        assertTrue(
            "$message: $readerRow is $reader from the middle, $leftRow is $left",
            reader < left,
        )
    }

    private fun openBrowse() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(readingPage).onFirst()
            .tapThePaper()
        val browse = compose.activity.getString(R.string.action_browse)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(browse).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(browse).performClick()
        waitForTag("browse-sheet")
        compose.waitForIdle()
    }

    /** The phone's own Back, the way a reader leaves the reading. */
    private fun back() {
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForSheetGone() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("browse-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }
}
