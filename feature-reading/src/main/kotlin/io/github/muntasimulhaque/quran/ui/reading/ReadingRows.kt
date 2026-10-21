package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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



/** The surah's rows, drawn and driven once the text is on hand. */
@Composable
internal fun ReadingRows(
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
                        modifier = Modifier.padding(horizontal = Space.Gutter, vertical = Space.Tight),
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
                    // Only the ayah being recited carries the moving word.
                    // Every other block keeps a stable null, so a word change
                    // does not rebuild its AnnotatedString or recompose it
                    // (owner decision, this session).
                    playingWord = playingWord.takeIf { playingAyah == row.ayah.number },
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
                                    reference = row.ayah.verseKey,
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
