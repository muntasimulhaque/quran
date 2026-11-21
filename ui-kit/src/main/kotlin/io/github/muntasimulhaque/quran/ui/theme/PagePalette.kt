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
 * [PaperReadingInk] at 11.9:1 and the night [NightInk] at 12.1:1, each one a
 * step under the word its buttons and labels wear (owner decision, 17.1).
 *
 * [highlight] is the word mark on the reading's line: the word the reciter
 * is saying and the word a search arrived on. Each theme sets its own,
 * because a lapis wash that reads as ink on paper reads as nothing at all on
 * a night ground. Its alpha is a contrast ceiling, not a taste: the seven
 * rule colors are drawn over it, and one drop more puts the closest of them
 * under 4.5:1.
 *
 * [selected] is the chosen thing's ground, and it is opaque: the
 * ayah whose menu is open, the introduction the reader opened, the row Browse
 * is standing on, and the word the word sheet is naming. It is the accent's
 * own family at the lightness the warm ground of 19.1 carried, so the one
 * surface in the app that says "this one" says it in the accent's voice
 * (owner decision, 21.0). See [PaperSelected] for the measurement.
 *
 * [match] is its sibling for the thing that was *found* rather than chosen:
 * the word a search matched, and the text a reader selected in a field. Same
 * family, same opaque treatment, one step louder. See [PaperMatch].
 *
 * [introInk] is the softer tone the surah's opened introduction wears: the
 * reading's ink at 0.85 on both grounds, a page that steps back under the
 * wash it stands on. The base is the reading ink on the day ground as well,
 * which it was not before 20.8: the day took 0.78 of the interface's word
 * while the night took 0.78 of the reading's, so one alpha landed two tones
 * and the night's introduction read weaker than the captions beside it
 * (Lc -56.7 against -65.2). One base and one alpha puts the prose at 83.9 on
 * the day page and -64.3 on the night, above every caption on its ground and
 * still a clear step under the verse, 11 Lc on the day and 17 on the night
 * (owner decision, 20.8).
 * [mastheadInk] is the first paint's display tone, the interface's word at
 * 0.9, because a 48 sp line does not need the reading's full ink. Both live
 * here so the theme owns them and the contrast test can measure them, rather
 * than each call site mixing its own alpha (owner decision, 14.0).
 */
data class PagePalette(
    val ink: Color,
    val ornament: Color,
    /**
     * The one word mark on the reading's own line: the word being heard and
     * the word a search arrived on. It is a wash and not a recolored word, so
     * the letters keep their own ink and their tajweed colors. Its alpha is
     * the strongest tint the seven rule colors can be read on, which is why
     * the ayah-level recitation wash is gone (owner decision, 15.9).
     */
    val highlight: Color,
    /** The chosen thing's ground: opaque, in the accent's own family. */
    val selected: Color,
    /**
     * The found thing's ground: the word a search matched, and the text a
     * reader selected in a field. Opaque, in the accent's own family, one
     * step louder than [selected] (owner decision, 21.0).
     */
    val match: Color,
    val tajweed: TajweedColors,
    val introInk: Color,
    val mastheadInk: Color,
)
