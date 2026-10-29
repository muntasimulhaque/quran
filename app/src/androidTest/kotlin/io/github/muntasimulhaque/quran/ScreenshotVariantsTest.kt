package io.github.muntasimulhaque.quran

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The two readings nobody has ever looked at: the dark one and the large one.
 *
 * The store's eight frames are the light theme at the shipped size, which is
 * one of the app's six readings: a scheme of its own, and five text sizes, all
 * of them drawn on the page and none of them ever photographed. A defect that
 * only the dark scheme shows, or only the largest step shows, had no frame to
 * appear in, so it could not be found by looking (owner decision, this
 * session).
 *
 * These frames are evidence and not the listing, so they are named apart from
 * the store's numbered eight and the workflow keeps them out of
 * `play-store/screenshots/`. Each one carries the orientation it was drawn in,
 * so the ten inch leg can photograph the reading standing up as well as on its
 * side without either frame overwriting the other.
 *
 * It is its own class rather than a ninth frame of the tour for one reason:
 * the workflow runs it a second time on the ten inch leg, in portrait, and a
 * class of its own is what lets a runner ask for these two frames without
 * re-photographing the store's set on its side. Its capture is the plain one,
 * with no sheet to settle and no gesture to make: it photographs a still
 * reading and nothing else.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotVariantsTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val orientation: String
        get() = if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            "portrait"
        } else {
            "landscape"
        }

    @Test
    fun theDarkReadingAndTheLargeOne() {
        prepareTheLibrary()
        try {
            runBlocking { SettingsStore(context).setTheme(AppTheme.Dark) }
            photograph("variant-dark-$orientation")

            runBlocking {
                val settings = SettingsStore(context)
                settings.setTheme(AppTheme.Light)
                // The top of the ladder, which is the step the reading is
                // least likely to have been measured at.
                for (role in TypeRole.entries) settings.setTypeSize(role, TextSize.STEPS.last())
            }
            photograph("variant-large-$orientation")
            photographStandingUp()
        } finally {
            // The app is handed back as it was found. Every instrumented
            // class in the run shares one process and one set of settings,
            // so a size left at the top of the ladder is not this test's
            // business to leave behind: SheetStillnessTest measured the
            // footnote sheet at 1.2 and its drag moved 11 dp against a 20 dp
            // floor, which was this class's leftover and not that sheet's
            // behavior (owner report, this session).
            runBlocking {
                val settings = SettingsStore(context)
                settings.setTheme(AppTheme.Light)
                for (role in TypeRole.entries) settings.setTypeSize(role, TextSize.DEFAULT)
            }
        }
    }

    /**
     * The reading standing up, on the screens that can stand up.
     *
     * The ten inch leg used to be asked for this by a second Gradle call from
     * the workflow, which cost that leg a second install, a second test
     * runner, and about a minute of the run; and a second
     * connectedAndroidTest call replaces the additional output folder, so the
     * store's eight had to be collected before it. The rotation belongs to
     * the test that wants the frame. It is best effort: an emulator that
     * refuses the rotation skips the frame instead of failing a leg whose
     * store set is already complete.
     */
    private fun photographStandingUp() {
        // A phone on its side is not a screen standing up, so only a window
        // Android calls a tablet is asked.
        if (context.resources.configuration.screenWidthDp < 840) return
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val turned = runCatching {
            automation.executeShellCommand("settings put system accelerometer_rotation 0").close()
            automation.executeShellCommand("settings put system user_rotation 1").close()
            Thread.sleep(3_000)
        }.isSuccess
        if (!turned) return
        runCatching { photograph("variant-reading-portrait") }
        runCatching {
            automation.executeShellCommand("settings put system user_rotation 0").close()
        }
    }

    /**
     * One reading, one frame, from a clean launch in the settings that are
     * standing.
     */
    private fun photograph(name: String) {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitUntil(timeoutMillis = 30_000) {
                rule.onAllNodesWithContentDescription("Reading").fetchSemanticsNodes().isNotEmpty()
            }
            // The window is not ready to be copied in the frame it appears
            // in, and this one is copied still.
            Thread.sleep(2_000)
            capture(name)
        }
    }

    /** The settled frame of the app's own surface, never the whole screen. */
    private fun capture(name: String) {
        repeat(3) { attempt ->
            rule.waitForIdle()
            try {
                val root = rule.onAllNodes(isRoot(), useUnmergedTree = true).onLast()
                write(name, root.captureToImage().asAndroidBitmap())
                return
            } catch (error: AssertionError) {
                if (attempt == 2) throw error
                Thread.sleep(2_000)
            }
        }
    }

    private fun write(name: String, bitmap: Bitmap) {
        val directory = screenshotOutputDirectory()
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /**
     * What the tour photographs: the reader's own translation and the word
     * list, from the verse the listing opens on.
     */
    private fun prepareTheLibrary() {
        val store = PackStore(context)
        store.install("translation-saheeh-en")
        store.install("words-en")
        store.install("tafsir-ibn-kathir-en")
        store.install("tafsir-ibn-kathir-bn")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setAyah(262)
                setTranslationPacks(setOf("translation-saheeh-en"))
                setTafsirPacks(setOf("tafsir-ibn-kathir-en", "tafsir-ibn-kathir-bn"))
                setShowTranslation(true)
                setWordByWord(true)
                setTajweed(true)
                setLongPressHintShown()
            }
        }
        Thread.sleep(500)
    }
}
