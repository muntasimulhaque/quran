package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph

/**
 * The reciters, in a menu the pill opened.
 *
 * One list, in one shape, wherever the choice is offered: the playing pill
 * carries it beside the reciter it is hearing, and the offer carries it
 * above the size it is asking for, and a reader who learned one has learned
 * both. A reciter who is already chosen wears the check, and every other row
 * says what that reciter would still need for the surah at hand, so the
 * choice is made with the price in view rather than by trying it.
 *
 * The menu wears the pill's own cloth: the same surface color, the same
 * rounded shape of its own, and no depth the pill does not have. The pill
 * carries no border and casts no shadow, so a menu that did read as a
 * foreign sheet laid over it.
 *
 * Its measure is the pill's own [PillMenuMeasure], which is also the measure
 * of the anchor the playing pill hangs it from: a menu takes the left edge of
 * the anchor it is given, so the two have to be one width for the menu to
 * stand on the pill's centre (owner report).
 */
@Composable
internal fun ReciterMenu(
    open: Boolean,
    options: List<ListenOption>,
    /** The reciter in use, by id; null where none of them is. */
    selected: String?,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = open,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.width(PillMenuMeasure),
    ) {
        options.forEach { option ->
            ReciterChoiceRow(
                option = option,
                selected = option.reciter == selected,
                onClick = { onChoose(option.reciter) },
            )
        }
    }
}

/**
 * One reciter in the chooser: the name, what still needs fetching, and a
 * check on the one already chosen. It is rounded and it is the pill's own
 * surface, so the choice reads as part of the pill it opened from.
 */
@Composable
private fun ReciterChoiceRow(
    option: ListenOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = option.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (option.bytes > 0L) {
                    formatBytes(option.bytes)
                } else {
                    stringResource(R.string.playback_reciter_ready)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            IconGlyph(
                icon = Icon.Check,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(18.dp),
            )
        }
    }
}
