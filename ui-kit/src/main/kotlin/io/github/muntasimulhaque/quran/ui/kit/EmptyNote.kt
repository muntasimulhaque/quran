package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.screenSubtitle

/**
 * What a page says before it has anything on it: a title that names what the
 * page is for, and one sentence that says what will stand there and what puts
 * it there.
 *
 * Browse's Saved and Last Read lists and the search sheet before a query draw
 * the same note, because they are the same moment: the reader has already
 * chosen the surface and is only owed the truth about it. So it is two lines
 * of type, the title in the page's ink and its sentence under it in the quiet
 * tone, on the page's own line and centered there. Nothing else is drawn: no
 * icon, no card, no picture, no button. A note that also offers something is a
 * page selling a feature to a reader who came to read, and the two surfaces
 * that want this note want it quiet (owner decision, this session).
 *
 * The words belong to the page that shows them, because an empty Saved list
 * and an empty search ask different things of a reader; only the voice is
 * shared. So do the airs: the caller passes them, because a sheet with a
 * field above the note and a keyboard below it keeps different room than a
 * list that begins under a page's head.
 *
 * The sentence is instructive and never a placeholder. It names the act that
 * fills the page ("Press and hold any ayah while reading, then Save") or the
 * forms the page accepts ("a reference like 2:255"), so a reader who reads it
 * knows what to do without touching anything.
 */
@Composable
fun EmptyNote(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.Gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = screenSubtitle(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Line),
        )
    }
}
