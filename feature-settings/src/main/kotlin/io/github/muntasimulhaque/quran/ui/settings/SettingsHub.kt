package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadError
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.clockText
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageChoiceName
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName
import io.github.muntasimulhaque.quran.uikit.R as UiKitR

/** The pages the settings hub opens, one at a time. */
enum class SettingsPage {
    Language, Theme, FontSize, Reciters, Daily, Translations, Tafsirs, Words, Tajweed, About,
}

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
        SettingsPage.Words -> R.string.settings_title_words
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
 * The ten rows read as one list, the app's own surfaces first because they
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
            summary = fontSizeSummary(settings),
            modifier = Modifier.testTag("row-text"),
        ) { onOpen(SettingsPage.FontSize) }
        // What the reading draws, in the order the page draws it: the tajweed
        // colors, which are the Arabic line itself, then the word meanings under
        // it, then the translation, then the tafsir behind the ayah card. Each
        // row is a place and the state it stands in, and the page it opens is
        // where the reader changes it: the hub is an index, and an index carries
        // no second way to act (owner decision, this session).
        PageRow(
            title = stringResource(R.string.settings_tajweed_title),
            summary = onOff(settings.tajweed),
            modifier = Modifier.testTag("row-tajweed"),
        ) { onOpen(SettingsPage.Tajweed) }
        PageRow(
            title = stringResource(R.string.settings_row_words),
            summary = wordsSummary(settings, packs, packSetup),
            modifier = Modifier.testTag("row-words"),
        ) { onOpen(SettingsPage.Words) }
        PageRow(
            title = stringResource(R.string.settings_row_translation),
            summary = translationSummary(settings, packs),
            modifier = Modifier.testTag("row-translation"),
        ) { onOpen(SettingsPage.Translations) }
        // The tafsir is a list, not a switch. The reading opens a tafsir only
        // when the reader asks for one, from the pill's own Tafsir mark, so
        // there is no page of tafsir being drawn that a switch could hide:
        // the packs are the setting, the same way the reciter's voice is the
        // setting and no switch turns a reciter on (owner decision, this
        // session).
        PageRow(
            title = stringResource(R.string.settings_title_tafsirs),
            summary = tafsirSummary(settings, packs),
            modifier = Modifier.testTag("row-tafsirs"),
        ) { onOpen(SettingsPage.Tafsirs) }
        PageRow(
            title = stringResource(R.string.settings_title_reciters),
            summary = reciterName(settings, recitations),
        ) { onOpen(SettingsPage.Reciters) }
        // The daily ayah: one ayah a day, at the reader's own moment. The time
        // is the row's value, and the page behind it is where the reminder is
        // turned on and the hour set.
        PageRow(
            title = stringResource(R.string.settings_daily_title),
            summary = dailySubtitle(settings, notificationsBlocked, exactGrantWithheld),
            modifier = Modifier.testTag("row-daily"),
        ) { onOpen(SettingsPage.Daily) }
        PageRow(
            title = stringResource(R.string.settings_title_about),
            summary = stringResource(R.string.settings_version_short, localizedDigits(version)),
        ) { onOpen(SettingsPage.About) }
    }
}

/**
 * The four sizes in the hub's one line: "Default" while every kind of text
 * stands on the middle step, and "Adjusted" once the reader has moved one
 * of them.
 *
 * It used to print the place of every kind ("Quran text 3 of 5, translation
 * 3 of 5, tafsir 3 of 5, word meanings 3 of 5"), which took two or three
 * lines of the row and said "Default" in four numbers. The page behind the
 * row shows every step as a filled cell beside the reader's own sample, so
 * the row's own job is only whether anything has moved (owner report).
 */
@Composable
private fun fontSizeSummary(settings: AppSettings): String {
    val steps = listOf(
        settings.arabicSize,
        settings.translationSize,
        settings.tafsirSize,
        settings.wordsSize,
    )
    return stringResource(
        if (steps.all { it == TextSize.DEFAULT }) {
            R.string.settings_summary_text_default
        } else {
            R.string.settings_summary_text_adjusted
        },
    )
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
    !settings.dailyAyah -> stringResource(R.string.settings_state_off)
    notificationsBlocked -> stringResource(R.string.settings_daily_subtitle_blocked)
    exactGrantWithheld -> stringResource(R.string.settings_daily_subtitle_late)
    else -> stringResource(
        R.string.settings_daily_subtitle_on,
        clockText(settings.dailyAyahMinute),
    )
}

/** The hub's two state words. A row says On or Off, and the page says more. */
@Composable
private fun onOff(on: Boolean): String = stringResource(
    if (on) R.string.settings_state_on else R.string.settings_state_off,
)

/**
 * What the word meanings row says: whether the reading draws the meanings,
 * and, while the list is on its way or has failed, what the download is
 * doing. The size and the Add live on the page, so the index row keeps one
 * short line (owner decision, this session).
 *
 * The value follows the switch's old rule: it reads On only when the aid is
 * on and the list it needs is on the device, so an On in the list always
 * means meanings are being drawn.
 */
@Composable
private fun wordsSummary(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
): String {
    val wanted = wantedWordsPack(settings, packs)
    val installed = wanted?.installed == true
    val setup = packSetup?.takeIf { it.packId == wanted?.id }
    return when {
        setup != null && setup.failed -> stringResource(
            if (setup.failure == DownloadError.Storage) {
                UiKitR.string.failure_storage_words
            } else {
                UiKitR.string.failure_download
            },
        )
        setup != null && setup.progress != null -> stringResource(
            R.string.settings_pack_downloading,
            (setup.progress * 100).toInt(),
        )
        setup != null -> stringResource(R.string.settings_pack_preparing)
        else -> onOff(settings.wordByWord && installed)
    }
}

/**
 * What the translation row says: the pack the reading draws with, or Off.
 * The pack's name is the useful half when the translation is on, and a
 * chosen pack that is not on the device reads as the reader's file and not
 * as a missing setting (owner decision, this session).
 */
@Composable
private fun translationSummary(settings: AppSettings, packs: List<ContentPack>): String =
    if (settings.showTranslation && hasChosenTranslation(settings, packs)) {
        translationName(settings, packs)
    } else {
        stringResource(R.string.settings_state_off)
    }
