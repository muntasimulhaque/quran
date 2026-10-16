package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Reading

/**
 * The width a sheet takes, read from the window it is drawn in.
 *
 * [Reading.sheetWidth] owns the rule; this is where the window's own width
 * comes from. The window is read rather than the screen, so a landscape
 * split, a fold, and a display cutout all move the page with the glass it is
 * drawn on, the way the reading's own column does. The window is read where
 * the sheet's call stands (the app's own window), before the sheet's dialog
 * window exists, and it is the width the sheet is about to be given.
 *
 * A window that has not been measured yet keeps the page rather than taking a
 * zero-width sheet, so the first frame of a cold start is a whole sheet and
 * never a sliver.
 */
@Composable
fun rememberSheetWidth(): Dp {
    val density = LocalDensity.current
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    return if (windowWidth > 0.dp) Reading.sheetWidth(windowWidth) else Reading.SheetMeasure
}
