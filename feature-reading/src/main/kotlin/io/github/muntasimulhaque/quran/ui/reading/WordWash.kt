package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import io.github.muntasimulhaque.quran.ui.rich.drawWash

/**
 * A word's wash: the app's one recipe, one rounded shape per line the word
 * spans. Used by a search match, by the reciter's word, and by the word
 * sheet while the word's own voice sounds: the page's one place mark, under
 * the letters, at the strongest tint the seven rule colors keep their
 * contrast on.
 *
 * The geometry lives with `ui-kit`'s [drawWash], one recipe for every
 * matched run in the app, so the reading's marks and the search sheet's
 * matches cannot drift apart (owner decision, 21.1). This is the reading's
 * name for one range of it, lifted verbatim, so the marks do not move.
 */
internal fun DrawScope.drawWordWash(
    layout: TextLayoutResult,
    range: IntRange,
    wash: Color,
) {
    drawWash(layout, listOf(range), wash)
}
