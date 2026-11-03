package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.settings.PackChoiceRow
import io.github.muntasimulhaque.quran.ui.settings.SettingsActions
import io.github.muntasimulhaque.quran.ui.settings.TranslationsPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A pack's row tells the reader's choice apart from the file's presence.
 *
 * The 14.6 report: a Bangla translation the owner had chosen came back under
 * the ayah on the first launch after an update, and the Settings page had
 * drawn it as Add, so nothing on the glass said the choice still stood. The
 * row drew a chosen pack that was not on the device exactly like one that was
 * never chosen, and the reading obeyed the stored choice the moment the file
 * was attached again. These legs pin the two halves: the row wears the app's
 * mark and says why it still asks to Add, and the page's switch follows the
 * choice rather than the file (owner report, this session).
 */
@RunWith(AndroidJUnit4::class)
class PackRowStateTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun aChosenPackThatIsNotHereStillReadsChosen() {
        val absent = absentBayan()
        val title = context.getString(
            io.github.muntasimulhaque.quran.uikit.R.string.pack_name_al_bayan,
        )
        val chosenMissing = context.getString(
            io.github.muntasimulhaque.quran.feature.settings.R.string.settings_pack_chosen_missing,
        )
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                Column {
                    // The reader's choice, and the file is not here.
                    PackChoiceRow(
                        pack = absent,
                        subtitle = "5.4 MB",
                        selected = true,
                        radio = false,
                        setup = null,
                        onActivate = {},
                        onInstall = {},
                        onRemove = {},
                        onCancel = {},
                    )
                    // The same pack, never chosen.
                    PackChoiceRow(
                        pack = absent,
                        subtitle = "5.4 MB",
                        selected = false,
                        radio = false,
                        setup = null,
                        onActivate = {},
                        onInstall = {},
                        onRemove = {},
                        onCancel = {},
                    )
                }
            }
        }
        // One mark, on the chosen row, though neither file is here.
        compose.onAllNodesWithTag("choice-mark", useUnmergedTree = true).assertCountEquals(1)
        // The chosen row says why it still asks to Add; the row that was
        // never chosen says nothing about a choice.
        compose.onAllNodesWithText(chosenMissing, substring = true).assertCountEquals(1)
        compose.onAllNodesWithText(title).assertCountEquals(2)
        // Both rows are still the Add door: the mark and its words carry the
        // choice, and the word carries the act.
        compose.onAllNodesWithText("Add").assertCountEquals(2)
    }

    @Test
    fun theSwitchFollowsTheChoiceWhenThePackIsNotHere() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                TranslationsPage(
                    settings = AppSettings(
                        uiLanguage = "en",
                        translationPacks = setOf("translation-al-bayan-bn"),
                        showTranslation = true,
                    ),
                    packs = listOf(absentBayan()),
                    packSetup = null,
                    actions = SettingsActions(),
                )
            }
        }
        // The reader's answer stands even though the file is not here: the
        // switch reads on and cannot be used until a translation arrives.
        compose.onNodeWithText("Show translation").assertIsOn().assertIsNotEnabled()
    }

    /** The Bangla translation, chosen by the reader and not on the device. */
    private fun absentBayan() = ContentPack(
        id = "translation-al-bayan-bn",
        type = PackType.Translation,
        name = "Al-Bayan Simple Translation",
        language = "bn",
        credit = "test",
        license = "test",
        version = "1.0.1",
        shipped = false,
        ayahs = 6236,
        bytes = 5_652_480,
        installed = false,
    )
}
