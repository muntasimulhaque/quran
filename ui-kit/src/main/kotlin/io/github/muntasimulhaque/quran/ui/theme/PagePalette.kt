package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The colors a Mushaf page is drawn with. The theme owns them so the page
 * follows the reader's choice, and the page renderer only asks for them.
 *
 * [selection] and [highlight] are the two washes the page can carry: the one
 * under a chosen ayah, and the one that follows a recitation. Each theme sets
 * its own, because a lapis wash that reads as ink on paper reads as nothing at
 * all on a night ground.
 */
data class PagePalette(
    val paper: Color,
    val ink: Color,
    val ornament: Color,
    val selection: Color,
    val highlight: Color,
)
