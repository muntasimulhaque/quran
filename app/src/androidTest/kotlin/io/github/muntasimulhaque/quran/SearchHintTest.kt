package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.feature.search.R as SearchR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The search sheet's own note before a query.
 *
 * The sheet used to be a field, a filter door, and a blank page, so the three
 * things a reader cannot guess about this search (a word in their own language
 * works, an Arabic word works without its marks, and a reference is a query)
 * were nowhere on it. The note says them in two sentences and leaves the
 * moment the field holds a word, because it teaches and does not stand beside
 * an answer (owner decision, this session).
 *
 * Only the title and the second sentence are asserted: the first sentence
 * follows the packs on the device, and a test that pinned one of its two forms
 * would pass or fail on the library rather than on the app.
 */
@RunWith(AndroidJUnit4::class)
class SearchHintTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** The library a search opens on, the way a reader arrives at it. */
    private fun prepare() {
        val store = PackStore(context)
        for (id in listOf("translation-saheeh-en", "tafsir-ibn-kathir-en", "words-en")) {
            store.install(id)
        }
        val settings = SettingsStore(context)
        runBlocking {
            settings.setUiLanguage(UiLanguage.English.tag)
            settings.setAyah(1)
            settings.setTranslationPacks(setOf("translation-saheeh-en"))
            settings.setTafsirPacks(setOf("tafsir-ibn-kathir-en"))
            settings.setShowTranslation(true)
            settings.setWordByWord(false)
            settings.setLongPressHintShown()
        }
        Thread.sleep(500)
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

    private fun noteIsShown(): Boolean =
        rule.onAllNodesWithText(text(SearchR.string.search_empty_title))
            .fetchSemanticsNodes()
            .isNotEmpty()

    private fun text(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    /**
     * The note stands on the empty sheet, goes with the first character, and
     * comes back when the field is emptied again, so the reader who clears a
     * query meets the same help they met on arrival.
     */
    @Test
    fun theNoteTeachesTheFieldAndLeavesWhenTheQueryBegins() {
        prepare()
        openSearch().use {
            rule.waitUntil(timeoutMillis = 60_000) { noteIsShown() }
            assertTrue(
                "the note must say that a reference is a query",
                rule.onAllNodesWithText(text(SearchR.string.search_empty_body), substring = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                    rule.onAllNodesWithText(
                        text(SearchR.string.search_empty_body_quran),
                        substring = true,
                    ).fetchSemanticsNodes().isNotEmpty(),
            )

            rule.onNode(hasSetTextAction()).performTextInput("k")
            rule.waitUntil(timeoutMillis = 15_000) { !noteIsShown() }

            rule.onNodeWithContentDescription(text(SearchR.string.search_clear)).performClick()
            rule.waitUntil(timeoutMillis = 15_000) { noteIsShown() }
        }
    }
}
