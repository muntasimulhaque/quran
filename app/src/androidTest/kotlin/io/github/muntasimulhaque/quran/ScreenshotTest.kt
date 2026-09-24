package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
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
 * The tour is deliberately small: the eight frames the store lists, one
 * surface each, captured in the order the listing names them. A ninth has to
 * earn its place against the reading, so the tour is trimmed rather than
 * allowed to grow back.
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
     * Keeps the settled frame of the app's own surface. The capture is the
     * Compose root, not the whole screen, so a system dialog can never enter
     * a frame and a loaded emulator cannot steal the tour's next tap through
     * a screenshot. The slow software emulator can still stall the PixelCopy
     * behind the first try; the frame is static, so a fresh idle wait and a
     * retry is always safe.
     */
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

    /**
     * Keeps the whole display for the frames a sheet owns. A modal sheet
     * lives in its own window, and the compose root cannot PixelCopy that
     * window's surface, so the search, the settings, the Browse list, and the
     * ayah card each need one screen capture. A sheet's semantics exist
     * before its window has drawn and its opening animation has settled, so
     * the frame is kept only when two captures in a row are identical:
     * whatever is still arriving photographs differently from the capture
     * before it, and a settled screen photographs the same twice. This loop
     * only looks at the screen, unlike the old settle loop that drove the app
     * between full-screen captures and took the phone emulator down, and the
     * window list is checked before every frame: a frame of Android is worse
     * than no frame at all.
     */
    private fun captureScreen(name: String) {
        var prior: android.graphics.Bitmap? = null
        repeat(8) { attempt ->
            rule.waitForIdle()
            Thread.sleep(600)
            val intruder = intruderWindow()
            if (intruder != null) {
                dismissDialog()
                if (attempt == 7) {
                    throw AssertionError("a system window ($intruder) stayed over $name")
                }
            } else {
                val bitmap = androidx.test.runner.screenshot.Screenshot.capture().bitmap
                val settled = prior != null && prior!!.sameAs(bitmap)
                prior?.recycle()
                prior = bitmap
                if (settled) {
                    write(name, bitmap)
                    return
                }
            }
        }
        throw AssertionError("the screen over $name never settled")
    }

    private fun hasSystemDialog(): Boolean = intruderWindow() != null

    /**
     * The package that owns the focused window when it is not ours, or null
     * when the app itself holds focus and the frame is safe to keep. The
     * owner is returned so a failure names the window that stole the screen
     * instead of only saying that something did.
     */
    private fun intruderWindow(): String? = runCatching {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ours = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.executeShellCommand("dumpsys window windows").use { command ->
            val text = java.io.FileInputStream(command.fileDescriptor).bufferedReader().use { it.readText() }
            val focus = text.lineSequence().firstOrNull { line ->
                line.contains("mCurrentFocus=") || line.contains("mFocusedWindow=")
            } ?: return@runCatching null
            val owner = Regex("""Window\{[^}]*?\s([^\s/}]+)/""").find(focus)?.groupValues?.get(1)
            if (owner == null || owner == ours) null else owner
        }
    }.getOrNull()

    private fun dismissDialog() {
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("input keyevent KEYCODE_BACK")
                .close()
        }
        Thread.sleep(1_500)
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

    /**
     * A surface identified by a test tag rather than by its copy. The tour
     * must never anchor on a user-visible string: a label can be renamed
     * ("Appearance" became "Theme") and the tour that waited on the old word
     * then fails in CI over a change that is otherwise correct. Tags are
     * stable; copy is not. See AGENTS.md, "Store screenshots".
     */
    private fun waitForTag(tag: String, timeout: Long = 15_000) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
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
            settings.setUiLanguage("en")
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

        // 1. The Mushaf, with nothing over it.
        if (!inMushaf()) {
            revealChrome()
            rule.onNodeWithContentDescription("Switch to the Mushaf page").performClick()
            Thread.sleep(1_000)
        }
        hideChrome()
        capture("01-mushaf")

        // 2. The chrome over the page: the mode door, Browse, Search, and
        // Settings, which is every door the reader has.
        revealChrome()
        capture("02-chrome")

        // 3. The study reading of the same place, with its translation.
        rule.onNodeWithContentDescription("Switch to the study reading").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithContentDescription("Study page").fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(1_200)
        capture("03-study")

        // 4. The surah opening, which is the top of the study list.
        rule.onNodeWithContentDescription("Study page")
            .performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.85f, durationMillis = 200) }
        Thread.sleep(800)
        capture("04-surah-opening")

        // 5. Search, with a word a reader would type, and the filter group
        // under the field.
        revealChrome()
        rule.onNodeWithContentDescription("Search").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNode(hasSetTextAction()).performTextInput("mercy")
        // The first search over a forty megabyte tafsir is the slowest thing
        // the tour does; the frame is kept once a match is on the screen, not
        // on a sleep that lands in the middle of the query.
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithText("matches", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("match", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(600)
        captureScreen("05-search")
        back()

        // 6. The settings hub.
        revealChrome()
        rule.onNodeWithContentDescription("Settings").performClick()
        waitForTag("settings-hub")
        captureScreen("06-settings")
        back()

        // 7. Browse, the surah list.
        revealChrome()
        rule.onNodeWithContentDescription("Browse the Quran").performClick()
        // The anchor is the sheet itself, by tag: the reader's own title sits
        // behind the sheet and matches a text wait at once, which let the
        // capture run before the sheet existed and photographed the reader.
        waitForTag("browse-sheet")
        captureScreen("07-browse")
        back()

        // 8. An ayah's actions, and the card they open, captured from the
        // Mushaf: there the card carries the translation, word by word, and
        // the tafsir, which is the richer surface and the one a reader coming
        // off the page meets. The Arabic line sits at the top of the block;
        // the markers, which are links, sit lower, and a link would take the
        // press instead.
        revealChrome()
        rule.onNodeWithContentDescription("Switch to the Mushaf page").performClick()
        Thread.sleep(1_200)
        rule.onAllNodes(hasContentDescription("1:1.", substring = true)).onFirst()
            .performTouchInput { longClick() }
        // The actions bar slides in over the ayah; the tap on More waits for
        // it to settle, so the press lands on the control and not on an
        // animation.
        Thread.sleep(1_500)
        rule.onNodeWithContentDescription("More").performClick()
        // The card composes after the tap, and its capture raced ahead of it
        // the same way Browse's did; the tag waits for the card itself.
        waitForTag("ayah-card")
        captureScreen("08-ayah-card")
        back()

        // Leave the app on the Mushaf page for the next run. The card was
        // captured from the Mushaf, so the door already offers the study
        // reading when the tour is here; asking for the Mushaf again would
        // look for a door that is not on the bar.
        if (!inMushaf()) {
            revealChrome()
            rule.onNodeWithContentDescription("Switch to the Mushaf page").performClick()
            Thread.sleep(800)
        }
    }
}
