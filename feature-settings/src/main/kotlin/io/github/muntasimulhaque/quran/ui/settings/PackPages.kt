package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.WordAudioOnDevice
import io.github.muntasimulhaque.quran.data.WordAudioState
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRowNameInset
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRowTrailingInset
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
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
 * its own sheet, and the one list the reading speaks.
 *
 * The list is the reader's language's list and no other, so the page shows
 * one pack and not a catalog: there is nothing to choose between, and a
 * second list would only invite a reader to read the Quran in a language they
 * did not ask for. The row is still a choice row, because its tap is the crop
 * of the aid that sits behind the same question as the switch above it
 * (owner decision, this session).
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
 * **The page names the switch, prices the list, and keeps the voices to one
 * row** (owner decision, this session). It carried a sentence under the switch
 * repeating the row's own name, and a line pricing the pronunciation for the
 * surah being read. The first was cut after two lines by the row's rule for a
 * value, and a value is not a sentence; the second reported a pack with no
 * control on this page, about a surah the page cannot choose, and it showed
 * the price of the voices even with the aid off. Both are gone. What is left
 * is the switch, the list it fetches with its own size, and, when the reader
 * has downloaded any, the voices on the device with the one act they allow:
 * taking them back. A transfer says so in one short line, because a failure
 * that said nothing would be silent, and the page owns no other state line: a
 * word with no voice says so under its own speaker.
 */
@Composable
fun WordsPage(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    actions: SettingsActions,
    wordAudio: WordAudioState = WordAudioState(),
    wordVoices: suspend () -> WordAudioOnDevice = { WordAudioOnDevice() },
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
        // six thousand files and the count changes only when one arrives
        // (owner decision, this session).
        var voices by remember { mutableStateOf(WordAudioOnDevice()) }
        LaunchedEffect(wordAudio.surah, wordAudio.installed) { voices = wordVoices() }
        val scope = rememberCoroutineScope()
        ToggleRow(
            title = stringResource(R.string.settings_words_title),
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
        if (pack != null) {
            Spacer(Modifier.height(Space.Block))
            PackChoiceRow(
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
                // The one list the reading speaks: a check says the file is
                // here, and the switch above says whether it is drawn.
                selected = installed,
                radio = true,
                setup = packSetup?.takeIf { it.packId == pack.id },
                onActivate = { actions.onWordByWord(!settings.wordByWord) },
                onInstall = { actions.onInstallPack(pack.id) },
                onRemove = { actions.onRemovePack(pack.id) },
                onCancel = actions.onPackSetupCancel,
            )
        }
        if (voices.present) {
            Spacer(Modifier.height(Space.Section))
            VoicesRow(
                voices = voices,
                onRemove = {
                    scope.launch {
                        actions.onRemoveWordVoices()
                        voices = wordVoices()
                    }
                },
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * The voices on the device: one row, because the app fetched them a surah at
 * a time and the page's one act is to take them all back. The bundled
 * Al-Fatiha is the app's own asset and is not counted, so the number is the
 * reader's downloads and the Remove leaves the first page's voice alone.
 */
@Composable
private fun VoicesRow(voices: WordAudioOnDevice, onRemove: () -> Unit) {
    val remove = stringResource(R.string.settings_voices_remove)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ChoiceRowNameInset, end = ChoiceRowTrailingInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_voices_row, voices.surahs),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatBytes(voices.bytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Tight),
            )
        }
        TextButton(
            label = stringResource(R.string.pack_action_remove),
            onClick = onRemove,
            modifier = Modifier
                .padding(start = Space.Row)
                .semantics { contentDescription = remove },
            quiet = true,
        )
    }
}

/**
 * The pronunciation line, only while it can be acted on: a transfer that is
 * running, and one that failed. What is on the device is the voices row's
 * business, and a word with no voice says so under its own speaker.
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
