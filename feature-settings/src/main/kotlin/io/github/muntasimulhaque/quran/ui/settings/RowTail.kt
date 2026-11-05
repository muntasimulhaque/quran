package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.kit.SheetTitle
import io.github.muntasimulhaque.quran.ui.reader.IconButton
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Space

/** The head of a page the reader opened: one way back, one name. */
@Composable
fun PageHeader(title: String, onBack: () -> Unit) {
    val back = stringResource(R.string.settings_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = Space.Gutter, top = 2.dp, bottom = Space.Line),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // One back button for every page head the app draws: the same 48 dp,
        // the same rotated chevron, the same spoken name. It used to be a
        // hand-rolled box here and a second copy on the surah grid (owner
        // decision, the theme and consistency session).
        IconButton(
            icon = Icon.Chevron,
            description = back,
            onClick = onBack,
            tint = MaterialTheme.colorScheme.onSurface,
            rotation = 90f,
        )
        SheetTitle(
            text = title,
            // The page's own name, in the app's one title voice; the air
            // beside the back mark belongs to this head.
            modifier = Modifier.padding(start = Space.Tight),
        )
    }
}

/**
 * The mark a settings row ends with: the chevron, when the row opens a page.
 *
 * The chevron is a picture of where a tap goes and not a control of its own.
 * The whole row is the door, so the row is one stop to a screen reader and the
 * mark takes no name. A row that also carried a switch used to read as a pair
 * of marks; the hub carries no switch now, so a row's last mark is either the
 * chevron or the switch, never both (owner decision, this session).
 *
 * The slot is 48 dp wide and a width and not a height: a square tail is taller
 * than the line of text beside it, and a Row takes the height of its tallest
 * child, so a square stretched every plain row and turned the hub into a
 * ladder (owner report). The row owes the finger its 48 dp from its own target.
 *
 * The mark stands at the end of its slot, so its right edge is the row's own
 * margin: that is exactly where a switch's right edge stands in the row below
 * it, and the two kinds of tail keep one line down the sheet.
 */
@Composable
internal fun RowTail(chevron: Boolean) {
    if (!chevron) return
    Box(
        modifier = Modifier
            .width(48.dp)
            // The slot, not the mark: a mark is centred in its slot, so the
            // slot's edges are what a row's alignment is about. The tag is on
            // a row that draws a chevron only, so a count of them is a count
            // of the rows that have a door (owner report).
            .testTag("chevron-slot"),
        contentAlignment = Alignment.CenterEnd,
    ) {
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(GlyphSize.ChevronRow)
                // The mark says where a tap goes: the chevron points the way
                // the page slides in, to the right.
                .rotate(-90f)
                .testTag("row-chevron"),
        )
    }
}
