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
 * Arabic reads right to left, so the words wrap that way too.
 */
@Composable
internal fun WordByWord(
    meanings: List<WordMeaning>,
    hafs: FontFamily,
    settings: AppSettings,
    modifier: Modifier = Modifier,
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
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = meaning.meaning.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = settings.wordsMeaningSp.sp,
                            lineHeight = (settings.wordsMeaningSp * 1.45f).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Space.Tight),
                    )
                }
            }
        }
    }
}
