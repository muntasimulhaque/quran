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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.kit.AppPopup
import io.github.muntasimulhaque.quran.ui.kit.MenuActionRow
import io.github.muntasimulhaque.quran.ui.kit.PopupAlignment
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * The one row's menu a chip raises: the width a single short word needs, not
 * the panel's 288 and not the ayah menu's 216. The menu family keeps its shape
 * and its row rhythm; only the measure follows the content (owner decision:
 * one menu family, not one measure).
 *
 * It is 160, not 176: the row's own leading rail, its mark, its word, and the
 * popup's gutters need about 154 dp, and the extra 22 dp read as an empty
 * right side under a six letter word (owner report, this session).
 */
private val RecentChipMenu = 160.dp

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
 * press raises the app's one popup with one row, Remove, and the query leaves
 * the list and the phone. Clear stays: the two answer two questions, this
 * word and all of them, and clearing a whole session should not be five long
 * presses. The long press is not the only road, because a gesture that has to
 * be taught is not a signifier: the chip also carries a spoken Remove action,
 * so a reader using TalkBack removes it without the gesture.
 *
 * The row wears the quiet ink, because the app never gives a removal the
 * loudest word on a surface (owner report: Remove was brighter than the Clear
 * beside it). The mark stays the app's X, which is the same 'take this text
 * away' the field's clear wears.
 *
 * The menu takes the chip's middle. A chip is a capsule, and [AppPopup]'s own
 * rule answers a capsule with [PopupAlignment.Center]; the Start alignment is
 * the door's, and the chip is not a door. Start put the menu's left edge on
 * the chip's left edge and let it run off to the right, so a wider menu no
 * longer read as belonging to the word that raised it (owner report, this
 * session). A chip near the leading edge still clamps to the window's gutter,
 * which is the window's rule and not a second alignment.
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
            // The chip is a 36 dp pill inside a 48 dp target, so the row gap
            // the eye reads is this spacing plus the 6 dp of target above and
            // below each pill; 0 here leaves a 12 dp block break between rows
            // while the chips keep the size the search filters already have.
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
 * One recent query: a chip that fills the field on a tap, and that says how to
 * remove itself on a long press or through a screen reader's own action.
 *
 * The popup hangs from the chip's own place on the glass, measured here
 * because a popup cannot read its anchor back through a test, and it opens
 * with the query as its spoken name: a reader who cannot see the chip hears
 * which word the one row is about to remove.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentChip(
    label: String,
    onQuery: () -> Unit,
    onRemove: () -> Unit,
) {
    val press = remember { MutableInteractionSource() }
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val remove = stringResource(R.string.search_recent_remove)
    val bounds = remember { mutableStateOf(IntRect.Zero) }
    var anchor by remember { mutableStateOf<IntRect?>(null) }
    Row(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val box = coordinates.boundsInWindow()
                bounds.value = IntRect(
                    box.left.toInt(),
                    box.top.toInt(),
                    box.right.toInt(),
                    box.bottom.toInt(),
                )
            }
            .combinedClickable(
                interactionSource = press,
                indication = null,
                role = Role.Button,
                onClick = onQuery,
                onLongClickLabel = remove,
                onLongClick = { anchor = bounds.value },
            )
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(remove) {
                        onRemove()
                        true
                    },
                )
            }
            .minimumInteractiveComponentSize()
            .clip(Radii.Pill)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door))
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
    }
    anchor?.let { at ->
        AppPopup(
            anchor = at,
            onDismiss = { anchor = null },
            width = RecentChipMenu,
            alignment = PopupAlignment.Center,
            name = label,
        ) {
            MenuActionRow(
                icon = Icon.Close,
                label = remove,
                // The one removing word in a menu: the quiet ink, as Clear
                // beside it and every other removal in the app (owner
                // decision, this version).
                quiet = true,
                onClick = {
                    anchor = null
                    onRemove()
                },
            )
        }
    }
}
