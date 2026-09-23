package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.WordMeaning
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * Word by word: each Arabic word with the meaning the word list gives it.
 * The study reading and the ayah card draw the same aid, so a reader who
 * learns it in one place finds it in the other.
 *
 * The word sits centered over its meaning, because the two are one unit: a
 * word hung at one edge of a longer meaning reads as belonging to its
 * neighbor. The Arabic is set at the ayah's own ratio to its translation, so
 * the aid is a small copy of the reading rather than a footnote under it, and
 * both sizes scale with the one choice the reader made for word meanings.
 *
 * The meaning takes the theme's secondary tone rather than an alpha that
 * just looks quiet: that tone is what each theme defines for secondary
 * text, so the meaning stays readable on paper and on sepia alike.
 *
 * [gloss] says what the aid is standing on. Under the study reading's ayah
 * the tiles repeat words the verse above already shows, so the Arabic steps
 * one tone down and the line stays the verse. In the ayah card, opened from
 * the Mushaf, there is no line above: the aid is the only Arabic there, so
 * it keeps the reading's own ink. The step is small either way, so the word
 * always reads as the Quran's own.
 *
 * Arabic reads right to left, so the words wrap that way too.
 */
@Composable
internal fun WordByWord(
    meanings: List<WordMeaning>,
    hafs: FontFamily,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    gloss: Boolean = true,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.Block),
            verticalArrangement = Arrangement.spacedBy(Space.Block),
        ) {
            meanings.forEach { meaning ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = meaning.word,
                        style = TextStyle(
                            fontFamily = hafs,
                            fontSize = settings.wordsSp.sp,
                            lineHeight = (settings.wordsSp * 1.8f).sp,
                            // The tiles are the same words the ayah above
                            // already shows, so a gloss steps the Arabic one
                            // tone down from the verse's own ink: the line
                            // stays the verse, the tiles read as its gloss.
                            // Still well above the reading contrast on every
                            // theme. Standalone, the aid is the reading.
                            color = if (gloss) {
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                        ),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = meaning.meaning.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = settings.wordsMeaningSp.sp,
                            lineHeight = (settings.wordsMeaningSp * 1.45f).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Space.Tight),
                    )
                }
            }
        }
    }
}
