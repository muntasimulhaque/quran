package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.kit.ConfirmSheet
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.prose
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The one question before a save is removed from an ayah that carries a note:
 * the note is the reader's own writing, and removing the save removes it too,
 * so the reader is asked rather than having their words taken by one tap. The
 * note itself is drawn in the sheet, so the reader sees what is at stake
 * before they answer. The question's own shape is [ConfirmSheet]'s; this is
 * the reader's words on it.
 */
@Composable
fun RemoveSavedSheet(
    note: String?,
    /** The reader's translation size and line, so the note reads as they read. */
    noteSp: Float,
    noteLineSp: Float,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasNote = !note.isNullOrBlank()
    ConfirmSheet(
        name = stringResource(R.string.remove_saved_sheet_name),
        title = stringResource(R.string.remove_saved_title),
        body = stringResource(R.string.remove_saved_body),
        confirm = stringResource(R.string.remove_saved_action),
        keep = stringResource(R.string.remove_saved_keep),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        tag = "remove-saved",
        atStake = if (hasNote) {
            {
                Text(
                    text = note.orEmpty(),
                    style = LocalReadingVoice.current.prose(noteSp, noteLineSp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Space.Line),
                )
            }
        } else {
            null
        },
    )
}
