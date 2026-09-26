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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.resolved
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.kit.clockText
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.languageChoiceName
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.theme.Space
/** The pages the settings hub opens, one at a time. */
enum class SettingsPage { Language, Theme, FontSize, Reciters, Listening, Daily, Translations, Tafsirs, About }

@Composable
fun SettingsPage.title(): String = stringResource(
    when (this) {
        SettingsPage.Language -> R.string.settings_title_language
        SettingsPage.Theme -> R.string.settings_title_appearance
        SettingsPage.FontSize -> R.string.settings_title_text
        SettingsPage.Reciters -> R.string.settings_title_reciters
        SettingsPage.Listening -> R.string.settings_title_listening
        SettingsPage.Daily -> R.string.settings_daily_title
        SettingsPage.Translations -> R.string.settings_title_translations
        SettingsPage.Tafsirs -> R.string.settings_title_tafsirs
        SettingsPage.About -> R.string.settings_title_about
    },
)

/**
 * The page a choice reads as, with the automatic switch said in the same
 * breath, so the hub row never claims a page the reader is not on: in dark
 * mode with auto-night on, the row says Night, the page drawing, and names
 * the day choice after it (owner report, D-097).
 */
@Composable
private fun themeSummary(settings: AppSettings): String {
    val dark = (LocalContext.current.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    val shown = settings.theme.resolved(autoNight = settings.autoNight, systemDark = dark)
    val name = shown.name()
    return if (settings.autoNight) {
        stringResource(R.string.settings_summary_theme_auto, name, settings.theme.name())
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
    packSetup: PackSetupState?,
    onKeepAwake: (Boolean) -> Unit,
    onShowTranslation: (Boolean) -> Unit,
    onShowTafsir: (Boolean) -> Unit,
    onWordByWord: (Boolean) -> Unit,
    onDailyAyah: (Boolean) -> Unit,
    onOpen: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().testTag("settings-hub")) {
        PageRow(
            title = stringResource(R.string.settings_title_language),
            summary = languageChoiceName(settings.uiLanguage ?: UiLanguage.English.tag),
        ) { onOpen(SettingsPage.Language) }
        PageRow(
            title = stringResource(R.string.settings_title_appearance),
            summary = themeSummary(settings),
        ) { onOpen(SettingsPage.Theme) }
        PageRow(
            title = stringResource(R.string.settings_title_text),
            summary = stringResource(
                R.string.settings_summary_text,
                settings.arabicSp.toInt(),
                settings.translationSp.toInt(),
            ),
        ) { onOpen(SettingsPage.FontSize) }
        // What the reading draws, in the order the reader puts it together: the
        // word meanings, which sit closest to the Arabic, then the translation,
        // then the tafsir (owner report, 2.3). Each switch says whether the
        // reading shows what it names, and the rest of the row opens the list
        // it is chosen from. The switch is the only thing that switches, so a
        // reader who came to look at the translations never changes the reading
        // by looking (owner report, 2.3).
        WordByWordRow(settings, packs, packSetup, onWordByWord)
        ToggleRow(
            title = stringResource(R.string.settings_show_translation_title),
            subtitle = translationName(settings, packs),
            checked = settings.showTranslation,
            onChange = onShowTranslation,
            onOpen = { onOpen(SettingsPage.Translations) },
            openLabel = stringResource(R.string.settings_open_translations),
            switchTag = "switch-translation",
        )
        ToggleRow(
            title = stringResource(R.string.settings_show_tafsir_title),
            subtitle = tafsirSummary(settings, packs),
            checked = settings.showTafsir,
            onChange = onShowTafsir,
            onOpen = { onOpen(SettingsPage.Tafsirs) },
            openLabel = stringResource(R.string.settings_open_tafsirs),
            switchTag = "switch-tafsir",
        )
        PageRow(
            title = stringResource(R.string.settings_title_reciters),
            summary = reciterName(settings, recitations),
        ) { onOpen(SettingsPage.Reciters) }
        PageRow(
            title = stringResource(R.string.settings_title_listening),
            summary = listeningSummary(settings),
        ) { onOpen(SettingsPage.Listening) }
        // Reading with the screen awake sits in the hub itself, beside the
        // other whole-app choices: it is one switch with no page behind it,
        // and a page that held only this one row was a door to a single tap.
        ToggleRow(
            title = stringResource(R.string.settings_keep_awake_title),
            subtitle = stringResource(R.string.settings_keep_awake_subtitle),
            checked = settings.keepAwake,
            onChange = onKeepAwake,
        )
        // The daily reminder: one ayah a day, at the reader's own moment. It
        // comes on from the first launch, and this row is the door to the page
        // that turns it off or moves its time, so the switch and the clock are
        // read together in one place rather than as a strip of hours under a
        // switch in the hub (owner report, 2.3).
        ToggleRow(
            title = stringResource(R.string.settings_daily_title),
            subtitle = dailySubtitle(settings),
            checked = settings.dailyAyah,
            onChange = onDailyAyah,
            onOpen = { onOpen(SettingsPage.Daily) },
            openLabel = stringResource(R.string.settings_open_daily),
            switchTag = "switch-daily",
        )
        PageRow(
            title = stringResource(R.string.settings_title_about),
            summary = stringResource(R.string.settings_version_short, version),
        ) { onOpen(SettingsPage.About) }
    }
}

@Composable
private fun reciterName(settings: AppSettings, recitations: List<Recitation>): String =
    recitations.firstOrNull { it.id == settings.recitation }?.name
        ?: stringResource(R.string.settings_none_yet)

/**
 * Where the reminder stands, in one line: whether it will come, and when.
 * The moment is read from the same value the alarm is armed with, so the row
 * never promises a time the reminder does not keep.
 */
@Composable
private fun dailySubtitle(settings: AppSettings): String =
    if (settings.dailyAyah) {
        stringResource(
            R.string.settings_daily_subtitle_on,
            clockText(settings.dailyAyahMinute),
        )
    } else {
        stringResource(R.string.settings_daily_subtitle_off)
    }

/**
 * Where the listening choices stand, in one line: the pace, and whether an
 * ayah is repeating. A reader who set a pace and forgot it must be able to
 * see it from the hub, or the reading sounds slow for a reason they cannot
 * find.
 */
@Composable
private fun listeningSummary(settings: AppSettings): String {
    val speed = stringResource(R.string.settings_listening_speed_value, speedText(settings.playbackSpeed))
    return if (settings.repeatAyah) {
        stringResource(R.string.settings_listening_summary_repeat, speed)
    } else {
        speed
    }
}

/**
 * The listening page: how fast the recitation plays, and whether one ayah
 * repeats. Both are about hearing, not about the page, so they sit together
 * under the reciter whose voice they shape.
 */
@Composable
fun ListeningPage(
    settings: AppSettings,
    onSpeed: (Float) -> Unit,
    onRepeat: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_speed))
        Text(
            text = stringResource(R.string.settings_speed_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = Space.Line),
        )
        SpeedRow(value = settings.playbackSpeed, onChange = onSpeed)
        Spacer(Modifier.height(Space.Section))
        Group(stringResource(R.string.settings_group_repeat))
        ToggleRow(
            title = stringResource(R.string.settings_repeat_title),
            subtitle = stringResource(R.string.settings_repeat_subtitle),
            checked = settings.repeatAyah,
            onChange = onRepeat,
        )
        Spacer(Modifier.height(Space.Section))
    }
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

/**
 * The word by word switch, and the state of the list behind it. The row is
 * checked only when the aid is on and the list it needs is on the device, so
 * a switch that reads on always means meanings are being drawn; when the
 * list is missing the row says so, with its size, and the same tap that would
 * turn the aid on fetches it.
 */
@Composable
internal fun WordByWordRow(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    onWordByWord: (Boolean) -> Unit,
) {
    val wanted = wantedWordsPack(settings, packs)
    val installed = wanted?.installed == true
    val setup = packSetup?.takeIf { it.packId == wanted?.id }
    val subtitle = when {
        setup != null && setup.failed -> stringResource(R.string.settings_words_failed)
        setup != null && setup.progress != null -> stringResource(
            R.string.settings_pack_downloading,
            (setup.progress * 100).toInt(),
        )
        setup != null -> stringResource(R.string.settings_pack_preparing)
        !installed && wanted != null -> stringResource(
            R.string.settings_words_add,
            languageName(wanted.language),
            formatBytes(wanted.bytes),
        )
        else -> stringResource(R.string.settings_words_subtitle)
    }
    ToggleRow(
        title = stringResource(R.string.settings_words_title),
        subtitle = subtitle,
        checked = settings.wordByWord && installed,
        onChange = onWordByWord,
    )
}

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
        Text(
            text = stringResource(R.string.settings_language_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = Space.Line),
        )
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
                subtitle = stringResource(R.string.settings_language_subtitle),
                selected = settings.uiLanguage == language.tag,
                onClick = { onLanguage(language.tag) },
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The word list the reading speaks: the one that matches the first chosen
 * translation, and English when that language has no list. The translation
 * counts as chosen even before it is installed, so a Bangla reader is never
 * offered the English word list because the Bangla translation is still on
 * its way. The pack is the one the switch adds when it is missing.
 */
internal fun wantedWordsPack(settings: AppSettings, packs: List<ContentPack>): ContentPack? {
    val language = packs.firstOrNull { it.id in settings.translationPacks }?.language
        ?: UiLanguage.English.tag
    val preferred = ContentDatabase.wordsPackId(language)
    return packs.firstOrNull { it.id == preferred }
        ?: packs.firstOrNull { it.id == ContentDatabase.WORDS_PACK }
}

@Composable
internal fun translationSubtitle(pack: ContentPack): String {
    val detail = if (pack.shipped) {
        stringResource(R.string.pack_included_suffix)
    } else {
        formatBytes(pack.bytes)
    }
    return stringResource(R.string.pack_installed, languageName(pack.language), detail)
}

/**
 * The packs of one kind, grouped by the language they speak, in the
 * alphabetical order of those languages, and alphabetical inside each group.
 * A list of choices is read, not searched: the reader looks for a name, so
 * names are in one order everywhere in the app.
 */
@Composable
internal fun LanguageGroups(
    packs: List<ContentPack>,
    type: PackType,
    row: @Composable (ContentPack) -> Unit,
) {
    val groups = packs.filter { it.type == type }.groupBy { it.language }
    val names = HashMap<String, String>()
    for (language in groups.keys) names[language] = languageName(language)
    groups.entries
        .sortedBy { (names[it.key] ?: it.key).lowercase() }
        .forEach { (language, group) ->
            Text(
                text = names[language] ?: language,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = Space.Block, bottom = Space.Tight),
            )
            group.sortedBy { it.name.lowercase() }.forEach { pack -> row(pack) }
        }
}

