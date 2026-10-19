package io.github.muntasimulhaque.quran

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
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
 * A sheet holds still.
 *
 * The owner met a footnote sheet that shook while a finger was on it, and the
 * same shake came back on the settings pages and again on a downward flick:
 * Material3 1.4.0 settled a dragged sheet with a spring that was allowed to
 * pass its anchor, and a content height that changed mid-settle restarted the
 * spring with the finger's own velocity, so the sheet could bounce at the top
 * for as long as the reader watched. The fix is Material3 1.5's bounded fling
 * with every step clamped between the anchors, pinned in
 * `gradle/libs.versions.toml`; this test is the promise kept.
 *
 * The small pull is the one that used to bounce: the finger moves the sheet
 * down a little, the finger lifts, and the sheet returns to its resting
 * place. A sheet that passes its resting place on the way back is the shake,
 * and the frame-by-frame reading below fails the moment it does. The full
 * pull pins the other half of the frozen behavior: a pull down still closes.
 */
@RunWith(AndroidJUnit4::class)
class SheetStillnessTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            PackStore(context).install("translation-saheeh-en")
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTranslationPacks(setOf("translation-saheeh-en"))
                    setShowTranslation(true)
                    setShowTafsir(true)
                    setLongPressHintShown()
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    /**
     * Opens the note on 1:1's third marker, the note the owner reported on.
     */
    private fun openTheFootnote() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(1_500)
        compose.onAllNodesWithText("Especially", substring = true).onFirst()
            .performFirstLinkClick { range ->
                val link = range.item
                link is androidx.compose.ui.text.LinkAnnotation.Clickable &&
                    link.tag == "footnote-3"
            }
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithTag("footnote-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // The window is up before the sheet has finished moving; the gesture
        // has to start where the sheet will rest, not under it.
        Thread.sleep(1_500)
    }

    private fun theSheet() =
        compose.onAllNodesWithTag("footnote-sheet", useUnmergedTree = true).onLast()

    @Test
    fun aSmallPullReturnsWithoutPassingTheRestingPlace() {
        openTheFootnote()

        compose.mainClock.autoAdvance = false
        val sheet = theSheet()
        val rest = sheet.fetchSemanticsNode().positionInRoot.y
        val places = mutableListOf(rest)

        sheet.performTouchInput { down(center) }
        repeat(20) {
            compose.mainClock.advanceTimeByFrame()
            sheet.performTouchInput { moveBy(Offset(0f, 5f)) }
            compose.mainClock.advanceTimeByFrame()
            places += runCatching { sheet.fetchSemanticsNode().positionInRoot.y }
                .getOrNull() ?: Float.NaN
        }
        sheet.performTouchInput { up() }
        repeat(80) {
            compose.mainClock.advanceTimeByFrame()
            places += runCatching { sheet.fetchSemanticsNode().positionInRoot.y }
                .getOrNull() ?: Float.NaN
        }
        compose.mainClock.autoAdvance = true

        val moved = places.filter { !it.isNaN() }.maxOrNull() ?: rest
        assertTrue(
            "the pull must move the sheet before the shrug means anything; " +
                "rest=$rest highest=$moved places=$places",
            moved > rest + 20f,
        )
        val afterRelease = places.filter { !it.isNaN() }.dropWhile { it <= rest + 4f }
        val overshoot = afterRelease.minOrNull() ?: rest
        assertTrue(
            "the sheet passed its resting place on the way back: " +
                "rest=$rest overshoot=$overshoot places=$places",
            overshoot >= rest - 1f,
        )
        val settled = places.last()
        assertTrue(
            "the sheet did not come back to rest; rest=$rest settled=$settled places=$places",
            kotlin.math.abs(settled - rest) <= 1.5f,
        )
    }

    @Test
    fun aPullOnTheHandleStillMovesTheSheet() {
        openTheFootnote()

        compose.mainClock.autoAdvance = false
        val handle = compose.onAllNodesWithContentDescription("Sheet handle").onFirst()
        val rest = handle.fetchSemanticsNode().positionInRoot.y
        handle.performTouchInput { down(center) }
        repeat(20) {
            compose.mainClock.advanceTimeByFrame()
            handle.performTouchInput { moveBy(Offset(0f, 5f)) }
            compose.mainClock.advanceTimeByFrame()
        }
        val moved = handle.fetchSemanticsNode().positionInRoot.y
        handle.performTouchInput { up() }
        repeat(60) { compose.mainClock.advanceTimeByFrame() }
        compose.mainClock.autoAdvance = true

        assertTrue(
            "the handle must still drag the sheet under it; rest=$rest moved=$moved",
            moved > rest + 20f,
        )
    }

    @Test
    fun aPullDownStillCloses() {
        openTheFootnote()
        theSheet().performTouchInput { swipeDown(durationMillis = 300) }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("footnote-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }
}
