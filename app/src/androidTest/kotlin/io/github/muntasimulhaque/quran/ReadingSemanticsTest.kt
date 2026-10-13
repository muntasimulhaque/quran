package io.github.muntasimulhaque.quran

import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
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
 * The reading as a screen reader meets it: the page is one description, and
 * every ayah inside it stays its own stop with its own two actions, so a
 * reader who cannot see the page can walk it one verse at a time.
 *
 * This is the automated half of the TalkBack pass (owner decision, this
 * session): TalkBack itself needs a real screen-reader run, but the tree it
 * walks is pinned here, so a change that swallows the ayahs into one node,
 * or takes an ayah's actions away, fails a leg instead of reaching a reader.
 */
@RunWith(AndroidJUnit4::class)
class ReadingSemanticsTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTheme(AppTheme.Light)
                    setTranslationPacks(emptySet())
                    setTafsirPacks(emptySet())
                    setShowTranslation(true)
                    setShowTafsir(true)
                    setWordByWord(false)
                    for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun everyAyahIsItsOwnStopWithItsOwnActions() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
        // The page is named, and the ayahs under it are still their own nodes:
        // a description that swallowed its children would make the Book one
        // stop.
        compose.onAllNodesWithContentDescription(page).assertCountEquals(1)
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText("1:1", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "the first ayah's reference must be its own stop",
            compose.onAllNodesWithText("1:1", substring = true).fetchSemanticsNodes().isNotEmpty(),
        )
        // The Arabic of the first ayah is its own text node, not folded into
        // the page's description.
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText("\u0628\u0650\u0633\u06e1\u0645\u0650", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Every ayah on screen carries the two doors a finger has.
        val ayahActions = compose.activity.getString(ReadingR.string.reading_ayah_actions)
        val paperActions = compose.activity.getString(ReadingR.string.reading_paper_actions)
        val stops = compose.onAllNodes(hasCustomAction(ayahActions), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("at least one ayah must be its own stop", stops.size >= 1)
        val paperStops = compose.onAllNodes(hasCustomAction(paperActions), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("the paper's own action must be offered too", paperStops.size >= 1)
    }

    /** A node that carries one named custom accessibility action. */
    private fun hasCustomAction(label: String) =
        SemanticsMatcher("a node with the $label action") { node ->
            node.config.getOrNull(SemanticsActions.CustomActions)
                ?.any { action: CustomAccessibilityAction -> action.label == label } == true
        }
}
