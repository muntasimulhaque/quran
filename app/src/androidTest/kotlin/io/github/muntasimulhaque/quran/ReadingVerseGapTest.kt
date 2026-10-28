package io.github.muntasimulhaque.quran

import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The room between two verses, on the glass.
 *
 * An ayah block keeps two rhythms, and they are two because the block is two
 * things. Carrying a translation or a word list it is a card, and two cards
 * keep the room the app's rows keep, [Space.Row] on each side, because the
 * eye has to leave one stack of text and find the head of the next. Arabic
 * only it is a line of the mushaf, and the verses of a page ride the line's
 * own rhythm: [Space.Tight] on each side, so a verse stands about a quarter
 * of a line from the next and the face's own medallion closes it.
 *
 * The Arabic-only page used to keep the card's 44 dp, standing a verse 1.8 of
 * a line from the next, which read as a table of rows and cost the reader
 * four verses in ten. The two rules are measured here, against the tokens
 * that draw them and, for the Arabic-only page, against the reader's own
 * line, so a later change to either rhythm fails a leg instead of reaching a
 * page.
 *
 * Each test seeds the reader's choices and then launches the app, rather than
 * launching it ahead under a rule: the reading reads its settings once while
 * its library opens and owns them after that, so an activity that was
 * launched before the test could say otherwise would measure the previous
 * test's page.
 */
@RunWith(AndroidJUnit4::class)
class ReadingVerseGapTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * The page that draws the Quran alone rides the line's own rhythm: the two
     * blocks stand [Space.Tight] apart on each side, and the next verse's line
     * begins within a line and a half of this one's.
     */
    @Test
    fun theArabicOnlyPageRidesTheLinesOwnRhythm() = onReading(showTranslation = false) {
        val verses = verseBlocks()
        val gap = verses[1].top.value - verses[0].bottom.value
        assertEquals(
            "two Arabic-only verses must stand ${2 * Space.Tight.value} dp apart, the block's " +
                "own Space.Tight on each side",
            2 * Space.Tight.value,
            gap,
            1f,
        )
        val line = with(compose.density) {
            TextSize.lineSp(TypeRole.Arabic, TextSize.DEFAULT).sp.toDp().value
        }
        val step = verses[1].top.value - verses[0].top.value
        assertTrue(
            "two verses stand $step dp apart on a $line dp line: the Arabic-only page rides the " +
                "line's own rhythm, so a verse is a quarter of a line from the next and not most " +
                "of a blank one",
            step <= line * 1.5f,
        )
    }

    /**
     * A page that was asked for a translation, or that carries the word list,
     * keeps the card's room: this is the room the reading has always kept
     * between two blocks, and the Arabic-only page is the only one that
     * changed.
     */
    @Test
    fun aPageThatCarriesMoreKeepsTheCardsRoom() = onReading(showTranslation = true) {
        val verses = verseBlocks()
        val gap = verses[1].top.value - verses[0].bottom.value
        assertEquals(
            "two blocks that carry more than the Arabic must stand ${2 * Space.Row.value} dp " +
                "apart, the card's own Space.Row on each side",
            2 * Space.Row.value,
            gap,
            1f,
        )
    }

    /**
     * Seeds the reader's choices, opens the reading, and hands it to [check]
     * with its first two verses measured.
     */
    private fun onReading(showTranslation: Boolean, check: () -> Unit) {
        seed(showTranslation)
        val reading = ActivityScenario.launch(MainActivity::class.java)
        try {
            val page = context.getString(ReadingR.string.reading_page_description)
            compose.waitUntil(timeoutMillis = 180_000) {
                compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty() &&
                    ayahStops().fetchSemanticsNodes().size >= 2
            }
            check()
        } finally {
            reading.close()
        }
    }

    /** A reader with the shipped sizes, on Al-Fatiha's first verse. */
    private fun seed(showTranslation: Boolean) {
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setAyah(1)
                setTheme(AppTheme.Light)
                setShowTranslation(showTranslation)
                setWordByWord(false)
                setTajweed(true)
                for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
            }
        }
    }

    /** The first two ayah blocks, top to bottom, each with its own room. */
    private fun verseBlocks(): List<DpRect> {
        val stops = ayahStops()
        val count = stops.fetchSemanticsNodes().size
        val blocks = (0 until count)
            .map { stops[it].getUnclippedBoundsInRoot() }
            .sortedBy { it.top.value }
        require(blocks.size >= 2) { "two verses must be on the glass, found ${blocks.size}" }
        return blocks.take(2)
    }

    /** Every ayah block on the page: the nodes that offer the ayah's actions. */
    private fun ayahStops() = compose.onAllNodes(
        hasCustomAction(context.getString(ReadingR.string.reading_ayah_actions)),
        useUnmergedTree = true,
    )

    /** A node that carries one named custom accessibility action. */
    private fun hasCustomAction(label: String) =
        SemanticsMatcher("a node with the $label action") { node ->
            node.config.getOrNull(SemanticsActions.CustomActions)
                ?.any { action: CustomAccessibilityAction -> action.label == label } == true
        }
}
