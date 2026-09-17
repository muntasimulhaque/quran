package io.github.muntasimulhaque.quran.ui.study

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.TafsirPassage
import io.github.muntasimulhaque.quran.data.TranslationText
import io.github.muntasimulhaque.quran.data.WordMeaning
import io.github.muntasimulhaque.quran.ui.rich.ArabicBody
import io.github.muntasimulhaque.quran.ui.rich.FootnoteList
import io.github.muntasimulhaque.quran.ui.rich.RichBlocks
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class AyahPanel { Words, IbnKathir, Saadi }

private data class TafsirView(
    val title: String,
    val range: String,
    val passage: TafsirPassage,
)

/**
 * The study card of one ayah: its text, the translation with footnotes, and
 * the three ways deeper: word by word, Ibn Kathir, and As-Sa'di. Copy and
 * share write the ayah, its translation, and its reference.
 *
 * The action row is pinned, so the reader can always switch depth without
 * hunting for buttons; the card scrolls underneath it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AyahSheet(
    content: ContentDatabase,
    ayah: Ayah,
    surahName: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }

    var panel by remember { mutableStateOf<AyahPanel?>(null) }
    var copied by remember { mutableStateOf(false) }

    val translation by produceState<TranslationText?>(initialValue = null, ayah.number) {
        value = withContext(Dispatchers.IO) {
            content.translations(listOf(ayah.number))[ayah.number]
        }
    }
    val words by produceState<List<WordMeaning>>(initialValue = emptyList(), ayah.number) {
        value = withContext(Dispatchers.IO) { content.wordMeanings(ayah.number) }
    }
    val tafsir by produceState<TafsirView?>(initialValue = null, ayah.number, panel) {
        val source = when (panel) {
            AyahPanel.IbnKathir -> "ibn-kathir"
            AyahPanel.Saadi -> "as-sadi"
            else -> null
        }
        if (source != null) {
            value = withContext(Dispatchers.IO) {
                val passage = content.tafsir(ayah.number, source)
                if (passage == null) {
                    null
                } else {
                    val name = content.surah(passage.surah)?.nameSimple ?: surahName
                    val span = if (passage.fromAyah == passage.toAyah) {
                        "${passage.surah}:${passage.fromAyah}"
                    } else {
                        "${passage.surah}:${passage.fromAyah}-${passage.toAyah}"
                    }
                    TafsirView(
                        title = if (source == "ibn-kathir") "Ibn Kathir" else "As-Sa'di",
                        range = "$name $span",
                        passage = passage,
                    )
                }
            }
        }
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    // The card opens fully: the ayah, the translation, and the ways deeper
    // should all be one glance away, never behind a drag.
    LaunchedEffect(Unit) { sheetState.expand() }

    val shareText = remember(translation, ayah) {
        buildString {
            append(ayah.text)
            translation?.text?.takeIf { it.isNotBlank() }?.let {
                append("\n\n")
                append(RichText.plain(it))
            }
            append("\n\n")
            append(surahName)
            append(' ')
            append(ayah.surah)
            append(':')
            append(ayah.ayah)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                ) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$surahName ${ayah.surah}:${ayah.ayah}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ActionPill(label = if (copied) "Copied" else "Copy", selected = copied) {
                            copyToClipboard(context, shareText)
                            copied = true
                        }
                        ActionPill(label = "Share") { shareText(context, shareText) }
                        ActionPill(label = "Words", selected = panel == AyahPanel.Words) {
                            panel = if (panel == AyahPanel.Words) null else AyahPanel.Words
                        }
                        ActionPill(label = "Ibn Kathir", selected = panel == AyahPanel.IbnKathir) {
                            panel = if (panel == AyahPanel.IbnKathir) null else AyahPanel.IbnKathir
                        }
                        ActionPill(label = "As-Sa'di", selected = panel == AyahPanel.Saadi) {
                            panel = if (panel == AyahPanel.Saadi) null else AyahPanel.Saadi
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            item(key = "ayah") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                ) {
                    Text(
                        text = ayah.text,
                        style = TextStyle(
                            fontFamily = hafs,
                            fontSize = 27.sp,
                            lineHeight = 54.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item(key = "content") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                ) {
                    when (panel) {
                        null -> translation?.let {
                            TranslationBody(RichText.footnotes(it.text), Modifier.padding(top = 14.dp))
                            FootnoteList(it.footnotes)
                        }
                        AyahPanel.Words -> WordsPanel(words, hafs)
                        AyahPanel.IbnKathir -> tafsir?.let { TafsirPanel(it, arabic = false) }
                        AyahPanel.Saadi -> tafsir?.let { TafsirPanel(it, arabic = true) }
                    }
                }
            }

            item(key = "end") { Spacer(Modifier.height(34.dp)) }
        }
    }
}

@Composable
private fun TafsirPanel(view: TafsirView, arabic: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = view.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = view.range,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (arabic) {
            val runs = remember(view.passage.text) { RichText.quotes(view.passage.text) }
            ArabicBody(runs)
        } else {
            val blocks = remember(view.passage.text) { RichText.parseHtml(view.passage.text) }
            RichBlocks(blocks)
        }
    }
}

@Composable
private fun WordsPanel(words: List<WordMeaning>, hafs: FontFamily) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    ) {
        words.forEachIndexed { index, word ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = word.meaning.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = word.word,
                    style = TextStyle(
                        fontFamily = hafs,
                        fontSize = 23.sp,
                        lineHeight = 44.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .widthIn(min = 104.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionPill(label: String, selected: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Ayah", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, null)) }
}
