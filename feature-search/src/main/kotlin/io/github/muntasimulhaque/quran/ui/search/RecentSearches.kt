package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.ChipMeasure
import io.github.muntasimulhaque.quran.ui.kit.chipMeasure
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * The queries the reader acted on before, as chips over the empty sheet.
 *
 * A search usually begins with a word the reader has already used, and the
 * empty sheet is the one place that can hand it back. The chips are the app's
 * own chip, so a query never reads as the field's own text or as the app's
 * one action, and the head names the block. A query is only here because the
 * reader opened something it found, which is why the list is short and useful
 * instead of a record of every keystroke (owner decision, this session).
 *
 * **The X is the only removal** (owner decision, this session). Clear all is
 * gone: the list holds at most five words, every one wears its own 48 dp
 * Remove, and a second, bulk destruction beside that precise one asked the
 * reader to choose the same thing twice. The history never leaves the phone,
 * so there is no privacy reason for a bulk wipe either.
 *
 * **A chip goes on its own with the X it always shows** (owner decision, this
 * session). The X is a real 48 dp target at the chip's trailing end, always
 * visible and quiet at rest, so a reader who wants to forget one word does
 * not have to discover a long press. The query and the X are two targets in
 * one capsule: the query's half searches, the X's half removes, and each
 * half's press is drawn inside the capsule, so the shape the eye reads is
 * still the shape the finger presses.
 *
 * **A chip is as wide as its word, never the row** (owner decision, this
 * session). The word's half asks for the width it needs and the X keeps its
 * own 48 dp, so a short query is a short capsule and two of them stand on
 * one line. The chip used to fill the row because the word's half asked for
 * the width it was allowed instead of the width it needed, which made every
 * query a bar of its own and turned the list into a column.
 *
 * **The capsule is the label's line, not a fixed box** (owner decision, this
 * session). It is `labelLarge`'s own line plus [Space.Line] above and below,
 * the measure [ChipMeasure] gives every chip on this sheet, so it is 36 dp at
 * the default and grows with the system font scale instead of clipping a 20 sp
 * line inside a fixed 36 dp box at 200 percent. The target is at least 48 dp
 * and grows with the capsule, so the finger never gets less than the app's
 * floor, and the X's press circle is the default capsule's 36 dp or the
 * capsule itself when the reader's text is smaller.
 *
 * The X's target is 48 dp and its mark is 18 dp: the field's clear mark is
 * 24 dp inside a 48 dp field, half the field's height, so the chip's mark is
 * half the default capsule's 36 dp instead of filling two thirds of it and
 * reading as loud as the query (owner decision, this session). At a larger
 * system scale the mark stays 18 dp while the capsule and the target grow
 * around it, because the mark is a fixed drawing and the word beside it is
 * the thing that scales. The query is the thing the reader came back for, so
 * its word takes the row's own ink and the X stays in the quiet tone. Its
 * spoken name is "Remove <query>", so a screen reader reaches the one word
 * without a custom action and without the long press the chip used to hide.
 *
 * **The head belongs to its chips** (owner decision, this session). It stands
 * [Space.Line] below the filter door and [Space.Line] above the chips. The
 * door's 48 dp target already spends 14 dp under its words, so a Section on
 * top of that counted the target's air twice and left the caption twice as
 * far from the door as the door is from the field. The note below keeps its
 * Section, because the note is a block and the chips are the head's own list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecentSearches(
    queries: List<String>,
    onQuery: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val chip = chipMeasure()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Line),
    ) {
        SectionHead(
            text = stringResource(R.string.search_recent_title),
            modifier = Modifier.padding(bottom = Space.Line),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.Line),
            // The capsules stand Space.Row apart, or the target's air where
            // that is more: the measure's gap is what the target does not
            // already leave between two rows.
            verticalArrangement = Arrangement.spacedBy(chip.rowGap),
        ) {
            queries.forEach { query ->
                RecentChip(
                    label = query,
                    measure = chip,
                    onQuery = { onQuery(query) },
                    onRemove = { onRemove(query) },
                )
            }
        }
    }
}

/**
 * One recent query: a chip whose word searches and whose X removes.
 *
 * The outer box is the target of both halves, at least 48 dp tall; the
 * capsule behind them is the label's line plus [Space.Line] above and below,
 * and it ends where the two halves end. The word's half wraps its own text and
 * keeps 8 dp of air after it, so the word's target owns the space after its
 * last letter and the X's target, the one that removes, begins 8 dp further
 * from the word (owner decision, this session). The chip's width is still the
 * word's plus the X's 48 dp and never the row's (owner decision, this
 * session). The word's ripple is the pill's left half, and the X's ripple is
 * the circle at its end, so no press paints the invisible target. The X is
 * always drawn, never armed: the old long press was a gesture that had to be
 * taught, and a chip that hides its own removal is a chip that hides a door.
 */
@Composable
private fun RecentChip(
    label: String,
    measure: ChipMeasure,
    onQuery: () -> Unit,
    onRemove: () -> Unit,
) {
    val queryPress = remember { MutableInteractionSource() }
    val removePress = remember { MutableInteractionSource() }
    val queryInk = MaterialTheme.colorScheme.onSurface
    val markInk = MaterialTheme.colorScheme.onSurfaceVariant
    val remove = stringResource(R.string.search_recent_remove_named, label)
    val leftHalf = RoundedCornerShape(
        topStart = measure.radius,
        bottomStart = measure.radius,
        topEnd = 0.dp,
        bottomEnd = 0.dp,
    )
    BoxWithConstraints(
        modifier = Modifier
            .height(measure.target)
            .testTag("recent-chip"),
    ) {
        // The X's 48 dp comes out of the room before the word is measured,
        // so a long query ellipsizes instead of pushing the removal off the
        // glass, and the word takes only what it needs of what is left: the
        // chip's width is the word's, not the row's (owner decision, this
        // session).
        val textMax = (maxWidth - 48.dp).coerceAtLeast(48.dp)
        // The capsule the reader sees: the label's line plus 8 dp above and
        // below, centered in the target. matchParentSize does not influence
        // the chip's width; the Row below is what decides it.
        Box(Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(measure.pill)
                    .clip(Radii.Pill)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)),
            )
        }
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = textMax)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = queryPress,
                        indication = null,
                        role = Role.Button,
                        onClick = onQuery,
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        // No fill of its own: the capsule behind both halves
                        // is the shape, and the word's own width is the
                        // chip's. The 48 dp floor is the app's: a two-letter
                        // query still owes the finger a target, so the word's
                        // pill stops at 48 and grows with the word above it
                        // (owner decision, this session).
                        .defaultMinSize(minWidth = 48.dp)
                        .height(measure.pill)
                        .clip(leftHalf)
                        .focusRing(queryPress, leftHalf)
                        .indication(queryPress, ripple(color = queryInk))
                        .padding(start = Space.Row, end = Space.Line),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = queryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = removePress,
                        indication = null,
                        role = Role.Button,
                        onClick = onRemove,
                    )
                    .semantics { contentDescription = remove },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(measure.press)
                        .clip(CircleShape)
                        .focusRing(removePress, CircleShape)
                        .indication(removePress, ripple(color = markInk)),
                    contentAlignment = Alignment.Center,
                ) {
                    IconGlyph(
                        icon = Icon.Close,
                        tint = markInk,
                        modifier = Modifier.size(GlyphSize.ChipRemove),
                    )
                }
            }
        }
    }
}
