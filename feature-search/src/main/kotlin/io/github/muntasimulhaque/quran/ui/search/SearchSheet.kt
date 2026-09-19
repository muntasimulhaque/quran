package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.core.SearchQuery
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.SearchHit
import io.github.muntasimulhaque.quran.data.SearchRequest
import io.github.muntasimulhaque.quran.data.SearchResults
import io.github.muntasimulhaque.quran.data.SearchSources
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.rich.HighlightedText
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** More than a screenful of scroll is not a search; the rest is a narrower query. */
private const val LIMIT = 200

/**
 * The search sheet: one field, no modes, everything the reader has turned on.
 * Results are computed on a worker thread from index columns, so the first
 * keystroke and the hundredth cost the same, and a new query cancels the one
 * before it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    content: ContentDatabase,
    translationPacks: List<String>,
    tafsirPacks: List<String>,
    packNames: Map<String, String>,
    packLanguages: Map<String, String>,
    wordsPack: String,
    onDismiss: () -> Unit,
    onAyah: (Ayah, Int) -> Unit,
    onSurah: (Surah) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }

    var text by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(SearchResults()) }
    var searching by remember { mutableStateOf(false) }
    var sources by remember { mutableStateOf(SearchSources.ALL) }
    val queryTerms = remember(text) { Search.parse(text)?.terms.orEmpty() }
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) focus.requestFocus()
    }

    /*
     * One search per settled keystroke, or per changed filter: a new query
     * cancels the one before it instead of queueing behind it, so the reader
     * always sees the results of what they last asked for.
     */
    LaunchedEffect(text, content, sources) {
        val query = Search.parse(text)
        if (query == null) {
            results = SearchResults()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        val found = withContext(Dispatchers.IO) {
            content.search(
                SearchRequest(
                    query = query,
                    translationPacks = translationPacks,
                    tafsirPacks = tafsirPacks,
                    packNames = packNames,
                    packLanguages = packLanguages,
                    wordsPack = wordsPack,
                    sources = sources,
                    limit = LIMIT,
                ),
            )
        }
        results = found
        searching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            SearchField(
                value = text,
                onChange = { text = it },
                onClose = {
                    if (text.isEmpty()) onDismiss() else text = ""
                },
                focus = focus,
            )
            FilterRow(
                sources = sources,
                hasTranslations = translationPacks.isNotEmpty(),
                hasTafsirs = tafsirPacks.isNotEmpty(),
                onChange = { sources = it },
            )
            StatusLine(query = Search.parse(text), results = results, searching = searching)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                items(results.hits, key = { key(it) }) { hit ->
                    when (hit) {
                        is SearchHit.ReferenceHit -> ReferenceRow(hit.ayah, onAyah)
                        is SearchHit.SurahHit -> SurahRow(hit.surah, onSurah)
                        is SearchHit.AyahHit -> AyahRow(
                            hit = hit,
                            hafs = hafs,
                            terms = queryTerms,
                            showSource = translationPacks.size > 1,
                            onAyah = onAyah,
                        )
                        is SearchHit.TafsirHitResult -> TafsirRow(hit, onAyah)
                    }
                }
            }
        }
    }
}

private fun key(hit: SearchHit): String = when (hit) {
    is SearchHit.ReferenceHit -> "ref-${hit.ayah.number}"
    is SearchHit.SurahHit -> "surah-${hit.surah.number}"
    is SearchHit.AyahHit -> "ayah-${hit.ayah.number}"
    is SearchHit.TafsirHitResult -> "tafsir-${hit.pack}-${hit.surah}-${hit.fromAyah}"
}

/**
 * The filter row under the field: a labelled group of chips for the sources a
 * query reads, each one on until the reader turns it off. It is a quiet line,
 * not a mode: it never hides the field and never asks for a confirmation, so a
 * reader who ignores it searches exactly as before.
 *
 * The chips wrap instead of scrolling sideways. A row that runs off the edge
 * hides its own contents: a reader cannot turn off a source they cannot see,
 * and a source they cannot see is a source they do not know is on. The label
 * above the group says what the chips are, because "Translations" beside a
 * search field could as easily be a filter as a result type.
 *
 * A source the reader does not have (a translation they have not added, a
 * tafsir that is not installed) is not offered, rather than offered and
 * empty.
 */
@Composable
private fun FilterRow(
    sources: SearchSources,
    hasTranslations: Boolean,
    hasTafsirs: Boolean,
    onChange: (SearchSources) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = Space.Line, bottom = Space.Tight),
    ) {
        Text(
            text = stringResource(R.string.search_filter),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

/** One source, on or off, as a chip the reader taps. */
@Composable
private fun FilterChip(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (on) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (on) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                },
            )
            .toggleable(value = on, role = Role.Checkbox, onValueChange = onChange)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun SearchField(
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
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                        }
                        inner()
                    }
                },
            )
        }
        Text(
            text = stringResource(R.string.search_close),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClose)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun StatusLine(query: SearchQuery?, results: SearchResults, searching: Boolean) {
    val total = results.counts.total
    val counts = results.counts
    val partSurahs = if (counts.surahs > 0) {
        pluralStringResource(R.plurals.search_part_surah_names, counts.surahs, counts.surahs)
    } else {
        null
    }
    val partText = if (counts.arabic > 0) {
        pluralStringResource(R.plurals.search_part_text, counts.arabic, counts.arabic)
    } else {
        null
    }
    val partTranslation = if (counts.translation > 0) {
        pluralStringResource(R.plurals.search_part_translation, counts.translation, counts.translation)
    } else {
        null
    }
    val partWords = if (counts.words > 0) {
        pluralStringResource(R.plurals.search_part_words, counts.words, counts.words)
    } else {
        null
    }
    val partTafsir = if (counts.tafsir > 0) {
        pluralStringResource(R.plurals.search_part_tafsir, counts.tafsir, counts.tafsir)
    } else {
        null
    }
    val capped = if (results.capped) stringResource(R.string.search_status_capped, LIMIT) else null
    val message = when {
        query == null -> stringResource(R.string.search_status_prompt)
        searching && results.hits.isEmpty() -> stringResource(R.string.search_status_searching)
        total == 0 -> stringResource(R.string.search_status_no_matches)
        else -> buildString {
            append(pluralStringResource(R.plurals.search_matches, total, total))
            val parts = listOfNotNull(
                partSurahs,
                partText,
                partTranslation,
                partWords,
                partTafsir,
                capped,
            )
            if (parts.isNotEmpty()) {
                append("  \u00B7  ")
                append(parts.joinToString(", "))
            }
        }
    }
    Text(
        text = message,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 10.dp),
    )
}

@Composable
private fun ReferenceRow(ayah: Ayah, onAyah: (Ayah, Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAyah(ayah, 0) }
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.search_label_go_to),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = ayah.verseKey,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = stringResource(R.string.search_action_open),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SurahRow(surah: Surah, onSurah: (Surah) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSurah(surah) }
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.search_label_surah),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = surah.nameSimple,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(
                    R.string.search_surah_meta,
                    surah.versesCount,
                    placeName(surah.revelationPlace),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = surah.nameArabic,
            style = TextStyle(fontFamily = Amiri, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
private fun AyahRow(
    hit: SearchHit.AyahHit,
    hafs: FontFamily,
    terms: List<String>,
    showSource: Boolean,
    onAyah: (Ayah, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAyah(hit.ayah, hit.page) }
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(
            text = "${hit.ayah.verseKey}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = arabic(hit, hafs, terms),
            style = TextStyle(
                fontFamily = hafs,
                fontSize = 22.sp,
                lineHeight = 42.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Right,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        hit.translation?.let { translation ->
            Spacer(Modifier.height(8.dp))
            HighlightedText(
                text = translation.text,
                ranges = translation.ranges,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
            )
            if (showSource) {
                Text(
                    text = translation.packName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        hit.wordMeaning?.let { meaning ->
            Text(
                text = stringResource(R.string.search_word_meaning, meaning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun TafsirRow(hit: SearchHit.TafsirHitResult, onAyah: (Ayah, Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAyah(hit.ayah, hit.page) }
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(
                R.string.search_tafsir_range,
                hit.packName,
                hit.surahName,
                if (hit.fromAyah == hit.toAyah) {
                    "${hit.surah}:${hit.fromAyah}"
                } else {
                    "${hit.surah}:${hit.fromAyah}-${hit.toAyah}"
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        HighlightedText(
            text = hit.text,
            ranges = hit.ranges,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
            maxLines = 5,
        )
    }
}

/** The ayah with the matched words washed; the ranges are exact. */
@Composable
private fun arabic(hit: SearchHit.AyahHit, hafs: FontFamily, terms: List<String>): AnnotatedString {
    val text = hit.ayah.text
    val ranges = remember(text, terms) { Search.matchRanges(text, terms, arabic = true) }
    if (ranges.isEmpty()) return AnnotatedString(text)
    val base = LocalPagePalette.current.highlight
    val wash = base.copy(alpha = base.alpha.coerceAtLeast(0.25f))
    return buildAnnotatedString {
        var index = 0
        for (range in ranges) {
            val from = range.first.coerceIn(index, text.length)
            val to = (range.last + 1).coerceIn(from, text.length)
            if (from > index) append(text.substring(index, from))
            if (to > from) {
                withStyle(SpanStyle(background = wash)) { append(text.substring(from, to)) }
            }
            index = to
        }
        if (index < text.length) append(text.substring(index))
    }
}

@Composable
private fun placeName(place: String): String =
    if (place.equals("makkah", true)) {
        stringResource(R.string.search_place_makkah)
    } else {
        stringResource(R.string.search_place_madinah)
    }
