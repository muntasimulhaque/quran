package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.ControlMetrics
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
 * The quiet tone is also the destructive one: Remove, Clear, and Cancel wear
 * the same quiet shape, because the app asks before it destroys and never
 * gives a removal the loudest mark on the page. M3 would reach for the error
 * color; this app reaches for a question.
 *
 * The visible pill is 36 dp tall at the default type, the same measure the
 * app's chips and tabs wear, inside the 48 dp target every finger gets. It is
 * not Material's 40 dp small button: the app's actions are compact shapes,
 * and the target is the finger's, not the eye's.
 *
 * A button that cannot be used says so. [enabled] is the app's one disabled
 * state: the pill keeps its shape, loses the accent, and stops answering a
 * finger or a screen reader. A word that is only a status ("Saving…") is not
 * a button at all, or it is a disabled one.
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
 *
 * The same shape is available without a click as [ActionPill], for a word
 * that names a row's own act when the row around it is the control. It is
 * not a second button: the row owns the target and the spoken state, and the
 * pill only says what the act is, so the two cannot disagree about where the
 * finger lands or how many stops TalkBack counts.
 */
@Composable
fun TextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    quiet: Boolean = false,
    /**
     * The one answer a surface exists to ask for: a solid accent shape with
     * the accent's own ink. One to a surface, and never on a destructive
     * act.
     */
    prominent: Boolean = false,
    /**
     * The anchor a test reads to measure the pill itself, when the button's
     * 48 dp target is wider than the shape inside it. The target is still the
     * button.
     */
    pillTag: String? = null,
) {
    val fill = buttonFill(enabled = enabled, quiet = quiet, prominent = prominent)
    val ink = buttonInk(enabled = enabled, quiet = quiet, prominent = prominent)
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clickable(
                interactionSource = press,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(
                minWidth = ControlMetrics.Target,
                minHeight = ControlMetrics.Target,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PillBox(
            label = label,
            fill = fill,
            ink = ink,
            modifier = Modifier
                .focusRing(press, Radii.Pill)
                .indication(press, ripple(color = ink, bounded = false))
                .then(if (pillTag != null) Modifier.testTag(pillTag) else Modifier),
        )
    }
}

/**
 * The word that names a row's own act, in the same pill a [TextButton] wears,
 * with no click of its own.
 *
 * A row that is already the control (a pack row's Add, a retry, the word at
 * the end of a row that answers the touch itself) must not grow a second
 * target at its trailing end, but the word that names its act still has to
 * read as an action and not as a bare label. This is that shape: the same
 * fill, ink, type, and 14 dp inset as [TextButton], drawn inside the row that
 * owns the touch. The row's own 48 dp target and its ripple are the only
 * control; this pill is the word on it, so TalkBack hears one stop.
 *
 * It is never the only control on a surface. A word that does not have a row
 * behind it is a [TextButton].
 *
 * The two tones are the same two the button keeps: the primary tone for what
 * the row will do (Add, Retry), the quiet tone for what ends or removes.
 */
@Composable
fun ActionPill(
    label: String,
    modifier: Modifier = Modifier,
    quiet: Boolean = false,
) {
    PillBox(
        label = label,
        fill = buttonFill(enabled = true, quiet = quiet, prominent = false),
        ink = buttonInk(enabled = true, quiet = quiet, prominent = false),
        modifier = modifier,
    )
}

/** The room a button's word keeps from its pill's edge, horizontally. */
val ButtonPad = 14.dp

/** One pill, shared by the button that answers a touch and the label on a row. */
@Composable
private fun PillBox(
    label: String,
    fill: Color,
    ink: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .clip(Radii.Pill)
            .background(fill)
            .then(modifier)
            .heightIn(min = ControlMetrics.PillHeight)
            .padding(horizontal = ButtonPad, vertical = ControlMetrics.PillPad),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = ink,
            // A button's word is one line: the app's labels are short, and a
            // caller that hands a long one (a recent search, a query the
            // reader wrote) gets an ellipsis at the row's edge rather than a
            // shape that grows past it (owner decision, this session).
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun buttonFill(enabled: Boolean, quiet: Boolean, prominent: Boolean): Color = when {
    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Disabled)
    prominent -> MaterialTheme.colorScheme.primary
    quiet -> MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)
    else -> MaterialTheme.colorScheme.primary.copy(alpha = Wash.Button)
}

@Composable
private fun buttonInk(enabled: Boolean, quiet: Boolean, prominent: Boolean): Color = when {
    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
    prominent -> MaterialTheme.colorScheme.onPrimary
    quiet -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
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
