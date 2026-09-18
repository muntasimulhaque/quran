package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageName
import kotlinx.coroutines.launch

/**
 * What the settings sheet can ask the app to do. The sheet itself owns no
 * state beyond what is open; every change is a call, so the feature can be
 * read, tested, and previewed without a view model.
 */
data class SettingsActions(
    val onTheme: (AppTheme) -> Unit = {},
    val onTextSize: (TextSize) -> Unit = {},
    val onKeepAwake: (Boolean) -> Unit = {},
    val onFollowReciter: (Boolean) -> Unit = {},
    val onShowFootnotes: (Boolean) -> Unit = {},
    val onWordByWord: (Boolean) -> Unit = {},
    val onDimLevel: (Int) -> Unit = {},
    val onSelectRecitation: (String) -> Unit = {},
    val onTranslationPack: (String) -> Unit = {},
    val onToggleTafsir: (String) -> Unit = {},
    val onInstallPack: (String) -> Unit = {},
    val onRemovePack: (String) -> Unit = {},
    val onPackSetupCancel: () -> Unit = {},
    val onRemoveDownloads: suspend (String, Int) -> Unit = { _, _ -> },
    val onExport: () -> Unit = {},
    val onImport: () -> Unit = {},
    val onCheckContent: () -> Unit = {},
    val onOpenLink: (String) -> Unit = {},
)

/** A pack the reader asked for, while it downloads. */
data class PackSetupState(
    val packId: String,
    val name: String,
    val bytes: Long,
    val progress: Float?,
    val failed: Boolean,
)

/** What the last export or import did, so the row can say it once. */
sealed interface DataNotice {
    data object Exported : DataNotice
    data class Imported(val count: Int) : DataNotice
    data object NothingToExport : DataNotice
    data object NothingImported : DataNotice
    data object ImportFailed : DataNotice
}

/** What the content self check is doing, or what it found. */
sealed interface ContentCheck {
    data object Running : ContentCheck
    data class Done(val damaged: List<String>) : ContentCheck
}

/**
 * One sheet for everything the reader can choose: how the page looks, how it
 * behaves, who recites, which content packs are on, and their own saved work.
 * The sheet is grouped and each group is short enough to read at a glance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    recitations: List<Recitation>,
    surahs: List<Surah>,
    savedCount: Int,
    dataNotice: DataNotice?,
    contentCheck: ContentCheck?,
    version: String,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var downloadsOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }

    if (aboutOpen) {
        AboutSheet(
            packs = packs,
            version = version,
            onOpenLink = actions.onOpenLink,
            onDismiss = { aboutOpen = false },
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
            )

            Group(stringResource(R.string.settings_group_appearance))
            ThemeRow(settings.theme, settings.dimLevel) { actions.onTheme(it) }
            TextSizeRow(settings.textSize) { actions.onTextSize(it) }
            DimRow(settings.dimLevel) { actions.onDimLevel(it) }

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
                title = stringResource(R.string.settings_words_title),
                subtitle = stringResource(R.string.settings_words_subtitle),
                checked = settings.wordByWord,
            ) { actions.onWordByWord(it) }
            ToggleRow(
                title = stringResource(R.string.settings_footnotes_title),
                subtitle = stringResource(R.string.settings_footnotes_subtitle),
                checked = settings.showFootnotes,
            ) { actions.onShowFootnotes(it) }

            Group(stringResource(R.string.settings_group_recitation))
            packs.filter { it.type == PackType.Recitation }.forEach { pack ->
                PackRow(
                    pack = pack,
                    selected = pack.id == ContentDatabase.reciterPack(settings.recitation),
                    setup = packSetup?.takeIf { it.packId == pack.id },
                    onInstall = { actions.onInstallPack(pack.id) },
                    onRemove = { actions.onRemovePack(pack.id) },
                    onSelect = { actions.onSelectRecitation(pack.id.removePrefix(ContentDatabase.RECITER_PREFIX)) },
                )
            }
            TextRow(
                title = stringResource(
                    if (downloadsOpen) {
                        R.string.settings_downloaded_hide
                    } else {
                        R.string.settings_downloaded_show
                    },
                ),
            ) { downloadsOpen = !downloadsOpen }
            if (downloadsOpen) {
                DownloadedList(
                    settings = settings,
                    downloadedSurahs = downloadedSurahs,
                    onRemove = { surah ->
                        scope.launch { actions.onRemoveDownloads(settings.recitation, surah) }
                    },
                )
            }

            Group(stringResource(R.string.settings_group_content))
            Text(
                text = stringResource(R.string.settings_content_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
            )
            ContentPacks(
                settings = settings,
                packs = packs,
                packSetup = packSetup,
                actions = actions,
            )
            TextRow(
                title = stringResource(R.string.settings_check_content),
                subtitle = stringResource(R.string.settings_check_content_subtitle),
                onClick = actions.onCheckContent,
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

            Group(stringResource(R.string.settings_group_data))
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

            Group(stringResource(R.string.settings_group_about))
            AboutRow(stringResource(R.string.settings_version), version)
            AboutRow(
                stringResource(R.string.settings_ads),
                stringResource(R.string.settings_ads_none),
            )
            TextRow(title = stringResource(R.string.settings_privacy)) {
                actions.onOpenLink(PRIVACY_URL)
            }
            TextRow(title = stringResource(R.string.settings_source)) {
                actions.onOpenLink(SOURCE_URL)
            }
            TextRow(title = stringResource(R.string.settings_rights)) {
                actions.onOpenLink(RIGHTS_URL)
            }
            TextRow(title = stringResource(R.string.settings_credits)) { aboutOpen = true }
        }
    }
}

@Composable
private fun DataNotice.text(savedCount: Int): String = when (this) {
    DataNotice.Exported -> stringResource(R.string.settings_export_done, savedCount)
    is DataNotice.Imported -> stringResource(R.string.settings_import_done, count)
    DataNotice.NothingToExport -> stringResource(R.string.settings_export_empty)
    DataNotice.NothingImported -> stringResource(R.string.settings_import_none)
    DataNotice.ImportFailed -> stringResource(R.string.settings_import_failed)
}

/** The downloaded surahs of the chosen reciter, each with its size. */
@Composable
private fun DownloadedList(
    settings: AppSettings,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    onRemove: (Int) -> Unit,
) {
    val downloaded by produceState(initialValue = emptyList<DownloadedSurah>(), settings.recitation) {
        value = downloadedSurahs(settings.recitation).sortedBy { it.surah }
    }
    if (downloaded.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_downloaded_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 10.dp),
        )
        return
    }
    downloaded.forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatBytes(row.bytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.pack_action_remove),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .clickable { onRemove(row.surah) },
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}

/** Translations, tafsirs, and word lists, grouped by the language they speak. */
@Composable
private fun ContentPacks(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
) {
    packs
        .filter { it.type == PackType.Translation || it.type == PackType.Tafsir || it.type == PackType.Words }
        .groupBy { it.language }
        .entries
        .sortedBy { (language, _) -> languageOrder(language) }
        .forEach { (language, group) ->
            Text(
                text = languageName(language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 2.dp),
            )
            group.forEach { pack ->
                PackRow(
                    pack = pack,
                    selected = when (pack.type) {
                        PackType.Translation -> pack.id == settings.translationPack
                        PackType.Tafsir -> pack.id in settings.tafsirPacks
                        else -> true
                    },
                    setup = packSetup?.takeIf { it.packId == pack.id },
                    onInstall = { actions.onInstallPack(pack.id) },
                    onRemove = { actions.onRemovePack(pack.id) },
                    onSelect = {
                        when (pack.type) {
                            PackType.Translation -> actions.onTranslationPack(pack.id)
                            PackType.Tafsir -> actions.onToggleTafsir(pack.id)
                            else -> Unit
                        }
                    },
                )
            }
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

internal const val PRIVACY_URL = "https://muntasimulhaque.github.io/quran/privacy.html"
internal const val SOURCE_URL = "https://github.com/muntasimulhaque/quran"
internal const val RIGHTS_URL = "https://github.com/muntasimulhaque/quran/issues"
