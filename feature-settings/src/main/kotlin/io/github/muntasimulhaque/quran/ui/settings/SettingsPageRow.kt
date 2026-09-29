package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One category in the settings hub: what it is called, where it stands right
 * now, and the chevron that says it opens something, in the chevron's own
 * column of the row's tail, before the switch's column (D-111).
 *
 * The name and the value are two halves of one line, and the name is the half
 * that is read first, so the name holds its room: the value is measured
 * inside a share of the row and never past it, and the name's share is the
 * larger of the two, so a value twice as long as its name still leaves the
 * name a line to itself. Before this, the value was measured unweighted and
 * took every pixel it wanted: a long value left "Theme" a column so narrow
 * that the word broke one letter to a line and the row became four lines
 * tall, and "Font size" two (owner report, 37th session). A value that is
 * longer than its share wraps to a second line, right-aligned under itself,
 * and one longer still is cut with an ellipsis, which says less rather than
 * saying nothing.
 *
 * Both halves fill their share, so the chevron and the switch columns that
 * follow stand at the same place on every row in the sheet whatever the value
 * measures (D-111): a value that took only the width it needed would push the
 * marks after it along with itself.
 *
 * The value keeps the same type as a switch row's status line, so the sheet
 * has one size for "where this stands" and one for the name of the thing.
 */
@Composable
fun PageRow(
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.25f),
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
            )
        }
        RowTail(chevron = true)
    }
}
