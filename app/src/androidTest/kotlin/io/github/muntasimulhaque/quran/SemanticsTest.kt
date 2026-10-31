package io.github.muntasimulhaque.quran

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
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

/**
 * The semantics a screen reader depends on, pinned where a test can read them:
 * the name of a sheet, the role of a Browse view, the name of an empty search
 * field, the preview's own name, and the note draft that survives a closed
 * sheet.
 *
 * These are not visual rules. A pane with no name, a tab that says "radio
 * button", or a field that says nothing is invisible in a screenshot and
 * obvious to a reader with TalkBack, so they get their own gate rather than a
 * sentence in a comment (owner decision, this session).
 */
@RunWith(AndroidJUnit4::class)
class SemanticsTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            val store = PackStore(context)
            store.install("translation-saheeh-en")
            store.install("tafsir-ibn-kathir-en")
            store.install("words-en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTranslationPacks(setOf("translation-saheeh-en"))
                    setTafsirPacks(emptySet())
                    setShowTranslation(true)
                    setWordByWord(false)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theSheetNamesAndBrowseViewsAreSpoken() {
        awaitReading()

        // Browse names itself, its views are tabs, and the drag mark is not a
        // dead control in the screen reader's path.
        openBrowse()
        assertPaneTitle("Browse")
        assertTrue(
            "the sheet's drag mark must not be a spoken control",
            compose.onAllNodesWithContentDescription("Sheet handle").fetchSemanticsNodes().isEmpty(),
        )
        compose.onAllNodesWithText("Juz").onFirst()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        Espresso.pressBack()
        waitForTagGone("browse-sheet")

        // Search names itself and its empty field says what it is.
        revealChrome()
        compose.onNodeWithContentDescription("Search").performClick()
        waitForTag("search-sheet")
        assertPaneTitle("Search")
        compose.onNode(hasSetTextAction())
            .assert(hasContentDescription(context.getString(io.github.muntasimulhaque.quran.feature.search.R.string.search_hint)))
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        waitForTagGone("search-sheet")

        // A switch row is two controls: the door says what it opens, the
        // switch says what it switches and carries the value under it.
        revealChrome()
        compose.onNodeWithContentDescription("Settings").performClick()
        waitForTag("settings-hub")
        compose.onNodeWithContentDescription("Choose translations").assertExists()
        compose.onNodeWithTag("switch-translation")
            .assert(hasContentDescription("Show translation. Saheeh International"))
        Espresso.pressBack()
        waitForTagGone("settings-hub")

        // The note sheet says its name, keeps an unsaved draft after it
        // closes, and the share sheet names its preview.
        openDoor()
        compose.onAllNodesWithText(context.getString(R.string.action_note)).onFirst().performClick()
        waitForTag("note-sheet")
        assertPaneTitle("Note")
        compose.onNode(hasSetTextAction()).performTextInput("A draft kept")
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        waitForTagGone("note-sheet")

        openDoor()
        compose.onAllNodesWithText(context.getString(R.string.action_note)).onFirst().performClick()
        waitForTag("note-sheet")
        compose.onNode(androidx.compose.ui.test.hasText("A draft kept")).assertExists()
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        waitForTagGone("note-sheet")

        openDoor()
        compose.onAllNodesWithText(context.getString(R.string.action_share)).onFirst().performClick()
        waitForTag("share-sheet")
        assertPaneTitle("Share ayah")
        compose.onNodeWithContentDescription(context.getString(R.string.share_preview)).assertExists()
    }

    private fun assertPaneTitle(title: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, title))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitReading() {
        compose.waitUntil(180_000) {
            compose.onAllNodesWithContentDescription("Reading").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun revealChrome() {
        repeat(3) {
            if (compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()) {
                return
            }
            compose.onNodeWithContentDescription("Reading").performClick()
            runCatching {
                compose.waitUntil(4_000) {
                    compose.onAllNodesWithContentDescription("Settings")
                        .fetchSemanticsNodes().isNotEmpty()
                }
            }
        }
    }

    private fun openBrowse() {
        revealChrome()
        compose.onNodeWithContentDescription("Browse the Quran").performClick()
        waitForTag("browse-sheet")
    }

    private fun openDoor() {
        revealChrome()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithContentDescription(doorName()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(doorName()).onFirst().performClick()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText(context.getString(R.string.action_play))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun doorName(): String =
        context.getString(ReadingR.string.reading_ayah_door, "1:1")

    private fun waitForTag(tag: String, timeout: Long = 20_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTagGone(tag: String, timeout: Long = 20_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }
}
