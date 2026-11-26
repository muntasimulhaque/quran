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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.StorageSummary
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ConfirmSheet
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.launch

/**
 * What the downloads weigh, and the two acts that take the large ones back.
 *
 * The recitation page lists one reciter's surahs and the words page lists one
 * surah's voices. Neither ever added the whole library up, so a reader could
 * not see why the phone was full or clear the audio in one act. This page is
 * the sum and the two "remove all" doors (owner decision, 23.1).
 *
 * The content packs and the partial transfers are facts, not actions. A pack
 * is removed on its own page, where its name and its terms are; a part is the
 * unfinished half of a transfer the reader can resume from the row that
 * started it. The page says what they weigh and leaves the acts where the
 * things are.
 */
@Composable
fun StoragePage(
    summary: suspend () -> StorageSummary,
    onRemoveAllRecitations: suspend () -> Unit,
    onRemoveAllWordAudio: suspend () -> Unit,
) {
    var refresh by remember { mutableIntStateOf(0) }
    val state by produceState<StorageSummary?>(initialValue = null, refresh) {
        value = summary()
    }
    var pending by remember { mutableStateOf<StorageRemoval?>(null) }
    val scope = rememberCoroutineScope()
    val value = state
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        if (value == null) {
            Text(
                text = stringResource(R.string.settings_storage_reading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Space.Gutter, vertical = Space.Row),
            )
        } else {
            StorageRow(
                title = stringResource(R.string.settings_storage_free),
                value = formatBytes(value.freeBytes),
                tag = "storage-free",
            )
            Group(stringResource(R.string.settings_storage_on_phone))
            StorageRow(
                title = stringResource(R.string.settings_storage_content),
                value = formatBytes(value.contentBytes),
                tag = "storage-content",
            )
            StorageRow(
                title = stringResource(R.string.settings_storage_partial),
                value = formatBytes(value.partialBytes),
                tag = "storage-partial",
            )
            Group(stringResource(R.string.settings_storage_downloads))
            StorageRow(
                title = stringResource(R.string.settings_storage_recitations),
                value = formatBytes(value.recitationBytes),
                action = if (value.recitationBytes > 0) {
                    stringResource(R.string.settings_storage_remove_all)
                } else {
                    null
                },
                tag = "storage-recitations",
                actionDescription = stringResource(R.string.settings_storage_remove_recitations_name),
                onAction = { pending = StorageRemoval.Recitations },
            )
            StorageRow(
                title = stringResource(R.string.settings_storage_pronunciation),
                value = formatBytes(value.pronunciationBytes),
                action = if (value.pronunciationBytes > 0) {
                    stringResource(R.string.settings_storage_remove_all)
                } else {
                    null
                },
                tag = "storage-pronunciation",
                actionDescription = stringResource(R.string.settings_storage_remove_pronunciation_name),
                onAction = { pending = StorageRemoval.Pronunciation },
            )
            Text(
                text = stringResource(R.string.settings_storage_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = Space.Gutter,
                    end = Space.Gutter,
                    top = Space.Block,
                ),
            )
            Spacer(Modifier.height(Space.Section))
        }
    }
    when (pending) {
        StorageRemoval.Recitations -> ConfirmSheet(
            name = stringResource(R.string.settings_storage_remove_recitations_name),
            title = stringResource(R.string.settings_storage_remove_recitations_title),
            body = stringResource(R.string.settings_storage_remove_recitations_body),
            confirm = stringResource(R.string.settings_storage_remove_all),
            keep = stringResource(R.string.settings_storage_keep),
            onConfirm = {
                pending = null
                scope.launch {
                    onRemoveAllRecitations()
                    refresh++
                }
            },
            onDismiss = { pending = null },
            tag = "remove-all-recitations",
        )
        StorageRemoval.Pronunciation -> ConfirmSheet(
            name = stringResource(R.string.settings_storage_remove_pronunciation_name),
            title = stringResource(R.string.settings_storage_remove_pronunciation_title),
            body = stringResource(R.string.settings_storage_remove_pronunciation_body),
            confirm = stringResource(R.string.settings_storage_remove_all),
            keep = stringResource(R.string.settings_storage_keep),
            onConfirm = {
                pending = null
                scope.launch {
                    onRemoveAllWordAudio()
                    refresh++
                }
            },
            onDismiss = { pending = null },
            tag = "remove-all-pronunciation",
        )
        null -> Unit
    }
}

/** One line of storage: its name, its weight, and the act the weight allows. */
@Composable
private fun StorageRow(
    title: String,
    value: String,
    tag: String,
    action: String? = null,
    actionDescription: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Space.Gutter - SheetRowGutter,
                end = Space.Row - SheetRowGutter,
                top = Space.Row,
                bottom = Space.Row,
            )
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowName(
            title = title,
            value = value,
            modifier = Modifier.weight(1f).padding(end = Space.Row),
        )
        if (action != null) {
            TextButton(
                label = action,
                onClick = onAction,
                quiet = true,
                modifier = Modifier
                    .testTag("$tag-action")
                    .then(
                        if (actionDescription != null) {
                            Modifier.semantics { contentDescription = actionDescription }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

private enum class StorageRemoval {
    Recitations,
    Pronunciation,
}
