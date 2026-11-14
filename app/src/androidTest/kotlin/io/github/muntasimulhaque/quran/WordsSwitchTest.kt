package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.WordAudioOnDevice
import io.github.muntasimulhaque.quran.ui.settings.PackSetupState
import io.github.muntasimulhaque.quran.ui.settings.SettingsActions
import io.github.muntasimulhaque.quran.ui.settings.WordsPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The words page's one switch on a fresh install.
 *
 * A fresh install carries no word list, so the switch used to stand disabled
 * until the pack was added: a dead control under the name of the feature,
 * while the row below it did the work. The switch is the ask now, the "tap
 * that turns the aid on" the rule names: it fetches the one list, stays off
 * while the list is on its way, says the transfer under the finger, and turns
 * on when the list lands (owner decision, this session).
 *
 * The page is composed on its own, so the legs need no device reading and no
 * network: the pack's state is handed in.
 */
@RunWith(AndroidJUnit4::class)
class WordsSwitchTest {

    @get:Rule
    val compose = createComposeRule()

    /** The list this install does not have yet. */
    private val missing = ContentPack(
        id = "words-en",
        type = PackType.Words,
        name = "Word meanings",
        language = "en",
        credit = "",
        license = "",
        version = "1",
        shipped = false,
        ayahs = 6236,
        bytes = 6_303_744L,
        installed = false,
    )

    @Test
    fun theSwitchAsksForTheOneList() {
        var asked: Boolean? = null
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                WordsPage(
                    settings = AppSettings(uiLanguage = "en", wordByWord = false),
                    packs = listOf(missing),
                    packSetup = null,
                    actions = SettingsActions(onWordByWord = { asked = it }),
                )
            }
        }
        compose.onNodeWithTag("switch-words-page").assertIsEnabled().performClick()
        assertEquals("the switch must be the ask", true, asked)
    }

    @Test
    fun theSwitchSaysTheTransferUnderTheFinger() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                WordsPage(
                    settings = AppSettings(uiLanguage = "en", wordByWord = false),
                    packs = listOf(missing),
                    packSetup = PackSetupState(
                        packId = "words-en",
                        name = "Word meanings",
                        bytes = 6_303_744L,
                        progress = 0.25f,
                    ),
                    actions = SettingsActions(),
                )
            }
        }
        // The switch's own line and the pack row under it both say the
        // transfer, so the tap is answered where the finger is.
        compose.onAllNodesWithText("Downloading 25%").assertCountEquals(2)
    }

    @Test
    fun aLanguageWithNoListKeepsTheSwitchClosed() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                WordsPage(
                    settings = AppSettings(uiLanguage = "en", wordByWord = false),
                    packs = emptyList(),
                    packSetup = null,
                    actions = SettingsActions(),
                )
            }
        }
        compose.onNodeWithTag("switch-words-page").assertIsNotEnabled()
        compose.onNodeWithTag("pack-action-add", useUnmergedTree = true).assertDoesNotExist()
    }

    /**
     * The voices the reader downloaded: one row that says how many surahs and
     * what they weigh, and the one act they allow. The row is drawn for
     * downloads only, so a fresh page carries nothing to remove (owner
     * decision, this session).
     */
    @Test
    fun theDownloadedVoicesSayWhatTheyWeighAndCanGo() {
        var removed = 0
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                WordsPage(
                    settings = AppSettings(uiLanguage = "en", wordByWord = true),
                    packs = listOf(missing),
                    packSetup = null,
                    actions = SettingsActions(onRemoveWordVoices = { removed++ }),
                    wordVoices = { WordAudioOnDevice(surahs = 12, bytes = 39_845_888) },
                )
            }
        }
        compose.onNodeWithText("Downloaded voices (12)").assertExists()
        compose.onNodeWithText("38 MB").assertExists()
        compose.onNodeWithText("Remove").performClick()
        assertEquals("the page must ask once", 1, removed)
    }
}
