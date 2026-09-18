package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
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
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.TafsirPassage
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TranslationText
import io.github.muntasimulhaque.quran.data.WordMeaning
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconButton
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.rich.ArabicBody
import io.github.muntasimulhaque.quran.ui.rich.FootnoteList
import io.github.muntasimulhaque.quran.ui.rich.RichBlocks
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.theme.LatinReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed interface Door {
    data object Words : Door
    data class Tafsir(val pack: ContentPack) : Door
}

private data class TafsirView(
    val title: String,
    val range: String,
    val passage: TafsirPassage,
)

/**
 * The ayah card: one screen per ayah that holds everything deeper without
 * asking the reader to learn an interface. The ayah and its translation are
 * open; word by word and each tafsir are doors that unfold where they stand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahCard(
    content: ContentDatabase,
    ayah: Ayah,
    surahName: String,
    translationPack: ContentPack?,
    tafsirPacks: List<ContentPack>,
    textSize: TextSize,
    wordLanguage: String,
    hasWords: Boolean,
    isSaved: Boolean,
    note: String?,
    onToggleSave: () -> Unit,
    onSaveNote: (String?) -> Unit,
    onPlay: () -> Unit,
    onAddContent: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    var door by remember(ayah.number) { mutableStateOf<Door?>(null) }
    var editingNote by remember(ayah.number) { mutableStateOf(false) }

    val translation by produceState<TranslationText?>(initialValue = null, ayah.number, translationPack?.id) {
        value = withContext(Dispatchers.IO) {
            translationPack?.let { content.translations(listOf(ayah.number), it.id)[ayah.number] }
        }
    }
    val words by produceState<List<WordMeaning>>(initialValue = emptyList(), ayah.number, door) {
        if (door == Door.Words) {
            value = withContext(Dispatchers.IO) { content.wordMeanings(ayah.number, wordLanguage) }
        }
    }
    val tafsir by produceState<TafsirView?>(initialValue = null, ayah.number, door) {
        val pack = (door as? Door.Tafsir)?.pack
        value = if (pack == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val passage = content.tafsir(ayah.number, pack.id)
                passage?.let {
                    val name = content.surah(it.surah)?.nameSimple ?: surahName
                    val span = if (it.fromAyah == it.toAyah) {
                        "${it.surah}:${it.fromAyah}"
                    } else {
                        "${it.surah}:${it.fromAyah}-${it.toAyah}"
                    }
                    TafsirView(title = it.source, range = "$name $span", passage = it)
                }
            }
        }
    }

    val shareText = remember(translation, ayah, surahName) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = surahName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${ayah.surah}:${ayah.ayah}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    icon = if (isSaved) Icon.BookmarkFilled else Icon.Bookmark,
                    description = if (isSaved) "Saved" else "Save this ayah",
                    onClick = onToggleSave,
                    active = isSaved,
                )
                IconButton(Icon.Copy, "Copy the ayah", onClick = { onCopy(shareText) })
                IconButton(Icon.Share, "Share the ayah", onClick = { onShare(shareText) })
            }

            Text(
                text = ayah.text,
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = (textSize.arabicSp + 2).sp,
                    lineHeight = (textSize.arabicLineSp + 8).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 16.dp),
            )

            translation?.let { body ->
                TranslationBody(
                    runs = remember(body.text) { RichText.footnotes(body.text) },
                    modifier = Modifier.padding(horizontal = 22.dp),
                    textSize = textSize,
                )
            } ?: Text(
                text = "This ayah has no text in the chosen translation.",
                style = LatinReading.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp),
            )

            Spacer(Modifier.height(18.dp))

            if (hasWords) {
                DoorRow(
                    title = "Word by word",
                    subtitle = "",
                    open = door == Door.Words,
                    onClick = { door = if (door == Door.Words) null else Door.Words },
                )
                if (door == Door.Words) {
                    WordsPanel(words, hafs, textSize, Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
                }
            } else {
                DoorRow(
                    title = "Add word by word",
                    subtitle = "",
                    open = false,
                    onClick = onAddContent,
                )
            }
            tafsirPacks.sortedBy { it.language }.forEach { pack ->
                val open = (door as? Door.Tafsir)?.pack?.id == pack.id
                DoorRow(
                    title = pack.name,
                    subtitle = if (pack.language == "ar") "Arabic" else "English",
                    open = open,
                    onClick = { door = if (open) null else Door.Tafsir(pack) },
                )
                if (open) {
                    Box(Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
                        val view = tafsir
                        if (view == null) {
                            Text(
                                text = "Opening ${pack.name}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            TafsirPanel(view, arabic = pack.language == "ar")
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            NoteBlock(
                note = note,
                editing = editingNote,
                onEdit = { editingNote = true },
                onSave = { text ->
                    onSaveNote(text.trim().takeIf { it.isNotEmpty() })
                    editingNote = false
                },
                onClear = {
                    onSaveNote(null)
                    editingNote = false
                },
            )

            translation?.footnotes?.takeIf { it.isNotEmpty() }?.let { footnotes ->
                Column(Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
                    Text(
                        text = "Translator notes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    FootnoteList(footnotes)
                }
            }

            Spacer(Modifier.height(18.dp))
            PlayButton(onPlay, Modifier.padding(horizontal = 22.dp))
        }
    }
}

@Composable
private fun PlayButton(onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onPlay)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        io.github.muntasimulhaque.quran.ui.reader.IconGlyph(
            icon = Icon.Play,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.heightIn(min = 16.dp).widthIn(min = 16.dp),
        )
        Text(
            text = "Play from this ayah",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun DoorRow(
    title: String,
    subtitle: String,
    open: Boolean,
    onClick: () -> Unit,
) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 22.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .heightIn(min = 14.dp)
                .widthIn(min = 14.dp)
                .rotate(if (open) 180f else 0f),
        )
    }
}

@Composable
private fun TafsirPanel(view: TafsirView, arabic: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        if (arabic) {
            ArabicBody(remember(view.passage.text) { RichText.quotes(view.passage.text) })
        } else {
            RichBlocks(remember(view.passage.text) { RichText.parseHtml(view.passage.text) })
        }
        Text(
            text = view.range,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun WordsPanel(
    words: List<WordMeaning>,
    hafs: FontFamily,
    textSize: TextSize,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        words.forEachIndexed { index, word ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                        fontSize = (textSize.arabicSp - 2).sp,
                        lineHeight = (textSize.arabicLineSp - 6).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .widthIn(min = 96.dp),
                )
            }
        }
    }
}

/** The reader's own note, or the quiet door to writing one. */
@Composable
private fun NoteBlock(
    note: String?,
    editing: Boolean,
    onEdit: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 22.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
        when {
            editing -> NoteEditor(
                initial = note,
                onSave = onSave,
                onClear = onClear,
            )
            note.isNullOrBlank() -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "Add a note",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "Your note",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = note,
                    style = LatinReading.copy(fontSize = 15.sp, lineHeight = 23.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun NoteEditor(initial: String?, onSave: (String) -> Unit, onClear: () -> Unit) {
    var draft by remember(initial) { mutableStateOf(initial.orEmpty()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(160)
        focus.requestFocus()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Note",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (!initial.isNullOrBlank()) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Text(
                text = "Save",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSave(draft) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        BasicTextField(
            value = draft,
            onValueChange = { draft = it },
            textStyle = LatinReading.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .padding(top = 10.dp)
                .focusRequester(focus),
            decorationBox = { inner ->
                Box {
                    if (draft.isEmpty()) {
                        Text(
                            text = "Write a note for this ayah",
                            style = LatinReading.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                    inner()
                }
            },
        )
    }
}
