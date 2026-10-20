package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import io.github.muntasimulhaque.quran.ui.reader.GlyphSize
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconButton
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
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
 * The chevron at the end of a settings row: a decorative mark, 48 dp wide,
 * drawn when the row has a page behind it and taking no width at all when it
 * does not, so every switch in the sheet stands at one place whatever the row
 * carries and no label is shortened for a mark the row does not draw.
 *
 * The column used to be reserved on every row, drawn or not, so a row with
 * no door carried 48 dp of empty sheet in front of its switch and its name
 * was 48 dp shorter than a row that had one. It was not what kept the
 * switches on one line: a switch stands at the row's own margin whatever is
 * in front of it, so the empty column never moved one. What it bought was a
 * row that grows a door later keeping its switch where it was, which is a
 * convenience for whoever edits this file next and not a thing a reader can
 * see (owner decision, forty-sixth session).
 *
 * The slot is a width, not a height: a square tail is taller than the line of
 * text beside it, and a Row takes the height of its tallest child, so a
 * square stretched every plain row from 51 dp to 76 dp and turned the hub
 * into a ladder (owner report). The row's own 48 dp target comes from the
 * switch beside it or from the row itself, so the mark costs the target
 * nothing.
 *
 * The chevron used to be a control of its own, with a spoken open label, so a
 * row with a switch and a door was three TalkBack stops. The whole row is the
 * door now and the chevron is a picture of where a tap goes, so the row is
 * one door and the switch is one control (owner decision, this session).
 *
 * [contentAlignment] is where the mark stands inside its slot. A row that
 * also has a switch centres it, because the switch's own column is beside it
 * and the two are read as a pair. A row with no switch puts the mark at the
 * end of the slot, so its right edge is the row's own margin: that is exactly
 * where a switch's right edge stands, and the rows that carry only a door
 * were ending a whole chevron short of it (owner report).
 */
@Composable
private fun ChevronSlot(
    visible: Boolean,
    contentAlignment: Alignment = Alignment.Center,
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .width(48.dp)
            // The slot, not the mark: a mark is centred in its slot, so the
            // slot's edges are what a row's alignment is about. The tag is
            // on a row that draws a chevron only, so a count of them is a
            // count of the rows that have a door (owner report).
            .testTag("chevron-slot"),
        contentAlignment = contentAlignment,
    ) {
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(GlyphSize.ChevronRow)
                // The mark says where a tap goes: the chevron points the
                // way the page slides in, to the right.
                .rotate(-90f)
                .testTag("row-chevron"),
        )
    }
}

/** The head of a page the reader opened: one way back, one name. */
@Composable
fun PageHeader(title: String, onBack: () -> Unit) {
    val back = stringResource(R.string.settings_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = Space.Gutter, top = 2.dp, bottom = Space.Line),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // One back button for every page head the app draws: the same 48 dp,
        // the same rotated chevron, the same spoken name. It used to be a
        // hand-rolled box here and a second copy on the surah grid (owner
        // decision, the theme and consistency session).
        IconButton(
            icon = Icon.Chevron,
            description = back,
            onClick = onBack,
            tint = MaterialTheme.colorScheme.onSurface,
            rotation = 90f,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            // the page's own name, and the one heading on it
            modifier = Modifier
                .semantics { heading() }
                .padding(start = 4.dp),
        )
    }
}

/**
 * Material 3's switch is one track wide, and a row that draws a switch
 * reserves that exact width for it, so every switch in the sheet ends at one
 * line at the row's own margin.
 *
 * The number is kept here, beside the code that reserves it, and
 * [SettingsRowAlignmentTest] measures the real control against it. A Material
 * release that widens the track therefore fails there, where the fix is one
 * number, instead of quietly pushing every switch out of its own column and
 * leaving it overlapping the chevron in front of it, which is what a track
 * wider than its slot does (found by that test, thirty-seventh session).
 */
val SwitchSlot = 52.dp

/**
 * The 48 dp a control owes a finger, and the height the switch's own target
 * keeps even though it no longer sets its row's height (owner decision, this
 * session). Material's switch draws this itself; the number is here because
 * the row now measures the target and reports its words, so a release that
 * shrank the target would quietly shrink the finger's room with it.
 */
private val SwitchTarget = 48.dp

/**
 * The marks a settings row ends with, in the order a row's own hands read
 * them: the chevron's column, then the switch's.
 *
 * The switch's column exists on a row that has a switch and on no other row.
 * The chevron's column exists on a row that draws a chevron and on no other
 * row: a row with nowhere to switch to keeps no empty room for one, so the
 * chevron in it is the row's last mark, and the mark itself stands on the
 * margin where a switch's own right edge stands rather than in the middle of
 * an empty column in front of it (owner report: the column had moved and left
 * the mark centred inside it).
 *
 * Every switch ends at the row's own margin, with or without a chevron in
 * front of it, and that is the whole of the alignment: both columns are read
 * from their right edges, and a right edge never moves.
 */
@Composable
internal fun RowTail(
    chevron: Boolean,
    switch: (@Composable () -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ChevronSlot(
            visible = chevron,
            // The row's last mark is the one nearest the reader's margin: a
            // row with a switch reads its chevron against the switch's own
            // column, and a row without one has nothing after it but the
            // margin, so its chevron stands on the margin (owner report).
            contentAlignment = if (switch == null) {
                Alignment.CenterEnd
            } else {
                Alignment.Center
            },
        )
        if (switch != null) {
            Box(
                modifier = Modifier
                    .width(SwitchSlot)
                    // The switch owes the finger its 48 dp target, and it does
                    // not owe the row its height. A row that carries a switch
                    // used to stand 48 dp tall inside its padding, 13 dp more
                    // than a row whose tallest part is its own two lines, and
                    // the hub read as two rhythms alternating down the sheet
                    // (owner report, this session). The target is laid out on
                    // the row's own middle and takes no height of its own: the
                    // 6.6 dp it reaches past the words on each side stand in
                    // the row's 12 dp padding, where there is room for them,
                    // and the row's height is its words'. The target itself is
                    // unchanged, and the accessibility test still measures it.
                    .layout { measurable, constraints ->
                        val target = SwitchTarget.roundToPx().coerceAtMost(constraints.maxHeight)
                        val placeable = measurable.measure(
                            constraints.copy(
                                minHeight = maxOf(constraints.minHeight, target),
                            ),
                        )
                        layout(placeable.width, 0) {
                            placeable.place(0, -placeable.height / 2)
                        }
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                switch()
            }
        }
    }
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
                            if (!openLabel.isNullOrBlank()) {
                                Modifier.semantics { contentDescription = openLabel }
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

