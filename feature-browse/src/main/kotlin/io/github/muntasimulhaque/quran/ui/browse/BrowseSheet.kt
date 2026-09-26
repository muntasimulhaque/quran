package io.github.muntasimulhaque.quran.ui.browse

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.JuzStart
import io.github.muntasimulhaque.quran.data.ReadPlace
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.browse.R
import io.github.muntasimulhaque.quran.ui.kit.SheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.sheetDragGate
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class BrowseTab(val labelRes: Int) {
    Surahs(R.string.browse_tab_surahs),
    Juz(R.string.browse_tab_juz),
    LastRead(R.string.browse_tab_last_read),
    Saved(R.string.browse_tab_saved),
    Notes(R.string.browse_tab_notes),
    GoToAyah(R.string.browse_go_to_ayah),
}

/**
 * The browse sheet: the surahs, the thirty juz, where the reader has been
 * reading, everything they saved, and every ayah they wrote a note on. Each
 * list is one line per row and opens the reader exactly where it says, and
 * the tab row carries a Go to ayah door for a place inside a surah that
 * would otherwise be a scroll away.
 *
 * Five tabs, and every tab was asked for: Surahs and Juz are the Book's own
 * divisions, Saved and Notes are the reader's own work, and Last Read is the
 * way back to a place they left. Saved is what the reader marked to keep;
 * Notes is only the ayahs they wrote something on, so the two lists answer
 * two different questions and a note can be found without reading every
 * saved row.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowseSheet(
    content: ContentDatabase,
    surahs: List<Surah>,
    saved: List<SavedAyah>,
    lastRead: List<ReadPlace>,
    currentAyah: Int,
    startOnLastRead: Boolean = false,
    onDismiss: () -> Unit,
    onAyah: (Int) -> Unit,
    onSurah: (Int) -> Unit,
    onRemoveSaved: (Int) -> Unit,
    onForget: (Int) -> Unit,
    onNote: (Int) -> Unit,
    onRemoveNote: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember {
        mutableStateOf(if (startOnLastRead) BrowseTab.LastRead else BrowseTab.Surahs)
    }
    // Each tab keeps its own list state so its place is remembered while the
    // reader moves between tabs, and each list carries a gate so a scroll
    // back to the top never turns into a pull that closes the sheet.
    val surahsList = rememberLazyListState()
    val juzList = rememberLazyListState()
    val lastReadList = rememberLazyListState()
    val savedList = rememberLazyListState()
    val notesList = rememberLazyListState()
    val surahsGate = remember(surahsList) { SheetDragGate(surahsList) }
    val juzGate = remember(juzList) { SheetDragGate(juzList) }
    val lastReadGate = remember(lastReadList) { SheetDragGate(lastReadList) }
    val savedGate = remember(savedList) { SheetDragGate(savedList) }
    val notesGate = remember(notesList) { SheetDragGate(notesList) }
    // The picker is a tab like the rest, not a page over them: the chips stay
    // on screen, the chosen chip is filled, and the reader leaves by tapping
    // another list exactly as they leave Surahs. Inside it are two steps, the
    // surah and its ayah numbers, with the reader's own surah already chosen
    // so a jump within it is one tap on a number. Choosing a surah is the one
    // step with a head, because it is the one step with somewhere to go back
    // to. The numbering is the Quran's own, one past every ayah before, which
    // is where a surah starts and how a grid number becomes a reference.
    var choosingSurah by remember { mutableStateOf(false) }
    var pickSurah by remember { mutableIntStateOf(1) }
    val surahStarts = remember(surahs) { surahStartMap(surahs) }
    val currentSurah = remember(surahs, surahStarts, currentAyah) {
        surahs.sortedBy { it.number }
            .lastOrNull { (surahStarts[it.number] ?: Int.MAX_VALUE) <= currentAyah }
            ?.number
            ?: surahs.firstOrNull()?.number
            ?: 1
    }
    // Both number columns are measured once here and handed to their rows: every
    // number of a list ends at one edge, and every name starts at one place.
    // The widest number each list can draw is found by measuring the list
    // itself, so no three-digit surah loses its last digit to a column sized
    // on a narrower stand-in.
    val juzStarts by produceState(initialValue = emptyList<JuzStart>(), content) {
        value = withContext(Dispatchers.IO) { content.juzStarts() }
    }
    val surahNumberWidth = rememberNumberWidth(surahs.map { it.number })
    val juzNumberWidth = rememberNumberWidth(juzStarts.map { it.juz })
    // Every list that needs the ayah's own name reads it in one pass: the
    // reader never sees a row that is still looking for what it says. The
    // ayah's own number inside the label follows the interface's digits.
    val locale = LocalConfiguration.current.locales[0]
    val ayahLabel = stringResource(R.string.last_read_ayah_label)
    val texts by produceState<Map<Int, AyahText>>(
        initialValue = emptyMap(),
        saved,
        lastRead,
    ) {
        value = withContext(Dispatchers.IO) {
            val numbers = (saved.map { it.ayahNumber } + lastRead.map { it.ayahNumber }).distinct()
            if (numbers.isEmpty()) return@withContext emptyMap()
            val ayahs = content.ayahsWithPages(numbers).associateBy { it.ayah.number }
            val surahs = content.surahs().associateBy { it.number }
            numbers.associateWith { number ->
                val ayah = ayahs[number]?.ayah
                AyahText(
                    // The reader's own way of naming the place: the surah as
                    // they know it, and the ayah's own number under it.
                    surahName = ayah?.let { surahs[it.surah]?.nameSimple },
                    ayahLabel = ayah?.let { String.format(locale, ayahLabel, it.ayah) },
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // Choosing a surah is a step inside the Go to Ayah tab, so the phone's
        // back returns to the numbers before it leaves the sheet: the same
        // two-level behavior a settings page has, one level down.
        BackHandler(enabled = tab == BrowseTab.GoToAyah && choosingSurah) {
            choosingSurah = false
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                // The tag is the tour's anchor: the reader's own title sits
                // behind this sheet and would answer a text wait at once.
                .testTag("browse-sheet"),
        ) {
            // The sheet needs no title: the tabs name everything it holds,
            // and a heading over a control that already says where the reader
            // is spends the first line of the sheet on nothing. The tabs wrap
            // rather than scroll, the way the search filters do: the labels
            // are read in two languages and follow the system font scale, and
            // a tab that runs off the edge is a list the reader cannot open.
            // Wrapped, every tab stays visible and whole. Each tab is its own
            // rounded chip, the shape search already taught the reader, rather
            // than one strip that would have to break its own outline to
            // wrap. Go to Ayah is one of the chips, not a door beside them:
            // its content is a list of ayahs, and the chip that opened it is
            // filled like the tab it is.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = Space.Block),
                horizontalArrangement = Arrangement.spacedBy(Space.Line, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Space.Line),
            ) {
                BrowseTab.entries.forEach { entry ->
                    BrowseTabChip(
                        label = stringResource(entry.labelRes),
                        active = entry == tab,
                        modifier = if (entry == BrowseTab.GoToAyah) {
                            Modifier.testTag("go-to-ayah")
                        } else {
                            Modifier
                        },
                        onClick = {
                            tab = entry
                            if (entry == BrowseTab.GoToAyah) {
                                pickSurah = currentSurah
                                choosingSurah = false
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                BrowseTab.Surahs -> LazyColumn(
                    state = surahsList,
                    modifier = Modifier
                        .sheetDragGate(surahsGate)
                        .testTag("browse-surahs"),
                    contentPadding = PaddingValues(bottom = 28.dp),
                ) {
                    items(surahs, key = { it.number }) { surah ->
                        SurahRow(surah, surahNumberWidth) { onSurah(surah.number) }
                    }
                }
                BrowseTab.Juz -> LazyColumn(
                    state = juzList,
                    modifier = Modifier.sheetDragGate(juzGate),
                    contentPadding = PaddingValues(bottom = 28.dp),
                ) {
                    itemsIndexedCompat(juzStarts) { index, start ->
                        val surah = surahs.firstOrNull { it.number == start.surah }
                        JuzRow(
                            juz = start.juz,
                            numberWidth = juzNumberWidth,
                            reference = start.verseKey,
                            surahName = surah?.nameSimple ?: "",
                            // A juz begins at one ayah; a tap opens exactly
                            // that ayah, which is what the row's own line says.
                            onJuz = { onAyah(start.ayah) },
                        )
                    }
                }
                BrowseTab.LastRead -> LastReadList(
                    places = lastRead,
                    texts = texts,
                    listState = lastReadList,
                    listModifier = Modifier.sheetDragGate(lastReadGate),
                    onAyah = onAyah,
                    onForget = onForget,
                )
                BrowseTab.Saved -> SavedList(
                    saved = saved,
                    texts = texts,
                    listState = savedList,
                    listModifier = Modifier.sheetDragGate(savedGate),
                    onAyah = onAyah,
                    onRemove = onRemoveSaved,
                )
                BrowseTab.Notes -> NotesList(
                    saved = saved,
                    texts = texts,
                    listState = notesList,
                    listModifier = Modifier.sheetDragGate(notesGate),
                    onNote = onNote,
                    onRemove = onRemoveNote,
                )
                BrowseTab.GoToAyah -> GoToAyahPicker(
                    surahs = surahs,
                    surahNumberWidth = surahNumberWidth,
                    starts = surahStarts,
                    chosenSurah = pickSurah,
                    currentAyah = currentAyah,
                    choosingSurah = choosingSurah,
                    onChoose = { choosingSurah = true },
                    onSurah = { number ->
                        pickSurah = number
                        choosingSurah = false
                    },
                    onBack = { choosingSurah = false },
                    onAyah = onAyah,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * One tab as a chip: the reader taps a chip, so the tab row is the same kind
 * of control as the search filters, and one choice among five is read the same
 * way wherever the app asks for it.
 */
@Composable
private fun BrowseTabChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                },
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

// A tiny helper so the juz list can use its index as the juz number.
private fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexedCompat(
    items: List<T>,
    row: @Composable (Int, T) -> Unit,
) {
    items(items.size) { index -> row(index, items[index]) }
}
