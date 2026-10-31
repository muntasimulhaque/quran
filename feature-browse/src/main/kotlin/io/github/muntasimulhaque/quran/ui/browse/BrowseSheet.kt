package io.github.muntasimulhaque.quran.ui.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.JuzStart
import io.github.muntasimulhaque.quran.data.ReadPlace
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.browse.R
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.SheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.sheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * The browse sheet: the surahs, the thirty juz, where the reader has been
 * reading, and everything they kept. Each list is one line per row and opens
 * the reader exactly where it says. A surah is a door to its own ayahs: the
 * list stays one line per surah, and a tap opens the surah's numbers with the
 * reader's own place marked and in view, so a jump inside a long surah is a
 * tap on a number instead of a scroll, and the surah list is the only list of
 * surahs the sheet needs (owner decision).
 *
 * Four tabs, and every tab was asked for: Surahs and Juz are the Book's own
 * divisions, Saved is the reader's own work, and Last Read is the way back to
 * a place they left. A note is written on a kept ayah, so it lives in Saved
 * with the note previewed under its place, and there is no second list for
 * the same work (owner decision).
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
) {
    val sheetState = rememberExpandedSheetState()
    var tab by rememberSaveable {
        mutableStateOf(if (startOnLastRead) BrowseTab.LastRead else BrowseTab.Surahs)
    }
    // The surah whose ayahs are on screen, or null while the tabs are. The
    // grid is a page inside the sheet, not a second sheet over it: one
    // window, one scrim, and the phone's back returns to the list it came
    // from before it leaves the sheet (owner decision).
    var gridSurah by rememberSaveable { mutableStateOf<Int?>(null) }
    // Each tab keeps its own list state so its place is remembered while the
    // reader moves between tabs, and each list carries a gate so a scroll
    // back to the top never turns into a pull that closes the sheet.
    val surahsList = rememberLazyListState()
    val juzList = rememberLazyListState()
    val lastReadList = rememberLazyListState()
    val savedList = rememberLazyListState()
    val surahsGate = remember(surahsList) { SheetDragGate(surahsList) }
    val juzGate = remember(juzList) { SheetDragGate(juzList) }
    val lastReadGate = remember(lastReadList) { SheetDragGate(lastReadList) }
    val savedGate = remember(savedList) { SheetDragGate(savedList) }
    // The numbering is the Quran's own, one past every ayah before, which is
    // where a surah starts and how a grid number becomes a reference.
    val surahStarts = remember(surahs) { surahStartMap(surahs) }
    val currentSurah = remember(surahs, surahStarts, currentAyah) {
        surahOfAyah(surahs, surahStarts, currentAyah) ?: surahs.firstOrNull()?.number ?: 1
    }
    // The centering is the sheet's answer to "where am I", and it is given
    // again on every raising, because Browse's place is the reading's place:
    // a reader who opened Al-Kahf from the list and read on to An-Nas must
    // find An-Nas in the middle when the sheet comes back, not Al-Kahf where
    // their thumb left the glass. The sheet used to restore the list's own
    // place instead, which was the same answer whenever nothing had moved and
    // the wrong one the moment the reading had (owner decision, this session).
    //
    // The two flags are plain `remember`, so they last no longer than one
    // raising: inside a raising the reader's own scroll and their tab
    // switches are left alone, and a new raising centers again. The tab and
    // the grid are still saved, because a tab is context and a grid is a
    // level, not a place in the Book.
    var surahsCentered by remember { mutableStateOf(false) }
    var juzCentered by remember { mutableStateOf(false) }
    // The reader's own surah is put in the middle of the Surahs list, the
    // way the grid puts their ayah in the middle of the numbers: Browse is
    // a door to the reader's place, and Al-Fatiha is a long walk from
    // Maryam. The ends of the Book clamp the scroll, so the first surah
    // stays at the top and the last at the bottom without a special case
    // (owner decision).
    LaunchedEffect(tab, surahs, currentSurah, gridSurah) {
        // The grid covers the list when a saved sheet comes back into it,
        // and a list that is not on the glass has no viewport to be centered
        // in: centering it there would leave it at the top with the flag
        // already spent. Closing the grid runs this again, and only then it
        // centers.
        if (tab != BrowseTab.Surahs || surahsCentered || gridSurah != null) {
            return@LaunchedEffect
        }
        val index = surahs.indexOfFirst { it.number == currentSurah }
        if (index < 0) return@LaunchedEffect
        centerListItem(surahsList, index)
        surahsCentered = true
    }
    // The place the grid marks when it opens: the reader's own ayah when the
    // surah they opened is the one they are in, and otherwise the newest
    // place they left in that surah. The current ayah is one place and the
    // history is around it, so a surah the reader is not in still has a
    // place of its own (owner report).
    fun placeIn(surahNumber: Int): Int? {
        if (currentSurah == surahNumber) return currentAyah
        return lastRead.firstOrNull {
            surahOfAyah(surahs, surahStarts, it.ayahNumber) == surahNumber
        }?.ayahNumber
    }
    // Both number columns are measured once here and handed to their rows: every
    // number of a list ends at one edge, and every name starts at one place.
    // The widest number each list can draw is found by measuring the list
    // itself, so no three-digit surah loses its last digit to a column sized
    // on a narrower stand-in.
    val juzStarts by produceState(initialValue = emptyList<JuzStart>(), content) {
        value = withContext(Dispatchers.IO) { content.juzStarts() }
    }
    // The reader's juz, the way currentSurah is the reader's surah: the last
    // juz that begins at or before the ayah they are standing on. It is put
    // in the middle of the Juz list by the same rule as the surah, so the
    // two divisions of the Book open the same way (owner decision).
    val currentJuz = remember(juzStarts, currentAyah) {
        juzStarts.lastOrNull { it.ayah <= currentAyah }?.juz
    }
    LaunchedEffect(tab, juzStarts, currentJuz, gridSurah) {
        if (tab != BrowseTab.Juz || juzCentered || currentJuz == null ||
            gridSurah != null
        ) {
            return@LaunchedEffect
        }
        val index = juzStarts.indexOfFirst { it.juz == currentJuz }
        if (index < 0) return@LaunchedEffect
        centerListItem(juzList, index)
        juzCentered = true
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
            val ayahs = content.ayahsByNumbers(numbers).associateBy { it.number }
            val surahs = content.surahs().associateBy { it.number }
            numbers.associateWith { number ->
                val ayah = ayahs[number]
                AyahText(
                    // The reader's own way of naming the place: the surah as
                    // they know it, and the ayah's own number under it.
                    surahName = ayah?.let { surahs[it.surah]?.nameSimple },
                    ayahLabel = ayah?.let { String.format(locale, ayahLabel, it.ayah) },
                )
            }
        }
    }

    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
    ) {
        // The grid is one level inside the sheet, so the phone's back returns
        // to the surah list before it leaves the sheet: the same two-level
        // behavior a settings page has, one level down.
        BackHandler(enabled = gridSurah != null) { gridSurah = null }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                // The tag is the tour's anchor: the reader's own title sits
                // behind this sheet and would answer a text wait at once.
                .testTag("browse-sheet"),
        ) {
            val openSurah = gridSurah?.let { number ->
                surahs.firstOrNull { it.number == number }
            }
            if (openSurah != null) {
                SurahAyahGrid(
                    surah = openSurah,
                    starts = surahStarts,
                    markedAyah = placeIn(openSurah.number),
                    onBack = { gridSurah = null },
                    onAyah = { number ->
                        // The first ayah of a surah the reader never read
                        // opens the surah itself, so the reading still lands
                        // on its opening rather than mid-list at ayah 1; any
                        // other number is the jump the reader asked for.
                        val first = surahStarts[openSurah.number] ?: 1
                        if (number == first && placeIn(openSurah.number) == null) {
                            onSurah(openSurah.number)
                        } else {
                            onAyah(number)
                        }
                    },
                )
            } else {
                // The sheet needs no title: the tabs name everything it holds,
                // and a heading over a control that already says where the
                // reader is spends the first line of the sheet on nothing.
                // The tabs wrap rather than scroll, the way the search filters
                // do: the labels are read in two languages and follow the
                // system font scale, and a tab that runs off the edge is a
                // list the reader cannot open. Wrapped, every tab stays
                // visible and whole. Each tab is its own rounded chip, the
                // shape search already taught the reader, rather than one
                // strip that would have to break its own outline to wrap.
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Block),
                    // The tabs are centered where the search filters start at
                    // the left: tabs are the sheet's own navigation, and
                    // filters apply to a list below them (owner decision,
                    // the theme and consistency session).
                    horizontalArrangement = Arrangement.spacedBy(Space.Line, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    BrowseTab.entries.forEach { entry ->
                        BrowseTabChip(
                            label = stringResource(entry.labelRes),
                            active = entry == tab,
                            onClick = { tab = entry },
                        )
                    }
                }
                Spacer(Modifier.height(Space.Line))
                when (tab) {
                    BrowseTab.Surahs -> LazyColumn(
                        state = surahsList,
                        modifier = Modifier
                            .sheetDragGate(surahsGate)
                            .testTag("browse-surahs"),
                        contentPadding = PaddingValues(bottom = Space.SheetFoot),
                    ) {
                        items(surahs, key = { it.number }) { surah ->
                            SurahRow(
                                surah = surah,
                                numberWidth = surahNumberWidth,
                                current = surah.number == currentSurah,
                            ) { gridSurah = surah.number }
                        }
                    }
                    BrowseTab.Juz -> LazyColumn(
                        state = juzList,
                        modifier = Modifier
                            .sheetDragGate(juzGate)
                            .testTag("browse-juz"),
                        contentPadding = PaddingValues(bottom = Space.SheetFoot),
                    ) {
                        itemsIndexedCompat(juzStarts) { index, start ->
                            val surah = surahs.firstOrNull { it.number == start.surah }
                            JuzRow(
                                juz = start.juz,
                                numberWidth = juzNumberWidth,
                                surahName = surah?.nameSimple,
                                surahAyah = start.surahAyah,
                                current = start.juz == currentJuz,
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
                }
            }
        }
    }
}
