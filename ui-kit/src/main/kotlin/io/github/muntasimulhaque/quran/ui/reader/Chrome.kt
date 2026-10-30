package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.GlyphButton
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * An icon button: a full target of touch, the control's mark, one spoken
 * label. The mark is [GlyphSize.Control], the size every mark inside the app's
 * round control takes, so the top bar's three and the pill's transport draw the
 * same measure, 24 dp, the measure the Material icon button was drawn for.
 *
 * The target is the smallest the guidelines allow, never the size of the icon,
 * and the press is the app's own round control: the circle inside the target,
 * not the target itself, so a mark that stands beside another mark keeps its
 * air (owner decision, this session). [GlyphButton] is the whole of the
 * geometry, which the top bar's marks and the playback pill's transport read
 * as one rule.
 */
@Composable
fun IconButton(
    icon: Icon,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    active: Boolean = false,
    /** A quarter turn for a mark that points a way, like the back chevron. */
    rotation: Float = 0f,
) {
    val ink = if (active) MaterialTheme.colorScheme.primary else tint
    GlyphButton(
        description = description,
        onClick = onClick,
        tint = ink,
        modifier = modifier,
    ) {
        IconGlyph(
            icon = icon,
            tint = ink,
            modifier = Modifier
                .size(GlyphSize.Control)
                .then(if (rotation != 0f) Modifier.rotate(rotation) else Modifier),
        )
    }
}
/** The gap a control keeps between its words and the shape around them. */
private val TitlePad = 14.dp

/**
 * The surah and its part, as one quiet line of type, centered under the
 * controls. The juz label is drawn only when it truly fits beside the whole
 * name: the name of the surah is what the reader looks for, and a tight bar
 * must shorten the label, never the name.
 *
 * When [onClick] is given the title answers a tap, which is the way back to
 * the top of a long surah without a scroll. It then wears the app's one
 * control shape, a capsule, and its own target hugs the words it names
 * rather than filling the bar's centre: the shape the eye reads and the
 * node a finger gets are the same size, so a reader who taps the title sees
 * the boundary of what they tapped (owner decision, this session). The
 * words keep a [TitlePad] gap from the capsule's edge, the same gap the
 * app's text button keeps between its label and its shape, so the capsule
 * never touches the type it names; the gap is measured into the shape, so
 * the shape and the node stay the same size. The capsule carries no fill,
 * so the surah's name still reads as a label at rest, and the shape is only
 * the press.
 */
@Composable
fun ReadingTitle(
    surah: String,
    detail: String?,
    modifier: Modifier = Modifier,
    /** What a tap on the title does, said out loud. */
    action: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Layout(
        content = {
            Text(
                text = surah,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .clip(Radii.Pill)
                    .clickable(role = Role.Button, onClick = onClick)
                    // The surah's own name and its juz are the node's text and
                    // stay what a screen reader reads first; the tap is said
                    // beside them as an action rather than over them, because
                    // a description here would replace the name the reader is
                    // looking for.
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(action.orEmpty()) {
                                onClick()
                                true
                            },
                        )
                    }
            } else {
                Modifier
            },
        ),
    ) { measurables, constraints ->
        val gap = 8.dp.roundToPx()
        val padX = TitlePad.roundToPx()
        // The name is measured with the capsule's own gap on both sides, so
        // an ellipsis it may need falls inside the shape rather than under
        // its curve.
        val name = measurables[0].measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxWidth = (constraints.maxWidth - 2 * padX).coerceAtLeast(0),
            ),
        )
        // The label is measured at its own size and placed only if the whole
        // name plus the label and the capsule's gap fit; the name is never
        // cut to make room for the label beside it.
        val label = measurables.getOrNull(1)
            ?.measure(Constraints())
            ?.takeIf { name.width + gap + it.width + 2 * padX <= constraints.maxWidth }
        // The name and its part are one unit, and the unit is the shape: the
        // box reports what the words measure plus the gap around them rather
        // than the bar's whole centre, so the capsule the reader sees on a
        // tap is exactly the control they tapped. The unit is a control when
        // it answers a tap, and a control owes the finger 48 dp: the type is
        // shorter and narrower than that, so the box is grown to the floor
        // and the lines are centered in it (owner report, the accessibility
        // test). The row that holds the title centres the unit, so the name
        // stays where the eye expects it whether it is wide or narrow.
        val contentWidth = name.width + (label?.let { gap + it.width } ?: 0)
        val shapeWidth = contentWidth + 2 * padX
        val width = shapeWidth
            .coerceAtLeast(48.dp.roundToPx())
            .coerceAtMost(constraints.maxWidth)
        val start = ((width - shapeWidth) / 2).coerceAtLeast(0) + padX
        val height = maxOf(name.height, label?.height ?: 0, 48.dp.roundToPx())
        layout(width, height) {
            name.place(start, (height - name.height) / 2)
            label?.place(start + name.width + gap, (height - label.height) / 2)
        }
    }
}
