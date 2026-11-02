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
 * [highlight] is the wash that follows a recitation at the ayah level. Each
 * theme sets its own, because a lapis wash that reads as ink on paper reads
 * as nothing at all on a night ground.
 *
 * [recitationInk] is the word being heard: the one word the reciter is on is
 * recolored rather than washed, so its tone must be opaque, past the contrast
 * floor on every ground the line is drawn on, and far enough from each of the
 * seven tajweed families that it cannot be read as another rule.
 *
 * [introInk] is the softer tone the surah's opened introduction wears: the
 * reading's ink at 0.78, a page that steps back under the wash it stands on.
 * [mastheadInk] is the first paint's display tone, the interface's word at
 * 0.9, because a 48 sp line does not need the reading's full ink. Both live
 * here so the theme owns them and the contrast test can measure them, rather
 * than each call site mixing its own alpha (owner decision, this session).
 */
data class PagePalette(
    val ink: Color,
    val ornament: Color,
    val highlight: Color,
    /**
     * The ink of the word being heard. The reciter's word is recolored, not
     * washed, so this tone is opaque and has to stand apart from the seven
     * tajweed families as well as past the contrast floor on the page, the
     * sheet, and the reciter's own wash (owner decision, this session).
     */
    val recitationInk: Color,
    val tajweed: TajweedColors,
    val introInk: Color,
    val mastheadInk: Color,
)
