package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Dp
import io.github.muntasimulhaque.quran.core.AyahList
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.AddDoor
import io.github.muntasimulhaque.quran.ui.kit.rememberPageMargin
import io.github.muntasimulhaque.quran.ui.kit.rememberReducedMotion
import io.github.muntasimulhaque.quran.ui.theme.Space

/** The surah's rows, drawn and driven once the text is on hand. */
@Composable
internal fun ReadingRows(
    content: ContentDatabase,
    /** The surface's selection clearer, so one touch puts the menu away. */
    selectionClearer: SelectionClearer,
    surah: Surah,
    rows: List<ReadingRow>,
    ayahs: List<Int>,
    metrics: ReadingMetrics,
    reveal: ReadingReveal?,
    /** The reader's own word-by-word switch, before any arrival turns it on. */
    wordAid: Boolean,
    place: Int,
    hasTranslation: Boolean,
    contentLanguage: String,
    nextSurahName: String?,
    selectedAyah: Int?,
    /** The ayah whose menu is open, so its own door can say so. */
    menuAyah: Int?,
    /** The ayahs the reader has pressed Save on, so their doors say so. */
    savedAyahs: Set<Int> = emptySet(),
    playingAyah: Int?,
    /** The word being recited, read inside the one ayah being heard. */
    playedWord: State<Int?>,
    hafs: FontFamily,
    pageDescription: String,
    onAyahMenu: (Ayah, IntRect) -> Unit,
    onBackgroundTap: () -> Unit,
    onScrolled: () -> Unit,
    onNextSurah: (Int) -> Unit,
    onAddContent: () -> Unit,
    onPlaceChanged: (Int) -> Unit,
    startAtOpening: Boolean,
    onOpeningReached: () -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    footnote: OpenFootnote?,
    onFootnote: (OpenFootnote?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The stored place is the ayah, so the list opens on it and needs nothing
    // else. A place used to carry a pixel depth tagged with the width and font
    // scale it was measured in, and a rotation made the tag stale; the list
    // then corrected the stale depth by pulling the landed ayah to the top,
    // which the reader felt as a jump at the end of every drag (owner report:
    // the landscape reading stepped from ayah to ayah). An ayah cannot go
    // stale, so there is nothing to correct and nothing to move
    // (owner decision, this session).
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = AyahList.indexOf(ayahs, place),
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

    // A place that arrives from elsewhere moves the list, and a place the list
    // wrote itself never does, because it is already the ayah the list shows.
    // That is the whole of the rule, and it is arithmetic in
    // [AyahList.moveTarget]: a rotation, a text size, a translation toggle, or
    // a new phone can no longer land as a jump, because the place they hand
    // back is an ayah the list already stands on.
    LaunchedEffect(place, surah.number, ayahs) {
        val target = AyahList.moveTarget(ayahs, place, listState.firstVisibleItemIndex)
            ?: return@LaunchedEffect
        listState.scrollToItem(target)
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
    // (owner decision, this session).
    val side = rememberPageMargin() - Space.WashInset
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
            DisableSelection {
                SurahOpening(
                    surah = surah,
                    content = content,
                    language = contentLanguage,
                    expanded = aboutOpen,
                    onExpandedChange = { aboutOpen = it },
                    onPaperTap = { if (!aboutOpen) onBackgroundTap() },
                    metrics = metrics,
                )
                // The door asks for a translation only while translations
                // are something the reading draws at all: a reader who hid
                // them in Settings is not looking for one here.
                if (metrics.showTranslation && !hasTranslation) {
                    AddDoor(
                        text = stringResource(R.string.reading_add_translation),
                        action = stringResource(R.string.reading_action_add),
                        onClick = onAddContent,
                        modifier = Modifier.padding(vertical = Space.Tight),
                    )
                }
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
                selectionClearer = selectionClearer,
                metrics = metrics,
                isSelected = selectedAyah == row.ayah.number,
                playingAyah = playingAyah,
                // The word is read here, inside this item, and only for the
                // ayah being recited: a tick that moves the wash invalidates
                // this one item and leaves the page around it alone (owner
                // decision, this session). Every other block keeps a stable
                // null, so a word change never rebuilds its AnnotatedString.
                playingWord = if (playingAyah == row.ayah.number) playedWord.value else null,
                reveal = reveal?.takeIf { it.ayah == row.ayah.number },
                showWordAid = wordAid ||
                    (reveal?.wordByWord == true && reveal.ayah == row.ayah.number),
                isMenuOpen = menuAyah == row.ayah.number,
                saved = row.ayah.number in savedAyahs,
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
            )
        }
        item(key = "end-${surah.number}", contentType = "end") {
            DisableSelection {
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
