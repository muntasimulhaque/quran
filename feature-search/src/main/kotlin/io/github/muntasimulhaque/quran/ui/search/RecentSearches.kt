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
import io.github.muntasimulhaque.quran.ui.kit.TextButton
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
 * one action; the head names the block, and Clear empties the list on the
 * phone as well. A query is only here because the reader opened something it
 * found, which is why the list is short and useful instead of a record of
 * every keystroke (owner decision, this session).
 *
 * **A chip goes on its own with the X it always shows** (owner decision, this
 * session). The X is a real 48 dp target at the chip's trailing end, always
 * visible and quiet at rest, so a reader who wants to forget one word does
 * not have to discover a long press. The query and the X are two targets in
 * one capsule: the query's half searches, the X's half removes. The capsule
 * stays the chip's own 36 dp, and each half's press is drawn inside it, so
 * the shape the eye reads is still the shape the finger presses. Clear stays
 * for the whole list: the two answer two questions, this word and all of
 * them.
 *
 * The X's target is 48 dp and its mark is 24 dp, the same mark the search
 * field's clear wears. Its spoken name is "Remove <query>", so a screen
 * reader reaches the one word without a custom action and without the long
 * press the chip used to hide.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecentSearches(
    queries: List<String>,
    onQuery: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Section),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHead(
                text = stringResource(R.string.search_recent_title),
                modifier = Modifier.weight(1f),
            )
            TextButton(
                label = stringResource(R.string.search_recent_clear),
                onClick = onClear,
                quiet = true,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.Line),
            // A chip's target is 48 dp and its visible capsule is 36 dp, so
            // the 12 dp of air the row reads is the target's own six on each
            // side. The next row's target starts where this one's ends.
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            queries.forEach { query ->
                RecentChip(
                    label = query,
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
 * The outer box is the 48 dp target of both halves; the capsule behind them
 * is the 36 dp shape the reader sees. The word's ripple is the pill's left
 * half, and the X's ripple is the 36 dp circle at its end, so no press paints
 * the invisible target. The X is always drawn, never armed: the old long
 * press was a gesture that had to be taught, and a chip that hides its own
 * removal is a chip that hides a door.
 */
@Composable
private fun RecentChip(
    label: String,
    onQuery: () -> Unit,
    onRemove: () -> Unit,
) {
    val queryPress = remember { MutableInteractionSource() }
    val removePress = remember { MutableInteractionSource() }
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val remove = stringResource(R.string.search_recent_remove_named, label)
    val leftHalf = RoundedCornerShape(
        topStart = ChipRadius,
        bottomStart = ChipRadius,
        topEnd = 0.dp,
        bottomEnd = 0.dp,
    )
    BoxWithConstraints(
        modifier = Modifier
            .height(48.dp)
            .testTag("recent-chip"),
    ) {
        // The word keeps the room the screen has, and gives 48 dp to the X:
        // a long query ellipsizes instead of pushing the removal off the
        // glass.
        val textMax = (maxWidth - 48.dp).coerceAtLeast(48.dp)
        // The capsule the reader sees: 36 dp, centered in the 48 dp target.
        // matchParentSize does not influence the chip's width; the Row below
        // is what decides it.
        Box(Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(36.dp)
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
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(leftHalf)
                        .focusRing(queryPress, leftHalf)
                        .indication(queryPress, ripple(color = ink))
                        .padding(start = Space.Row, end = Space.Line),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = ink,
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .focusRing(removePress, CircleShape)
                        .indication(removePress, ripple(color = ink)),
                    contentAlignment = Alignment.Center,
                ) {
                    IconGlyph(
                        icon = Icon.Close,
                        tint = ink,
                        modifier = Modifier.size(GlyphSize.Control),
                    )
                }
            }
        }
    }
}

/** The capsule's own radius, so the word's half meets the X's circle. */
private val ChipRadius = 18.dp
