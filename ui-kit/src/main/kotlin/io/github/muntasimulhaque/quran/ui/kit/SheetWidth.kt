package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The width a sheet takes, read from the window it is drawn in.
 *
 * A sheet is the glass: the app's page is as wide as the window it is raised
 * in, on every form factor, and the one margin that holds its words (the
 * theme's `Space.Gutter`) is the whole of what stands between a row and the
 * edge of the page.
 *
 * The three answers this file used to give were the 65 character measure
 * arriving on the sheet. The phone and the seven inch tablet took their
 * window, because the fixed page would have left them a sliver; the ten inch
 * screen kept that fixed page centered on a glass nearly three times its
 * width. So the reader met a page that did not match the reading under it, on
 * the one surface with room to spare (owner decision, this session).
 *
 * The window is read rather than the screen, so a landscape split, a fold,
 * and a display cutout all move the page with the glass it is drawn on. The
 * window is read where the sheet's call stands (the app's own window), before
 * the sheet's dialog window exists, and it is the width the sheet is about to
 * be given.
 */
fun sheetWidth(windowWidth: Dp): Dp = windowWidth

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
