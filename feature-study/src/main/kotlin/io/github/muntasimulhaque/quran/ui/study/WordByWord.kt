package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.platform.testTag
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
 * The aid is a flow of pairs, and each pair is as wide as its own meaning.
 * That is what it was before it became a grid, and it is what the owner
 * put back after reading the grid on a phone: a grid measures every tile
 * against the widest meaning in the ayah, so on a long ayah the tiles are
 * half empty, two of them stand in a row, and the whole aid runs to twice
 * the length of the verse it glosses. A pair that is as wide as its own
 * words reads as one unit whatever the ayah says, and a row of them reads
 * across the screen the way the verse above it does.
 *
 * The cost the grid was made to close is real and it is paid for
 * differently: the pairs in a row are not in columns, so two long meanings
 * can leave a short one in a gap. The eye still never hunts for a pair,
 * because the word sits directly over its own meaning and the flow keeps
 * the verse's own order (owner report, which reverses the grid).
 *
 * The word sits centred over its meaning, because the two are one unit: a
 * word hung at one edge of a longer meaning reads as belonging to its
 * neighbour. The Arabic is set at the ayah's own ratio to its translation,
 * so the aid is a small copy of the reading rather than a footnote under
 * it, and both sizes scale with the one choice the reader made for word
 * meanings.
 *
 * The meaning takes the theme's secondary tone rather than an alpha that
 * just looks quiet: that tone is what each theme defines for secondary
 * text, so the meaning stays readable on paper and on sepia alike.
 *
 * [gloss] says what the aid is standing on. Under the study reading's ayah
 * the pairs repeat words the verse above already shows, so the Arabic steps
 * one tone down and the line stays the verse. In the ayah card, opened from
 * the Mushaf, there is no line above: the aid is the only Arabic there, so
 * it keeps the reading's own ink. The step is small either way, so the word
 * always reads as the Quran's own.
 *
 * The aid is a reading and not a control, and nothing here answers a touch.
 * Hearing one word on its own was in this app and is out of it: the word was
 * reached by a tap that could not be offered without a mark, a way out, and
 * a reciter ready, and the reader has the whole verse in front of them
 * either way. The ayah is heard from the ayah, where Play already is, a
 * long press away (owner decision).
 *
 * Arabic reads right to left, so the pairs wrap that way too.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordByWord(
    meanings: List<WordMeaning>,
    hafs: FontFamily,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    gloss: Boolean = true,
) {
    if (meanings.isEmpty()) return
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            // the aid is one surface with one name, so a test can wait for the
            // aid itself rather than for a word that may be scrolled out of a
            // wide screen's tree
            modifier = modifier
                .fillMaxWidth()
                .testTag("word-by-word"),
            horizontalArrangement = Arrangement.spacedBy(Space.Block),
            verticalArrangement = Arrangement.spacedBy(Space.Block),
        ) {
            meanings.forEach { meaning ->
                WordPair(
                    meaning = meaning,
                    settings = settings,
                    hafs = hafs,
                    gloss = gloss,
                )
            }
        }
    }
}

/**
 * One word over its own meaning, as wide as the two of them and no wider.
 *
 * The pair is type and not a control: a role on this node would merge its
 * two lines into a single labelled node, which hides the word and the
 * meaning from anything that reads the aid as text, and a shape around it
 * would read as a button where there is nothing to press.
 */
@Composable
private fun WordPair(
    meaning: WordMeaning,
    settings: AppSettings,
    hafs: FontFamily,
    gloss: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = meaning.word,
            style = TextStyle(
                fontFamily = hafs,
                fontSize = settings.wordsSp.sp,
                lineHeight = (settings.wordsSp * 1.25f).sp,
                // The pairs are the same words the ayah above already shows,
                // so a gloss steps the Arabic one tone down from the verse's
                // own ink: the line stays the verse, the pairs read as its
                // gloss. Still well above the reading contrast on every
                // theme. Standalone, the aid is the reading.
                color = if (gloss) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
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
