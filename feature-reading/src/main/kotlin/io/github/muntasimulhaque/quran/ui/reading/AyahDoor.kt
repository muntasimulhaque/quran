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
import io.github.muntasimulhaque.quran.ui.kit.ReferenceLabel
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The ayah's own door: the number at the head of the verse, in the app's one
 * action color, with the reader's marks beside it.
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
 * It has no capsule and no caret. A shape around every number would turn the
 * page into a list of buttons, and the action color already says that a
 * number answers a tap. The visible line is the reference alone; the 48 dp
 * the finger needs is the row the head already keeps, so the numbers still
 * form one rail down the page and the card gap is untouched.
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
                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReferenceLabel(text = reference, color = ink)
            if (marked) {
                IconGlyph(
                    icon = Icon.BookmarkFilled,
                    tint = ink,
                    modifier = Modifier
                        .padding(start = Space.Tight)
                        .size(GlyphSize.ChevronInline)
                        // The mark is a drawing, not a control, so it carries
                        // no name of its own; the tag is how a test sees that
                        // the door of a saved ayah draws it.
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
