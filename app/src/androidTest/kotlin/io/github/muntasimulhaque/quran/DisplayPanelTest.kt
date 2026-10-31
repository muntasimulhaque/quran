package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The reading's display panel: the page's own sizes and switches, one tap from
 * the page.
 *
 * The panel is a second hand on values that already have a home, so this test
 * walks the whole path a reader does: the bar's sliders, a step of the Quran's
 * ladder, the translation's switch, and the hub at the foot. The settings it
 * checks are the store's own, not a drawn state, because the panel is a
 * shortcut and the store is the answer (owner decision, this session).
 */
@RunWith(AndroidJUnit4::class)
class DisplayPanelTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            val store = PackStore(context)
            for (id in listOf("translation-saheeh-en", "tafsir-ibn-kathir-en", "words-en")) {
                store.install(id)
            }
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTranslationPacks(setOf("translation-saheeh-en"))
                    setTafsirPacks(setOf("tafsir-ibn-kathir-en"))
                    setShowTranslation(true)
                    setWordByWord(false)
                    setTajweed(true)
                    setTypeSize(TypeRole.Arabic, TextSize.DEFAULT)
                    setTypeSize(TypeRole.Translation, TextSize.DEFAULT)
                }
            }
            Thread.sleep(300)
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun thePanelOffersThePageSizesAndSwitches() {
        openDisplay()
        compose.onNodeWithText("Quran text").assertExists()
        compose.onNodeWithText("Show translation").assertExists()
        compose.onNodeWithText("Show word meanings").assertExists()
        compose.onNodeWithText("Tajweed colors").assertExists()
        compose.onNodeWithTag("display-all-settings").assertExists()
    }

    @Test
    fun aStepSetsTheReadingSize() {
        openDisplay()
        val step = TextSize.STEPS[3]
        compose.onNodeWithContentDescription("Quran text size 4 of 5").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            runBlocking { SettingsStore(context).settings.first().arabicSize } == step
        }
    }

    @Test
    fun theTranslationSwitchTurnsTheTranslationOff() {
        openDisplay()
        compose.onNodeWithText("Show translation").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            runBlocking { !SettingsStore(context).settings.first().showTranslation }
        }
    }

    @Test
    fun allSettingsOpensTheHub() {
        openDisplay()
        compose.onNodeWithTag("display-all-settings").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("settings-hub", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The bar's own seven second step-back must not take the panel with it
     * while the reader is still choosing. The panel holds the bar open, and
     * this test waits past the bar's timer to say so (owner decision, this
     * session).
     */
    @Test
    fun thePanelStaysWhileTheReaderUsesIt() {
        openDisplay()
        Thread.sleep(8_000)
        compose.onNodeWithTag("display-all-settings").assertExists()
    }

    /**
     * The reading, then the bar's sliders. The bar may still be up from the
     * first reveal on a cold install, so the tap that reveals it is only made
     * when the mark is not there yet: a tap on a visible bar would hide it.
     */
    private fun openDisplay() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        val display = compose.activity.getString(R.string.action_display)
        if (compose.onAllNodesWithContentDescription(display).fetchSemanticsNodes().isEmpty()) {
            compose.onAllNodesWithContentDescription(readingPage).onFirst().tapThePaper()
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(display).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(display).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("display-all-settings").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
