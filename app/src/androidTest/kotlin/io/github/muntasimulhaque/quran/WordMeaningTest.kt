package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * The word meaning, end to end: the reader's language is set, its word list
 * is installed, the switch is on, and a long press on the first Arabic word
 * opens its meaning on the word's own sheet.
 *
 * The sheet is the footnote's container and the word aid's own page: the
 * head (the sheet's name and the ayah's place) on the page's left edge, then
 * the word and its meaning centered as the card they are. The word is the
 * Quran's own size and the gloss the translation's. The frame this test
 * keeps is the evidence the head and the card are read from, not only the
 * semantics tree.
 *
 * The library is assembled before the activity starts, so no network is
 * involved; development builds carry every pack. The test hands the app back
 * as it found it, because every instrumented class in a run shares one
 * process and one set of settings (owner report, the capture classes).
 */
@RunWith(AndroidJUnit4::class)
class WordMeaningTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var original: AppSettings = AppSettings()

    private val library = object : ExternalResource() {
        override fun before() {
            original = runBlocking { SettingsStore(context).settings.first() }
            val store = PackStore(context)
            store.install("translation-al-bayan-bn")
            store.install("words-bn")
            // The roots travel with the word list in the app, so the store
            // frame the sheet is judged from carries them too (owner
            // decision, this session).
            store.install("word-root")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("bn")
                    // The frame is the page's own day, so the sheet is judged
                    // on the ground the store set is captured on and the
                    // frame does not change with whatever the last class left
                    // behind (owner report: a capture class states its own
                    // setup).
                    setTheme(AppTheme.Light)
                    setTranslationPacks(setOf("translation-al-bayan-bn"))
                    setWordByWord(true)
                    setAyah(1)
                    for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
                }
            }
        }

        override fun after() {
            runBlocking {
                SettingsStore(context).apply {
                    setAyah(original.ayah)
                    setTranslationPacks(original.translationPacks)
                    setTafsirPacks(original.tafsirPacks)
                    setShowTranslation(original.showTranslation)
                    setWordByWord(original.wordByWord)
                    original.uiLanguage?.let { setUiLanguage(it) }
                    setTheme(original.theme)
                    for (role in TypeRole.entries) setTypeSize(role, original.sizeOf(role))
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun pressingAndHoldingAWordOpensItsMeaningInTheReadersLanguage() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithContentDescription(page)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // The first word of Al-Fatihah 1:1, in the print's own spelling, held
        // where it is drawn: the line runs right to left, so the word begins
        // at the line's right edge.
        val line = compose.onAllNodesWithText(
            "بِسۡمِ",
            substring = true,
            useUnmergedTree = true,
        ).onFirst()
        line.holdTheFirstWord()
        // The sheet is the word's own page: the word it names is the one the
        // reader held, and the first Bangla meaning of that word stands under
        // it. The meaning is read inside the sheet, not on the page: the
        // verse's own translation carries the same word, so a gloss found
        // anywhere on the glass was no proof that the sheet had opened.
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodes(
                hasAnyAncestor(hasTestTag("word-sheet")) and
                    hasText("নামে", substring = true),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        // The study lines the word pack carries: the dictionary form and the
        // grammar, on the same sheet and in the reader's language. The word
        // is 1:1's first, whose lemma and part of speech the corpus names
        // (owner decision, this session).
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet-lemma", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet-grammar", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // The frame the head and the facts are reviewed from. The sheet is
        // its own window, so the root at this moment is the sheet the reader
        // sees. A sheet's semantics exist before its window has finished
        // rising, and the 18.0 frame was taken mid-rise, so the frame is kept
        // only when two captures in a row are identical: a settled sheet
        // photographs the same twice (owner report, this session).
        val frame = settle()
        File(screenshotOutputDirectory(), "word-meaning.png").outputStream().use { output ->
            frame.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        frame.recycle()
    }

    /**
     * The dictionary form's places: the count line is the door, the page is
     * the sheet's second page, and a place is the reading's next stop.
     *
     * 1:1's first word is the dictionary form اسْم. Its places open with the
     * word the reader is on (1:1), and Al-Baqarah 2:31 is the second place in
     * the Book's order. The tap on that place must put the sheet away, move
     * the reading to 2:31, and write the reader's place. The frame is the
     * page's evidence, not only the semantics tree (owner decision, this
     * session).
     */
    @Test
    fun theDictionaryFormsPlacesOpenAndLandOnTheAyah() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithContentDescription(page)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("بِسۡمِ", substring = true, useUnmergedTree = true)
            .onFirst()
            .holdTheFirstWord()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet-lemma-door", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithTag("word-sheet-lemma-door", useUnmergedTree = true)
            .onFirst()
            .performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet-places", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet-place-38-3", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val frame = settle()
        File(screenshotOutputDirectory(), "word-places.png").outputStream().use { output ->
            frame.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        frame.recycle()
        // The page's back returns to the word, and the door opens the same
        // page again: the reader's way back is not a dismissal of the sheet
        // (owner decision, this session).
        compose.onAllNodesWithContentDescription(
            compose.activity.getString(ReadingR.string.reading_word_places_back),
            useUnmergedTree = true,
        ).onFirst().performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithTag("word-sheet-lemma-door", useUnmergedTree = true)
            .onFirst()
            .performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet-place-38-3", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithTag("word-sheet-place-38-3", useUnmergedTree = true)
            .onFirst()
            .performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            runBlocking { SettingsStore(context).settings.first().ayah } == 38
        }
    }

    /**
     * The sheet's frame, once its arrival has stopped moving. Two captures in
     * a row that are identical are a still sheet; a frame that arrives while
     * the window is still rising is not evidence of anything.
     */
    private fun settle(): Bitmap {
        var prior: Bitmap? = null
        repeat(8) {
            compose.waitForIdle()
            Thread.sleep(600)
            val frame = compose.onRoot().captureToImage().asAndroidBitmap()
            val last = prior
            prior = frame
            if (last != null && last.sameAs(frame)) {
                last.recycle()
                return frame
            }
            last?.recycle()
        }
        throw AssertionError("the word sheet never settled")
    }
}
