package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph

/**
 * What a chevron means, so its angle is a decision and never a number at the
 * call site.
 *
 * [Expand] is the one turn that changes: down while the content is hidden,
 * up once it is shown. [Next] points the way a page goes, to the right in a
 * left-to-right interface, and [Back] points the way it came. A row that
 * opens another page must not borrow [Expand]'s down arrow (owner decision,
 * the consistency audit: the search results did).
 */
enum class ChevronRole { Expand, Back, Next }

/**
 * The app's one chevron, drawn at the row's measure and turned by its job.
 *
 * The mark is [GlyphSize.ChevronRow] at a row, and [GlyphSize.ChevronInline]
 * where a door stands beside a word inside a compact control, which is the
 * pill's three doors; the control decides the measure, the tint belongs to the
 * row that draws it, and the turn is [chevronTurn]'s. The raw angles that used
 * to stand at the call sites are gone, so a new row cannot invent a fifth
 * direction. The pill's doors kept their own raw chevrons past that sweep
 * because the pill's mark is the compact 14 dp and this composable could only
 * draw the row's 18 (owner decision, this session: the turn, and the state
 * said out loud).
 */
@Composable
fun AppChevron(
    role: ChevronRole,
    tint: Color,
    modifier: Modifier = Modifier,
    /** For [ChevronRole.Expand]: true once the content is shown. */
    expanded: Boolean = false,
    /** The mark's measure: the row's, or the compact control's. */
    size: Dp = GlyphSize.ChevronRow,
) {
    IconGlyph(
        icon = Icon.Chevron,
        tint = tint,
        modifier = modifier
            .size(size)
            .rotate(chevronTurn(role, expanded)),
    )
}

/**
 * Where a chevron stands, as arithmetic: the one rule the drawing and the
 * test read, so a mark's direction is never a number at a call site, and a
 * door that opens a popup says so on its own mark.
 */
internal fun chevronTurn(role: ChevronRole, expanded: Boolean): Float = when (role) {
    ChevronRole.Expand -> if (expanded) 180f else 0f
    ChevronRole.Back -> 90f
    ChevronRole.Next -> -90f
}
