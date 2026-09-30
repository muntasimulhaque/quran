package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
 * That is what it was before D-122 made it a grid, and it is what the owner
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
 * the verse's own order (owner report, D-130, which reverses the grid of
 * D-122).
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
 * the tiles repeat words the verse above already shows, so the Arabic steps
 * one tone down and the line stays the verse. In the ayah card, opened from
 * the Mushaf, there is no line above: the aid is the only Arabic there, so
 * it keeps the reading's own ink. The step is small either way, so the word
 * always reads as the Quran's own.
 *
 * Arabic reads right to left, so the pairs wrap that way too.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordByWord(
    meanings: List<WordMeaning>,
    hafs: FontFamily,
    settings: AppSettings,
    /** What the tap on a pair does, said to a screen reader. */
    hearLabel: String,
    modifier: Modifier = Modifier,
    gloss: Boolean = true,
    /**
     * Hears one word again, given the word's own number. The tap lives on the
     * aid and not on the verse: the verse's tap belongs to the reader's bar and
     * its long press to the ayah's actions, and a third meaning on the same
     * surface takes one of them away (the 3.1 capture found both). Here the
     * reader is already looking at the words, and nothing else is waiting for
     * the gesture.
     */
    onWord: ((Int) -> Unit)? = null,
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
            meanings.forEachIndexed { index, meaning ->
                WordPair(
                    meaning = meaning,
                    settings = settings,
                    hafs = hafs,
                    gloss = gloss,
                    // the word list numbers its words from one, and they are
                    // contiguous, so the pair's place in the ayah is the
                    // number the reciter's timings use
                    onClick = onWord?.let { hear -> { hear(index + 1) } },
                    hearLabel = hearLabel,
                )
            }
        }
    }
}

/**
 * One word over its own meaning, as wide as the two of them and no wider.
 *
 * A pair is a control, and it must not *read* as one: a role on this node
 * merges its two lines into a single labelled node, which hides the word and
 * the meaning from anything that reads the aid as text, screen reader and
 * test alike. The tap is here, and what it does is said, without swallowing
 * what the pair says.
 */
@Composable
private fun WordPair(
    meaning: WordMeaning,
    settings: AppSettings,
    hafs: FontFamily,
    gloss: Boolean,
    onClick: (() -> Unit)?,
    hearLabel: String,
) {
    Column(
        modifier = Modifier
            // A pair is as small as a finger can be trusted with, which is the
            // app's own floor: a word above one meaning line is close to it
            // already, and a short meaning must not take the target below it.
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            modifier = Modifier
                .padding(top = Space.Tight)
                .semantics {
                    // the door the tap opens, said without a role that would
                    // merge away the pair's own words
                    if (onClick != null) {
                        customActions = listOf(
                            CustomAccessibilityAction(hearLabel) {
                                onClick()
                                true
                            },
                        )
                    }
                },
        )
    }
}