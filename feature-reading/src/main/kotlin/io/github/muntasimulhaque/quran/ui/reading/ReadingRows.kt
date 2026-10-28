package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
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
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlin.math.abs



/** The surah's rows, drawn and driven once the text is on hand. */
@Composable
internal fun ReadingRows(
    content: ContentDatabase,
    surah: Surah,
    rows: List<ReadingRow>,
    ayahs: List<Int>,
    metrics: ReadingMetrics,
    reveal: ReadingReveal?,
    /** The reader's own word-by-word switch, before any arrival turns it on. */
    wordAid: Boolean,
    place: Int,
    placeOffset: Int,
    placeOffsetWidth: Int,
    placeOffsetFontScale: Float,
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
    onPlaceChanged: (Int, Int, Int, Float) -> Unit,
    startAtOpening: Boolean,
    onOpeningReached: () -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    footnote: OpenFootnote?,
    onFootnote: (OpenFootnote?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val fontScale = density.fontScale
    // The stored depth belongs to the width and font scale it was measured
    // in. A different phone, a rotation, a split window, or a larger system
    // font reflows the same pixel into a different line, so the depth is
    // dropped to the ayah's own top rather than pointing at the wrong text.
    val depthKnown = windowWidth > 0
    val depthMatches = placeOffset == 0 ||
        (depthKnown && placeOffsetWidth == windowWidth &&
            abs(placeOffsetFontScale - fontScale) < 0.01f)
    val savedOffset = if (!depthKnown || depthMatches) placeOffset else 0
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = AyahList.indexOf(ayahs, place),
        // The reader may have stopped inside a long ayah that takes more than
        // a screen. The place carries that depth, so the app opens on the
        // line the reader was reading and not on the top of the block
        // (owner decision, this session).
        initialFirstVisibleItemScrollOffset = savedOffset,
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

    // The reader's place is written down when a drag of their own ends, and
    // never by a scroll the app started. The flag is cleared on the write, so
    // the jump that follows a drag cannot write a place of its own.
    var dragged by remember(surah.number) { mutableStateOf(false) }
    // Moving the page means reading, and reading wants the page: the chrome
    // is told to step aside when the reader's own drag begins. The place
    // landing on screen is not the reader moving the page, so a scroll the
    // app started does not hide the first reveal.
    LaunchedEffect(listState, surah.number) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                dragged = true
                onScrolled()
            }
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
                AyahList.ayahAt(ayahs, index)?.let { onPlaceChanged(it, depth, windowWidth, fontScale) }
            }
    }

    // A depth that does not belong to this layout is rewritten as the ayah's
    // own top once, so the next launch does not read it as a line again.
    LaunchedEffect(windowWidth, fontScale, place, placeOffset) {
        if (placeOffset > 0 && depthKnown && !depthMatches) {
            onPlaceChanged(place, 0, windowWidth, fontScale)
        }
    }

    // The reader's place moves the list, exactly, and the list never moves
    // itself. The place is an ayah and a depth inside it, so a round trip
    // through the list has to land on the same item at the same depth;
    // anything less leaves the place behind.
    LaunchedEffect(place, placeOffset, surah.number, ayahs) {
        val target = AyahList.indexOf(ayahs, place)
        // A drag that settles on the surah opening writes the first ayah as
        // the place, at depth 0. The opening is list index 0 and the first
        // ayah is index 1, so the round trip would scroll one item down and
        // hide the opening and the basmalah under the reader's own gesture.
        // The opening is already the truthful place for that first ayah.
        if (target == 1 && placeOffset == 0 && listState.firstVisibleItemIndex == 0) {
            return@LaunchedEffect
        }
        if (target != listState.firstVisibleItemIndex ||
            savedOffset != listState.firstVisibleItemScrollOffset
        ) {
            listState.scrollToItem(target, savedOffset)
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

    // The reading's words stop at the app's one margin, on every glass. The
    // list keeps that margin less the room a wash reaches past its text, so
    // the ayah and its wash stand on the same two lines on a phone, a seven
    // inch tablet, and a ten inch screen, and the paper around them stays full
    // width to answer a tap with the chrome and a drag with the scroll.
    //
    // Nothing here reads the window's width: a line of reading no longer
    // grows or shrinks by a count of characters, only by the glass it is
    // drawn on, so a reader who turns the text down gets more lines and not a
    // shorter column (owner decision, this session).
    val side = Space.Gutter - Space.WashInset
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = pageDescription }
            .pointerInput(Unit) {
                // The paper around the text brings the chrome; the text
                // itself belongs to its ayah, and every ayah answers its own
                // tap.
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
                    modifier = Modifier.padding(vertical = Space.Tight),
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
                reveal = reveal?.takeIf { it.ayah == row.ayah.number },
                showWordAid = wordAid ||
                    (reveal?.wordByWord == true && reveal.ayah == row.ayah.number),
                onAyah = onAyah,
                onBackgroundTap = dismissAbout,
                onFootnote = { line, number ->
                    val note = line.text.footnotes.firstOrNull { it.number == number }
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
