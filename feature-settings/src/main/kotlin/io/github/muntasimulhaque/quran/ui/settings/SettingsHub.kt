package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.resolved
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.clockText
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageChoiceName
import io.github.muntasimulhaque.quran.ui.kit.languageName
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
 * the day choice after it (owner report).
 *
 * The row carries the page that is drawing and nothing more. With the switch
 * on and the phone in the light, that is the reader's own day page, so the
 * row reads "Paper" and stops: the old summary said "Paper · auto, day page
 * Paper", which repeated the same word twice, and its length is what pushed
 * the row's name into four lines (owner report, 37th session). The second
 * half is kept for the only case it says anything, when the phone is in dark
 * mode and the day page the reader chose is the one being named.
 */
@Composable
private fun themeSummary(settings: AppSettings): String {
    val dark = (LocalContext.current.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    val shown = settings.theme.resolved(autoNight = settings.autoNight, systemDark = dark)
    val name = shown.name()
    return if (settings.autoNight && shown != settings.theme) {
        stringResource(R.string.settings_summary_theme_auto, name, settings.theme.name())
    } else {
        name
    }
}

/**
 * The hub: one row per category, each carrying where it stands, so a reader
 * can see their own setup at a glance and open only what they came to change.
 *
 * The rows are in four quiet groups, because a flat list of eleven rows is
 * read one at a time and a list with a name over each part of it is read at a
 * glance. The groups are the three things a reader does with the app and the
 * one thing it does for them: the app itself, the reading, the recitation,
 * and the reminder. The interface comes first because it is the app's own
 * words, its page, and its type, and a reader who has come to change one of
 * those is not looking for the reading (owner report). About stands at the
 * foot on its own, which is where an app names itself (owner report, 37th
 * session).
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
        // What the app itself is: the words it speaks, the page it draws, and
        // the size of its type. The three were one row and a group above it,
        // with the language alone over the top on its own, and a reader
        // looking for the theme had to read past a group name to find the
        // first of the three (owner report). The language is the group's own
        // first row: it changes the words of every row below it, so a reader
        // who changes it reads the whole sheet again in the new language, and
        // it is said here rather than only behind its own page.
        Group(stringResource(R.string.settings_group_interface))
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
        // by looking (owner report, 2.3). The screen staying awake belongs
        // here too: it is about the reading in front of the reader and not
        // about what the app looks like (owner report).
        Group(stringResource(R.string.settings_group_reading))
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
        // The screen staying awake is one switch with no page behind it, and a
        // page that held only this one row was a door to a single tap. It says
        // nothing under its name: the switch is the whole of what it is, and a
        // line of explanation under a control that already shows its own state
        // is the sheet talking to itself.
        ToggleRow(
            title = stringResource(R.string.settings_keep_awake_title),
            subtitle = null,
            checked = settings.keepAwake,
            onChange = onKeepAwake,
        )
        Group(stringResource(R.string.settings_group_recitation))
        PageRow(
            title = stringResource(R.string.settings_title_reciters),
            summary = reciterName(settings, recitations),
        ) { onOpen(SettingsPage.Reciters) }
        PageRow(
            title = stringResource(R.string.settings_title_listening),
            summary = listeningSummary(settings),
        ) { onOpen(SettingsPage.Listening) }
        // The daily reminder: one ayah a day, at the reader's own moment. It
        // comes on with the app, and this row is the door to the page that
        // turns it off or moves its time, so the switch and the clock are read
        // together in one place rather than as a strip of hours under a
        // switch in the hub (owner report, 2.3).
        Group(stringResource(R.string.settings_group_reminder))
        ToggleRow(
            title = stringResource(R.string.settings_daily_title),
            subtitle = dailySubtitle(settings),
            checked = settings.dailyAyah,
            onChange = onDailyAyah,
            onOpen = { onOpen(SettingsPage.Daily) },
            openLabel = stringResource(R.string.settings_open_daily),
            switchTag = "switch-daily",
        )
        Spacer(Modifier.height(Space.Section))
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
 * Where the reminder stands, in one line: whether it will come, and when. The
 * moment is read from the same value the alarm is armed with, so the row never
 * promises a time the reminder does not keep, and it says the moment whether
 * the reminder is on or off, because the page stays live while it is off and a
 * reader who has set a time is owed to see it.
 */
@Composable
private fun dailySubtitle(settings: AppSettings): String = stringResource(
    if (settings.dailyAyah) {
        R.string.settings_daily_subtitle_on
    } else {
        R.string.settings_daily_subtitle_off
    },
    clockText(settings.dailyAyahMinute),
)

/**
 * Where the listening choices stand, in one line: the pace, and what happens
 * as the reading moves on. A reader who set a pace and forgot it must be able
 * to see it from the hub, or the reading sounds slow for a reason they cannot
 * find, and the answer at the end of an ayah is the other thing a reader sets
 * once and then forgets. A reading told to stop after each ayah says so here,
 * because a stopped reading is the one state a reader must be able to see
 * without opening the page.
 */
@Composable
private fun listeningSummary(settings: AppSettings): String {
    val speed = stringResource(R.string.settings_listening_speed_value, speedText(settings.playbackSpeed))
    return when (settings.endOfAudio) {
        EndOfAudio.REPEAT_AYAH -> stringResource(R.string.settings_listening_summary_repeat, speed)
        EndOfAudio.REPEAT_SURAH -> stringResource(R.string.settings_listening_summary_repeat_surah, speed)
        EndOfAudio.STOP_AFTER_AYAH -> stringResource(R.string.settings_listening_summary_stop, speed)
        EndOfAudio.CONTINUE_AYAH, EndOfAudio.CONTINUE_SURAH -> speed
    }
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

