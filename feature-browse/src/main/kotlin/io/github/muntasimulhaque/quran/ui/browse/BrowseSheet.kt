package io.github.muntasimulhaque.quran.ui.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.JuzStart
import io.github.muntasimulhaque.quran.data.ReadPlace
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.browse.R
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class BrowseTab(val labelRes: Int) {
    Surahs(R.string.browse_tab_surahs),
    Juz(R.string.browse_tab_juz),
    LastRead(R.string.browse_tab_last_read),
    Saved(R.string.browse_tab_saved),
}

/**
 * The browse sheet: the surahs, the thirty juz, where the reader has been
 * reading, and everything they saved. Each list is one line per row and opens
 * the reader exactly where it says, so a reader is never more than two taps
 * from any ayah in the Quran.
 *
 * Four tabs, one row of them, and every tab was asked for: Surahs and Juz are
 * the Book's own divisions, Saved is the reader's own work, and Last Read is
 * the way back to a place they left.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSheet(
    content: ContentDatabase,
    surahs: List<Surah>,
    saved: List<SavedAyah>,
    lastRead: List<ReadPlace>,
    translationPack: String,
    startOnLastRead: Boolean = false,
    onDismiss: () -> Unit,
    onAyah: (Int) -> Unit,
    onSurah: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onForget: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember {
        mutableStateOf(if (startOnLastRead) BrowseTab.LastRead else BrowseTab.Surahs)
    }
    val juzStarts by produceState(initialValue = emptyList<JuzStart>(), content) {
        value = withContext(Dispatchers.IO) { content.juzStarts() }
    }
    // Every list that needs the ayah's own text reads it in one pass: the
    // reader never sees a row that is still looking for what it says.
    val texts by produceState<Map<Int, AyahText>>(
        initialValue = emptyMap(),
        saved,
        lastRead,
        translationPack,
    ) {
        value = withContext(Dispatchers.IO) {
            val numbers = (saved.map { it.ayahNumber } + lastRead.map { it.ayahNumber }).distinct()
            if (numbers.isEmpty()) return@withContext emptyMap()
            val ayahs = content.ayahsWithPages(numbers).associateBy { it.ayah.number }
            val translations = content.translations(numbers, translationPack)
            numbers.associateWith { number ->
                AyahText(
                    reference = ayahs[number]?.ayah?.verseKey ?: "Ayah $number",
                    page = ayahs[number]?.page ?: 1,
                    arabic = ayahs[number]?.ayah?.text.orEmpty(),
                    translation = translations[number]?.text?.let { RichText.plain(it) },
                )
            }
        }
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                BrowseTab.LastRead -> LastReadList(
                    places = lastRead,
                    texts = texts,
                    onAyah = onAyah,
                    onForget = onForget,
                )
                BrowseTab.Saved -> SavedList(
                    saved = saved,
                    texts = texts,
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
