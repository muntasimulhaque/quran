package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * A quiet heading over a group of rows. It opens the group with more room
 * than the rows keep between themselves, so the heading reads as the name of
 * what follows rather than as another row.
 */
@Composable
fun Group(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 22.dp,
            end = 22.dp,
            top = Space.Block + Space.Tight,
            bottom = Space.Line,
        ),
    )
}

/**
 * One category in the settings hub: what it is called, where it stands right
 * now, and the chevron that says it opens something, in the chevron's own
 * column of the row's tail, before the switch's column (D-111).
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
            modifier = Modifier.weight(1f),
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        RowTail(chevron = true)
    }
}

/**
 * The chevron that stands one column in from a settings row's end, where
 * every row keeps it: 48 dp wide, drawn when the row has a page behind it
 * and held empty when it does not, so every chevron in the sheet stands at
 * one place and every switch at another, whatever the row carries (owner
 * report, D-111).
 *
 * The slot owns a 48 dp square only when the chevron is a control of its
 * own. On every other row it is a width, not a height: a square tail is
 * taller than the line of text beside it, and a Row takes the height of its
 * tallest child, so an empty square stretched every plain row from 51 dp to
 * 76 dp and turned the hub into a ladder (owner report, D-105). Where the
 * chevron does carry its own tap it keeps the square, which costs the row
 * nothing, because the switch beside it already holds 48 dp.
 */
@Composable
private fun ChevronSlot(
    visible: Boolean,
    onOpen: (() -> Unit)? = null,
    label: String? = null,
) {
    val tail = if (onOpen != null) Modifier.size(48.dp) else Modifier.width(48.dp)
    Box(
        modifier = tail
            .then(
                if (onOpen != null) {
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpen)
                        .semantics {
                            if (!label.isNullOrBlank()) contentDescription = label
                            role = Role.Button
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (visible) {
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    // The mark says where a tap goes: the chevron points the
                    // way the page slides in, to the right.
                    .graphicsLayer(rotationZ = -90f)
                    .testTag("row-chevron"),
            )
        }
    }
}

/** The head of a page the reader opened: one way back, one name. */
@Composable
fun PageHeader(title: String, onBack: () -> Unit) {
    val back = stringResource(R.string.settings_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 22.dp, top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack)
                .semantics {
                    contentDescription = back
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(22.dp)
                    // The chevron opens a page pointing down, so going back
                    // is the same mark turned to point the way it came.
                    .graphicsLayer(rotationZ = 90f),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/**
 * Material 3's switch is one track wide, 52 dp, and the tail reserves that
 * exact width so the chevron column lands at one place whether the row
 * carries a switch or not. [SettingsRowAlignmentTest] measures the real
 * control against this number, so a Material release that widens the track
 * fails there instead of silently moving every chevron off the line.
 */
private val SwitchSlot = 52.dp

/**
 * The two columns every settings row ends with: the chevron's, then the
 * switch's. A row draws the marks it carries and leaves the other column's
 * room empty, so every chevron in the sheet stands at one place and every
 * switch at another, whatever a row carries, and the switch is always the
 * line's last mark (owner report, D-111).
 */
@Composable
private fun RowTail(
    chevron: Boolean,
    onOpen: (() -> Unit)? = null,
    chevronLabel: String? = null,
    switch: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ChevronSlot(visible = chevron, onOpen = onOpen, label = chevronLabel)
        Box(
            modifier = Modifier.width(SwitchSlot),
            contentAlignment = Alignment.CenterEnd,
        ) {
            switch()
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
 * as a second, unexplained control (owner report, D-097). One row now carries
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
 * sheet's edge, and the chevron sits one column before it whether the row has
 * a door or not, so every switch in the sheet ends at one line and every
 * chevron stands at one place (owner report, D-111). The chevron keeps its
 * own touch target and its own spoken name beside the door, so TalkBack reads
 * a switch and a door rather than one crowded control.
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
                    Modifier.clickable(role = Role.Button, onClick = onOpen)
                } else {
                    Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                },
            )
            .padding(start = 22.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The name and its note keep a clear gap from the switch, the way the
        // marks in the rows above keep theirs from the names they select: a
        // subtitle set flush against the control reads as part of it, and on
        // a long line the two merge into one crowded shape. The gap is the
        // same one a choice row puts on the other side of its trailing
        // control, so the whole sheet answers with one measurement.
        Column(
            Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Where the row opens a page, the switch is the only thing that
        // switches, so it takes its own clicks and carries the row's name: a
        // control that reads "on" with nothing to say who is on tells TalkBack
        // nothing. Where there is no page, the whole row is the control and the
        // switch is only a picture of its state.
        val switchModifier = Modifier
            .then(if (opens) Modifier.semantics { contentDescription = title } else Modifier)
            .then(if (switchTag != null) Modifier.testTag(switchTag) else Modifier)
        RowTail(
            chevron = onOpen != null,
            onOpen = onOpen,
            chevronLabel = openLabel,
        ) {
            Switch(
                checked = checked,
                onCheckedChange = if (opens) onChange else null,
                modifier = switchModifier,
            )
        }
    }
}

/** A row of two lines that opens a door. */
@Composable
fun TextRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A line the reader cannot change: a version, a size, what stands where. */
@Composable
fun ValueRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
        )
    }
}

