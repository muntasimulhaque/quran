package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithText("নামে", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The aid is a grid, not a heap: a word stands in the same column as its
     * own meaning, and the row beneath it is the next word's, so the reader
     * reads down a column and never hunts for the pair.
     *
     * This is the geometry the change was about, and a string the aid happens
     * to contain does not prove it: two Bangla meanings of 1:1 are long enough
     * that a naive tile would put the whole verse down the page as one
     * column. So the test reads the first word and the first meaning and asks
     * whether they share a column.
     */
    @Test
    fun aWordStandsOverItsOwnMeaning() {
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithText("নামে", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        // the first word of 1:1 exactly as the database spells it, in the reading's Arabic
        val word = compose.onAllNodesWithText("بِسۡمِ", substring = true)
            .fetchSemanticsNodes().first()
        val meaning = compose.onAllNodesWithText("নামে", substring = true)
            .fetchSemanticsNodes().first()
        val wordBox = word.boundsInRoot
        val meaningBox = meaning.boundsInRoot
        // a word and its meaning are one tile: their columns are the same, so
        // the horizontal centres are within a tile's own width
        val wordCentre = wordBox.center.x
        val meaningCentre = meaningBox.center.x
        assertTrue(
            "the word and its meaning stand in one column (word $wordCentre, meaning $meaningCentre)",
            kotlin.math.abs(wordCentre - meaningCentre) < wordBox.width,
        )
    }
}
