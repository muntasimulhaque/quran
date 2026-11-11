package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppSettings
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
        // The first word of Al-Fatihah 1:1, in the print's own spelling. The
        // line is right-aligned, so the first word stands at its right edge.
        val line = compose.onAllNodesWithText(
            "بِسۡمِ",
            substring = true,
            useUnmergedTree = true,
        ).onFirst()
        line.performTouchInput {
            longClick(Offset(width - 32f, height / 2f))
        }
        // The sheet is the word's own page, and the first Bangla meaning of
        // the word stands under it.
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("word-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText("নামে", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // The frame the head and the card are reviewed from. The sheet is its
        // own window, so the root at this moment is the sheet the reader sees.
        compose.waitForIdle()
        val frame = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotOutputDirectory(), "word-meaning.png").outputStream().use { output ->
            frame.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
