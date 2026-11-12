package io.github.muntasimulhaque.quran

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp

/**
 * Presses and holds the first word of an Arabic line.
 *
 * The line is drawn right to left on top of its box, so its first word stands
 * at the box's own top right corner, and this holds the corner a hair inside
 * both edges rather than at a measure of them.
 *
 * The hairs are the point. The press used to be 32 px inside the right edge
 * and half the box's height down, and neither number survives a change of
 * glass. On a 601 dp tablet, 32 px is 24 dp, past the ink of a short first
 * word, بِسۡمِ, which is about 22 dp wide there, so the finger landed in the
 * space between the first word and the second, where no word answers and the
 * word sheet never opened (owner report, quran_tablet7). And the middle of the
 * box is not always words either: a verse that wraps ends its last line in the
 * printed medallion, and Al-Baqarah 2:7 on that tablet wraps to a second line
 * that holds the medallion and nothing else, so the old press held the
 * ornament and the sheet could not open at all, on every inset. The space
 * between words and the medallion are both the app's own drawing, so a press
 * sized in pixels, or aimed at the middle of a box, can only be right on one
 * arrangement. In dp, at the corner the paragraph starts from, it is inside
 * the first word on every glass and at every one of the reader's five type
 * sizes.
 */
internal fun SemanticsNodeInteraction.holdTheFirstWord() = performTouchInput {
    longClick(Offset(width - 2.dp.toPx(), 2.dp.toPx()))
}
