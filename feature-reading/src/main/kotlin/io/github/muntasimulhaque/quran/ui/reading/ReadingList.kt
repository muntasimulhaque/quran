package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.AyahList
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.AddDoor
import io.github.muntasimulhaque.quran.ui.kit.rememberReducedMotion
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Reading
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The reading of one surah: its opening, then a continuous scroll
 * through every ayah to the end of the surah, and a quiet closing line that
 * offers the next surah.
 *
 * A surah is the unit the Quran itself gives, and readers read it as one:
 * this view ends where the surah ends, instead of sliding into the next one
 * without a word. Tapping an ayah raises its actions; tapping the paper
 * around it brings the chrome.
 */
@Composable
fun ReadingList(
    content: ContentDatabase,
    surah: Surah,
    loadRows: suspend () -> List<ReadingRow>,
    /**
     * The rows already on hand for a surah, when there are any: the copy the
     * app read ahead while the reader was still in the surah before it. Read
     * here, during composition, so a turn at the end of a surah lands on text
     * that is already measured rather than on a blank frame.
     */
    cachedRows: (Int) -> List<ReadingRow>?,
    /** Reads the surah after this one while the reader is still in it. */
    onReadAhead: (Int) -> Unit,
    metrics: ReadingMetrics,
    /** The reader's place, which moves the list and never the other way round. */
    place: Int,
    /** How far into that ayah the reader was: a long ayah is many screens. */
    placeOffset: Int,
    hasTranslation: Boolean,
    /** The reader's language: the reading reads it, and so does the opening. */
    contentLanguage: String,
    nextSurahName: String?,
    selectedAyah: Int?,
    playingAyah: Int?,
    playingWord: Int?,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    onScrolled: () -> Unit,
    onNextSurah: (Int) -> Unit,
    onAddContent: () -> Unit,
    onPlaceChanged: (Int, Int) -> Unit,
    /** True when the reader opened this surah at its top; answered once. */
    startAtOpening: Boolean,
    onOpeningReached: () -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pageDescription = stringResource(R.string.reading_page_description)
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    // The whole surah arrives at once, so every item has its text before the
    // list is measured: the reader's place lands exactly, and nothing grows or
    // shifts under them while they read. Until the rows for *this* surah are
    // in hand the paper is shown, never the surah the reader just left: a
    // stale list would be measured with the new surah's name on it, and a
    // scroll through it would write a place in the wrong surah.
    //
    // The library is a key, because a pack arriving replaces the database and
    // the rows read from the old one would never show the new translation
    // under the ayah. The packs the reading draws are a key of their own,
    // because turning one on replaces the rows without reopening it.
    val loaded by produceState<Pair<Int, List<ReadingRow>>?>(
        initialValue = null,
        content,
        surah.number,
        metrics.translationPacks,
        metrics.wordByWord,
    ) { value = surah.number to loadRows() }
    // The copy read ahead in the surah before this one. It is this surah's
    // own, checked against the surah's number, so a stale list can never be
    // measured under the new surah's name; and it is there in the very frame
    // the surah changes, because it is read during composition rather than
    // from an effect. That is what makes the turn at the end of a surah land
    // on text instead of on the paper.
    val ahead = remember(content, surah.number, metrics.wordByWord) {
        cachedRows(surah.number)
    }
    val rows = ahead ?: loaded?.takeIf { it.first == surah.number }?.second.orEmpty()
    val ayahs = remember(rows) { rows.map { it.ayah.number } }

    // The surah after this one is read while the reader is still in this one,
    // so the turn at its end has its text on hand when it comes.
    LaunchedEffect(surah.number, content, metrics.wordByWord) { onReadAhead(surah.number) }

    var footnote by remember { mutableStateOf<OpenFootnote?>(null) }
    if (ayahs.isEmpty()) {
        // The paper is already there; the text is a frame away.
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    ReadingRows(
        content = content,
        surah = surah,
        rows = rows,
        ayahs = ayahs,
        metrics = metrics,
        place = place,
        placeOffset = placeOffset,
        hasTranslation = hasTranslation,
        contentLanguage = contentLanguage,
        nextSurahName = nextSurahName,
        selectedAyah = selectedAyah,
        playingAyah = playingAyah,
        playingWord = playingWord,
        hafs = hafs,
        pageDescription = pageDescription,
        onAyah = onAyah,
        onBackgroundTap = onBackgroundTap,
        onScrolled = onScrolled,
        onNextSurah = onNextSurah,
        onAddContent = onAddContent,
        onPlaceChanged = onPlaceChanged,
        startAtOpening = startAtOpening,
        onOpeningReached = onOpeningReached,
        contentPaddingTop = contentPaddingTop,
        contentPaddingBottom = contentPaddingBottom,
        footnote = footnote,
        onFootnote = { footnote = it },
        modifier = modifier,
    )
}

/** The surah's rows, drawn and driven once the text is on hand. */
@Composable
private fun ReadingRows(
    content: ContentDatabase,
    surah: Surah,
    rows: List<ReadingRow>,
    ayahs: List<Int>,
    metrics: ReadingMetrics,
    place: Int,
    placeOffset: Int,
    hasTranslation: Boolean,
    contentLanguage: String,
    nextSurahName: String?,
    selectedAyah: Int?,
    playingAyah: Int?,
    playingWord: Int?,
    hafs: FontFamily,
    pageDescription: String,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    onScrolled: () -> Unit,
    onNextSurah: (Int) -> Unit,
    onAddContent: () -> Unit,
    onPlaceChanged: (Int, Int) -> Unit,
    startAtOpening: Boolean,
    onOpeningReached: () -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    footnote: OpenFootnote?,
    onFootnote: (OpenFootnote?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = AyahList.indexOf(ayahs, place),
        // The reader may have stopped inside a long ayah that takes more than
        // a screen. The place carries that depth, so the app opens on the
        // line the reader was reading and not on the top of the block
        // (owner decision, this session).
        initialFirstVisibleItemScrollOffset = placeOffset,
    )

    // The surah's introduction opens from its quiet door; once it is open, a
    // tap anywhere on the paper, the ayahs, or the closing line puts it away
    // again, exactly as the Hide action does, so the reader is never left
    // looking for the way out. The state lives here rather than inside the
    // opening item because the taps that dismiss it land outside that item.
    var aboutOpen by rememberSaveable(surah.number) { mutableStateOf(false) }
    val dismissAbout: () -> Unit = {
        if (aboutOpen) aboutOpen = false else onBackgroundTap()
    }

    // Moving the page means reading, and reading wants the page: the chrome
    // is told to step aside as soon as the list is moving under a finger.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling -> if (scrolling) onScrolled() }
    }

    // The reader's place is written down when a drag of their own ends, and
    // never by a scroll the app started. The flag is cleared on the write, so
    // the jump that follows a drag cannot write a place of its own.
    var dragged by remember(surah.number) { mutableStateOf(false) }
    LaunchedEffect(listState, surah.number) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) dragged = true
        }
    }
    LaunchedEffect(listState, surah.number) {
        snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
            )
        }
            .collect { (scrolling, index, offset) ->
                if (scrolling || !dragged) return@collect
                dragged = false
                // The opening item is not an ayah: its own scroll depth
                // belongs to the surah's top, so the place is written as the
                // first ayah at its own top rather than at a depth measured
                // against a different item.
                val depth = if (index == 0) 0 else offset
                AyahList.ayahAt(ayahs, index)?.let { onPlaceChanged(it, depth) }
            }
    }

    // The reader's place moves the list, exactly, and the list never moves
    // itself. The place is an ayah and a depth inside it, so a round trip
    // through the list has to land on the same item at the same depth;
    // anything less leaves the place behind.
    LaunchedEffect(place, placeOffset, surah.number, ayahs) {
        val target = AyahList.indexOf(ayahs, place)
        if (target != listState.firstVisibleItemIndex ||
            placeOffset != listState.firstVisibleItemScrollOffset
        ) {
            listState.scrollToItem(target, placeOffset)
        }
    }

    // Opening a surah from Browse lands on its top: the opening item, which
    // is not an ayah and so has no place of its own. The request is its own
    // effect rather than a branch of the place above, because the place must
    // not re-run when the request is cleared: that is what would pull the
    // reader back down to the first ayah the moment the opening appeared.
    LaunchedEffect(listState, ayahs, startAtOpening) {
        if (!startAtOpening || ayahs.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(0)
        onOpeningReached()
    }

    // The playing ayah comes back into view when the reciter moves on. A
    // reader who asked the system to reduce motion is taken there at once.
    val reducedMotion by rememberReducedMotion()
    LaunchedEffect(playingAyah, ayahs, reducedMotion) {
        val ayahNumber = playingAyah ?: return@LaunchedEffect
        if (!metrics.followReciter) return@LaunchedEffect
        val target = AyahList.indexOf(ayahs, ayahNumber)
        val current = listState.firstVisibleItemIndex
        if (target < current || target > current + 3) {
            if (reducedMotion) listState.scrollToItem(target)
            else listState.animateScrollToItem(target)
        }
    }

    // The reading sits in a column a person can read, centered when the
    // screen is wider than it. The column is the reader's own measure: the
    // character cap of the voice the translation is read in and of the Arabic
    // above it, whichever is narrower, so neither text runs past a line the
    // eye can follow back to its margin on its own. The list itself stays
    // full width, so the paper around the column still answers a tap with the
    // chrome and a drag with the scroll.
    val measure = minOf(
        Reading.proseMeasure(LocalReadingVoice.current, metrics.translationSp),
        Reading.quranMeasure(metrics.arabicSp),
    )
    // The column's own width is what centers it, not the window's: a
    // landscape split, a fold, and a display cutout all move the width the
    // reading actually has, and the measure follows the glass it is drawn on
    // (owner decision, this session).
    BoxWithConstraints(modifier.fillMaxSize()) {
        val side = maxOf(20.dp, (maxWidth - measure) / 2)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = pageDescription }
                .pointerInput(Unit) {
                    // The paper around the text brings the chrome; the text
                    // itself belongs to its ayah, and every ayah answers its
                    // own tap.
                    detectTapGestures(onTap = { dismissAbout() })
                },
            contentPadding = PaddingValues(
                start = side,
                end = side,
                top = contentPaddingTop,
                bottom = contentPaddingBottom,
            ),
        ) {
            item(key = "surah-${surah.number}", contentType = "opening") {
                SurahOpening(
                    surah = surah,
                    content = content,
                    language = contentLanguage,
                    expanded = aboutOpen,
                    onExpandedChange = { aboutOpen = it },
                    onPaperTap = { if (!aboutOpen) onBackgroundTap() },
                    metrics = metrics,
                )
                // The door asks for a translation only while translations are
                // something the reading draws at all: a reader who hid them in
                // Settings is not looking for one here.
                if (metrics.showTranslation && !hasTranslation) {
                    AddDoor(
                        text = stringResource(R.string.reading_add_translation),
                        action = stringResource(R.string.reading_action_add),
                        onClick = onAddContent,
                        modifier = Modifier.padding(horizontal = Space.Gutter, vertical = 4.dp),
                    )
                }
            }
            items(
                count = rows.size,
                key = { "ayah-${rows[it].ayah.number}" },
                // One slot type for every ayah, so a block that scrolls out
                // is the block the next one reuses; the opening and the
                // closing line are their own kinds and never borrow an
                // ayah's measured composition (owner decision, this
                // session).
                contentType = { "ayah" },
            ) { index ->
                val row = rows[index]
                AyahBlock(
                    row = row,
                    hafs = hafs,
                    metrics = metrics,
                    isSelected = selectedAyah == row.ayah.number,
                    playingAyah = playingAyah,
                    playingWord = playingWord,
                    onAyah = onAyah,
                    onBackgroundTap = dismissAbout,
                    onFootnote = { number ->
                        val note = row.translations.asSequence()
                            .flatMap { it.text.footnotes.asSequence() }
                            .firstOrNull { it.number == number }
                        if (note != null) {
                            onFootnote(
                                OpenFootnote(
                                    note = note,
                                    reference = "${row.ayah.surah}:${row.ayah.ayah}",
                                    sizeSp = metrics.translationSp,
                                    lineSp = metrics.translationLineSp,
                                ),
                            )
                        }
                    },
                )
            }
            item(key = "end-${surah.number}", contentType = "end") {
                SurahEnd(
                    surah = surah,
                    nextSurahName = nextSurahName,
                    onNextSurah = onNextSurah,
                )
            }
        }
    }

    footnote?.let { open ->
        FootnoteSheet(
            footnote = open.note,
            surahName = surah.nameSimple,
            reference = open.reference,
            sizeSp = open.sizeSp,
            lineSp = open.lineSp,
            onDismiss = { onFootnote(null) },
        )
    }
}

/** A quiet door to the packs the reader does not have yet. */

