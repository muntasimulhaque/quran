package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The one question the app asks before it takes something it cannot give
 * back: a saved ayah that carries the reader's own note, and a downloaded
 * recitation that took minutes of the reader's connection to arrive.
 *
 * The shape was written twice, once in the reading and once in Settings, and
 * the second copy described itself as the first one's shape. Two copies of a
 * question match until the next edit, and a reader then meets the same
 * question drawn two ways (owner decision, 19.6).
 *
 * The question wears the app's title voice, what is at stake is drawn under
 * it, and the answers are the pair every removing question carries: the quiet
 * word for the act and the prominent one for the safe answer, which leads
 * because it is the one the sheet recommends (owner decision, 6.6).
 *
 * [tag] names the three nodes a test reads: `"<tag>-title"`, `"<tag>-confirm"`,
 * and `"<tag>-keep"`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSheet(
    name: String,
    title: String,
    body: String,
    confirm: String,
    keep: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    /** What the question is about, drawn under it: the note, or the weight at stake. */
    atStake: (@Composable () -> Unit)? = null,
) {
    val sheetState = rememberExpandedSheetState()
    AppSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        name = name,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                // The gate rides with the scroll, so an upward flick on the
                // question never bounces the sheet on its top anchor.
                .sheetVerticalScroll(rememberScrollState())
                .padding(horizontal = Space.Gutter, vertical = Space.Row)
                .padding(bottom = Space.SheetFoot),
        ) {
            Text(
                text = title,
                // The sheet's own name, in the app's one title voice.
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .semantics { heading() }
                    .testTag("$tag-title"),
            )
            atStake?.invoke()
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Line),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Block),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    label = confirm,
                    onClick = onConfirm,
                    modifier = Modifier.testTag("$tag-confirm"),
                    quiet = true,
                )
                TextButton(
                    label = keep,
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(start = Space.Action)
                        .testTag("$tag-keep"),
                    prominent = true,
                )
            }
        }
    }
}
