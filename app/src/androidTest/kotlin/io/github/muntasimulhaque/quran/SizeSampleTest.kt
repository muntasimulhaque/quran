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
 * The Font size sample is the type specimen for the Translation row: the
 * ayah and its lead translation, with no pack name above it and no reading
 * switch of its own.
 *
 * The sample used to draw the first translation the catalog held, and the
 * catalog sorts by pack name: Al-Bayan stood before Saheeh, so an English
 * reader's sample was Bangla. The lead now follows the reader's language,
 * and the sample draws it alone (owner decision, this session).
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
        waitFor(ENGLISH_LINE)
        compose.onNodeWithText(ENGLISH_LINE).assertExists()
        compose.onAllNodesWithText(BANGLA_LINE).assertCountEquals(0)
        compose.onAllNodesWithText("Saheeh International").assertCountEquals(0)
        compose.onAllNodesWithText("Al-Bayan Simple Translation").assertCountEquals(0)
    }

    @Test
    fun banglaLeadsWhenBothAreOn() {
        show("bn", listOf(bangla, english))
        waitFor(BANGLA_LINE)
        compose.onNodeWithText(BANGLA_LINE).assertExists()
        compose.onAllNodesWithText(ENGLISH_LINE).assertCountEquals(0)
        compose.onAllNodesWithText("Saheeh International").assertCountEquals(0)
        compose.onAllNodesWithText("Al-Bayan Simple Translation").assertCountEquals(0)
    }

    @Test
    fun aForeignLineIsDrawnWithoutAName() {
        // The sample is type, not a reading: the Bangla line is drawn alone
        // under an English interface and says nothing about its pack.
        show("en", listOf(bangla))
        waitFor(BANGLA_LINE)
        compose.onNodeWithText(BANGLA_LINE).assertExists()
        compose.onAllNodesWithText("Al-Bayan Simple Translation").assertCountEquals(0)
    }

    @Test
    fun theSampleNeverNamesTheLine() {
        show("en", listOf(english))
        waitFor(ENGLISH_LINE)
        compose.onAllNodesWithText("Saheeh International").assertCountEquals(0)
    }

    @Test
    fun theSwitchDoesNotHideTheTypeSpecimen() {
        // The reading's Show translation switch is not the sample's: the
        // reader opened this page to judge the size of the prose, so the
        // sentence is here either way.
        show("en", listOf(english, bangla), showTranslation = false)
        waitFor(ENGLISH_LINE)
        compose.onNodeWithText(ENGLISH_LINE).assertExists()
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
