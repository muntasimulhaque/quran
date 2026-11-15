package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The colors the reading is drawn with. The theme owns them so the page
 * follows the reader's choice, and the page renderer only asks for them.
 *
 * [ink] is the reading's own text: the Quran, the translation, the tafsir,
 * and the word meanings. It is deliberately softer than the interface's word
 * on both grounds: on the night page a bright white word blooms on a dark
 * ground over a long read, and on paper the interface's near black is the tone
 * a label wants while a page read for an hour wants ink. The day page carries
 * [PaperReadingInk] at 12.1:1 and the night [NightInk] at 12.1:1, each one a
 * step under the word its buttons and labels wear (owner decision, the long
 * read session).
 *
 * [highlight] is the word mark on the reading's line: the word the reciter
 * is saying and the word a search arrived on. Each theme sets its own,
 * because a lapis wash that reads as ink on paper reads as nothing at all on
 * a night ground. Its alpha is a contrast ceiling, not a taste: the seven
 * rule colors are drawn over it, and one drop more puts the closest of them
 * under 4.5:1.
 *
 * [selected] is the chosen thing's ground, and it is opaque and warm: the
 * ayah whose menu is open, the introduction the reader opened, the row Browse
 * is standing on, and the word the word sheet is naming. It is the page's own
 * family and never a tint of the accent, so the one surface in the app that
 * says "this one" says it in the paper's voice (owner decision, this
 * session). See [PaperSelected] for the measurement.
 *
 * [match] is its sibling for the thing that was *found* rather than chosen:
 * the word a search matched, and the text a reader selected in a field. Same
 * family, same opaque treatment, one step louder. See [PaperMatch].
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
    /**
     * The one word mark on the reading's own line: the word being heard and
     * the word a search arrived on. It is a wash and not a recolored word, so
     * the letters keep their own ink and their tajweed colors. Its alpha is
     * the strongest tint the seven rule colors can be read on, which is why
     * the ayah-level recitation wash is gone (owner decision, this session).
     */
    val highlight: Color,
    /** The chosen thing's ground: warm, opaque, and never the accent. */
    val selected: Color,
    /**
     * The found thing's ground: the word a search matched, and the text a
     * reader selected in a field. Warm, opaque, one step louder than
     * [selected], and never the accent (owner decision, this session).
     */
    val match: Color,
    val tajweed: TajweedColors,
    val introInk: Color,
    val mastheadInk: Color,
)
