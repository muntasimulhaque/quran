package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
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
import io.github.muntasimulhaque.quran.feature.settings.R as SettingsR
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.flow.first
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
 * the same thing, which read as two controls for one decision (owner report).
 * Each switch now carries the chevron that opens its own list, so the
 * translation's row is the translation's door. This test turns the
 * translation off at its switch and opens its list from the same row.
 *
 * The page the row opens carries the same switch at its top, so the reader who
 * tapped the words to flip it finds the switch they meant, and the two are one
 * value read in two places (owner decision, this session).
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
                    setTranslationPacks(setOf("translation-saheeh-en"))
                    setShowTranslation(true)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    /**
     * The row is the door, and it is the only one.
     *
     * The hub used to carry a switch and a chevron side by side, with the
     * chevron holding a spoken name of its own. The row is one door now: it
     * says the setting and where it stands, and a tap anywhere on it lands on
     * the list (owner decision, this session).
     */
    @Test
    fun theTranslationRowIsTheDoor() {
        openSettings()
        val title = compose.activity.getString(SettingsR.string.settings_row_translation)
        compose.onNodeWithTag("row-translation").assertIsDisplayed()
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithTag("row-translation").performClick()

        // And it lands on the translations themselves, not on another hub.
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Saheeh International").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The hub is an index: it names the page and never draws the list itself.
     * The page's own title is on screen once, as the head of the page the row
     * opened.
     */
    @Test
    fun theTranslationListIsNotRepeatedInTheHub() {
        openSettings()
        val hubRows = compose.onAllNodesWithText(
            compose.activity.getString(SettingsR.string.settings_title_translations),
        ).fetchSemanticsNodes().size
        assertTrue(
            "the hub must not carry the Translations list as well as its row",
            hubRows == 0,
        )
    }

    /**
     * The page owns the switch, and the hub row reads what the page wrote.
     *
     * The row is an index line and the switch is the page's, so the two cannot
     * disagree by construction: the row's value is computed from the same
     * setting the switch writes. Turning the reading off here is what the row
     * says when the reader goes back (owner decision, this session).
     */
    @Test
    fun thePageSwitchIsWhatTheRowReadsBack() {
        openSettings()
        compose.onNodeWithTag("row-translation").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Saheeh International").fetchSemanticsNodes().isNotEmpty()
        }
        // A finger on the drawn switch: the row underneath it is the control,
        // and the switch is its picture.
        compose.onNodeWithTag("switch-translation-page-mark", useUnmergedTree = true)
            .performTouchInput { click() }
        compose.waitUntil(timeoutMillis = 15_000) {
            runBlocking { !SettingsStore(context).settings.first().showTranslation }
        }
        // Back on the hub, the row's value says Off.
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        Thread.sleep(700)
        compose.onNodeWithTag("row-translation")
            .assert(hasText(compose.activity.getString(SettingsR.string.settings_state_off)))
    }

    private fun openSettings() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(readingPage).onFirst()
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
