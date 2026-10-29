package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.theme.rememberHafs

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
    /** The search arrival to reveal, if it belongs to this surah. */
    reveal: ReadingReveal? = null,
    /** The reader's own word-by-word switch, before any arrival turns it on. */
    wordAid: Boolean = metrics.wordByWord,
    /** The reader's place, which moves the list and never the other way round. */
    place: Int,
    /** How far into that ayah the reader was: a long ayah is many screens. */
    placeOffset: Int,
    /** The width and font scale the depth was measured in; 0 when unknown. */
    placeOffsetWidth: Int = 0,
    placeOffsetFontScale: Float = 0f,
    hasTranslation: Boolean,
    /** The reader's language: the reading reads it, and so does the opening. */
    contentLanguage: String,
    nextSurahName: String?,
    selectedAyah: Int?,
    playingAyah: Int?,
    /** The word being recited, read only by the ayah being heard. */
    playedWord: State<Int?>,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    onScrolled: () -> Unit,
    onNextSurah: (Int) -> Unit,
    onAddContent: () -> Unit,
    onPlaceChanged: (Int, Int, Int, Float) -> Unit,
    /** True when the reader opened this surah at its top; answered once. */
    startAtOpening: Boolean,
    onOpeningReached: () -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    modifier: Modifier = Modifier,
) {
    val pageDescription = stringResource(R.string.reading_page_description)
    val hafs = rememberHafs()
    val effective = remember(metrics, reveal) {
        metrics.copy(
            wordByWord = metrics.wordByWord || reveal?.wordByWord == true,
            showTranslation = metrics.showTranslation || reveal?.showTranslation == true,
        )
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
        effective.translationPacks,
        effective.wordByWord,
    ) { value = surah.number to loadRows() }
    // The copy read ahead in the surah before this one. It is this surah's
    // own, checked against the surah's number, so a stale list can never be
    // measured under the new surah's name; and it is there in the very frame
    // the surah changes, because it is read during composition rather than
    // from an effect. That is what makes the turn at the end of a surah land
    // on text instead of on the paper.
    val ahead = remember(
        content,
        surah.number,
        effective.wordByWord,
        effective.translationPacks,
        contentLanguage,
    ) {
        cachedRows(surah.number)
    }
    val rows = ahead ?: loaded?.takeIf { it.first == surah.number }?.second.orEmpty()
    val ayahs = remember(rows) { rows.map { it.ayah.number } }

    // The surah after this one is read while the reader is still in this one,
    // so the turn at its end has its text on hand when it comes.
    LaunchedEffect(surah.number, content, effective.wordByWord) { onReadAhead(surah.number) }

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
        metrics = effective,
        reveal = reveal,
        wordAid = wordAid,
        place = place,
        placeOffset = placeOffset,
        placeOffsetWidth = placeOffsetWidth,
        placeOffsetFontScale = placeOffsetFontScale,
        hasTranslation = hasTranslation,
        contentLanguage = contentLanguage,
        nextSurahName = nextSurahName,
        selectedAyah = selectedAyah,
        playingAyah = playingAyah,
        playedWord = playedWord,
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
