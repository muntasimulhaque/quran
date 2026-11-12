package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The head of a sheet: the name it draws, and the one action it owns at the
 * trailing edge of that name.
 *
 * **The head has one line, and it is the name's own** (owner decision, this
 * session). A mark beside the name owes a finger 48 dp, a name owes it
 * nothing, and the line a sheet's place stands under owes the same air on
 * every sheet. So the head is measured by its name, and the action's target
 * paints and answers above and below that line instead of making the head
 * taller. A `Row` could not say this: its height is its tallest child's, which
 * is how the word meaning sheet's speaker made a 48 dp head under a 26 dp
 * title and pushed the place to 15 dp while the footnote's stood at 4, and how
 * the note sheet's Clear and Save did the same to its head (owner report, this
 * session).
 *
 * The action is a lambda rather than a component, because a sheet may own one
 * mark or two words: the word meaning sheet's speaker is one round control and
 * the note's Clear and Save are two buttons on one row. The name keeps
 * [Space.Block] from whatever the caller draws, so an action never runs into
 * the words that name the sheet.
 *
 * A sheet that draws only a name hands no action, and then the head is the
 * name alone. The name is [SheetTitle]'s own voice and its own heading, so a
 * reader who cannot see the sheet still finds it by jumping to its name.
 */
@Composable
fun SheetHead(
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    if (action == null) {
        SheetTitle(text = text, modifier = modifier.fillMaxWidth())
        return
    }
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            SheetTitle(text = text, modifier = Modifier)
            action()
        },
    ) { measurables, constraints ->
        // The action first, at its own size: a target is the one part of a head
        // that never shrinks, and its width is what the name may not use.
        val mark = measurables[1].measure(Constraints())
        val gap = Space.Block.roundToPx()
        val titleWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - mark.width - gap).coerceAtLeast(0)
        } else {
            Constraints.Infinity
        }
        val title = measurables[0].measure(Constraints(maxWidth = titleWidth))
        layout(width = constraints.maxWidth, height = title.height) {
            title.place(0, 0)
            // Centered on the name's line, so the mark reads as standing beside
            // the name even though its target reaches past the line's air.
            mark.place(
                x = constraints.maxWidth - mark.width,
                y = (title.height - mark.height) / 2,
            )
        }
    }
}

/**
 * The top room an action head needs inside a sheet's scroll, for a name line
 * of [titleLine].
 *
 * The action's target is [GlyphTarget] tall and is centered on the name's
 * line, so it reaches half the difference above the head. A scroll clips at
 * its first pixel; the caller gives its content this much top inset, and the
 * target's whole height stays inside the scroll. The place under the name
 * keeps [Space.Tight], because the inset moves the whole head and never
 * pushes the place away from its name.
 *
 * The number follows the reader's type scale: 11 dp at the default 26 dp
 * line, 4.5 dp at 39, and zero once the name's line reaches the target. The
 * head itself is never measured at the target's height; the air is above the
 * name, not under it (owner decision, this session).
 */
fun sheetHeadInsetFor(titleLine: Dp): Dp =
    ((GlyphTarget - titleLine) / 2f).coerceAtLeast(0.dp)

/**
 * The top room an action head needs at the reader's current type scale, for
 * a caller that places [SheetHead] inside a scrolling sheet.
 *
 * A caller with no action passes no inset. The word meaning sheet's speaker
 * and the note sheet's Clear and Save are the two callers; a sheet with no
 * action has no target above its line to make room for.
 */
@Composable
fun sheetHeadActionInset(): Dp {
    val line = MaterialTheme.typography.titleLarge.lineHeight
    return with(LocalDensity.current) { sheetHeadInsetFor(line.toDp()) }
}
