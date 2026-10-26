package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.muntasimulhaque.quran.BuildConfig
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ReadPlace
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import io.github.muntasimulhaque.quran.ui.browse.BrowseSheet
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.search.SearchArrival
import io.github.muntasimulhaque.quran.ui.search.SearchSheet
import io.github.muntasimulhaque.quran.ui.settings.PackSetupState
import io.github.muntasimulhaque.quran.ui.settings.SettingsActions
import io.github.muntasimulhaque.quran.ui.settings.SettingsSheet

/**
 * The sheets that rise over the reading: Browse, Search, and Settings. The
 * reading screen keeps their state and decides when one is open; this file
 * draws the one that is open and answers it.
 */
@Composable
internal fun ReaderSheets(
    sheet: ReaderSheet,
    content: ContentDatabase,
    viewModel: ReaderViewModel,
    settings: AppSettings,
    saved: List<SavedAyah>,
    lastRead: List<ReadPlace>,
    notificationsBlocked: () -> Boolean,
    exactGrantWithheld: () -> Boolean,
    onOpenNotificationSettings: () -> Unit,
    onAskExactAlarm: () -> Unit,
    onReminderPermission: () -> Unit,
    onDismiss: () -> Unit,
    /** A sheet's result opens the sheet's way back. */
    onResult: (ReaderSheet) -> Unit,
    /** A search result arrives with the evidence the reading should show. */
    onSearchResult: (SearchArrival) -> Unit,
    onPendingRemove: (Int) -> Unit,
) {
    val context = LocalContext.current
    val wordsPack = ContentDatabase.wordsPackId(viewModel.readingLanguage)
    when (sheet) {
        ReaderSheet.None -> Unit
        ReaderSheet.Browse -> BrowseSheet(
            content = content,
            surahs = viewModel.surahs,
            saved = saved,
            lastRead = lastRead,
            currentAyah = settings.ayah,
            onDismiss = onDismiss,
            onAyah = { ayahNumber ->
                onResult(ReaderSheet.Browse)
                viewModel.jumpToAyah(ayahNumber)
            },
            onSurah = { number ->
                onResult(ReaderSheet.Browse)
                // From Browse, a surah opens where the reader left it, or at
                // its top when they have never been there.
                viewModel.openSurah(number)
            },
            onRemoveSaved = { ayahNumber ->
                // A save that carries a note asks before it goes, so the
                // reader's own words are never removed by one tap. The
                // question opens over the sheet the reader is still standing
                // in.
                val note = saved.firstOrNull { it.ayahNumber == ayahNumber }?.note
                if (note.isNullOrBlank()) {
                    viewModel.removeSaved(ayahNumber)
                } else {
                    onPendingRemove(ayahNumber)
                }
            },
            onForget = { viewModel.forgetPlace(it) },
        )
        ReaderSheet.Search -> SearchSheet(
            content = content,
            // Search reads the language the reader chose and no other: a
            // Bangla reader searches Bangla translations and tafsirs even
            // when an English column was added by hand (owner decision, this
            // session).
            translationPacks = remember(viewModel.searchTranslationPacks) {
                viewModel.searchTranslationPacks.map { it.id }
            },
            tafsirPacks = remember(viewModel.searchTafsirPacks) {
                viewModel.searchTafsirPacks.map { it.id }
            },
            packNames = remember(viewModel.packs) { viewModel.packs.associate { it.id to it.name } },
            packLanguages = remember(viewModel.packs) {
                viewModel.packs.associate { it.id to it.language }
            },
            // The word list follows the same one language, and never falls
            // back to another: a reader who chose Bangla must not meet
            // English meanings in the results.
            wordsPack = wordsPack,
            wordsInstalled = content.installedPack(wordsPack),
            // The reader's own Arabic step, so a reader who enlarged the
            // Quran's text reads the results at the same preference.
            arabicStep = settings.arabicSize,
            onDismiss = onDismiss,
            onAyah = { arrival ->
                onSearchResult(arrival)
            },
            onSurah = { surah ->
                onResult(ReaderSheet.Search)
                viewModel.jumpToSurah(surah.number)
            },
        )
        ReaderSheet.Settings -> SettingsSheet(
            settings = settings,
            packs = viewModel.packs,
            packSetup = viewModel.packSetup?.let { setup ->
                PackSetupState(
                    packId = setup.pack.id,
                    name = contentPackName(setup.pack.id, setup.pack.name),
                    bytes = setup.pack.bytes,
                    progress = setup.progress,
                    failure = setup.failure,
                )
            },
            recitations = viewModel.recitations,
            version = BuildConfig.VERSION_NAME,
            preview = { viewModel.sizePreviewRow() },
            tajweedSample = { viewModel.tajweedSampleAyah() },
            downloadedSurahs = { recitation -> viewModel.downloadedSurahs(recitation) },
            // Read where it is drawn, so the Daily page tells the truth about
            // the phone's own switch even when the reader comes back from the
            // system settings without the app restarting.
            notificationsBlocked = notificationsBlocked,
            exactGrantWithheld = exactGrantWithheld,
            actions = SettingsActions(
                onLanguage = { tag ->
                    UiLanguage.of(tag)?.let { language ->
                        // The language already chosen is not chosen again: a
                        // second recreation for the same answer would close
                        // the page for nothing.
                        if (language.tag == settings.uiLanguage) return@let
                        viewModel.chooseLanguage(language)
                        // The locale belongs to the Activity's own resources;
                        // the recreation brings every window up speaking it.
                        (context as? android.app.Activity)?.recreate()
                    }
                },
                onTheme = { viewModel.setTheme(it) },
                onTypeSize = { role, step -> viewModel.setTypeSize(role, step) },
                onFollowReciter = { viewModel.setFollowReciter(it) },
                onShowTranslation = { viewModel.setShowTranslation(it) },
                onShowTafsir = { viewModel.setShowTafsir(it) },
                onWordByWord = { viewModel.setWordByWord(it) },
                onTajweed = { viewModel.setTajweed(it) },
                onDailyAyah = { enabled ->
                    viewModel.setDailyAyah(enabled, onReminderPermission)
                },
                onDailyAyahTime = { minute ->
                    // Moving the moment is as clear a yes as turning the
                    // switch, so it asks for the same permission.
                    viewModel.setDailyAyahTime(minute, onReminderPermission)
                },
                onOpenNotificationSettings = onOpenNotificationSettings,
                onAskExactAlarm = onAskExactAlarm,
                onSelectRecitation = { viewModel.selectRecitation(it) },
                onToggleTranslation = { viewModel.toggleTranslationPack(it) },
                onToggleTafsir = { viewModel.toggleTafsirPack(it) },
                onInstallPack = { viewModel.installPack(it) },
                onRemovePack = { viewModel.removePack(it) },
                onPackSetupCancel = { viewModel.cancelPackSetup() },
                onRemoveDownloads = { recitation, surah ->
                    viewModel.removeDownloads(recitation, surah)
                },
            ),
            onDismiss = onDismiss,
        )
    }
}
