package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.theme.Literata
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The one question before a save is removed from an ayah that carries a note:
 * the note is the reader's own writing, and removing the save removes it too,
 * so the reader is asked rather than having their words taken by one tap. The
 * note itself is drawn in the sheet, so the reader sees what is at stake
 * before they answer. Keep is the filled door, because the safe answer is the
 * one the sheet leads with; Remove keeps the quiet shape every removing word
 * wears (owner decision).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveSavedSheet(
    note: String?,
    onConfirm: () -> Unit,
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
                .padding(horizontal = 22.dp, vertical = 10.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.remove_saved_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!note.isNullOrBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Literata,
                        lineHeight = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Space.Line),
                )
            }
            Text(
                text = stringResource(R.string.remove_saved_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Line),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Block),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    label = stringResource(R.string.remove_saved_action),
                    onClick = onConfirm,
                    modifier = Modifier.testTag("remove-saved-confirm"),
                    quiet = true,
                )
                TextButton(
                    label = stringResource(R.string.remove_saved_keep),
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(start = Space.Line)
                        .testTag("remove-saved-keep"),
                )
            }
        }
    }
}
