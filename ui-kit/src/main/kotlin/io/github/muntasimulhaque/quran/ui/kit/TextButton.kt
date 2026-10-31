package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The app's one text-only button: a word that acts, worn in a rounded shape
 * of the theme's own quiet fill.
 *
 * The shape is the distinction the app keeps. A heading, a title, or a name
 * is bare type; a control is a shape the finger can see, and it says so
 * before it is tapped. Without it, "Save" and "Note" read as the same kind of
 * word on the same line, and the reader has to guess which one answers a
 * touch; with it, the reader never has to.
 *
 * Two tones, one shape: the primary tone carries what the reader came to do
 * (Save, Add, Retry, Download), and the quiet tone carries what ends or
 * removes (Clear, Cancel, Remove).
 *
 * **A surface that asks one question has one prominent answer** (owner
 * decision, this session). The wash is right for a door among doors, but a
 * note sheet, the reminder card, and a download offer exist to ask for one
 * act, and a pale shape beside a Cancel read as its equal. [prominent] is
 * the same pill in the accent at full strength, so the reader's eye has one
 * place to land. Its fill and ink are the scheme's own `primary` and
 * `onPrimary`, the pair every ground already measures in `ContrastTest`, so
 * the emphasis costs the palette no new tone. It is never for a destructive
 * action and never for a row in a list.
 *
 * The click and the node a screen reader focuses own a full 48 dp slab,
 * while the shape the eye reads is drawn on the pill inside it. The ripple
 * draws on the pill too, not on the slab, so the press never flashes outside
 * the shape the reader is looking at. The touch target is the slab, not a
 * modifier outside the click: the accessibility test measures the node the
 * platform exposes, and that node has to be the whole target, not a wrapper
 * around it (owner decision, this session). The ripple is unbounded, so its
 * circle is never sliced by the pill's shorter side: the pill's own clip is
 * what shapes the press (owner decision, this session).
 *
 * Two text buttons side by side stand `Space.Action` (20 dp) apart between
 * their pills: each is a standalone action, so the pair reads as two answers
 * and the gap belongs between the shapes the reader sees, not between the
 * invisible 48 dp targets each one keeps. The share card's two doors, the
 * reminder dialog's Set and Cancel, the remove question, a note's Clear and
 * Save, and the pill's Retry and Close all read that one action gap. The app's
 * other side-by-side gap, `Space.Line` (8 dp), belongs to a cluster of
 * same-kind small controls, the search and Browse chips, where the eye reads
 * one group (owner decision, this session).
 *
 * A pair inside Material's own dialog is not outside the rule: the platform
 * lays two dialog buttons [DialogButtonSpacing] apart, so the caller asks for
 * `Space.Action - DialogButtonSpacing` and the shapes end up as far apart as
 * every other pair in the app.
 */
@Composable
fun TextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quiet: Boolean = false,
    /**
     * The one answer a surface exists to ask for: a solid accent shape with
     * the accent's own ink. One to a surface, and never on a destructive
     * act.
     */
    prominent: Boolean = false,
) {
    val fill = when {
        prominent -> MaterialTheme.colorScheme.primary
        quiet -> MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = Wash.Button)
    }
    val ink = when {
        prominent -> MaterialTheme.colorScheme.onPrimary
        quiet -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clickable(
                interactionSource = press,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(Radii.Pill)
                .background(fill)
                .indication(press, ripple(color = ink, bounded = false))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = ink,
                // A button's word is one line: the app's labels are short, and
                // a caller that hands a long one (a recent search, a query
                // the reader wrote) gets an ellipsis at the row's edge rather
                // than a shape that grows past it (owner decision, this
                // session).
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * How far apart Material's own dialog lays two of its buttons.
 *
 * `TimePickerDialog` puts its actions in a `Row` with `spacedBy(Space.Line)`, and an
 * `AlertDialog` in a flow row with `ButtonsMainAxisSpacing`, also 8 dp, so a
 * dialog is never edge to edge however its two buttons look. A caller that
 * padded its pair by the app's whole action gap drew a gap wider than every
 * other pair in the app, and nothing on the page said where the extra 8 dp
 * came from. The number stands here, named, so a Material update that moves it
 * is one constant to change rather than a silence (owner decision, this
 * session).
 */
val DialogButtonSpacing = 8.dp
