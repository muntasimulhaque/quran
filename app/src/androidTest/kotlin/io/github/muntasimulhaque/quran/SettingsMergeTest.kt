package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.click
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

    @Test
    fun theTranslationSwitchCarriesItsOwnDoor() {
        openSettings()
        val title = compose.activity.getString(SettingsR.string.settings_show_translation_title)
        compose.onNodeWithText(title).assertIsDisplayed()

        // The door beside the switch is named for what it opens, and the
        // row's spoken name now also carries the title and the subtitle so a
        // screen reader does not lose the state under the switch.
        val door = compose.activity.getString(SettingsR.string.settings_open_translations)
        compose.onNodeWithContentDescription(door, substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription(door, substring = true).performClick()

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

    /**
     * The page carries the same switch as the row that opened it.
     *
     * It is not a second setting: the page reads the value the hub row shows
     * and writes it where the row reads it. Turning the reading off here is
     * what the row says when the reader goes back (owner decision, this
     * session).
     */
    @Test
    fun thePageCarriesTheSwitchesOtherHand() {
        openSettings()
        val door = compose.activity.getString(SettingsR.string.settings_open_translations)
        compose.onNodeWithContentDescription(door, substring = true).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Saheeh International").fetchSemanticsNodes().isNotEmpty()
        }
        // The page's switch reads the state the row left, and writes the same
        // value the row writes. The tap is a finger on the switch, which the
        // row underneath it answers: the row is the control and the switch is
        // its picture.
        compose.onNodeWithTag("switch-translation-page", useUnmergedTree = true)
            .performTouchInput { click() }
        compose.waitUntil(timeoutMillis = 15_000) {
            runBlocking { !SettingsStore(context).settings.first().showTranslation }
        }
        // Back on the hub, the row's own switch reads the same answer.
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        Thread.sleep(700)
        compose.onNodeWithTag("switch-translation", useUnmergedTree = true)
            .assertIsOff()
    }

    private fun openSettings() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(readingPage).onFirst()
            .tapThePaper()
        // Settings is the display panel's last row now (owner decision, this
        // session).
        val display = compose.activity.getString(R.string.action_display)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(display).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(display).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("display-all-settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("display-all-settings").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("settings-hub", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
