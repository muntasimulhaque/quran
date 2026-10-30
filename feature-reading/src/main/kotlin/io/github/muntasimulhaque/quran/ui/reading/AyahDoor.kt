package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * The door's own line: the 48 dp every control owes the finger, taken as the
 * reference's line rather than added under it.
 *
 * The visible capsule is 24 dp and stands on the row's middle, so the line's
 * air is the target's air. The block keeps its own room above and below the
 * row, so two cards still stand the 24 dp apart the reading keeps, and the
 * ayah's head is a little taller than the bare reference line was (owner
 * decision, this session).
 */
private val DoorRow = 48.dp

/**
 * The ayah's own door: the number at its head, quiet at rest and a capsule
 * while its menu is open.
 *
 * The reference used to be bare type, and the ayah's actions were a long
 * press that nothing on the page announced. It is now the one visible
 * signifier on every ayah, at the one place the reader already looks for the
 * ayah's name. It keeps the 48 dp every control owes the finger, and it
 * stands inside the room the ayah already kept, so the card gap is untouched
 * and each head is a little taller (owner decision, this session: the one-tap
 * ayah of 6.5 and the long press that followed it are both gone, because a
 * signifier that must be taught is not a signifier).
 *
 * It carries its state the way the search sheet's filter door does: a caret
 * that turns when the menu is open, and the accent's own tone on the capsule,
 * so the door says which way it will move before it is tapped and says that
 * it is open while it is. The caret is the app's one chevron, drawn on the
 * icon set's grid, so the door and the filter door read as one hand.
 *
 * [onOpen] hands the door's own place on the glass to the caller, measured in
 * the window, because the menu hangs from the door and a popup cannot read
 * its anchor back through a test.
 */
@Composable
internal fun AyahDoor(
    reference: String,
    open: Boolean,
    onOpen: (IntRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bounds = remember { mutableStateOf(IntRect.Zero) }
    val press = remember { MutableInteractionSource() }
    val name = stringResource(R.string.reading_ayah_door, reference)
    val expanded = stringResource(R.string.reading_door_expanded)
    val collapsed = stringResource(R.string.reading_door_collapsed)
    val ink = if (open) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val fill = if (open) {
        MaterialTheme.colorScheme.primary.copy(alpha = Wash.Control)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)
    }
    Box(
        modifier = modifier
            .height(DoorRow),
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    val box = it.boundsInWindow()
                    bounds.value = IntRect(
                        box.left.toInt(),
                        box.top.toInt(),
                        box.right.toInt(),
                        box.bottom.toInt(),
                    )
                }
                .clickable(
                    interactionSource = press,
                    indication = null,
                    role = Role.Button,
                ) { onOpen(bounds.value) }
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .semantics {
                    contentDescription = name
                    stateDescription = if (open) expanded else collapsed
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .clip(Radii.Pill)
                    .background(fill)
                    .indication(press, ripple(color = ink, bounded = false))
                    .padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                )
                IconGlyph(
                    icon = Icon.Chevron,
                    tint = if (open) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(GlyphSize.ChevronRow)
                        .rotate(if (open) 180f else 0f),
                )
            }
        }
    }
}
