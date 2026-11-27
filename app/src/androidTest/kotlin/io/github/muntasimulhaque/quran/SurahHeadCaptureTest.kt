package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * The surah's head, photographed for review: the Arabic name, the Latin name,
 * the place and the count, the introduction's door, and the basmalah under
 * them.
 *
 * No store frame meets the head. The first opens on 2:256, and the recitation
 * frame opens on 1:1 with the head already above the glass, so the order the
 * door and the basmalah keep cannot be seen in the eight cards. This class is
 * the evidence for that one surface: the shipped type and the top of the
 * reading's ladder, in English and in Bangla, on the phone (owner decision,
 * 23.6). It is a review frame, not a store frame, and it is not part of a
 * release gate.
 *
 * The phone is handed back as it was found, in a `finally`: every instrumented
 * class in a run shares one process and one set of settings, and a language
 * or a type size left behind would reach every class after this one.
 */
@RunWith(AndroidJUnit4::class)
class SurahHeadCaptureTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theHeadAtBothLanguagesAndBothEndsOfTheLadder() {
        val review = ReviewState(context)
        review.save()
        try {
            // The word lists are installed so the head is photographed alone:
            // a missing list draws its Add row under the basmalah, and that
            // row is the reading's invitation, not the head.
            review.install("words-en", "words-bn")
            photograph("surah-head", UiLanguage.English, TextSize.DEFAULT)
            photograph("surah-head-large", UiLanguage.English, TextSize.STEPS.last())
            photograph("surah-head-bn", UiLanguage.Bangla, TextSize.DEFAULT)
            photograph("surah-head-bn-large", UiLanguage.Bangla, TextSize.STEPS.last())
        } finally {
            review.restore()
        }
    }

    /**
     * One head, one frame. The reading is put at Al-Baqarah's first verse and
     * asked for the list's own top, because a scroll to the door stops where
     * the door is visible and leaves the opening a little below the head.
     * The door is drawn only after the introduction has loaded and the
     * basmalah only after the content has, so the frame waits for both before
     * it is taken.
     */
    private fun photograph(name: String, language: UiLanguage, step: Float) {
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage(language.tag)
                // Al-Baqarah's first ayah is the global number after
                // Al-Fatiha's seven.
                setAyah(8)
                setTheme(AppTheme.Light)
                setTranslationPacks(emptySet())
                setTafsirPacks(emptySet())
                setShowTranslation(false)
                setWordByWord(false)
                setTajweed(true)
                for (role in TypeRole.entries) setTypeSize(role, step)
            }
        }
        val resources = contextIn(language)
        ActivityScenario.launch(MainActivity::class.java).use {
            val page = resources.getString(ReadingR.string.reading_page_description)
            compose.waitUntil(timeoutMillis = 180_000) {
                compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription(page).performScrollToIndex(0)
            compose.waitForIdle()
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithText(
                    resources.getString(ReadingR.string.reading_about_surah),
                ).fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithTag("surah-basmalah").fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitForIdle()
            Thread.sleep(800)

            val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            checkNotNull(screen) { "the display did not answer with a frame" }
            File(screenshotOutputDirectory(), "$name.png").outputStream().use { output ->
                screen.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }

    /**
     * The app's own resources, read in the language it is drawing. The
     * interface's names are resources, so in the Bangla interface the reading
     * is described in Bengali while the test's own context still answers in
     * the phone's system language.
     */
    private fun contextIn(language: UiLanguage): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language.tag))
        return context.createConfigurationContext(configuration)
    }
}
