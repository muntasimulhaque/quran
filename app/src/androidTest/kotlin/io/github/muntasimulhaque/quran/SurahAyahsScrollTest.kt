package io.github.muntasimulhaque.quran

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.LastReadStore
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.study.R as StudyR
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
 * The grid opens on the place the reader is being shown, in the middle of the
 * viewport, not at the top of the surah. Landing it at the top edge answers
 * "go to" with a screenful of the surah's first ayahs: the one number the
 * reader came for sits in the same corner as every row above it, so the place
 * has to be centered to be read as the place (owner report, D-097).
 *
 * The place itself comes from two sources, and both are pinned here: the
 * reader's own ayah when the surah they open is the one they are in, and the
 * newest place they left in that surah when it is not (owner report, D-101).
 * The reader is standing at Al-Baqarah 2:84 and has a history place at
 * Al-Fatihah 1:5.
 */
@RunWith(AndroidJUnit4::class)
class SurahAyahsScrollTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Al-Fatihah is 7 ayahs, so 2:84 is the Quran's own number 91. */
    private val place = 7 + 84

    /** Al-Fatihah 1:5 is the Quran's own number 5. */
    private val historyPlace = 5

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(place)
                    setMode(ReadingMode.Study)
                }
                LastReadStore(context).apply {
                    record(historyPlace, ReadingMode.Study)
                    close()
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theReadersAyahOpensInTheMiddleOfTheGrid() {
        openBrowse()
        compose.onNodeWithTag("surah-row-2").performClick()
        waitForTag("surah-ayahs")

        // The place is on screen, and the surah's first ayah is not: the
        // reader's row and the top of the surah are different places.
        waitForAyah(84)
        compose.onNodeWithContentDescription("Ayah 84").assertIsDisplayed()
        val firstAyah = compose.onAllNodesWithContentDescription("Ayah 1", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue(
            "the grid must not open at the top of a 286 ayah surah",
            firstAyah.isEmpty(),
        )

        // And it is centered, not pinned to the viewport's top edge: a place
        // flush against the edge is the one thing the fix is about, so the
        // test measures the row against the grid it sits in.
        val grid = compose.onNodeWithTag("surah-ayahs", useUnmergedTree = true)
            .fetchSemanticsNode()
        val cell = ayahCell(84)
        val gridCenter = (grid.boundsInRoot.top + grid.boundsInRoot.bottom) / 2f
        val cellCenter = (cell.boundsInRoot.top + cell.boundsInRoot.bottom) / 2f
        assertTrue(
            "the reader's ayah must sit at the grid's center, not its top edge: " +
                "grid center $gridCenter, cell center $cellCenter",
            abs(gridCenter - cellCenter) < cell.boundsInRoot.height,
        )
    }

    @Test
    fun aSurahTheReaderIsNotInMarksThePlaceTheyLeft() {
        openBrowse()
        compose.onNodeWithTag("surah-row-1").performClick()
        waitForTag("surah-ayahs")
        // Al-Fatihah's own place is the history entry, not the current ayah:
        // the reader is standing in Al-Baqarah, so the mark has to come from
        // the history and not from where they are (owner report, D-101).
        waitForAyah(5)
        compose.onNodeWithContentDescription("Ayah 5", useUnmergedTree = true)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Current",
                ),
            )
    }

    private fun ayahCell(number: Int): androidx.compose.ui.semantics.SemanticsNode {
        val nodes = compose.onAllNodesWithContentDescription("Ayah $number", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Ayah $number must be in the grid", nodes.isNotEmpty())
        return nodes.first()
    }

    private fun waitForAyah(number: Int) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription("Ayah $number", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openBrowse() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
            .tapThePaper()
        val browse = compose.activity.getString(R.string.action_browse)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(browse).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(browse).performClick()
        waitForTag("browse-sheet")
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
