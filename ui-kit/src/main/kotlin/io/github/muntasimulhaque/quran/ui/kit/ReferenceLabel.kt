package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * The app's one reference label: an ayah named where it is a label rather than
 * a title, at the head of a result and on the reading's own door.
 *
 * It is the meta line's 13 at Medium with the figures tabular and a touch of
 * tracking, in the secondary ink. The two were 12 and lapis in two different
 * places: the search result's reference wore the action color, so a number the
 * reader could not tap read as a button, while the reading's door was the same
 * size in ink. One role now, and only a reference that is a passage's title
 * (the head of a tafsir) wears the title voice instead (owner decision, this
 * session).
 *
 * The color is a parameter because a door says its state in the action color
 * while its reference is open; every quiet reference keeps the default.
 */
@Composable
fun ReferenceLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
            fontFeatureSettings = "tnum",
        ),
        color = color,
        textAlign = textAlign,
        modifier = modifier,
    )
}
