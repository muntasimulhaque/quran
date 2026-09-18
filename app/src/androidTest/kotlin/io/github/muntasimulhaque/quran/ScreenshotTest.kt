package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Photographs the reading surfaces, so anyone can look at the app without a
 * device. The PNGs land in the app's external files directory under
 * `screenshots/`, and the pipeline pulls them out for review.
 *
 * The tour is deliberately small and stable: one page, its chrome, and the
 * study reading. The sheets and the card are covered by the other
 * instrumented tests, which assert behavior rather than looks.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun capture(name: String) {
        rule.waitForIdle()
        Thread.sleep(450)
        val bitmap = androidx.test.runner.screenshot.Screenshot.capture().bitmap
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

    private fun mushafPage() = rule.onAllNodesWithContentDescription("Mushaf page").onFirst()

    private fun studyPage() = rule.onAllNodesWithContentDescription("Study page").onFirst()

    private fun waitForAReading() {
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithContentDescription("Mushaf page").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithContentDescription("Study page").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun inMushaf(): Boolean =
        rule.onAllNodesWithContentDescription("Mushaf page").fetchSemanticsNodes().isNotEmpty()

    private fun back() {
        Espresso.pressBack()
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
        if (chromeIsUp()) return
        val node = if (inMushaf()) mushafPage() else studyPage()
        node.performTouchInput { click(Offset(width / 2f, height * 0.05f)) }
        Thread.sleep(700)
    }

    @Test
    fun walkTheReading() {
        waitForAReading()

        // The Mushaf, with nothing over it.
        if (!inMushaf()) {
            revealChrome()
            rule.onNodeWithContentDescription("Mushaf").performClick()
            Thread.sleep(1_000)
        }
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
        back()

        // Browse: the surahs, the juz, and the saved.
        revealChrome()
        rule.onNodeWithContentDescription("Browse the Quran").performClick()
        waitFor("Al-Fatihah")
        capture("07-browse-surahs")
        rule.onNodeWithText("Juz").performClick()
        Thread.sleep(800)
        capture("08-browse-juz")
        rule.onAllNodesWithText("Saved").onFirst().performClick()
        Thread.sleep(800)
        capture("09-browse-saved")
        back()

        // The study reading of the same place.
        revealChrome()
        rule.onNodeWithContentDescription("Study").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithContentDescription("Study page").fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(1_200)
        capture("03-study")

        // The surah opening, which is the top of the study list.
        rule.onNodeWithContentDescription("Study page")
            .performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.85f, durationMillis = 200) }
        Thread.sleep(800)
        capture("04-study-surah-opening")

        // An ayah, its actions, and its card.
        rule.onNodeWithText("the Entirely Merciful", substring = true).performClick()
        Thread.sleep(600)
        capture("10-ayah-actions")
        rule.onNodeWithContentDescription("More").performClick()
        waitFor("Word by word")
        capture("11-ayah-card")
        back()

        // Leave the app on the Mushaf page for the next run.
        revealChrome()
        rule.onNodeWithContentDescription("Mushaf").performClick()
        Thread.sleep(800)
    }
}
