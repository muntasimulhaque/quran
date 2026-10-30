package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.core.SearchQuery
import io.github.muntasimulhaque.quran.data.SearchResults
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.theme.Space

@Composable
internal fun StatusLine(
    query: SearchQuery?,
    results: SearchResults,
    searching: Boolean,
    hasSources: Boolean,
    failed: Boolean,
) {
    val total = results.counts.total
    // Nothing is said while the field is empty. The line carried a prompt
    // that began with the field's own words ("Search the Quran, or type
    // 2:255 to go there"), so the same sentence stood twice on one sheet
    // (owner report). The status line is the results' line now, and an
    // empty query draws nothing at all: the sheet is the field, the filter
    // door, and the results, and the first match arrives in a place no row
    // has to move out of.
    val message = when {
        !hasSources -> stringResource(R.string.search_status_no_sources)
        failed -> stringResource(R.string.search_status_failed)
        query == null -> null
        searching && results.hits.isEmpty() -> stringResource(R.string.search_status_searching)
        total == 0 -> stringResource(R.string.search_status_no_matches)
        // A capped list has no honest count: the number the pass collected is
        // not the number that exists, and a reader who acts on it is acting
        // on a floor. The line says what the reader is looking at instead of
        // how much there is (owner decision, this session).
        results.capped -> stringResource(R.string.search_status_capped, LIMIT)
        else -> pluralStringResource(R.plurals.search_matches, total, total)
    } ?: return
    Text(
        text = message,
        // A status line is a caption, not prose: the meta line's 13 reads it
        // under the field without competing with the results (owner decision,
        // this session).
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = Space.Gutter,
            end = Space.Gutter,
            top = Space.Tight,
            bottom = Space.Row,
        ),
    )
}
