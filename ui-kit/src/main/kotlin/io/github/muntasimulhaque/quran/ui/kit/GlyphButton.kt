package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * One round glyph control: the target every control owes a finger, and inside
 * it the circle the press lights.
 *
 * The app draws a round mark that answers a tap one way, whether it stands
 * alone on a page's head, or three in a row in the playback pill. The two
 * numbers live here because they are one rule and were two answers: the
 * chrome's marks drew their press on the whole 48 dp target, so the Search and
 * Settings marks that stand edge to edge in the top bar lit two circles that
 * met at a single point, and the pill's transport could not come closer than
 * 48 dp apart without its circles closing on each other. One rule answers both
 * (owner decision, this session).
 *
 * [GlyphPress] inside [GlyphTarget] is Material's own icon button, a 40 dp
 * container in a 48 dp target, and it is what makes a cluster possible: two
 * neighbouring targets stand edge to edge, where a finger never falls into a
 * dead pixel between them, while the circles they light keep the app's own
 * [io.github.muntasimulhaque.quran.ui.theme.Space.Line] of air. A target that
 * overlapped its neighbour would be a 40 dp target wearing a 48 dp name, so
 * edge to edge is the floor, and the air is measured between the shapes the
 * reader sees, never between the invisible targets.
 *
 * The click owns the target and the press is drawn on the circle inside it:
 * an indication left on the target while the shape is smaller lights a circle
 * where the reader is not looking. The same rule every other control in the
 * app wears, the press is the shape the reader sees (owner decision).
 *
 * [actions] is what TalkBack offers beside the tap, for the one thing the pill
 * does that has no shape of its own: stopping a recitation (owner decision).
 */

/** The touch target a round control owes a finger. */
val GlyphTarget = 48.dp

/** The circle a round control's press lights, inside [GlyphTarget]. */
val GlyphPress = 40.dp

/**
 * One round control: the target, the spoken name, the press, and the mark the
 * caller draws in the middle. The mark keeps its own size, because a
 * transport's 20 dp mark and a way back's 22 dp mark answer two jobs and the
 * circle they stand in is one.
 *
 * The circle carries a test tag, because it is the shape a test reads to prove
 * that two neighbours keep their air while their targets touch.
 */
@Composable
fun GlyphButton(
    description: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    /** What TalkBack offers beside the tap, where a gesture has no shape. */
    actions: List<CustomAccessibilityAction> = emptyList(),
    content: @Composable () -> Unit,
) {
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clickable(
                interactionSource = press,
                indication = null,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize()
            .semantics {
                contentDescription = description
                role = Role.Button
                if (actions.isNotEmpty()) customActions = actions
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(GlyphPress)
                .clip(Radii.Pill)
                .indication(press, ripple(color = tint))
                .testTag("glyph-press"),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
