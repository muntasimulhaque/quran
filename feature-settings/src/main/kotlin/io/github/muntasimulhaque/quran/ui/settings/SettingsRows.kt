package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.kit.AppSwitch
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.kit.sheetToggleRow
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * A quiet heading over a group of rows. It opens the group with more room
 * than the rows keep between themselves, so the heading reads as the name of
 * what follows rather than as another row.
 *
 * It wears the app's one head over a list, [SectionHead], so a group in the
 * settings sheet and a group in the pill's listening panel are one voice. The
 * air is this page's own: the hub opens a group with a block's room plus a
 * tight, [Space.Block] + [Space.Tight], 20 dp, and closes it with a line's.
 *
 * It is 20 and not a block's 16: the head's room above it belongs to the list
 * under it, so the air over the first row is 20 (a line's 8 plus the row's own
 * 12) and the air between two groups is 32 (the row's 12 plus the head's 20).
 * The word meaning sheet's facts are the other job: a fact is a block, so it
 * stands a section from the fact before it (owner decision, this session).
 */
@Composable
fun Group(title: String) {
    SectionHead(
        text = title,
        modifier = Modifier.padding(
            start = Space.Gutter,
            end = Space.Gutter,
            top = Space.Block + Space.Tight,
            bottom = Space.Line,
        ),
    )
}

/**
 * A switch row: the whole row turns the reading setting on or off.
 *
 * The row is the control. A finger that lands on the name flips it, and a
 * screen reader hears one stop that says the setting, its value, and that it
 * is a switch. The switch drawn at the row's end is a picture of the row's
 * state, so the words and the mark can never answer different taps.
 *
 * This row is the page's own shape. The hub is an index and carries no
 * switches: a reader who wants to change what the reading draws opens the page
 * that names it, and the switch stands at the top of that page, next to the
 * list it governs. So a setting is read in one place and changed in one place,
 * and a reader who came to look at the translations cannot switch them off by
 * looking (owner decision, this session).
 *
 * [enabled] is for the page that has nothing to draw yet, which is a fact about
 * the file and not about the reader's answer: the switch keeps reading on
 * while a chosen pack is on its way, and the page says why it cannot be used.
 */
@Composable
fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    /** The anchor a test taps when it means the switch. */
    switchTag: String? = null,
    /** False when the reading has nothing to draw yet; the page says why. */
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sheetToggleRow(
                value = checked,
                onValueChange = onChange,
                enabled = enabled,
            )
            .then(if (switchTag != null) Modifier.testTag(switchTag) else Modifier)
            // The row is the control, so the row owes the finger the 48 dp.
            .minimumInteractiveComponentSize()
            .padding(
                start = Space.Gutter - SheetRowGutter,
                end = Space.Row - SheetRowGutter,
                top = Space.Row,
                bottom = Space.Row,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The name and its value keep a clear gap from the switch, the way
        // every row in the sheet keeps one gap between its words and its mark.
        RowName(
            title = title,
            value = subtitle,
            modifier = Modifier
                .weight(1f)
                .padding(end = Space.Row),
        )
        AppSwitch(
            checked = checked,
            enabled = enabled,
            modifier = Modifier
                .then(if (switchTag != null) Modifier.testTag("$switchTag-mark") else Modifier),
        )
    }
}
