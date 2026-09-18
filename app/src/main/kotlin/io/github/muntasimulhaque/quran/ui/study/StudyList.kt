package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
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
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import io.github.muntasimulhaque.quran.ui.StudyItem
import io.github.muntasimulhaque.quran.ui.StudyRow
import io.github.muntasimulhaque.quran.ui.rich.FootnoteList
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.MushafHighlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The study reading: one continuous scroll through the whole Quran, ayah
 * after ayah, from Al-Fatihah to An-Nas, with surah openings like a printed
 * study edition. Nothing is paginated here, because a translation is read as
 * a book, and a page break in the middle of an ayah is a page break in the
 * middle of a thought.
 */
@Composable
fun StudyList(
    viewModel: ReaderViewModel,
    selected: Ayah?,
    playback: PlaybackUiState,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    modifier: Modifier = Modifier,
) {
    val settings = viewModel.settings
    val items = viewModel.studyItems
    val context = androidx.compose.ui.platform.LocalContext.current
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.studyIndexOf(settings.ayah),
    )

    // The reader's place is written down when the scroll rests.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collect { (scrolling, index) ->
                if (!scrolling) {
                    val item = items.getOrNull(index)
                    if (item is StudyItem.AyahItem) viewModel.onStudySettled(item.header.number)
                }
            }
    }

    // A jump from a sheet moves the list; the list never moves itself.
    LaunchedEffect(settings.ayah) {
        val target = viewModel.studyIndexOf(settings.ayah)
        val current = listState.firstVisibleItemIndex
        if (target < current || target > current + 2) {
            listState.scrollToItem(target)
        }
    }

    // The playing ayah comes back into view when the reciter moves on.
    LaunchedEffect(playback.ayahNumber) {
        val ayahNumber = playback.ayahNumber ?: return@LaunchedEffect
        if (!settings.followReciter) return@LaunchedEffect
        val target = viewModel.studyIndexOf(ayahNumber)
        val current = listState.firstVisibleItemIndex
        if (target < current || target > current + 3) listState.animateScrollToItem(target)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Study page" }
            .pointerInput(Unit) {
                // The paper around the text brings the chrome; the text itself
                // belongs to its ayah, and every ayah consumes its own taps.
                detectTapGestures(onTap = { onBackgroundTap() })
            },
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = contentPaddingTop, bottom = contentPaddingBottom),
    ) {
        items(
            count = items.size,
            key = { index ->
                when (val item = items[index]) {
                    is StudyItem.Header -> "s${item.surah.number}"
                    is StudyItem.AyahItem -> "a${item.header.number}"
                }
            },
        ) { index ->
            when (val item = items[index]) {
                is StudyItem.Header -> SurahOpening(
                    surah = item.surah,
                    content = viewModel.content,
                    onBackgroundTap = onBackgroundTap,
                )
                is StudyItem.AyahItem -> {
                    val number = item.header.number
                    val row by produceState<StudyRow?>(initialValue = null, number, settings.translationPack) {
                        value = withContext(Dispatchers.IO) {
                            viewModel.studyRow(number, settings.translationPack)
                        }
                    }
                    row?.let {
                        AyahBlock(
                            row = it,
                            hafs = hafs,
                            textSize = settings.textSize,
                            showFootnotes = settings.showFootnotes,
                            isSelected = selected?.number == number,
                            playingAyah = playback.ayahNumber,
                            playingWord = playback.wordPosition,
                            onAyah = onAyah,
                            onBackgroundTap = onBackgroundTap,
                        )
                    }
                }
            }
        }
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
            .padding(top = 40.dp, bottom = 14.dp)
            .clickable(onClick = onBackgroundTap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = surah.nameArabic,
            style = TextStyle(
                fontFamily = Amiri,
                fontSize = 34.sp,
                color = io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette.current.ornament,
            ),
        )
        Text(
            text = surah.nameSimple,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "${if (surah.revelationPlace.equals("makkah", true)) "Makkah" else "Madinah"}  \u00B7  " +
                "${surah.versesCount} ayahs",
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
            text = if (expanded) "Hide" else "About this surah",
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

@Composable
private fun AyahBlock(
    row: StudyRow,
    hafs: FontFamily,
    textSize: TextSize,
    showFootnotes: Boolean,
    isSelected: Boolean,
    playingAyah: Int?,
    playingWord: Int?,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
) {
    val playing = row.ayah.number == playingAyah
    val wash = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        playing -> MushafHighlight.copy(alpha = 0.07f)
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
                .clickable { onAyah(row.ayah) }
                .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
            Text(
                text = arabic(row, playing, playingWord, hafs, textSize),
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = textSize.arabicSp.sp,
                    lineHeight = textSize.arabicLineSp.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth(),
            )
            row.translation?.let { translation ->
                TranslationBody(
                    runs = remember(translation.text) { RichText.footnotes(translation.text) },
                    modifier = Modifier.padding(top = 12.dp),
                    textSize = textSize,
                )
                if (showFootnotes) {
                    FootnoteList(translation.footnotes, quiet = true)
                }
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
    textSize: TextSize,
): AnnotatedString {
    val words = row.words
    if (words.isEmpty()) return AnnotatedString(row.ayah.text)
    return buildAnnotatedString {
        words.forEachIndexed { index, word ->
            if (index > 0) append(' ')
            if (playing && word.position == playingWord) {
                withStyle(
                    SpanStyle(
                        background = MushafHighlight.copy(alpha = 0.22f),
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
