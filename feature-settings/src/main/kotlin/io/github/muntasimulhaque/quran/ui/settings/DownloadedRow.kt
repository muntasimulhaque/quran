package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRowNameInset
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRowTrailingInset
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * One surah on the device: its name, its size, and the one action it has.
 *
 * The reciter page lists a reciter's surahs with it, and the words page lists
 * the pronunciation the reader downloaded, so one file row is learned once in
 * both places. The name starts on the line the block belongs to: the reciter's
 * choice row's name, or the words page's own gutter. The Remove ends on the
 * choice row's trailing rail, the same rail a pack's Remove and Add use, so the
 * sheet's file actions stand on one line (owner decision, this session).
 *
 * A list of identical Remove buttons needs each one to say what it removes, so
 * the caller passes [removeAction] with the surah's name in it. A bundled
 * surah, Al-Fatiha in a reciter's voice, is part of the app's own assets. It is
 * listed with its size like a downloaded one, because the reader asked which
 * surahs are already there, and it has no Remove: the word Bundled says why the
 * row stays and why there is no button that could take it away.
 *
 * The size is under its name, the way every row in the sheet reads: it stood
 * in a right-hand column, the last row in the sheet that still answered a name
 * with a value beside it (owner decision, the theme and consistency session). A
 * size is the row's value, so it wears the caption, not a source line (owner
 * decision, this session).
 */
@Composable
internal fun DownloadedSurahRow(
    row: DownloadedSurah,
    /** The Remove's spoken name, including the surah it takes away. */
    removeAction: String,
    onRemove: (() -> Unit)?,
    /** Where the name begins, from the sheet's own edge. */
    start: Dp = ChoiceRowNameInset,
    end: Dp = ChoiceRowTrailingInset,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = start, end = end),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatBytes(row.bytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Tight),
            )
        }
        if (onRemove != null) {
            TextButton(
                label = stringResource(R.string.pack_action_remove),
                onClick = onRemove,
                modifier = Modifier
                    .padding(start = Space.Row)
                    .semantics { contentDescription = removeAction },
                quiet = true,
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(start = Space.Row)
                    .height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.settings_downloaded_bundled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
