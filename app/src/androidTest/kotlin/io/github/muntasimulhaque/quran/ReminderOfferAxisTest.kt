package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
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
import kotlin.math.abs

/**
 * The reminder offer's one axis, measured: the tile's two answers stand
 * centered under its centered lines, and the quiet word still leads.
 *
 * The offer centers its name, its sentence, and its answers (owner decision,
 * 21.8), the axis the empty note already keeps for a short block that must
 * read as one (15.0). The lines wear that pattern in the code; the glass can
 * read the pair, so the pair is what this pins. The two answers' joint middle
 * must sit on the tile's middle, where the leading edge held them before, and
 * Not now must stand before Turn on, the one order every pair in the app
 * keeps (18.8).
 *
 * The offer stands once, at the end of the first surah a reader finishes, so
 * the test writes the one state that stands it: the answer unwritten, the
 * reminder off, and the reader at Al-Fatiha's last ayah, one breath above the
 * tile. It hands the app back answered, because every instrumented class in a
 * run shares one process and one set of settings.
 */
@RunWith(AndroidJUnit4::class)
class ReminderOfferAxisTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(7)
                    setDailyAyah(false)
                    setReminderAnswered(false)
                }
            }
        }

        override fun after() {
            // The settled state every existing install keeps, so a class that
            // walks the reading after this one never meets the tile.
            runBlocking {
                SettingsStore(context).apply {
                    setDailyAyah(false)
                    setReminderAnswered(true)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theAnswersStandCenteredUnderTheCenteredLines() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }

        // The reader opens at Al-Fatiha's last ayah, one breath above the
        // tile. A swipe of their own is how a reader gets there, and the
        // swipes stop the moment the tile stands, so the count cannot carry
        // past a short surah's end.
        var drawn = theOfferStands()
        repeat(6) {
            if (!drawn) {
                compose.onAllNodesWithContentDescription(readingPage).onFirst()
                    .performTouchInput { swipeUp() }
                compose.waitForIdle()
                drawn = theOfferStands()
            }
        }
        assertTrue("the reminder offer must stand at the end of the first surah", drawn)
        compose.onNodeWithTag("reading-reminder-offer").assertIsDisplayed()

        val tile = compose.onNodeWithTag("reading-reminder-offer").getUnclippedBoundsInRoot()
        val notNow = bounds(ReadingR.string.reading_reminder_not_now)
        val turnOn = bounds(ReadingR.string.reading_reminder_turn_on)
        val tileMiddle = (tile.left.value + tile.right.value) / 2f
        // The pair's middle is the middle of the span its two answers make,
        // not the midpoint between their own middles. The labels differ in
        // width, so that average leans by a quarter of the difference: it read
        // this centered pair 1.1 dp off the tile's middle, 205.7 against
        // 206.9, which is the arithmetic and not the row.
        val pairMiddle = (notNow.left.value + turnOn.right.value) / 2f
        assertTrue(
            "the two answers must stand centered on the tile, not at its " +
                "leading edge: the tile's middle is $tileMiddle and the " +
                "pair's is $pairMiddle",
            abs(tileMiddle - pairMiddle) < 1f,
        )
        assertTrue(
            "the quiet word leads and the filled answer trails",
            notNow.left.value < turnOn.left.value,
        )
    }

    /** Whether the tile is on the glass yet. */
    private fun theOfferStands(): Boolean {
        compose.waitForIdle()
        if (compose.onAllNodesWithTag("reading-reminder-offer").fetchSemanticsNodes().isEmpty()) {
            return false
        }
        return runCatching {
            compose.onNodeWithTag("reading-reminder-offer").assertIsDisplayed()
        }.isSuccess
    }

    /** One answer's box on the glass, held by its word. */
    private fun bounds(label: Int): DpRect =
        compose.onAllNodesWithText(context.getString(label))
            .onFirst()
            .getUnclippedBoundsInRoot()
}
