package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.data.TranslationLine
import io.github.muntasimulhaque.quran.data.TranslationText
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.settings.TextPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Font size sample shows the line the reading leads with, and says whose
 * line it is when the one rule calls for a name.
 *
 * The sample used to draw the first translation the catalog held, and the
 * catalog sorts by pack name: Al-Bayan stood before Saheeh, so an English
 * reader's sample was Bangla with no name on it (owner report, this session).
 * The row is built here in the order the reading builds it, so the sample's
 * own choice is one line and its name is the other.
 */
@RunWith(AndroidJUnit4::class)
class SizeSampleTest {

    @get:Rule
    val compose = createComposeRule()

    private val english = line(
        pack = "translation-saheeh-en",
        language = "en",
        text = ENGLISH_LINE,
    )
    private val bangla = line(
        pack = "translation-al-bayan-bn",
        language = "bn",
        text = BANGLA_LINE,
    )

    @Test
    fun englishLeadsWhenBothAreOn() {
        show("en", listOf(english, bangla))
        waitFor("Saheeh International")
        compose.onNodeWithText(ENGLISH_LINE).assertExists()
        compose.onAllNodesWithText("Al-Bayan Simple Translation").assertCountEquals(0)
        compose.onAllNodesWithText(BANGLA_LINE).assertCountEquals(0)
    }

    @Test
    fun banglaLeadsWhenBothAreOn() {
        show("bn", listOf(bangla, english))
        waitFor("Al-Bayan Simple Translation")
        compose.onNodeWithText(BANGLA_LINE).assertExists()
        compose.onAllNodesWithText("Saheeh International").assertCountEquals(0)
        compose.onAllNodesWithText(ENGLISH_LINE).assertCountEquals(0)
    }

    @Test
    fun aForeignLineIsNamedWhenItIsTheOnlyOne() {
        // The report itself: one Bangla column under an English interface,
        // with nothing on the glass to say whose line it was.
        show("en", listOf(bangla))
        waitFor("Al-Bayan Simple Translation")
        compose.onNodeWithText(BANGLA_LINE).assertExists()
    }

    @Test
    fun oneLineInTheReadersOwnLanguageStaysUnnamed() {
        show("en", listOf(english))
        waitFor(ENGLISH_LINE)
        compose.onAllNodesWithText("Saheeh International").assertCountEquals(0)
    }

    @Test
    fun theSwitchHidesTheLineAndItsName() {
        show("en", listOf(english, bangla), showTranslation = false)
        compose.waitForIdle()
        compose.onAllNodesWithText(ENGLISH_LINE).assertCountEquals(0)
        compose.onAllNodesWithText("Saheeh International").assertCountEquals(0)
    }

    private fun show(
        language: String,
        translations: List<TranslationLine>,
        showTranslation: Boolean = true,
    ) {
        val settings = AppSettings(
            uiLanguage = language,
            translationPacks = translations.map { it.pack }.toSet(),
            showTranslation = showTranslation,
        )
        val row = ReadingRow(
            ayah = Ayah(
                number = 1,
                surah = 1,
                ayah = 1,
                verseKey = "1:1",
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            ),
            words = emptyList(),
            translations = translations,
        )
        compose.setContent {
            QuranTheme(
                theme = AppTheme.Light,
                language = UiLanguage.of(language) ?: UiLanguage.English,
            ) {
                TextPage(
                    settings = settings,
                    preview = { row },
                    onSize = { _, _ -> },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun waitFor(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun line(pack: String, language: String, text: String) = TranslationLine(
        pack = pack,
        packName = pack,
        language = language,
        text = TranslationText(text, emptyList()),
    )

    private companion object {
        const val ENGLISH_LINE = "The English line."
        const val BANGLA_LINE = "The Bangla line."
    }
}
