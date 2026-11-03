package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The one measure the search sheet's chips share: the filter's source chips
 * and the recent searches.
 *
 * The capsule is the label's own line plus [Space.Line] above and below, the
 * same pill both chips draw. At the default a 20 sp line makes a 36 dp
 * capsule in a 48 dp target, and the target's six dp above and below each
 * capsule is the 12 dp block break between two rows, so the flow adds nothing
 * of its own. At 200 percent a 40 sp line makes a 56 dp capsule in a 56 dp
 * target, the target's air is gone, and the flow adds the 12 dp itself. The
 * fixed 36 dp box used to clip the line at the largest text size, and the
 * flow's fixed gap then had nothing left to keep two rows apart (owner
 * decision, this session).
 *
 * The press circle is the default capsule's 36 dp, or the capsule itself when
 * the reader's text is smaller than the default. A mark stays 18 dp: it is a
 * drawing, and the word beside it is the thing that scales.
 */
internal data class ChipMeasure(
    /** The capsule the reader sees. */
    val pill: Dp,
    /** The target the finger gets, never under the app's 48 dp floor. */
    val target: Dp,
    /** The X's press circle, kept inside the capsule's cap. */
    val press: Dp,
    /** The flow's own gap after the target's air has done what it can. */
    val rowGap: Dp,
) {
    /** The capsule's radius, so the word's half meets the X's circle. */
    val radius: Dp get() = pill / 2
}

/**
 * The measure of one chip, read from the type it holds.
 *
 * The fallback pill is the default 36 dp, for a theme that left the label's
 * line unspecified; every theme this app ships sets it.
 */
@Composable
internal fun chipMeasure(): ChipMeasure {
    val density = LocalDensity.current
    val line = MaterialTheme.typography.labelLarge.lineHeight
    val pill = if (line.isSp) {
        with(density) { line.toDp() } + Space.Line * 2
    } else {
        36.dp
    }
    val target = maxOf(48.dp, pill)
    return ChipMeasure(
        pill = pill,
        target = target,
        press = minOf(36.dp, pill),
        // Two capsules stand Space.Row apart, or a little more when a
        // smaller capsule's target leaves more air: the target air above and
        // below each capsule, plus this gap, is the block break.
        rowGap = (Space.Row - (target - pill)).coerceAtLeast(0.dp),
    )
}
