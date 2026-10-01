package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * One category in the settings hub: what it is called, where it stands right
 * now, and the chevron that says it opens something. The chevron is the row's
 * own last mark, standing on the margin where a switch's right edge stands
 * (owner report, D-120, completed by D-130).
 *
 * The row wears the same words as every other row in the sheet: its name, and
 * under it the value, in [RowName]. The value used to stand at the right of
 * the name in a column of its own, measured so that the name kept its room
 * (D-116, D-130), which made this row the one place in the sheet where a grey
 * value sat beside its name while the rows with a switch printed it underneath
 * (owner decision, D-134). It is under the name here for the same reason it
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
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The name and its value keep the same clear gap from the marks as the
        // switch row keeps from its switch, so every row in the sheet reads as
        // one block of words and a column of marks.
        RowName(
            title = title,
            value = summary,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        RowTail(chevron = true)
    }
}