package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Assert.assertTrue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
/**
 * The word by word reading aid, end to end: a translation and its word list
 * are installed, the switch is turned on, and the study reading is checked to
 * show a meaning under a word, in the language of the chosen translation.
 *
 * The library is assembled before the reader screen starts, which is the
 * order a real reader creates it in, and development builds carry every pack,
 * so no network is involved. That is why the preparation is a rule outside
 * the compose rule: the compose rule owns the activity, so the activity
 * launches after the library is in place, and it is the supported pairing for
 * this test (an empty rule beside a separately launched activity could
 * compose on a thread without a looper and take the study list's prefetch
 * scheduler down with it).
 */
@RunWith(AndroidJUnit4::class)
class WordByWordTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    private val library = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val store = PackStore(context)
            store.install("translation-taisirul-quran-bn")
            store.install("words-bn")
            val settings = SettingsStore(context)
            runBlocking {
                settings.setUiLanguage("en")
                settings.setTranslationPacks(setOf("translation-taisirul-quran-bn"))
                settings.setWordByWord(true)
                settings.setMode(ReadingMode.Study)
                // The reader's own place, set here rather than inherited.
                // These tests share one install and one process, so whatever
                // the last test left standing is where the reader opens; the
                // first meaning this test looks for belongs to 1:1, and the
                // screenshot tour now leaves the reading at 2:255, so a test
                // that did not set its own place was looking for a verse the
                // reader was never on (found by the 3.1 capture).
                settings.setAyah(1)
                for (role in TypeRole.entries) settings.setTypeSize(role, TextSize.DEFAULT)
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun wordMeaningsFollowTheTranslationLanguage() {
        // The first Bangla meaning of Al-Fatihah 1:1, from the word list. The
        // wait is generous on purpose: a software-rendered emulator takes its
        // time opening the content library, and a slow machine is not a
        // failing reading aid.
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithTag("word-by-word", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithText("নামে", substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The aid is a flow of pairs, and a pair is one unit: a word stands
     * directly over its own meaning, and the pair is as wide as the two of
     * them.
     *
     * This is the geometry the change was about (owner report, which
     * took back the grid), and a string the aid happens to contain
     * does not prove it: two Bangla meanings of 1:1 are long enough that a
     * tile measured against the widest of them would put the whole verse down
     * the page. So the test reads the first word and the first meaning and
     * asks whether they stand in one pair.
     */
    @Test
    fun aWordStandsOverItsOwnMeaning() {
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithTag("word-by-word", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithText("নামে", substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        // the first word of 1:1 exactly as the database spells it, in the reading's Arabic
        val word = compose.onAllNodesWithText("بِسۡمِ", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().first()
        val meaning = compose.onAllNodesWithText("নামে", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().first()
        val wordBox = word.boundsInRoot
        val meaningBox = meaning.boundsInRoot
        // a word and its meaning are one pair: they are drawn over each other,
        // so their centres are within the pair's own width of one another
        assertTrue(
            "the word must stand over its own meaning (word ${wordBox.center.x}, " +
                "meaning ${meaningBox.center.x})",
            kotlin.math.abs(wordBox.center.x - meaningBox.center.x) < wordBox.width,
        )
        // and the pair hangs below the word rather than beside it
        assertTrue(
            "the meaning must be under the word (${meaningBox.top} of ${wordBox.bottom})",
            meaningBox.top > wordBox.top,
        )
    }
}
