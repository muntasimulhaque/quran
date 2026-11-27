package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.data.RecitedWord
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.WordAudioState
import io.github.muntasimulhaque.quran.data.WordVoice
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.theme.rememberHafs

/**
 * Everything the reading of one surah draws from that the screen owns: the
 * surah and the reader's place in it, the sizes and the language, and the
 * answers a search or a word sheet leaves on the page.
 *
 * The list took these as separate parameters, and its call site was a page of
 * named arguments in which a value that belongs to the reading stood beside a
 * callback that belongs to the screen. What the reading is showing is one
 * value now (owner decision, 19.6).
 */
data class ReadingListState(
    val surah: Surah,
    val metrics: ReadingMetrics,
    /** The search arrival to reveal, if it belongs to this surah. */
    val reveal: ReadingReveal? = null,
    /** The reader's own word-by-word switch, before any arrival turns it on. */
    val wordAid: Boolean = metrics.wordByWord,
    /** What the speaker's press has to say, and what its pack weighs. */
    val wordVoice: WordVoice = WordVoice.Ready,
    val wordAudio: WordAudioState? = null,
    /** The reader's place: the ayah, which the list shows and never corrects. */
    val place: Int,
    /** True when the reader's own word list is on the device. */
    val wordsInstalled: Boolean = true,
    /** The reader's language: the reading reads it, and so does the opening. */
    val contentLanguage: String,
    val nextSurahName: String?,
    /**
     * The ayah whose ground the page paints: the one whose menu is open, or
     * the one whose note is open over it.
     */
    val selectedAyah: Int?,
    /** The ayah whose menu is open, so its door's turn says which way. */
    val menuAyah: Int? = null,
    /** The ayahs the reader wrote a note on, so their head can say so. */
    val notedAyahs: Set<Int> = emptySet(),
    /** The ayahs the reader has pressed Save on, so their doors say so. */
    val savedAyahs: Set<Int> = emptySet(),
    val playingAyah: Int?,
    /** The word being recited, read only by the ayah being heard. */
    val playedWord: State<RecitedWord?>,
    /** The ayah whose word is sounding, when the sound is a word sample. */
    val hearingAyah: Int? = null,
    /** The sounding word's position in its ayah, when the sound is a sample. */
    val hearingWord: Int? = null,
    /** True while the daily reminder offer stands at this surah's end. */
    val reminderOffer: Boolean = false,
    /** True when the reader opened this surah at its top; answered once. */
    val startAtOpening: Boolean,
    val contentPaddingTop: Dp,
    val contentPaddingBottom: Dp,
)

/**
 * What the reading of one surah asks the screen to do: read the rows, read
 * ahead, open a door, hold the bar, and take the reader on to another surah.
 *
 * The callbacks arrive as one value, so the call site says what the reading is
 * and what it may do, in that order (owner decision, 19.6).
 */
data class ReadingListActions(
    val loadRows: suspend () -> List<ReadingRow>,
    /**
     * The rows already on hand for a surah, when there are any: the copy the
     * app read ahead while the reader was still in the surah before it. Read
     * here, during composition, so a turn at the end of a surah lands on text
     * that is already measured rather than on a blank frame.
     */
    val cachedRows: (Int) -> List<ReadingRow>?,
    /** Reads the surah after this one while the reader is still in it. */
    val onReadAhead: (Int) -> Unit,
    /** The word sheet's speaker: one word of the verse, in the reader's voice. */
    val onHearWord: (ayahNumber: Int, position: Int) -> Unit = { _, _ -> },
    /** The word sheet closed: a press still waiting on a pack is forgotten. */
    val onWordClosed: () -> Unit = {},
    /** The surah's own name at a reference, for the word sheet's head. */
    val surahNameOf: (Int) -> String = { "" },
    val onAddWords: () -> Unit = {},
    val onAyahMenu: (Ayah, IntRect) -> Unit,
    val onBackgroundTap: () -> Unit,
    val onScrolled: () -> Unit,
    /**
     * What a scroll spends on the reading's bar before the page moves, and
     * what the bar took. The bar's own arithmetic lives in the app beside
     * the screen; this list is the reader's finger's only way to it.
     */
    val onBarScroll: (Float) -> Float = { 0f },
    /** The reader's scroll stopped with the bar between its two ends. */
    val onBarSettled: () -> Unit = {},
    val onNextSurah: (Int) -> Unit,
    /** Opens an internal reference the surah's introduction links to. */
    val onOpenReference: (surah: Int, ayah: Int?) -> Unit = { _, _ -> },
    val onReminderOn: () -> Unit = {},
    val onReminderNotNow: () -> Unit = {},
    val onPlaceChanged: (Int) -> Unit,
    /** The ayah a jump landed on, so the reading can announce the place. */
    val onJumped: (Ayah) -> Unit = {},
    val onOpeningReached: () -> Unit,
)

/**
 * The reading of one surah: its opening, then a continuous scroll
 * through every ayah to the end of the surah, and a quiet closing line that
 * offers the next surah.
 *
 * A surah is the unit the Quran itself gives, and readers read it as one:
 * this view ends where the surah ends, instead of sliding into the next one
 * without a word. The number at each ayah's head is its own door: the menu it
 * opens holds the ayah's actions, and the paper around it brings the chrome
 * (owner decision, 13.3).
 */
@Composable
fun ReadingList(
    content: ContentDatabase,
    modifier: Modifier = Modifier,
    /** What the reading is showing. */
    state: ReadingListState,
    /** What the reading may ask the screen to do. */
    actions: ReadingListActions,
) {
    val pageDescription = stringResource(R.string.reading_page_description)
    val hafs = rememberHafs()
    val effective = remember(state.metrics, state.reveal) {
        state.metrics.copy(
            wordByWord = state.metrics.wordByWord || state.reveal?.wordByWord == true,
            showTranslation = state.metrics.showTranslation || state.reveal?.showTranslation == true,
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
        state.surah.number,
        effective.translationPacks,
        effective.wordByWord,
        // The reciter is a key because the word sheet's speaker is drawn
        // from the audio it names, and a surah downloaded under one voice is
        // not on the device under another (owner decision, 17.7).
        effective.recitation,
    ) { value = state.surah.number to actions.loadRows() }
    // The copy read ahead in the surah before this one. It is this surah's
    // own, checked against the surah's number, so a stale list can never be
    // measured under the new surah's name; and it is there in the very frame
    // the surah changes, because it is read during composition rather than
    // from an effect. That is what makes the turn at the end of a surah land
    // on text instead of on the paper.
    val ahead = remember(
        content,
        state.surah.number,
        effective.wordByWord,
        effective.translationPacks,
        state.contentLanguage,
        effective.recitation,
    ) {
        actions.cachedRows(state.surah.number)
    }
    val rows = ahead ?: loaded?.takeIf { it.first == state.surah.number }?.second.orEmpty()
    val ayahs = remember(rows) { rows.map { it.ayah.number } }

    var footnote by remember { mutableStateOf<OpenFootnote?>(null) }
    // The word the reader pressed and held. It is a sheet of its own, at the
    // reading's level rather than inside the ayah, so the page never grows
    // under the finger and the word survives the item being recycled (owner
    // decision, 19.0).
    var word by remember { mutableStateOf<OpenWord?>(null) }
    if (ayahs.isEmpty()) {
        // The paper is already there; the text is a frame away.
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    ReadingRows(
        content = content,
        surah = state.surah,
        rows = rows,
        ayahs = ayahs,
        metrics = effective,
        reveal = state.reveal,
        wordAid = state.wordAid,
        place = state.place,
        wordsInstalled = state.wordsInstalled,
        onAddWords = actions.onAddWords,
        contentLanguage = state.contentLanguage,
        nextSurahName = state.nextSurahName,
        selectedAyah = state.selectedAyah,
        menuAyah = state.menuAyah,
        notedAyahs = state.notedAyahs,
        savedAyahs = state.savedAyahs,
        playingAyah = state.playingAyah,
        playedWord = state.playedWord,
        hearingAyah = state.hearingAyah,
        hearingWord = state.hearingWord,
        hafs = hafs,
        pageDescription = pageDescription,
        onAyahMenu = actions.onAyahMenu,
        onBackgroundTap = actions.onBackgroundTap,
        onScrolled = actions.onScrolled,
        onBarScroll = actions.onBarScroll,
        onBarSettled = actions.onBarSettled,
        // The next surah is read when the reader is close to the end of this
        // one, where [ReadingRows] asks for it (owner decision, 19.9).
        onReadAhead = { actions.onReadAhead(state.surah.number) },
        onNextSurah = actions.onNextSurah,
        onOpenReference = actions.onOpenReference,
        reminderOffer = state.reminderOffer,
        onReminderOn = actions.onReminderOn,
        onReminderNotNow = actions.onReminderNotNow,
        onPlaceChanged = actions.onPlaceChanged,
        onJumped = actions.onJumped,
        startAtOpening = state.startAtOpening,
        onOpeningReached = actions.onOpeningReached,
        contentPaddingTop = state.contentPaddingTop,
        contentPaddingBottom = state.contentPaddingBottom,
        footnote = footnote,
        onFootnote = { footnote = it },
        word = word,
        onWord = { next -> word = next },
        onHearWord = actions.onHearWord,
        wordVoice = state.wordVoice,
        wordAudio = state.wordAudio,
        onWordClosed = actions.onWordClosed,
        surahNameOf = actions.surahNameOf,
        modifier = modifier,
    )
}
