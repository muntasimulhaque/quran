package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
 * The search field's own clear mark.
 *
 * The reader who has run one query and wants another used to reach to the end
 * of the line and backspace the whole word, or discover that the field's
 * outside Close quietly cleared the text instead of closing the sheet. The
 * mark ends both: it appears only while there is a query, empties the field,
 * and leaves the reader focused and ready for the next word, while Close now
 * always closes (owner decision, this session).
 */
@RunWith(AndroidJUnit4::class)
class SearchClearTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /**
     * The library a search opens on: the reader's language chosen and the
     * packs the app installs for it, so the sheet is the one a reader meets.
     */
    private fun prepare() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
            settings.setShowTafsir(true)
            settings.setWordByWord(false)
            settings.setLongPressHintShown()
        }
        Thread.sleep(500)
    }

    /** Open the search sheet over the reading, the way the shortcut does. */
    private fun openSearch(): ActivityScenario<MainActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_SEARCH)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        rule.waitUntil(timeoutMillis = 60_000) {
            rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        return scenario
    }

    private fun clearDescription(): String =
        InstrumentationRegistry.getInstrumentation().targetContext
            .getString(SearchR.string.search_clear)

    /**
     * The mark appears with a query, empties it, and leaves the field focused
     * with the results cleared, the reader ready for another word.
     */
    @Test
    fun theClearMarkEmptiesTheFieldAndKeepsItReady() {
        prepare()
        openSearch().use {
            rule.onNode(hasSetTextAction()).performTextInput("kahf")
            rule.waitUntil(timeoutMillis = 60_000) {
                rule.onAllNodesWithText("Al-Kahf", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(clearDescription()).performClick()
            rule.waitUntil(timeoutMillis = 15_000) {
                rule.onAllNodesWithText("Al-Kahf", substring = true)
                    .fetchSemanticsNodes().isEmpty()
            }
            // The field is still the reader's, the query is gone, and the
            // cursor stays where it was so the next word can be typed at once.
            rule.onNode(hasSetTextAction()).assertIsDisplayed()
            rule.waitUntil(timeoutMillis = 15_000) { fieldIsEmpty() }
            assertTrue(
                "the field keeps the cursor after the clear",
                fieldFocused(),
            )
        }
    }

    /** Close ends the search whether or not a query is in the field. */
    @Test
    fun closeEndsTheSearchWithAQueryInTheField() {
        prepare()
        openSearch().use {
            rule.onNode(hasSetTextAction()).performTextInput("kahf")
            rule.waitUntil(timeoutMillis = 60_000) {
                rule.onAllNodesWithText("Al-Kahf", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val close = InstrumentationRegistry.getInstrumentation().targetContext
                .getString(SearchR.string.search_close)
            rule.onAllNodesWithText(close).onFirst().performClick()
            rule.waitUntil(timeoutMillis = 15_000) {
                rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty()
            }
        }
    }

    private fun fieldNode() = rule.onNode(hasSetTextAction()).fetchSemanticsNode()

    private fun fieldIsEmpty(): Boolean =
        fieldNode().config.getOrElseNullable(SemanticsProperties.EditableText) { null }
            ?.text.isNullOrEmpty()

    private fun fieldFocused(): Boolean =
        fieldNode().config.getOrElseNullable(SemanticsProperties.Focused) { null } == true
}
