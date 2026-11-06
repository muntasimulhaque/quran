package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconButton
import io.github.muntasimulhaque.quran.ui.theme.Space

/** The line a page head's back mark starts on: the sheet's words less the
 * air inside a round target. */
private val HeadStart = 6.dp

/** The head's own top air, so the back mark sits above the page's title. */
private val HeadTop = 2.dp

/**
 * The head of a page the reader opened: one way back, and the page's own
 * name in the caller's voice.
 *
 * The two page heads the app draws (a settings page and the surah grid) were
 * the same row copied twice: the same 6 dp lead, 2 dp top, 8 dp foot, the same
 * rotated chevron, and the same spoken name. The copy is the drift waiting to
 * happen. The row lives here now, and the caller supplies only what the page
 * is called and what stands beside the mark.
 *
 * The back mark is [IconButton]'s round control, with the same 48 dp target
 * and focus ring as every other mark. The content slot is a [RowScope], so a
 * page can give its title the remaining width, put a trailing Arabic name
 * beside it, or use [SheetTitle] for an app page.
 */
@Composable
fun PageHead(
    onBack: () -> Unit,
    backDescription: String,
    modifier: Modifier = Modifier,
    backModifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = HeadStart, end = Space.Gutter, top = HeadTop, bottom = Space.Line),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            icon = Icon.Chevron,
            description = backDescription,
            onClick = onBack,
            modifier = backModifier,
            tint = MaterialTheme.colorScheme.onSurface,
            rotation = 90f,
        )
        content()
    }
}
