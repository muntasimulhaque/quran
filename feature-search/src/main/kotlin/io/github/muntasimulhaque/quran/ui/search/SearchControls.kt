package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.core.SearchQuery
import io.github.muntasimulhaque.quran.data.SearchResults
import io.github.muntasimulhaque.quran.data.SearchSources
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash

@Composable
internal fun FilterRow(
    sources: SearchSources,
    hasTranslations: Boolean,
    hasTafsirs: Boolean,
    hasWords: Boolean,
    onChange: (SearchSources) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = Space.Line, bottom = Space.Tight),
    ) {
        // The header is a door, drawn the way the app draws every status line
        // that opens something: the words, the state, and a chevron that
        // turns to say which way the list will move, with no fill, so it is
        // read as a door and never as a chip that happens to be off. The
        // whole row is the target, and its own gap keeps the words off the
        // ripple (owner decision, this session).
        Row(
            modifier = Modifier
                .clip(Radii.Pill)
                .clickable(role = Role.Button, onClick = { open = !open })
                .minimumInteractiveComponentSize()
                .semantics {
                    stateDescription = if (open) expanded else collapsed
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.search_filter),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = state,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
            // The arrow sits with the words it belongs to, and turns to say
            // which way the list will move: down while it is hidden, up once
            // it is open.
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(16.dp)
                    .rotate(if (open) 180f else 0f),
            )
        }
        AnimatedVisibility(visible = open) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Line),
                horizontalArrangement = Arrangement.spacedBy(Space.Line),
                verticalArrangement = Arrangement.spacedBy(Space.Line),
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
    // A chip is a small word around a small mark; the click and the node a
    // screen reader focuses carry the app's 48 dp target, so the source a
    // reader cannot hit is not a source they can turn off (owner decision,
    // the accessibility test). The pill fills that target rather than sitting
    // small inside it, the way the ayah-number cell and the icon buttons do:
    // at the label's 16 dp line, 16 dp above and below makes the shape 48 dp
    // tall, so the gap the flow leaves between two chips is the gap the eye
    // reads and no part of it is an invisible target (owner decision, this
    // session). The ripple is drawn on the pill inside the click node, not on
    // the node itself: an indication left on the click would flash a
    // rectangle over the shape the reader is looking at (the same rule
    // TextButton and BrowseTabChip wear).
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
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (on) {
            IconGlyph(
                icon = Icon.Check,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ink,
        )
    }
}

@Composable
internal fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    onClose: () -> Unit,
    focus: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(Radii.Pill)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                // The hint is the only text in an empty field,
                                // so it has to be read to be a hint at all. The
                                // 0.55 alpha it wore measured 2.5:1, under the
                                // theme's 4.5:1 rule.
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
        }
        TextButton(
            label = stringResource(R.string.search_close),
            onClick = onClose,
            modifier = Modifier.padding(start = 8.dp),
            quiet = true,
        )
    }
}

@Composable
internal fun StatusLine(
    query: SearchQuery?,
    results: SearchResults,
    searching: Boolean,
    hasSources: Boolean,
) {
    val total = results.counts.total
    val message = when {
        !hasSources -> stringResource(R.string.search_status_no_sources)
        query == null -> stringResource(R.string.search_status_prompt)
        searching && results.hits.isEmpty() -> stringResource(R.string.search_status_searching)
        total == 0 -> stringResource(R.string.search_status_no_matches)
        // A capped list has no honest count: the number the pass collected is
        // not the number that exists, and a reader who acts on it is acting
        // on a floor. The line says what the reader is looking at instead of
        // how much there is (owner decision, this session).
        results.capped -> stringResource(R.string.search_status_capped, LIMIT)
        else -> pluralStringResource(R.plurals.search_matches, total, total)
    }
    Text(
        text = message,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 10.dp),
    )
}

