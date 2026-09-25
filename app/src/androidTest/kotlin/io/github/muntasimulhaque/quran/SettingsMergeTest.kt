package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.settings.R as SettingsR
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
 * The reading's switches and the packs behind them are one row each.
 *
 * The hub used to carry a switch and, four rows below it, a separate page for
 * the same thing, which read as two controls for one decision (owner report,
 * D-097). Each switch now carries the chevron that opens its own list, so the
 * translation's row is the translation's door. This test turns the
 * translation off at its switch and opens its list from the same row.
 */
@RunWith(AndroidJUnit4::class)
class SettingsMergeTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            PackStore(context).install("translation-saheeh-en")
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setMode(ReadingMode.Study)
                    setTranslationPacks(setOf("translation-saheeh-en"))
                    setShowTranslation(true)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theTranslationSwitchCarriesItsOwnDoor() {
        openSettings()
        val title = compose.activity.getString(SettingsR.string.settings_show_translation_title)
        compose.onNodeWithText(title).assertIsDisplayed()

        // The door beside the switch is named for what it opens.
        val door = compose.activity.getString(SettingsR.string.settings_open_translations)
        compose.onNodeWithContentDescription(door).assertIsDisplayed()
        compose.onNodeWithContentDescription(door).performClick()

        // And it lands on the translations themselves, not on another hub.
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Saheeh International").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The old duplication is gone: there is no hub row that opens the
     * Translations page a second time under a different name. The page's own
     * title is on screen once, as the head of the page the door opened.
     */
    @Test
    fun theTranslationListIsNotRepeatedInTheHub() {
        openSettings()
        val hubRows = compose.onAllNodesWithText(
            compose.activity.getString(SettingsR.string.settings_title_translations),
        ).fetchSemanticsNodes().size
        assertTrue(
            "the hub must not carry the Translations row as well as the switch",
            hubRows == 0,
        )
    }

    private fun openSettings() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
            .tapThePaper()
        val settings = compose.activity.getString(R.string.action_settings)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(settings).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(settings).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("settings-hub", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
