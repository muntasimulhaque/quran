package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.sheetToggleRow
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.appSwitchColors

/**
 * A quiet heading over a group of rows. It opens the group with more room
 * than the rows keep between themselves, so the heading reads as the name of
 * what follows rather than as another row.
 *
 * It wears the app's one head over a list, [SectionHead], so a group in the
 * settings sheet and a group in the pill's listening panel are one voice. The
 * air is this page's own: the hub opens a group with a block's room and closes
 * it with a line's.
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
 * A switch row: the whole row is the switch, and the switch is the picture of
 * its state. When the content behind the switch is chosen on its own page,
 * the page gets a separate [TextButton] under the row, never a second action
 * inside it. One row, one meaning, one target.
 *
 * This is the one grammar for every switch in the sheet. The older shape put a
 * chevron and a switch in the same row, where the switch toggled and the rest
 * of the row opened a page; a reader who tapped the words expected the switch
 * and landed on a page, and TalkBack met two controls where the eye saw one.
 * The switch is now a state mark, the row is the control, and the page has its
 * own quiet door below.
 */
@Composable
fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    /** The page this switch's content is chosen on, when there is one. */
    onOpen: (() -> Unit)? = null,
    openLabel: String? = null,
    /** The anchor a test taps when it means the switch's row. */
    switchTag: String? = null,
    /** A separate anchor for the row itself, when a test needs one. */
    rowTag: String? = null,
    /** A separate anchor for the page's quiet door. */
    doorTag: String? = null,
    /** False when the switch has nothing to show yet; the page door stays. */
    enabled: Boolean = true,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sheetToggleRow(value = checked, onValueChange = onChange, enabled = enabled)
                .then(if (rowTag != null) Modifier.testTag(rowTag) else Modifier)
                .minimumInteractiveComponentSize()
                .padding(
                    start = Space.Gutter - SheetRowGutter,
                    end = Space.Row - SheetRowGutter,
                    top = Space.Row,
                    bottom = Space.Row,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowName(
                title = title,
                value = subtitle,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            )
            // The switch accepts a touch as a second way to the same state,
            // but its semantics are cleared: the row is the one spoken
            // control, and the wrapper carries the test anchor.
            RowTail(chevron = false) {
                // The wrapper carries the test anchor and the switch keeps its
                // own touch, while the row carries the one spoken control:
                // a switch that repeats the row's name would be a second stop
                // for one setting.
                Box(
                    modifier = Modifier.then(
                        if (switchTag != null) Modifier.testTag(switchTag) else Modifier,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Switch(
                        checked = checked,
                        onCheckedChange = onChange,
                        enabled = enabled,
                        colors = appSwitchColors(),
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
        if (onOpen != null && !openLabel.isNullOrBlank()) {
            TextButton(
                label = openLabel,
                onClick = onOpen,
                quiet = true,
                modifier = Modifier
                    .padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.Line)
                    .then(if (doorTag != null) Modifier.testTag(doorTag) else Modifier)
                    .semantics { contentDescription = openLabel },
            )
        }
    }
}

/**
 * A line the reader cannot change: a version, a license, what stands where.
 *
 * It is the same words every other row wears, in the same place, and it ends
 * in no mark at all: there is nothing here to tap and nothing here to open, so
 * a chevron would promise a page that is not there (owner decision).
 */
@Composable
fun ValueRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Row, bottom = Space.Row),
    ) {
        RowName(title = title, value = value)
    }
}
