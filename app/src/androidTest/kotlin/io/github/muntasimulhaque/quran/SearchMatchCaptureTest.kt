package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.UiLanguage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The search sheet's matched runs, as they are drawn: one frame for a
 * translation hit and one for an Arabic hit.
 *
 * The wash is a tint and not a word, so no semantics tree can carry it, and
 * the store's own search frame reads a reference, which washes nothing. These
 * two frames are the evidence that the found word wears the reading's rounded
 * mark on both of the sheet's paths (owner decision, 21.1).
 *
 * The class borrows the tour's two rules: it writes through
 * [screenshotOutputDirectory] and nowhere else, and it changes no setting, so
 * the app is handed back as it was found. Every instrumented class in a run
 * shares one process and one set of settings.
 */
@RunWith(AndroidJUnit4::class)
class SearchMatchCaptureTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val review = ReviewState(context)

    /**
     * The prose path reads the reader's translation, so the class states that
     * library itself: a clean install has no tafsir or word list installed by
     * the catalog's defaults, and a capture that trusted the previous class
     * passed or failed on the run order (owner decision, 23.0).
     */
    @Before
    fun prepareTheLibrary() {
        review.save()
        review.install("translation-saheeh-en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage(UiLanguage.English.tag)
                setTheme(AppTheme.Light)
                setTranslationPacks(setOf("translation-saheeh-en"))
                setTafsirPacks(emptySet())
                setShowTranslation(true)
                setWordByWord(false)
                setAyah(1)
            }
        }
        Thread.sleep(300)
    }

    @After
    fun handThePhoneBack() {
        review.restore()
    }

    @Test
    fun captureTheFoundWordInTheProsePath() {
        val scenario = openSearch()
        try {
            rule.onNode(hasSetTextAction()).performTextInput("mercy")
            awaitTheMatch("mercy")
            capture("search-match-prose")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun captureTheFoundWordInTheArabicPath() {
        val scenario = openSearch()
        try {
            rule.onNode(hasSetTextAction()).performTextInput("الرحمن")
            awaitTheMatch("الرحمن")
            capture("search-match-arabic")
        } finally {
            scenario.close()
        }
    }

    /** The search sheet, raised on an empty field. */
    private fun openSearch(): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_SEARCH),
        )
        rule.waitUntil(timeoutMillis = 60_000) {
            rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        return scenario
    }

    /**
     * Waits until the query is on two nodes: the field's own editable text and
     * at least one result that carries it. One node is the question; two are
     * the answer.
     */
    private fun awaitTheMatch(term: String) {
        rule.waitUntil(timeoutMillis = 180_000) {
            rule.onAllNodesWithText(term, substring = true)
                .fetchSemanticsNodes().size >= 2
        }
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        Thread.sleep(600)
    }

    /** The whole glass, the way the tour captures it. */
    private fun capture(name: String) {
        val bitmap: Bitmap = androidx.test.runner.screenshot.Screenshot.capture().bitmap
        File(screenshotOutputDirectory(), "$name.png").outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
