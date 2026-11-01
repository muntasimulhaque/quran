package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SearchHistory
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.UiLanguage
import kotlinx.coroutines.runBlocking
import org.junit.After
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
 * its query back in the field, and that Clear empties the list on the phone
 * as well as on the glass (owner decision, this session).
 *
 * A chip can also go on its own: a long press arms it, its X appears inside
 * the capsule, and the next tap takes the one word from the phone while the
 * others stay, which is the per query door Clear never was (owner decision,
 * this session).
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
    fun aLongPressRemovesOnlyThatQuery() {
        SearchHistory(context).record("absolute")
        openSearch()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Recent searches").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("mercy").performTouchInput { longClick() }
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("mercy").fetchSemanticsNodes().isNotEmpty() &&
                rule.onAllNodesWithTag("recent-armed").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("mercy").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("mercy").fetchSemanticsNodes().isEmpty()
        }
        assertTrue("the chip's own word is gone", rule.onAllNodesWithText("mercy").fetchSemanticsNodes().isEmpty())
        assertTrue("the other word stays", rule.onAllNodesWithText("absolute").fetchSemanticsNodes().isNotEmpty())
        assertTrue("the phone keeps only the other word", SearchHistory(context).read() == listOf("absolute"))
    }

    @Test
    fun clearEmptiesTheListOnThePhone() {
        openSearch()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Recent searches").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Clear").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Recent searches").fetchSemanticsNodes().isEmpty()
        }
        assertTrue(SearchHistory(context).read().isEmpty())
    }
}
