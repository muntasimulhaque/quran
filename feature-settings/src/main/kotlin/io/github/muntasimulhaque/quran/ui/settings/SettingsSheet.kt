package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

/**
 * What the settings sheet can ask the app to do. The sheet itself owns no
 * state beyond what is open; every change is a call, so the feature can be
 * read, tested, and previewed without a view model.
 */
data class SettingsActions(
    val onTheme: (AppTheme) -> Unit = {},
    val onTypeSize: (TypeRole, Float) -> Unit = { _, _ -> },
    val onKeepAwake: (Boolean) -> Unit = {},
    val onFollowReciter: (Boolean) -> Unit = {},
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
    var page by remember { mutableStateOf<SettingsPage?>(null) }
    var credits by remember { mutableStateOf(false) }
    val hubScroll = rememberScrollState()

    // Back steps out of a page before it closes the sheet, the way a stack
    // of pages is expected to behave.
    BackHandler(enabled = page != null) { page = null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 30.dp),
        ) {
            val open = page
            if (open == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(hubScroll),
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
                        onOpen = { page = it },
                    )
                }
            } else {
                PageHeader(title = open.title(), onBack = { page = null })
                when (open) {
                    SettingsPage.Appearance -> AppearancePage(settings, actions.onTheme)
                    SettingsPage.Text -> TextPage(settings, preview) { role, step ->
                        actions.onTypeSize(role, step)
                    }
                    SettingsPage.Reading -> ReadingPage(settings, actions)
                    SettingsPage.Reciters -> RecitersPage(
                        settings = settings,
                        packs = packs,
                        packSetup = packSetup,
                        recitations = recitations,
                        downloadedSurahs = downloadedSurahs,
                        actions = actions,
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
                    SettingsPage.Words -> WordsPage(
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
