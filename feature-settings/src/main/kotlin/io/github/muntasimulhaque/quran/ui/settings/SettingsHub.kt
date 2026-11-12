package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.readingMetrics
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.clockText
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageChoiceName
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName
import io.github.muntasimulhaque.quran.ui.theme.Literata

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
 * The ten rows read as one list, ordered by what a reader comes to change:
 * the size of the text first, because that is the row this list is opened
 * for most, then the ground and the language, then the reading's own choices,
 * the voice, the reminder, and the app. It used to lead with the app's own
 * surfaces on the reasoning that they are the words everything else is read
 * in; that is a designer's reason and not a reader's (owner decision, this
 * build). About is the list's last row like any other. Every row keeps the
 * same room above and below it, so the list has no gap that reads as a
 * section break.
 *
 * **The hub carries no leading icons** (owner decision, this session). The
 * value under each name is the row's sign: Aa is the text size, System is
 * the ground, English is the language, the pack names are the reading,
 * Minshawi is the voice, 8:00 is the reminder. A 20 dp leading mark would
 * take 32 dp from the words and move them off the page's gutter, and the
 * icon set is a vocabulary of controls and states, not categories. The row's
 * one mark is the chevron that says it opens.
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
        // The row a reader opens this list for, and it opens with the size
        // itself: the sample is drawn at the reader's own reading size, so
        // the row answers how big the text is without a word that describes
        // it (owner decision, this build).
        PageRow(
            title = stringResource(R.string.settings_title_text),
            summary = null,
            valueContent = { FontSizeSample(settings) },
            modifier = Modifier.testTag("row-text"),
        ) { onOpen(SettingsPage.FontSize) }
        PageRow(
            title = stringResource(R.string.settings_title_appearance),
            summary = themeSummary(settings),
        ) { onOpen(SettingsPage.Theme) }
        PageRow(
            title = stringResource(R.string.settings_title_language),
            summary = languageChoiceName(settings.uiLanguage ?: UiLanguage.English.tag),
        ) { onOpen(SettingsPage.Language) }
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
 * The Font size row's value: the reader's reading size, drawn.
 *
 * "Default" and "Adjusted" were a state and not a size: the row said whether
 * the reader had moved a rail, never what the page looks like. The sample is
 * the reading's own body at the reader's step, in the Book's face, so the row
 * shows the thing it names. It takes the translation's step rather than the
 * Quran's because this is the interface's own list, and the translation is
 * the reading's body (owner decision, this build).
 */
@Composable
private fun FontSizeSample(settings: AppSettings) {
    Text(
        text = "Aa",
        fontFamily = Literata,
        fontSize = settings.readingMetrics().translationSp.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    return packSetupLine(setup) ?: onOff(settings.wordByWord && installed)
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
