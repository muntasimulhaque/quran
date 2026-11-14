package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.WordAudioState
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ActionPill
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRowTrailingInset
import io.github.muntasimulhaque.quran.ui.kit.DisclosureRow
import io.github.muntasimulhaque.quran.ui.kit.DownloadLine
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.launch

/**
 * Translations: more than one may be on, and each one reads in its own column
 * under the ayah, so the mark is a check rather than a single choice. The
 * first one turned on is the one search and the ayah card read by default.
 *
 * The master switch stands at the top of the page, in the same words and the
 * same state as the hub row that opened it. The page that owns the list owns
 * the switch that shows it, and a reader who came here from the row, or from
 * a reader's own path, can turn the reading on without going back for it
 * (owner decision, this session). The switch that used to live on the hub row
 * and nowhere else is still there; this is one value read in two places, not
 * a second setting.
 */
@Composable
fun TranslationsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        val installed = hasInstalledTranslation(settings, packs)
        val chosen = hasChosenTranslation(settings, packs)
        ToggleRow(
            title = stringResource(R.string.settings_show_translation_title),
            subtitle = translationName(settings, packs),
            checked = settings.showTranslation && chosen,
            onChange = actions.onShowTranslation,
            enabled = installed,
            switchTag = "switch-translation-page",
        )
        LanguageGroups(
            packs = packs,
            type = PackType.Translation,
            leadingLanguage = settings.readingLanguage,
        ) { pack ->
            PackChoiceRow(
                pack = pack,
                subtitle = packSizeLine(pack),
                selected = pack.id in settings.translationPacks,
                radio = false,
                setup = packSetup?.takeIf { it.packId == pack.id },
                onActivate = { actions.onToggleTranslation(pack.id) },
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
                onCancel = actions.onPackSetupCancel,
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * Tafsirs: as many as the reader wants, each behind its own door.
 *
 * There is no master switch over the list. The reading opens a tafsir only
 * when the reader asks for one, from the ayah pill's own Tafsir mark, so
 * there is no passage being drawn that a switch could hide; a tafsir is on
 * the reading exactly while a pack is chosen, the same way a reciter is the
 * reader's voice exactly while it is chosen (owner decision, this session).
 */
@Composable
fun TafsirsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        LanguageGroups(
            packs = packs,
            type = PackType.Tafsir,
        ) { pack ->
            PackChoiceRow(
                pack = pack,
                subtitle = packSizeLine(pack),
                selected = pack.id in settings.tafsirPacks,
                radio = false,
                setup = packSetup?.takeIf { it.packId == pack.id },
                onActivate = { actions.onToggleTafsir(pack.id) },
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
                onCancel = actions.onPackSetupCancel,
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The word list, on its own page: the switch that opens a word's meaning on
 * its own sheet, the one list the reading speaks, and the pronunciation the
 * reader has downloaded.
 *
 * The list is the reader's language's list and no other, so the page shows
 * one pack and not a catalog: there is nothing to choose between, and a
 * second list would only invite a reader to read the Quran in a language they
 * did not ask for.
 *
 * **The switch is the ask** (owner decision, this session). A fresh install
 * carries no word list, and the switch used to stand disabled until the pack
 * was added, a dead control under the name of the feature while two other
 * rows did the same thing. It is the "tap that turns the aid on" the rule
 * names: turning it on fetches the one list, through the same sized add every
 * pack uses, and the aid turns on when the list lands. The row's own line says
 * the transfer while it runs, so the tap is answered under the finger; a
 * failure leaves the switch off and says why; and a language the catalog
 * offers no list for keeps the switch disabled, because a switch that can
 * fetch nothing is a switch that lies. The translations page keeps its own
 * master switch disabled until a pack is installed, because there the switch
 * cannot choose which translation to fetch; here there is exactly one list.
 *
 * **The switch carries the feature's name, and the gesture is a lead** (owner
 * decision, this session). The row used to be titled with the instruction
 * "Press and hold a word for its meaning and pronunciation", a sentence in the
 * size and weight every other row's name wears, wrapped beside its switch. The
 * name is the one the hub already uses, and the instruction, which is the only
 * door to the word sheet, stands under it as the page's lead, the way the
 * Tajweed page opens with its own.
 *
 * **The one list is a file row, not a choice row** (owner decision, this
 * session). There is nothing to choose between: the switch is the choice, and
 * the row is the file that choice needs. It carried a radio, the reciter's
 * one-answer mark, with `selected` set to the file's presence, so the same row
 * drew an empty check ring and installed on a fresh page, then a filled radio
 * and toggled the switch once the file was here. It now reads as the reciter's
 * downloaded surahs read: its name, its size, Add or Retry while it is not
 * here, Remove when it is. The whole row is the add while the file is absent,
 * and the pill at the trailing end names the act.
 *
 * **The pronunciation is a door over its own surahs** (owner decision, this
 * session). It was one row, "Downloaded voices (12)" and a total size, with
 * one Remove for all of them: a count the reader could not act on, a word that
 * belongs to the reciter, and a leading column the choice rows use with no
 * mark in it. The page now uses the reciter page's own shape: a disclosure
 * that says how many surahs are on the device, and under it one row per surah
 * with its name, its size, and its own Remove. The count is the door's, and
 * every Remove says which surah it takes. The bundled Al-Fatiha is the app's
 * own asset and is not counted, because the page's one act is to take back
 * what the reader downloaded. A transfer says so in one short line, because a
 * failure that said nothing would be silent, and the page owns no other state
 * line: a word with no voice says so under its own speaker.
 */
@Composable
fun WordsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
    wordAudio: WordAudioState = WordAudioState(),
    wordVoices: suspend () -> List<DownloadedSurah> = { emptyList() },
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        val pack = wantedWordsPack(settings, packs)
        val installed = pack?.installed == true
        val setup = packSetup?.takeIf { it.packId == pack?.id }
        // The root travels with the list, so the row says what the tap will
        // fetch and not only what the list's own file weighs. The line names
        // the root while it is still to come, and goes back to the list's
        // size once it is on the device (owner decision, this session).
        val root = packs.firstOrNull { it.id == ContentDatabase.WORD_ROOT_PACK }
        val rootToCome = root != null && !root.installed
        // The voices the reader has downloaded, read again when a pack
        // lands or leaves, and never on a progress tick: a folder holds up to
        // six thousand files and the list changes only when one arrives
        // (owner decision, this session).
        var voices by remember { mutableStateOf<List<DownloadedSurah>>(emptyList()) }
        LaunchedEffect(wordAudio.surah, wordAudio.installed) { voices = wordVoices() }
        val scope = rememberCoroutineScope()
        ToggleRow(
            title = stringResource(R.string.settings_row_words),
            // The value is the list's own transfer while it runs, and nothing
            // otherwise: a name and its switch need no sentence.
            subtitle = packSetupLine(setup),
            checked = settings.wordByWord && installed,
            onChange = actions.onWordByWord,
            switchTag = "switch-words-page",
            // The one list is offered by the catalog, so the switch has a
            // list to fetch; a language with none keeps it disabled.
            enabled = pack != null,
        )
        pronunciationLine(wordAudio)?.let { line ->
            Spacer(Modifier.height(Space.Line))
            Text(
                text = line,
                // The switch's own value voice, on the row's own edge: the
                // line is a fact about the aid, not a second control.
                modifier = Modifier
                    .padding(
                        start = Space.Gutter - SheetRowGutter,
                        end = Space.Row - SheetRowGutter,
                    )
                    .testTag("words-audio-line"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The gesture that opens a word's own sheet is a page lead, not the
        // switch's name: the name is the feature, and this is how the feature
        // is used (owner decision, this session).
        Text(
            text = stringResource(R.string.settings_words_lead),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Space.Gutter,
                    end = Space.Gutter,
                    top = Space.Line,
                ),
        )
        if (pack != null) {
            Spacer(Modifier.height(Space.Block))
            WordListRow(
                pack = pack,
                subtitle = if (rootToCome && root != null) {
                    stringResource(
                        R.string.settings_words_size_with_root,
                        packSizeLine(pack),
                        packSizeLine(root),
                    )
                } else {
                    packSizeLine(pack)
                },
                setup = setup,
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
                onCancel = actions.onPackSetupCancel,
            )
        }
        if (voices.isNotEmpty()) {
            Spacer(Modifier.height(Space.Section))
            PronunciationDownloads(
                surahs = voices,
                onRemove = { surah ->
                    scope.launch {
                        actions.onRemoveWordVoice(surah)
                        voices = wordVoices()
                    }
                },
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The one list as a file row: its name, its size, and the one act its file
 * allows. There is no choice mark, because the page has no choice to make;
 * the switch above is the aid's on and off. While the file is absent the whole
 * row is the add, and the pill at its trailing end names what the row will do,
 * the way a pack row that is not here names its act. The installed row is not
 * a door: Remove is the only thing it can do. A transfer is a status, so the
 * row stops answering a finger and Cancel is the one control.
 */
@Composable
private fun WordListRow(
    pack: ContentPack,
    subtitle: String,
    setup: PackSetupState?,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit,
) {
    val busy = setup != null && !setup.failed
    val failed = setup?.failed == true
    val clickable = !pack.installed
    // The disabled text button's target ends on the choice row's rail inside
    // the surface's inset; a row with no surface keeps the page's own gutter.
    val leading = if (clickable) Space.Gutter - SheetRowGutter else Space.Gutter
    val trailing = if (clickable) {
        ChoiceRowTrailingInset - SheetRowGutter
    } else {
        ChoiceRowTrailingInset
    }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (clickable) {
                        Modifier.sheetRow(onClick = onInstall, enabled = !busy)
                    } else {
                        Modifier
                    },
                )
                .minimumInteractiveComponentSize()
                .padding(
                    start = leading,
                    end = trailing,
                    top = Space.Row,
                    bottom = Space.Row,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowName(
                title = contentPackName(pack.id, pack.name),
                value = subtitle,
                modifier = Modifier.weight(1f).padding(end = Space.Row),
            )
            when {
                busy -> {
                    val progress = setup?.progress
                    Text(
                        text = if (progress == null) {
                            stringResource(R.string.settings_pack_preparing)
                        } else {
                            stringResource(
                                R.string.settings_pack_downloading,
                                (progress * 100).toInt(),
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        label = stringResource(R.string.settings_cancel),
                        onClick = onCancel,
                        modifier = Modifier.padding(start = Space.Row),
                        quiet = true,
                    )
                }
                pack.installed -> TextButton(
                    label = stringResource(R.string.pack_action_remove),
                    onClick = onRemove,
                    quiet = true,
                    pillTag = "pack-action-remove",
                )
                else -> ActionPill(
                    label = stringResource(
                        if (failed) R.string.pack_action_retry else R.string.pack_action_add,
                    ),
                    modifier = Modifier.testTag(
                        if (failed) "pack-action-retry" else "pack-action-add",
                    ),
                )
            }
        }
        if (busy) {
            // The same 2 dp line the pill and the pack row carry, so a download
            // has one shape wherever it is shown (owner decision, this
            // session).
            DownloadLine(
                progress = setup?.progress ?: 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Space.Gutter,
                        end = Space.Gutter,
                        top = Space.Tight,
                        bottom = Space.Line,
                    )
                    .height(2.dp),
            )
        }
    }
}

/**
 * The pronunciation on the device, behind the reciter page's own door: a
 * count, the chevron that says it opens, and one row per surah with its own
 * Remove. The door is the list's count, not a total size, because a number a
 * reader cannot act on says nothing (owner decision, this session).
 */
@Composable
private fun PronunciationDownloads(
    surahs: List<DownloadedSurah>,
    onRemove: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    DisclosureRow(
        expanded = open,
        onToggle = { open = !open },
    ) {
        Text(
            text = stringResource(R.string.settings_pronunciation_label, surahs.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (!open) return
    surahs.forEach { row ->
        DownloadedSurahRow(
            row = row,
            start = Space.Gutter,
            removeAction = stringResource(R.string.settings_pronunciation_remove, row.name),
            onRemove = { onRemove(row.surah) },
        )
    }
    Spacer(Modifier.height(Space.Block))
}

/**
 * The pronunciation line, only while it can be acted on: a transfer that is
 * running, and one that failed. What is on the device is the door's business,
 * and a word with no voice says so under its own speaker.
 */
@Composable
private fun pronunciationLine(state: WordAudioState): String? {
    val surah = formatBytes(state.bytes)
    return when {
        state.failed ->
            stringResource(R.string.settings_words_pronunciation_failed)
        state.read > 0 && !state.installed && state.surah > 0 ->
            stringResource(
                R.string.settings_words_pronunciation_downloading,
                formatBytes(state.read),
                surah,
            )
        else -> null
    }
}
