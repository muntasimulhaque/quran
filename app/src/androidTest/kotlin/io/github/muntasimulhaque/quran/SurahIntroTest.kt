package io.github.muntasimulhaque.quran

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
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
import io.github.muntasimulhaque.quran.data.PackStore
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
 * The introduction opens from the quiet door above the basmalah, at the foot
 * of the surah's head. Opened, the door is gone: the introduction is the
 * whole of what stands there, and a tap anywhere on it puts it away. The tap
 * is a standard click, so a drag that starts on it belongs to the reading's
 * list and a reader who scrolls through a long introduction does not lose it
 * to the press. The click's label is the
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
 *
 * The head's order is measured here too: the door closes the name, the
 * place, and the count, and the basmalah opens the text, so the preface
 * never stands between the Quran's own first line and the first ayah
 * (owner decision, 23.6). The word meanings' invitation is measured with the
 * same head: under the door, above the basmalah, and away while the preface
 * is open (owner decision, 23.7).
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

    /**
     * The head's order on the glass: the name, the place, and the count end
     * with the introduction's door, and the basmalah, where the surah has a
     * separate one, opens the text above the first ayah. Al-Fatiha and
     * At-Tawbah open without that line, so the door is the head's last line
     * there and the first verse follows it (owner decision, 23.6).
     */
    @Test
    fun theDoorClosesTheHeadAndTheBasmalahOpensTheText() {
        seed()
        ActivityScenario.launch(MainActivity::class.java).use {
            openTheHead()
            val door = compose.onNodeWithText(about).getUnclippedBoundsInRoot()
            val basmalah = compose.onNodeWithTag("surah-basmalah")
                .getUnclippedBoundsInRoot()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag("ayah-2:1").fetchSemanticsNodes().isNotEmpty()
            }
            val first = compose.onNodeWithTag("ayah-2:1").getUnclippedBoundsInRoot()
            assertTrue(
                "the door must close the head, above the basmalah: " +
                    "door=$door basmalah=$basmalah",
                door.bottom <= basmalah.top,
            )
            assertTrue(
                "the basmalah must open the text, above the first verse: " +
                    "basmalah=$basmalah first=$first",
                basmalah.bottom <= first.top,
            )
        }

        // The two surahs with no separate line: the door is the head's last
        // line, and the first verse follows it.
        for ((surah, ayah) in listOf(1 to 1, 9 to 1236)) {
            seed(ayah)
            ActivityScenario.launch(MainActivity::class.java).use {
                openTheHead()
                assertTrue(
                    "surah $surah opens without a separate basmalah",
                    compose.onAllNodesWithTag("surah-basmalah").fetchSemanticsNodes().isEmpty(),
                )
                val firstTag = "ayah-$surah:1"
                compose.waitUntil(timeoutMillis = 10_000) {
                    compose.onAllNodesWithTag(firstTag).fetchSemanticsNodes().isNotEmpty()
                }
                val door = compose.onNodeWithText(about).getUnclippedBoundsInRoot()
                val first = compose.onNodeWithTag(firstTag).getUnclippedBoundsInRoot()
                assertTrue(
                    "the door must close the head above the first verse of surah $surah: " +
                        "door=$door first=$first",
                    door.bottom <= first.top,
                )
            }
        }
    }

    /**
     * The invitation is part of the head (owner decision, 23.7).
     *
     * The word list's door stands under the introduction's door and above the
     * basmalah, so the Quran's own first line is still the last thing before
     * the verse. While the preface is open the ask waits, because the head is
     * then the surah's own prose. The list is taken off the phone for the
     * measurement, so the door is drawn whatever the class before this one
     * left behind, and put back in the finally.
     */
    @Test
    fun theInvitationStandsInTheHeadAboveTheBasmalah() {
        val review = ReviewState(context)
        review.save()
        try {
            PackStore(context).remove("words-en")
            seed()
            ActivityScenario.launch(MainActivity::class.java).use {
                openTheHead()
                compose.waitUntil(timeoutMillis = 10_000) {
                    compose.onAllNodesWithTag("surah-add-words").fetchSemanticsNodes().isNotEmpty()
                }
                val door = compose.onNodeWithText(about).getUnclippedBoundsInRoot()
                val invitation = compose.onNodeWithTag("surah-add-words").getUnclippedBoundsInRoot()
                val basmalah = compose.onNodeWithTag("surah-basmalah").getUnclippedBoundsInRoot()
                compose.waitUntil(timeoutMillis = 10_000) {
                    compose.onAllNodesWithTag("ayah-2:1").fetchSemanticsNodes().isNotEmpty()
                }
                val first = compose.onNodeWithTag("ayah-2:1").getUnclippedBoundsInRoot()
                assertTrue(
                    "the invitation must stand under the surah's own door: " +
                        "door=$door invitation=$invitation",
                    door.bottom <= invitation.top,
                )
                assertTrue(
                    "the invitation must stand above the basmalah, never between it and " +
                        "the verse: invitation=$invitation basmalah=$basmalah",
                    invitation.bottom <= basmalah.top,
                )
                assertTrue(
                    "the basmalah must open the text above the first verse: " +
                        "basmalah=$basmalah first=$first",
                    basmalah.bottom <= first.top,
                )

                // Opened, the head is the surah's own prose: the ask waits
                // until the preface closes.
                compose.onNodeWithText(about).performClick()
                compose.waitUntil(timeoutMillis = 10_000) {
                    compose.onAllNodesWithTag("surah-about").fetchSemanticsNodes().isNotEmpty()
                }
                compose.onAllNodesWithTag("surah-add-words").assertCountEquals(0)
                compose.onNodeWithTag("surah-basmalah").assertExists()
            }
        } finally {
            review.restore()
        }
    }

    /**
     * The reading's head at the list's own top. The page is asked for item
     * zero because a scroll to the door stops where the door is visible and
     * leaves the opening a little below the head. The door is drawn only
     * after the introduction has loaded, so the wait stands on the door
     * itself.
     */
    private fun openTheHead() {
        val page = context.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(page).performScrollToIndex(0)
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(about).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
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
     * A reader on the shipped sizes at the verse asked for. Al-Baqarah's
     * first verse is the default: its introduction is long enough to carry
     * the reader several screens past the glass, and the opening item stands
     * one row above the place the reading opens on.
     */
    private fun seed(ayah: Int = 8) {
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                // Al-Baqarah's first ayah is the global number after
                // Al-Fatiha's seven.
                setAyah(ayah)
                setTheme(AppTheme.Light)
                setShowTranslation(false)
                setWordByWord(false)
                setTajweed(true)
                for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
            }
        }
    }
}
