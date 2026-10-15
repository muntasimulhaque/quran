package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
import io.github.muntasimulhaque.quran.ui.kit.SheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.sheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.SheetHandle
import io.github.muntasimulhaque.quran.ui.theme.Reading
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
    onDismiss: () -> Unit,
    onAyah: (Ayah) -> Unit,
    onSurah: (Surah) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }

    var text by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(SearchResults()) }
    var searching by remember { mutableStateOf(false) }
    var sources by remember { mutableStateOf(SearchSources.ALL) }
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
            return@LaunchedEffect
        }
        delay(SETTLE_MILLIS)
        searching = true
        // The search is blocking work over the reader's whole library, so it
        // runs on a worker; the delay above is where a superseded query is
        // dropped, and the database checks for the same thing between its own
        // passes so a query already inside SQLite gives up at the next source.
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { SheetHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        // The snippets are the one prose the sheet draws, and they take the
        // same character measure as the reading: about 65 characters at the
        // 14 sp they are set in, rather than the 85 the default 640 dp sheet
        // gave them on a tablet (owner report, the audio and type audit
        // session).
        sheetMaxWidth = Reading.SearchMeasure,
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
