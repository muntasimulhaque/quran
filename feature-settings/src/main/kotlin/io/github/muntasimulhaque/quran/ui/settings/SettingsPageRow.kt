package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.SettingsRow

/** The name's own clearance to the value beside it, inside its own column. */
private val ValueGap = 10.dp

/**
 * One category in the settings hub: what it is called, where it stands right
 * now, and the chevron that says it opens something. The chevron is the
 * row's own last mark, standing on the margin where a switch's right edge
 * stands (owner report, D-120, completed by D-130).
 *
 * The name and the value are two halves of one line, and the name is the
 * half that is read first, so it holds its room: the value is measured inside
 * its own share of the row and never past it, and the name's share is the
 * larger of the two, so a value twice as long as its name still leaves the
 * name a line to itself (owner report, 37th session, D-116).
 *
 * Both halves are measured, so the split is the room the name actually needs
 * rather than a fixed share of the row: on a 360 dp phone the value's share
 * was 125 dp, and both values the sheet really prints ("Night · day page
 * Paper" and "Arabic 25, translation 14") are longer than that, so the theme
 * and font rows each broke to a second line under a name with room to give
 * (owner report, D-130). The rule is pure, in `core`, with its own tests. A
 * value longer than the room left over takes a second line, right-aligned
 * under itself, and one longer still is cut with an ellipsis, which says less
 * rather than saying nothing.
 *
 * The value fills whatever the name does not, so the row's end stands at the
 * same place on every row whatever the value measures (D-111 for the switch
 * column, D-120 for the chevron that ends a row without one).
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
    val nameStyle = MaterialTheme.typography.bodyLarge
    val valueStyle = MaterialTheme.typography.bodySmall
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The row's own measure, which is what the split is a share of: the
        // text's half of the row, with the chevron's column already taken out
        // of it by the tail beside this box.
        BoxWithConstraints(Modifier.weight(1f)) {
            val nameColumn = remember(maxWidth, title, nameStyle) {
                val gap = with(density) { ValueGap.roundToPx() }
                val name = measurer.measure(
                    text = title,
                    style = nameStyle,
                    constraints = Constraints(),
                    // The measure has to be the one the row is laid out at, or
                    // it comes back in dp and is compared with a measure in
                    // pixels, and a name is then a third of its real width.
                    density = density,
                ).size.width
                with(density) {
                    SettingsRow.nameColumn(
                        availablePx = maxWidth.roundToPx(),
                        nameWidthPx = name,
                        gapPx = gap,
                    ).toDp()
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = nameStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(nameColumn),
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = valueStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        RowTail(chevron = true)
    }
}