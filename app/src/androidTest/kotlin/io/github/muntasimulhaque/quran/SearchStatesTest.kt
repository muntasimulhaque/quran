package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two states the search sheet used to leave unspoken.
 *
 * **One source off is named.** "6 of 7" is a count, and the reader whose one
 * missing source is the reason a query found nothing could not read which one
 * from the door. One source off now says "Everything but Quran text", so the
 * closed door can explain the empty result (owner decision, this session).
 *
 * **No matches teaches the forms.** "No matches." left most of the sheet blank
 * at the moment the reader needed to know that a shorter word or a reference
 * is a query. The hint under the count is the empty sheet's teaching at the
 * one moment it is an answer (owner decision, this session).
 */
@RunWith(AndroidJUnit4::class)
class SearchStatesTest {

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

    private fun text(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun theDoorNamesTheOneSourceThatIsOff() {
        prepare()
        openSearch().use {
            rule.onNodeWithText("Search in").performClick()
            rule.waitUntil(timeoutMillis = 15_000) {
                rule.onAllNodesWithText("Quran text").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Quran text").performClick()
            rule.waitUntil(timeoutMillis = 15_000) {
                rule.onAllNodesWithText(
                    context.getString(SearchR.string.search_filter_all_but, "Quran text"),
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Quran text").performClick()
            rule.waitUntil(timeoutMillis = 15_000) {
                rule.onAllNodesWithText(text(SearchR.string.search_filter_all))
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun noMatchesTeachesTheForms() {
        prepare()
        openSearch().use {
            rule.onNode(hasSetTextAction()).performTextInput("zzzz")
            rule.waitUntil(timeoutMillis = 60_000) {
                rule.onAllNodesWithText(text(SearchR.string.search_status_no_matches))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText(text(SearchR.string.search_status_no_match_hint))
                .assertExists()
        }
    }
}
