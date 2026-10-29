package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
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
 *
 * The master switch stands at the top of the page, in the same words and the
 * same state as the hub row that opened it. The page that owns the list owns
 * the switch that shows it, and a reader who came here from the row, or from
 * a reader's own path, can turn the reading on without going back for it
 * (owner decision, this session). The switch that used to live on the hub row
 * and nowhere else is still there; this is one value read in two places, not
 * a second setting.
 */
@Composable
fun TranslationsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        val installed = hasInstalledTranslation(settings, packs)
        ToggleRow(
            title = stringResource(R.string.settings_show_translation_title),
            subtitle = translationName(settings, packs),
            checked = settings.showTranslation && installed,
            onChange = actions.onShowTranslation,
            enabled = installed,
            switchTag = "switch-translation-page",
        )
        LanguageGroups(
            packs = packs,
            type = PackType.Translation,
        ) { pack ->
            PackChoiceRow(
                pack = pack,
                subtitle = packSizeLine(pack),
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

/**
 * Tafsirs: as many as the reader wants, each behind its own door.
 *
 * There is no master switch over the list. The reading opens a tafsir only
 * when the reader asks for one, from the ayah pill's own Tafsir mark, so
 * there is no passage being drawn that a switch could hide; a tafsir is on
 * the reading exactly while a pack is chosen, the same way a reciter is the
 * reader's voice exactly while it is chosen (owner decision, this session).
 */
@Composable
fun TafsirsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        LanguageGroups(
            packs = packs,
            type = PackType.Tafsir,
        ) { pack ->
            PackChoiceRow(
                pack = pack,
                subtitle = packSizeLine(pack),
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
