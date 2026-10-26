package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    val message = when {
        !hasSources -> stringResource(R.string.search_status_no_sources)
        failed -> stringResource(R.string.search_status_failed)
        query == null -> stringResource(R.string.search_status_prompt)
        searching && results.hits.isEmpty() -> stringResource(R.string.search_status_searching)
        total == 0 -> stringResource(R.string.search_status_no_matches)
        // A capped list has no honest count: the number the pass collected is
        // not the number that exists, and a reader who acts on it is acting
        // on a floor. The line says what the reader is looking at instead of
        // how much there is (owner decision, this session).
        results.capped -> stringResource(R.string.search_status_capped, LIMIT)
        else -> pluralStringResource(R.plurals.search_matches, total, total)
    }
    Text(
        text = message,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = 6.dp, bottom = 10.dp),
    )
}
