package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.uikit.R as UiKitR
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.AppChevron
import io.github.muntasimulhaque.quran.ui.kit.ChevronRole
import io.github.muntasimulhaque.quran.ui.kit.ReferenceLabel
import io.github.muntasimulhaque.quran.ui.theme.ControlMetrics
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
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
 * The ayah's own door: the number at its head, in the app's action lapis,
 * with an accent wash and a hairline while its menu is open.
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
 *
 * **The door also says what the reader kept** (owner decision, this session).
 * Save closes the menu and leaves the page exactly as it was, so the only way
 * to learn that the tap worked was to open the menu again. A saved ayah now
 * wears the app's filled bookmark beside its number, in the accent, and the
 * mark is spoken with the door's own state. It is the Save mark and never a
 * note's keep, because the app keeps those two apart: the bookmark says the
 * reader asked for it, and a note written on an ayah is the note's own mark in
 * the menu. The Arabic-only reading draws the verse's printed medallion as its
 * door, so this mark belongs to the reference line and the page keeps its
 * Quran alone.
 */
@Composable
internal fun AyahDoor(
    reference: String,
    open: Boolean,
    /** True when the reader pressed Save on this ayah. */
    marked: Boolean = false,
    onOpen: (IntRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bounds = remember { mutableStateOf(IntRect.Zero) }
    val press = remember { MutableInteractionSource() }
    val name = stringResource(R.string.reading_ayah_door, reference)
    val expanded = stringResource(UiKitR.string.door_expanded)
    val collapsed = stringResource(UiKitR.string.door_collapsed)
    val saved = stringResource(R.string.reading_door_saved)
    // The door belongs to the reading's line, but it is still a control.
    // It wears the app's one action color, so a number that answers a tap is
    // never mistaken for the page's ornament. Gold stays the page's ornament:
    // the surah's name, the ayah's medallion, and the juz label (owner
    // decision, the consistency audit).
    val ink = MaterialTheme.colorScheme.primary
    val fill = if (open) {
        ink.copy(alpha = Wash.Control)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
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
                    // The door says every state it is in, in the order the
                    // reader meets them: the keep, then whether the menu is
                    // open. A mark a reader cannot see is one they were never
                    // told about, and a bookmark is a color before it is a
                    // sentence (owner decision, this session).
                    stateDescription = buildList {
                        if (marked) add(saved)
                        add(if (open) expanded else collapsed)
                    }.joinToString(", ")
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .clip(Radii.Pill)
                    .background(fill)
                    .then(
                        if (open) {
                            Modifier.border(1.dp, ink, Radii.Pill)
                        } else {
                            Modifier
                        },
                    )
                    .focusRing(press, Radii.Pill)
                    .indication(press, ripple(color = ink, bounded = false))
                    .padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReferenceLabel(text = reference, color = ink)
                if (marked) {
                    IconGlyph(
                        icon = Icon.BookmarkFilled,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = Space.Tight)
                            .size(GlyphSize.ChevronInline)
                            // The mark is a drawing, not a control, so it
                            // carries no name of its own; the tag is how a
                            // test sees that the door of a saved ayah draws
                            // it (owner decision, this session).
                            .testTag("door-saved"),
                    )
                }
                AppChevron(
                    role = ChevronRole.Expand,
                    expanded = open,
                    tint = ink,
                    modifier = Modifier.padding(start = ControlMetrics.ChevronGap),
                )
            }
        }
    }
}
