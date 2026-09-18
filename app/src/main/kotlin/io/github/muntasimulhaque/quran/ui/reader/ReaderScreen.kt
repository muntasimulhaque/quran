package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import io.github.muntasimulhaque.quran.ui.browse.BrowseSheet
import io.github.muntasimulhaque.quran.ui.mushaf.MushafPage
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.shortReciterName
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.search.SearchSheet
import io.github.muntasimulhaque.quran.ui.settings.PackSetupState
import io.github.muntasimulhaque.quran.ui.settings.SettingsActions
import io.github.muntasimulhaque.quran.ui.settings.SettingsSheet
import io.github.muntasimulhaque.quran.ui.study.AyahCard
import io.github.muntasimulhaque.quran.ui.study.StudyList
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.PagePalette
import io.github.muntasimulhaque.quran.ui.theme.LocalPageThemeName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the chrome stays after a touch before it steps back. */
private const val CHROME_MILLIS = 7000L

/** Which sheet is over the reader, if any. */
private enum class ReaderSheet { None, Browse, Search, Settings, Saved }

/**
 * The reading screen: one surface, two modes, and chrome that only appears
 * when it is asked for. Everything else in the app happens in a sheet over
 * this surface or in the ayah card, so the page itself is never crowded.
 */
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    content: ContentDatabase,
    onPlaybackPermission: () -> Unit,
) {
    val settings = viewModel.settings
    val palette = LocalPagePalette.current
    val themeKey = LocalPageThemeName.current
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    var chrome by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Ayah?>(null) }
    var cardAyah by remember { mutableStateOf<Ayah?>(null) }
    var sheet by remember { mutableStateOf(ReaderSheet.None) }
    var touch by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Reading with the screen awake is part of reading.
    val view = LocalView.current
    LaunchedEffect(settings.keepAwake) { view.keepScreenOn = settings.keepAwake }

    // The chrome steps back on its own; a chosen ayah waits for the reader.
    LaunchedEffect(chrome, touch) {
        if (!chrome) return@LaunchedEffect
        delay(CHROME_MILLIS)
        chrome = false
    }

    fun copyAyah(text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Ayah", text))
    }

    fun shareAyah(text: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(android.content.Intent.createChooser(intent, null))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawWithContent {
                drawContent()
                // The dim never blocks a touch: it is ink over the glass.
                val dim = when (settings.dimLevel) {
                    1 -> 0.18f
                    2 -> 0.34f
                    else -> 0f
                }
                if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
            },
    ) {
        when (settings.mode) {
            ReadingMode.Mushaf -> MushafReader(
                viewModel = viewModel,
                content = content,
                palette = palette,
                themeKey = themeKey,
                selected = selected,
                playback = playback,
                onAyah = { ayah ->
                    selected = if (selected?.number == ayah.number) null else ayah
                    chrome = false
                    touch++
                },
                onBackgroundTap = {
                    chrome = !chrome
                    selected = null
                    touch++
                },
            )
            ReadingMode.Study -> {
                val surah = viewModel.surahOf(settings.ayah)
                if (surah != null) {
                    StudyList(
                        content = content,
                        surah = surah,
                        ayahs = viewModel.ayahNumbersOfSurah(surah.number),
                        settings = settings,
                        loadRow = { number -> viewModel.studyRow(number, settings.translationPack) },
                        hasTranslation = viewModel.installedTranslationPacks.isNotEmpty(),
                        nextSurahName = viewModel.surahs
                            .firstOrNull { it.number == surah.number + 1 }?.nameSimple,
                        selected = selected,
                        playingAyah = playback.ayahNumber,
                        playingWord = playback.wordPosition,
                        onAyah = { ayah ->
                            selected = ayah
                            chrome = false
                            touch++
                        },
                        onBackgroundTap = {
                            chrome = !chrome
                            selected = null
                            touch++
                        },
                        onNextSurah = { number -> viewModel.jumpToSurah(number) },
                        onAddContent = { sheet = ReaderSheet.Settings },
                        onPlaceChanged = { ayah -> viewModel.onStudySettled(ayah) },
                        contentPaddingTop = 64.dp,
                        contentPaddingBottom = 120.dp,
                    )
                }
            }
        }

        ReaderTopBar(
            title = surahName(viewModel, settings.ayah),
            detail = viewModel.position?.let { "Juz ${it.juz}" },
            visible = chrome,
            onBrowse = { sheet = ReaderSheet.Browse },
            onSearch = { sheet = ReaderSheet.Search },
            onSettings = { sheet = ReaderSheet.Settings },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // The long press is the richest gesture in the app and the least
        // visible, so it is said once, quietly, and then never again.
        if (!settings.longPressHintShown) {
            var hintVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(1400)
                hintVisible = true
                delay(8000)
                hintVisible = false
                viewModel.markLongPressHintShown()
            }
            AnimatedVisibility(
                visible = hintVisible,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            hintVisible = false
                            viewModel.markLongPressHintShown()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconGlyph(
                        icon = Icon.More,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Press and hold any ayah for its actions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }

        val bottomItems = selected != null || playback.isAnything() || chrome
        AnimatedVisibility(
            visible = bottomItems,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut() + slideOutVertically { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomStack(
                viewModel = viewModel,
                playback = playback,
                saved = saved,
                selected = selected,
                chrome = chrome,
                settings = settings,
                onPlay = { ayah ->
                    onPlaybackPermission()
                    viewModel.playAyah(ayah.number)
                },
                onDeselect = {
                    selected = null
                    touch++
                },
                onListen = {
                    val target = selected?.number ?: settings.ayah
                    onPlaybackPermission()
                    viewModel.playAyah(target)
                    selected = null
                },
                onMode = { viewModel.switchMode(it) },
                onSaved = { sheet = ReaderSheet.Saved },
                onReciter = { sheet = ReaderSheet.Settings },
                onMore = { ayah ->
                    cardAyah = ayah
                    selected = null
                },
                onCopyText = { text -> copyAyah(text) },
                onShareText = { text -> shareAyah(text) },
                onTouch = { touch++ },
            )
        }
    }

    when (sheet) {
        ReaderSheet.None -> Unit
        ReaderSheet.Browse -> BrowseSheet(
            content = content,
            surahs = viewModel.surahs,
            saved = saved,
            headers = viewModel.headers,
            translationPack = settings.translationPack,
            onDismiss = { sheet = ReaderSheet.None },
            onAyah = { ayahNumber ->
                sheet = ReaderSheet.None
                viewModel.jumpToAyah(ayahNumber)
            },
            onSurah = { number ->
                sheet = ReaderSheet.None
                viewModel.jumpToSurah(number)
            },
            onRemove = { viewModel.removeSaved(it) },
        )
        ReaderSheet.Saved -> BrowseSheet(
            content = content,
            surahs = viewModel.surahs,
            saved = saved,
            headers = viewModel.headers,
            translationPack = settings.translationPack,
            startOnSaved = true,
            onDismiss = { sheet = ReaderSheet.None },
            onAyah = { ayahNumber ->
                sheet = ReaderSheet.None
                viewModel.jumpToAyah(ayahNumber)
            },
            onSurah = { number ->
                sheet = ReaderSheet.None
                viewModel.jumpToSurah(number)
            },
            onRemove = { viewModel.removeSaved(it) },
        )
        ReaderSheet.Search -> SearchSheet(
            content = content,
            translationPacks = viewModel.translationPacks.map { it.id },
            tafsirPacks = viewModel.enabledTafsirPacks.map { it.id },
            packNames = viewModel.packs.associate { it.id to it.name },
            packLanguages = viewModel.packs.associate { it.id to it.language },
            onDismiss = { sheet = ReaderSheet.None },
            onAyah = { ayah, _ ->
                sheet = ReaderSheet.None
                viewModel.jumpToAyah(ayah.number)
            },
            onSurah = { surah ->
                sheet = ReaderSheet.None
                viewModel.jumpToSurah(surah.number)
            },
        )
        ReaderSheet.Settings -> SettingsSheet(
            settings = settings,
            packs = viewModel.packs,
            packSetup = viewModel.packSetup?.let { setup ->
                PackSetupState(
                    packId = setup.pack.id,
                    name = setup.pack.name,
                    bytes = setup.pack.bytes,
                    progress = setup.progress,
                    failed = setup.failed,
                )
            },
            recitations = viewModel.recitations,
            surahs = viewModel.surahs,
            downloadedSurahs = { recitation -> viewModel.downloadedSurahs(recitation) },
            actions = SettingsActions(
                onTheme = { viewModel.setTheme(it) },
                onTextSize = { viewModel.setTextSize(it) },
                onKeepAwake = { viewModel.setKeepAwake(it) },
                onFollowReciter = { viewModel.setFollowReciter(it) },
                onShowFootnotes = { viewModel.setShowFootnotes(it) },
                onDimLevel = { viewModel.setDimLevel(it) },
                onSelectRecitation = { viewModel.selectRecitation(it) },
                onTranslationPack = { viewModel.setTranslationPack(it) },
                onToggleTafsir = { viewModel.toggleTafsirPack(it) },
                onInstallPack = { viewModel.installPack(it) },
                onRemovePack = { viewModel.removePack(it) },
                onPackSetupCancel = { viewModel.cancelPackSetup() },
                onRemoveDownloads = { recitation, surah ->
                    viewModel.removeDownloads(recitation, surah)
                },
            ),
            onDismiss = { sheet = ReaderSheet.None },
        )
    }

    cardAyah?.let { ayah ->
        val savedRow = saved.firstOrNull { it.ayahNumber == ayah.number }
        AyahCard(
            content = content,
            ayah = ayah,
            surahName = viewModel.surahs.firstOrNull { it.number == ayah.surah }?.nameSimple
                ?: "Surah ${ayah.surah}",
            translationPack = viewModel.translationPacks.firstOrNull { it.id == settings.translationPack },
            tafsirPacks = viewModel.enabledTafsirPacks,
            textSize = settings.textSize,
            isSaved = savedRow != null,
            note = savedRow?.note,
            onToggleSave = { viewModel.toggleSaved(ayah) },
            onSaveNote = { note -> viewModel.setNote(ayah, note) },
            onPlay = {
                onPlaybackPermission()
                viewModel.playAyah(ayah.number)
                cardAyah = null
            },
            onCopy = { text -> copyAyah(text) },
            onShare = { text -> shareAyah(text) },
            onDismiss = { cardAyah = null },
        )
    }
}

@Composable
private fun MushafReader(
    viewModel: ReaderViewModel,
    content: ContentDatabase,
    palette: PagePalette,
    themeKey: String,
    selected: Ayah?,
    playback: PlaybackUiState,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = (viewModel.page - 1).coerceIn(0, 603),
        pageCount = { 604 },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled ->
            viewModel.onPageSettled(settled + 1)
        }
    }
    // A jump from the cards or the sheets moves the pager; a swipe never
    // moves the pager from here.
    LaunchedEffect(viewModel.page) {
        val target = (viewModel.page - 1).coerceIn(0, 603)
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .semantics(mergeDescendants = false) { },
        beyondViewportPageCount = 1,
    ) { index ->
        MushafPage(
            content = content,
            fonts = viewModel.fonts,
            renderer = viewModel.renderer,
            page = index + 1,
            palette = palette,
            themeKey = themeKey,
            selectedAyah = selected?.number,
            playingAyah = playback.ayahNumber,
            playingWord = playback.wordPosition,
            onAyah = onAyah,
            onLongPressAyah = onAyah,
            onBackgroundTap = onBackgroundTap,
            modifier = Modifier.padding(
                top = 0.dp,
            ),
        )
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    detail: String?,
    visible: Boolean,
    onBrowse: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 3 },
        exit = fadeOut() + slideOutVertically { -it / 3 },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                        ),
                    ),
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReadingTitle(surah = title, detail = detail)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                IconButton(Icon.Browse, "Browse the Quran", onBrowse)
                IconButton(Icon.Search, "Search", onSearch)
                IconButton(Icon.Settings, "Settings", onSettings)
            }
        }
    }
}

/** Everything that can sit at the foot of the page, stacked in one place. */
@Composable
private fun BottomStack(
    viewModel: ReaderViewModel,
    playback: PlaybackUiState,
    saved: List<SavedAyah>,
    selected: Ayah?,
    chrome: Boolean,
    settings: io.github.muntasimulhaque.quran.data.AppSettings,
    onPlay: (Ayah) -> Unit,
    onDeselect: () -> Unit,
    onListen: () -> Unit,
    onMode: (ReadingMode) -> Unit,
    onSaved: () -> Unit,
    onReciter: () -> Unit,
    onMore: (Ayah) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit,
    onTouch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val scope = rememberCoroutineScope()
        viewModel.packSetup?.let { setup ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.padding(end = 12.dp)) {
                    Text(
                        text = setup.pack.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = when {
                            setup.failed -> "Could not download. Check your connection."
                            setup.progress == null -> "Preparing..."
                            else -> "Downloading ${(setup.progress * 100).toInt()}%  \u00B7  ${formatBytes(setup.pack.bytes)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (setup.failed) {
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { viewModel.installPack(setup.pack.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Text(
                    text = if (setup.failed) "Close" else "Cancel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { viewModel.cancelPackSetup() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        selected?.let { ayah ->
            AyahActions(
                ayah = ayah,
                surahName = viewModel.surahs.firstOrNull { it.number == ayah.surah }?.nameSimple
                    ?: "Surah ${ayah.surah}",
                isSaved = saved.any { it.ayahNumber == ayah.number },
                onSave = {
                    viewModel.toggleSaved(ayah)
                    onTouch()
                },
                onPlay = {
                    onPlay(ayah)
                    onDeselect()
                },
                onMore = { onMore(ayah) },
                onCopy = {
                    scope.launch { onCopyText(viewModel.ayahShareText(ayah)) }
                },
                onShare = {
                    scope.launch { onShareText(viewModel.ayahShareText(ayah)) }
                },
            )
        }

        if (playback.isAnything()) {
            PlaybackBar(
                state = playback,
                reciterName = shortReciterName(
                    settings.recitation,
                    viewModel.recitations.firstOrNull { it.id == settings.recitation }?.name.orEmpty(),
                ),
                reference = playback.reference,
                pendingLabel = playback.pendingDownloadSurah?.let { surah ->
                    val name = viewModel.surahs.firstOrNull { it.number == surah }?.nameSimple
                        ?: "Surah $surah"
                    val size = formatBytes(playback.pendingDownloadBytes)
                    if (playback.pendingIsContinuation) "Continue to $name  \u00B7  $size" else "$name  \u00B7  $size"
                },
                onToggle = { viewModel.togglePlayback() },
                onNext = { viewModel.nextAyah() },
                onPrevious = { viewModel.previousAyah() },
                onReciter = onReciter,
                onDownload = { viewModel.confirmDownload() },
                onClose = {
                    if (playback.pendingDownloadSurah != null || playback.downloadFailed) {
                        viewModel.cancelDownload()
                    } else {
                        viewModel.stopPlayback()
                    }
                },
            )
        }

        if (chrome && selected == null) {
            ReaderBottomBar(
                mode = settings.mode,
                onMode = onMode,
                onListen = onListen,
                onSaved = onSaved,
                listenActive = playback.isPlaying,
            )
        }
    }
}

@Composable
private fun ReaderBottomBar(
    mode: ReadingMode,
    onMode: (ReadingMode) -> Unit,
    onListen: () -> Unit,
    onSaved: () -> Unit,
    listenActive: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabeledIconButton(Icon.Listen, "Listen", onListen, active = listenActive)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
            ModeSwitch(mode, onMode)
        }
        LabeledIconButton(Icon.Bookmark, "Saved", onSaved)
    }
}

/** How anything playing or pending shows at the foot of the page. */
internal fun PlaybackUiState.isAnything(): Boolean =
    ayahNumber != null || unavailable || pendingDownloadSurah != null || downloadFailed

private fun surahName(viewModel: ReaderViewModel, ayah: Int): String =
    viewModel.surahOf(ayah)?.nameSimple ?: "Quran"

@Composable
private fun AyahActions(
    ayah: Ayah,
    surahName: String,
    isSaved: Boolean,
    onSave: () -> Unit,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = 10.dp)) {
            Text(
                text = surahName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${ayah.surah}:${ayah.ayah}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        TextAction(if (isSaved) "Saved" else "Save", if (isSaved) Icon.BookmarkFilled else Icon.Bookmark, onSave, active = isSaved)
        TextAction("Play", Icon.Play, onPlay)
        TextAction("Copy", Icon.Copy, onCopy)
        TextAction("Share", Icon.Share, onShare)
        TextAction("More", Icon.More, onMore)
    }
}
