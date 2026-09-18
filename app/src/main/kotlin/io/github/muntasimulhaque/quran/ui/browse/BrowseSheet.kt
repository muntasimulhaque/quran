package io.github.muntasimulhaque.quran.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AyahLocation
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.ui.theme.LatinReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class BrowseTab { Surahs, Saved }

/**
 * The browse sheet: the 114 surahs and everything the reader has saved, under
 * one roof. Saved ayahs keep their notes here, and a tap opens the study card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSheet(
    content: ContentDatabase,
    surahs: List<Surah>,
    saved: List<SavedAyah>,
    onDismiss: () -> Unit,
    onSurah: (Int) -> Unit,
    onSaved: (AyahLocation) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var tab by remember { mutableStateOf(BrowseTab.Surahs) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) { sheetState.expand() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxHeight(0.94f)) {
            Text(
                text = "Browse",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
            )
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SheetTab("Surahs", selected = tab == BrowseTab.Surahs) { tab = BrowseTab.Surahs }
                SheetTab(
                    label = if (saved.isEmpty()) "Saved" else "Saved  ${saved.size}",
                    selected = tab == BrowseTab.Saved,
                ) { tab = BrowseTab.Saved }
            }
            when (tab) {
                BrowseTab.Surahs -> SurahList(surahs, onSurah)
                BrowseTab.Saved -> SavedList(content, saved, onSaved, onRemove)
            }
        }
    }
}

@Composable
private fun SheetTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun SurahList(surahs: List<Surah>, onSurah: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxHeight()) {
        items(surahs, key = { it.number }) { surah ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSurah(surah.number) }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = surah.number.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(34.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = surah.nameSimple,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${surah.nameArabic}  ·  ${surah.revelationPlace}  ·  ${surah.versesCount} ayahs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

private data class SavedRow(val saved: SavedAyah, val location: AyahLocation?)

@Composable
private fun SavedList(
    content: ContentDatabase,
    saved: List<SavedAyah>,
    onSaved: (AyahLocation) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val context = LocalContext.current
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    if (saved.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Nothing saved yet.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Open any ayah and tap Save.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }

    val rows by produceState(initialValue = emptyList<SavedRow>(), saved) {
        value = withContext(Dispatchers.IO) {
            val byNumber = content.ayahsWithPages(saved.map { it.ayahNumber })
                .associateBy { it.ayah.number }
            saved.map { SavedRow(it, byNumber[it.ayahNumber]) }
        }
    }

    LazyColumn(Modifier.fillMaxHeight()) {
        items(rows, key = { it.saved.ayahNumber }) { row ->
            SavedAyahRow(
                row = row,
                hafs = hafs,
                onOpen = { row.location?.let(onSaved) },
                onRemove = { onRemove(row.saved.ayahNumber) },
            )
        }
    }
}

@Composable
private fun SavedAyahRow(
    row: SavedRow,
    hafs: FontFamily,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.location?.ayah?.verseKey.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Remove",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        row.location?.let {
            Text(
                text = it.ayah.text,
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = 20.sp,
                    lineHeight = 38.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                textAlign = TextAlign.Right,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
        row.saved.note?.let { note ->
            Text(
                text = note,
                style = LatinReading.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
    }
}
