package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.SearchHit
import io.github.muntasimulhaque.quran.data.SearchRequest
import io.github.muntasimulhaque.quran.data.SearchResults
import io.github.muntasimulhaque.quran.data.SearchSources
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.SheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.sheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.theme.Reading
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
    onDismiss: () -> Unit,
    onAyah: (SearchArrival) -> Unit,
    onSurah: (Surah) -> Unit,
) {
    val sheetState = rememberExpandedSheetState()
    val focus = remember { FocusRequester() }
    val hafs = rememberHafs()

    var text by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(SearchResults()) }
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
    val queryTerms = remember(text) { Search.parse(text)?.terms.orEmpty() }
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) focus.requestFocus()
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
            searching = false
            failed = false
            return@LaunchedEffect
        }
        failed = false
        delay(SETTLE_MILLIS)
        searching = true
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
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = Reading.SearchMeasure)
                .fillMaxSize()
                .imePadding(),
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
            StatusLine(
                query = Search.parse(text),
                results = results,
                searching = searching,
                hasSources = sources.any,
                failed = failed,
            )
            LazyColumn(
                state = resultsList,
                modifier = Modifier
                    .weight(1f)
                    .sheetDragGate(resultsGate),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                items(results.hits, key = { key(it) }) { hit ->
                    when (hit) {
                        is SearchHit.ReferenceHit -> ReferenceRow(hit.ayah) {
                            onAyah(SearchArrival.AyahMatch(hit.ayah))
                        }
                        is SearchHit.SurahHit -> SurahRow(hit.surah, onSurah)
                        is SearchHit.AyahHit -> AyahRow(
                            hit = hit,
                            hafs = hafs,
                            terms = queryTerms,
                            showSource = translationPacks.size > 1,
                            arabicStep = arabicStep,
                            onOpen = {
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
                        is SearchHit.TafsirHitResult -> TafsirRow(hit) {
                            onAyah(SearchArrival.TafsirMatch(hit.ayah, hit.pack))
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
