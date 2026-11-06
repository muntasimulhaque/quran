package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
 * The mark is [GlyphSize.ChevronRow] at every caller; the tint belongs to the
 * row that draws it. The four raw angles that used to stand at the call sites
 * are gone, so a new row cannot invent a fifth direction.
 */
@Composable
fun AppChevron(
    role: ChevronRole,
    tint: Color,
    modifier: Modifier = Modifier,
    /** For [ChevronRole.Expand]: true once the content is shown. */
    expanded: Boolean = false,
) {
    val turn = when (role) {
        ChevronRole.Expand -> if (expanded) 180f else 0f
        ChevronRole.Back -> 90f
        ChevronRole.Next -> -90f
    }
    IconGlyph(
        icon = Icon.Chevron,
        tint = tint,
        modifier = modifier
            .size(GlyphSize.ChevronRow)
            .rotate(turn),
    )
}
