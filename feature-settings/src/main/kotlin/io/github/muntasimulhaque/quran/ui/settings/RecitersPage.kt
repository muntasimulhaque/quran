package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Space
/** The pages the settings hub opens, one at a time. */
/**
 * The reciters page: one choice per reciter, and under each one the surahs
 * that are already on the device. Choosing a reciter here only says whose
 * voice the reading uses; the word timings and the audio arrive together the
 * first time the reader taps Play, so nothing is downloaded before it is
 * wanted and this page has nothing to explain.
 */
@Composable
fun RecitersPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_reciters))
        packs.filter { it.type == PackType.Recitation }
            .sortedBy { it.name.lowercase() }
            .forEach { pack ->
                val id = pack.id.removePrefix(ContentDatabase.RECITER_PREFIX)
                val selected = pack.id == ContentDatabase.reciterPack(settings.recitation)
                ChoiceRow(
                    title = pack.name,
                    subtitle = null,
                    selected = selected,
                    onClick = { actions.onSelectRecitation(id) },
                    // The downloads door follows the reciter it belongs to,
                    // so the reciter's own room at the foot would read as a
                    // gap between the two.
                    bottomPadding = if (pack.installed) 0.dp else 12.dp,
                    trailing = {
                        // A reciter whose timings are on the device can give
                        // the room back; a reciter nothing was heard from has
                        // nothing to remove.
                        if (pack.installed) {
                            PackActionText(stringResource(R.string.pack_action_remove)) {
                                actions.onRemovePack(pack.id)
                            }
                        }
                    },
                )
                if (pack.installed) {
                    ReciterDownloads(id, downloadedSurahs, actions)
                }
                Spacer(Modifier.height(Space.Line))
            }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The downloaded surahs of one reciter, with what each one weighs. A surah
 * removed disappears from the list in the same frame, because the list the
 * reader sees is the list the removal changed: a row that stayed after its
 * Remove was tapped would read as a button that did nothing.
 *
 * The block is indented under the reciter's own name and its label carries a
 * disclosure arrow, so the surahs read as belonging to the reciter above them
 * rather than to the list at large. The label is a door: it opens and closes,
 * and it says so with the arrow the rest of the app uses for a door.
 */
@Composable
private fun ReciterDownloads(
    recitation: String,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
) {
    val scope = rememberCoroutineScope()
    var open by remember(recitation) { mutableStateOf(false) }
    var downloaded by remember(recitation) { mutableStateOf<List<DownloadedSurah>?>(null) }
    LaunchedEffect(recitation) {
        downloaded = downloadedSurahs(recitation).sortedBy { it.surah }
    }
    val rows = downloaded ?: return
    ReciterDownloadsDoor(open = open, count = rows.size) { open = !open }
    if (!open) return
    if (rows.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_downloaded_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 50.dp, end = 22.dp, bottom = Space.Block),
        )
        return
    }
    // The rows of this one block are read as one list of names, so their
    // Remove actions keep a compact target instead of the 48 dp a standalone
    // row carries: the whole block tightens, and the door above it still
    // gives a finger the full target.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
        rows.forEach { row ->
            DownloadedSurahRow(row) {
                scope.launch {
                    actions.onRemoveDownloads(recitation, row.surah)
                    // Whatever the removal did, the list is read from the device
                    // again: the row goes when its files are gone.
                    downloaded = downloadedSurahs(recitation).sortedBy { it.surah }
                }
            }
        }
    }
    Spacer(Modifier.height(Space.Block))
}

/**
 * The door to one reciter's downloaded surahs. Its name never changes; the
 * arrow beside it turns to say whether the list is open, so the door says
 * what it opens rather than what a tap will do.
 */
@Composable
private fun ReciterDownloadsDoor(open: Boolean, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(start = 50.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_downloaded_label, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        // The arrow sits with the name it belongs to, and turns to say which
        // way the list will move: down while it is hidden, up once it is open.
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(18.dp)
                .rotate(if (open) 180f else 0f),
        )
    }
}

/** One downloaded surah: its name, its size, and the one action it needs. */
@Composable
private fun DownloadedSurahRow(row: DownloadedSurah, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The row is already as tall as a finger needs, because the
            // Remove action brings its own touch target; the surah names sit
            // close together so the list reads as one block.
            .padding(start = 50.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatBytes(row.bytes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PackActionText(stringResource(R.string.pack_action_remove), onRemove)
    }
}

