package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.TextBlock
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.TafsirPassage
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.AddDoor
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.kit.SheetHandle
import io.github.muntasimulhaque.quran.ui.reader.GlyphSize
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.rich.RichBlocks
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Reading
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash
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
    /**
     * The passage already parsed into its blocks. The parse is the one heavy
     * step in showing a tafsir, and it is pure work: doing it here, on the
     * read's own worker, leaves the frame that unfolds the panel with only
     * the layout to do (owner report, this session).
     */
    val blocks: List<TextBlock>,
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
    // One tafsir on, and the card opens it: the reader tapped Tafsir on the
    // pill, so the second tap the door list asks for is a question whose
    // answer is already in the tap. With more than one, the doors stand and
    // the reader chooses (owner decision, this session).
    val firstPack = tafsirPacks.singleOrNull()
    var door by remember(ayah.number) { mutableStateOf(firstPack?.let { Door.Tafsir(it) }) }

    // Every enabled tafsir is read as the card opens, not when its door is
    // tapped: the read is one indexed query of a millisecond or two, and
    // paying for it here puts the text in hand before the sheet has finished
    // moving, so the panel opens on prose instead of on "Opening…" (owner
    // report, this session: the wait was the tap-to-read, not the query).
    val passages by produceState<Map<String, TafsirView>>(
        initialValue = emptyMap(),
        content,
        ayah.number,
        tafsirPacks,
    ) {
        value = withContext(Dispatchers.IO) {
            tafsirPacks.mapNotNull { pack ->
                val passage = content.tafsir(ayah.number, pack.id) ?: return@mapNotNull null
                val name = content.surah(passage.surah)?.nameSimple
                    ?: surahName
                val span = if (passage.fromAyah == passage.toAyah) {
                    "${passage.surah}:${passage.fromAyah}"
                } else {
                    "${passage.surah}:${passage.fromAyah}-${passage.toAyah}"
                }
                pack.id to TafsirView(
                    title = passage.source,
                    range = "$name $span",
                    passage = passage,
                    blocks = RichText.parseHtml(passage.text),
                )
            }.toMap()
        }
    }

    // The card is a page: the tafsir takes the reader's own measure, plus the
    // gutter each side, so a ten inch screen centers a readable column instead
    // of stretching one line across the glass. The old widthIn sat behind
    // fillMaxWidth, which forced the full width first, so the cap never took
    // effect and a tafsir line ran to about 150 characters on a tablet
    // (owner report, the audio and type audit session).
    val sheetWidth = Reading.proseMeasure(LocalReadingVoice.current, settings.tafsirSp) +
        Space.Gutter * 2
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { SheetHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = sheetWidth,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    AddDoor(
                        text = stringResource(R.string.card_add_tafsir),
                        action = stringResource(R.string.reading_action_add),
                        onClick = onAddContent,
                        modifier = Modifier.padding(horizontal = Space.Gutter),
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
                            val view = passages[pack.id]
                            if (view == null) {
                                Text(
                                    text = stringResource(R.string.card_opening_pack, pack.name),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                TafsirPanel(view, settings = settings)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.Line))
        }
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
            .sheetRow(onClick = onClick, role = Role.Button)
            .semantics {
                if (state != null) stateDescription = state
            }
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Block),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The value is under its name, the way every row in every sheet
        // reads: it stood in a right-hand column, the one grammar the card
        // still kept from the hub's old shape (owner decision, the theme
        // and consistency session).
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.Tight),
                )
            }
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
                .size(GlyphSize.ChevronRow)
                .rotate(if (open == true) 180f else 0f),
        )
    }
}

@Composable
private fun TafsirPanel(view: TafsirView, settings: AppSettings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tafsir-panel")
            .padding(top = Space.Tight),
    ) {
        RichBlocks(
            blocks = view.blocks,
            sizeSp = settings.tafsirSp,
            lineSp = settings.tafsirLineSp,
            arabicSp = settings.arabicInlineSp,
        )
        Text(
            text = view.range,
            style = MaterialTheme.typography.labelMedium,
            // The range is read, not decoration: it is where this tafsir
            // passage begins and ends. It takes the theme's own secondary
            // tone, which is measured at 6.1:1 and up on every ground, rather
            // than a quiet alpha. The 0.7 alpha it wore measured 3.3:1,
            // under the theme's own 4.5:1 rule for muted text, and this
            // is the pass that closes it.
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Row),
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
    settings: AppSettings,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { SheetHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        // The note is prose: it keeps the reading's own measure, the same
        // column the ayah card keeps (owner decision, the theme and
        // consistency session).
        sheetMaxWidth = Reading.proseMeasure(LocalReadingVoice.current, settings.translationSp) +
            Space.Gutter * 2,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                // The gate rides with the scroll, so an upward flick on the
                // note never bounces the sheet on its top anchor.
                .sheetVerticalScroll(rememberScrollState())
                .padding(bottom = Space.SheetFoot),
        ) {
            NoteEditor(
                initial = note,
                settings = settings,
                onSave = onSave,
                onClear = { onSave(null) },
            )
        }
    }
}

/** The note's own editor: the reader's words, or the quiet door to writing them. */
@Composable
private fun NoteEditor(
    initial: String?,
    settings: AppSettings,
    onSave: (String?) -> Unit,
    onClear: () -> Unit,
) {
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
                label = stringResource(R.string.reading_note_save),
                onClick = { onSave(draft.trim().takeIf { it.isNotEmpty() }) },
                modifier = Modifier.padding(start = Space.Line),
            )
        }
        BasicTextField(
            value = draft,
            onValueChange = { draft = it },
            // The reader writes their note in the reading's own voice at the
            // size they read at, not at a fixed point: it is one voice, the
            // reader's own, and it follows the choice they made for the
            // translation (owner decision, this session: the fixed 16 sp is
            // out, the reading voice is in).
            textStyle = LocalReadingVoice.current.style.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = settings.translationSp.sp,
                lineHeight = settings.translationLineSp.sp,
                fontFamily = LocalReadingVoice.current.atSize(settings.translationSp),
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
                                fontSize = settings.translationSp.sp,
                                lineHeight = settings.translationLineSp.sp,
                                fontFamily = LocalReadingVoice.current.atSize(settings.translationSp),
                            ),
                            // The hint is the one thing in an empty field, so it
                            // has to be read to be a hint at all. The theme's
                            // secondary tone measures 6.1:1 and up on every
                            // ground; the 0.5 alpha it wore measured 2.2:1
                            // and is closed here.
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
