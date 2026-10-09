package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.core.app.ActivityScenario
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.PackStore
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

    /**
     * The count a search frame waits for: the digits and the word.
     *
     * A wait on any text holding "match" is satisfied by the sheet's own "No
     * matches.", which is what a query that has not run yet says, so the
     * store's search frame was a sheet that had found nothing. A count
     * with its digits is only ever drawn by a search that came back, and
     * Compose's own matchers match a string or a substring, so this is the one
     * that asks for the number.
     */
    private val matchCount = SemanticsMatcher("a match count with its digits") { node ->
        node.config.getOrElseNullable(SemanticsProperties.Text) { null }
            ?.any { part -> Regex("""\d+\s+matches""").containsMatchIn(part.text) } == true
    }

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
                // A dialog answers the back key; a window that merely holds
                // the foreground does not. The launcher can crash and come
                // back over the tour on a loaded emulator, and pressing back
                // at it eight times only loses the leg. After two
                // unanswered backs, put our own task in front again.
                if (attempt >= 2) bringAppForward() else dismissDialog()
                if (attempt == 7) {
                    throw AssertionError("a system window ($intruder) stayed over $name; ${focusEvidence()}")
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
     * Waits until no window of ours stands over the reading: a sheet is its
     * own window, and it outlives its semantics by an animation. The tour
     * already reads the window list to catch an intruder, so the same reading
     * answers whether our own window has gone.
     */
    /**
     * The package that owns the window in front of the app, or null when only
     * ours is. Two questions are asked. Which window holds focus: that is the
     * ordinary case, and the owner's name goes into the failure message. And
     * which visible window is an error dialog: an ANR dialog can sit over the
     * screen without ever taking focus, which is how the 2.1 capture kept two
     * phone frames of "Pixel Launcher isn't responding" while the leg was
     * green (twenty-ninth session). The focused window was still the app, the
     * old guard saw nothing, the dialog swallowed the tour's back key, and
     * the last two frames photographed a screen two steps behind the tour.
     *
     * The visible check reads the window blocks themselves, so a dialog whose
     * surface is gone (the stale record the old phrase search tripped on)
     * is never called an intruder: only `mHasSurface=true` counts. The focus
     * check is held to the same standard: a window can die under the tour
     * and keep the focus line as a stale record, and its own block's
     * `mHasSurface=true` is what makes it real.
     */
    private fun intruderWindow(): String? = runCatching {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ours = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.executeShellCommand("dumpsys window windows").use { command ->
            val text = java.io.FileInputStream(command.fileDescriptor).bufferedReader().use { it.readText() }
            focusedIntruder(text, ours) ?: visibleErrorDialog(text, ours)
        }
    }.getOrNull()

    private fun focusedIntruder(text: String, ours: String): String? {
        val focus = text.lineSequence().firstOrNull { line ->
            line.contains("mCurrentFocus=") || line.contains("mFocusedWindow=")
        } ?: return null
        val owner = Regex("""Window\{[^}]*?\s([^\s/}]+)/""").find(focus)?.groupValues?.get(1)
        if (owner == null || owner == ours) return null
        // A focused window without a live surface is a ghost: the launcher
        // can crash under the tour and keep the focus line while its window
        // is gone, and pressing back at a ghost only loses the leg.
        val token = Regex("""Window\{([^}\s]+)\s""").find(focus)?.groupValues?.get(1)
            ?: return owner
        return if (windowHasSurface(text, token)) owner else null
    }

    /** True when the block of the named window token reports a live surface. */
    private fun windowHasSurface(text: String, token: String): Boolean {
        var inBlock = false
        for (line in text.lineSequence()) {
            if (line.contains("Window #") && line.contains("Window{")) {
                if (inBlock) return false
                inBlock = line.contains("Window{$token ")
            }
            if (inBlock && line.contains("mHasSurface=true")) return true
        }
        return false
    }

    /**
     * The evidence a red leg keeps: the focus line from the window dump at
     * the moment the guard gave up, so the failure names the world it died
     * in rather than only the window it blamed.
     */
    private fun focusEvidence(): String = runCatching {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("dumpsys window windows").use { command ->
            val text = java.io.FileInputStream(command.fileDescriptor).bufferedReader().use { it.readText() }
            text.lineSequence().firstOrNull { line ->
                line.contains("mCurrentFocus=") || line.contains("mFocusedWindow=")
            }?.trim() ?: "no focus line"
        }
    }.getOrElse { "dumpsys failed: ${it.message}" }

    /**
     * Puts the tour's own task back in front after a system window took the
     * foreground. A plain launch would start a second activity and bury the
     * sheet the tour is standing in, so the running instance is reordered to
     * the front of its task instead (the flag is
     * FLAG_ACTIVITY_REORDER_TO_FRONT).
     */
    private fun bringAppForward() {
        runCatching {
            val ours = InstrumentationRegistry.getInstrumentation().targetContext.packageName
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("am start -n $ours/.MainActivity -f 0x00020000")
                .close()
        }
        Thread.sleep(1_500)
    }

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

    private fun readingPage() = rule.onAllNodesWithContentDescription("Reading").onFirst()

    private fun waitForAReading() {
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithContentDescription("Reading").fetchSemanticsNodes().isNotEmpty()
        }
        // The window is not ready for a gesture in the frame it appears in;
        // the first tap of a run must not be swallowed by startup.
        Thread.sleep(1_500)
    }

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
     * stable; copy is not.
     */
    private fun waitForTag(tag: String, timeout: Long = 15_000) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun chromeIsUp(): Boolean =
        rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()

    /**
     * A tap on the reading's own quiet margin brings the chrome up.
     *
     * The page's exact centre is not a safe place to tap, and this tour found
     * that the hard way: a control can sit under the finger there. A word by
     * word tile is a control, and the study reading now carries the aid in the
     * store's own frames, so a centre tap could open one of those tiles and the
     * bar never came up, and the tour then reached for a door that was not
     * there. The margin is the reading's quiet edge, where no control lives,
     * which is the same rule the rest of the suite already follows through
     * `tapThePaper`.
     */
    private fun tapTheReading() {
        readingPage().performTouchInput { click(Offset(4f, height * 0.5f)) }
    }

    /** A tap on the margin brings the chrome up; the states are remembered. */
    private fun revealChrome() {
        repeat(3) {
            if (chromeIsUp()) return
            tapTheReading()
            runCatching {
                rule.waitUntil(timeoutMillis = 4_000) { chromeIsUp() }
            }
        }
    }

    /** And a tap on the margin puts it away again, for the frames that want the page bare. */
    private fun hideChrome() {
        repeat(3) {
            if (!chromeIsUp()) return
            tapTheReading()
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
     * The visible-dialog half of the window guard: a live ANR or crash dialog
     * is an intruder even when our app still holds focus, and a dialog whose
     * surface is gone is not. Both shapes are fed in directly, because the
     * guard cannot be waiting for a real ANR to test whether it would see
     * one.
     */
    @Test
    fun theWindowGuardReadsVisibleDialogs() {
        val ours = "io.github.muntasimulhaque.quran"
        val live = """
            Window #12 Window{6fdf0b8 u0 Application Not Responding: com.google.android.apps.nexuslauncher}:
              mHasSurface=true isReadyForDisplay()=true mWindowRemovalAllowed=false
        """.trimIndent()
        assertEquals(
            "com.google.android.apps.nexuslauncher",
            visibleErrorDialog(live, ours),
        )

        val gone = """
            Window #12 Window{6fdf0b8 u0 Application Not Responding: com.google.android.apps.nexuslauncher}:
              mHasSurface=false isReadyForDisplay()=false mWindowRemovalAllowed=false
        """.trimIndent()
        assertNull(visibleErrorDialog(gone, ours))

        val aNormalWindow = """
            Window #12 Window{6fdf0b8 u0 com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity}:
              mHasSurface=true isReadyForDisplay()=true mWindowRemovalAllowed=false
        """.trimIndent()
        assertNull(visibleErrorDialog(aNormalWindow, ours))
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
            // The tour photographs 2:255, and it is here rather than at 1:1
            // because the verse is one a reader who has never seen the app
            // can recognise. The frames are the store, and the store's
            // first image is the app's promise.
            settings.setAyah(262)
            settings.setTheme(AppTheme.Paper)
            settings.setTranslationPacks(setOf("translation-saheeh-en"))
            settings.setTafsirPacks(setOf("tafsir-ibn-kathir-en"))
            settings.setShowTranslation(true)
            settings.setShowTafsir(true)
            settings.setWordByWord(true)
            for (role in TypeRole.entries) settings.setTypeSize(role, TextSize.DEFAULT)
        }
        Thread.sleep(500)
    }

    private fun runTheTour() {
        waitForAReading()

        // 1. The reading at 2:255, with nothing over it. The list opens on
        // the reader's place, so the verse is on screen until anything
        // scrolls it away.
        hideChrome()
        capture("01-reading")

        // 2. The chrome over the reading: Browse, Search, and Settings.
        revealChrome()
        capture("02-chrome")
        hideChrome()

        // 3. An ayah's actions, and the card they open. The card carries
        // the tafsir doors, which is the deeper surface a reader meets off
        // the reading. This runs before any scrolling, while 2:255 is still
        // the verse on screen.
        val pillIsUp = {
            rule.onAllNodesWithContentDescription("Tafsir").fetchSemanticsNodes().isNotEmpty()
        }
        var pillRaised = false
        repeat(3) {
            if (pillRaised) return@repeat
            runCatching {
                rule.onAllNodesWithText("2:255", substring = true).onFirst()
                    .performTouchInput { longClick() }
            }
            pillRaised = runCatching {
                rule.waitUntil(timeoutMillis = 20_000) { pillIsUp() }
            }.isSuccess
            if (!pillRaised) back()
        }
        if (!pillRaised) {
            runCatching { capture("09-long-press-failed") }
            throw AssertionError("the ayah's long press did not raise the actions bar")
        }
        rule.onNodeWithContentDescription("Tafsir").performClick()
        waitForTag("ayah-card", timeout = 30_000)
        Thread.sleep(600)
        captureScreen("07-ayah-card")

        // 4. The tafsir open on the card: Ibn Kathir for 2:255.
        rule.onNodeWithText("Ibn Kathir").performClick()
        Thread.sleep(1_200)
        captureScreen("08-tafsir")
        back()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithTag("ayah-card", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }

        // 5. The surah opening, which is the top of the reading list.
        rule.onNodeWithContentDescription("Reading")
            .performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.85f, durationMillis = 200) }
        Thread.sleep(800)
        capture("03-surah-opening")

        // 6. Search, with a word a reader would type.
        revealChrome()
        rule.onNodeWithContentDescription("Search").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNode(hasSetTextAction()).performTextInput("mercy")
        rule.waitUntil(timeoutMillis = 180_000) {
            rule.onAllNodes(matchCount).fetchSemanticsNodes().isNotEmpty()
        }
        captureScreen("04-search")
        back()

        // 7. The settings hub.
        revealChrome()
        rule.onNodeWithContentDescription("Settings").performClick()
        waitForTag("settings-hub")
        captureScreen("05-settings")
        back()

        // 8. Browse, the surah list.
        revealChrome()
        rule.onNodeWithContentDescription("Browse the Quran").performClick()
        waitForTag("browse-sheet")
        captureScreen("06-browse")
        back()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithTag("browse-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }
}

/**
 * A live ANR or crash dialog in a `dumpsys window windows` dump, whatever
 * window holds focus, or null when there is none. Each window is read as its
 * own block: only one whose surface is up can be an intruder, and only one
 * whose title is an error is returned, so a stale record of a dialog that is
 * gone is never mistaken for one that is in front. The title names the
 * package that stopped responding; the window itself belongs to the system,
 * so the package in the title is what a failure should say.
 */
internal fun visibleErrorDialog(text: String, ours: String): String? {
    val error = listOf("Application Not Responding", "isn't responding", "Application Error")
    var block = StringBuilder()
    var inBlock = false
    fun inspect(): String? {
        val body = block.toString()
        if (!body.contains("mHasSurface=true")) return null
        val title = Regex("""Window\{([^}]*)\}""").find(body)?.groupValues?.get(1) ?: return null
        if (title.contains(ours)) return null
        if (error.none { title.contains(it) }) return null
        return title.substringAfter(": ", "a system dialog").trim().substringBefore(' ')
    }
    for (line in text.lineSequence()) {
        if (line.contains("Window #") && line.contains("Window{")) {
            if (inBlock) inspect()?.let { return it }
            block = StringBuilder()
            inBlock = true
        }
        if (inBlock) block.append(line).append('\n')
    }
    if (inBlock) inspect()?.let { return it }
    return null
}
