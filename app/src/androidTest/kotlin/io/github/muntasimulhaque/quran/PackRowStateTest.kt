package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.Assert.assertEquals
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
 * choice rather than the file.
 *
 * The 14.8 report: after Remove the check vanished and the mark column stood
 * empty, and the Add word sat 14 dp outside the page's own line while Remove
 * sat on the choice row's rail. The row now draws the off ring on a pack that
 * was never chosen, its Add wears the same pill and the same trailing rail as
 * Remove, and that off row is the checkbox it looks like. The chosen-but-missing
 * row stays a button, because its only act is Add (owner report, this
 * session).
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
        // Two marks: the chosen row's check and the off ring on the row that
        // was never chosen. The column is a state, not a space that opens only
        // when the file is here.
        compose.onAllNodesWithTag("choice-mark", useUnmergedTree = true).assertCountEquals(2)
        // Exactly one of them is a check; the other is the off ring.
        compose.onAllNodesWithTag("choice-check", useUnmergedTree = true).assertCountEquals(1)
        // The chosen row says why it still asks to Add; the row that was
        // never chosen says nothing about a choice.
        compose.onAllNodesWithText(chosenMissing, substring = true).assertCountEquals(1)
        compose.onAllNodesWithText(title).assertCountEquals(2)
        // The off choice is a real checkbox; checking it is the Add. The
        // chosen row is a button because a checked choice whose file is gone
        // can only be added, not unchecked.
        compose.onNode(hasText(title) and isSelectable()).assertIsNotSelected()
        // Both rows are still the Add door: the mark and its words carry the
        // choice, and the word carries the act.
        compose.onAllNodesWithText("Add").assertCountEquals(2)
    }

    /**
     * The two states of one row end on one line.
     *
     * Add and Remove are the same act slot in the same list. Add used to be a
     * bare word at the sheet's edge while Remove was a pill on the choice
     * row's rail, so the eye read two kinds of action and the Add stood
     * outside the page's word line (owner report, an earlier session).
     * The test reads the pills themselves, not their words: a word's ink edge
     * depends on its letters, a shape's edge does not.
     */
    @Test
    fun theTwoPackActionsEndOnTheSameLine() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                Column {
                    PackChoiceRow(
                        pack = absentBayan(),
                        subtitle = "5.4 MB",
                        selected = false,
                        radio = false,
                        setup = null,
                        onActivate = {},
                        onInstall = {},
                        onRemove = {},
                        onCancel = {},
                    )
                    PackChoiceRow(
                        pack = presentSaheeh(),
                        subtitle = "2.8 MB",
                        selected = true,
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
        val add = compose.onNodeWithTag("pack-action-add", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val remove = compose.onNodeWithTag("pack-action-remove", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(
            "the Add and Remove shapes must end on one line",
            remove.right.value,
            add.right.value,
            0.5f,
        )
        assertEquals(
            "the two actions must wear one shape",
            (remove.bottom - remove.top).value,
            (add.bottom - add.top).value,
            0.5f,
        )
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

    /** An English translation that is here and chosen, with its Remove. */
    private fun presentSaheeh() = ContentPack(
        id = "translation-saheeh-en",
        type = PackType.Translation,
        name = "Saheeh International",
        language = "en",
        credit = "test",
        license = "test",
        version = "1.1.2+2",
        shipped = false,
        ayahs = 6236,
        bytes = 2_936_013,
        installed = true,
    )
}
