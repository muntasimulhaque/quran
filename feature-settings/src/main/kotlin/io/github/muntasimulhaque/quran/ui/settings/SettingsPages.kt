package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.kit.languageChoiceName
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.rich.ArabicFonts
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.core.RichText

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
        Group(stringResource(R.string.settings_group_language))
        // A list of choices is read, not searched: the languages are sorted by
        // the name the interface itself shows them under, so Bangla sits above
        // English in an English interface and each language's own alphabet is
        // respected in its own interface. Each row is named in the language's
        // own script first, which the reader must recognise before they can
        // read anything else here, with the English name beside it so the two
        // names are never a guess.
        val entries = UiLanguage.entries.map { it to languageName(it.tag) }
        entries.sortedBy { it.second.lowercase() }.forEach { (language, _) ->
            ChoiceRow(
                title = languageChoiceName(language.tag),
                subtitle = null,
                selected = settings.uiLanguage == language.tag,
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
    val context = LocalContext.current
    val hafs = remember(context) { ArabicFonts(context).hafsFamily }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Row, bottom = Space.Tight),
    ) {
        Text(
            text = row.ayah.text,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = hafs,
                fontSize = settings.arabicSp.sp,
                lineHeight = settings.arabicLineSp.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
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

/**
 * Translations: more than one may be on, and each one reads in its own column
 * under the ayah, so the mark is a check rather than a single choice. The
 * first one turned on is the one search and the ayah card read by default.
 */
@Composable
fun TranslationsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        // The master switch lives on the hub row and nowhere else: this page
        // is the list of translations, and a switch at its head re-asked a
        // decision the hub already owns (owner report). The line below
        // only says what the reading is doing while the switch is off, so a
        // reader standing here is never left to wonder why nothing changed.
        if (!settings.showTranslation) {
            Text(
                text = stringResource(R.string.settings_translation_hidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.Line),
            )
        }
        LanguageGroups(
            packs = packs,
            type = PackType.Translation,
        ) { pack ->
            PackChoiceRow(
                pack = pack,
                subtitle = translationSubtitle(pack),
                selected = pack.installed && pack.id in settings.translationPacks,
                radio = false,
                setup = packSetup?.takeIf { it.packId == pack.id },
                onActivate = { actions.onToggleTranslation(pack.id) },
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
                onCancel = actions.onPackSetupCancel,
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/** Tafsirs: as many as the reader wants, each behind its own door. */
@Composable
fun TafsirsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        // The same rule as the translations list: the hub row owns the
        // switch, and this page owns the list (owner report). The line
        // says what the reading is doing while the switch is off.
        if (!settings.showTafsir) {
            Text(
                text = stringResource(R.string.settings_tafsir_hidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.Line),
            )
        }
        LanguageGroups(
            packs = packs,
            type = PackType.Tafsir,
        ) { pack ->
            PackChoiceRow(
                pack = pack,
                subtitle = translationSubtitle(pack),
                selected = pack.installed && pack.id in settings.tafsirPacks,
                radio = false,
                setup = packSetup?.takeIf { it.packId == pack.id },
                onActivate = { actions.onToggleTafsir(pack.id) },
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
                onCancel = actions.onPackSetupCancel,
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/** How the catalog joins the licenses of one pack's datasets. */
private const val LICENSE_SEPARATOR = " · "

/**
 * The app itself: its version, who made what it carries and under what terms.
 *
 * The credits stand here, on the page, rather than behind a row that opens
 * them (owner decision, this session): every license is honored by being read
 * where a reader already is, not by a door they have to think to open, and a
 * reader who wonders who made the text meets the answer the moment they open
 * About.
 *
 * The page is facts only: every row is read and nothing opens (owner
 * decision). Corrections and rights requests travel through the store
 * listing's contact and the project repository rather than through the app.
 *
 * The content is checked on the reader's behalf, once a day, without being
 * asked; there is no row for it here, because a reader should never have to
 * know the word for it (owner decision, this session).
 */
@Composable
fun AboutPage(
    packs: List<ContentPack>,
    version: String,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_about))
        ValueRow(stringResource(R.string.settings_version), version)
        ValueRow(stringResource(R.string.settings_license_label), stringResource(R.string.about_license))

        Group(stringResource(R.string.about_group_content))
        // What is on the device is what the page names: every license is
        // honored where the reader already meets the content it covers, and
        // a catalog of packs nobody installed is a list of other people's
        // books (owner decision, this session).
        packs.filter { it.shipped || it.installed }.forEach { pack ->
            AboutCredit(
                title = stringResource(
                    R.string.about_pack_title,
                    pack.name,
                    languageName(pack.language),
                ),
                lines = listOf(pack.credit) +
                    pack.license.split(LICENSE_SEPARATOR)
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
            )
        }

        Group(stringResource(R.string.about_group_fonts))
        AboutCredit(
            stringResource(R.string.credit_page_fonts),
            listOf(
                stringResource(R.string.credit_page_fonts_by),
                stringResource(R.string.credit_page_fonts_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_amiri),
            listOf(
                stringResource(R.string.credit_amiri_by),
                stringResource(R.string.credit_amiri_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_literata),
            listOf(
                stringResource(R.string.credit_literata_by),
                stringResource(R.string.credit_literata_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_noto_serif_bengali),
            listOf(
                stringResource(R.string.credit_noto_serif_bengali_by),
                stringResource(R.string.credit_noto_serif_bengali_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_inter),
            listOf(
                stringResource(R.string.credit_inter_by),
                stringResource(R.string.credit_inter_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_noto_sans_bengali),
            listOf(
                stringResource(R.string.credit_noto_sans_bengali_by),
                stringResource(R.string.credit_noto_sans_bengali_use),
            ),
        )

        Group(stringResource(R.string.about_group_recitations))
        AboutCredit(
            stringResource(R.string.credit_minshawi),
            listOf(
                stringResource(R.string.credit_minshawi_by),
                stringResource(R.string.credit_minshawi_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_husary),
            listOf(
                stringResource(R.string.credit_husary_by),
                stringResource(R.string.credit_husary_use),
            ),
        )

        Spacer(Modifier.height(Space.Section))
    }
}

/** One credit: its name, and the lines that say who made it and what for. */
@Composable
private fun AboutCredit(title: String, lines: List<String>) {
    Column(Modifier.padding(start = Space.Gutter, end = Space.Gutter, bottom = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}



