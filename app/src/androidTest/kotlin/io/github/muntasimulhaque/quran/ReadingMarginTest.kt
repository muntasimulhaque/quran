package io.github.muntasimulhaque.quran

import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.ui.kit.pageMargin
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Where the reading's words stop, on the glass.
 *
 * The app has one page, and its margin is arithmetic on the window: a glass no
 * wider than [Space.Measure] takes [Space.Gutter] on each side, and a wider
 * glass keeps a 600 dp page in the middle. A verse, its translation, the word
 * list under it, a search result, a settings row, and a footnote all stand on
 * that page, on a phone, a seven inch tablet, and a ten inch screen alike.
 *
 * Two lines are measured, because the reading draws two: the wash of the ayah
 * reaches [Space.WashInset] past its words, so the block starts the page's
 * margin less that inset from each edge, and the words inside it start on the
 * page's line. Both numbers are read against the page the verse is drawn on,
 * so this holds on every form factor without knowing which one it runs on:
 * the ten inch screen's own run proves the page held at 600 dp and the glass
 * became margin, and the phone's proves a narrow glass still runs edge to edge
 * (owner decision, this session).
 */
@RunWith(AndroidJUnit4::class)
class ReadingMarginTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun aVerseStandsOnTheAppsOneEdge() {
        seed()
        val reading = ActivityScenario.launch(MainActivity::class.java)
        try {
            val page = context.getString(ReadingR.string.reading_page_description)
            compose.waitUntil(timeoutMillis = 180_000) {
                compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty() &&
                    ayahStops().fetchSemanticsNodes().size >= 1
            }
            val sheet = pageBounds(page)
            val verse = verseBlock()
            val pageWidth = (sheet.right.value - sheet.left.value).dp
            val side = (pageMargin(pageWidth) - Space.WashInset).value
            assertEquals(
                "the wash must stand $side dp from the left edge of the page",
                side,
                verse.left.value,
                1f,
            )
            assertEquals(
                "the wash must stand $side dp from the right edge of the page",
                sheet.right.value - side,
                verse.right.value,
                1f,
            )
            assertEquals(
                "the words' own line is the page's edge, so the wash is that edge less " +
                    "its inset on each side",
                (sheet.right.value - sheet.left.value) - 2 * side,
                verse.right.value - verse.left.value,
                1f,
            )
        } finally {
            reading.close()
        }
    }

    /** The reading's list, which is the whole page. */
    private fun pageBounds(page: String): DpRect =
        compose.onAllNodesWithContentDescription(page)[0].getUnclippedBoundsInRoot()

    /** The first ayah block on the glass. */
    private fun verseBlock(): DpRect =
        ayahStops()[0].getUnclippedBoundsInRoot()

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

    /** A reader with the shipped sizes, on Al-Fatiha's first verse. */
    private fun seed() {
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setAyah(1)
                setTheme(AppTheme.Light)
                setShowTranslation(true)
                setWordByWord(false)
                setTajweed(true)
                for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
            }
        }
    }
}
