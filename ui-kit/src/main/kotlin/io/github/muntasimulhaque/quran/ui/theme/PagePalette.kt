package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The colors the reading is drawn with. The theme owns them so the page
 * follows the reader's choice, and the page renderer only asks for them.
 *
 * [ink] is the reading's own text: the Quran, the translation, the tafsir,
 * and the word meanings. It is deliberately softer than the interface's word
 * on the night page, where a bright white word blooms on a dark ground over a
 * long read; on paper it is the same near black every other word wears.
 *
 * [highlight] is the wash that follows a recitation. Each theme sets its own,
 * because a lapis wash that reads as ink on paper reads as nothing at all on
 * a night ground.
 */
data class PagePalette(
    val ink: Color,
    val ornament: Color,
    val highlight: Color,
    val tajweed: TajweedColors,
)
