package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Dp
import io.github.muntasimulhaque.quran.core.AyahList
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.RecitedWord
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.data.WordAudioState
import io.github.muntasimulhaque.quran.data.WordVoice
import io.github.muntasimulhaque.quran.ui.kit.rememberPageSideMargins
import io.github.muntasimulhaque.quran.ui.kit.rememberReducedMotion
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.flow.first

/**
 * How close to the end of the surah the next one is read ahead. Twelve ayahs
 * is more than a screen, so the turn at the end still lands on text while the
 * read has all the time it needs.
 */
private const val READ_AHEAD_AYAHS = 12

/**
 * The reading's cache window: how far ahead of the reader a row is built, and
 * how far behind them it is kept (owner decision, 21.6).
 *
 * The list's own default keeps nothing behind the page and holds its ahead
 * window under half a screen. An item that leaves the top is disposed on the
 * frame it leaves, and a return to it rebuilds the whole block on the frame it
 * comes back: the spoken ayah, the shaped Arabic line, the translation's runs,
 * and the semantics around all three. A reader who scrolls up is therefore the
 * reader who pays for their rows twice, and the taller a block is the less the
 * default can reach across it, because a block that carries a translation is
 * often more than half the viewport.
 *
 * A whole viewport each way was measured on the phone AVD by counting the
 * blocks composed during a return: four slow drags into the surah, then four
 * back, with a temporary counter in the block itself, taken out before this
 * was committed. The return built two blocks at the list's own default (three
 * of the four trials built two), one with half a viewport behind, and one with
 * a whole viewport behind, and the whole viewport was the one that never built
 * more than one. The same counter settled the rest of the diagnosis: no block
 * ever recomposed during a scroll, so every block a return meets is a fresh
 * composition, which is what makes that count a count of rows rebuilt.
 *
 * What the window costs is composed state alone: the surah's rows are read
 * whole and held in hand before the list is drawn, so nothing here asks the
 * library for a query it was not already making.
 *
 * One consequence is written down where it shows. The read ahead request reads
 * the last visible index, and a window keeps items composed past the viewport,
 * so that request can arrive a row or two early. It waits behind twelve rows,
 * which is more than a screen, so an early arrival lands inside the room the
 * request already kept.
 */
private val ReadingCacheWindow = LazyLayoutCacheWindow(
    aheadFraction = 1f,
    behindFraction = 1f,
)

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
    /** True when the reader's own word list is on the device. */
    wordsInstalled: Boolean = true,
    /** The reader's tap on the head's invitation: the page with the list. */
    onAddWords: () -> Unit = {},
    contentLanguage: String,
    nextSurahName: String?,
    /**
     * The ayah whose ground the page paints: the one whose menu is open, or
     * the one whose note is open over it.
     */
    selectedAyah: Int?,
    /** The ayah whose menu is open, so its door's turn says which way. */
    menuAyah: Int? = null,
    /** The ayahs the reader wrote a note on, so their head can say so. */
    notedAyahs: Set<Int> = emptySet(),
    /** The ayahs the reader has pressed Save on, so their doors say so. */
    savedAyahs: Set<Int> = emptySet(),
    playingAyah: Int?,
    /** The word being recited, read inside the one ayah being heard. */
    playedWord: State<RecitedWord?>,
    /** The ayah whose word is sounding, when the sound is a word sample. */
    hearingAyah: Int? = null,
    /** The sounding word's position in its ayah, when the sound is a sample. */
    hearingWord: Int? = null,
    hafs: FontFamily,
    pageDescription: String,
    onAyahMenu: (Ayah, IntRect) -> Unit,
    onBackgroundTap: () -> Unit,
    onScrolled: () -> Unit,
    /**
     * What a scroll spends on the reading's bar before the page moves, and
     * what the bar took. The bar's own arithmetic lives in the app beside
     * the screen; this list is the reader's finger's only way to it.
     */
    onBarScroll: (Float) -> Float = { 0f },
    /** The reader's scroll stopped with the bar between its two ends. */
    onBarSettled: () -> Unit = {},
    /** The reader is close to the end: read the surah after this one. */
    onReadAhead: () -> Unit = {},
    onNextSurah: (Int) -> Unit,
    /** Opens an internal reference the surah's introduction links to. */
    onOpenReference: (surah: Int, ayah: Int?) -> Unit = { _, _ -> },
    /** True while the daily reminder offer stands at this surah's end. */
    reminderOffer: Boolean = false,
    onReminderOn: () -> Unit = {},
    onReminderNotNow: () -> Unit = {},
    onPlaceChanged: (Int) -> Unit,
    /** The ayah a jump landed on, so the reading can announce the place. */
    onJumped: (Ayah) -> Unit = {},
    startAtOpening: Boolean,
    onOpeningReached: () -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    footnote: OpenFootnote?,
    onFootnote: (OpenFootnote?) -> Unit,
    /** The word the reader pressed and held, and the sheet that holds it. */
    word: OpenWord?,
    onWord: (OpenWord?) -> Unit,
    /** The word sheet's speaker: one word of the verse, in the reader's voice. */
    onHearWord: (ayahNumber: Int, position: Int) -> Unit = { _, _ -> },
    /** What the speaker's press has to say, and what its pack weighs. */
    wordVoice: WordVoice = WordVoice.Ready,
    wordAudio: WordAudioState? = null,
    /** The sheet closed: a press still waiting on a pack is forgotten. */
    onWordClosed: () -> Unit = {},
    /** The surah's own name at a reference, for the word sheet's head. */
    surahNameOf: (Int) -> String = { "" },
    modifier: Modifier = Modifier,
) {
    // The stored place is the ayah, so the list opens on it and needs nothing
    // else. A place used to carry a pixel depth tagged with the width and font
    // scale it was measured in, and a rotation made the tag stale; the list
    // then corrected the stale depth by pulling the landed ayah to the top,
    // which the reader felt as a jump at the end of every drag (owner report:
    // the landscape reading stepped from ayah to ayah). An ayah cannot go
    // stale, so there is nothing to correct and nothing to move
    // (owner decision, 13.1).
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = AyahList.indexOf(ayahs, place),
    )
    // A place that arrives from elsewhere is also the place a screen reader
    // should hear. The focus request moves the reading's own focus to the
    // target ayah, and the callback announces its reference; the list can
    // only focus an item it has composed, so the request waits one frame
    // after the scroll (owner decision, 14.7).
    val jumpFocus = remember { FocusRequester() }
    var jumpAyah by remember { mutableStateOf<Int?>(null) }

    // The surah's introduction opens from its quiet door; once it is open,
    // a tap anywhere on the introduction itself, the paper, the ayahs, or
    // the closing line puts it away again, so the reader is never left
    // looking for the way out. Opened, the door is gone and the
    // introduction is its own close, spoken as Hide to TalkBack (owner
    // decision, 23.2). The state lives here rather than inside the opening
    // item because the taps that dismiss it land outside that item.
    var aboutOpen by rememberSaveable(surah.number) { mutableStateOf(false) }
    // The introduction is the list's first item, so reading down it spends
    // the list's own scroll offset: the index stays 0 while the document
    // fills the glass. Closing used to leave that offset behind, and the
    // shrink of the item spent it on the ayahs below, handing the reader
    // the ayah ten or fourteen rows down instead of the head they opened
    // from. The head is now asked for in the measure that drops the
    // introduction, and only while the opening is what the glass shows:
    // once the reader has scrolled past the introduction into the ayahs,
    // the item is above the anchor and its collapse moves nothing, so the
    // close leaves the ayah where they reached it, and a reference
    // followed from the introduction is not undone by a later tap. A pixel
    // offset would go stale on a rotation or a text size change while the
    // document is open, which is why the head is an item and not a depth
    // (owner decision, 23.3).
    val closeAbout: () -> Unit = {
        if (aboutOpen) {
            if (listState.firstVisibleItemIndex == 0) {
                listState.requestScrollToItem(0)
            }
            aboutOpen = false
        }
    }
    val dismissAbout: () -> Unit = {
        if (aboutOpen) closeAbout() else onBackgroundTap()
    }

    // The reader's place is written down when a drag of their own ends, and
    // never by a scroll the app started. The flag is cleared on the write, so
    // the jump that follows a drag cannot write a place of its own.
    var dragged by remember(surah.number) { mutableStateOf(false) }
    // The app's own scrolls are not the reader's thumb. A place that arrives,
    // the opening a surah is opened at, and the reciter's follow must not
    // spend the bar's distance or carry it away; the reader's drag is the
    // only scroll the bar answers (owner decision, 19.4).
    val programmatic = remember { mutableStateOf(false) }
    val consumeBar by rememberUpdatedState(onBarScroll)
    val barSettled by rememberUpdatedState(onBarSettled)
    val barScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (programmatic.value) return Offset.Zero
                val consumed = consumeBar(available.y)
                return Offset(0f, consumed)
            }
        }
    }
    // The reader's scroll stopped, fling and all: a bar left between its two
    // ends finishes the nearer one, the way a browser's toolbar does.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) barSettled()
        }
    }
    // Moving the page means reading, and reading wants the page: the bar's
    // scroll connection spends the drag's first distance on the bar, and the
    // menu goes with the page it hangs from. The place landing on screen is
    // not the reader moving the page, so a scroll the app started does not
    // spend the bar (owner decision, 19.4).
    LaunchedEffect(listState, surah.number) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                dragged = true
                onScrolled()
            }
        }
    }
    LaunchedEffect(listState, surah.number) {
        // The snapshot is the scroll's own state and the head ayah: the place
        // is written when a drag of the reader's stops, and the head is read
        // at that moment. The pixel offset is deliberately not part of it; it
        // is no longer part of the place either.
        snapshotFlow {
            listState.isScrollInProgress to listState.firstVisibleItemIndex
        }
            .collect { (scrolling, index) ->
                if (scrolling || !dragged) return@collect
                dragged = false
                // The place is the ayah at the head of the reading. The
                // opening item is not an ayah, so a settle on it writes the
                // surah's first one, which is where the reader is.
                AyahList.ayahAt(ayahs, index)?.let(onPlaceChanged)
            }
    }
    // The surah after this one is read when the reader is close to the end,
    // not when this one opens: a turn they may never make is not a whole
    // surah of rows to pay for, and the last screen is still read ahead of
    // the turn (owner decision, 19.9). The flag resets with the rows, so a
    // change of the reading's mode reads the next surah again.
    var readAheadAsked by remember(surah.number, rows) { mutableStateOf(false) }
    LaunchedEffect(listState, rows, surah.number, readAheadAsked) {
        if (readAheadAsked || rows.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .first { it >= rows.size - READ_AHEAD_AYAHS }
        readAheadAsked = true
        onReadAhead()
    }

    // A place that arrives from elsewhere moves the list, and a place the list
    // wrote itself never does, because it is already the ayah the list shows.
    // That is the whole of the rule, and it is arithmetic in
    // [AyahList.moveTarget]: a rotation, a text size, a translation toggle, or
    // a new phone can no longer land as a jump, because the place they hand
    // back is an ayah the list already stands on.
    LaunchedEffect(place, surah.number, ayahs) {
        val target = AyahList.moveTarget(ayahs, place, listState.firstVisibleItemIndex)
            ?: return@LaunchedEffect
        programmatic.value = true
        try {
            listState.scrollToItem(target)
        } finally {
            programmatic.value = false
        }
        jumpAyah = place
        rows.firstOrNull { it.ayah.number == place }?.ayah?.let(onJumped)
    }

    LaunchedEffect(jumpAyah, ayahs) {
        if (jumpAyah == null) return@LaunchedEffect
        withFrameNanos { }
        runCatching { jumpFocus.requestFocus() }
        jumpAyah = null
    }

    // Opening a surah from Browse lands on its top: the opening item, which
    // is not an ayah and so has no place of its own. The request is its own
    // effect rather than a branch of the place above, because the place must
    // not re-run when the request is cleared: that is what would pull the
    // reader back down to the first ayah the moment the opening appeared.
    LaunchedEffect(listState, ayahs, startAtOpening) {
        if (!startAtOpening || ayahs.isEmpty()) return@LaunchedEffect
        programmatic.value = true
        try {
            listState.scrollToItem(0)
        } finally {
            programmatic.value = false
        }
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
            programmatic.value = true
            try {
                if (reducedMotion) listState.scrollToItem(target)
                else listState.animateScrollToItem(target)
            } finally {
                programmatic.value = false
            }
        }
    }

    // The reading's words stand on the page, on every glass. The list keeps
    // that page's margin less the room a wash reaches past its text, so the
    // ayah and its wash stand on the same two lines whether the glass is a
    // phone (where the page is the whole window) or the ten inch screen
    // (where the page is 600 dp and the rest is margin), and the paper around
    // them stays full width to answer a tap with the chrome and a drag with
    // the scroll.
    //
    // The margin is the page's, read once here, and not a count of
    // characters. A reader who turns the text down gets more lines and not a
    // narrower column, and a reader on a wide screen gets the page the seven
    // inch tablet already reads instead of a line as long as the glass
    // (owner decision, 12.7).
    //
    // The two sides are read apart because a landscape glass can carry a
    // camera on one of them: a word under a lens is a word the reader cannot
    // read, so the camera is that side's floor while the wash still reaches
    // under it (owner decision, 22.8).
    val (marginStart, marginEnd) = rememberPageSideMargins()
    val sideStart = marginStart - Space.WashInset
    val sideEnd = marginEnd - Space.WashInset
    LazyColumn(
        state = listState,
        // A viewport ahead and half of one behind: the row the reader is about
        // to reach is already built, and the row they just passed is still
        // there when they turn back (owner decision, 21.6).
        cacheWindow = ReadingCacheWindow,
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = pageDescription }
            .nestedScroll(barScroll)
            .pointerInput(Unit) {
                // The paper around the text brings the chrome, and so does the
                // text: the whole page is the page's own tap, and an ayah's
                // actions are its number, never the verse around it.
                detectTapGestures(onTap = { dismissAbout() })
            },
        contentPadding = PaddingValues(
            start = sideStart,
            end = sideEnd,
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
                onExpandedChange = { open -> if (open) aboutOpen = true else closeAbout() },
                onOpenReference = onOpenReference,
                showWordsDoor = !wordsInstalled,
                onAddWords = onAddWords,
                metrics = metrics,
            )
            // The reading's whole navigation is one tap on the paper, and
            // nothing on a bare page says so. The bar is the page's head,
            // so the sentence that taught the tap retired with the tap
            // itself (owner decision, 19.4).
        }
        items(
            count = rows.size,
            // The ayah's own number is the key: it is unique in the Book, it
            // is what the list already holds, and it costs no string per item
            // per composition (owner decision, 19.8).
            key = { rows[it].ayah.number },
            // One slot type for every ayah, so a block that scrolls out
            // is the block the next one reuses; the opening and the
            // closing line are their own kinds and never borrow an
            // ayah's measured composition
            // (owner decision, 10.0).
            contentType = { "ayah" },
        ) { index ->
            val row = rows[index]
            AyahBlock(
                row = row,
                hafs = hafs,
                metrics = metrics,
                isSelected = selectedAyah == row.ayah.number,
                playingAyah = playingAyah,
                // The word is handed down as a state and read in the draw,
                // only for the ayah being recited: a tick that moves the wash
                // invalidates that one line's draw and neither the item's
                // composition nor the page around it (owner decision, 19.9).
                // Every other block keeps a stable null.
                playedWord = if (playingAyah == row.ayah.number) playedWord else null,
                reveal = reveal?.takeIf { it.ayah == row.ayah.number },
                showWordAid = wordAid ||
                    (reveal?.wordByWord == true && reveal.ayah == row.ayah.number),
                noted = row.ayah.number in notedAyahs,
                saved = row.ayah.number in savedAyahs,
                menuOpen = menuAyah == row.ayah.number,
                focusRequester = if (jumpAyah == row.ayah.number) jumpFocus else null,
                onMenu = onAyahMenu,
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
                onWord = { pressed ->
                    onWord(pressed)
                },
            )
        }
        item(key = "end-${surah.number}", contentType = "end") {
            SurahEnd(
                surah = surah,
                nextSurahName = nextSurahName,
                onNextSurah = onNextSurah,
            )
            if (reminderOffer) {
                ReadingReminderOffer(
                    onTurnOn = onReminderOn,
                    onNotNow = onReminderNotNow,
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

    word?.let { open ->
        WordSheet(
            word = open,
            surahNameOf = surahNameOf,
            metrics = metrics,
            hafs = hafs,
            onDismiss = {
                onWordClosed()
                onWord(null)
            },
            onHear = { onHearWord(open.ayahNumber, open.position) },
            hearing = open.ayahNumber == hearingAyah &&
                open.position == hearingWord,
            voice = wordVoice,
            voiceAudio = wordAudio,
        )
    }
}

/** A quiet door to the packs the reader does not have yet. */
