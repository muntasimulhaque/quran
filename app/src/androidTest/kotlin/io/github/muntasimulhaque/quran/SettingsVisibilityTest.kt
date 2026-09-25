package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.study.R as StudyR
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
 * Settings gained toggles for the translation and the tafsir, on by default.
 * A reader who hid the translation gets the ayah and nothing under it, and
 * the switch brings the same text back without reopening the library: the
 * packs are untouched, the page follows the switch. This test turns the
 * translation off before the activity starts and reads the reading back.
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
                    setMode(ReadingMode.Study)
                    setTranslationPacks(setOf(TRANSLATION))
                    setShowTranslation(false)
                    setShowTafsir(true)
                }
            }
        }

        override fun after() {
            runBlocking {
                SettingsStore(context).apply {
                    setShowTranslation(true)
                    setShowTafsir(true)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun hidingTheTranslationHidesItAndTheSwitchBringsItBack() {
        awaitStudy()
        Thread.sleep(1_500)
        assertTrue(
            "the translation must not be drawn while the switch is off",
            compose.onAllNodesWithText(TRANSLATION_OPENING, substring = true)
                .fetchSemanticsNodes().isEmpty(),
        )

        revealChrome()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.action_settings))
            .performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(compose.activity.getString(
                io.github.muntasimulhaque.quran.feature.settings.R.string.settings_show_translation_title,
            )).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(compose.activity.getString(
            io.github.muntasimulhaque.quran.feature.settings.R.string.settings_show_translation_title,
        )).performClick()
        back()

        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(TRANSLATION_OPENING, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitStudy() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun revealChrome() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
            .performTouchInput { click(center) }
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(
                compose.activity.getString(R.string.action_settings),
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
