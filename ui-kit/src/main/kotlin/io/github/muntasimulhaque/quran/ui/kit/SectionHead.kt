package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * The one voice a label over a list wears, wherever the app draws one: the
 * About page's groups, the pack lists' groups, and the pill's listening
 * panel.
 *
 * It is the app's label size at Medium, with the tracking a small label
 * needs, in the secondary ink. It used to wear the accent, which is the color
 * every control that answers a tap wears: a group's name in lapis read as a
 * button the reader could press, and the sheet had one more blue thing than
 * it had doors (owner decision, this session). A label that only names what
 * follows is quiet; the rows under it lead.
 *
 * It is small on purpose. A head that is larger than its rows competes with
 * them; this one steps back and lets the choices lead, which is the opposite
 * of the sheet's own name and is why the two cannot share a voice.
 *
 * It is a heading to a screen reader too. TalkBack has navigated by heading
 * since 9.1, and a reader who cannot see the list can jump from one group of
 * choices to the next instead of hearing every row between them.
 *
 * The air around it belongs to the surface that draws it, because the room a
 * head keeps is the room of the page it stands on and not of the label: a
 * sheet's group opens with more air than a popup's, and both wear this voice.
 * The caller passes it as [modifier].
 */
@Composable
fun SectionHead(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}
