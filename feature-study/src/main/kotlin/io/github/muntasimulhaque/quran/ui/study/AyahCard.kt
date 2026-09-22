package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.TafsirPassage
import io.github.muntasimulhaque.quran.data.TranslationLine
import io.github.muntasimulhaque.quran.data.WordMeaning
import io.github.muntasimulhaque.quran.feature.study.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.rich.ArabicBody
import io.github.muntasimulhaque.quran.ui.rich.RichBlocks
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.theme.LatinReading
import io.github.muntasimulhaque.quran.ui.theme.Space
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
 * asking the reader to learn an interface.
 *
 * What it shows follows the reading the reader came from. From the Mushaf,
 * where the page carries neither translation nor meanings, the card is the
 * whole study surface: the translation, word by word, and each tafsir. From
 * the study reading, where the ayah and its translation and meanings are
 * already on the page, the card does not repeat them: it opens only the
 * tafsirs. The note is not here at all: it belongs to the pill that the long
 * press raises, one tap away rather than a scroll to the foot of a card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahCard(
    content: ContentDatabase,
    ayah: Ayah,
    surahName: String,
    translations: List<ContentPack>,
    tafsirPacks: List<ContentPack>,
    settings: AppSettings,
    wordLanguage: String,
    hasWords: Boolean,
    fromMushaf: Boolean,
    onAddContent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    var door by remember(ayah.number) { mutableStateOf<Door?>(null) }
    var footnote by remember(ayah.number) { mutableStateOf<OpenFootnote?>(null) }

    // Every translation the reader turned on, each read once. A card with no
    // translation chosen is not a failure: it is a reader who has not chosen
    // one yet, so the card offers the door instead of a line that sounds like
    // a bug. From the study reading the lines are already on the page, so
    // they are not read again.
    var translationReady by remember(ayah.number, translations.map { it.id }) {
        mutableStateOf(false)
    }
    val lines by produceState<List<TranslationLine>>(
        initialValue = emptyList(),
        ayah.number,
        translations.map { it.id },
        fromMushaf,
    ) {
        if (!fromMushaf) return@produceState
        value = withContext(Dispatchers.IO) {
            translations.mapNotNull { pack ->
                content.translations(listOf(ayah.number), pack.id)[ayah.number]?.let { text ->
                    TranslationLine(pack.id, pack.name, pack.language, text)
                }
            }
        }
        translationReady = true
    }
    val words by produceState<List<WordMeaning>>(initialValue = emptyList(), ayah.number, door) {
        if (fromMushaf && door == Door.Words) {
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
                    val name = content.surah(it.surah)?.nameSimple ?: "Surah ${it.surah}"
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                // The gate rides with the scroll: a long tafsir scrolled down
                // and then scrolled back up is the reader reading, never a
                // pull that closes the card under them.
                .sheetVerticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            // From the Mushaf the card is the study surface, so the
            // translation, the word by word meanings, and the tafsirs are
            // here. The ayah itself is not drawn: the reader came from it and
            // it is on the page behind the card, so repeating the Arabic here
            // would only push the study down. From the study reading the ayah,
            // its translation, and its meanings are already open on the page,
            // and only the tafsir doors remain.
            if (fromMushaf) {
                // What each block is is said in words, not with a rule: a
                // horizontal line between the translation and the tafsir is
                // furniture the app draws nowhere else, and a name over the
                // text tells the reader more than a line ever could. The
                // translation is named once because it is one block even
                // when more than one translation is on; the pack's own name
                // stays on the lines, where it says whose reading it is.
                if (lines.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.card_translation_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = Space.Block),
                    )
                }
                lines.forEach { line ->
                    if (lines.size > 1) {
                        Text(
                            text = line.packName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp),
                        )
                    }
                    TranslationBody(
                        runs = remember(line.text.text) { RichText.footnotes(line.text.text) },
                        modifier = Modifier.padding(
                            start = 22.dp,
                            end = 22.dp,
                            top = if (lines.size > 1) Space.Tight else Space.Line,
                        ),
                        sizeSp = settings.translationSp,
                        lineSp = settings.translationLineSp,
                        arabicSp = settings.arabicSp * 0.8f,
                        onFootnote = { number ->
                            // A footnote is a door here too: the marker opens the
                            // note where it stands, the way it does in the study
                            // reading, instead of the card printing every note as
                            // a block at the foot of the page.
                            line.text.footnotes.firstOrNull { it.number == number }?.let { note ->
                                footnote = OpenFootnote(
                                    note = note,
                                    reference = "${ayah.surah}:${ayah.ayah}",
                                    sizeSp = settings.translationSp,
                                    lineSp = settings.translationLineSp,
                                )
                            }
                        },
                    )
                }
                if (lines.isEmpty()) {
                    when {
                        translations.isEmpty() -> AddTranslation(
                            text = stringResource(R.string.card_add_translation),
                            onClick = onAddContent,
                            modifier = Modifier.padding(horizontal = 22.dp),
                        )
                        translationReady -> Text(
                            text = stringResource(R.string.card_no_translation),
                            style = LatinReading.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 22.dp),
                        )
                        else -> Unit
                    }
                }

                if (hasWords) {
                    // Every block of the card is named above itself: the
                    // translation, the words, the tafsir. The door below
                    // this label does not repeat it; it names what is inside,
                    // the list in the language the reading speaks, the way a
                    // tafsir door names its pack.
                    Text(
                        text = stringResource(R.string.card_word_by_word),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = Space.Block),
                    )
                    DoorRow(
                        title = stringResource(R.string.card_words_meanings),
                        subtitle = languageName(wordLanguage),
                        open = door == Door.Words,
                        onClick = { door = if (door == Door.Words) null else Door.Words },
                    )
                    if (door == Door.Words) {
                        WordByWord(
                            meanings = words,
                            hafs = hafs,
                            settings = settings,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = Space.Block),
                        )
                    }
                } else {
                    Spacer(Modifier.height(Space.Block))
                    DoorRow(
                        title = stringResource(R.string.card_add_word_by_word),
                        subtitle = "",
                        open = null,
                        onClick = onAddContent,
                    )
                }
            }
            if (!fromMushaf && tafsirPacks.isEmpty()) {
                // From the study reading the card is only the tafsir doors, so
                // a reader with none open gets the door to add one rather than
                // a sheet with nothing in it.
                AddTranslation(
                    text = stringResource(R.string.card_add_tafsir),
                    onClick = onAddContent,
                    modifier = Modifier.padding(horizontal = 22.dp),
                )
            }
            // The label stands over the tafsir doors in both readings: the
            // doors themselves name the pack, never the kind of text, so
            // without a name over them "Ibn Kathir" could be read as the word
            // by word list.
            if (tafsirPacks.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.card_tafsir_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = Space.Block),
                )
            }
            tafsirPacks.sortedBy { it.language }.forEach { pack ->
                val open = (door as? Door.Tafsir)?.pack?.id == pack.id
                DoorRow(
                    title = pack.name,
                    subtitle = languageName(pack.language),
                    open = open,
                    onClick = { door = if (open) null else Door.Tafsir(pack) },
                )
                if (open) {
                    Box(Modifier.padding(horizontal = 22.dp, vertical = Space.Line)) {
                        val view = tafsir
                        if (view == null) {
                            Text(
                                text = stringResource(R.string.card_opening_pack, pack.name),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            TafsirPanel(view, arabic = pack.language == "ar", settings = settings)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    footnote?.let { open ->
        FootnoteSheet(
            footnote = open.note,
            surahName = surahName,
            reference = open.reference,
            sizeSp = open.sizeSp,
            lineSp = open.lineSp,
            onDismiss = { footnote = null },
        )
    }
}

@Composable
private fun AddTranslation(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
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
private fun DoorRow(
    title: String,
    subtitle: String,
    open: Boolean?,
    onClick: () -> Unit,
) {
    // A door that unfolds says whether its content is shown; the row that
    // only opens the settings for a missing pack is an action, not a door,
    // and it says nothing about a state it does not have.
    val state = open?.let {
        stringResource(if (it) R.string.card_door_shown else R.string.card_door_hidden)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                if (state != null) stateDescription = state
            }
            .padding(horizontal = 22.dp, vertical = 16.dp),
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
        // The arrow is the door's only sign: a row whose content unfolds
        // under it has to say so, and it has to say it loudly enough to be
        // seen without a tap. It points down while the content is hidden and
        // turns up once the content is under it.
        IconGlyph(
            icon = Icon.Chevron,
            tint = if (open == true) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .size(20.dp)
                .rotate(if (open == true) 180f else 0f),
        )
    }
}

@Composable
private fun TafsirPanel(view: TafsirView, arabic: Boolean, settings: AppSettings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        if (arabic) {
            ArabicBody(
                runs = remember(view.passage.text) { RichText.quotes(view.passage.text) },
                sizeSp = settings.tafsirSp,
            )
        } else {
            RichBlocks(
                blocks = remember(view.passage.text) { RichText.parseHtml(view.passage.text) },
                sizeSp = settings.tafsirSp,
                lineSp = settings.tafsirLineSp,
                arabicSp = settings.tafsirArabicSp,
            )
        }
        Text(
            text = view.range,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/**
 * The reader's note, written on its own: a quiet sheet raised from the More
 * pill's note action. The note is not drawn anywhere else, because a note is
 * something the reader writes, not a block repeated at the foot of a card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahNoteSheet(
    note: String?,
    onSave: (String?) -> Unit,
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
                .padding(bottom = 28.dp),
        ) {
            NoteEditor(initial = note, onSave = onSave, onClear = { onSave(null) })
        }
    }
}

/** The note's own editor: the reader's words, or the quiet door to writing them. */
@Composable
private fun NoteEditor(initial: String?, onSave: (String?) -> Unit, onClear: () -> Unit) {
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
            // The words at the left name the sheet by what the reader is
            // about to do: a fresh sheet invites the note, an existing one
            // edits what is already written. The two at the right wear the
            // app's button shape, so the name and the buttons are never
            // mistaken for the same kind of word.
            Text(
                text = stringResource(
                    if (initial.isNullOrBlank()) R.string.card_note_take else R.string.card_note_edit,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (!initial.isNullOrBlank()) {
                TextButton(
                    label = stringResource(R.string.action_clear),
                    onClick = onClear,
                    modifier = Modifier.padding(start = 12.dp),
                    quiet = true,
                )
            }
            TextButton(
                label = stringResource(R.string.study_note_save),
                onClick = { onSave(draft.trim().takeIf { it.isNotEmpty() }) },
                modifier = Modifier.padding(start = Space.Line),
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
                            text = stringResource(R.string.card_note_hint),
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
