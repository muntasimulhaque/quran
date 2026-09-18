package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.TextRun
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.Footnote
import io.github.muntasimulhaque.quran.data.Word
import io.github.muntasimulhaque.quran.ui.rich.FootnoteList
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AyahView(
    val ayah: Ayah,
    val words: List<Word>,
    val runs: List<TextRun>,
    val footnotes: List<Footnote>,
)

/**
 * The study page of one Mushaf page: every ayah in the page's own order, its
 * canonical text, the translation with quiet footnote markers, and the
 * footnotes themselves. Tapping an ayah opens the study card. While a
 * recitation plays, the ayah and the word being recited are marked, and the
 * page follows the reciter.
 */
@Composable
fun StudyPage(
    content: ContentDatabase,
    page: Int,
    playingAyah: Int?,
    playingWord: Int?,
    onAyah: (Ayah) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    val ayahs by produceState(initialValue = emptyList<AyahView>(), page, content) {
        value = withContext(Dispatchers.IO) {
            val list = content.ayahsForPage(page)
            val translations = content.translations(list.map { it.number })
            val words = content.wordsForAyahs(list.map { it.number })
            list.map { ayah ->
                val translation = translations[ayah.number]
                AyahView(
                    ayah = ayah,
                    words = words[ayah.number].orEmpty(),
                    runs = translation?.let { RichText.footnotes(it.text) }.orEmpty(),
                    footnotes = translation?.footnotes.orEmpty(),
                )
            }
        }
    }
    val listState = rememberLazyListState()

    // The playing ayah comes back into view when the reciter moves on.
    LaunchedEffect(playingAyah, ayahs) {
        val index = ayahs.indexOfFirst { it.ayah.number == playingAyah }
        if (index >= 0 && index != listState.firstVisibleItemIndex) {
            listState.animateScrollToItem(index)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 58.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(34.dp),
    ) {
        items(ayahs, key = { it.ayah.number }) { view ->
            AyahBlock(
                view = view,
                hafs = hafs,
                isPlaying = view.ayah.number == playingAyah,
                activeWord = if (view.ayah.number == playingAyah) playingWord else null,
                onAyah = onAyah,
            )
        }
    }
}

@Composable
private fun AyahBlock(
    view: AyahView,
    hafs: FontFamily,
    isPlaying: Boolean,
    activeWord: Int?,
    onAyah: (Ayah) -> Unit,
) {
    val background = if (isPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
    } else {
        Color.Transparent
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable { onAyah(view.ayah) }
            .padding(vertical = 8.dp, horizontal = 6.dp),
    ) {
        Text(
            text = arabic(view.words, view.ayah.text, activeWord),
            style = TextStyle(
                fontFamily = hafs,
                fontSize = 27.sp,
                lineHeight = 54.sp,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth(),
        )
        if (view.runs.isNotEmpty()) {
            TranslationBody(view.runs, Modifier.padding(top = 14.dp))
        }
        FootnoteList(view.footnotes)
        Text(
            text = view.ayah.verseKey,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** The ayah as whole words, with the word being recited in lapis. */
@Composable
private fun arabic(
    words: List<Word>,
    fallback: String,
    activeWord: Int?,
): AnnotatedString {
    if (words.isEmpty()) return AnnotatedString(fallback)
    val primary = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        words.forEachIndexed { index, word ->
            if (index > 0) append(' ')
            if (word.position == activeWord) {
                withStyle(SpanStyle(color = primary)) { append(word.text) }
            } else {
                append(word.text)
            }
        }
    }
}
