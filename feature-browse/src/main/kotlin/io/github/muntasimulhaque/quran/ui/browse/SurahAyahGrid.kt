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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
 * The ayahs of one surah, opened from its row in Browse: the surah's own
 * numbers, with the reader's place marked and in view. This is the whole of
 * "go to ayah" now, and the surah list is its first step, so the sheet holds
 * one list of surahs instead of two (owner decision, D-101).
 *
 * The grid opens on the place the reader is being shown, centered in the
 * viewport rather than pinned to its top: a number that answers "go to" has
 * to be unmistakably the one the reader is standing on, and on a tall grid a
 * top-aligned row reads as any other row (owner report, D-097). The place is
 * a key beside the surah, so a jump that changes the reader's ayah while this
 * page is composed can never leave the grid on a place that is no longer
 * theirs. A surah with no place of its own starts at its first ayah, which is
 * what opening an unread surah means.
 */
@Composable
internal fun SurahAyahGrid(
    surah: Surah,
    starts: Map<Int, Int>,
    markedAyah: Int?,
    onBack: () -> Unit,
    onAyah: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val gridGate = remember(gridState) { SheetDragGate(gridState) }
    val firstAyah = starts[surah.number] ?: 1
    val placeInSurah = markedAyah
        ?.minus(firstAyah)
        ?.plus(1)
        ?.takeIf { it in 1..surah.versesCount }
    LaunchedEffect(surah.number, placeInSurah) {
        val index = placeInSurah?.minus(1) ?: 0
        gridState.scrollToItem(index)
        if (placeInSurah == null) return@LaunchedEffect
        val viewport = gridState.layoutInfo.viewportSize.height
        val cell = gridState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }?.size?.height ?: 0
        if (viewport > cell && cell > 0) {
            gridState.scrollToItem(index, scrollOffset = -((viewport - cell) / 2))
        }
    }

    Column(modifier.fillMaxWidth()) {
        SurahGridHeader(surah = surah, onBack = onBack)
        // The gap between the header and the numbers lives outside the grid,
        // not in its content padding: padding scrolls away with the first
        // row, and the reader met the header and a clipped pill touching once
        // the grid moved under them (owner report, D-090 closed the resting
        // frame; this closes the scrolled one).
        Spacer(Modifier.height(Space.Block))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 54.dp),
            state = gridState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .sheetDragGate(gridGate)
                .testTag("surah-ayahs"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(count = surah.versesCount, key = { it + 1 }) { index ->
                val number = index + 1
                AyahNumberCell(
                    number = number,
                    current = placeInSurah != null && number == placeInSurah,
                    onClick = { onAyah(firstAyah + index) },
                )
            }
        }
    }
}

/**
 * The head of the grid: one way back, and the surah the numbers belong to.
 * The name, the place it was revealed, its length, and its own Arabic name
 * are all on the row, because the reader is being asked to know which surah
 * they are about to jump through, and its length is what tells them how far
 * the grid runs (owner report, 2.3).
 */
@Composable
private fun SurahGridHeader(surah: Surah, onBack: () -> Unit) {
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
                .testTag("surah-ayah-back"),
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
        Column(
            Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                text = surah.nameSimple,
                style = MaterialTheme.typography.titleLarge,
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
    }
}

/**
 * One ayah's number in the grid: a shape the finger can see, the reader's own
 * ayah filled, and both states spoken so the grid means the same thing to
 * TalkBack.
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
