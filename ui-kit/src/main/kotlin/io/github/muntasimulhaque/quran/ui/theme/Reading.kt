package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How long a line of reading is allowed to grow.
 *
 * A line of text has a length at which the eye stops returning to the margin
 * on its own. Past roughly ninety characters a Latin line costs the reader a
 * re-reading at every wrap, and the wide-tablet study reading was laying a
 * translation across the full 2560 px of a ten inch screen, which is well
 * over two hundred. Paper never does this, and neither should the app: a
 * reading column is capped at a measure a person can actually read and
 * centered on the ground around it.
 *
 * The cap is in dp and moves with the reader's font scale, so a reader who
 * turns the text up gets a shorter line rather than a wider one; the measure
 * is about words per line, not pixels. On a phone, whose width is already
 * under the cap, a surface that uses this keeps the gutter it always had.
 */
object Reading {
    /** The widest a column of reading prose may be. */
    val MaxMeasure: Dp = 620.dp
}
