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
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.ui.index.IndexSheet
import io.github.muntasimulhaque.quran.ui.mushaf.MushafPage
import io.github.muntasimulhaque.quran.ui.study.StudyPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme

@Composable
fun QuranApp(viewModel: ReaderViewModel = viewModel()) {
    QuranTheme {
        val content = viewModel.content
        if (!viewModel.ready || content == null) {
            // The cold start paints the paper instantly; the content opens behind it.
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        } else {
            ReaderScreen(viewModel, content)
        }
    }
}

@Composable
private fun ReaderScreen(viewModel: ReaderViewModel, content: ContentDatabase) {
    var indexOpen by remember { mutableStateOf(false) }
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
            ReadingMode.Study -> StudyPage(
                content = content,
                page = viewModel.page,
                modifier = Modifier.fillMaxSize(),
            )
        }
        ReaderTopBar(
            title = viewModel.position?.surah
                ?.let { number -> viewModel.surahs.firstOrNull { it.number == number }?.nameSimple }
                ?: "Quran",
            mode = viewModel.mode,
            onIndex = { indexOpen = true },
            onMode = {
                viewModel.switchMode(
                    if (viewModel.mode == ReadingMode.Mushaf) ReadingMode.Study else ReadingMode.Mushaf,
                )
            },
        )
        ReaderBottomPill(viewModel)
    }

    if (indexOpen) {
        IndexSheet(
            surahs = viewModel.surahs,
            onDismiss = { indexOpen = false },
            onSurah = { number ->
                viewModel.jumpToSurah(number)
                indexOpen = false
            },
        )
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    mode: ReadingMode,
    onIndex: () -> Unit,
    onMode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        BarAction("Index", onIndex)
        Spacer(Modifier.padding(horizontal = 6.dp))
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
