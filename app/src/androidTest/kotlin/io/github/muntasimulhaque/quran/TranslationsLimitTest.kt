package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.settings.SettingsActions
import io.github.muntasimulhaque.quran.ui.settings.TranslationsPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The translations page holds four columns, and says so where the fifth waits.
 *
 * Four is the number that leaves SQLite's ten attachments room for the page's
 * other packs (owner decision, 20.5). The fifth switch is drawn and named and
 * quiet, and one line above the list says why; a chosen column still answers,
 * so one turned off gives the next its place. The page is composed on its own,
 * with the packs handed in, so the leg needs no device library.
 */
@RunWith(AndroidJUnit4::class)
class TranslationsLimitTest {

    @get:Rule
    val compose = createComposeRule()

    private fun translation(id: String) = ContentPack(
        id = id,
        type = PackType.Translation,
        name = id,
        language = "en",
        credit = "",
        license = "",
        version = "1",
        shipped = true,
        ayahs = 6236,
        bytes = 1_000_000,
        installed = true,
    )

    private fun page(selected: Set<String>, packs: List<ContentPack>) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                TranslationsPage(
                    settings = AppSettings(uiLanguage = "en", translationPacks = selected),
                    packs = packs,
                    packSetup = null,
                    actions = SettingsActions(),
                )
            }
        }
    }

    @Test
    fun theFifthColumnWaitsWhileThePageHoldsFour() {
        val packs = (1..5).map { translation("translation-$it") }
        page((1..4).map { "translation-$it" }.toSet(), packs)
        compose.onNodeWithTag("translations-full").assertExists()
        compose.onNodeWithTag("pack-choice-translation-5").assertIsNotEnabled()
        // A chosen column still answers, which is how a place is made.
        compose.onNodeWithTag("pack-choice-translation-1").assertIsEnabled()
    }

    @Test
    fun aPageWithRoomOffersTheNextColumn() {
        val packs = (1..4).map { translation("translation-$it") }
        page(setOf("translation-1", "translation-2", "translation-3"), packs)
        compose.onNodeWithTag("translations-full").assertDoesNotExist()
        compose.onNodeWithTag("pack-choice-translation-4").assertIsEnabled()
    }
}
