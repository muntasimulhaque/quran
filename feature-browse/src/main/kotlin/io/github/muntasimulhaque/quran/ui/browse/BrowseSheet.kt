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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.JuzStart
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.browse.R
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.LatinReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class BrowseTab(val labelRes: Int) {
    Surahs(R.string.browse_tab_surahs),
    Juz(R.string.browse_tab_juz),
    Saved(R.string.browse_tab_saved),
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
    val juzStarts by produceState(initialValue = emptyList<JuzStart>(), content) {
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
                text = stringResource(R.string.browse_title),
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
                            text = stringResource(entry.labelRes),
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
                    itemsIndexedCompat(juzStarts) { index, start ->
                        val surah = surahs.firstOrNull { it.number == start.surah }
                        JuzRow(
                            juz = start.juz,
                            reference = start.verseKey,
                            surahName = surah?.nameSimple ?: "",
                            onJuz = { onSurah(start.surah) },
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
                text = stringResource(
                    R.string.surah_meta_place_ayahs,
                    placeName(surah.revelationPlace),
                    surah.versesCount,
                ),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.juz_title, juz),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (reference.isEmpty()) "" else stringResource(R.string.juz_starts_at, surahName, reference),
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
                text = stringResource(R.string.saved_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.saved_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }
    val context = LocalContext.current
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    // Every saved ayah is read in two batched queries, once, so the list
    // never opens on a row that is still looking for its text.
    val texts by produceState<Map<Int, SavedText>>(emptyMap(), saved, translationPack) {
        value = withContext(Dispatchers.IO) {
            val numbers = saved.map { it.ayahNumber }
            val ayahs = content.ayahsWithPages(numbers).associateBy { it.ayah.number }
            val translations = content.translations(numbers, translationPack)
            numbers.associateWith { number ->
                SavedText(
                    reference = ayahs[number]?.ayah?.verseKey ?: "Ayah $number",
                    arabic = ayahs[number]?.ayah?.text.orEmpty(),
                    translation = translations[number]?.text?.let { RichText.plain(it) },
                )
            }
        }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
        items(saved, key = { it.ayahNumber }) { row ->
            SavedRow(
                saved = row,
                text = texts[row.ayahNumber],
                hafs = hafs,
                onAyah = onAyah,
                onRemove = onRemove,
            )
        }
    }
}

/** What one saved row shows, read with its neighbours. */
private data class SavedText(
    val reference: String,
    val arabic: String,
    val translation: String?,
)

@Composable
private fun SavedRow(
    saved: SavedAyah,
    text: SavedText?,
    hafs: FontFamily,
    onAyah: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAyah(saved.ayahNumber) }
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text?.reference ?: stringResource(R.string.saved_reference_fallback, saved.ayahNumber),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.action_open),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.action_remove),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onRemove(saved.ayahNumber) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        text?.arabic?.takeIf { it.isNotEmpty() }?.let { arabic ->
            Text(
                text = arabic,
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = 20.sp,
                    lineHeight = 38.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Right,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
        text?.translation?.let { translation ->
            Text(
                text = translation,
                style = LatinReading.copy(fontSize = 15.sp, lineHeight = 23.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
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

@Composable
private fun placeName(place: String): String =
    if (place.equals("makkah", true)) {
        stringResource(R.string.place_makkah)
    } else {
        stringResource(R.string.place_madinah)
    }
