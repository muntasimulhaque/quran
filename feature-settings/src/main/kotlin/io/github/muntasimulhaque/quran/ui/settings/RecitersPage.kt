package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.kit.DisclosureRow
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRowNameInset
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
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
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        // Following the recitation is a choice about the reciter, not about
        // the page: it sits here, above the list of voices, and turns the
        // reading into one that moves with the one being heard. The row
        // carries no line under it: its value is what the switch says, and a
        // sentence describing the feature in the value slot was the one
        // shape no other switch row in the sheet wore (owner report).
        ToggleRow(
            title = stringResource(R.string.settings_follow_title),
            subtitle = null,
            checked = settings.followReciter,
            onChange = actions.onFollowReciter,
        )
        packs.filter { it.type == PackType.Recitation }
            // The list is read by the name each row draws, so it is ordered
            // by the reader's own alphabet and not the catalog's English one
            // (owner decision, this session).
            .map { pack ->
                pack to reciterShortName(pack.id.removePrefix(ContentDatabase.RECITER_PREFIX), pack.name)
            }
            .sortedBy { (_, name) -> name.lowercase() }
            .forEach { (pack, name) ->
                val id = pack.id.removePrefix(ContentDatabase.RECITER_PREFIX)
                val selected = pack.id == ContentDatabase.reciterPack(settings.recitation)
                ChoiceRow(
                    title = name,
                    subtitle = null,
                    selected = selected,
                    radio = true,
                    onClick = { actions.onSelectRecitation(id) },
                    // The downloads door follows the reciter it belongs to,
                    // so the reciter's own room at the foot would read as a
                    // gap between the two.
                    bottomPadding = if (pack.installed) 0.dp else Space.Row,
                )
                if (pack.installed) {
                    ReciterDownloads(
                        recitation = id,
                        downloadedSurahs = downloadedSurahs,
                        actions = actions,
                        onRemovePack = { actions.onRemovePack(pack.id) },
                    )
                }
                Spacer(Modifier.height(Space.Line))
            }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The surahs of one reciter that are on the device, bundled or downloaded,
 * with what each one weighs. A surah removed disappears from the list in the
 * same frame, because the list the reader sees is the list the removal
 * changed: a row that stayed after its Remove was tapped would read as a
 * button that did nothing.
 *
 * A bundled surah, Al-Fatiha in both voices, is part of the app's own assets.
 * It is listed with its size like a downloaded one, because the reader asked
 * which surahs are already there, and it has no Remove: the word Bundled says
 * why the row stays and why there is no button that could take it away.
 *
 * The block is indented under the reciter's own name and its label carries a
 * disclosure arrow, so the surahs read as belonging to the reciter above them
 * rather than to the list at large. The label is a door: it opens and closes,
 * and it says so with the arrow the rest of the app uses for a door.
 *
 * The door is drawn from the first frame, before the count lands, at the
 * height the page will keep: the files are counted on a worker, and a door
 * that appeared when the count arrived would grow the sheet under the reader
 * and shift every row above it (owner decision, this session). While the
 * count is on its way the label carries no number, so the count appears in
 * place as a text change that moves nothing.
 *
 * **The last downloaded surah takes the reciter's pack with it** (owner
 * decision, this session). The pack is the timings and audio paths of the
 * voice, so with no audio on the device it is a held file with nothing to
 * hold: the row's own Remove is the whole act, and the separate Remove
 * <reciter> button that used to stand at the foot of the list is gone. The
 * voice stays in the list above, and the next Play fetches the pack again
 * with the surah it needs. A bundled surah is the exception: the pack stays
 * while the app's own audio still plays through its timings.
 */
@Composable
private fun ReciterDownloads(
    recitation: String,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
    onRemovePack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var open by remember(recitation) { mutableStateOf(false) }
    var downloaded by remember(recitation) { mutableStateOf<List<DownloadedSurah>?>(null) }
    LaunchedEffect(recitation) {
        downloaded = downloadedSurahs(recitation).sortedBy { it.surah }
    }
    ReciterDownloadsDoor(open = open, count = downloaded?.size) { open = !open }
    val rows = downloaded ?: return
    if (!open) return
    if (rows.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_downloaded_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = ChoiceRowNameInset, end = Space.Gutter, bottom = Space.Block),
        )
    } else {
        // Every Remove action wears the app's own button, and the button
        // already carries its 48 dp target, so the rows need no extra height
        // of their own: the names sit close together and the block reads as
        // one list (owner decision, this session: a 40 dp provider wrapped
        // these rows and never applied, because the button holds its own
        // floor and reads no provider).
        rows.forEach { row ->
            val remove: (() -> Unit)? = if (row.bundled) {
                null
            } else {
                {
                    scope.launch {
                        actions.onRemoveDownloads(recitation, row.surah)
                        // Whatever the removal did, the list is read from the
                        // device again: the row goes when its files are gone.
                        val left = downloadedSurahs(recitation).sortedBy { it.surah }
                        downloaded = left
                        // The last downloaded file is the pack's own reason to
                        // stay, so it goes with the file. A bundled surah is
                        // not a download and keeps the timings it plays
                        // through. The voice row above stays either way, and
                        // the next Play fetches the pack again if it left.
                        if (left.isEmpty()) onRemovePack()
                    }
                }
            }
            DownloadedSurahRow(
                row = row,
                removeAction = stringResource(R.string.settings_downloaded_remove, row.name),
                onRemove = remove,
            )
        }
    }
    Spacer(Modifier.height(Space.Block))
}

/**
 * The door to one reciter's downloaded surahs. The arrow beside it turns to
 * say whether the list is open, so the door says what it opens rather than
 * what a tap will do. The count is null while the files are being counted,
 * and the label then says the one thing it can honestly say (owner decision,
 * this session).
 */
@Composable
private fun ReciterDownloadsDoor(open: Boolean, count: Int?, onClick: () -> Unit) {
    DisclosureRow(
        expanded = open,
        onToggle = onClick,
        contentPadding = PaddingValues(
            start = ChoiceRowNameInset - SheetRowGutter,
            end = Space.Row - SheetRowGutter,
        ),
    ) {
        Text(
            text = if (count == null) {
                stringResource(R.string.settings_downloaded_label_counting)
            } else {
                stringResource(R.string.settings_downloaded_label, count)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
