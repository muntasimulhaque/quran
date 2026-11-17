package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ConfirmSheet
import io.github.muntasimulhaque.quran.ui.kit.formatBytes

/**
 * The weight at which removing a file is worth a question: 20 MB.
 *
 * A translation or a tafsir is a few megabytes, fetched again in seconds, and
 * a reader who removes one means it. A surah's recitation is tens of megabytes
 * of audio that took minutes to arrive on the reader's own connection, and the
 * removal cannot be undone: the only way back is another download of the same
 * size. The app asks before it takes something it cannot give back, and it
 * does not ask about the small files where the question would be a nuisance
 * (owner decision, 19.1). This is the same rule the saved ayah already
 * keeps: a save that carries the reader's own note is asked about, and a save
 * that carries nothing is not.
 */
internal const val AskBeforeRemoving = 20L * 1024 * 1024

/**
 * The one question before a downloaded file is removed.
 *
 * It is [ConfirmSheet]'s shape on a settings page: the file's own name in the
 * page's title voice, the size at stake in a sentence under it, and the pair
 * every removing question wears, with the safe answer as the prominent one. It
 * rises over the sheet the reader is standing in, the way the saved ayah's
 * question rises over Browse, because both are the same moment: a tap that
 * would destroy something the reader waited for.
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
    ConfirmSheet(
        name = stringResource(R.string.remove_download_sheet_name),
        title = stringResource(R.string.remove_download_title, row.name),
        body = stringResource(R.string.remove_download_body, formatBytes(row.bytes)),
        confirm = stringResource(R.string.pack_action_remove),
        keep = stringResource(R.string.remove_download_keep),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        tag = "remove-download",
    )
}
