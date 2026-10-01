package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton

/** One choice among several: the reader takes one, and the mark says which. */
@Composable
fun ChoiceRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
    /** The room under the row; a row with a block beneath it keeps less. */
    bottomPadding: Dp = 12.dp,
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        selected = selected,
        role = Role.RadioButton,
        onClick = onClick,
        bottomPadding = bottomPadding,
        trailing = trailing,
    ) { Mark(selected = selected, radio = true) }
}

/** A row that can be on or off among many, with a check for its state. */
@Composable
fun MarkRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        selected = selected,
        role = Role.Checkbox,
        onClick = onClick,
        trailing = trailing,
    ) { Mark(selected = selected, radio = false) }
}

/**
 * One pack in a list of choices: a mark for its state, its name, and the one
 * action it needs. The mark and the name are one control, so a reader who
 * taps the name of a pack they do not have yet is asking for it, with the
 * size already on the row in front of them; a reader who taps a name they do
 * have turns it on or off. The action at the right is the same door for a
 * reader who looks for a button instead of a row.
 *
 * Radios for a reciter, checks for translations, tafsirs, and word lists:
 * one reciter is heard at a time, while more than one reading may be on.
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
    SettingRow(
        title = pack.name,
        subtitle = subtitle,
        selected = selected,
        role = if (radio) Role.RadioButton else Role.Checkbox,
        // A pack that is not here yet cannot be turned on: its row is the
        // door that brings it here instead, and nothing looks selectable
        // while it is on its way in.
        onClick = if (pack.installed && !busy) onActivate else onInstall,
        bottomPadding = bottomPadding,
        trailing = { PackTrailing(pack, setup, onInstall, onRemove) },
    ) { Mark(selected = selected, radio = radio) }
}

/**
 * A row with a mark at its left, the way a list of choices is read: the mark
 * comes before the name, so the eye lands on the state first and the name
 * reads as the thing it belongs to. The whole row is one tappable control.
 *
 * The mark and the name keep a clear gap between them: a radio set against
 * the word it selects reads as one crowded glyph, and the reader has to look
 * twice to see which mark belongs to which name.
 */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
    bottomPadding: Dp = 12.dp,
    trailing: @Composable () -> Unit,
    mark: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, role = role, onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mark()
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Box(Modifier.padding(start = 12.dp)) { trailing() }
    }
}

/** The app's own mark: a drawn ring and dot, or a check, never a stock icon. */
@Composable
private fun Mark(selected: Boolean, radio: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.size(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            if (radio) {
                drawCircle(
                    // An unselected mark is a control boundary, so it holds the
                    // 3:1 a control owes; 0.6 of the accent was measured at
                    // 3.0:1 and up on all four grounds, where the 0.35 it wore
                    // measured 1.8:1 and read as nothing.
                    color = if (selected) accent else accent.copy(alpha = 0.6f),
                    radius = w * 0.42f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f),
                )
                if (selected) {
                    drawCircle(color = accent, radius = w * 0.2f)
                }
            } else if (selected) {
                drawLine(
                    color = accent,
                    start = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.52f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.78f),
                    strokeWidth = w * 0.13f,
                )
                drawLine(
                    color = accent,
                    start = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.78f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.22f),
                    strokeWidth = w * 0.13f,
                )
            } else {
                drawCircle(
                    // The same control-boundary rule as the radio mark above.
                    color = accent.copy(alpha = 0.6f),
                    radius = w * 0.42f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f),
                )
            }
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

