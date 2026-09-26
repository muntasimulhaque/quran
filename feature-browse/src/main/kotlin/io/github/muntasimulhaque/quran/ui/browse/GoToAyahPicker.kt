package io.github.muntasimulhaque.quran.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.browse.R
import io.github.muntasimulhaque.quran.ui.kit.SheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.sheetDragGate
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The picker: a surah's ayah numbers, with the surah itself as the first
 * choice. The reader's own surah is already chosen, so moving within a long
 * surah is one tap on a number; another surah is one more tap through the
 * same list Browse draws. The grid starts on the reader's own ayah, marked,
 * so the place they are leaving is visible before they leave it.
 *
 * The 2.3 pass made both halves say what they are standing for. The card above
 * the grid used to carry the surah's Latin name alone, which left two things a
 * reader is asked to know before tapping a number: which surah this is in its
 * own script, and how long it is, since length is what tells them how far the
 * grid runs and whether the ayah they have in mind is even in it. The surah
 * step used to open at Al-Fātiḥah, which hid the one row that answers "where
 * am I"; it now opens on the reader's own surah and wears the same filled mark
 * the grid wears for their own ayah, so one shape means "the place you are
 * standing" on both steps (owner report, 2.3).
 */
@Composable
internal fun GoToAyahPicker(
    surahs: List<Surah>,
    surahNumberWidth: Dp,
    starts: Map<Int, Int>,
    chosenSurah: Int,
    currentAyah: Int,
    choosingSurah: Boolean,
    onChoose: () -> Unit,
    onSurah: (Int) -> Unit,
    onBack: () -> Unit,
    onAyah: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chosen = surahs.firstOrNull { it.number == chosenSurah } ?: surahs.firstOrNull() ?: return
    val gridState = rememberLazyGridState()
    val gridGate = remember(gridState) { SheetDragGate(gridState) }
    val surahList = rememberLazyListState()
    val surahGate = remember(surahList) { SheetDragGate(surahList) }
    val firstAyah = starts[chosen.number] ?: 1
    val currentInChosen = currentAyah - firstAyah + 1
    val currentHere = currentInChosen in 1..chosen.versesCount
    // The grid opens on the reader's own ayah, and the ayah is centered in
    // the viewport rather than pinned to its top: a number that answers "go
    // to" has to be unmistakably the one the reader is standing on, and on a
    // tall grid a top-aligned row reads as any other row (owner report,
    // D-097). The place is a second key beside the surah, so a jump that
    // changes the reader's ayah while this picker is composed can never
    // leave the grid on a place that is no longer theirs. A grid for a
    // surah the reader is not in has no place to show and starts at its
    // first ayah, which is what choosing another surah means.
    LaunchedEffect(chosen.number, currentInChosen) {
        val index = if (currentHere) currentInChosen - 1 else 0
        gridState.scrollToItem(index)
        if (!currentHere) return@LaunchedEffect
        val viewport = gridState.layoutInfo.viewportSize.height
        val cell = gridState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }?.size?.height ?: 0
        if (viewport > cell && cell > 0) {
            gridState.scrollToItem(index, scrollOffset = -((viewport - cell) / 2))
        }
    }
    // The list of surahs opens where the reader is standing. The step exists
    // to leave this surah, and a list that always begins at row one spends the
    // reader's first look on rows they did not ask about while the one row
    // that says "here" is somewhere below. One line at the top of the
    // viewport keeps both: the reader's own surah first, and everything above
    // it one scroll away.
    LaunchedEffect(choosingSurah, chosen.number) {
        if (!choosingSurah) return@LaunchedEffect
        val index = surahs.indexOfFirst { it.number == chosen.number }
        if (index > 0) surahList.scrollToItem(index)
    }

    Column(modifier.fillMaxWidth()) {
        if (choosingSurah) {
            // The one step with a head: the step with a way back. Its title
            // is the choice it asks for, never the tab's own name again.
            PickerHeader(
                title = stringResource(R.string.browse_choose_surah),
                onBack = onBack,
            )
            LazyColumn(
                state = surahList,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .sheetDragGate(surahGate)
                    .testTag("go-to-surahs"),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                items(surahs, key = { it.number }) { surah ->
                    SurahRow(
                        surah = surah,
                        numberWidth = surahNumberWidth,
                        current = surah.number == chosen.number,
                    ) { onSurah(surah.number) }
                }
            }
        } else {
            SurahCard(surah = chosen, onClick = onChoose)
            // The gap between the card and the numbers lives outside the
            // grid, not in its content padding: padding scrolls away with
            // the first row, and the reader met the card and a clipped pill
            // touching once the grid moved under them (owner report, D-090
            // closed the resting frame; this closes the scrolled one).
            Spacer(Modifier.height(Space.Block))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 54.dp),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .sheetDragGate(gridGate)
                    .testTag("go-to-ayahs"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(count = chosen.versesCount, key = { it + 1 }) { index ->
                    val number = index + 1
                    AyahNumberCell(
                        number = number,
                        current = currentHere && number == currentInChosen,
                        onClick = { onAyah(firstAyah + index) },
                    )
                }
            }
        }
    }
}

/**
 * The head of the picker: one way back, one name for the step. The title and
 * the surah card below it share one gutter with the grid, so the picker reads
 * as one page instead of a heading with a list under it (D-090).
 */
@Composable
private fun PickerHeader(title: String, onBack: () -> Unit) {
    val back = stringResource(R.string.browse_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 16.dp, top = 2.dp, bottom = Space.Line),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack)
                .semantics {
                    contentDescription = back
                    role = Role.Button
                }
                .testTag("go-to-back"),
            contentAlignment = Alignment.Center,
        ) {
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(22.dp)
                    // The chevron opens downward, so going back is the same
                    // mark turned to point the way it came.
                    .rotate(90f),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/**
 * The surah the grid belongs to, and the door that changes it.
 *
 * It is a card of its own quiet fill rather than a bare row: the name a reader
 * is about to leave has to read as a thing they hold, not as a label over a
 * list. It carries the surah's own Arabic name and its length beside the Latin
 * one, in the same two lines the Browse list gives every surah, because the
 * reader is being asked to know a surah before they tap through it, and a bare
 * name made them scroll the grid to find out how big it was (owner report,
 * 2.3).
 */
@Composable
private fun SurahCard(surah: Surah, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable(role = Role.Button, onClick = onClick)
            // The test tag and the click sit on the row itself, so the whole
            // card is one button: the name, the length, the Arabic, and the
            // mark all open the same list.
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
            .testTag("go-to-surah"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = surah.nameSimple,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
            style = TextStyle(
                fontFamily = Amiri,
                fontSize = 23.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.padding(start = 12.dp),
        )
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(20.dp)
                // A tap opens the surah list in place, so the mark points the
                // way the page turns.
                .rotate(-90f),
        )
    }
}

/**
 * One ayah's number in the picker's grid: a shape the finger can see, the
 * reader's own ayah filled, and both states spoken so the grid means the same
 * thing to TalkBack.
 *
 * The cell is 48 dp tall, the design document's own floor for a thing a finger
 * aims at, and the grid's columns are as wide as that: a picker of small
 * numbers is exactly the surface where a target under the floor is felt, since
 * the reader aims at a two- or three-digit shape and not at a word.
 */
@Composable
private fun AyahNumberCell(number: Int, current: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.last_read_ayah_label, number)
    val currentState = stringResource(R.string.browse_ayah_current)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                },
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                if (current) stateDescription = currentState
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = numberLabel(number),
            style = MaterialTheme.typography.labelLarge,
            color = if (current) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/** The first ayah of every surah, from the Quran's sequential numbering. */
internal fun surahStartMap(surahs: List<Surah>): Map<Int, Int> {
    var next = 1
    val starts = HashMap<Int, Int>(surahs.size)
    for (surah in surahs.sortedBy { it.number }) {
        starts[surah.number] = next
        next += surah.versesCount
    }
    return starts
}
