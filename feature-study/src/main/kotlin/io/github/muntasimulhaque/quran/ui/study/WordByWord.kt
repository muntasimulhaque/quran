package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.WordGrid
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.WordMeaning
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * Word by word: each Arabic word with the meaning the word list gives it.
 * The study reading and the ayah card draw the same aid, so a reader who
 * learns it in one place finds it in the other.
 *
 * The aid is a grid, and that is the whole of the change. A flow of tiles
 * each as wide as its own meaning reads as a heap: nothing lines up, the
 * words drift out of the order of the verse, and the eye has to hunt for the
 * pair it was told about. Here every tile is the width of the widest one in
 * this ayah, so the words stand in columns, each word sits over the meaning
 * that is its own, and the meanings of a row share one baseline. The column
 * count comes from the measure the parent actually has, so the aid reads the
 * same on a phone and on a tablet and neither is a number typed in by hand.
 *
 * The word sits centred over its meaning, because the two are one unit: a
 * word hung at one edge of a longer meaning reads as belonging to its
 * neighbour. The Arabic is set at the ayah's own ratio to its translation, so
 * the aid is a small copy of the reading rather than a footnote under it, and
 * both sizes scale with the one choice the reader made for word meanings. Its
 * own line height is tight, because the gap between a word and its meaning is
 * the gap this grid is closing.
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordByWord(
    meanings: List<WordMeaning>,
    hafs: FontFamily,
    settings: AppSettings,
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
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val wordStyle = TextStyle(fontFamily = hafs, fontSize = settings.wordsSp.sp)
    val glossStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = settings.wordsMeaningSp.sp,
    )

    // The longest word and the longest meaning in this ayah, measured once, to
    // learn how wide one tile of the grid wants to be.
    val longestWord = remember(meanings) {
        meanings.maxByOrNull { it.word.length }?.word.orEmpty()
    }
    val longestMeaning = remember(meanings) {
        meanings.maxByOrNull { it.meaning.orEmpty().length }?.meaning.orEmpty()
    }
    val wordWidth = measurer.measure(
        text = longestWord,
        style = wordStyle,
        constraints = Constraints(),
    ).size.width
    val meaningWidth = measurer.measure(
        text = longestMeaning,
        style = glossStyle,
        constraints = Constraints(),
    ).size.width

    BoxWithConstraints(
        // the aid is one surface with one name, so a test can wait for the
        // aid itself rather than for a word that may be scrolled out of a
        // wide screen's tree
        modifier = modifier
            .fillMaxWidth()
            .testTag("word-by-word"),
    ) {
        val available = with(density) { maxWidth.roundToPx() }
        // the grid's width rule is pure and lives in core, where the suite
        // holds it: a tile is as wide as the widest thing in it, never
        // narrower than a word, and never so wide that the aid is one word a
        // line
        val tile = remember(available, wordWidth, meaningWidth) {
            WordGrid.tileWidth(
                availablePx = available,
                wordWidthPx = wordWidth,
                meaningWidthPx = meaningWidth,
                padPx = with(density) { 12.dp.roundToPx() },
            )
        }
        val columns = remember(tile, available) { WordGrid.columns(available, tile) }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(Space.Block),
                maxItemsInEachRow = columns,
            ) {
                meanings.forEachIndexed { index, meaning ->
                    WordTile(
                        meaning = meaning,
                        wordStyle = wordStyle,
                        settings = settings,
                        gloss = gloss,
                        width = with(density) { tile.toDp() },
                        // the word list numbers its words from one, and
                        // they are contiguous, so the tile's place in the
                        // ayah is the number the reciter's timings use
                        onClick = onWord?.let { hear -> { hear(index + 1) } },
                    )
                }
            }
        }
    }
}

/**
 * One word over its meaning, in a column of the grid's own width.
 *
 * The row is measured at its tallest tile, so every word of a row stands on
 * one line and the meanings below them share one baseline, however long the
 * meanings are.
 */
@Composable
private fun WordTile(
    meaning: WordMeaning,
    wordStyle: TextStyle,
    settings: AppSettings,
    gloss: Boolean,
    width: Dp,
    onClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .width(width)
            .height(IntrinsicSize.Min)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                        .semantics { role = Role.Button }
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = meaning.word,
            style = wordStyle.copy(
                lineHeight = (settings.wordsSp * 1.15f).sp,
                // The tiles are the same words the ayah above already shows,
                // so a gloss steps the Arabic one tone down from the verse's
                // own ink: the line stays the verse, the tiles read as its
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
