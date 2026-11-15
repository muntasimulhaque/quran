package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The weight at which removing a file is worth a question: 20 MB.
 *
 * A translation or a tafsir is a few megabytes, fetched again in seconds, and
 * a reader who removes one means it. A surah's recitation is tens of megabytes
 * of audio that took minutes to arrive on the reader's own connection, and the
 * removal cannot be undone: the only way back is another download of the same
 * size. The app asks before it takes something it cannot give back, and it
 * does not ask about the small files where the question would be a nuisance
 * (owner decision, this session). This is the same rule the saved ayah already
 * keeps: a save that carries the reader's own note is asked about, and a save
 * that carries nothing is not.
 */
internal const val AskBeforeRemoving = 20L * 1024 * 1024

/**
 * The one question before a downloaded file is removed.
 *
 * It is [RemoveSavedSheet]'s shape on a settings page: the file's own name in
 * the page's title voice, the size at stake in a sentence under it, and the
 * pair every removing question wears, with the safe answer as the prominent
 * one. It rises over the sheet the reader is standing in, the way the saved
 * ayah's question rises over Browse, because both are the same moment: a tap
 * that would destroy something the reader waited for.
 *
 * [pending] is null when nothing is being asked. The caller keeps it, because
 * the row that raised the question knows which file it was.
 */
@Composable
internal fun RemoveDownloadSheet(
    pending: DownloadedSurah?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val row = pending ?: return
    RemoveDownloadQuestion(row = row, onConfirm = onConfirm, onDismiss = onDismiss)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoveDownloadQuestion(
    row: DownloadedSurah,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberExpandedSheetState()
    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        name = stringResource(R.string.remove_download_sheet_name),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .sheetVerticalScroll(rememberScrollState())
                .padding(horizontal = Space.Gutter, vertical = Space.Row)
                .padding(bottom = Space.SheetFoot),
        ) {
            Text(
                text = stringResource(R.string.remove_download_title, row.name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .semantics { heading() }
                    .testTag("remove-download-title"),
            )
            Text(
                text = stringResource(R.string.remove_download_body, formatBytes(row.bytes)),
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
                    label = stringResource(R.string.pack_action_remove),
                    onClick = onConfirm,
                    modifier = Modifier.testTag("remove-download-confirm"),
                    quiet = true,
                )
                TextButton(
                    label = stringResource(R.string.remove_download_keep),
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(start = Space.Action)
                        .testTag("remove-download-keep"),
                    prominent = true,
                )
            }
        }
    }
}
