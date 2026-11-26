package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.UiLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Photographs the app in the language half its readers use, and that no store
 * frame has ever shown.
 *
 * The app ships English and Bangla as two first-class interfaces, and the type
 * specimen frames are the only place Bangla has been looked at. Every screen
 * that carries the interface's own words is a place where Bengali's longer
 * lines can meet a limit written for English: a settings row's name is held to
 * two lines, a button's word to one, and the reading's title to one with no
 * wrapping. A frame is the only evidence that they are fine, and the store set
 * is English, so this class exists to produce that evidence and nothing else.
 * It is dispatch-only and never part of a release
 * (owner decision, 19.1).
 *
 * The phone is handed back as it was found, in a `finally`: every instrumented
 * class in a run shares one process and one set of settings, and a language
 * left in Bengali would reach every class after this one.
 */
@RunWith(AndroidJUnit4::class)
class BanglaCaptureTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * The app's own resources, read in the language the app is drawing.
     *
     * The interface's names are resources, so in the Bangla interface the
     * reading's description is Bengali while the test's own context still
     * answers in the phone's system language. A matcher built from the raw
     * context looks for "Reading" on a screen that says "পাঠ" and waits thirty
     * seconds for a page that is already there, which is how this class failed
     * its first run. The configuration is made once here, in the one place a
     * name is read (owner report, 19.1).
     */
    private val bangla by lazy {
        val configuration = android.content.res.Configuration(context.resources.configuration)
        configuration.setLocale(java.util.Locale.forLanguageTag(UiLanguage.Bangla.tag))
        context.createConfigurationContext(configuration)
    }

    @Test
    fun theBanglaInterface() {
        val review = ReviewState(context)
        review.save()
        try {
            prepareTheBanglaReading()
            ActivityScenario.launch(MainActivity::class.java).use {
                waitForAReading()
                // The reading with its own chrome up: the surah's name, its
                // juz, and the three doors, all in Bengali.
                revealChrome()
                captureTheScreen("bn-01-reading")

                // The settings hub: ten rows of the interface's own words, each
                // with the value under it, which is where the longest
                // translations live.
                rule.onNodeWithContentDescription(bangla.getString(R.string.action_settings))
                    .performClick()
                rule.waitUntil(timeoutMillis = 20_000) {
                    rule.onAllNodesWithTag("settings-hub", useUnmergedTree = true)
                        .fetchSemanticsNodes().isNotEmpty()
                }
                captureTheScreen("bn-02-settings")
            }
        } finally {
            review.restore()
        }
    }

    /**
     * The reader's own library in Bengali: the Al-Bayan translation, the Bangla
     * word list, and Ibn Kathir in Bangla, at the shipped defaults. A
     * development build carries every pack, so nothing here needs a network.
     */
    private fun prepareTheBanglaReading() {
        val store = PackStore(context)
        store.install("translation-al-bayan-bn")
        store.install("words-bn")
        store.install("tafsir-ibn-kathir-bn")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage(UiLanguage.Bangla.tag)
                setTheme(AppTheme.Light)
                setTranslationPacks(setOf("translation-al-bayan-bn"))
                setTafsirPacks(setOf("tafsir-ibn-kathir-bn"))
                setShowTranslation(true)
                setWordByWord(false)
                setTajweed(true)
                setAyah(262)
                for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
            }
        }
        Thread.sleep(500)
    }

    private fun waitForAReading() {
        val page = bangla.getString(
            io.github.muntasimulhaque.quran.feature.reading.R.string.reading_page_description,
        )
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(1_500)
    }

    /**
     * A tap on the paper, in the quiet margin the reading keeps for it.
     *
     * The page's center is not a safe tap: a translation's footnote marker sits
     * in the flow of the text, so a center tap opens the footnote instead of the
     * chrome. Four pixels in from the page's leading edge is the margin no ayah
     * or control reaches.
     */
    private fun revealChrome() {
        val page = bangla.getString(
            io.github.muntasimulhaque.quran.feature.reading.R.string.reading_page_description,
        )
        rule.onAllNodesWithContentDescription(page).onFirst().performTouchInput {
            click(Offset(4f, centerY))
        }
        val settings = bangla.getString(R.string.action_settings)
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithContentDescription(settings).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Keeps the display, and only when two copies of it agree.
     *
     * A sheet lives in its own window, which the compose root cannot copy, so
     * every frame here comes from the screen. A still page photographs the same
     * twice; anything still arriving does not.
     */
    private fun captureTheScreen(name: String) {
        var prior: Bitmap? = null
        repeat(8) {
            rule.waitForIdle()
            Thread.sleep(600)
            val bitmap = androidx.test.runner.screenshot.Screenshot.capture().bitmap
            val settled = prior != null && prior.sameAs(bitmap)
            prior?.recycle()
            prior = bitmap
            if (settled) {
                write(name, bitmap)
                return
            }
        }
        throw AssertionError("the screen over $name never settled")
    }

    private fun write(name: String, bitmap: Bitmap) {
        val directory = screenshotOutputDirectory()
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
