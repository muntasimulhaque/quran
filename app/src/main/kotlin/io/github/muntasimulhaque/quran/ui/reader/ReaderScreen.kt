package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import io.github.muntasimulhaque.quran.BuildConfig
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import io.github.muntasimulhaque.quran.ui.browse.BrowseSheet
import io.github.muntasimulhaque.quran.ui.mushaf.MushafPage
import io.github.muntasimulhaque.quran.ui.mushaf.PAGE_ASPECT
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.shortReciterName
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.search.SearchSheet
import io.github.muntasimulhaque.quran.ui.settings.PackSetupState
import io.github.muntasimulhaque.quran.ui.settings.SettingsActions
import io.github.muntasimulhaque.quran.ui.settings.SettingsSheet
import io.github.muntasimulhaque.quran.ui.study.AyahCard
import io.github.muntasimulhaque.quran.ui.study.AyahNoteSheet
import io.github.muntasimulhaque.quran.ui.study.StudyList
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.PagePalette
import io.github.muntasimulhaque.quran.ui.theme.LocalPageThemeName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

/** How long the chrome stays after a touch before it steps back. */
private const val CHROME_MILLIS = 7000L

/** The bar's own margins; the title aligns with the reading under it. */
private val BarStart = 20.dp
private val BarEnd = 8.dp

/** Which sheet is over the reader, if any. */
private enum class ReaderSheet { None, Browse, Search, Settings }

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
    val lastRead by viewModel.lastRead.collectAsStateWithLifecycle()

    var chrome by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Ayah?>(null) }
    var cardAyah by remember { mutableStateOf<Ayah?>(null) }
    // The note sheet is keyed by the ayah's number, not by the ayah itself:
    // the same sheet opens from the reading's pill and from a Notes row in
    // Browse, and Browse only knows the number until the reader is there.
    var cardNote by remember { mutableStateOf<Int?>(null) }
    // The open sheet survives the window, not only the composition: a change
    // of language recreates the Activity, and a sheet kept in plain `remember`
    // closed with the old window while the reader was still in it. The reader
    // chose a language from the Language page and was returned to the reading;
    // the sheet is written into the saved state, so the choice is answered
    // where it was made (owner decision, 21).
    var sheet by rememberSaveable { mutableStateOf(ReaderSheet.None) }
    var touch by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    // A share is a card: the parts are read off the main thread, then the
    // card composes invisibly, is recorded into a layer, and leaves as a
    // PNG through the chooser. While this holds a card the capture is
    // composing for the frame alone; it is never drawn to the screen.
    var sharing by remember { mutableStateOf<ShareCard?>(null) }

    // The wash under an ayah: the one the long press chose, or the one whose
    // note is open over it. A note opened from Browse has no pill behind it,
    // but the reader is still looking at the ayah the note belongs to, so it
    // wears the same mark while the sheet is up.
    val washedAyah = selected?.number ?: cardNote

    // The phone's back button dismisses the ayah pill first, and only closes
    // the app when nothing is raised. A reader who long-pressed an ayah means
    // the pill, and back is the gesture they already use to put a thing away.
    // The sheets (the ayah card, the note, Browse, Search, Settings) are their
    // own windows and take back through their own handlers; this one is for
    // the pill, which is not a window.
    androidx.activity.compose.BackHandler(enabled = selected != null) {
        selected = null
        touch++
    }

    // Reading with the screen awake is part of reading.
    val view = LocalView.current
    LaunchedEffect(settings.keepAwake) { view.keepScreenOn = settings.keepAwake }

    // The chrome steps back on its own; a chosen ayah waits for the reader.
    LaunchedEffect(chrome, touch) {
        if (!chrome) return@LaunchedEffect
        delay(CHROME_MILLIS)
        chrome = false
    }

    fun sharePlainText(text: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(android.content.Intent.createChooser(intent, null))
        }
    }

    /**
     * The card as an image beside the plain text: the PNG goes out through
     * the app's own content provider and the text rides along as the caption
     * every receiving app can use. Anything that fails along the way (a full
     * cache, a provider that will not resolve) falls back to the plain text,
     * so a Share is never a tap that did nothing.
     */
    fun shareCard(image: ImageBitmap, text: String) {
        scope.launch {
            val file = withContext(Dispatchers.IO) { writeShareCard(context, image) }
            val opened = file != null && startShareCard(context, file, text)
            if (!opened) sharePlainText(text)
        }
    }

    fun openLink(target: String) {
        runCatching {
            context.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, target.toUri()),
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (settings.mode) {
            ReadingMode.Mushaf -> MushafReader(
                viewModel = viewModel,
                content = content,
                palette = palette,
                themeKey = themeKey,
                selectedAyah = washedAyah,
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
                        loadRows = {
                            viewModel.studyRows(
                                surah = surah.number,
                                wordByWord = settings.wordByWord,
                            )
                        },
                        settings = settings,
                        hasTranslation = viewModel.enabledTranslationPacks.isNotEmpty(),
                        nextSurahName = viewModel.surahs
                            .firstOrNull { it.number == surah.number + 1 }?.nameSimple,
                        selectedAyah = washedAyah,
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
                        onScrolled = {
                            // The text is what the reader is looking at; the
                            // chrome steps out of its way the moment the page
                            // starts moving under a finger.
                            if (chrome) {
                                chrome = false
                                touch++
                            }
                        },
                        onNextSurah = { number -> viewModel.jumpToSurah(number) },
                        onAddContent = { sheet = ReaderSheet.Settings },
                        onPlaceChanged = { ayah -> viewModel.onStudySettled(ayah) },
                        startAtOpening = viewModel.startAtSurahOpening == surah.number,
                        onOpeningReached = { viewModel.consumeSurahOpening(surah.number) },
                        contentPaddingTop = 64.dp,
                        contentPaddingBottom = 120.dp,
                    )
                }
            }
        }

        ReaderTopBar(
            title = surahName(viewModel, settings.ayah),
            detail = viewModel.position?.let { stringResource(R.string.juz_label, it.juz) },
            visible = chrome,
            onBrowse = { sheet = ReaderSheet.Browse },
            onSearch = { sheet = ReaderSheet.Search },
            onSettings = { sheet = ReaderSheet.Settings },
            mode = settings.mode,
            onMode = { viewModel.switchMode(it) },
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
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        // A control that floats over the reading wears the
                        // floating tone and a soft lift, not the sheet's own
                        // surface: the page runs around it, and it has to read
                        // as above the page rather than printed into it.
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                        text = stringResource(R.string.hint_long_press_ayah),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }

        val bottomItems = selected != null || playback.isAnything() || viewModel.listenOffer != null
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
                fromMushaf = settings.mode == ReadingMode.Mushaf,
                onPlay = { ayah ->
                    onPlaybackPermission()
                    viewModel.playAyah(ayah.number)
                },
                onDeselect = {
                    selected = null
                    touch++
                },
                onMore = { ayah ->
                    cardAyah = ayah
                    selected = null
                },
                onNote = { ayah -> cardNote = ayah.number },
                onShare = { ayah ->
                    scope.launch { sharing = loadShareCard(context, viewModel, ayah) }
                },
                onTouch = { touch++ },
                onReciter = { sheet = ReaderSheet.Settings },
                reciterName = shortReciterName(
                    settings.recitation,
                    viewModel.recitations.firstOrNull { it.id == settings.recitation }?.name.orEmpty(),
                ),
            )
        }

        // The share card, composed for its capture alone. It never reaches
        // the screen: the layer it records into is read back and then the
        // whole node leaves the composition.
        sharing?.let { card ->
            ShareCardCapture(
                card = card,
                onImage = { image ->
                    sharing = null
                    shareCard(image, card.text)
                },
                onFailed = {
                    sharing = null
                    sharePlainText(card.text)
                },
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }

    when (sheet) {
        ReaderSheet.None -> Unit
        ReaderSheet.Browse -> BrowseSheet(
            content = content,
            surahs = viewModel.surahs,
            saved = saved,
            lastRead = lastRead,
            currentAyah = settings.ayah,
            onDismiss = { sheet = ReaderSheet.None },
            onAyah = { ayahNumber ->
                sheet = ReaderSheet.None
                viewModel.jumpToAyah(ayahNumber)
            },
            onSurah = { number ->
                sheet = ReaderSheet.None
                // From Browse, a surah opens where the reader left it, or at
                // its top when they have never been there.
                viewModel.openSurah(number)
            },
            onRemoveSaved = { viewModel.removeSaved(it) },
            onForget = { viewModel.forgetPlace(it) },
            onNote = { ayahNumber ->
                // The row the reader tapped says "a note on this ayah": the
                // reading goes to the ayah and the note opens over it, so the
                // words the note was written about are under the sheet.
                sheet = ReaderSheet.None
                viewModel.jumpToAyah(ayahNumber)
                cardNote = ayahNumber
            },
            onRemoveNote = { viewModel.removeNote(it) },
        )
        ReaderSheet.Search -> SearchSheet(
            content = content,
            translationPacks = viewModel.enabledTranslationPacks.map { it.id },
            tafsirPacks = viewModel.enabledTafsirPacks.map { it.id },
            packNames = viewModel.packs.associate { it.id to it.name },
            packLanguages = viewModel.packs.associate { it.id to it.language },
            wordsPack = content.meaningPack(viewModel.wordLanguage) ?: ContentDatabase.WORDS_PACK,
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
            contentCheck = viewModel.contentCheck,
            version = BuildConfig.VERSION_NAME,
            preview = { viewModel.sizePreviewRow() },
            downloadedSurahs = { recitation -> viewModel.downloadedSurahs(recitation) },
            actions = SettingsActions(
                onLanguage = { tag ->
                    io.github.muntasimulhaque.quran.data.UiLanguage.of(tag)?.let { language ->
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
                onAutoNight = { viewModel.setAutoNight(it) },
                onTypeSize = { role, step -> viewModel.setTypeSize(role, step) },
                onKeepAwake = { viewModel.setKeepAwake(it) },
                onFollowReciter = { viewModel.setFollowReciter(it) },
                onWordByWord = { viewModel.setWordByWord(it) },
                onSelectRecitation = { viewModel.selectRecitation(it) },
                onToggleTranslation = { viewModel.toggleTranslationPack(it) },
                onToggleTafsir = { viewModel.toggleTafsirPack(it) },
                onInstallPack = { viewModel.installPack(it) },
                onRemovePack = { viewModel.removePack(it) },
                onPackSetupCancel = { viewModel.cancelPackSetup() },
                onRemoveDownloads = { recitation, surah ->
                    viewModel.removeDownloads(recitation, surah)
                },
                onCheckContent = { viewModel.checkContent() },
                onOpenLink = { url -> openLink(url) },
            ),
            onDismiss = { sheet = ReaderSheet.None },
        )
    }

    cardAyah?.let { ayah ->
        AyahCard(
            content = content,
            ayah = ayah,
            surahName = viewModel.surahs.firstOrNull { it.number == ayah.surah }?.nameSimple
                ?: stringResource(R.string.surah_fallback_name, ayah.surah),
            translations = viewModel.enabledTranslationPacks,
            tafsirPacks = viewModel.enabledTafsirPacks,
            settings = settings,
            wordLanguage = viewModel.wordLanguage,
            hasWords = content.meaningPack(viewModel.wordLanguage) != null,
            fromMushaf = settings.mode == ReadingMode.Mushaf,
            onAddContent = {
                cardAyah = null
                sheet = ReaderSheet.Settings
            },
            onDismiss = { cardAyah = null },
        )
    }

    cardNote?.let { ayahNumber ->
        val savedRow = saved.firstOrNull { it.ayahNumber == ayahNumber }
        AyahNoteSheet(
            note = savedRow?.note,
            onSave = { note ->
                viewModel.setNote(ayahNumber, note)
                cardNote = null
            },
            onDismiss = { cardNote = null },
        )
    }
}

@Composable
private fun MushafReader(
    viewModel: ReaderViewModel,
    content: ContentDatabase,
    palette: PagePalette,
    themeKey: String,
    selectedAyah: Int?,
    playback: PlaybackUiState,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val availableHeight = with(density) { maxHeight.toPx() }
        // The page's height follows from its width; every page is rendered at
        // the width it will actually be drawn at, and never larger.
        val pageWidth = min(availableWidth, availableHeight / PAGE_ASPECT).toInt().coerceAtLeast(1)
        val pagerState = rememberPagerState(
            initialPage = (viewModel.page - 1).coerceIn(0, 603),
            pageCount = { 604 },
        )
        val haptics = LocalHapticFeedback.current
        val startup = viewModel.startupPage

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { settled ->
                viewModel.onPageSettled(settled + 1, pageWidth, themeKey)
            }
        }
        // A page that has stopped moving marks its settle with one light tick.
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { scrolling ->
                    if (!scrolling) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
        }
        // A jump from the cards or the sheets moves the pager; a swipe never
        // moves the pager from here. A far jump lands at once: animating
        // across three hundred pages would render all of them on the way.
        LaunchedEffect(viewModel.page) {
            val target = (viewModel.page - 1).coerceIn(0, 603)
            val current = pagerState.currentPage
            if (current == target) return@LaunchedEffect
            if (kotlin.math.abs(target - current) > 2) {
                pagerState.scrollToPage(target)
            } else {
                pagerState.animateScrollToPage(target)
            }
        }
        // Once the first real page is on screen the picture has served its
        // purpose and its memory is given back.
        LaunchedEffect(pagerState.settledPage, startup) {
            if (startup != null && startup.page == pagerState.settledPage + 1) {
                delay(600)
                viewModel.releaseStartupPage()
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .semantics(mergeDescendants = false) { },
            // An Arabic Mushaf opens on the right: the next page lies to the
            // left of this one, so a swipe to the right turns forward.
            reverseLayout = true,
            beyondViewportPageCount = 1,
        ) { index ->
            MushafPage(
                content = content,
                fonts = viewModel.fonts,
                renderer = viewModel.renderer,
                page = index + 1,
                pageWidth = pageWidth,
                palette = palette,
                themeKey = themeKey,
                selectedAyah = selectedAyah,
                playingAyah = playback.ayahNumber,
                playingWord = playback.wordPosition,
                onLongPressAyah = onAyah,
                onBackgroundTap = onBackgroundTap,
                active = index == pagerState.currentPage,
                placeholder = startup
                    ?.takeIf { it.page == index + 1 && it.widthPx == pageWidth && it.theme == themeKey }
                    ?.bitmap,
            )
        }
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
    mode: ReadingMode,
    onMode: (ReadingMode) -> Unit,
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
                // The scrim stays solid behind the controls and gives way
                // only below the name, so the surah never sits on the fading
                // edge and the page runs out from under the bar.
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background,
                        0.72f to MaterialTheme.colorScheme.background,
                        0.9f to MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                        1f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                    ),
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = BarStart, end = BarEnd, top = 8.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The bar is one row: Browse and the mode door lead, the surah
            // and its juz sit centered between the two pairs, and Search and
            // Settings close it. Two doors on each side is what keeps the
            // title on the screen's own center, and the single mode icon
            // leaves the name the room the old two-choice switch took.
            IconButton(Icon.Browse, stringResource(R.string.action_browse), onBrowse)
            ModeDoor(mode, onMode)
            ReadingTitle(
                surah = title,
                detail = detail,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )
            IconButton(Icon.Search, stringResource(R.string.action_search), onSearch)
            IconButton(Icon.Settings, stringResource(R.string.action_settings), onSettings)
        }
    }
}

/**
 * The two reading modes as one door: the glyph shows the mode the reader is
 * not in, and a tap takes them there. One icon instead of a two-choice
 * switch, so the bar keeps a single row and the surah name keeps its room;
 * the accent says this door is the reading itself, not another tool.
 */
@Composable
private fun ModeDoor(mode: ReadingMode, onMode: (ReadingMode) -> Unit) {
    val other = if (mode == ReadingMode.Mushaf) ReadingMode.Study else ReadingMode.Mushaf
    val description = stringResource(
        if (other == ReadingMode.Mushaf) {
            io.github.muntasimulhaque.quran.uikit.R.string.mode_switch_to_mushaf
        } else {
            io.github.muntasimulhaque.quran.uikit.R.string.mode_switch_to_study
        },
    )
    IconButton(
        icon = if (other == ReadingMode.Mushaf) Icon.MushafPage else Icon.StudyPage,
        description = description,
        onClick = { onMode(other) },
        active = true,
    )
}

/** Everything that can sit at the foot of the page, stacked in one place. */
@Composable
private fun BottomStack(
    viewModel: ReaderViewModel,
    playback: PlaybackUiState,
    saved: List<SavedAyah>,
    selected: Ayah?,
    fromMushaf: Boolean,
    onPlay: (Ayah) -> Unit,
    onDeselect: () -> Unit,
    onNote: (Ayah) -> Unit,
    onMore: (Ayah) -> Unit,
    onShare: (Ayah) -> Unit,
    onTouch: () -> Unit,
    onReciter: () -> Unit,
    reciterName: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        viewModel.packSetup?.let { setup ->
            Row(
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            setup.failed -> stringResource(R.string.pack_download_failed)
                            setup.progress == null -> stringResource(R.string.reader_pack_preparing)
                            else -> stringResource(
                                R.string.reader_pack_downloading,
                                (setup.progress * 100).toInt(),
                                formatBytes(setup.pack.bytes),
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (setup.failed) {
                    TextButton(
                        label = stringResource(R.string.action_retry),
                        onClick = { viewModel.installPack(setup.pack.id) },
                    )
                }
                TextButton(
                    label = stringResource(
                        if (setup.failed) R.string.action_close else R.string.action_cancel,
                    ),
                    onClick = { viewModel.cancelPackSetup() },
                    modifier = Modifier.padding(start = 6.dp),
                    quiet = true,
                )
            }
        }
        selected?.let { ayah ->
            AyahActions(
                isSaved = saved.any { it.ayahNumber == ayah.number && it.saved },
                hasNote = saved.any { it.ayahNumber == ayah.number && !it.note.isNullOrBlank() },
                // The deeper door names what is actually behind it: from the
                // study reading the card is only the tafsir doors, so the
                // action says Tafsir; from the Mushaf the card is the whole
                // study surface (the meanings, the translation, the tafsirs),
                // so the name stays More and the glyph promises everything.
                fromMushaf = fromMushaf,
                onSave = {
                    viewModel.toggleSaved(ayah)
                    onTouch()
                },
                onPlay = {
                    onPlay(ayah)
                    onDeselect()
                },
                onNote = { onNote(ayah) },
                onMore = { onMore(ayah) },
                onShare = { onShare(ayah) },
            )
        }

        val offer = viewModel.listenOffer
        if (playback.isAnything() || offer != null) {
            PlaybackBar(
                state = playback,
                offer = offer,
                offerTitle = offer?.let { pending ->
                    stringResource(R.string.playback_offer, pending.surahName, formatBytes(pending.bytes))
                }.orEmpty(),
                onOfferConfirm = { viewModel.confirmListen() },
                onOfferCancel = { viewModel.cancelListen() },
                onOfferReciter = { viewModel.chooseListenReciter(it) },
                reciterName = reciterName,
                reference = playback.reference,
                pendingLabel = playback.pendingDownloadSurah?.let { surah ->
                    val name = viewModel.surahs.firstOrNull { it.number == surah }?.nameSimple
                        ?: stringResource(R.string.surah_fallback_name, surah)
                    val size = formatBytes(playback.pendingDownloadBytes)
                    if (playback.pendingIsContinuation) {
                        stringResource(R.string.playback_continue_to, name, size)
                    } else {
                        stringResource(R.string.playback_offer, name, size)
                    }
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
    }
}

/** How anything playing or pending shows at the foot of the page. */
internal fun PlaybackUiState.isAnything(): Boolean =
    ayahNumber != null || unavailable || pendingDownloadSurah != null || downloadFailed

@Composable
private fun surahName(viewModel: ReaderViewModel, ayah: Int): String =
    viewModel.surahOf(ayah)?.nameSimple ?: stringResource(R.string.reader_fallback_title)

@Composable
private fun AyahActions(
    isSaved: Boolean,
    hasNote: Boolean,
    fromMushaf: Boolean,
    onSave: () -> Unit,
    onPlay: () -> Unit,
    onNote: () -> Unit,
    onMore: () -> Unit,
    onShare: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .shadow(elevation = 6.dp, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextAction(stringResource(R.string.action_play), Icon.Play, onPlay)
        TextAction(
            label = stringResource(R.string.action_note),
            icon = Icon.Note,
            onClick = onNote,
            active = hasNote,
        )
        TextAction(
            label = stringResource(R.string.action_save),
            icon = if (isSaved) Icon.BookmarkFilled else Icon.Bookmark,
            onClick = onSave,
            active = isSaved,
        )
        TextAction(stringResource(R.string.action_share), Icon.Share, onShare)
        TextAction(
            label = stringResource(
                if (fromMushaf) R.string.action_more else R.string.action_tafsir,
            ),
            icon = if (fromMushaf) Icon.More else Icon.Tafsir,
            onClick = onMore,
        )
    }
}

