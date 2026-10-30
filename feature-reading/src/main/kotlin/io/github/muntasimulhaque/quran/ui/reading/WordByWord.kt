package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.WordMeaning
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.prose
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * Word by word: each Arabic word with the meaning the word list gives it,
 * drawn under the ayah of the reading, where the verse above the pairs
 * already shows the words they gloss.
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
 * The word sits centered over its meaning, because the two are one unit: a
 * word hung at one edge of a longer meaning reads as belonging to its
 * neighbour. The Arabic is set at the aid's own ratio to its meaning, so the
 * aid is a small copy of the reading rather than a footnote under it, and
 * both sizes scale with the one choice the reader made for word meanings.
 *
 * The meaning takes the reading's ink rather than an alpha that
 * just looks quiet or the theme's secondary tone: body copy in the
 * secondary tone tires the eye over a long reading, so the meaning
 * stays readable on paper and on night alike.
 *
 * The aid is a reading and not a control, and nothing here answers a touch.
 * Hearing one word on its own was in this app and is out of it: the word was
 * reached by a tap that could not be offered without a mark, a way out, and
 * a reciter ready, and the reader has the whole verse in front of them
 * either way. The ayah is heard from the ayah, where Play already is, a tap
 * away (owner decision).
 *
 * Arabic reads right to left, so the pairs wrap that way too.
 */
/** The step the word list's Arabic takes down from the verse's own ink. */
private const val WordGlossAlpha = 0.78f

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordByWord(
    meanings: List<WordMeaning>,
    hafs: FontFamily,
    metrics: ReadingMetrics,
    /** The words a search matched by meaning, washed in the match color. */
    highlightWords: Set<String> = emptySet(),
    highlightColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
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
            // The pairs are separated by a section's air, not a block's: on a
            // wide page a row holds five or six pairs, and at a block's gap
            // they read as one run of words instead of five units (owner
            // decision, this session).
            horizontalArrangement = Arrangement.spacedBy(Space.Section),
            verticalArrangement = Arrangement.spacedBy(Space.Block),
        ) {
            meanings.forEach { meaning ->
                WordPair(
                    meaning = meaning,
                    metrics = metrics,
                    hafs = hafs,
                    highlighted = meaning.word in highlightWords,
                    highlightColor = highlightColor,
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
    metrics: ReadingMetrics,
    hafs: FontFamily,
    highlighted: Boolean,
    highlightColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val palette = LocalPagePalette.current
        TajweedText(
            text = AnnotatedString(meaning.word),
            code = meaning.tajweed,
            colors = palette.tajweed,
            enabled = metrics.tajweed,
            // The pairs are the same words the ayah above already shows.
            // The uncolored letters step one tone down from the verse's own
            // ink; the rule-colored letters keep the palette's full color,
            // because a hue at the gloss alpha would fall under the contrast
            // floor. The aid stays a gloss by its size and its meanings, not
            // by dimming the rules (owner report, the QPC rebuild audit).
            style = arabicStyle(
                hafs,
                metrics.wordsSp,
                metrics.wordsLineSp,
                palette.ink.copy(alpha = WordGlossAlpha),
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            wash = if (highlighted) listOf(meaning.word.indices) else emptyList(),
            washColor = highlightColor,
            // The gloss is heard in the portable form, the same words a
            // speech engine can pronounce (owner decision, the palette
            // session).
            modifier = Modifier.semantics {
                contentDescription = Arabic.portableText(meaning.word)
            },
        )
        Text(
            text = meaning.meaning.orEmpty(),
            style = LocalReadingVoice.current.prose(
                metrics.wordsMeaningSp,
                metrics.wordsMeaningLineSp,
            ),
            color = LocalPagePalette.current.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.Tight),
        )
    }
}
