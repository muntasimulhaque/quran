package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.AppChevron
import io.github.muntasimulhaque.quran.ui.kit.ChevronRole
import io.github.muntasimulhaque.quran.ui.kit.ReferenceLabel
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The ayah's own door: the number at the head of the verse, in the app's one
 * action color, the one turn that says it opens, and the reader's marks
 * beside them.
 *
 * **The verse is not a button** (owner decision, this session). A tap on the
 * Quran is the page's own tap: it brings or puts away the bar, and it never
 * chooses a verse. In 16.6 a tap on the verse raised its actions in the head
 * row, and because the page is almost all verse, the tap that was meant to
 * put the actions away landed on the next ayah and chose it instead. The
 * number is the one thing on the reading that opens an ayah's actions, and
 * the menu it opens is the app's one popup: the popup is a window of its own,
 * so a tap outside it can only put it away, never fall through to another
 * verse (owner decision, this session).
 *
 * **The door says it opens** (owner decision, this session). The number wore
 * the action color and nothing else, and a number in the accent is a caption
 * rather than a signifier: the head read as the verse's citation, so nothing
 * told the reader that it answers a tap. Every other door in the app ends in
 * the one turn the set draws, and the pill's reciter door, the closest
 * sibling the app has, is a line of small type with that same 14 dp chevron
 * standing 4 dp after it in the same ink. The reading's number wears the pair
 * now, so a reader who has learned the pill has already learned the ayah.
 *
 * It has no capsule. A shape around every number would turn the page into a
 * list of buttons, and the turn and the action color already say that a
 * number answers a tap. The state is the turn and the page's own wash: while
 * this ayah's menu is open the block wears the selected ground, so the door
 * has only to say which way it went. The visible line is the reference and
 * its turn; the 48 dp the finger needs is the row the head already keeps, so
 * the numbers still form one rail down the page and the card gap is
 * untouched.
 *
 * **The press is the app's pill, and it stands on the card's own line**
 * (owner decision, this session). The flash used to be the row's own bounds:
 * no air on the leading end, so the digits touched it, 4 dp above and below,
 * and the reader's marks stood inside it, which made one door three shapes. It
 * is the compact measure the pill's own doors wear now, 8 dp of air at each end
 * and above and below, the line plus 16 dp inside the 48 dp target, with the
 * ring on the same shape.
 *
 * The flash is also a control *on* the washed card, so its shape begins where
 * the card's content begins, one [Space.WashInset] inside the card's edge,
 * and the digits sit one [Space.WashInset] inside the flash. It used to reach
 * out to the card's own edge, so that the digits landed on the line the
 * translation keeps, and the two rounded shapes met along that edge: two blues
 * with one silhouette, which is the one thing a control over the reading must
 * not do (owner report, this session). Now the flash keeps the card's air on
 * every side, and the digits are a shape's content, inset inside it, the way
 * the word on every pill in the app is. The card's content line is the line
 * the flash's shape holds; the digits lead it by that shape's own inset.
 *
 * The turn is [ChevronRole.Expand], the pill's own mark for a panel that opens
 * in place, and it turns up while the menu is open, so a door that has been
 * answered goes on saying so. The mark is drawn and never spoken: a screen
 * reader has the door as one named 48 dp button, and the menu names itself
 * when it opens, so a state sentence on the door would be a stop that says
 * nothing.
 *
 * [onOpen] hands the door's own place on the glass to the caller, measured in
 * the window, because the menu hangs from the door and a popup cannot read
 * its anchor back through a test.
 */
@Composable
internal fun AyahDoor(
    reference: String,
    /** The verse's key in the app's own digits, for the test's tag. */
    key: String = reference,
    /** True when the reader pressed Save on this ayah. */
    marked: Boolean = false,
    /** True when the reader wrote a note on this ayah. */
    noted: Boolean = false,
    /** True while this ayah's menu is open, so its turn says which way. */
    open: Boolean = false,
    onOpen: (IntRect) -> Unit,
) {
    val bounds = remember { mutableStateOf(IntRect.Zero) }
    val press = remember { MutableInteractionSource() }
    // The door belongs to the reading's line, but it is still a control, so
    // it wears the app's one action color. Gold stays the page's ornament
    // (owner decision, the consistency audit).
    val ink = MaterialTheme.colorScheme.primary
    val name = stringResource(R.string.reading_door, reference)
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
            .testTag("door-$key")
            .clickable(
                interactionSource = press,
                indication = null,
                role = Role.Button,
            ) { onOpen(bounds.value) }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .clip(Radii.Pill)
                .focusRing(press, Radii.Pill)
                .indication(press, ripple(color = ink, bounded = false))
                .testTag("door-flash")
                .padding(horizontal = Space.WashInset, vertical = Space.Line),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReferenceLabel(
                text = reference,
                color = ink,
                modifier = Modifier.testTag("door-reference"),
            )
            AppChevron(
                role = ChevronRole.Expand,
                expanded = open,
                tint = ink,
                size = GlyphSize.ChevronInline,
                modifier = Modifier
                    // The gap the pill's own door keeps between its word and
                    // its turn, so the number and the mark read as one thing
                    // (owner decision, this session).
                    .padding(start = Space.Tight)
                    // The turn is a drawing, not a control, so it carries no
                    // name of its own; the tag is how a test sees that the
                    // door says it opens.
                    .testTag("door-chevron"),
            )
            if (marked) {
                IconGlyph(
                    icon = Icon.BookmarkFilled,
                    tint = ink,
                    modifier = Modifier
                        .padding(start = Space.Tight)
                        .size(GlyphSize.ChevronInline)
                        // The mark is a drawing, not a control, so it
                        // carries no name of its own; the tag is how a test
                        // sees that the door of a saved ayah draws it.
                        .testTag("door-saved"),
                )
            }
            if (noted) {
                IconGlyph(
                    icon = Icon.Note,
                    tint = ink,
                    modifier = Modifier
                        .padding(start = Space.Tight)
                        .size(GlyphSize.ChevronInline)
                        .testTag("door-noted"),
                )
            }
        }
    }
}
