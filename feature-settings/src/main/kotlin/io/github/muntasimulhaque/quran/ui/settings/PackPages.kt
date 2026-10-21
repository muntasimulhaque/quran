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
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space

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
