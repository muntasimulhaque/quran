package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.ui.settings.SettingsHub
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The settings hub's four reading switches are one run, in the order the page
 * draws them: the tajweed colors, which are the Arabic line itself, then the
 * word meanings under it, then the translation, then the tafsir behind the
 * ayah card.
 *
 * The four are the reader's answer to one question, what the reading shows,
 * and a reader checks them against the page top to bottom. The colors
 * arrived after the other three and landed between the meanings and the
 * translation, so the group broke in the middle and the one row that draws
 * the Quran's own line was filed with the two that add prose under it (owner
 * decision, the reading switches session). The order is measured here so a
 * later row edit cannot quietly split the group again.
 */
@RunWith(AndroidJUnit4::class)
class SettingsRowOrderTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theReadingSwitchesRunFromTheLineToTheProse() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SettingsHub(
                        settings = AppSettings(uiLanguage = "en", wordByWord = true),
                        packs = emptyList(),
                        recitations = emptyList(),
                        version = "1.0",
                        notificationsBlocked = false,
                        exactGrantWithheld = false,
                        packSetup = null,
                        onShowTranslation = {},
                        onShowTafsir = {},
                        onWordByWord = {},
                        onTajweed = {},
                        onDailyAyah = {},
                        onOpen = {},
                    )
                }
            }
        }
        val tajweed = top("switch-tajweed")
        val words = top("switch-words")
        val translation = top("switch-translation")
        val tafsir = top("switch-tafsir")
        assertTrue(
            "the tajweed colors are the Arabic line, so they stand above the meanings " +
                "(tajweed at $tajweed, meanings at $words)",
            tajweed < words,
        )
        assertTrue(
            "the meanings sit closest to the Arabic, so they stand above the translation " +
                "(meanings at $words, translation at $translation)",
            words < translation,
        )
        assertTrue(
            "the translation stands above the tafsir (translation at $translation, tafsir at " +
                "$tafsir)",
            translation < tafsir,
        )
    }

    /** Where one switch stands on the page, from the top of the sheet. */
    private fun top(switchTag: String): Float = compose
        .onNodeWithTag(switchTag, useUnmergedTree = true)
        .getUnclippedBoundsInRoot()
        .top
        .value
}
