package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.MAX_NOTE_LENGTH
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.prose
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.delay

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
    /** True while the write is in flight, so a second tap cannot start one. */
    saving: Boolean,
    /** True after a failed write; the draft stays in the field. */
    failed: Boolean,
    onSave: (String?) -> Unit,
    /** The draft as it changes, so closing the sheet never loses it. */
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberExpandedSheetState()
    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        name = stringResource(R.string.reading_note_sheet_name),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                // The gate rides with the scroll, so an upward flick on the
                // note never bounces the sheet on its top anchor.
                .sheetVerticalScroll(rememberScrollState())
                .padding(bottom = Space.SheetFoot)
                .testTag("note-sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NoteEditor(
                initial = note,
                settings = settings,
                saving = saving,
                failed = failed,
                onSave = onSave,
                onClear = { if (!saving) onSave(null) },
                onDraftChange = onDraftChange,
            )
        }
    }
}

/** The note's own editor: the reader's words, or the quiet door to writing them. */
@Composable
private fun NoteEditor(
    initial: String?,
    settings: AppSettings,
    saving: Boolean,
    failed: Boolean,
    onSave: (String?) -> Unit,
    onClear: () -> Unit,
    onDraftChange: (String) -> Unit,
) {
    // The draft survives a theme change, a language change, and the window
    // being rebuilt while the reader is still writing. `remember` alone
    // threw the words away with the old composition.
    var draft by rememberSaveable(initial) { mutableStateOf(initial.orEmpty()) }
    val hint = stringResource(R.string.card_note_hint)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(160)
        focus.requestFocus()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.Gutter, vertical = 10.dp),
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (!initial.isNullOrBlank()) {
                TextButton(
                    label = stringResource(R.string.action_clear),
                    onClick = onClear,
                    modifier = Modifier.padding(start = Space.Row),
                    quiet = true,
                )
            }
            TextButton(
                label = stringResource(
                    if (saving) R.string.reading_note_saving else R.string.reading_note_save,
                ),
                onClick = {
                    if (!saving) onSave(draft.trim().takeIf { it.isNotEmpty() })
                },
                // Save and Clear are two standalone actions, so they stand
                // the action gap apart (owner decision, this session). Save
                // is the one act the sheet exists for, so it is the app's
                // one prominent shape (owner decision, this session).
                modifier = Modifier.padding(start = Space.Action),
                prominent = true,
            )
        }
        BasicTextField(
            value = draft,
            onValueChange = {
                draft = it.take(MAX_NOTE_LENGTH)
                onDraftChange(draft)
            },
            // The reader writes their note in the reading's own voice at the
            // size they read at, not at a fixed point: it is one voice, the
            // reader's own, and it follows the choice they made for the
            // translation (owner decision, this session: the fixed 16 sp is
            // out, the reading voice is in).
            textStyle = LocalReadingVoice.current.prose(
                settings.translationSp,
                settings.translationLineSp,
                MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .padding(top = 10.dp)
                .focusRequester(focus)
                .then(
                    if (draft.isEmpty()) {
                        Modifier.semantics { contentDescription = hint }
                    } else {
                        Modifier
                    },
                ),
            decorationBox = { inner ->
                Box {
                    if (draft.isEmpty()) {
                        Text(
                            text = hint,
                            style = LocalReadingVoice.current.prose(
                                settings.translationSp,
                                settings.translationLineSp,
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            // The hint is the one thing in an empty field, so it
                            // has to be read to be a hint at all. The theme's
                            // secondary tone measures 6.1:1 and up on every
                            // ground; the 0.5 alpha it wore measured 2.2:1
                            // and is closed here. The field's own node speaks it
                            // once; the drawn copy is not a second stop.
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                    inner()
                }
            },
        )
        if (failed) {
            Text(
                text = stringResource(R.string.reading_note_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Space.Line),
            )
        }
    }
}
