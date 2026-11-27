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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * The surah's introduction is its own way out.
 *
 * The introduction opens from the quiet door under the basmalah. Opened, the
 * door is gone: the introduction is the whole of what stands there, and a tap
 * anywhere on it puts it away. The tap is a standard click, so a drag that
 * starts on it belongs to the reading's list and a reader who scrolls through
 * a long introduction does not lose it to the press. The click's label is the
 * word a screen reader hears, so the one exit is spoken as Hide (owner
 * decision, 23.2).
 *
 * This leg is the one that measures both halves: the tap closes, the drag
 * does not, and the click carries the label. The state is seeded and the
 * activity launched inside the test for the reason [ReadingVerseGapTest]
 * states: the reading reads its settings once while its library opens.
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
            compose.onNodeWithText(about).performScrollTo().performClick()
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
            val beforeDrag = compose.onNodeWithTag("surah-about")
                .getUnclippedBoundsInRoot().top
            compose.onNodeWithTag("surah-about").performTouchInput {
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
            compose.onNodeWithTag("surah-about").performScrollTo()
            compose.onNodeWithTag("surah-about").performTouchInput {
                click(Offset(8f, 8f))
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText(about).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(about).assertExists()
            compose.onNodeWithTag("surah-about").assertDoesNotExist()
        } finally {
            reading.close()
        }
    }

    /**
     * A reader on the shipped sizes, at Al-Fatiha's first verse, with the
     * opening item one row above the place the reading opens on.
     */
    private fun seed() {
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setAyah(1)
                setTheme(AppTheme.Light)
                setShowTranslation(false)
                setWordByWord(false)
                setTajweed(true)
                for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
            }
        }
    }
}
