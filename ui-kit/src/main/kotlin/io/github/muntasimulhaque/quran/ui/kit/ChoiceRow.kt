package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph

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
 * 16 dp from it to the name, 12 dp above the row and 12 below it
 * ([bottomPadding] closes the foot of a row that has a block under it), and
 * the 48 dp minimum target every row owes a finger. The action slot is drawn
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = if (radio) Role.RadioButton else Role.Checkbox,
                onClick = onClick,
            )
            // Inside the selectable, so the target the finger gets is the full
            // 48 dp the minimum holds and not the text's own shorter box.
            .minimumInteractiveComponentSize()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChoiceMark(selected = selected, radio = radio)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
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
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (trailing != null) {
            Box(Modifier.padding(start = 12.dp)) { trailing() }
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
            val w = size.width
            drawCircle(
                color = if (selected) accent else accent.copy(alpha = 0.6f),
                radius = w * 0.42f,
                style = Stroke(width = w * 0.09f),
            )
            if (selected && radio) {
                drawCircle(color = accent, radius = w * 0.2f)
            }
        }
        if (selected && !radio) {
            IconGlyph(
                icon = Icon.Check,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
