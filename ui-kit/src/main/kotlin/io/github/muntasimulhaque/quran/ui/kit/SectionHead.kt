package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight

/**
 * The one voice a label over a list wears, wherever the app draws one: the
 * settings hub's groups, the ayah card's tafsir doors, and the pill's
 * listening panel.
 *
 * It is the app's small label size in the accent, at a weight above the choice
 * rows it opens, so it reads as the name of what follows rather than as
 * another row: a label that wore the same voice as the things under it made a
 * list read as one undifferentiated block, and the air between the rows had no
 * rhythm to lean on (owner report, this session: the pill's own panel was
 * twelve lines of one size). It is small on purpose. A head that is larger
 * than its rows competes with them; this one steps back and lets the choices
 * lead, which is the opposite of the row's own name and is why the two cannot
 * share a voice.
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
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() },
    )
}
