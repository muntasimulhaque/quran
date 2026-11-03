package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ActionPill
import io.github.muntasimulhaque.quran.ui.kit.ChoiceMark
import io.github.muntasimulhaque.quran.ui.kit.ChoiceMarkSize
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRowNameInset
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRowTrailingInset
import io.github.muntasimulhaque.quran.ui.kit.DownloadLine
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * One pack in a list of choices: a mark for its state, its name, and the one
 * action it needs. The mark and the name are the choice, so a reader who
 * taps the name of a pack they do not have yet is asking for it, with the
 * size already on the row in front of them; a reader who taps a name they do
 * have turns it on or off. The word at the right names the row's other act:
 * Add when the file is not here (the row itself is the door), Remove when it
 * is (the word is a second control), and a fact when the pack ships.
 *
 * Radios for a reciter, checks for translations, tafsirs, and word lists:
 * one reciter is heard at a time, while more than one reading may be on. The
 * row is [ChoiceRow] when its tap is the choice itself, an off pack the
 * reader can add and check or a pack that is here. It is [PackActionRow]
 * when the tap is something else: a chosen pack whose file is gone, a failed
 * download, or a download on its way.
 *
 * [selected] is the reader's choice and not the file's presence. The mark is
 * drawn on every pack row, chosen or not: a chosen pack wears the check and,
 * when its file is not on the device, the line that says so; a pack that was
 * never chosen wears the off ring, because the tap that adds it also chooses
 * it. The mark column used to stand empty on a pack that was not here, so a
 * removed pack lost the only visible sign of its state and its name hung off
 * a 38 dp void (owner report, this session). Drawing the ring changes no
 * geometry: the column was already held open for it.
 *
 * A pack that is not here is a checkbox while the reader has not chosen it:
 * the tap that adds it also chooses it, so the off ring is a control the reader
 * can turn on. Once the choice stands and only the file is missing, the row is
 * a button whose mark is the check, because its act is Add and a checked box
 * that cannot be unchecked would be a lie. The word at the trailing end is a
 * pill in the same shape and on the same rail as Remove in both states, so
 * the row does not change its grammar with its file. It used to be a bare word
 * at the sheet's 12 dp edge, 14 dp outside the page's 28 dp word line, while
 * Remove sat on the choice row's 22 dp rail; the eye read two kinds of action
 * in one list (owner report, this session). Remove still deletes the pack and
 * clears the reader's choice: the row then draws the off ring, not a blank
 * column, and the page's switch says there is no translation. A chosen pack
 * whose file disappears outside the app keeps its check, because the choice is
 * the reader's (owner decision, this session).
 */
@Composable
fun PackChoiceRow(
    pack: ContentPack,
    subtitle: String,
    /** The reader's choice, whether or not the pack's file is here. */
    selected: Boolean,
    radio: Boolean,
    setup: PackSetupState?,
    onActivate: () -> Unit,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    /**
     * The reader's way out of a download they no longer want. A pack on its
     * way in used to have no answer but to wait it out (owner report, this
     * session): the reader is the one who asked for it, and the reader is
     * the one who can call it off.
     */
    onCancel: () -> Unit,
    /** The room under the row; a row with a block beneath it keeps less. */
    bottomPadding: Dp = 12.dp,
) {
    val setupHere = setup?.takeIf { it.packId == pack.id }
    val busy = setupHere != null && setupHere.failed.not()
    val failed = setupHere?.failed == true
    val absent = pack.installed.not()
    val chosenState = stringResource(R.string.settings_pack_chosen_missing)
    // The row's value says what the file is and what the choice is doing: a
    // chosen pack that is not here is a state a reader has to be told, not
    // one they should have to infer from a mark (owner report, this session).
    val value = if (absent && selected) {
        if (subtitle.isBlank()) chosenState else "$subtitle. $chosenState"
    } else {
        subtitle
    }
    if (absent && selected.not() && failed.not() && busy.not()) {
        // The common case after Remove: an off choice the reader can turn on.
        // The row is the checkbox, the Add pill names what checking it costs,
        // and the tap that checks it also brings the file (owner report, this
        // session).
        ChoiceRow(
            title = contentPackName(pack.id, pack.name),
            subtitle = value,
            selected = false,
            radio = false,
            onClick = onInstall,
            bottomPadding = bottomPadding,
            trailing = {
                ActionPill(
                    label = stringResource(R.string.pack_action_add),
                    modifier = Modifier.testTag("pack-action-add"),
                )
            },
        )
        return
    }
    if (absent || busy) {
        PackActionRow(
            pack = pack,
            value = value,
            chosen = selected,
            setup = setupHere,
            busy = busy,
            failed = failed,
            onInstall = onInstall,
            onCancel = onCancel,
            bottomPadding = bottomPadding,
        )
        return
    }
    ChoiceRow(
        title = contentPackName(pack.id, pack.name),
        subtitle = value,
        selected = selected,
        radio = radio,
        onClick = onActivate,
        bottomPadding = bottomPadding,
        // An installed pack is a choice, and Remove is its one supplementary
        // action at the trailing end of the row (the same member of the
        // multi-action list that Saved and Last Read already use).
        trailing = {
            PackTrailing(pack, setupHere, onInstall, onRemove)
        },
    )
}

/**
 * One pack whose tap is not the choice itself: a chosen pack whose file is
 * gone, a failed download, or a download on its way. The whole row is the one
 * act; the word at its trailing end is a pill that names the act and is not a
 * second control.
 *
 * The mark is the reader's choice, drawn in the column every choice row
 * keeps: an off ring when the pack was never chosen, a check when the reader
 * chose a pack whose file is gone. It used to be drawn only for the chosen
 * one, so the never-chosen row's column stood empty and the list lost its off
 * state (owner report, this session).
 *
 * The pill ends on [ChoiceRowTrailingInset], the same rail as the Remove of a
 * pack that is here, so the row does not change its right edge with its file.
 * While bytes move, the row is a status and the only button is Cancel,
 * because stopping a transfer is the one thing a finger should be able to do
 * from here (owner decision, this session).
 */
@Composable
private fun PackActionRow(
    pack: ContentPack,
    value: String,
    chosen: Boolean,
    setup: PackSetupState?,
    busy: Boolean,
    failed: Boolean,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    bottomPadding: Dp,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sheetRow(onClick = onInstall, enabled = !busy)
                .minimumInteractiveComponentSize()
                // The name's rail less the mark and its gap, so every pack
                // stands on the same line as every choice row.
                .padding(
                    start = ChoiceRowNameInset - ChoiceMarkSize - Space.Block - SheetRowGutter,
                    end = ChoiceRowTrailingInset - SheetRowGutter,
                    top = Space.Row,
                    bottom = (bottomPadding - Space.Row).coerceAtLeast(0.dp),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChoiceMark(selected = chosen, radio = false)
            RowName(
                title = contentPackName(pack.id, pack.name),
                value = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Space.Block, end = Space.Row),
            )
            if (busy) {
                Text(
                    text = if (setup?.progress == null) {
                        stringResource(R.string.settings_pack_preparing)
                    } else {
                        stringResource(
                            R.string.settings_pack_downloading,
                            (setup.progress * 100).toInt(),
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
            } else {
                ActionPill(
                    label = stringResource(
                        if (failed) {
                            R.string.pack_action_retry
                        } else {
                            R.string.pack_action_add
                        },
                    ),
                    modifier = Modifier.testTag(
                        if (failed) "pack-action-retry" else "pack-action-add",
                    ),
                )
            }
        }
        if (busy) {
            // The same 2 dp line the pill and the offer carry, so a download
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
 * The action one pack row carries once it is here: Retry when a download
 * failed, Remove when it is, and the word that says a shipped pack is
 * included. A pack that is not here carries its Add or Retry in
 * [PackActionRow], because its row is the act; this slot is for the actions of
 * a pack the reading can already use.
 */
@Composable
private fun PackTrailing(
    pack: ContentPack,
    setup: PackSetupState?,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
) {
    when {
        setup?.failed == true ->
            TextButton(
                label = stringResource(R.string.pack_action_retry),
                onClick = onInstall,
                pillTag = "pack-action-retry",
            )

        pack.shipped -> Text(
            text = stringResource(R.string.pack_included),
            // A fact about the row is its value, so it is the caption (owner
            // decision, this session).
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        else -> TextButton(
            label = stringResource(R.string.pack_action_remove),
            onClick = onRemove,
            quiet = true,
            pillTag = "pack-action-remove",
        )
    }
}
