package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The one page the app draws, and how much glass a page leaves over.
 *
 * There are two numbers here and they are one rule. A window that holds no
 * more than a page is the page: its words stand [Space.Gutter] inside the
 * glass and the line runs to the far edge. A window wider than a page is a
 * page on a desk: the words keep [Space.Measure] and the glass keeps what is
 * left as margin. Nothing in the app decides a width by counting characters,
 * and nothing reads the window's width outside this file (owner decision,
 * this session).
 *
 * The page and the sheet are the same page. A sheet is the page plus its own
 * two gutters, so its edges land on the reading's lines behind it, and the
 * rows inside it keep the one gutter everything else keeps. A sheet draws the
 * page it holds and never a wider one, which is why the sheet's own width is
 * [sheetWidth] and not the window: the ten inch screen used to hand its
 * sheets 1280 dp of row, and a settings row's switch stood most of a screen
 * away from the words that name it (owner report, this session).
 */

/**
 * How far the words of a full-width surface stand from the edge of its glass.
 *
 * A phone, and the seven inch tablet standing up, take [Space.Gutter] and
 * nothing else, because they are narrower than a page. A wider glass takes
 * half of what it has over the page, which is more than a gutter, so the
 * larger of the two is always the answer and the rule has no seam in it: at
 * exactly a page plus two gutters the two halves meet at [Space.Gutter].
 *
 * A window that has not been measured yet reads as narrower than a page and
 * takes the gutter, so a cold start's first frame is a phone's page and never
 * a margin of nothing.
 */
fun pageMargin(windowWidth: Dp): Dp {
    val over = (windowWidth - Space.Measure) / 2
    return if (over > Space.Gutter) over else Space.Gutter
}

/**
 * The width to hand a sheet: the page and its two gutters, or the glass when
 * the glass is narrower than that.
 *
 * So the sheet is the page. On the phone and the seven inch tablet it is the
 * whole window, exactly as before. On the ten inch screen it is 656 dp
 * centered, and the reading behind it is a 600 dp page with 28 dp of its own
 * air on each side: the sheet's edge and the page's edge are the same line.
 */
fun sheetWidth(windowWidth: Dp): Dp {
    val page = Space.Measure + Space.Gutter * 2
    return if (windowWidth < page) windowWidth else page
}

/**
 * The page's margin, from the window the caller stands in.
 *
 * The window is read rather than the screen, so a landscape split, a fold,
 * and a display cutout all move the page with the glass it is drawn on.
 */
@Composable
fun rememberPageMargin(): Dp {
    val density = LocalDensity.current
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    return pageMargin(windowWidth)
}

/**
 * The width to hand the sheet, from the window the call stands in.
 *
 * A window that has not been measured yet takes no page at all rather than a
 * zero width sheet, so the first frame of a cold start is a whole sheet and
 * never a sliver: [Dp.Infinity] is the measurement that asks for the parent's
 * own width.
 */
@Composable
fun rememberSheetWidth(): Dp {
    val density = LocalDensity.current
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    return if (windowWidth > 0.dp) sheetWidth(windowWidth) else Dp.Infinity
}
