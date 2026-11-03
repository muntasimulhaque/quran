package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SearchHistory
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.UiLanguage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The empty search sheet's recent queries.
 *
 * A search usually begins with a word the reader has already used, and the
 * empty sheet now hands the last few back as chips. The list is written when
 * a result is opened and not when a key is typed, which the pure tests in
 * `data` pin; this test pins that the sheet draws the list, that a chip puts
 * its query back in the field, and that a chip's own X takes that word from
 * the phone while the others stay (owner decision, this session).
 *
 * The X is the only removal: a chip goes on its own, with its X at the
 * trailing end of the capsule, so a reader who wants to forget a word does
 * not have to discover a long press (owner decision, this session).
 */
@RunWith(AndroidJUnit4::class)
class RecentSearchesTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun prepare() {
        val store = PackStore(context)
        for (id in listOf("translation-saheeh-en", "tafsir-ibn-kathir-en", "words-en")) {
            store.install(id)
        }
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage(UiLanguage.English.tag)
                setAyah(1)
                setTranslationPacks(setOf("translation-saheeh-en"))
                setTafsirPacks(setOf("tafsir-ibn-kathir-en"))
                setShowTranslation(true)
                setWordByWord(false)
            }
        }
        SearchHistory(context).apply {
            clear()
            record("mercy")
        }
        Thread.sleep(300)
    }

    @After
    fun cleanUp() {
        SearchHistory(context).clear()
    }

    private fun openSearch(): ActivityScenario<MainActivity> {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_SEARCH)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        rule.waitUntil(timeoutMillis = 60_000) {
            rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        return scenario
    }

    @Test
    fun theQueriesTheReaderUsedComeBackAsChips() {
        openSearch()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Recent searches").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("mercy").assertExists()
        // A chip is a query, so tapping it puts its word back in the field.
        rule.onNodeWithText("mercy").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodes(hasSetTextAction() and hasText("mercy"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun theVisibleXRemovesOnlyThatQuery() {
        SearchHistory(context).record("absolute")
        openSearch()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Recent searches").fetchSemanticsNodes().isNotEmpty()
        }
        // The X is a real control at rest, named for the word it removes.
        rule.onNodeWithContentDescription("Remove mercy").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("mercy").fetchSemanticsNodes().isEmpty()
        }
        assertTrue("the chip's own word is gone", rule.onAllNodesWithText("mercy").fetchSemanticsNodes().isEmpty())
        assertTrue("the other word stays", rule.onAllNodesWithText("absolute").fetchSemanticsNodes().isNotEmpty())
        assertTrue("the phone keeps only the other word", SearchHistory(context).read() == listOf("absolute"))
    }

    /**
     * The chip is the word's own width, not the sheet's. Two short queries
     * stand on one line because each capsule ends at its own X; a chip that
     * filled the row put every query on a line of its own, and the list read
     * as a column of bars (owner report, this session).
     */
    @Test
    fun aChipTakesItsWordAndNotTheRow() {
        SearchHistory(context).record("absolute")
        openSearch()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Recent searches").fetchSemanticsNodes().isNotEmpty()
        }
        val chips = rule.onAllNodesWithTag("recent-chip").fetchSemanticsNodes()
        assertEquals("two recents draw two chips", 2, chips.size)
        val first = chips[0].boundsInRoot
        val second = chips[1].boundsInRoot
        assertEquals("the two chips stand on one line", first.top, second.top, 1f)
        assertTrue(
            "the second chip stands beside the first, not under it",
            second.left >= first.right - 1f,
        )
    }

    /**
     * The caption belongs to its chips, not to the filter door above.
     *
     * The door's 48 dp target already spends its own air below the words, and
     * the block used to add a Section on top of that, so the caption stood
     * about twice as far from the door as the door is from the field. The
     * caption now opens at Space.Line, one row break under the door (owner
     * decision, this session).
     */
    @Test
    fun theCaptionStandsWithItsChips() {
        openSearch()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Recent searches").fetchSemanticsNodes().isNotEmpty()
        }
        val door = rule.onNodeWithText("Search in", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val caption = rule.onNodeWithText("Recent searches", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        // The door's line ends 8 dp above its content, the 48 dp target adds
        // 6 under it, the row adds 4, and the caption opens with Space.Line:
        // 26 dp. The text's own line box adds about a dp to the drawn
        // bounds, so the tolerance is the line's, not the layout's. The block
        // used to open with a Section on top of that air.
        assertEquals(
            "the caption sits one row break under the filter door",
            26f,
            (caption.top - door.bottom).value,
            2f,
        )
    }
}
