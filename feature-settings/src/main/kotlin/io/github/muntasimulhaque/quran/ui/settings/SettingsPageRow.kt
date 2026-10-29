package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * One category in the settings hub: what it is called, where it stands right
 * now, and the chevron that says it opens something. The chevron is the row's
 * own last mark, standing on the margin where a switch's right edge stands
 * (owner report).
 *
 * The row wears the same words as every other row in the sheet: its name, and
 * under it the value, in [RowName]. The value used to stand at the right of
 * the name in a column of its own, measured so that the name kept its room
 *, which made this row the one place in the sheet where a grey
 * value sat beside its name while the rows with a switch printed it underneath
 * (owner decision). It is under the name here for the same reason it
 * is there, and the row's padding is the switch row's padding, so the sheet
 * has one rhythm as well as one grammar.
 *
 * A value longer than the row takes a second line under itself, and one longer
 * still is cut with an ellipsis.
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
            // The click owns the 48 dp node, so a row without a summary is
            // still a full target to a finger and to a screen reader (owner
            // decision, the accessibility test), and the press is a card
            // inset from the sheet's edge.
            .sheetRow(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(
                start = Space.Gutter - SheetRowGutter,
                end = Space.Row - SheetRowGutter,
                top = Space.Row,
                bottom = Space.Row,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The name and its value keep the same clear gap from the marks as the
        // switch row keeps from its switch, so every row in the sheet reads as
        // one block of words and a column of marks.
        RowName(
            title = title,
            value = summary,
            modifier = Modifier.weight(1f).padding(end = Space.Row),
        )
        RowTail(chevron = true)
    }
}