package io.github.muntasimulhaque.quran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.ui.browse.BrowseSheet
import io.github.muntasimulhaque.quran.ui.mushaf.MushafPage
import io.github.muntasimulhaque.quran.ui.playback.DownloadedSurah
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.playback.RecitationSheet
import io.github.muntasimulhaque.quran.ui.playback.ReciterSummary
import io.github.muntasimulhaque.quran.ui.playback.formatBytes
import io.github.muntasimulhaque.quran.ui.playback.shortReciterName
import io.github.muntasimulhaque.quran.ui.search.SearchSheet
import io.github.muntasimulhaque.quran.ui.study.AyahSheet
import io.github.muntasimulhaque.quran.ui.study.StudyPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme

@Composable
fun QuranApp(
    viewModel: ReaderViewModel = viewModel(),
    onPlaybackPermission: () -> Unit = {},
) {
    QuranTheme {
        val content = viewModel.content
        if (!viewModel.ready || content == null) {
            // The cold start paints the paper instantly; the content opens behind it.
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        } else {
            ReaderScreen(viewModel, content, onPlaybackPermission)
        }
    }
}

@Composable
private fun ReaderScreen(
    viewModel: ReaderViewModel,
    content: ContentDatabase,
    onPlaybackPermission: () -> Unit,
) {
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    var browseOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var recitationsOpen by remember { mutableStateOf(false) }
    var selectedAyah by remember { mutableStateOf<Ayah?>(null) }
    val pagerState = rememberPagerState(
        initialPage = (viewModel.page - 1).coerceIn(0, 603),
        pageCount = { 604 },
    )

    // The pill and the top bar follow settled pages only, so a half-finished
    // swipe never rewrites the reader's position.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled ->
            viewModel.goToPage(settled + 1)
        }
    }

    // A jump from the index drives the pager; a pager change never drives itself.
    LaunchedEffect(viewModel.page) {
        val target = viewModel.page - 1
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (viewModel.mode) {
            ReadingMode.Mushaf -> HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                MushafPage(
                    content = content,
                    fonts = viewModel.fonts,
                    page = index + 1,
                )
            }
            ReadingMode.Study -> HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                StudyPage(
                    content = content,
                    page = index + 1,
                    playingAyah = playback.ayahNumber,
                    playingWord = playback.wordPosition,
                    onAyah = { selectedAyah = it },
                )
            }
        }
        ReaderTopBar(
            title = viewModel.position?.surah
                ?.let { number -> viewModel.surahs.firstOrNull { it.number == number }?.nameSimple }
                ?: "Quran",
            mode = viewModel.mode,
            onBrowse = { browseOpen = true },
            onSearch = { searchOpen = true },
            onMode = {
                viewModel.switchMode(
                    if (viewModel.mode == ReadingMode.Mushaf) ReadingMode.Study else ReadingMode.Mushaf,
                )
            },
        )
        if (playback.ayahNumber != null || playback.unavailable || playback.pendingDownloadSurah != null || playback.downloadFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 14.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                PlaybackBar(
                    state = playback,
                    reciterName = shortReciterName(
                        viewModel.recitation,
                        viewModel.recitations.firstOrNull { it.id == viewModel.recitation }?.name.orEmpty(),
                    ),
                    reference = playback.reference,
                    pendingLabel = playback.pendingDownloadSurah?.let { surah ->
                        val name = viewModel.surahs.firstOrNull { it.number == surah }?.nameSimple
                            ?: "Surah $surah"
                        "$name  ·  ${formatBytes(playback.pendingDownloadBytes)}"
                    },
                    onToggle = { viewModel.togglePlayback() },
                    onNext = { viewModel.nextAyah() },
                    onPrevious = { viewModel.previousAyah() },
                    onReciter = { recitationsOpen = true },
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
        } else {
            ReaderBottomPill(viewModel)
        }
    }

    if (browseOpen) {
        BrowseSheet(
            content = content,
            surahs = viewModel.surahs,
            saved = saved,
            onDismiss = { browseOpen = false },
            onSurah = { number ->
                viewModel.jumpToSurah(number)
                browseOpen = false
            },
            onSaved = { location ->
                browseOpen = false
                viewModel.goToPage(location.page)
                selectedAyah = location.ayah
            },
            onRemove = { viewModel.removeSaved(it) },
        )
    }

    if (searchOpen) {
        SearchSheet(
            content = content,
            onDismiss = { searchOpen = false },
            onAyah = { ayah, page ->
                searchOpen = false
                viewModel.goToPage(page)
                selectedAyah = ayah
            },
            onSurah = { surah ->
                searchOpen = false
                viewModel.jumpToSurah(surah.number)
            },
        )
    }

    if (recitationsOpen) {
        RecitationSheet(
            recitations = viewModel.recitations,
            selected = viewModel.recitation,
            summaries = {
                val totals = viewModel.downloadedTotals()
                viewModel.recitations.map { recitation ->
                    val (count, bytes) = totals[recitation.id] ?: (0 to 0L)
                    ReciterSummary(recitation, count, bytes)
                }
            },
            downloads = { recitation ->
                viewModel.downloadedSurahs(recitation).entries.sortedBy { it.key }.map { (surah, bytes) ->
                    val name = viewModel.surahs.firstOrNull { it.number == surah }?.nameSimple
                        ?: "Surah $surah"
                    DownloadedSurah(surah, name, bytes)
                }
            },
            onSelect = { id ->
                viewModel.selectRecitation(id)
                recitationsOpen = false
            },
            onRemove = { surah -> viewModel.removeDownloads(viewModel.recitation, surah) },
            onDismiss = { recitationsOpen = false },
        )
    }

    selectedAyah?.let { ayah ->
        val savedRow = saved.firstOrNull { it.ayahNumber == ayah.number }
        AyahSheet(
            content = content,
            ayah = ayah,
            surahName = viewModel.surahs.firstOrNull { it.number == ayah.surah }?.nameSimple
                ?: "Surah ${ayah.surah}",
            isSaved = savedRow != null,
            note = savedRow?.note,
            onToggleSave = { viewModel.toggleSaved(ayah) },
            onSaveNote = { note -> viewModel.setNote(ayah, note) },
            onPlay = {
                onPlaybackPermission()
                viewModel.playAyah(ayah.number)
                // The reader wants to follow the reciter, not stare at the card.
                selectedAyah = null
            },
            onDismiss = { selectedAyah = null },
        )
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    mode: ReadingMode,
    onBrowse: () -> Unit,
    onSearch: () -> Unit,
    onMode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        BarAction("Browse", onBrowse)
        Spacer(Modifier.padding(horizontal = 4.dp))
        BarAction("Search", onSearch)
        Spacer(Modifier.padding(horizontal = 4.dp))
        BarAction(if (mode == ReadingMode.Mushaf) "Study" else "Mushaf", onMode)
    }
}

@Composable
private fun BarAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun ReaderBottomPill(viewModel: ReaderViewModel) {
    val position = viewModel.position
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Page ${viewModel.page}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (position != null) {
                Text(
                    text = "  ·  Juz ${position.juz}  ·  Hizb ${position.hizb}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
