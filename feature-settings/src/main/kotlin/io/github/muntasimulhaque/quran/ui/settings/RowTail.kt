package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.reader.GlyphSize
import io.github.muntasimulhaque.quran.ui.reader.IconButton
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Space

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
