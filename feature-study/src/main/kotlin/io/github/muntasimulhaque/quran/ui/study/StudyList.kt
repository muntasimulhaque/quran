package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.AyahList
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.Footnote
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.WordMeaning
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.StudyRow
import io.github.muntasimulhaque.quran.feature.study.R
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The study reading of one surah: its opening, then a continuous scroll
 * through every ayah to the end of the surah, and a quiet closing line that
 * offers the next surah.
 *
 * A surah is the unit the Quran itself gives, and readers read it as one:
 * this view ends where the surah ends, instead of sliding into the next one
 * without a word. Tapping the paper brings the chrome; a long press asks
 * about the ayah under the finger.
 */
@Composable
fun StudyList(
    content: ContentDatabase,
    surah: Surah,
    loadRows: suspend () -> List<StudyRow>,
    settings: AppSettings,
    hasTranslation: Boolean,
    nextSurahName: String?,
    selected: Ayah?,
    playingAyah: Int?,
    playingWord: Int?,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    onNextSurah: (Int) -> Unit,
    onAddContent: () -> Unit,
    onPlaceChanged: (Int) -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pageDescription = stringResource(R.string.study_page_description)
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    // The whole surah arrives at once, so every item has its text before the
    // list is measured: the reader's place lands exactly, and nothing grows or
    // shifts under them while they read. Until the rows for *this* surah are
    // in hand the paper is shown, never the surah the reader just left: a
    // stale list would be measured with the new surah's name on it, and a
    // scroll through it would write a place in the wrong surah.
    val loaded by produceState<Pair<Int, List<StudyRow>>?>(
        initialValue = null,
        surah.number,
        settings.translationPacks,
        settings.wordByWord,
    ) { value = surah.number to loadRows() }
    val rows = loaded?.takeIf { it.first == surah.number }?.second.orEmpty()
    val ayahs = remember(rows) { rows.map { it.ayah.number } }
    var footnote by remember { mutableStateOf<OpenFootnote?>(null) }
    if (ayahs.isEmpty()) {
        // The paper is already there; the text is a frame away.
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    StudyRows(
        content = content,
        surah = surah,
        rows = rows,
        ayahs = ayahs,
        settings = settings,
        hasTranslation = hasTranslation,
        nextSurahName = nextSurahName,
        selected = selected,
        playingAyah = playingAyah,
        playingWord = playingWord,
        hafs = hafs,
        pageDescription = pageDescription,
        onAyah = onAyah,
        onBackgroundTap = onBackgroundTap,
        onNextSurah = onNextSurah,
        onAddContent = onAddContent,
        onPlaceChanged = onPlaceChanged,
        contentPaddingTop = contentPaddingTop,
        contentPaddingBottom = contentPaddingBottom,
        footnote = footnote,
        onFootnote = { footnote = it },
        modifier = modifier,
    )
}

/** The surah's rows, drawn and driven once the text is on hand. */
@Composable
private fun StudyRows(
    content: ContentDatabase,
    surah: Surah,
    rows: List<StudyRow>,
    ayahs: List<Int>,
    settings: AppSettings,
    hasTranslation: Boolean,
    nextSurahName: String?,
    selected: Ayah?,
    playingAyah: Int?,
    playingWord: Int?,
    hafs: FontFamily,
    pageDescription: String,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    onNextSurah: (Int) -> Unit,
    onAddContent: () -> Unit,
    onPlaceChanged: (Int) -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    footnote: OpenFootnote?,
    onFootnote: (OpenFootnote?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = AyahList.indexOf(ayahs, settings.ayah),
    )

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
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collect { (scrolling, index) ->
                if (scrolling || !dragged) return@collect
                dragged = false
                AyahList.ayahAt(ayahs, index)?.let(onPlaceChanged)
            }
    }

    // The reader's place moves the list, exactly, and the list never moves
    // itself. The place is an ayah, so a round trip through the list has to
    // land on the same item; anything less leaves the place behind.
    LaunchedEffect(settings.ayah, surah.number, ayahs) {
        val target = AyahList.indexOf(ayahs, settings.ayah)
        if (target != listState.firstVisibleItemIndex) listState.scrollToItem(target)
    }

    // The playing ayah comes back into view when the reciter moves on.
    LaunchedEffect(playingAyah, ayahs) {
        val ayahNumber = playingAyah ?: return@LaunchedEffect
        if (!settings.followReciter) return@LaunchedEffect
        val target = AyahList.indexOf(ayahs, ayahNumber)
        val current = listState.firstVisibleItemIndex
        if (target < current || target > current + 3) listState.animateScrollToItem(target)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = pageDescription }
            .pointerInput(Unit) {
                // The paper around the text brings the chrome; the text itself
                // belongs to its ayah, and every ayah consumes its own taps.
                detectTapGestures(onTap = { onBackgroundTap() })
            },
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPaddingTop,
            bottom = contentPaddingBottom,
        ),
    ) {
        item(key = "surah-${surah.number}") {
            SurahOpening(
                surah = surah,
                content = content,
                onBackgroundTap = onBackgroundTap,
            )
            if (!hasTranslation) {
                AddContent(
                    text = stringResource(R.string.study_add_translation),
                    onClick = onAddContent,
                )
            }
        }
        items(rows.size, key = { "ayah-${rows[it].ayah.number}" }) { index ->
            val row = rows[index]
            AyahBlock(
                row = row,
                hafs = hafs,
                settings = settings,
                wordByWord = settings.wordByWord,
                isSelected = selected?.number == row.ayah.number,
                playingAyah = playingAyah,
                playingWord = playingWord,
                onAyah = onAyah,
                onBackgroundTap = onBackgroundTap,
                onFootnote = { number ->
                    val note = row.translations.asSequence()
                        .flatMap { it.text.footnotes.asSequence() }
                        .firstOrNull { it.number == number }
                    if (note != null) {
                        onFootnote(
                            OpenFootnote(note, "${row.ayah.surah}:${row.ayah.ayah}"),
                        )
                    }
                },
            )
        }
        item(key = "end-${surah.number}") {
            SurahEnd(
                surah = surah,
                nextSurahName = nextSurahName,
                onNextSurah = onNextSurah,
                onBackgroundTap = onBackgroundTap,
            )
        }
    }

    footnote?.let { open ->
        FootnoteSheet(
            footnote = open.note,
            surahName = surah.nameSimple,
            reference = open.reference,
            onDismiss = { onFootnote(null) },
        )
    }
}

/** A footnote the reader opened, with the ayah it belongs to. */
private data class OpenFootnote(val note: Footnote, val reference: String)

/** The footnote of one ayah, opened from the marker the reader tapped. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FootnoteSheet(
    footnote: Footnote,
    surahName: String,
    reference: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 34.dp),
        ) {
            Text(
                text = stringResource(R.string.study_footnote_title, footnote.number, surahName, reference),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = footnote.text,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** A quiet door to the packs the reader does not have yet. */
@Composable
private fun AddContent(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.study_action_add),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SurahOpening(
    surah: Surah,
    content: ContentDatabase?,
    onBackgroundTap: () -> Unit,
) {
    var expanded by rememberSaveable(surah.number) { mutableStateOf(false) }
    val info by produceState<String?>(initialValue = null, surah.number, expanded) {
        value = if (expanded) {
            withContext(Dispatchers.IO) { content?.surahInfo(surah.number) }
        } else {
            null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 34.dp, bottom = 14.dp)
            .clickable(onClick = onBackgroundTap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = surah.nameArabic,
            style = TextStyle(
                fontFamily = Amiri,
                fontSize = 34.sp,
                color = LocalPagePalette.current.ornament,
            ),
        )
        Text(
            text = surah.nameSimple,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = stringResource(
                R.string.study_surah_meta,
                if (surah.revelationPlace.equals("makkah", true)) {
                    stringResource(R.string.study_place_makkah)
                } else {
                    stringResource(R.string.study_place_madinah)
                },
                surah.versesCount,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp),
        )
        if (surah.number != 1 && surah.bismillahPre) {
            Text(
                text = BASMALLAH,
                style = TextStyle(
                    fontFamily = Amiri,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            )
        }
        if (info != null) {
            Text(
                text = RichText.plain(info.orEmpty()),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                maxLines = if (expanded) 40 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
        Text(
            text = if (expanded) {
                stringResource(R.string.study_hide)
            } else {
                stringResource(R.string.study_about_surah)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(50))
                .clickable {
                    if (info == null) expanded = true else expanded = !expanded
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** The end of a surah, and the door to the next one. */
@Composable
private fun SurahEnd(
    surah: Surah,
    nextSurahName: String?,
    onNextSurah: (Int) -> Unit,
    onBackgroundTap: () -> Unit,
) {
    val next = surah.number + 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 30.dp, bottom = 20.dp)
            .clickable(onClick = onBackgroundTap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.22f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        )
        Text(
            text = stringResource(R.string.study_surah_complete, surah.nameSimple),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 16.dp),
        )
        if (next <= 114 && nextSurahName != null) {
            Text(
                text = stringResource(R.string.study_continue_to, nextSurahName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onNextSurah(next) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        } else if (next > 114) {
            Text(
                text = stringResource(R.string.study_end_of_quran),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AyahBlock(
    row: StudyRow,
    hafs: FontFamily,
    settings: AppSettings,
    wordByWord: Boolean,
    isSelected: Boolean,
    playingAyah: Int?,
    playingWord: Int?,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    onFootnote: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val palette = LocalPagePalette.current
    val playing = row.ayah.number == playingAyah
    val wash = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        playing -> palette.highlight.copy(alpha = palette.highlight.alpha * 0.55f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    // The padding belongs to the paper, so the gaps between ayahs still
    // answer a tap with the reading chrome.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(wash)
                .combinedClickable(
                    onClick = onBackgroundTap,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAyah(row.ayah)
                    },
                )
                .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
            Text(
                text = arabic(row, playing, playingWord, hafs, palette.highlight),
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = settings.arabicSp.sp,
                    lineHeight = settings.arabicLineSp.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth(),
            )
            if (wordByWord && row.meanings.any { it.meaning != null }) {
                WordByWord(row.meanings, hafs, settings)
            }
            // More than one translation may be on, and each is drawn in its
            // own column, named, so the reader always knows whose reading
            // they are looking at.
            row.translations.forEach { line ->
                if (row.translations.size > 1) {
                    Text(
                        text = line.packName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                TranslationBody(
                    runs = remember(line.text.text) { RichText.footnotes(line.text.text) },
                    modifier = Modifier.padding(top = if (row.translations.size > 1) 4.dp else 12.dp),
                    sizeSp = settings.translationSp,
                    lineSp = settings.translationLineSp,
                    arabicSp = settings.arabicSp * 0.8f,
                    onFootnote = onFootnote,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = "${row.ayah.surah}:${row.ayah.ayah}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                )
            }
        }
    }
}

/** The ayah as whole words, the recited word washed in the accent color. */
@Composable
private fun arabic(
    row: StudyRow,
    playing: Boolean,
    playingWord: Int?,
    hafs: FontFamily,
    wash: Color,
): AnnotatedString {
    val words = row.words
    if (words.isEmpty()) return AnnotatedString(row.ayah.text)
    return buildAnnotatedString {
        words.forEachIndexed { index, word ->
            if (index > 0) append(' ')
            if (playing && word.position == playingWord) {
                withStyle(
                    SpanStyle(
                        background = wash,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                ) { append(word.text) }
            } else {
                append(word.text)
            }
        }
    }
}

private const val BASMALLAH = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064e\u0647\u0650 " +
    "\u0671\u0644\u0631\u0651\u064e\u062d\u0652\u0645\u064e\u0670\u0646\u0650 \u0671\u0644\u0631\u0651\u064e\u062d\u0650\u064a\u0645\u0650"

/**
 * Word by word, under the ayah: each word with the meaning the word list
 * gives it. This is the reading aid for a reader who is learning the Arabic,
 * and it is off until they ask for it.
 */
@Composable
private fun WordByWord(meanings: List<WordMeaning>, hafs: FontFamily, settings: AppSettings) {
    // Arabic reads right to left, so the words wrap that way too.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        meanings.forEach { meaning ->
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = meaning.word,
                    style = TextStyle(
                        fontFamily = hafs,
                        fontSize = settings.wordsSp.sp,
                        lineHeight = (settings.wordsSp * 1.8f).sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                Text(
                    text = meaning.meaning.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                )
            }
        }
    }
    }
}
