package io.github.muntasimulhaque.quran.ui.browse

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import io.github.muntasimulhaque.quran.data.Surah


/** The surah one ayah falls in, from the Quran's sequential numbering. */
internal fun surahOfAyah(surahs: List<Surah>, starts: Map<Int, Int>, ayah: Int): Int? =
    surahs.lastOrNull { (starts[it.number] ?: Int.MAX_VALUE) <= ayah }?.number

/**
 * Puts one row in the middle of its list, as far as the Book's ends allow.
 * The row is scrolled to the top first so its height can be measured, then
 * scrolled again by the offset that centers it: a row's own height is not
 * known before it is measured, and the reader's place is the one row that
 * has to be found without a hunt. It is the rule the ayah grid already
 * follows, drawn on a list instead of a grid.
 */
internal suspend fun centerListItem(state: LazyListState, index: Int) {
    state.scrollToItem(index)
    val viewport = state.layoutInfo.viewportSize.height
    val row = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size ?: 0
    if (viewport > row && row > 0) {
        state.scrollToItem(index, scrollOffset = -((viewport - row) / 2))
    }
}

// A tiny helper so the juz list can use its index as the juz number.
internal fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexedCompat(
    items: List<T>,
    row: @Composable (Int, T) -> Unit,
) {
    items(items.size) { index -> row(index, items[index]) }
}
