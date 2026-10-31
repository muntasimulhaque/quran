package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.SearchSources
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * The sources a query reads, folded away behind one quiet door.
 *
 * The six chips were a wall over the results on a phone, and a reader who
 * ignores them searches everything, which is what the app means by search.
 * The door says what the search reads in two words, and opens the same chips
 * for the reader who wants to narrow it. The fold is remembered for the
 * sheet's own life, so a reader who opened it once does not open it twice,
 * and it folds itself away when a query begins: once the reader is
 * searching, the results are the page, and the door is one tap away when
 * they want to narrow.
 *
 * The chips wrap instead of scrolling sideways. A row that runs off the edge
 * hides its own contents: a reader cannot turn off a source they cannot see.
 * A source the reader does not have is not offered, rather than offered and
 * empty.
 */
@Composable
internal fun FilterRow(
    sources: SearchSources,
    /** True while the reader's input is a real query. */
    hasQuery: Boolean,
    hasTranslations: Boolean,
    hasTafsirs: Boolean,
    hasWords: Boolean,
    onChange: (SearchSources) -> Unit,
) {
    // A query folds the source list away: once the reader is searching,
    // the results are the page, and the door stays one tap away for the
    // reader who wants to narrow. Keyed on the query becoming real,
    // not on every keystroke, so a reader who deliberately opened the
    // list while a query is live can still refine it (owner report).
    var open by remember(hasQuery) { mutableStateOf(false) }
    // A source the reader does not have is not offered, rather than offered
    // and empty. The word meanings are their own source: the chip follows the
    // word list being on the device, never the translations being on, because
    // a reader may read a translation and keep the meanings off, or keep the
    // meanings on with no translation at all (owner decision, this session).
    val offered = buildList {
        add(sources.text)
        add(sources.surahs)
        add(sources.references)
        if (hasTranslations) add(sources.translations)
        if (hasWords) add(sources.words)
        if (hasTafsirs) add(sources.tafsirs)
    }
    val chosen = offered.count { it }
    val state = if (chosen == offered.size) {
        stringResource(R.string.search_filter_all)
    } else {
        stringResource(R.string.search_filter_some, chosen, offered.size)
    }
    val expanded = stringResource(R.string.search_filter_state_expanded)
    val collapsed = stringResource(R.string.search_filter_state_collapsed)
    val press = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Line, bottom = Space.Tight),
    ) {
        // The header is a door, drawn the way the app draws every status line
        // that opens something: the words, the state, and a chevron that
        // turns to say which way the list will move, with no fill, so it is
        // read as a door and never as a chip that happens to be off. The
        // whole row is the target, and its own gap keeps the words off the
        // ripple (owner decision, this session).
        Row(
            modifier = Modifier
                // The door is built the way every other shape in the app is:
                // the click owns the 48 dp target, and the fill and the ripple
                // are drawn on the pill inside it, the chip's own 8 dp around
                // the label's 16 dp line. Left the other way round the clip
                // wrapped the whole target, so the highlight read as a 48 dp
                // slab over a chip-sized row (owner report, this session).
                .clickable(
                    interactionSource = press,
                    indication = null,
                    role = Role.Button,
                    onClick = { open = !open },
                )
                .minimumInteractiveComponentSize()
                .clip(Radii.Pill)
                .indication(press, ripple(color = MaterialTheme.colorScheme.onSurfaceVariant))
                .semantics {
                    stateDescription = if (open) expanded else collapsed
                }
                .padding(horizontal = Space.Row, vertical = Space.Line),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.search_filter),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = state,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Space.Line),
            )
            // The arrow sits with the words it belongs to, and turns to say
            // which way the list will move: down while it is hidden, up once
            // it is open.
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(GlyphSize.ChevronRow)
                    .rotate(if (open) 180f else 0f),
            )
        }
        AnimatedVisibility(visible = open) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Line),
                horizontalArrangement = Arrangement.spacedBy(Space.Line),
                // A chip is a 36 dp pill inside a 48 dp target, so the row gap
                // the eye reads is this spacing plus the 6 dp of target above
                // and below each pill. The flow's own 8 dp made that gap 24 dp
                // against the columns' 8 dp (owner report); 0 here leaves a
                // 12 dp block break between rows while the chips keep the
                // 36 dp size the ladder gives them.
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                FilterChip(stringResource(R.string.search_filter_text), sources.text) {
                    onChange(sources.copy(text = it))
                }
                FilterChip(stringResource(R.string.search_filter_surahs), sources.surahs) {
                    onChange(sources.copy(surahs = it))
                }
                FilterChip(stringResource(R.string.search_filter_references), sources.references) {
                    onChange(sources.copy(references = it))
                }
                if (hasTranslations) {
                    FilterChip(stringResource(R.string.search_filter_translations), sources.translations) {
                        onChange(sources.copy(translations = it))
                    }
                }
                if (hasWords) {
                    FilterChip(stringResource(R.string.search_filter_words), sources.words) {
                        onChange(sources.copy(words = it))
                    }
                }
                if (hasTafsirs) {
                    FilterChip(stringResource(R.string.search_filter_tafsirs), sources.tafsirs) {
                        onChange(sources.copy(tafsirs = it))
                    }
                }
            }
        }
    }
}

/**
 * One source, on or off, as a chip the reader taps. A chosen chip carries the
 * check the app uses for a mark, so its state is read at a glance instead of
 * being guessed from the fill alone; the whole chip is one checkbox to
 * TalkBack, which says whether it is on.
 */
@Composable
internal fun FilterChip(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    // A chip is a small word around a small mark, and it is the same chip the
    // app draws everywhere: the pill is the label's own 20 dp line with 8 dp
    // above and below, so it reads as a chip and not as a slab, and it follows
    // the system font scale because it is the type plus padding and not a
    // fixed box (owner decision, this session: the pill had grown to 48 dp,
    // the size of the whole touch target, and a chip is not a target's size).
    // The click and the node a screen reader focuses still carry the app's
    // 48 dp target, so the source a reader cannot hit is not a source they can
    // turn off (owner decision, the accessibility test): the minimum wraps
    // the pill, the finger gets 48 dp, and the eye gets the chip. The gap the
    // flow leaves between two chips is the gap the eye reads: a chip's target
    // is the pill's own width across, so the flow's own Space.Line is what
    // stands between two chips, the app's cluster gap, which the standalone
    // action pairs do not share (owner decision, this session). The ripple is
    // drawn on the pill inside the click node, not on the node itself: an
    // indication left on the click would flash a rectangle over the shape the
    // reader is looking at.
    val press = remember { MutableInteractionSource() }
    val ink = if (on) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .toggleable(
                value = on,
                role = Role.Checkbox,
                interactionSource = press,
                indication = null,
                onValueChange = onChange,
            )
            .minimumInteractiveComponentSize()
            .clip(Radii.Pill)
            .background(
                if (on) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)
                },
            )
            .indication(press, ripple(color = ink))
            .padding(horizontal = Space.Row, vertical = Space.Line),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (on) {
            IconGlyph(
                icon = Icon.Check,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(GlyphSize.Check),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = ink,
        )
    }
}

/**
 * A recent query is a suggestion, not an action: it wears the app's chip, not
 * the action pill, because tapping it fills the field and runs the search
 * rather than completing a flow. It is the same chip as the source filters,
 * without the check, so the two kinds of search chip read as one family.
 */
@Composable
internal fun QueryChip(label: String, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = press,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
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
}
