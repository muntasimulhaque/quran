package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.muntasimulhaque.quran.core.SearchQuery
import io.github.muntasimulhaque.quran.data.SearchResults
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The results' own line, and the two states that need more than a line.
 *
 * **A failed search offers its own retry** (owner decision, this session).
 * The line used to end in "Try again." with nothing to tap: the only way to
 * run the query again was to close the sheet or to edit a query the reader
 * had already written. A quiet Retry sits under the sentence now, so the
 * instruction and the control are the same act.
 *
 * **A search with no matches teaches the forms it accepts** (owner decision,
 * this session). "No matches." left most of the sheet blank at exactly the
 * moment the reader needed to know that a shorter word or a reference is a
 * query. The hint under the count is the empty sheet's teaching, at the one
 * moment it is an answer rather than a lesson beside the field.
 *
 * Nothing is said while the field is empty. The line carried a prompt that
 * began with the field's own words ("Search the Quran, or type 2:255 to go
 * there"), so the same sentence stood twice on one sheet (owner report). The
 * status line is the results' line, and an empty query draws nothing at all:
 * the sheet is the field, the filter door, and the results, and the first
 * match arrives in a place no row has to move out of.
 */
@Composable
internal fun StatusLine(
    query: SearchQuery?,
    results: SearchResults,
    searching: Boolean,
    hasSources: Boolean,
    failed: Boolean,
    /** True when these results belong to the query and filters on screen. */
    current: Boolean,
    /** Runs the same query again after a failure. */
    onRetry: () -> Unit,
) {
    val total = results.counts.total
    val message = when {
        !hasSources -> stringResource(R.string.search_status_no_sources)
        failed -> stringResource(R.string.search_status_failed)
        query == null -> null
        // A new query starts before its results exist; the previous results
        // are never shown under it. The line says the search is running
        // instead of naming a count that belongs to another question (owner
        // decision, this session).
        !current -> stringResource(R.string.search_status_searching)
        searching && results.hits.isEmpty() -> stringResource(R.string.search_status_searching)
        total == 0 -> stringResource(R.string.search_status_no_matches)
        // A capped list has no honest count: the number the pass collected is
        // not the number that exists, and a reader who acts on it is acting
        // on a floor. The line says what the reader is looking at instead of
        // how much there is (owner decision, this session).
        results.capped -> stringResource(R.string.search_status_capped, LIMIT)
        else -> pluralStringResource(R.plurals.search_matches, total, total)
    } ?: return
    // The settled, empty answer, and the only state that adds the hint.
    val noMatches = hasSources &&
        !failed &&
        query != null &&
        current &&
        !searching &&
        total == 0
    Column(
        modifier = Modifier.padding(
            start = Space.Gutter,
            end = Space.Gutter,
            top = Space.Tight,
            bottom = Space.Row,
        ),
    ) {
        Text(
            text = message,
            // A status line is a caption, not prose: the meta line's 13 reads it
            // under the field without competing with the results (owner decision,
            // this session).
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // A result count, a settled search, or a failure arrives while the
            // reader's focus is still in the field. The line announces itself
            // once each time it changes, and a failure interrupts because the
            // reader has to act on it (owner decision, this session).
            modifier = Modifier.semantics {
                liveRegion = if (failed) LiveRegionMode.Assertive else LiveRegionMode.Polite
            },
        )
        if (noMatches) {
            Text(
                text = localizedDigits(stringResource(R.string.search_status_no_match_hint)),
                // The hint is the one line on the results' page that teaches,
                // so it takes the body's 15 where the count above it is a
                // caption (owner decision, this session).
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = Space.Line)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (failed) {
            TextButton(
                label = stringResource(R.string.search_status_retry),
                onClick = onRetry,
                modifier = Modifier.padding(top = Space.Line),
            )
        }
    }
}
