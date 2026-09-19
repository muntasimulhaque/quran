package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.StudyRow
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.rich.ArabicFonts
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import androidx.compose.ui.platform.LocalContext
import io.github.muntasimulhaque.quran.core.RichText

/** The pages the settings hub opens, one at a time. */
enum class SettingsPage { Appearance, Text, Reading, Reciters, Translations, Tafsirs, Words, Saved, About }

@Composable
fun SettingsPage.title(): String = stringResource(
    when (this) {
        SettingsPage.Appearance -> R.string.settings_title_appearance
        SettingsPage.Text -> R.string.settings_title_text
        SettingsPage.Reading -> R.string.settings_title_reading
        SettingsPage.Reciters -> R.string.settings_title_reciters
        SettingsPage.Translations -> R.string.settings_title_translations
        SettingsPage.Tafsirs -> R.string.settings_title_tafsirs
        SettingsPage.Words -> R.string.settings_title_words
        SettingsPage.Saved -> R.string.settings_title_saved
        SettingsPage.About -> R.string.settings_title_about
    },
)

/**
 * The hub: one row per category, each carrying where it stands, so a reader
 * can see their own setup at a glance and open only what they came to change.
 */
@Composable
fun SettingsHub(
    settings: AppSettings,
    packs: List<ContentPack>,
    recitations: List<Recitation>,
    savedCount: Int,
    version: String,
    onOpen: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        PageRow(
            title = stringResource(R.string.settings_title_appearance),
            summary = settings.theme.name(),
        ) { onOpen(SettingsPage.Appearance) }
        PageRow(
            title = stringResource(R.string.settings_title_text),
            summary = stringResource(
                R.string.settings_summary_text,
                settings.arabicSp.toInt(),
                settings.translationSp.toInt(),
            ),
        ) { onOpen(SettingsPage.Text) }
        PageRow(
            title = stringResource(R.string.settings_title_reading),
            summary = stringResource(
                R.string.settings_summary_reading,
                listOf(settings.keepAwake, settings.followReciter, settings.showFootnotes)
                    .count { it },
            ),
        ) { onOpen(SettingsPage.Reading) }
        PageRow(
            title = stringResource(R.string.settings_title_reciters),
            summary = reciterName(settings, packs, recitations),
        ) { onOpen(SettingsPage.Reciters) }
        PageRow(
            title = stringResource(R.string.settings_title_translations),
            summary = translationName(settings, packs),
        ) { onOpen(SettingsPage.Translations) }
        PageRow(
            title = stringResource(R.string.settings_title_tafsirs),
            summary = tafsirSummary(settings, packs),
        ) { onOpen(SettingsPage.Tafsirs) }
        PageRow(
            title = stringResource(R.string.settings_title_words),
            summary = wordsSummary(settings, packs),
        ) { onOpen(SettingsPage.Words) }
        PageRow(
            title = stringResource(R.string.settings_title_saved),
            summary = stringResource(R.string.settings_summary_saved, savedCount),
        ) { onOpen(SettingsPage.Saved) }
        PageRow(
            title = stringResource(R.string.settings_title_about),
            summary = stringResource(R.string.settings_version_short, version),
        ) { onOpen(SettingsPage.About) }
    }
}

@Composable
private fun reciterName(
    settings: AppSettings,
    packs: List<ContentPack>,
    recitations: List<Recitation>,
): String {
    val installed = recitations.any {
        ContentDatabase.reciterPack(it.id) in packs.map { pack -> pack.id } &&
            packs.any { pack -> pack.id == ContentDatabase.reciterPack(it.id) && pack.installed }
    }
    if (!installed) return stringResource(R.string.settings_none_yet)
    return recitations.firstOrNull { it.id == settings.recitation }?.name
        ?: stringResource(R.string.settings_none_yet)
}

@Composable
private fun translationName(settings: AppSettings, packs: List<ContentPack>): String {
    val chosen = packs.firstOrNull { it.id == settings.translationPack && it.installed }
    return chosen?.name ?: stringResource(R.string.settings_none_yet)
}

@Composable
private fun tafsirSummary(settings: AppSettings, packs: List<ContentPack>): String {
    val chosen = packs.filter { it.type == PackType.Tafsir && it.installed && it.id in settings.tafsirPacks }
    return when {
        chosen.isEmpty() -> stringResource(R.string.settings_none_yet)
        chosen.size == 1 -> chosen.first().name
        else -> stringResource(R.string.settings_chosen_count, chosen.size)
    }
}

@Composable
private fun wordsSummary(settings: AppSettings, packs: List<ContentPack>): String {
    val active = activeWordsPack(settings, packs) ?: return stringResource(R.string.settings_none_yet)
    if (!settings.wordByWord) return stringResource(R.string.settings_words_off)
    val language = packs.firstOrNull { it.id == active }?.language ?: return stringResource(R.string.settings_none_yet)
    return languageName(language)
}

/**
 * The word list the reading actually uses: the one that speaks the language
 * of the translation the reader chose, and English when that one is absent.
 * The language is not a choice of its own, because a meaning under an Arabic
 * word is only useful in the language the reader is reading in.
 */
internal fun activeWordsPack(settings: AppSettings, packs: List<ContentPack>): String? {
    val language = packs.firstOrNull { it.id == settings.translationPack }?.language ?: "en"
    val preferred = "words-$language"
    if (packs.any { it.id == preferred && it.installed }) return preferred
    if (packs.any { it.id == ContentDatabase.WORDS_PACK && it.installed }) return ContentDatabase.WORDS_PACK
    return null
}

/** The appearance page: the four grounds, and nothing else to decide. */
@Composable
fun AppearancePage(settings: AppSettings, onTheme: (io.github.muntasimulhaque.quran.data.AppTheme) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_theme))
        ThemeRow(settings.theme, onTheme)
        Text(
            text = stringResource(R.string.settings_theme_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp),
        )
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
    preview: suspend () -> StudyRow?,
    onSize: (TypeRole, Int) -> Unit,
) {
    val row by produceState<StudyRow?>(initialValue = null, settings.translationPack) { value = preview() }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        if (row != null) {
            SizeSample(row = row!!, settings = settings)
        }
        Group(stringResource(R.string.settings_group_sizes))
        SizeRow(TypeRole.Arabic, settings.arabicSize) { onSize(TypeRole.Arabic, it) }
        SizeRow(TypeRole.Translation, settings.translationSize) { onSize(TypeRole.Translation, it) }
        SizeRow(TypeRole.Tafsir, settings.tafsirSize) { onSize(TypeRole.Tafsir, it) }
        SizeRow(TypeRole.Words, settings.wordsSize) { onSize(TypeRole.Words, it) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SizeSample(row: StudyRow, settings: AppSettings) {
    val context = LocalContext.current
    val hafs = remember(context) { ArabicFonts(context).hafsFamily }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 4.dp),
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
        row.translation?.let { translation ->
            TranslationBody(
                runs = remember(translation.text) { RichText.footnotes(translation.text) },
                modifier = Modifier.padding(top = 10.dp),
                sizeSp = settings.translationSp,
                lineSp = settings.translationLineSp,
                arabicSp = settings.arabicSp * 0.8f,
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
fun ReadingPage(settings: AppSettings, actions: SettingsActions) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_reading))
        ToggleRow(
            title = stringResource(R.string.settings_keep_awake_title),
            subtitle = stringResource(R.string.settings_keep_awake_subtitle),
            checked = settings.keepAwake,
        ) { actions.onKeepAwake(it) }
        ToggleRow(
            title = stringResource(R.string.settings_follow_title),
            subtitle = stringResource(R.string.settings_follow_subtitle),
            checked = settings.followReciter,
        ) { actions.onFollowReciter(it) }
        ToggleRow(
            title = stringResource(R.string.settings_footnotes_title),
            subtitle = stringResource(R.string.settings_footnotes_subtitle),
            checked = settings.showFootnotes,
        ) { actions.onShowFootnotes(it) }
    }
}

/**
 * The reciters page: one row per reciter, the mark showing which one the
 * reader hears, and each row carrying its own downloaded surahs with their
 * sizes, so a reader with more than one reciter always knows what is whose.
 */
@Composable
fun RecitersPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    recitations: List<Recitation>,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_reciters))
        packs.filter { it.type == PackType.Recitation }.forEach { pack ->
            val id = pack.id.removePrefix(ContentDatabase.RECITER_PREFIX)
            val selected = pack.id == ContentDatabase.reciterPack(settings.recitation)
            ChoiceRow(
                title = recitations.firstOrNull { it.id == id }?.name ?: pack.name,
                subtitle = reciterSubtitle(pack),
                selected = selected,
                onClick = { actions.onSelectRecitation(id) },
                trailing = {
                    when {
                        packSetup?.packId == pack.id && packSetup.failed ->
                            PackActionText(stringResource(R.string.pack_action_retry)) {
                                actions.onInstallPack(pack.id)
                            }
                        packSetup?.packId == pack.id -> Text(
                            text = stringResource(
                                R.string.pack_downloading,
                                ((packSetup.progress ?: 0f) * 100).toInt(),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        !pack.installed -> PackActionText(stringResource(R.string.pack_action_add)) {
                            actions.onInstallPack(pack.id)
                        }
                        else -> PackActionText(stringResource(R.string.pack_action_remove)) {
                            actions.onRemovePack(pack.id)
                        }
                    }
                },
            )
            if (pack.installed) {
                ReciterDownloads(id, downloadedSurahs, actions)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PackActionText(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
    )
}

@Composable
private fun reciterSubtitle(pack: ContentPack): String = when {
    pack.shipped -> stringResource(R.string.pack_included)
    pack.installed -> stringResource(R.string.settings_reciter_installed, formatBytes(pack.bytes))
    else -> stringResource(R.string.settings_reciter_size, formatBytes(pack.bytes))
}

/** The downloaded surahs of one reciter, with what each one weighs. */
@Composable
private fun ReciterDownloads(
    recitation: String,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
) {
    val scope = rememberCoroutineScope()
    var open by remember(recitation) { mutableStateOf(false) }
    val downloaded by produceState(initialValue = emptyList<DownloadedSurah>(), recitation) {
        value = downloadedSurahs(recitation).sortedBy { it.surah }
    }
    Text(
        text = if (open) {
            stringResource(R.string.settings_downloaded_hide)
        } else {
            stringResource(R.string.settings_downloaded_show, downloaded.size)
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 22.dp, top = 2.dp, bottom = 4.dp)
            .clickable { open = !open }
            .padding(vertical = 6.dp),
    )
    if (!open) return
    if (downloaded.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_downloaded_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
        )
        return
    }
    downloaded.forEach { row ->
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 16.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatBytes(row.bytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PackActionText(stringResource(R.string.pack_action_remove)) {
                scope.launch { actions.onRemoveDownloads(recitation, row.surah) }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

/** Translations: one is read, and each language keeps its own list. */
@Composable
fun TranslationsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_translations))
        Text(
            text = stringResource(R.string.settings_translations_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 6.dp),
        )
        LanguageGroups(packs, PackType.Translation) { pack ->
            ChoiceRow(
                title = pack.name,
                subtitle = translationSubtitle(pack),
                selected = pack.installed && pack.id == settings.translationPack,
                onClick = { if (pack.installed) actions.onTranslationPack(pack.id) },
                trailing = { PackTrailing(pack, packSetup, actions) },
            )
        }
        Spacer(Modifier.height(12.dp))
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
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_tafsirs))
        Text(
            text = stringResource(R.string.settings_tafsirs_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 6.dp),
        )
        LanguageGroups(packs, PackType.Tafsir) { pack ->
            MarkRow(
                title = pack.name,
                subtitle = translationSubtitle(pack),
                selected = pack.installed && pack.id in settings.tafsirPacks,
                onClick = { if (pack.installed) actions.onToggleTafsir(pack.id) },
                trailing = { PackTrailing(pack, packSetup, actions) },
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Word by word aids: one per language, and the switch that shows them. */
@Composable
fun WordsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_words))
        ToggleRow(
            title = stringResource(R.string.settings_words_title),
            subtitle = stringResource(R.string.settings_words_subtitle),
            checked = settings.wordByWord,
        ) { actions.onWordByWord(it) }
        Text(
            text = stringResource(R.string.settings_words_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 4.dp),
        )
        val active = activeWordsPack(settings, packs)
        LanguageGroups(packs, PackType.Words) { pack ->
            PackRow(
                pack = pack,
                selected = pack.id == active && settings.wordByWord,
                setup = packSetup?.takeIf { it.packId == pack.id },
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
                onSelect = { actions.onWordByWord(true) },
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PackTrailing(
    pack: ContentPack,
    setup: PackSetupState?,
    actions: SettingsActions,
) {
    when {
        setup?.packId == pack.id && setup.failed ->
            PackActionText(stringResource(R.string.pack_action_retry)) { actions.onInstallPack(pack.id) }
        setup?.packId == pack.id -> Text(
            text = stringResource(R.string.pack_downloading, ((setup.progress ?: 0f) * 100).toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        !pack.installed -> PackActionText(stringResource(R.string.pack_action_add)) {
            actions.onInstallPack(pack.id)
        }
        else -> PackActionText(stringResource(R.string.pack_action_remove)) {
            actions.onRemovePack(pack.id)
        }
    }
}

@Composable
private fun translationSubtitle(pack: ContentPack): String {
    val detail = if (pack.shipped) {
        stringResource(R.string.pack_included_suffix)
    } else {
        formatBytes(pack.bytes)
    }
    return stringResource(R.string.pack_installed, languageName(pack.language), detail)
}

/** The packs of one kind, grouped by the language they speak. */
@Composable
private fun LanguageGroups(
    packs: List<ContentPack>,
    type: PackType,
    row: @Composable (ContentPack) -> Unit,
) {
    packs
        .filter { it.type == type }
        .groupBy { it.language }
        .entries
        .sortedBy { (language, _) -> languageOrder(language) }
        .forEach { (language, group) ->
            Text(
                text = languageName(language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 2.dp),
            )
            group.forEach { pack -> row(pack) }
        }
}

/** Where the reader's own work lives, and how to carry it to a new phone. */
@Composable
fun SavedPage(
    savedCount: Int,
    dataNotice: DataNotice?,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_saved))
        ValueRow(
            stringResource(R.string.settings_saved_count),
            stringResource(R.string.settings_summary_saved, savedCount),
        )
        TextRow(
            title = stringResource(R.string.settings_export),
            subtitle = stringResource(R.string.settings_export_subtitle),
            onClick = actions.onExport,
        )
        TextRow(
            title = stringResource(R.string.settings_import),
            subtitle = stringResource(R.string.settings_import_subtitle),
            onClick = actions.onImport,
        )
        dataNotice?.let { notice ->
            Text(
                text = notice.text(savedCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** The app itself: its version, its credits, and a way to check its content. */
@Composable
fun AboutPage(
    version: String,
    contentCheck: ContentCheck?,
    onCredits: () -> Unit,
    onCheckContent: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_about))
        ValueRow(stringResource(R.string.settings_version), version)
        TextRow(title = stringResource(R.string.settings_credits), onClick = onCredits)
        Group(stringResource(R.string.settings_group_content))
        TextRow(
            title = stringResource(R.string.settings_check_content),
            subtitle = stringResource(R.string.settings_check_content_subtitle),
            onClick = onCheckContent,
        )
        contentCheck?.let { check ->
            Text(
                text = when (check) {
                    ContentCheck.Running -> stringResource(R.string.settings_check_running)
                    is ContentCheck.Done -> if (check.damaged.isEmpty()) {
                        stringResource(R.string.settings_check_ok)
                    } else {
                        stringResource(
                            R.string.settings_check_damaged,
                            check.damaged.joinToString(", "),
                        )
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * The order languages appear in: the interface's own language first, then the
 * language of the Quran's revelation, then the rest by name.
 */
internal fun languageOrder(language: String): String = when (language) {
    "en" -> "0"
    "ar" -> "1"
    else -> "2" + languageName(language)
}

@Composable
private fun DataNotice.text(savedCount: Int): String = when (this) {
    DataNotice.Exported -> stringResource(R.string.settings_export_done, savedCount)
    is DataNotice.Imported -> stringResource(R.string.settings_import_done, count)
    DataNotice.NothingToExport -> stringResource(R.string.settings_export_empty)
    DataNotice.NothingImported -> stringResource(R.string.settings_import_none)
    DataNotice.ImportFailed -> stringResource(R.string.settings_import_failed)
}
