package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.clockText
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageChoiceName
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName

/** The pages the settings hub opens, one at a time. */
enum class SettingsPage { Language, Theme, FontSize, Reciters, Daily, Translations, Tafsirs, Tajweed, About }

@Composable
fun SettingsPage.title(): String = stringResource(
    when (this) {
        SettingsPage.Language -> R.string.settings_title_language
        SettingsPage.Theme -> R.string.settings_title_appearance
        SettingsPage.FontSize -> R.string.settings_title_text
        SettingsPage.Reciters -> R.string.settings_title_reciters
        SettingsPage.Daily -> R.string.settings_daily_title
        SettingsPage.Translations -> R.string.settings_title_translations
        SettingsPage.Tafsirs -> R.string.settings_title_tafsirs
        SettingsPage.Tajweed -> R.string.settings_title_tajweed
        SettingsPage.About -> R.string.settings_title_about
    },
)

/**
 * Where the theme stands, in one line: the stored choice, Light, Dark, or
 * System, exactly as the reader chose it. The ring on the appearance page
 * says the same, so the hub never contradicts it by naming the page the
 * phone happens to be showing (owner decision).
 */
@Composable
private fun themeSummary(settings: AppSettings): String = settings.theme.name()

/**
 * The hub: one row per setting, each carrying where it stands, so a reader
 * can see their own setup at a glance and open only what they came to change.
 *
 * The nine rows read as one list, the app's own surfaces first because they
 * are the words the reader reads everything else in, then the reading's own
 * choices, then the reciter, then the daily ayah. The four group headings
 * that used to stand over them named parts of one list, and a reader who has
 * opened the settings already knows which part they are in (owner decision,
 * this session). About is the list's last row like any other: it used to
 * stand after a section's gap, which was the one place the sheet broke its
 * own rhythm, and the reader counted the gap (owner report, this session).
 * Every row keeps the same room above and below it, so the list no longer
 * has one gap that reads as a section break.
 */
@Composable
fun SettingsHub(
    settings: AppSettings,
    packs: List<ContentPack>,
    recitations: List<Recitation>,
    version: String,
    notificationsBlocked: Boolean,
    exactGrantWithheld: Boolean,
    packSetup: PackSetupState?,
    onShowTranslation: (Boolean) -> Unit,
    onShowTafsir: (Boolean) -> Unit,
    onWordByWord: (Boolean) -> Unit,
    onTajweed: (Boolean) -> Unit,
    onDailyAyah: (Boolean) -> Unit,
    onOpen: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().testTag("settings-hub")) {
        // The app's own surfaces first: the words it speaks, the page it
        // draws, and the size of its type. The language changes the words of
        // every row below it, so a reader who changes it reads the whole sheet
        // again in the new language, and it is said here rather than only
        // behind its own page.
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
                settings.tafsirSp.toInt(),
                settings.wordsSp.toInt(),
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
            title = stringResource(R.string.settings_tajweed_title),
            subtitle = stringResource(R.string.settings_tajweed_summary),
            checked = settings.tajweed,
            onChange = onTajweed,
            onOpen = { onOpen(SettingsPage.Tajweed) },
            openLabel = stringResource(R.string.settings_open_tajweed),
            switchTag = "switch-tajweed",
        )
        ToggleRow(
            title = stringResource(R.string.settings_show_translation_title),
            subtitle = translationName(settings, packs),
            checked = settings.showTranslation && hasInstalledTranslation(settings, packs),
            onChange = onShowTranslation,
            onOpen = { onOpen(SettingsPage.Translations) },
            openLabel = stringResource(R.string.settings_open_translations),
            switchTag = "switch-translation",
        )
        ToggleRow(
            title = stringResource(R.string.settings_show_tafsir_title),
            subtitle = tafsirSummary(settings, packs),
            checked = settings.showTafsir && hasInstalledTafsir(settings, packs),
            onChange = onShowTafsir,
            onOpen = { onOpen(SettingsPage.Tafsirs) },
            openLabel = stringResource(R.string.settings_open_tafsirs),
            switchTag = "switch-tafsir",
        )
        PageRow(
            title = stringResource(R.string.settings_title_reciters),
            summary = reciterName(settings, recitations),
        ) { onOpen(SettingsPage.Reciters) }
        // The daily ayah: one ayah a day, at the reader's own moment. The
        // switch turns it on or off, and the row is the door to the page that
        // moves its time, so the switch and the clock are read together in one
        // place (owner report, 2.3).
        ToggleRow(
            title = stringResource(R.string.settings_daily_title),
            subtitle = dailySubtitle(settings, notificationsBlocked, exactGrantWithheld),
            checked = settings.dailyAyah,
            onChange = onDailyAyah,
            onOpen = { onOpen(SettingsPage.Daily) },
            openLabel = stringResource(R.string.settings_open_daily),
            switchTag = "switch-daily",
        )
        PageRow(
            title = stringResource(R.string.settings_title_about),
            summary = stringResource(R.string.settings_version_short, localizedDigits(version)),
        ) { onOpen(SettingsPage.About) }
    }
}

/**
 * The reader's reciter, in the reader's own language: the hub row used to
 * draw the catalog's English name beside a Bangla interface (owner decision,
 * this session).
 */
@Composable
private fun reciterName(settings: AppSettings, recitations: List<Recitation>): String {
    val chosen = recitations.firstOrNull { it.id == settings.recitation }
    return chosen?.let { reciterShortName(it.id, it.name) }
        ?: stringResource(R.string.settings_none_yet)
}

/**
 * Where the daily ayah stands, in one line: whether it will come, and when.
 * The moment is read from the same value the alarm is armed with, so the row
 * never promises a time the daily ayah does not keep, and it says the moment
 * whether the daily ayah is on or off, because the page stays live while it is
 * off and a reader who has set a time is owed to see it.
 *
 * While the phone withholds notifications or the exact alarm, the row does not
 * promise the time at all: the daily ayah cannot arrive, or can arrive late,
 * and saying which is the honest line (owner decision, this session).
 */
@Composable
private fun dailySubtitle(
    settings: AppSettings,
    notificationsBlocked: Boolean,
    exactGrantWithheld: Boolean,
): String = when {
    !settings.dailyAyah -> stringResource(
        R.string.settings_daily_subtitle_off,
        clockText(settings.dailyAyahMinute),
    )
    notificationsBlocked -> stringResource(R.string.settings_daily_subtitle_blocked)
    exactGrantWithheld -> stringResource(R.string.settings_daily_subtitle_late)
    else -> stringResource(
        R.string.settings_daily_subtitle_on,
        clockText(settings.dailyAyahMinute),
    )
}

/**
 * The word by word switch, and the state of the list behind it. The row is
 * checked only when the aid is on and the list it needs is on the device, so
 * a switch that reads on always means meanings are being drawn; when the
 * list is missing the row says so, with its size, and the same tap that would
 * turn the aid on fetches it.
 *
 * The row says nothing else. The aid is a switch, the switch says whether the
 * reading draws the meanings, and a line under it restating that, or
 * explaining what a word meaning is to a reader who has just turned the aid
 * on, is the one kind of line a status list does not carry (owner report,
 * 37th session).
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
        else -> null
    }
    ToggleRow(
        title = stringResource(R.string.settings_words_title),
        subtitle = subtitle,
        checked = settings.wordByWord && installed,
        onChange = onWordByWord,
    )
}

