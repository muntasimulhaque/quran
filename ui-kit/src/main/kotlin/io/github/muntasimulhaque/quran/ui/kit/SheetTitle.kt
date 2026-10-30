package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * The one name a sheet wears, wherever the app raises one.
 *
 * The app had four: Settings named itself at 20, the note sheet at 16, and
 * the tafsir and footnote sheets at 12, which made the tafsir card's own name
 * its smallest line while a heading inside its text stood above its passage
 * title. A sheet names itself once, in the largest fixed voice it owns, so
 * the reader knows what they opened before they read what it holds. It is
 * 18: a quiet utility header, under Apple's Title 3 (20) and above the 15
 * body. The first attempt at this ladder used 24, and the settings hub lost
 * its last row on a phone; 20 still read as a display line for a utility
 * sheet; 18 is the header that leaves the list its room and stays out of the
 * reader's eyes (owner decision, this session).
 *
 * It is a heading to a screen reader. TalkBack has navigated by heading since
 * 9.1, and a reader who cannot see the sheet can jump to its name instead of
 * hearing the control that opened it and every row under it.
 *
 * The air around the name belongs to the surface that draws it, because a
 * sheet's head keeps the sheet's room and a page's head keeps the page's. The
 * caller passes it as [modifier].
 */
@Composable
fun SheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}
