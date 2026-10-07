package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ChoiceRow
import io.github.muntasimulhaque.quran.ui.kit.TextButton

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
    /** The room under the row; a row with a block beneath it keeps less. */
    bottomPadding: Dp = 12.dp,
) {
    val busy = setup?.packId == pack.id && setup.failed.not()
    ChoiceRow(
        title = pack.name,
        subtitle = subtitle,
        selected = selected,
        radio = radio,
        // A pack that is not here yet cannot be turned on: its row is the
        // door that brings it here instead, and nothing looks selectable
        // while it is on its way in.
        onClick = if (pack.installed && !busy) onActivate else onInstall,
        bottomPadding = bottomPadding,
        trailing = { PackTrailing(pack, setup, onInstall, onRemove) },
    )
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
) {
    when {
        setup?.packId == pack.id && setup.failed ->
            TextButton(
                label = stringResource(R.string.pack_action_retry),
                onClick = onInstall,
            )

        setup?.packId == pack.id -> Text(
            text = if (setup.progress == null) {
                stringResource(R.string.settings_pack_preparing)
            } else {
                stringResource(R.string.settings_pack_downloading, (setup.progress * 100).toInt())
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        !pack.installed -> TextButton(
            label = stringResource(R.string.pack_action_add),
            onClick = onInstall,
        )

        pack.shipped -> Text(
            text = stringResource(R.string.pack_included),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        else -> TextButton(
            label = stringResource(R.string.pack_action_remove),
            onClick = onRemove,
            quiet = true,
        )
    }
}
