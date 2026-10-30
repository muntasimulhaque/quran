package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.glyph.GlyphGrid
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space

/** The room the capsule keeps from the row's own edge, so stacked rows read
 * as a list and the first and last stay clear of the sheet. */
internal val CapsuleGutter = 6.dp

/** The capsule's own vertical inset: the chip's own 8 dp. */
internal val CapsulePad = 8.dp

/** The room from the row's edge to the capsule: 10 plus the row's own 12
 * stands the mark on the app's 22 dp gutter. */
internal val RowGutter = 10.dp

/**
 * Where the name of a choice row begins, from the row's own edge: the gutter,
 * the row's own inset, the mark, and the gap to the name. A block that lives
 * under a choice row (a reciter's own surahs) starts on this line, so it
 * reads as belonging to the name above it.
 */
val ChoiceRowNameInset = RowGutter + 12.dp + 22.dp + 16.dp

/**
 * One thing a reader chooses from a list: the app's one selectable row,
 * wherever the choice appears.
 *
 * The mark comes before the name, so the eye lands on the state first and the
 * name reads as the thing it belongs to, and the whole row is one control. The
 * mark and the name keep a clear gap between them: a mark set against the word
 * it selects reads as one crowded glyph, and the reader has to look twice to
 * see which mark belongs to which name. A row that also carries an action
 * keeps it at the far end, after the name.
 *
 * One mark per kind, learned once, so the same choice wears the same shape
 * whichever door opened its list:
 *
 * - [radio] true is one answer among several: a ring with a dot, the shape of
 *   "the one".
 * - [radio] false is any number on: the same ring with a check, the shape of
 *   "this one too". The ring stays drawn in both states, so the container
 *   never changes shape and an off row is never read as an off radio.
 *
 * The measurements are the app's one rhythm, so a list in the settings sheet
 * and the same list in the playback pill's menu cannot drift: a 22 dp mark,
 * 16 dp from it to the name, an 8 dp inset for the capsule the reader
 * presses, and the 48 dp minimum target every row owes a finger. The capsule
 * keeps a 6 dp gutter from the row's own edge and the row's own 22 dp gutter
 * from the sheet, so stacked rows read as a list and the first and last
 * capsules never meet the sheet they stand on (owner report, this session:
 * the capsule had carried the row's own room and met the popup's edge).
 * [bottomPadding] closes the foot of a row that has a block under it. The
 * ripple is drawn on the capsule inside that target, so a row that answers a
 * tap wears the same shape the chips and the doors wear, wherever it stands
 * (owner decision, this session: the row's press was a rectangle while the
 * app's other controls were capsules). The action slot is drawn
 * only where a row has an action, so a row that is only a mark and a name
 * keeps its whole width for the name. An unselected ring is a control
 * boundary, so it holds the 3:1 a control owes: 0.6 of the accent was
 * measured at 3.0:1 and up on all four grounds, where a lighter ring read as
 * nothing.
 *
 * The mark carries a test tag, because it is the anchor a test reads to prove
 * that the mark still leads the name on both surfaces.
 */
@Composable
fun ChoiceRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    radio: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    /** The room under the row; a row with a block beneath it keeps less. */
    bottomPadding: Dp = 12.dp,
) {
    val press = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = if (radio) Role.RadioButton else Role.Checkbox,
                interactionSource = press,
                indication = null,
                onClick = onClick,
            )
            // Inside the selectable, so the target the finger gets is the full
            // 48 dp the minimum holds and not the text's own shorter box.
            .minimumInteractiveComponentSize()
            // The gutter the capsule keeps from the row's own edge: the click
            // target still owns the whole row, and only the shape the eye
            // sees is inset, so stacked rows read as a list and the first and
            // last capsules stay clear of the sheet (owner report, this
            // session).
            .padding(
                start = RowGutter,
                end = RowGutter,
                top = CapsuleGutter,
                bottom = (bottomPadding - CapsulePad).coerceAtLeast(0.dp),
            )
            // The ripple is drawn on the capsule inside the target, not on the
            // target itself: one shape for anything that answers a tap, so a
            // settings list and the pill's own menu cannot disagree.
            .clip(Radii.Pill)
            .indication(press, ripple())
            .padding(
                start = Space.Row,
                end = Space.Row,
                top = CapsulePad,
                bottom = CapsulePad,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChoiceMark(selected = selected, radio = radio)
        Column(
            Modifier
                .weight(1f)
                .padding(start = Space.Block),
        ) {
            Text(
                text = title,
                // A choice's name wears the row-name voice, so a settings
                // list and the pill's menus read as one hand (owner decision,
                // this session).
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    // A choice's second line is its value, so it wears the
                    // caption: 12 at Regular, under a 16 Medium name (owner
                    // decision, this session).
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.Tight),
                )
            }
        }
        if (trailing != null) {
            Box(Modifier.padding(start = Space.Row)) { trailing() }
        }
    }
}

/**
 * The app's own mark: a drawn ring, with a dot for the one answer kept and a
 * check for one more of several on. The ring is the container and never
 * changes shape, so the state is the inner glyph and the kind of choice is the
 * glyph itself, not a second row style to learn.
 *
 * The check is drawn inside the ring rather than in its place: a bare check
 * left the mark column alternating between circles and checks, and an off
 * row's empty ring was the same shape an off radio wears.
 *
 * The ring is drawn on the glyph set's own grid, at the set's one stroke and
 * its round keyline, so the ring and the check it holds carry one weight. It
 * had drawn its own 0.09 of the box, which made a "v" of a ring heavier than
 * every mark beside it (owner decision, this session).
 */
@Composable
private fun ChoiceMark(selected: Boolean, radio: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(22.dp)
            .testTag("choice-mark"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            // The set's 24 unit box, its stroke for a mark of this size, and
            // the 10 unit radius of the choice ring, read at this mark's own
            // measure.
            val unit = size.width / GlyphGrid.Size
            val weight = GlyphGrid.strokeFor(size.width / density)
            drawCircle(
                color = if (selected) accent else accent.copy(alpha = 0.6f),
                radius = 10f * unit,
                style = Stroke(width = weight * unit),
            )
            if (selected && radio) {
                drawCircle(color = accent, radius = 5f * unit)
            }
        }
        if (selected && !radio) {
            IconGlyph(
                icon = Icon.Check,
                tint = accent,
                modifier = Modifier.size(GlyphSize.Check),
            )
        }
    }
}
