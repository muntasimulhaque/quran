package io.github.muntasimulhaque.quran

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
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
import android.os.Build
import androidx.test.core.app.ActivityScenario
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SavedStore
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
        // A single copy can land while the list is still settling after the
        // first measurement, which tore the phone's first frame: a line of
        // the translation was drawn twice. The frame is kept only when two
        // copies in a row are identical, so a still page photographs the same
        // twice.
        var prior: Bitmap? = null
        repeat(6) { attempt ->
            rule.waitForIdle()
            Thread.sleep(600)
            val bitmap = try {
                val root = rule.onAllNodes(isRoot(), useUnmergedTree = true).onLast()
                root.captureToImage().asAndroidBitmap()
            } catch (error: AssertionError) {
                if (attempt == 5) throw error
                null
            }
            if (bitmap != null) {
                val settled = prior != null && prior.sameAs(bitmap)
                prior?.recycle()
                prior = bitmap
                if (settled) {
                    write(name, bitmap)
                    bitmap.recycle()
                    return
                }
            }
        }
        prior?.recycle()
        throw AssertionError("the reading over $name never settled")
    }

    /**
     * Keeps the whole display for the frames a sheet owns.
     *
     * A modal sheet lives in its own window, and the compose root cannot
     * PixelCopy that window's surface, so the ayah's actions, the search, the
     * settings, the share card, and the tafsir each need one screen capture.
     * The status bar comes with the display, and so does the clock: on a page
     * scrolled into a surah it can stand over the first line, which the
     * reading does by decision (see the head's padding in the reading screen).
     * The frame is kept as the phone shows it (owner decision, this session).
     *
     * A sheet's semantics exist before its window has drawn and its opening
     * animation has settled, so the frame is kept only when two captures in a
     * row are identical: whatever is still arriving photographs differently
     * from the capture before it, and a settled screen photographs the same
     * twice. This loop only looks at the screen, unlike the old settle loop
     * that drove the app between full-screen captures and took the phone
     * emulator down, and the window list is checked before every frame: a
     * frame of Android is worse than no frame at all.
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
                val settled = prior != null && prior.sameAs(bitmap)
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
     * The directory the build collects: [screenshotOutputDirectory], shared
     * with the variant frames so the two cannot answer the runner's own
     * question differently.
     */
    private fun screenshotDirectory(): File = screenshotOutputDirectory()

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
     * The page's exact centre is not a safe place to tap: a control can sit
     * under the finger there, and a selected translation line is not the
     * paper either. The margin is the reading's quiet edge, where no control
     * lives, which is the same rule the rest of the suite already follows
     * through `tapThePaper`.
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
        // The library, the reader's note, and the phone's own chrome are all
        // this test's setup, so all three are taken down with it: every class
        // in a run shares one process, one store, and one phone.
        try {
            prepareTheLibrary()
            ActivityScenario.launch(MainActivity::class.java).use {
                runTheTour()
            }
        } finally {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            runBlocking {
                SavedStore(context).apply {
                    TOUR_NOTES.forEach { (ayah, _) -> unsave(ayah) }
                    close()
                }
            }
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
     * Puts the phone's own chrome into the state a shop window can show: one
     * clock on every frame, a full battery, and no notification icons.
     *
     * The demo mode is SystemUI's own broadcast, and some builds drop it. Every
     * command is therefore best effort: a phone that ignores them keeps its own
     * clock, and the frames still settle, which is the difference between a
     * listing that reads as two phones and one that does not (owner decision,
     * this session).
     *
     * The network is drawn by the demo broadcast as one group per modem, and
     * every AVD here reports one modem except the seven inch image, which
     * reports three: on that image the demo bar is "3G 3G 3G" and leaving demo
     * mode draws the device's own bar with a shield and a key glyph beside the
     * clock. Neither is a store frame, so that set was pulled rather than
     * shipped (owner decision, this session). The phone and the ten inch
     * images draw one clean group with this command.
     */
    private fun prepareTheSystemChrome() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String) {
            runCatching { automation.executeShellCommand(command).close() }
        }
        shell("settings put global sysui_demo_allowed 1")
        shell("am broadcast -a com.android.systemui.demo -e command enter")
        shell("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941")
        shell("am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false")
        shell("am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4")
        shell("am broadcast -a com.android.systemui.demo -e command notifications -e visible false")
    }

    /**
     * The state the tour photographs: the reader's own translation, the word
     * list that speaks its language, both tafsirs so the ayah card's doors have
     * two to choose between, and the defaults a fresh install starts from.
     */
    private fun prepareTheLibrary() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // The store frame should show a working daily reminder, not the
        // phone's refusal. These grants are test setup, not app behavior.
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching {
                instrumentation.uiAutomation.grantRuntimePermission(
                    context.packageName,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
        }
        runCatching {
            instrumentation.uiAutomation
                .executeShellCommand("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow")
                .close()
        }
        val store = PackStore(context)
        store.install("translation-saheeh-en")
        store.install("words-en")
        store.install("tafsir-ibn-kathir-en")
        store.install("tafsir-ibn-kathir-bn")
        // The Saved frame needs something saved: the reader's own work, under
        // the place it was written on. Three ayahs of one passage, because one
        // row in a sheet is a list that reads as an empty state, and a reader's
        // notes are the one surface in the app that holds their own words. The
        // notes are written before the activity starts and removed when the
        // tour is done, because the store is shared by every class in the run.
        SavedStore(context).apply {
            TOUR_NOTES.forEach { (ayah, note) ->
                runBlocking { setNote(ayah, note) }
            }
            close()
        }
        prepareTheSystemChrome()
        val settings = SettingsStore(context)
        runBlocking {
            settings.setUiLanguage("en")
            // The tour photographs 2:255, and it is here rather than at 1:1
            // because the verse is one a reader who has never seen the app
            // can recognise. The frames are the store, and the store's
            // first image is the app's promise.
            settings.setAyah(TOUR_AYAH)
            settings.setTheme(AppTheme.Light)
            settings.setTranslationPacks(setOf("translation-saheeh-en"))
            settings.setTafsirPacks(setOf("tafsir-ibn-kathir-en", "tafsir-ibn-kathir-bn"))
            settings.setShowTranslation(true)
            // The first frame is the reading at its default: the word aid is
            // a long press now, not a field of pairs under every ayah (owner
            // decision, this session).
            settings.setWordByWord(false)
            // The reminder and the reciter are state a phone keeps across runs,
            // and a test that trusted the device would photograph whatever the
            // last session left: the first ten inch capture in this session
            // found a reminder armed at 6:31 PM from a run in November on the
            // same AVD. The tour states its own setup rather than the device's
            // (owner report, this session).
            settings.setDailyAyah(false)
            settings.setRecitation("husary")
            // The reading's one-time tap hint is not this set's subject, and
            // a first launch's hint would otherwise ride into whatever frame
            // the tour reached first.
            settings.setBarTaught(true)
            // The colors are the 10.3 change; a test that left the switch off
            // would silently strip them from every store frame.
            settings.setTajweed(true)
            // The store set is the steady reading, not the first minute: the
            // word aid is off, so no frame catches a word sheet over the
            // opening, and the reminder's one-time offer is not this tour's
            // subject.
            for (role in TypeRole.entries) settings.setTypeSize(role, TextSize.DEFAULT)
        }
        Thread.sleep(500)
    }

    /**
     * The eight frames the store shows, in the order a reader meets them.
     *
     * The set used to be four photographs of the same page: the reading at
     * 2:255, the same reading with its menu up, the same reading at night, and
     * the same reading with the recitation's offer standing on it. A shopper
     * learned what one verse looks like and nothing about the app. The eight
     * are eight surfaces now, and the first one carries the app's own chrome,
     * so the frame that has to establish the product actually shows it: the
     * surah's name, the juz, and the three doors the reading keeps (owner
     * decision, this session).
     *
     * Every frame is a screen capture, the chrome's included. The four that
     * were root captures had no status bar while the four sheet frames did,
     * so one listing showed two different phones (owner decision, this
     * session).
     *
     * The order is not taste. The last frame is the pill's offer, which stays
     * on the reading until the reader answers it: it carries one answer and no
     * close, so every frame that wants a clean page is taken before it.
     */
    private fun runTheTour() {
        waitForAReading()

        // 1. The reading with its own chrome up: the surah's name, its juz, and
        // the three doors. This is the app's own face, and it is the frame a
        // reader who knows nothing about the product meets first.
        revealChrome()
        captureScreen("01-reading")
        hideChrome()

        // 2. The tafsir, open on its passage. One frame carries the card and
        // the commentary: the source line names the language and the
        // commentary, and the passage's own reference stands over its prose.
        openTheAyahActions()
        rule.onAllNodesWithText(TAFSIR).onFirst().performClick()
        waitForTag("ayah-card", timeout = 30_000)
        rule.onAllNodesWithText(TAFSIR_SOURCE, substring = true).onFirst().performClick()
        waitForTag("tafsir-panel", timeout = 30_000)
        captureScreen("02-tafsir")
        leave(AYAH_CARD)

        // 3. Search that reads a reference. The frame is the state a reader
        // meets: the field, the query, and the results, with the sources
        // folded away as the first keystroke folds them (owner decision,
        // this build). A frame that staged the open source list sold a
        // screen the app hides.
        revealChrome()
        rule.onNodeWithContentDescription("Search").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNode(hasSetTextAction()).performTextInput("kahf 10")
        rule.waitUntil(timeoutMillis = 180_000) {
            rule.onAllNodesWithText(REFERENCE_RESULT, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        // Close the keyboard before the frame: its contact-suggestion strip is
        // not the app, and the store should not show it. An emulator with a
        // hardware keyboard shows no IME at all, and a Back there closes the
        // sheet instead, so the keyboard is dismissed by name.
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        captureScreen("03-search")
        leave(SEARCH_SHEET)

        // 4. Browse's Saved list, with the reader's own note under the ayah it
        // was written on: the one surface in the app that holds the reader's
        // own words.
        revealChrome()
        rule.onNodeWithContentDescription("Browse the Quran").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithTag("browse-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText(SAVED_TAB).onFirst().performClick()
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithText(SAVED_LAST_NOTE, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        // All three rows have to be on the glass before the frame: the note that
        // sorts last is the one the wait above watched for, and the two under it
        // arrive in the same pass.
        Thread.sleep(600)
        captureScreen("04-saved")
        leave(BROWSE_SHEET)

        // 5. The page that explains the colors the first frame is full of:
        // seven families, each with a word from the reading and its rules. It
        // stands here rather than the settings hub, which was this frame's
        // first choice, because the hub's last row carries the version and a
        // store frame that dates itself is wrong the day the next release
        // ships. On a phone the About row happens to fall below the fold; on a
        // seven inch and a ten inch sheet all ten rows fit, so a frame built
        // on a phone's luck broke on its own tablets in the first capture
        // (owner report, this session).
        openSettingsHub()
        rule.onNodeWithTag("row-tajweed").performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(TAJWEED_FAMILY).fetchSemanticsNodes().isNotEmpty()
        }
        captureScreen("05-tajweed")
        leave(SETTINGS_SHEET)

        // 6. The reading at night: the other half of the app, and the frame
        // most likely to show what the light page hides. The flip is the
        // reader's own act, through Settings and Theme: a store write lands on
        // the next launch, and this frame is taken inside this one. The store
        // is set back in a finally as a second net, because every class in a
        // run shares one process and one set of settings.
        val store = io.github.muntasimulhaque.quran.data.SettingsStore(
            InstrumentationRegistry.getInstrumentation().targetContext,
        )
        try {
            chooseTheme(DARK)
            waitForAReading()
            hideChrome()
            Thread.sleep(1_200)
            captureScreen("06-dark-reading")
        } finally {
            // The light page is a promise to the frame after this one and to
            // every class after this one: a run shares one process and one set
            // of settings. The store is written first, so the next launch is
            // the day whatever else happens here, and the way back is then
            // taken in the open. A restore that quietly did not happen once
            // left the recitation's frame on the night page and the leg green.
            kotlinx.coroutines.runBlocking {
                store.setTheme(io.github.muntasimulhaque.quran.data.AppTheme.Light)
            }
            chooseTheme(LIGHT)
            Thread.sleep(800)
        }

        // 7. The card the app hands out: the picture of the ayah, its
        // translation, and the app's own mark, which is the one thing in this
        // app that leaves it.
        openTheAyahActions()
        rule.onAllNodesWithText(SHARE).onFirst().performClick()
        waitForTag("share-sheet", timeout = 30_000)
        captureScreen("07-share-card")
        leave(SHARE_SHEET)

        // 8. The recitation's own offer, last: the compact capsule names the
        // surah, the reciter, and the size before a tap, and it stays on the
        // reading until the reader answers it.
        openTheAyahActions()
        rule.onAllNodesWithText(PLAY).onFirst().performClick()
        waitForTag("listen-offer-bar", timeout = 30_000)
        captureScreen("08-listen-offer")
    }

    /**
     * The hub, from the reading.
     *
     * A closed sheet forgets the page it was on: opening a sheet clears the
     * saved state it left behind (see `openSheet` in the reading), so the mark
     * always rises on the index. A page that rose instead is a real failure and
     * ends this helper at once.
     *
     * The press itself needs more than one try. The bar fades in and takes a
     * moment to answer a touch, so a press can land before its mark is ready;
     * the retry re-summons the bar and presses again. The wait after each
     * press is longer than the sheet's rise, so a press that worked is never
     * followed by a second one on top of an open sheet.
     */
    private fun openSettingsHub() {
        repeat(3) {
            if (hubIsUp()) return
            if (pageIsUp()) {
                throw AssertionError("the settings sheet rose on a page, not on the index")
            }
            waitForAReading()
            revealChrome()
            runCatching {
                rule.onNodeWithContentDescription("Settings").performClick()
            }
            Thread.sleep(1_400)
        }
        if (!hubIsUp()) {
            throw AssertionError("the settings sheet did not rise on the index")
        }
    }

    private fun hubIsUp(): Boolean = rule
        .onAllNodesWithTag("settings-hub", useUnmergedTree = true)
        .fetchSemanticsNodes().isNotEmpty()

    /** A page's own way back is the one mark the index does not draw. */
    private fun pageIsUp(): Boolean = rule
        .onAllNodesWithContentDescription(BACK)
        .fetchSemanticsNodes().isNotEmpty()

    /** The theme page, reached the way a reader reaches it, and one ground. */
    private fun chooseTheme(ground: String) {
        openSettingsHub()
        rule.onAllNodesWithText(THEME).onFirst().performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(ground).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText(ground).onFirst().performClick()
        // The swatch takes a frame to repaint, and the sheet has to be gone
        // before the reading behind it can be photographed.
        Thread.sleep(900)
        leave(SETTINGS_SHEET)
        // The reading under the sheet takes the new ground as it closes, and
        // the first frame after a theme change is the one that repaints.
        rule.waitForIdle()
        Thread.sleep(900)
    }

    /** The ayah's actions, opened from its own number. */
    private fun openTheAyahActions() {
        var raised = false
        repeat(3) {
            if (raised) return@repeat
            runCatching {
                rule.onNodeWithTag("door-2:255").performClick()
            }
            raised = runCatching {
                rule.waitUntil(timeoutMillis = 20_000) {
                    rule.onAllNodesWithText(TAFSIR).fetchSemanticsNodes().isNotEmpty()
                }
            }.isSuccess
            if (!raised) back()
        }
        if (!raised) {
            runCatching { capture("09-ayah-tap-failed") }
            throw AssertionError("the door's tap did not open its menu")
        }
    }

    /**
     * Puts a sheet away however deep it stands, and waits for its window.
     *
     * The sheet is watched by the name it gives its own window
     * (`paneTitle`), not by a tag inside it: the settings hub's tag is gone the
     * moment a page opens, so a helper watching it returned with the sheet
     * still up and the next step pressed marks behind the scrim. Back is the
     * one act a page and a sheet both answer, and the wait is on the surface
     * that has to be gone rather than on a number of presses.
     */
    private fun leave(sheet: String) {
        repeat(3) {
            if (!sheetIsUp(sheet)) {
                settle()
                return
            }
            back()
            Thread.sleep(700)
        }
        if (sheetIsUp(sheet)) {
            throw AssertionError("the $sheet sheet stayed up")
        }
        settle()
    }

    private fun sheetIsUp(sheet: String): Boolean = rule.onAllNodes(
        SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, sheet),
    ).fetchSemanticsNodes().isNotEmpty()

    /**
     * The sheet's window outlives its semantics by its closing animation, and a
     * tap on the paper while the scrim is still up lands on the scrim: the next
     * step would then read a reading that never came.
     */
    private fun settle() {
        Thread.sleep(900)
    }

    private companion object {
        /** The menu rows and page names the tour stands on. */
        const val TAFSIR = "Tafsir"
        const val TAFSIR_SOURCE = "Ibn Kathir"
        const val SHARE = "Share"
        const val PLAY = "Play"
        const val REFERENCE_RESULT = "18:10"
        const val TAJWEED_FAMILY = "Silent letters"
        const val THEME = "Theme"
        const val BACK = "Back"
        const val SETTINGS_SHEET = "Settings"
        const val SEARCH_SHEET = "Search"
        const val SHARE_SHEET = "Share ayah"
        const val AYAH_CARD = "Tafsir"
        const val DARK = "Dark"
        const val LIGHT = "Light"
        const val SAVED_TAB = "Saved"
        const val BROWSE_SHEET = "Browse"

        /** The note the Saved list sorts last, so its arrival means all three are drawn. */
        const val SAVED_LAST_NOTE = "Come back to this one"

        /** The ayahs the tour keeps, and the reader's own words on them. */
        const val TOUR_AYAH = 262
        val TOUR_NOTES = listOf(
            262 to "Come back to this one on the hard mornings.",
            263 to "Read slowly.",
            264 to "For the weeks I could not see a way out.",
        )
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
