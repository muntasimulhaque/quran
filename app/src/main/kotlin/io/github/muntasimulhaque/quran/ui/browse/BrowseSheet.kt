package io.github.muntasimulhaque.quran.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.AyahHeader
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.ui.rich.HighlightedText
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.LatinReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class BrowseTab(val label: String) {
    Surahs("Surahs"),
    Juz("Juz"),
    Saved("Saved"),
}

/**
 * The browse sheet: the surahs, the thirty juz, and everything the reader
 * saved. Each list is one line per row and opens the reader exactly where it
 * says, so a reader is never more than two taps from any ayah in the Quran.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSheet(
    content: ContentDatabase,
    surahs: List<Surah>,
    saved: List<SavedAyah>,
    headers: List<AyahHeader>,
    translationPack: String,
    startOnSaved: Boolean = false,
    onDismiss: () -> Unit,
    onAyah: (Int) -> Unit,
    onSurah: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember {
        mutableStateOf(if (startOnSaved) BrowseTab.Saved else BrowseTab.Surahs)
    }
    val juzStarts by produceState(initialValue = emptyList<Int>(), content) {
        value = withContext(Dispatchers.IO) { content.juzStarts() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Text(
                text = "Browse",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BrowseTab.entries.forEach { entry ->
                    val active = entry == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (active) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                            )
                            .clickable { tab = entry }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                BrowseTab.Surahs -> LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
                    items(surahs, key = { it.number }) { surah ->
                        SurahRow(surah) { onSurah(surah.number) }
                    }
                }
                BrowseTab.Juz -> LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
                    itemsIndexedCompat(juzStarts) { index, firstAyah ->
                        val header = headers.firstOrNull { it.number == firstAyah }
                        val surah = header?.let { h -> surahs.firstOrNull { it.number == h.surah } }
                        JuzRow(
                            juz = index + 1,
                            reference = header?.verseKey ?: "",
                            surahName = surah?.nameSimple ?: "",
                            onJuz = { onSurah(header?.surah ?: 1) },
                        )
                    }
                }
                BrowseTab.Saved -> SavedList(
                    content = content,
                    saved = saved,
                    translationPack = translationPack,
                    onAyah = onAyah,
                    onRemove = onRemove,
                )
            }
        }
    }
}

// A tiny helper so the juz list can use its index as the juz number.
private fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexedCompat(
    items: List<T>,
    row: @Composable (Int, T) -> Unit,
) {
    items(items.size) { index -> row(index, items[index]) }
}

@Composable
private fun SurahRow(surah: Surah, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = surah.number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = surah.nameSimple,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${placeName(surah.revelationPlace)}  \u00B7  ${surah.versesCount} ayahs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = surah.nameArabic,
            style = TextStyle(fontFamily = Amiri, fontSize = 23.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
private fun JuzRow(juz: Int, reference: String, surahName: String, onJuz: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onJuz)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = juz.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = "Juz $juz",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (reference.isEmpty()) "" else "$surahName  \u00B7  starts at $reference",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SavedList(
    content: ContentDatabase,
    saved: List<SavedAyah>,
    translationPack: String,
    onAyah: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    if (saved.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Nothing saved yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Tap any ayah while reading, then Save. Your saved ayahs and notes " +
                    "appear here, newest first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
        items(saved, key = { it.ayahNumber }) { row ->
            SavedRow(
                content = content,
                saved = row,
                translationPack = translationPack,
                onAyah = onAyah,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun SavedRow(
    content: ContentDatabase,
    saved: SavedAyah,
    translationPack: String,
    onAyah: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val context = LocalContext.current
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    val row by produceState<Pair<String, String?>?>(initialValue = null, saved.ayahNumber, translationPack) {
        value = withContext(Dispatchers.IO) {
            val ayah = content.ayah(saved.ayahNumber) ?: return@withContext null
            val translation = content.translations(listOf(saved.ayahNumber), translationPack)[saved.ayahNumber]
            ayah.verseKey to translation?.text?.let { RichText.plain(it) }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAyah(saved.ayahNumber) }
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row?.first ?: "Ayah ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Open",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Remove",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(start = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onRemove(saved.ayahNumber) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        row?.second?.let { translation ->
            Text(
                text = translation,
                style = LatinReading.copy(fontSize = 15.sp, lineHeight = 23.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        } ?: Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        saved.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

private fun placeName(place: String): String =
    if (place.equals("makkah", true)) "Makkah" else "Madinah"
