package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.data.SearchSources
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.AppChip
import io.github.muntasimulhaque.quran.ui.kit.DisclosurePress
import io.github.muntasimulhaque.quran.ui.kit.DisclosureRow
import io.github.muntasimulhaque.quran.ui.kit.ChipKind
import io.github.muntasimulhaque.quran.ui.kit.ChipStyle
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.chipMeasure
import io.github.muntasimulhaque.quran.ui.kit.rememberReducedMotion
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * One source the reader's search can read, with the words it wears and the
 * way its chip changes the set. The list is built once, so the count on the
 * door, the name of the one source that is off, and the chips cannot
 * disagree.
 */
private data class OfferedSource(
    val label: String,
    val on: Boolean,
    val set: (Boolean) -> SearchSources,
)

/**
 * The sources a query reads, folded away behind one quiet door.
 *
 * The chips were a wall over the results on a phone, and a reader who
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
 *
 * **A chosen chip is a tint, not a button** (owner decision, this session).
 * Seven sources start on, and seven solid pills over the results read as
 * seven pressed buttons: the accent became the page's background instead of
 * the one chosen thing. A chosen chip now wears the ayah door's own open
 * state, the accent at `Wash.Control` with the accent's ink, so a set of
 * chosen sources reads as what the reader turned on and the solid accent
 * stays for the one place and the one answer. The check stays beside the
 * word, so the state survives a color the reader cannot see.
 *
 * **The door tells the truth when one source is off.** "6 of 7" is a count,
 * and a reader whose one missing source is the reason a query found nothing
 * cannot read which one from it. One source off is named ("Everything but
 * Translations"); two or more keep the count. A source the reader does not
 * have is not counted.
 *
 * **A search with no place to look opens the list.** The last source turned
 * off leaves the status line asking for a source, so the door opens itself
 * and the reader is not told to choose from a list that is shut. A quiet
 * "Turn everything on" stands under the chips whenever something is off, so
 * the reader is never left turning seven chips back on by hand.
 *
 * The words of the door stand on the page's own gutter, the line the chips
 * and every other block keep; the press shape hangs 12 dp to their left so
 * the ripple has its room without moving the words off the rail (owner
 * decision, this session).
 */
@Composable
internal fun FilterRow(
    sources: SearchSources,
    /** True while the reader's input is a real query. */
    hasQuery: Boolean,
    hasTranslations: Boolean,
    hasTafsirs: Boolean,
    hasWords: Boolean,
    /** True while the reader has at least one note to search. */
    hasNotes: Boolean,
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
        add(
            OfferedSource(
                stringResource(R.string.search_filter_text),
                sources.text,
            ) { on -> sources.copy(text = on) },
        )
        add(
            OfferedSource(
                stringResource(R.string.search_filter_surahs),
                sources.surahs,
            ) { on -> sources.copy(surahs = on) },
        )
        add(
            OfferedSource(
                stringResource(R.string.search_filter_references),
                sources.references,
            ) { on -> sources.copy(references = on) },
        )
        if (hasTranslations) {
            add(
                OfferedSource(
                    stringResource(R.string.search_filter_translations),
                    sources.translations,
                ) { on -> sources.copy(translations = on) },
            )
        }
        if (hasWords) {
            add(
                OfferedSource(
                    stringResource(R.string.search_filter_words),
                    sources.words,
                ) { on -> sources.copy(words = on) },
            )
        }
        if (hasTafsirs) {
            add(
                OfferedSource(
                    stringResource(R.string.search_filter_tafsirs),
                    sources.tafsirs,
                ) { on -> sources.copy(tafsirs = on) },
            )
        }
        if (hasNotes) {
            add(
                OfferedSource(
                    stringResource(R.string.search_filter_notes),
                    sources.notes,
                ) { on -> sources.copy(notes = on) },
            )
        }
    }
    val chosen = offered.count { it.on }
    val off = offered.filterNot { it.on }
    val state = when {
        off.isEmpty() -> stringResource(R.string.search_filter_all)
        // One source off is named, so the closed door can explain a query
        // that found nothing because of it (owner decision, this session).
        off.size == 1 -> stringResource(R.string.search_filter_all_but, off[0].label)
        else -> stringResource(R.string.search_filter_some, chosen, offered.size)
    }
    val none = chosen == 0
    // A search with no place to look is not a search: the door opens itself
    // when the last source is turned off, and again when a query begins,
    // because the query folds the list shut. The status line's sentence then
    // always points at a list the reader can see and touch (owner decision,
    // this session).
    LaunchedEffect(none, hasQuery) {
        if (none) open = true
    }
    // The door is a choice of place, not a place to type: opening it puts the
    // keyboard away so the chips and the results have the sheet's room. That
    // is what keeps the seven chips reachable at the largest text size, where
    // they can be taller than the sheet with the keyboard up (owner decision,
    // this session).
    val focusManager = LocalFocusManager.current
    LaunchedEffect(open) {
        if (open) focusManager.clearFocus()
    }
    val reducedMotion = rememberReducedMotion()
    val chip = chipMeasure()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.Line, bottom = Space.Tight),
    ) {
        // The header is the app's one disclosure row: the words, the state,
        // and the chevron that turns. The press is a pill inside the 48 dp
        // target, hung to the left of the page's own gutter, so the words
        // land on the rail the chips below them stand on.
        DisclosureRow(
            expanded = open,
            onToggle = { open = !open },
            modifier = Modifier.padding(start = Space.Gutter - Space.Row),
            press = DisclosurePress.Pill,
        ) {
            Text(
                text = stringResource(R.string.search_filter),
                // The door names its state the way a settings row names its
                // own value: the row name's 15 over the meta 13, told apart
                // by weight and tone (owner decision, this session: 14 and
                // 13 were too near to read as a pair).
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = state,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Space.Line),
            )
        }
        AnimatedVisibility(
            visible = open,
            // A reader who asked the phone to reduce motion gets the list
            // without the height change; the information arrives, the page
            // does not move (owner decision, this session).
            enter = if (reducedMotion.value) fadeIn() else expandVertically() + fadeIn(),
            exit = if (reducedMotion.value) fadeOut() else shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.Gutter),
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.Line),
                    horizontalArrangement = Arrangement.spacedBy(Space.Line),
                    // The chip's pill is the label's line plus 8 dp above and
                    // below, and its target is at least 48 dp. The measure
                    // gives the flow the gap the target's air does not
                    // already leave, so two rows of capsules stand Space.Row
                    // apart, or the target air where that is more (owner
                    // decision, this session).
                    verticalArrangement = Arrangement.spacedBy(chip.rowGap),
                ) {
                    offered.forEach { source ->
                        AppChip(
                            label = source.label,
                            selected = source.on,
                            onClick = { onChange(source.set(!source.on)) },
                            kind = ChipKind.Toggle,
                            style = ChipStyle.Toggle,
                        )
                    }
                }
                // Seven ways to narrow and no way back to everything is a
                // trap: the reset stands under the chips, in the quiet shape,
                // only while there is something to turn back on (owner
                // decision, this session).
                if (off.isNotEmpty()) {
                    TextButton(
                        label = stringResource(R.string.search_filter_all_on),
                        onClick = { onChange(SearchSources.ALL) },
                        modifier = Modifier.padding(top = Space.Line),
                        quiet = true,
                    )
                }
            }
        }
    }
}
