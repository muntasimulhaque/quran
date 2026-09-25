package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import android.content.res.Configuration
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.StudyRow
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.resolved
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.rich.ArabicFonts
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.theme.Space
import androidx.compose.ui.platform.LocalContext
import io.github.muntasimulhaque.quran.core.RichText
/** The pages the settings hub opens, one at a time. */
/**
 * The appearance page: the four grounds, and the switch that lets the system
 * choose between the day and the night halves of them.
 *
 * The filled swatch is the page drawing right now. With automatic night mode
 * on and the phone in dark mode, that is Night, even though the stored choice
 * is the day page under it; the note names the day page so the choice is
 * never lost (owner report, D-097). The system's own state is read from the
 * resources here rather than from a composition local, because a sheet is its
 * own window and never sees the activity's composition.
 */
@Composable
fun AppearancePage(
    settings: AppSettings,
    onTheme: (io.github.muntasimulhaque.quran.data.AppTheme) -> Unit,
    onAutoNight: (Boolean) -> Unit,
) {
    val systemDark = (LocalContext.current.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val shown = settings.theme.resolved(
        autoNight = settings.autoNight,
        systemDark = systemDark,
    )
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_theme))
        ThemeRow(selected = settings.theme, shown = shown, onSelect = onTheme)
        // The swatches and the switch are two separate decisions, and the
        // switch is not a fifth swatch: the break between them says so.
        Spacer(Modifier.height(Space.Section))
        ToggleRow(
            title = stringResource(R.string.settings_auto_night_title),
            subtitle = stringResource(R.string.settings_auto_night_subtitle),
            checked = settings.autoNight,
            onChange = onAutoNight,
        )
        Text(
            text = if (settings.autoNight) {
                stringResource(R.string.settings_theme_note_auto, settings.theme.name())
            } else {
                stringResource(R.string.settings_theme_note)
            },
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
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
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
        Group(stringResource(R.string.settings_group_translations))
        Text(
            text = stringResource(R.string.settings_translations_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = Space.Line),
        )
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
        Group(stringResource(R.string.settings_group_tafsirs))
        Text(
            text = stringResource(R.string.settings_tafsirs_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = Space.Line),
        )
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
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The app itself: its version, its credits, and a way to check its content.
 */@Composable
fun AboutPage(
    version: String,
    contentCheck: ContentCheck?,
    onCredits: () -> Unit,
    onCheckContent: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
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



