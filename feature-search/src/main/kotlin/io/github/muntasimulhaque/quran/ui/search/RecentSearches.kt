package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.TextButton
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
 * **A chip can also go on its own** (owner decision, this session). A long
 * press arms the chip, and the app's X appears at its trailing end, inside the
 * same capsule: the one shape that says "take this text away" everywhere else.
 * The armed chip is then the removing control in whole, so the X is a mark and
 * not a second target inside a 36 dp chip, and the reader's finger keeps the
 * app's 48 dp floor. The query leaves the list and the phone on that tap.
 * Clear stays: the two answer two questions, this word and all of them, and
 * clearing a whole session should not be five long presses.
 *
 * The long press is not the only road, because a gesture that has to be taught
 * is not a signifier: the chip also carries a spoken Remove action, so a
 * reader using TalkBack removes it without the gesture.
 *
 * The row wears the quiet ink, because the app never gives a removal the
 * loudest word on a surface (owner report: Remove was brighter than the Clear
 * beside it). The mark stays the app's X, which is the same 'take this text
 * away' the field's clear wears. The armed ground steps from [Wash.Door] to
 * [Wash.Selected], the wash the app gives a chosen thing, so the state is
 * read before the mark is.
 *
 * One chip is armed at a time. Tapping another chip searches for it and lays
 * the first one down; tapping the armed chip removes it; a long press on it
 * again lays it down. The armed chip grows by the X's width, which is the
 * deliberate cost of a mark inside the capsule rather than a badge crossing
 * its edge (owner decision, this session).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecentSearches(
    queries: List<String>,
    onQuery: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    var armed by remember { mutableStateOf<String?>(null) }
    // A word that has left the list cannot stay armed. The list is read from
    // the phone when the sheet opens and written as a result is opened, so a
    // clear or a new search can rebuild it under the state.
    LaunchedEffect(queries) {
        if (armed != null && armed !in queries) armed = null
    }
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
            // The chip is a 36 dp pill inside a 48 dp target, so the row gap
            // the eye reads is this spacing plus the 6 dp of target above and
            // below each pill; 0 here leaves a 12 dp block break between rows
            // while the chips keep the size the search filters already have.
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            queries.forEach { query ->
                RecentChip(
                    label = query,
                    armed = armed == query,
                    onQuery = {
                        armed = null
                        onQuery(query)
                    },
                    onArm = { armed = if (armed == query) null else query },
                    onRemove = {
                        armed = null
                        onRemove(query)
                    },
                )
            }
        }
    }
}

/**
 * One recent query: a chip that fills the field on a tap, and that turns into
 * its own removing control on a long press.
 *
 * The whole chip is the target in both states, so the X is a mark and not a
 * second, smaller control inside a 36 dp capsule. The chip keeps the app's
 * 48 dp target, and the mark is the same [Icon.Close] the field's clear wears
 * (owner decision, this session).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentChip(
    label: String,
    armed: Boolean,
    onQuery: () -> Unit,
    onArm: () -> Unit,
    onRemove: () -> Unit,
) {
    val press = remember { MutableInteractionSource() }
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val remove = stringResource(R.string.search_recent_remove)
    Row(
        modifier = Modifier
            .combinedClickable(
                interactionSource = press,
                indication = null,
                role = Role.Button,
                onClickLabel = if (armed) remove else null,
                onLongClickLabel = remove,
                onLongClick = onArm,
                onClick = if (armed) onRemove else onQuery,
            )
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(remove) {
                        onRemove()
                        true
                    },
                )
            }
            .testTag(if (armed) "recent-armed" else "recent-chip")
            .minimumInteractiveComponentSize()
            .clip(Radii.Pill)
            .background(
                if (armed) {
                    MaterialTheme.colorScheme.primary.copy(alpha = Wash.Selected)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)
                },
            )
            .indication(press, ripple(color = ink))
            .padding(horizontal = Space.Row, vertical = Space.Line),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (armed) {
            IconGlyph(
                icon = Icon.Close,
                tint = ink,
                modifier = Modifier
                    .padding(start = Space.Line)
                    .size(GlyphSize.Action),
            )
        }
    }
}
