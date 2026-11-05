package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * The place a small sheet is about: the surah and the ayah, or the ayah
 * alone, in the app's meta line under the sheet's own title.
 *
 * A sheet names itself at [SheetTitle]'s 20, and the place it holds is one
 * step quieter: 13 Regular on the quiet ink, the same line every source and
 * value in the app wears. The footnote and the word meaning are the same kind
 * of surface, so they read the place the same way, and a title never has to
 * share a line with the reference it opens (owner decision, this session).
 *
 * The alignment belongs to the sheet, not to the line: a footnote is prose
 * and keeps the page's left edge, while the word card is one entry and centers
 * its place. The size, weight, and tone are the same in both.
 */
@Composable
fun SheetPlace(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier,
    )
}
