package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * The five reading surfaces a release review needs and the store set does not
 * carry: the surah list, the ayah grid, the settings hub, the listening panel,
 * and the playing pill at the shipped type.
 *
 * These are review frames, never store frames. They go where the runner says
 * and are read before the next leg replaces the folder. The class states its
 * own library and settings and hands the phone back, so it can run alone or
 * after any other capture class (owner decision, 23.0).
 */
@RunWith(AndroidJUnit4::class)
class ReviewCaptureTest {

    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val review = ReviewState(context)

    private val library = object : ExternalResource() {
        override fun before() {
            review.save()
            review.install(
                "translation-saheeh-en",
                "words-en",
                "tafsir-ibn-kathir-en",
                "tafsir-ibn-kathir-bn",
            )
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage(UiLanguage.English.tag)
                    setTheme(AppTheme.Light)
                    setTranslationPacks(setOf("translation-saheeh-en"))
                    setTafsirPacks(setOf("tafsir-ibn-kathir-en", "tafsir-ibn-kathir-bn"))
                    setShowTranslation(true)
                    setWordByWord(false)
                    setTajweed(true)
                    setAyah(1)
                    setDailyAyah(false)
                }
            }
            Thread.sleep(300)
        }

        override fun after() {
            review.restore()
        }
    }

    @get:Rule
    val chain: TestRule = RuleChain.outerRule(library).around(rule)

    @Test
    fun theReadingSurfacesArePhotographedForReview() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForAReading()
            revealChrome()

            rule.onNodeWithContentDescription("Browse the Quran").performClick()
            waitForTag("browse-sheet")
            waitForTag("browse-surahs")
            captureTheScreen("review-browse-surahs")

            rule.onNodeWithTag("surah-row-2").performClick()
            waitForTag("surah-ayahs")
            captureTheScreen("review-browse-ayah-grid")
            back()
            waitForTag("browse-surahs")
            back()
            waitForTagGone("browse-sheet")

            revealChrome()
            rule.onNodeWithContentDescription("Settings").performClick()
            waitForTag("settings-hub")
            captureTheScreen("review-settings-hub")
            back()
            waitForTagGone("settings-hub")

            revealChrome()
            rule.onNodeWithTag("door-1:1").performClick()
            rule.onAllNodesWithText(activityString(R.string.action_play)).onFirst().performClick()
            waitForTag("playback-bar", timeout = 60_000)
            Thread.sleep(1_500)
            captureTheScreen("review-playback-default")

            repeat(3) {
                if (rule.onAllNodesWithText("Reciter").fetchSemanticsNodes().isNotEmpty()) {
                    return@repeat
                }
                rule.onNodeWithTag("playback-words").performClick()
                runCatching {
                    rule.waitUntil(timeoutMillis = 5_000) {
                        rule.onAllNodesWithText("Reciter").fetchSemanticsNodes().isNotEmpty()
                    }
                }
            }
            rule.waitUntil(timeoutMillis = 15_000) {
                rule.onAllNodesWithText("Reciter").fetchSemanticsNodes().isNotEmpty()
            }
            captureTheScreen("review-listening-panel")
        }
    }

    private fun waitForAReading() {
        val page = activityString(ReadingR.string.reading_page_description)
        rule.waitUntil(timeoutMillis = 60_000) {
            rule.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(1_000)
    }

    private fun revealChrome() {
        val page = activityString(ReadingR.string.reading_page_description)
        repeat(3) {
            if (rule.onAllNodesWithTag("reading-bar-shown", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            ) {
                return
            }
            rule.onAllNodesWithContentDescription(page).onFirst().performTouchInput {
                click(Offset(4f, centerY))
            }
            runCatching {
                rule.waitUntil(timeoutMillis = 4_000) {
                    rule.onAllNodesWithTag("reading-bar-shown", useUnmergedTree = true)
                        .fetchSemanticsNodes().isNotEmpty()
                }
            }
        }
    }

    private fun waitForTag(tag: String, timeout: Long = 20_000) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTagGone(tag: String, timeout: Long = 20_000) {
        rule.waitUntil(timeoutMillis = timeout) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        Thread.sleep(700)
    }

    private fun activityString(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

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
