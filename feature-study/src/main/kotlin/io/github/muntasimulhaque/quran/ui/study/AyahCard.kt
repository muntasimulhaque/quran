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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.TafsirPassage
import io.github.muntasimulhaque.quran.feature.study.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.rich.ArabicBody
import io.github.muntasimulhaque.quran.ui.rich.RichBlocks
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Reading
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed interface Door {
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
 * The ayah and its translation and meanings are already on the page, so the
 * card does not repeat them: it opens only the tafsirs. The note is not here at all: it belongs to the pill that the long
 * press raises, one tap away rather than a scroll to the foot of a card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahCard(
    content: ContentDatabase,
    ayah: Ayah,
    surahName: String,
    tafsirPacks: List<ContentPack>,
    settings: AppSettings,
    onAddContent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var door by remember(ayah.number) { mutableStateOf<Door?>(null) }
    var footnote by remember(ayah.number) { mutableStateOf<OpenFootnote?>(null) }

    val tafsir by produceState<TafsirView?>(initialValue = null, content, ayah.number, door) {
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
        // the card is a page; the ayah and its panels take the measure
        sheetMaxWidth = Dp.Unspecified,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = Reading.SheetMeasure)
                .align(Alignment.CenterHorizontally)
                .imePadding()
                // The gate rides with the scroll: a long tafsir scrolled down
                // and then scrolled back up is the reader reading, never a
                // pull that closes the card under them.
                .sheetVerticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
                // The sheet's window appears before its content has been read
                // off the database, so a capture that keeps the first settled
                // frame photographs a card missing its translation. The tag
                // says when the card is whole, and the tour waits on it: the
                // reader never sees the gap (the sheet is animating open), but
                // a still frame does (owner report).
                .testTag("ayah-card"),
        ) {
            // The tafsir is drawn only while the reader shows it. The packs
            // still stand in for search; this is what the ayah card draws.
            if (settings.showTafsir) {
                if (tafsirPacks.isEmpty()) {
                    // The card is only the tafsir doors, so a reader with none
                    // open gets the door to add one rather than a sheet with
                    // nothing in it.
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
private fun AddTranslation(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {    Row(
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
            // The range is read, not decoration: it is where this tafsir
            // passage begins and ends. It takes the theme's own secondary
            // tone, which is measured at 6.1:1 and up on every ground, rather
            // than a quiet alpha. The 0.7 alpha it wore measured 3.3:1 on the
            // sepia surface, under the theme's own 4.5:1 rule for
            // muted text, and this is the pass that closes it.
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            textStyle = LocalReadingVoice.current.style.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = LocalReadingVoice.current.atSize(16f),
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
                            style = LocalReadingVoice.current.style.copy(
                                fontSize = 16.sp,
                                fontFamily = LocalReadingVoice.current.atSize(16f),
                            ),
                            // The hint is the one thing in an empty field, so it
                            // has to be read to be a hint at all. The theme's
                            // secondary tone measures 6.1:1 and up on every
                            // ground; the 0.5 alpha it wore measured 2.2:1 on
                            // sepia and is closed here.
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
