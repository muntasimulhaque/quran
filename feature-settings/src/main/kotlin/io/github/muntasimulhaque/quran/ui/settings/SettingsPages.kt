package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import io.github.muntasimulhaque.quran.ui.kit.languageSortKey
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.rich.ArabicFonts
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.theme.Space
import androidx.compose.ui.platform.LocalContext
import io.github.muntasimulhaque.quran.core.RichText
/** The pages the settings hub opens, one at a time. */
enum class SettingsPage { Appearance, FontSize, Reading, Reciters, Translations, Tafsirs, Words, About }

@Composable
fun SettingsPage.title(): String = stringResource(
    when (this) {
        SettingsPage.Appearance -> R.string.settings_title_appearance
        SettingsPage.FontSize -> R.string.settings_title_text
        SettingsPage.Reading -> R.string.settings_title_reading
        SettingsPage.Reciters -> R.string.settings_title_reciters
        SettingsPage.Translations -> R.string.settings_title_translations
        SettingsPage.Tafsirs -> R.string.settings_title_tafsirs
        SettingsPage.Words -> R.string.settings_title_words
        SettingsPage.About -> R.string.settings_title_about
    },
)

/**
 * The page a choice reads as, with the automatic switch said in the same
 * breath, so the hub row never claims a page the reader is not on.
 */
@Composable
private fun themeSummary(settings: AppSettings): String {
    val name = settings.theme.name()
    return if (settings.autoNight) {
        stringResource(R.string.settings_summary_theme_auto, name)
    } else {
        name
    }
}

/**
 * The hub: one row per category, each carrying where it stands, so a reader
 * can see their own setup at a glance and open only what they came to change.
 */
@Composable
fun SettingsHub(
    settings: AppSettings,
    packs: List<ContentPack>,
    recitations: List<Recitation>,
    version: String,
    onOpen: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        PageRow(
            title = stringResource(R.string.settings_title_appearance),
            summary = themeSummary(settings),
        ) { onOpen(SettingsPage.Appearance) }
        PageRow(
            title = stringResource(R.string.settings_title_text),
            summary = stringResource(
                R.string.settings_summary_text,
                settings.arabicSp.toInt(),
                settings.translationSp.toInt(),
            ),
        ) { onOpen(SettingsPage.FontSize) }
        PageRow(
            title = stringResource(R.string.settings_title_reading),
            summary = stringResource(
                R.string.settings_summary_reading,
                listOf(settings.keepAwake, settings.followReciter).count { it },
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
    val chosen = ContentDatabase.reciterPack(settings.recitation)
    if (packs.none { it.id == chosen && it.installed }) return stringResource(R.string.settings_none_yet)
    return recitations.firstOrNull { it.id == settings.recitation }?.name
        ?: stringResource(R.string.settings_none_yet)
}

@Composable
private fun translationName(settings: AppSettings, packs: List<ContentPack>): String {
    val chosen = packs.filter { it.id in settings.translationPacks && it.installed }
    return when {
        chosen.isEmpty() -> stringResource(R.string.settings_none_yet)
        chosen.size == 1 -> chosen.first().name
        else -> stringResource(R.string.settings_chosen_count, chosen.size)
    }
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
    val chosen = packs.firstOrNull { it.id in settings.translationPacks && it.installed }
    val language = chosen?.language ?: "en"
    val preferred = "words-$language"
    if (packs.any { it.id == preferred && it.installed }) return preferred
    if (packs.any { it.id == ContentDatabase.WORDS_PACK && it.installed }) return ContentDatabase.WORDS_PACK
    return null
}

/**
 * The appearance page: the four grounds, and the switch that lets the system
 * choose between the day and the night halves of them.
 */
@Composable
fun AppearancePage(
    settings: AppSettings,
    onTheme: (io.github.muntasimulhaque.quran.data.AppTheme) -> Unit,
    onAutoNight: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_theme))
        ThemeRow(settings.theme, onTheme)
        // The swatches and the switch are two separate decisions, and the
        // switch is not a fifth swatch: the break between them says so.
        Spacer(Modifier.height(Space.Section))
        ToggleRow(
            title = stringResource(R.string.settings_auto_night_title),
            subtitle = stringResource(R.string.settings_auto_night_subtitle),
            checked = settings.autoNight,
        ) { onAutoNight(it) }
        Text(
            text = stringResource(
                if (settings.autoNight) {
                    R.string.settings_theme_note_auto
                } else {
                    R.string.settings_theme_note
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = 22.dp,
                end = 22.dp,
                top = Space.Block,
                bottom = Space.Section,
            ),
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
    onSize: (TypeRole, Float) -> Unit,
) {
    val row by produceState<StudyRow?>(initialValue = null, settings.translationPacks) { value = preview() }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        if (row != null) {
            SizeSample(row = row!!, settings = settings)
        }
        Group(stringResource(R.string.settings_group_sizes))
        SizeRow(TypeRole.Arabic, settings.arabicSize) { onSize(TypeRole.Arabic, it) }
        SizeRow(TypeRole.Translation, settings.translationSize) { onSize(TypeRole.Translation, it) }
        SizeRow(TypeRole.Tafsir, settings.tafsirSize) { onSize(TypeRole.Tafsir, it) }
        SizeRow(TypeRole.Words, settings.wordsSize) { onSize(TypeRole.Words, it) }
        Spacer(Modifier.height(Space.Section))
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
        row.translations.firstOrNull()?.let { line ->
            TranslationBody(
                runs = remember(line.text.text) { RichText.footnotes(line.text.text) },
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
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_reciters))
        Text(
            text = stringResource(R.string.settings_reciters_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = Space.Line),
        )
        packs.filter { it.type == PackType.Recitation }
            .sortedBy { it.name.lowercase() }
            .forEach { pack ->
                val id = pack.id.removePrefix(ContentDatabase.RECITER_PREFIX)
                val selected = pack.id == ContentDatabase.reciterPack(settings.recitation)
                PackChoiceRow(
                    pack = pack,
                    subtitle = reciterSubtitle(pack),
                    selected = selected,
                    radio = true,
                    setup = packSetup?.takeIf { it.packId == pack.id },
                    onActivate = { actions.onSelectRecitation(id) },
                    onInstall = { actions.onInstallPack(pack.id) },
                    onRemove = { actions.onRemovePack(pack.id) },
                )
                if (pack.installed) {
                    ReciterDownloads(id, downloadedSurahs, actions)
                }
                Spacer(Modifier.height(Space.Line))
            }
        Spacer(Modifier.height(Space.Section))
    }
}

@Composable
private fun reciterSubtitle(pack: ContentPack): String = when {
    pack.shipped -> stringResource(R.string.pack_included)
    pack.installed -> stringResource(R.string.settings_reciter_installed, formatBytes(pack.bytes))
    else -> stringResource(R.string.settings_reciter_size, formatBytes(pack.bytes))
}

/**
 * The downloaded surahs of one reciter, with what each one weighs. A surah
 * removed disappears from the list in the same frame, because the list the
 * reader sees is the list the removal changed: a row that stayed after its
 * Remove was tapped would read as a button that did nothing.
 *
 * The block is indented under the reciter's own name and its label carries a
 * disclosure arrow, so the surahs read as belonging to the reciter above them
 * rather than to the list at large. The label is a door: it opens and closes,
 * and it says so with the arrow the rest of the app uses for a door.
 */
@Composable
private fun ReciterDownloads(
    recitation: String,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
) {
    val scope = rememberCoroutineScope()
    var open by remember(recitation) { mutableStateOf(false) }
    var downloaded by remember(recitation) { mutableStateOf<List<DownloadedSurah>?>(null) }
    LaunchedEffect(recitation) {
        downloaded = downloadedSurahs(recitation).sortedBy { it.surah }
    }
    val rows = downloaded ?: return
    ReciterDownloadsDoor(open = open, count = rows.size) { open = !open }
    if (!open) return
    if (rows.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_downloaded_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 50.dp, end = 22.dp, bottom = Space.Block),
        )
        return
    }
    rows.forEach { row ->
        DownloadedSurahRow(row) {
            scope.launch {
                actions.onRemoveDownloads(recitation, row.surah)
                // Whatever the removal did, the list is read from the device
                // again: the row goes when its files are gone.
                downloaded = downloadedSurahs(recitation).sortedBy { it.surah }
            }
        }
    }
    Spacer(Modifier.height(Space.Block))
}

/** The door to one reciter's downloaded surahs, with the arrow it opens by. */
@Composable
private fun ReciterDownloadsDoor(open: Boolean, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(start = 50.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (open) {
                stringResource(R.string.settings_downloaded_hide)
            } else {
                stringResource(R.string.settings_downloaded_show, count)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        // The arrow says the label opens something, and which way it will
        // move: down while the list is hidden, up once it is under it.
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .rotate(if (open) 180f else 0f),
        )
    }
}

/** One downloaded surah: its name, its size, and the one action it needs. */
@Composable
private fun DownloadedSurahRow(row: DownloadedSurah, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The row is already as tall as a finger needs, because the
            // Remove action brings its own touch target; the surah names sit
            // close together so the list reads as one block.
            .padding(start = 50.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
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
        PackActionText(stringResource(R.string.pack_action_remove), onRemove)
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
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_translations))
        Text(
            text = stringResource(R.string.settings_translations_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = Space.Line),
        )
        LanguageGroups(packs, PackType.Translation) { pack ->
            PackChoiceRow(
                pack = pack,
                subtitle = translationSubtitle(pack),
                selected = pack.installed && pack.id in settings.translationPacks,
                radio = false,
                setup = packSetup?.takeIf { it.packId == pack.id },
                onActivate = { actions.onToggleTranslation(pack.id) },
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
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
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_tafsirs))
        Text(
            text = stringResource(R.string.settings_tafsirs_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = Space.Line),
        )
        LanguageGroups(packs, PackType.Tafsir) { pack ->
            PackChoiceRow(
                pack = pack,
                subtitle = translationSubtitle(pack),
                selected = pack.installed && pack.id in settings.tafsirPacks,
                radio = false,
                setup = packSetup?.takeIf { it.packId == pack.id },
                onActivate = { actions.onToggleTafsir(pack.id) },
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
            )
        }
        Spacer(Modifier.height(Space.Section))
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
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = Space.Block, bottom = Space.Tight),
        )
        val active = activeWordsPack(settings, packs)
        LanguageGroups(packs, PackType.Words) { pack ->
            PackChoiceRow(
                pack = pack,
                subtitle = translationSubtitle(pack),
                selected = pack.id == active && settings.wordByWord,
                radio = false,
                setup = packSetup?.takeIf { it.packId == pack.id },
                // Turning the switch on is part of choosing a word list: a
                // reader who taps one wants to see meanings, not a pack with
                // nothing to show.
                onActivate = { actions.onWordByWord(true) },
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
            )
        }
        Spacer(Modifier.height(Space.Section))
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

/**
 * The packs of one kind, grouped by the language they speak and alphabetical
 * inside each group. A list of choices is read, not searched: the reader
 * looks for a name, so names are in one order everywhere in the app.
 */
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
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = Space.Block, bottom = Space.Tight),
            )
            group.sortedBy { it.name.lowercase() }.forEach { pack -> row(pack) }
        }
}

/**
 * The app itself: its version, its credits, and a way to check its content.
 */
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
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = Space.Tight, bottom = Space.Line),
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The order languages appear in: by the name the reader reads, so the list is
 * alphabetical (Arabic, Bangla, English) and a reader looking for one knows
 * where to look. The order is the display name, never the ISO code, because
 * the code would sort English after Arabic for the wrong reason.
 */
internal fun languageOrder(language: String): String = languageSortKey(language)
