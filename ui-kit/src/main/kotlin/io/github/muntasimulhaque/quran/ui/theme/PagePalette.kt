package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The colors a Mushaf page is drawn with. The theme owns them so the page
 * follows the reader's choice, and the page renderer only asks for them.
 */
data class PagePalette(
    val paper: Color,
    val ink: Color,
    val ornament: Color,
)
