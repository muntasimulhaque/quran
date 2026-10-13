package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.uikit.R

/**
 * The one handle every sheet in the app wears: the bar the eye reads, inside
 * a 48 dp-wide and 48 dp-tall target.
 *
 * Material's own handle is a 32 dp-wide target, narrower than a finger and
 * narrower than the rule, and it carries the spoken name "Drag handle". This
 * handle keeps the name, because a bar a reader can touch without hearing
 * what it is is worse than no bar, and widens the target to the rule, which
 * is what the accessibility test asked for (owner decision, this session).
 */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.sheet_handle)
    Box(
        modifier = modifier
            .width(48.dp)
            .height(48.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}
