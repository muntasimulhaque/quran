package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.rememberHafs
import io.github.muntasimulhaque.quran.ui.kit.languageChoiceName
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The language page: the interface's language, and with it the language of
 * the translation, the tafsir, and the word meanings. Each choice is named
 * in its own script, because a reader who cannot read the current interface
 * must still be able to find their own language on this page.
 */
@Composable
fun LanguagePage(
    settings: AppSettings,
    onLanguage: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        // A list of choices is read, not searched: the languages are sorted by
        // the name the interface itself shows them under, so Bangla sits above
        // English in an English interface and each language's own alphabet is
        // respected in its own interface. Each row is named in the language's
        // own script first, which the reader must recognise before they can
        // read anything else here, with the English name beside it so the two
        // names are never a guess.
        //
        // The page title already names this list, so it carries no heading of
        // its own (owner decision, this session). The ring stands on the
        // default, English, when nothing is stored yet, the same default the
        // hub row names (owner decision, this session).
        val chosen = settings.uiLanguage ?: UiLanguage.English.tag
        val entries = UiLanguage.entries.map { it to languageName(it.tag) }
        entries.sortedBy { it.second.lowercase() }.forEach { (language, _) ->
            ChoiceRow(
                title = languageChoiceName(language.tag),
                subtitle = null,
                selected = chosen == language.tag,
                radio = true,
                onClick = { onLanguage(language.tag) },
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The appearance page: the three grounds, as one list. Light, Dark, and
 * System, which follows the phone's own day and night. The ring stands on
 * the stored choice (owner decision): with System chosen the ring is on
 * System on every phone, because the ring says what the reader chose and
 * the phone itself says what is on screen.
 * The switch that used to stand under the swatches is gone: System is the
 * third swatch, so following the phone is one tap in the same list as the two
 * fixed grounds, and a reader never reconciles a swatch with a toggle (owner
 * decision, this session).
 */
@Composable
fun AppearancePage(
    settings: AppSettings,
    onTheme: (AppTheme) -> Unit,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        ThemeRow(selected = settings.theme, onSelect = onTheme)
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The text page: each kind of text has its own size, and the sample above
 * them is the reader's own ayah, so a change is judged on the page it is
 * about to change.
 */
@Composable
fun TextPage(
    settings: AppSettings,
    preview: suspend () -> ReadingRow?,
    onSize: (TypeRole, Float) -> Unit,
) {
    val row by produceState<ReadingRow?>(initialValue = null, settings.translationPacks) { value = preview() }
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        row?.let { sample -> SizeSample(row = sample, settings = settings) }
        SizeRow(TypeRole.Arabic, settings.arabicSize) { onSize(TypeRole.Arabic, it) }
        SizeRow(TypeRole.Translation, settings.translationSize) { onSize(TypeRole.Translation, it) }
        SizeRow(TypeRole.Tafsir, settings.tafsirSize) { onSize(TypeRole.Tafsir, it) }
        SizeRow(TypeRole.Words, settings.wordsSize) { onSize(TypeRole.Words, it) }
        Spacer(Modifier.height(Space.Section))
    }
}

@Composable
private fun SizeSample(row: ReadingRow, settings: AppSettings) {
    val hafs = rememberHafs()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // The sample is its own block, not the first row's label: the
            // page keeps a block's gap under it and a block's gap between
            // every two rows, so the four controls read as four blocks under
            // one page of text (owner decision, this session).
            .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Row, bottom = Space.Block),
    ) {
        val palette = LocalPagePalette.current
        TajweedText(
            text = AnnotatedString(row.ayah.text),
            code = row.ayah.tajweed,
            colors = palette.tajweed,
            enabled = settings.tajweed,
            style = arabicStyle(hafs, settings.arabicSp, settings.arabicLineSp, palette.ink),
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth(),
        )
        row.translations.firstOrNull()?.let { line ->
            TranslationBody(
                runs = remember(line.text.text) { RichText.footnotes(line.text.text) },
                modifier = Modifier.padding(top = Space.Line),
                sizeSp = settings.translationSp,
                lineSp = settings.translationLineSp,
                arabicSp = settings.arabicInlineSp,
            )
        }
    }
}
