package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * The words a settings row wears: its name, and under it the place the row
 * stands. One shape, for every row in the sheet.
 *
 * The value is under the name and never beside it, and that is the answer to a
 * question the hub used to ask twice (owner decision, D-134). A row with no
 * switch printed its value in a column at the right ("Paper", "Husary",
 * "Arabic 25, translation 14"), and a row with a switch printed the same grey
 * line under its name ("Saheeh International", "Ibn Kathir"), so one list
 * read as two grammars and the eye had to change its reading half way down
 * (owner report, the forty-fourth session).
 *
 * Below is where it belongs, for reasons that are about the reading and not
 * about taste:
 *
 * - the tail of every row is the marks, and the marks already end every row at
 *   one line (D-111, D-120, D-130); a value beside them would have to be
 *   measured into what is left, and on a 360 dp phone that was 125 dp for two
 *   values that are both longer than it (D-116, D-130);
 * - some of the sheet's grey lines are sentences, not values ("Add English
 *   words, 4.2 MB", "The whole surah begins again at its first ayah"), and a
 *   sentence in a right-hand column is worse than a sentence under a name;
 * - the sheet's other rows already say it this way: a pack row is a name over
 *   its note with its action at the right, and a segmented row is a name over
 *   its steps (D-087).
 *
 * What it costs is height: a row with a value is two lines instead of one, so
 * the hub is about eighty dp taller and scrolls where it once fit. That is the
 * price of one grammar, and the alternative is a sheet that answers the same
 * question in two places.
 *
 * The value wraps to a second line under itself and is cut with an ellipsis
 * only past that, which says less rather than saying nothing. The name is
 * never squeezed: it has the row's width to itself.
 */
@Composable
internal fun RowName(
    title: String,
    /** Where the row stands, when the row has something to say about itself. */
    value: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}