package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The reading draws what the reader asked to see.
 *
 * The translation is drawn on the page unasked, so it has a switch, on by
 * default: a reader who hides it gets the ayah and nothing under it, and the
 * switch brings the same text back without reopening the library, because
 * the packs are untouched and the page follows the switch. (The tafsir has
 * no such switch: it is drawn only from the ayah pill's own mark, so its
 * packs are its one setting.) This test turns the translation off before the
 * activity starts and reads the reading back.
 */
@RunWith(AndroidJUnit4::class)
class SettingsVisibilityTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            PackStore(context).install(TRANSLATION)
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTranslationPacks(setOf(TRANSLATION))
                    setShowTranslation(false)
                }
            }
        }

        override fun after() {
            runBlocking {
                SettingsStore(context).apply {
                    setShowTranslation(true)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun hidingTheTranslationHidesItAndTheSwitchBringsItBack() {
        awaitReading()
        Thread.sleep(1_500)
        assertTrue(
            "the translation must not be drawn while the switch is off",
            compose.onAllNodesWithText(TRANSLATION_OPENING, substring = true)
                .fetchSemanticsNodes().isEmpty(),
        )

        revealChrome()
        // Settings is the display panel's last row now (owner decision, this
        // session).
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.action_display))
            .performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("display-all-settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("display-all-settings").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(compose.activity.getString(
                io.github.muntasimulhaque.quran.feature.settings.R.string.settings_show_translation_title,
            )).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("switch-translation").performClick()
        back()

        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(TRANSLATION_OPENING, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitReading() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun revealChrome() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.onAllNodesWithContentDescription(readingPage).onFirst()
            .tapThePaper()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(
                compose.activity.getString(R.string.action_display),
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        Thread.sleep(700)
    }

    private companion object {
        const val TRANSLATION = "translation-saheeh-en"

        /** The first words of 1:1 in Saheeh International, footnote marker off. */
        const val TRANSLATION_OPENING = "In the name of"
    }
}
