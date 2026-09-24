package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.StudyRow
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Reading

/**
 * What the settings sheet can ask the app to do. The sheet itself owns no
 * state beyond what is open; every change is a call, so the feature can be
 * read, tested, and previewed without a view model.
 */
data class SettingsActions(
    val onLanguage: (String) -> Unit = {},
    val onTheme: (AppTheme) -> Unit = {},
    val onAutoNight: (Boolean) -> Unit = {},
    val onTypeSize: (TypeRole, Float) -> Unit = { _, _ -> },
    val onKeepAwake: (Boolean) -> Unit = {},
    val onFollowReciter: (Boolean) -> Unit = {},
    val onPlaybackSpeed: (Float) -> Unit = {},
    val onRepeatAyah: (Boolean) -> Unit = {},
    val onWordByWord: (Boolean) -> Unit = {},
    val onSelectRecitation: (String) -> Unit = {},
    val onTranslationPack: (String) -> Unit = {},
    val onToggleTafsir: (String) -> Unit = {},
    val onToggleTranslation: (String) -> Unit = {},
    val onInstallPack: (String) -> Unit = {},
    val onRemovePack: (String) -> Unit = {},
    val onPackSetupCancel: () -> Unit = {},
    val onRemoveDownloads: suspend (String, Int) -> Unit = { _, _ -> },
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

/** What the content self check is doing, or what it found. */
sealed interface ContentCheck {
    data object Running : ContentCheck
    data class Done(val damaged: List<String>) : ContentCheck
}

/**
 * Settings: a hub of one row per category, each carrying where it stands, and
 * a page behind every row. Nothing is more than one tap from the hub, and a
 * reader who only came to change one thing never scrolls past the rest.
 *
 * The hub keeps its own scroll position, so closing a page, or the credits
 * behind the About page, returns the reader to the row they came from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    recitations: List<Recitation>,
    contentCheck: ContentCheck?,
    version: String,
    preview: suspend () -> StudyRow?,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // The page survives the window, not only the composition. A change of
    // language recreates the Activity, and a page kept in plain `remember`
    // went with the old window: the reader chose a language on the Language
    // page and came back to the reading. The open page and the open credits
    // are written into the saved state, so the sheet comes back up where the
    // choice was made, in the language just chosen.
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var credits by rememberSaveable { mutableStateOf(false) }
    val hubScroll = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // Back steps out of a page before it closes the sheet, the way a
        // stack of pages is expected to behave. It lives inside the sheet's
        // own window: the sheet registers its own back handling there, and a
        // handler in the activity's window never sees the event while the
        // sheet is up.
        BackHandler(enabled = page != null) { page = null }
        Column(
            modifier = Modifier
                // A settings row is a name and the place it stands, and on a
                // wide screen those two ends drift a foot apart. The sheet
                // keeps a column a person can read across, centered on the
                // ground, the way every other sheet and the study reading do.
                .align(Alignment.CenterHorizontally)
                .widthIn(max = Reading.MaxMeasure + 44.dp)
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 30.dp),
        ) {
            val open = page
            if (open == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sheetVerticalScroll(hubScroll),
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
                    )
                    SettingsHub(
                        settings = settings,
                        packs = packs,
                        recitations = recitations,
                        version = version,
                        packSetup = packSetup,
                        onKeepAwake = actions.onKeepAwake,
                        onOpen = { page = it },
                    )
                }
            } else {
                PageHeader(title = open.title(), onBack = { page = null })
                when (open) {
                    SettingsPage.Language -> LanguagePage(
                        settings = settings,
                        onLanguage = actions.onLanguage,
                    )
                    SettingsPage.Theme -> AppearancePage(
                        settings = settings,
                        onTheme = actions.onTheme,
                        onAutoNight = actions.onAutoNight,
                    )
                    SettingsPage.FontSize -> TextPage(settings, preview) { role, step ->
                        actions.onTypeSize(role, step)
                    }
                    SettingsPage.Reciters -> RecitersPage(
                        settings = settings,
                        packs = packs,
                        downloadedSurahs = downloadedSurahs,
                        actions = actions,
                    )
                    SettingsPage.Listening -> ListeningPage(
                        settings = settings,
                        onSpeed = actions.onPlaybackSpeed,
                        onRepeat = actions.onRepeatAyah,
                    )
                    SettingsPage.Translations -> TranslationsPage(
                        settings = settings,
                        packs = packs,
                        packSetup = packSetup,
                        actions = actions,
                    )
                    SettingsPage.Tafsirs -> TafsirsPage(
                        settings = settings,
                        packs = packs,
                        packSetup = packSetup,
                        actions = actions,
                    )
                    SettingsPage.About -> AboutPage(
                        version = version,
                        contentCheck = contentCheck,
                        onCredits = { credits = true },
                        onCheckContent = actions.onCheckContent,
                    )
                }
            }
        }
    }

    if (credits) {
        CreditsSheet(
            packs = packs,
            version = version,
            onOpenLink = actions.onOpenLink,
            onDismiss = { credits = false },
        )
    }
}

