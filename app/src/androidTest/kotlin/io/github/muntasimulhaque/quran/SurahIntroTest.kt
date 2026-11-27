package io.github.muntasimulhaque.quran

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The surah's introduction is its own way out, and the way back.
 *
 * The introduction opens from the quiet door under the basmalah. Opened, the
 * door is gone: the introduction is the whole of what stands there, and a tap
 * anywhere on it puts it away. The tap is a standard click, so a drag that
 * starts on it belongs to the reading's list and a reader who scrolls through
 * a long introduction does not lose it to the press. The click's label is the
 * word a screen reader hears, so the one exit is spoken as Hide (owner
 * decision, 23.2).
 *
 * The close also hands the reader back the head the door stands in (owner
 * decision, 23.3). The introduction is the list's first item, so a reader
 * several screens into it has spent the list's own scroll offset; closing
 * used to leave that offset behind, and the shrinking item handed back the
 * ayah ten or fourteen rows down. This leg measures the tap, the drag, the
 * label, and the landing. The state is seeded and the activity launched
 * inside the test for the reason [ReadingVerseGapTest] states: the reading
 * reads its settings once while its library opens.
 */
@RunWith(AndroidJUnit4::class)
class SurahIntroTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val about = context.getString(ReadingR.string.reading_about_surah)
    private val hide = context.getString(ReadingR.string.reading_hide)

    @Test
    fun tappingTheIntroductionClosesItButDraggingDoesNot() {
        seed()
        val reading = ActivityScenario.launch(MainActivity::class.java)
        try {
            val page = context.getString(ReadingR.string.reading_page_description)
            compose.waitUntil(timeoutMillis = 180_000) {
                compose.onAllNodesWithContentDescription(page)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithText(about).fetchSemanticsNodes().isNotEmpty()
            }
            // The opening at the head is the place the door opens from and
            // the place the close must hand back. It is read before the
            // first open, so the landing is compared to the head itself and
            // not to where the tap happened to be. The list is asked for
            // item 0, which is the head at the content's own top; a
            // scroll-to-node would stop as soon as the door was visible and
            // leave the opening a little below the head.
            compose.onNodeWithContentDescription(page).performScrollToIndex(0)
            compose.waitForIdle()
            val head = headOffset(page)
            compose.onNodeWithText(about).performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag("surah-about").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(about).assertDoesNotExist()
            assertEquals(
                "the introduction's one exit must be spoken as Hide",
                hide,
                compose.onNodeWithTag("surah-about").fetchSemanticsNode()
                    .config.getOrNull(SemanticsActions.OnClick)?.label,
            )

            // A slow drag of a known length, released after a pause so it
            // carries no velocity: the list scrolls and the press is
            // cancelled, so the introduction stays up. A quick swipe could
            // fling the opening out of the cache window and leave the test
            // looking for a node that was never closed. The frame's own
            // position is read before and after, so a click that ate the
            // drag would fail here instead of passing by standing still.
            // The drag starts on the page because the introduction is
            // taller than the viewport and its own center can be a document
            // away; the point is still over its wash, which is what the
            // press must not eat.
            val beforeDrag = compose.onNodeWithTag("surah-about")
                .getUnclippedBoundsInRoot().top
            compose.onNodeWithContentDescription(page).performTouchInput {
                down(center)
                moveBy(Offset(0f, -300f))
                advanceEventTime(500)
                up()
            }
            compose.waitForIdle()
            val afterDrag = compose.onNodeWithTag("surah-about")
                .getUnclippedBoundsInRoot().top
            assertTrue(
                "the drag must scroll the reading, not be eaten by the introduction's tap",
                afterDrag < beforeDrag,
            )
            compose.onNodeWithText(about).assertDoesNotExist()
            compose.onNodeWithTag("surah-about").assertExists()

            // The tap is the exit: a press on the wash, away from the prose's
            // links, puts the introduction away and brings its door back.
            val closeTap = washPoint()
            compose.onNodeWithTag("surah-about").performTouchInput {
                click(closeTap)
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText(about).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(about).assertExists()
            compose.onNodeWithTag("surah-about").assertDoesNotExist()
            assertEquals(
                "closing the introduction must hand the reader back the head it opened from",
                head,
                headOffset(page),
            )

            // The reported flow, and the one the old close lost: the reader
            // opens the introduction at the head and reads several screens
            // into it. The introduction is still the first item on the
            // glass, so its scroll is the list's offset, and the close must
            // not spend that offset on the ayahs below.
            compose.onNodeWithText(about).performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag("surah-about").fetchSemanticsNodes().isNotEmpty()
            }
            repeat(4) {
                compose.onNodeWithContentDescription(page).performTouchInput {
                    down(center)
                    moveBy(Offset(0f, -800f))
                    advanceEventTime(500)
                    up()
                }
                compose.waitForIdle()
            }
            val deepBounds = compose.onNodeWithTag("surah-about")
                .getUnclippedBoundsInRoot()
            val pageTop = compose.onNodeWithContentDescription(page)
                .getUnclippedBoundsInRoot().top
            assertTrue(
                "the document must be read far enough that the head is off the glass " +
                    "and still stand on it: intro=$deepBounds pageTop=$pageTop",
                deepBounds.top < pageTop && deepBounds.bottom > pageTop,
            )

            // The introduction is taller than the viewport, so its own
            // center is a document away; the close's action is invoked
            // directly. The gesture that reaches the same action is
            // measured by the first tap above.
            compose.onNodeWithTag("surah-about")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText(about).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(about).assertExists()
            compose.onNodeWithTag("surah-about").assertDoesNotExist()
            assertEquals(
                "a close deep in the introduction must hand back the head, not the ayah",
                head,
                headOffset(page),
            )
        } finally {
            reading.close()
        }
    }

    /** The opening's top, the place the head stands on the glass. */
    private fun openingTop() = compose.onNodeWithTag("surah-opening")
        .getUnclippedBoundsInRoot().top

    /**
     * The opening's place under the page's own head, read against the page's
     * top. The bar is drawn over the page while it is shown, so a screen
     * coordinate moves with the bar, while the list's head does not; the
     * difference is the place the close must hand back.
     */
    private fun headOffset(page: String) = openingTop() -
        compose.onNodeWithContentDescription(page).getUnclippedBoundsInRoot().top

    /**
     * A point on the introduction's wash that stands on the glass: the
     * node's own center is a document away when the introduction is taller
     * than the viewport. The point is four pixels inside the leading edge,
     * in the wash the prose does not reach, so no link is under the press.
     */
    private fun washPoint(): Offset {
        val top = compose.onNodeWithTag("surah-about").getUnclippedBoundsInRoot().top
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val y = (root.top + root.bottom) / 2 - top
        return with(compose.density) { Offset(4f, y.toPx()) }
    }

    /**
     * A reader on the shipped sizes, at Al-Baqarah's first verse, whose
     * introduction is long enough to carry the reader several screens past
     * the glass, with the opening item one row above the place the reading
     * opens on.
     */
    private fun seed() {
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                // Al-Baqarah's first ayah, the global number after Al-Fatiha's
                // seven: the long introduction the close must not lose.
                setAyah(8)
                setTheme(AppTheme.Light)
                setShowTranslation(false)
                setWordByWord(false)
                setTajweed(true)
                for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
            }
        }
    }
}
