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
import io.github.muntasimulhaque.quran.data.ReferenceNote
import io.github.muntasimulhaque.quran.data.SearchResults
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The results' own line, and the two states that need more than a line.
 *
 * **A failed search offers its own retry** (owner decision, 15.0).
 * The line used to end in "Try again." with nothing to tap: the only way to
 * run the query again was to close the sheet or to edit a query the reader
 * had already written. A quiet Retry sits under the sentence now, so the
 * instruction and the control are the same act.
 *
 * **A search with no matches teaches the forms it accepts** (owner decision,
 * 15.0). "No matches." left most of the sheet blank at exactly the
 * moment the reader needed to know that a shorter word or a reference is a
 * query. The hint under the count is the empty sheet's teaching, at the one
 * moment it is an answer rather than a lesson beside the field.
 *
 * **But a reader whose query was well formed is owed better than a lesson**
 * (owner decision, 22.3). A reference past its surah's last ayah is answered
 * with that surah's own count, because the Book already holds the answer and
 * the reader only mistyped a number. A misspelled word is answered with the
 * word, as one button, so the recovery is a tap and not a retyping. The hint
 * stands only when neither is true: one line teaches one thing, and a reader
 * is never taught query syntax beside a better answer.
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
    /** Rewrites the field to the corrected query the reader chose. */
    onSuggest: (String) -> Unit,
) {
    val total = results.counts.total
    val message = when {
        !hasSources -> stringResource(R.string.search_status_no_sources)
        failed -> stringResource(R.string.search_status_failed)
        query == null -> null
        // A new query starts before its results exist; the previous results
        // are never shown under it. The line says the search is running
        // instead of naming a count that belongs to another question (owner
        // decision, 13.8).
        !current -> stringResource(R.string.search_status_searching)
        searching && results.hits.isEmpty() -> stringResource(R.string.search_status_searching)
        total == 0 -> stringResource(R.string.search_status_no_matches)
        // A capped list has no honest count: the number the pass collected is
        // not the number that exists, and a reader who acts on it is acting
        // on a floor. The line says what the reader is looking at instead of
        // how much there is (owner decision, 10.0).
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
            // under the field without competing with the results
            // (owner decision, 13.3).
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // A result count, a settled search, or a failure arrives while the
            // reader's focus is still in the field. The line announces itself
            // once each time it changes, and a failure interrupts because the
            // reader has to act on it (owner decision, 13.8).
            modifier = Modifier.semantics {
                liveRegion = if (failed) LiveRegionMode.Assertive else LiveRegionMode.Polite
            },
        )
        if (noMatches) {
            // One answer, never two: the count a reference is owed, then the
            // word the reader may have meant, then the teaching hint. They
            // crowd the same small space, and each of them is a complete
            // answer to "why is this page empty" (owner decision, 22.3).
            val note = results.referenceNote
            val suggestion = results.suggestion
            when {
                note != null -> Text(
                    text = localizedDigits(
                        when (note) {
                            is ReferenceNote.SurahAyahs -> stringResource(
                                R.string.search_status_reference_past_end,
                                note.surah,
                                note.ayahs,
                            )

                            is ReferenceNote.BookSurahs -> stringResource(
                                R.string.search_status_surah_past_end,
                                note.surahs,
                            )
                        },
                    ),
                    // The count is the answer to the reader's question, so it
                    // wears the body's 15 where the line above is a caption,
                    // exactly as the hint does (owner decision, 15.0).
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = Space.Line)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )

                suggestion != null -> TextButton(
                    label = stringResource(R.string.search_status_suggestion, suggestion.word),
                    onClick = { onSuggest(suggestion.query) },
                    // The correction arrives while the reader's focus is still
                    // in the field, and it is the one thing on the page that
                    // answers their search: a polite region announces it once,
                    // in the same voice the hint uses (owner decision, 22.3).
                    modifier = Modifier
                        .padding(top = Space.Line)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )

                else -> Text(
                    text = localizedDigits(stringResource(R.string.search_status_no_match_hint)),
                    // The hint is the one line on the results' page that
                    // teaches, so it takes the body's 15 where the count above
                    // it is a caption (owner decision, 15.0).
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = Space.Line)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
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
