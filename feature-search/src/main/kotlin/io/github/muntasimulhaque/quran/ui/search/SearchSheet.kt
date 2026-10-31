package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.core.SearchQuery
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.SearchHit
import io.github.muntasimulhaque.quran.data.SearchHistory
import io.github.muntasimulhaque.quran.data.SearchRequest
import io.github.muntasimulhaque.quran.data.SearchResults
import io.github.muntasimulhaque.quran.data.SearchSources
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.EmptyNote
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.SheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.sheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.rememberHafs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** More than a screenful of scroll is not a search; the rest is a narrower query. */
internal const val LIMIT = 200

/**
 * How long a query has to sit still before it is run.
 *
 * A reader's hand outruns their typing: "mercy" arrives as five queries, and
 * each one is a pass over the whole of the reader's library. Five passes at
 * once on a phone are five scans contending for the same four cores, so the
 * results that land are the results of a busy machine rather than a fast one,
 * and the search the owner reported as slow was mostly this (owner report).
 * One search per settled query is both the promise this sheet has
 * always made in its own words and the fastest thing to hand a reader.
 *
 * The wait is short enough to read as instant and long enough to be past the
 * gap between two keys of a fast typist, and the first keystroke pays it too:
 * a reader who types one letter and waits is answered by a real search, not
 * by a search that is still settling.
 */
private const val SETTLE_MILLIS = 120L

/** One query and the sources it read, so a result can never be shown under a different question. */
private data class SearchKey(val query: SearchQuery, val sources: SearchSources)

/**
 * The source flags as six booleans in the saved state: the chips survive the
 * window with the query they were applied to, so a rotation cannot silently
 * turn a source back on (owner decision, the theme and consistency session).
 */
private val SearchSourcesSaver: Saver<SearchSources, List<Boolean>> = Saver(
    save = { listOf(it.text, it.surahs, it.references, it.words, it.translations, it.tafsirs) },
    restore = { flags ->
        SearchSources(
            text = flags.getOrElse(0) { true },
            surahs = flags.getOrElse(1) { true },
            references = flags.getOrElse(2) { true },
            words = flags.getOrElse(3) { true },
            translations = flags.getOrElse(4) { true },
            tafsirs = flags.getOrElse(5) { true },
        )
    },
)

/**
 * The search sheet: one field, no modes, everything the reader has turned on.
 * Results are computed on a worker thread from index columns, one search per
 * settled query, so the first query and the hundredth cost the same and a new
 * one cancels the one before it (owner report).
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
    /** Whether the reader's own word list is on the device. */
    wordsInstalled: Boolean,
    /** The reader's own Arabic step, so the results follow the reading. */
    arabicStep: Float,
    /** The reader's own translation size, so a snippet is the reading's own text. */
    proseSp: Float,
    proseLineSp: Float,
    onDismiss: () -> Unit,
    onAyah: (SearchArrival) -> Unit,
    onSurah: (Surah) -> Unit,
) {
    val sheetState = rememberExpandedSheetState()
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val hafs = rememberHafs()

    var text by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(SearchResults()) }
    // The key the results belong to. The old results stay in memory while a
    // new query runs, but they are never drawn under a query that did not
    // produce them (owner decision, this session).
    var resultKey by remember { mutableStateOf<SearchKey?>(null) }
    // The queries the reader acted on before, newest first. The list is read
    // from the phone's own shelf when the sheet opens and written back as a
    // result is opened, so a word typed and abandoned is never kept: the
    // reader's hand outruns their intent, and a list of half-typed words is
    // noise (owner decision, this session).
    val context = LocalContext.current
    val history = remember(context) { SearchHistory(context) }
    var recents by remember { mutableStateOf(history.read()) }
    val keepQuery: () -> Unit = {
        if (text.isNotBlank()) recents = history.record(text)
    }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    // The source flags survive the window like the query does: a rotation
    // with a chip turned off must not silently turn it back on and change
    // the results under the reader (owner decision, the theme and
    // consistency session).
    var sources by rememberSaveable(stateSaver = SearchSourcesSaver) {
        mutableStateOf(SearchSources.ALL)
    }
    // The result list wears the same drag gate Browse does, so a scroll back
    // to the top can never turn into a pull that closes the sheet.
    val resultsList = rememberLazyListState()
    val resultsGate = remember(resultsList) { SheetDragGate(resultsList) }
    val currentQuery = remember(text) { Search.parse(text) }
    val currentKey = remember(currentQuery, sources) {
        currentQuery?.let { SearchKey(it, sources) }
    }
    val resultsCurrent = currentKey != null && resultKey == currentKey
    val queryTerms = currentQuery?.terms.orEmpty()
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            // Type into a fresh search; return from a result to read the
            // results, not to type over them. The keyboard is the difference
            // the reader feels (owner decision, this session).
            if (text.isBlank()) {
                focus.requestFocus()
            } else {
                focusManager.clearFocus()
            }
        }
    }

    /*
     * One search per settled keystroke, or per changed filter: a new query
     * cancels the one before it instead of queueing behind it, so the reader
     * always sees the results of what they last asked for.
     *
     * Settled means settled: the query waits [SETTLE_MILLIS] before it runs,
     * because a search per key is a search per word five times over, and a
     * search that gives up early when it is superseded (see `search`) cannot
     * give up work that has already been handed to the database.
     */
    LaunchedEffect(text, content, sources) {
        val query = Search.parse(text)
        if (query == null || !sources.any) {
            results = SearchResults()
            resultKey = null
            searching = false
            failed = false
            return@LaunchedEffect
        }
        val key = SearchKey(query, sources)
        failed = false
        // Searching starts now, not after the settle: the old results are
        // hidden while the pause runs, so the page never shows an answer to
        // the previous query under the new one.
        searching = true
        delay(SETTLE_MILLIS)
        // The search is blocking work over the reader's whole library, so it
        // runs on a worker; the delay above is where a superseded query is
        // dropped, and the database checks for the same thing between its own
        // passes so a query already inside SQLite gives up at the next source.
        val found = withContext(Dispatchers.IO) {
            runCatching {
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
        }
        failed = found.isFailure
        results = found.getOrElse { SearchResults() }
        resultKey = key
        searching = false
    }

    /*
     * The tafsir is the one source too big to read per keystroke, so the app
     * keeps a folded copy of the enabled packs in memory and builds it when
     * the sheet opens rather than inside the reader's first query: opening the
     * sheet is the moment they have said they mean to search, and the build
     * then happens while the field is still empty (owner report).
     */
    LaunchedEffect(content, tafsirPacks) {
        content.warmTafsir(tafsirPacks)
    }

    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        name = stringResource(R.string.search_sheet_name),
    ) {
        // The results stand on the app's one page, which is the sheet, which
        // is the glass: a row's words stop at Space.Gutter and its own card
        // reaches outside that line, the same grammar the settings sheet and
        // the reading keep (owner decision, this session).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .testTag("search-sheet"),
        ) {
            SearchField(
                value = text,
                onChange = { text = it },
                // Close ends the search; the field's own clear mark empties
                // it. One shape does one job, so a reader with a query in the
                // field is not made to tap twice to leave (owner decision,
                // this session).
                onClose = onDismiss,
                focus = focus,
            )
            FilterRow(
                sources = sources,
                hasQuery = queryTerms.isNotEmpty(),
                hasTranslations = translationPacks.isNotEmpty(),
                hasTafsirs = tafsirPacks.isNotEmpty(),
                hasWords = wordsInstalled,
                onChange = { sources = it },
            )
            // Before a query the sheet is the field, the filter door, and the
            // note that says what the field can be asked, so a reader who
            // opens the search with a reference in hand does not have to
            // guess that a reference is a query (owner decision, this
            // session). The note is the app's one empty page, drawn by
            // Browse's lists too, and it goes the moment the field holds a
            // word: it teaches, it does not stand beside an answer.
            //
            // It is the results list's first item rather than a block above
            // it, because the list is the one part of the sheet that is
            // measured against what is left of the glass: a phone on its side
            // with the keyboard up leaves the sheet shorter than the field,
            // the door, and the note together, and a note in the list can
            // still be scrolled to where a note above it would be cut off.
            //
            // The sentence names the reader's own translation only when the
            // reader has prose on the device. A translation, a tafsir, or the
            // word list is what a Latin word can match, so with none of the
            // three installed the note asks for a word from the Quran and
            // promises nothing that would return nothing.
            val prose = translationPacks.isNotEmpty() ||
                tafsirPacks.isNotEmpty() ||
                wordsInstalled
            StatusLine(
                query = currentQuery,
                results = results,
                searching = searching,
                hasSources = sources.any,
                failed = failed,
                current = resultsCurrent,
            )
            LazyColumn(
                state = resultsList,
                modifier = Modifier
                    .weight(1f)
                    .sheetDragGate(resultsGate),
                contentPadding = PaddingValues(bottom = Space.SheetFoot),
            ) {
                if (text.isBlank()) {
                    if (recents.isNotEmpty()) {
                        item(key = "recent") {
                            RecentSearches(
                                queries = recents,
                                onQuery = { text = it },
                                onClear = { recents = history.clear() },
                            )
                        }
                    }
                    item(key = "note") {
                        EmptyNote(
                            title = stringResource(R.string.search_empty_title),
                            body = localizedDigits(
                                stringResource(
                                    if (prose) {
                                        R.string.search_empty_body
                                    } else {
                                        R.string.search_empty_body_quran
                                    },
                                ),
                            ),
                            modifier = Modifier.padding(top = Space.Section),
                        )
                    }
                }
                if (resultsCurrent) {
                    items(results.hits, key = { key(it) }) { hit ->
                        when (hit) {
                        is SearchHit.ReferenceHit -> ReferenceRow(hit.ayah) {
                            keepQuery()
                            onAyah(SearchArrival.AyahMatch(hit.ayah))
                        }
                        is SearchHit.SurahHit -> SurahRow(hit.surah) { surah ->
                            keepQuery()
                            onSurah(surah)
                        }
                        is SearchHit.AyahHit -> AyahRow(
                            hit = hit,
                            hafs = hafs,
                            terms = queryTerms,
                            showSource = translationPacks.size > 1,
                            arabicStep = arabicStep,
                            proseSp = proseSp,
                            proseLineSp = proseLineSp,
                            onOpen = {
                                keepQuery()
                                onAyah(
                                    SearchArrival.AyahMatch(
                                        ayah = hit.ayah,
                                        arabicWords = hit.arabicMatchedWords,
                                        meaningWords = hit.matchedWordText,
                                        translationPack = hit.translation?.pack,
                                    ),
                                )
                            },
                        )
                            is SearchHit.TafsirHitResult -> TafsirRow(
                                hit = hit,
                                proseSp = proseSp,
                                proseLineSp = proseLineSp,
                            ) {
                                keepQuery()
                                onAyah(SearchArrival.TafsirMatch(hit.ayah, hit.pack))
                            }
                        }
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
 * The queries the reader acted on before, as chips over the empty sheet.
 *
 * A search usually begins with a word the reader has already used, and the
 * empty sheet is the one place that can hand it back. The chips are the app's
 * own text button in its quiet tone, so a query never reads as the field's
 * own text or as the app's one action; the head names the block, and Clear
 * empties the list on the phone as well. A query is only here because the
 * reader opened something it found, which is why the list is short and
 * useful instead of a record of every keystroke (owner decision, this
 * session).
 */
@Composable
private fun RecentSearches(
    queries: List<String>,
    onQuery: (String) -> Unit,
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
                QueryChip(label = query, onClick = { onQuery(query) })
            }
        }
    }
}
