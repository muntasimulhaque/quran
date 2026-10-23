package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
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
 * A switch, and, when [onOpen] is given, the door to the page its content is
 * chosen on.
 *
 * The three reading switches and the packs behind them used to live in two
 * places: a switch here and a separate row for the packs, so a reader who
 * turned the translation off still had a "Translations" row below that read
 * as a second, unexplained control (owner report). One row now carries
 * both halves, and the two halves answer to different taps (owner report,
 * 2.3): the switch alone turns the reading on or off, while a tap anywhere
 * else on the row opens the page. A row that toggled wherever a finger landed
 * made the door impossible to reach without changing the reading, and a reader
 * who came to look at the translations left them switched off by accident.
 *
 * The tap that opens does not switch. A reader looking at a list has asked to
 * see it, not to change a setting, and a setting changed by a look is a
 * setting they did not choose: the page says plainly whether the reading is
 * showing what it lists, and its own switch turns it on from there.
 *
 * Without [onOpen] the whole row stays the switch, which is what a row with
 * nowhere to go owes a finger. The switch is the row's last mark, at the
 * row's own margin, and the chevron sits one column before it whether the
 * row has a door or not, so every switch in the sheet ends at one line
 * (owner report).
 *
 * The chevron is only a picture of where a tap goes. It used to be a control
 * of its own, with [openLabel] as its spoken name, so a row with a switch and
 * a door was three TalkBack stops: the row's door, the chevron's second door,
 * and the switch. The whole row is the door now, [openLabel] is its spoken
 * name, and the chevron is a decorative mark inside it, so the row is one
 * door and the switch is one control (owner decision, this session).
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
    /** The anchor a test taps when it means the switch rather than the door. */
    switchTag: String? = null,
) {
    val opens = onOpen != null
    // The whole row is the door, so its spoken name must carry the title and
    // the state under it. Naming only the open label made a screen reader
    // miss "Notifications are off, so the daily ayah cannot arrive" and the
    // other subtitles that explain why a switch is where it is.
    val spoken = listOfNotNull(openLabel, title, subtitle)
        .filter { it.isNotBlank() }
        .joinToString(". ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onOpen != null) {
                    Modifier
                        .sheetRow(onClick = onOpen, role = Role.Button)
                        // The whole row is the door, so the open label is the
                        // row's own spoken name; the chevron beside the switch
                        // is only a picture of where a tap goes (owner
                        // decision, this session).
                        .then(
                            if (spoken.isNotEmpty()) {
                                Modifier.semantics { contentDescription = spoken }
                            } else {
                                Modifier
                            },
                        )
                } else {
                    Modifier.sheetToggleRow(value = checked, onValueChange = onChange)
                },
            )
            // The row is the door, so the row owes the finger the 48 dp, the
            // same way a page row does: the switch's own target no longer
            // sets the row's height, and a row whose words are one line must
            // not fall short of it (owner decision, this session).
            .minimumInteractiveComponentSize()
            .padding(
                start = Space.Gutter - SheetRowGutter,
                end = Space.Row - SheetRowGutter,
                top = Space.Row,
                bottom = Space.Row,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The name and its value keep a clear gap from the switch, the way the
        // marks in the rows above keep theirs from the names they select: a
        // value set flush against the control reads as part of it, and on a
        // long line the two merge into one crowded shape. The gap is the same
        // one every row in the sheet keeps, so a row that opens a page and a
        // row that carries a switch are one shape.
        RowName(
            title = title,
            value = subtitle,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        // Where the row opens a page, the switch is the only thing that
        // switches, so it takes its own clicks and carries the row's name: a
        // control that reads "on" with nothing to say who is on tells TalkBack
        // nothing. Where there is no page, the whole row is the control and the
        // switch is only a picture of its state.
        val switchModifier = Modifier
            .then(if (opens) Modifier.semantics { contentDescription = title } else Modifier)
            .then(if (switchTag != null) Modifier.testTag(switchTag) else Modifier)
        RowTail(chevron = opens) {
            Switch(
                checked = checked,
                onCheckedChange = if (opens) onChange else null,
                // The switch wears the theme's own pair for this ground, and
                // not Material's two scheme roles: the off knob would be the
                // app's hairline, which on the night grounds is the same paint
                // as the track it sits in and left no knob in the control.
                colors = appSwitchColors(),
                modifier = switchModifier,
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
