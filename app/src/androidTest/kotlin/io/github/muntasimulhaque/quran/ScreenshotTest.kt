package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.core.app.ActivityScenario
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Photographs the reading surfaces, so anyone can look at the app without a
 * device. The PNGs land in the app's external files directory under
 * `screenshots/`, and the pipeline pulls them out for review.
 *
 * The tour is deliberately small and stable: one page, its chrome, and the
 * study reading. The sheets and the card are covered by the other
 * instrumented tests, which assert behavior rather than looks.
 *
 * It prepares its own library and its own settings first, so a photograph
 * never depends on what the last run left behind: the same frames come out
 * of a fresh emulator every time. Development builds carry every pack, so
 * nothing here needs a network.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /**
     * Waits until the screen has stopped changing, then keeps the frame. The
     * emulator draws in software, so a frame that includes a newly summoned
     * control can take seconds to appear; a fixed sleep would photograph the
     * frame before it.
     */
    private fun capture(name: String) {
        rule.waitForIdle()
        var previous: Bitmap? = null
        repeat(20) {
            Thread.sleep(400)
            val bitmap = androidx.test.runner.screenshot.Screenshot.capture().bitmap
            if (previous != null && previous.sameAs(bitmap)) {
                write(name, bitmap)
                return
            }
            previous = bitmap
        }
        previous?.let { write(name, it) }
    }

    private fun write(name: String, bitmap: Bitmap) {
        val directory = screenshotDirectory()
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    /**
     * The directory the build collects: the runner hands us an additional
     * output directory, and a plain run without one falls back to the app's
     * own external files.
     */
    private fun screenshotDirectory(): File {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val given = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val base = if (given.isNullOrBlank()) {
            File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots")
        } else {
            File(given)
        }
        return base
    }

    private fun mushafPage() = rule.onAllNodes(hasContentDescription("Mushaf page", substring = true)).onFirst()

    private fun studyPage() = rule.onAllNodesWithContentDescription("Study page").onFirst()

    private fun waitForAReading() {
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodes(hasContentDescription("Mushaf page", substring = true))
                .fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithContentDescription("Study page").fetchSemanticsNodes().isNotEmpty()
        }
        // The window is not ready for a gesture in the frame it appears in;
        // the first tap of a run must not be swallowed by startup.
        Thread.sleep(1_500)
    }

    private fun inMushaf(): Boolean =
        rule.onAllNodes(hasContentDescription("Mushaf page", substring = true))
            .fetchSemanticsNodes().isNotEmpty()

    private fun back() {
        // A raw back key is used instead of Espresso: it needs no window
        // focus, which a freshly opened sheet does not always have.
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        Thread.sleep(700)
    }

    private fun waitFor(text: String, timeout: Long = 15_000) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun chromeIsUp(): Boolean =
        rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()

    /** A tap on the paper brings the chrome up; the states are remembered. */
    private fun revealChrome() {
        repeat(3) {
            if (chromeIsUp()) return
            val node = if (inMushaf()) mushafPage() else studyPage()
            node.performTouchInput { click(Offset(width / 2f, height * 0.5f)) }
            runCatching {
                rule.waitUntil(timeoutMillis = 4_000) { chromeIsUp() }
            }
        }
    }

    /** And a tap puts it away again, for the captures that want the page bare. */
    private fun hideChrome() {
        repeat(3) {
            if (!chromeIsUp()) return
            val node = if (inMushaf()) mushafPage() else studyPage()
            node.performTouchInput { click(Offset(width / 2f, height * 0.5f)) }
            runCatching {
                rule.waitUntil(timeoutMillis = 4_000) { !chromeIsUp() }
            }
        }
    }

    @Test
    fun walkTheReading() {
        prepareTheLibrary()
        ActivityScenario.launch(MainActivity::class.java).use {
            runTheTour()
        }
    }

    /**
     * The state the tour photographs: the reader's own translation, the word
     * list that speaks its language, the first tafsir, and the defaults a
     * fresh install starts from.
     */
    private fun prepareTheLibrary() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PackStore(context)
        store.install("translation-saheeh-en")
        store.install("words-en")
        store.install("tafsir-ibn-kathir-en")
        val settings = SettingsStore(context)
        runBlocking {
            settings.setAyah(1)
            settings.setMode(ReadingMode.Mushaf)
            settings.setTheme(AppTheme.Paper)
            settings.setTranslationPacks(setOf("translation-saheeh-en"))
            settings.setTafsirPacks(setOf("tafsir-ibn-kathir-en"))
            settings.setWordByWord(true)
            for (role in TypeRole.entries) settings.setTypeSize(role, TextSize.DEFAULT)
        }
        Thread.sleep(500)
    }

    private fun runTheTour() {
        waitForAReading()

        // The Mushaf, with nothing over it.
        if (!inMushaf()) {
            revealChrome()
            rule.onNodeWithContentDescription("Mushaf").performClick()
            Thread.sleep(1_000)
        }
        hideChrome()
        capture("01-mushaf")

        // The chrome over the page.
        revealChrome()
        capture("02-chrome")

        // Search, with a word a reader would type.
        rule.onNodeWithContentDescription("Search").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNode(hasSetTextAction()).performTextInput("mercy")
        Thread.sleep(1_400)
        capture("05-search")
        back()

        // Settings.
        revealChrome()
        rule.onNodeWithContentDescription("Settings").performClick()
        waitFor("Appearance")
        capture("06-settings")
        rule.onNodeWithText("About").performClick()
        waitFor("Credits and licenses")
        rule.onNodeWithText("Credits and licenses").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("QPC V2 page fonts").fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(500)
        capture("12-credits")
        back()
        Thread.sleep(600)
        capture("13-about")
        back()
        back()

        // Browse: the surahs, the juz, Last read, and the saved.
        revealChrome()
        rule.onNodeWithContentDescription("Browse the Quran").performClick()
        waitFor("Al-Fatihah")
        capture("07-browse-surahs")
        rule.onNodeWithText("Juz").performClick()
        Thread.sleep(800)
        capture("08-browse-juz")
        rule.onNodeWithText("Last read").performClick()
        Thread.sleep(900)
        capture("09-browse-last-read")
        rule.onAllNodesWithText("Saved").onFirst().performClick()
        Thread.sleep(800)
        capture("14-browse-saved")
        back()

        // The study reading of the same place.
        revealChrome()
        rule.onNodeWithContentDescription("Study").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithContentDescription("Study page").fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(1_200)
        capture("03-study")

        // The translations page, where more than one may now be on.
        revealChrome()
        rule.onNodeWithContentDescription("Settings").performClick()
        waitFor("Translations")
        rule.onNodeWithText("Translations").performClick()
        waitFor("Saheeh International")
        Thread.sleep(700)
        capture("15-translations")
        back()
        back()

        // The surah opening, which is the top of the study list.
        rule.onNodeWithContentDescription("Study page")
            .performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.85f, durationMillis = 200) }
        Thread.sleep(800)
        capture("04-study-surah-opening")

        // An ayah, its actions, and its card.
        // The Arabic line sits at the top of the block; the markers, which
        // are links, sit lower, and a link would take the press instead.
        rule.onNodeWithText("1:1").performTouchInput {
            longClick(Offset(centerX, top + height * 0.15f))
        }
        Thread.sleep(600)
        capture("10-ayah-actions")
        rule.onNodeWithContentDescription("More").performClick()
        Thread.sleep(1_200)
        capture("11-ayah-card")
        back()

        // Leave the app on the Mushaf page for the next run.
        revealChrome()
        rule.onNodeWithContentDescription("Mushaf").performClick()
        Thread.sleep(800)
    }
}
