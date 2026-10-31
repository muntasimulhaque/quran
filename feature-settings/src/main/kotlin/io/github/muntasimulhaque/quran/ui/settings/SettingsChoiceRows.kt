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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.kit.DownloadLine
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRowNameInset
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * One pack in a list of choices: a mark for its state, its name, and the one
 * action it needs. The mark and the name are one control, so a reader who
 * taps the name of a pack they do not have yet is asking for it, with the
 * size already on the row in front of them; a reader who taps a name they do
 * have turns it on or off. The action at the right is the same door for a
 * reader who looks for a button instead of a row.
 *
 * Radios for a reciter, checks for translations, tafsirs, and word lists:
 * one reciter is heard at a time, while more than one reading may be on. The
 * row itself is [ChoiceRow], the app's one selectable row, so a pack list
 * reads the same here as the language list, the reciter list, and the pill's
 * own menus.
 */
@Composable
fun PackChoiceRow(
    pack: ContentPack,
    subtitle: String,
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
    val busy = setup?.packId == pack.id && setup.failed.not()
    // A pack that is not here yet is not a choice: its row is one action,
    // Add or Retry, and the row itself is the door. The old row was a
    // selectable checkbox whose tap installed the pack, with a second Add
    // button beside it that did the same thing; a reader heard "not
    // selected" and then got a download (owner decision, this session).
    if (!pack.installed || busy) {
        PackActionRow(
            pack = pack,
            subtitle = subtitle,
            setup = setup,
            busy = busy,
            onInstall = onInstall,
            onCancel = onCancel,
            bottomPadding = bottomPadding,
        )
        return
    }
    ChoiceRow(
        title = contentPackName(pack.id, pack.name),
        subtitle = subtitle,
        selected = selected,
        radio = radio,
        onClick = onActivate,
        bottomPadding = bottomPadding,
        // An installed pack is a choice, and Remove is its one supplementary
        // action at the trailing end of the row (the same member of the
        // multi-action list that Saved and Last Read already use).
        trailing = {
            PackTrailing(pack, setup, onInstall, onRemove, onCancel)
        },
    )
}

/**
 * One pack that is not a choice yet: Add, Retry, or a download on its way.
 * The whole row is the one act; the word at its trailing end names the act
 * and is not a second control. While bytes move, the row is a status and the
 * only button is Cancel, because stopping a transfer is the one thing a
 * finger should be able to do from here (owner decision, this session).
 */
@Composable
private fun PackActionRow(
    pack: ContentPack,
    subtitle: String,
    setup: PackSetupState?,
    busy: Boolean,
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
                .padding(
                    start = ChoiceRowNameInset - SheetRowGutter,
                    end = Space.Row - SheetRowGutter,
                    top = Space.Row,
                    bottom = (bottomPadding - Space.Row).coerceAtLeast(0.dp),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowName(
                title = contentPackName(pack.id, pack.name),
                value = subtitle,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Space.Row),
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
                Text(
                    text = stringResource(
                        if (setup?.failed == true) {
                            R.string.pack_action_retry
                        } else {
                            R.string.pack_action_add
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
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
 * The action one pack row carries: Add when it is not here, Retry when a
 * download failed, its progress while it is on its way, Remove once it is. A
 * row already in use shows nothing else, because the mark at the left already
 * says what state it is in.
 */
@Composable
private fun PackTrailing(
    pack: ContentPack,
    setup: PackSetupState?,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit,
) {
    when {
        setup?.packId == pack.id && setup.failed ->
            TextButton(
                label = stringResource(R.string.pack_action_retry),
                onClick = onInstall,
            )

        // The row on its way in carries its progress and the one word that
        // ends it. A reader who tapped Add can change their mind while the
        // bytes are still moving, the same way the pill's own progress row
        // lets them (owner report, this session).
        setup?.packId == pack.id -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (setup.progress == null) {
                    stringResource(R.string.settings_pack_preparing)
                } else {
                    stringResource(R.string.settings_pack_downloading, (setup.progress * 100).toInt())
                },
                // The same value style every row's status wears: the hub
                // draws this download as the caption under its name, and the
                // same fact must not change size between the two doors
                // (owner decision, this session).
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

        !pack.installed -> TextButton(
            label = stringResource(R.string.pack_action_add),
            onClick = onInstall,
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
        )
    }
}
