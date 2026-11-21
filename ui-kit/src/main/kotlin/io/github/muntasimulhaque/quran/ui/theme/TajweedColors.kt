package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.muntasimulhaque.quran.core.TajweedFamily
import io.github.muntasimulhaque.quran.core.TajweedRule

/**
 * The seven color families the eighteen tajweed rules are drawn in.
 *
 * The printed tajweed mushaf groups the rules rather than giving each rule a
 * hue of its own: the silent letters are quiet, the ghunnah is warm, the
 * qalqalah is red, the ikhafa is purple, the iqlab is teal, the idgham is
 * green, and the madda is blue. A group is the unit a reader's eye can hold;
 * the rules page names every rule inside its group. The grouping is the
 * print's, and the anchors follow the subcontinent's thirteen-line
 * convention, the one this app's own readers learned: the silent letters
 * gray, the qalaqah red, the idgham green. No mapping is universal across
 * publishers, and the two deliberate divergences are held by the three
 * tests below: the ikhfa wears purple rather than the subcontinent's blue
 * because the madda owns the blue and the iqlab owns the teal, and a
 * third blue would collapse two families under deuteranopia, which the
 * 0.02 floor forbids; the ghunnah is warm because the print's green is the
 * idgham's. What a reader who learned a color coded mushaf carries over is
 * the grouping and its anchors, and the page keeps them (owner decision,
 * 20.1).
 *
 * The ghunnah is amber since 21.2, not the dark brown it was: the print's
 * warmth is kept (amber is warm, and the idgham still owns green), but the
 * tone was #874306 at hue 54 and read as a page's fifth brown. The family
 * moves as far as the palette's own floors allow, and this note records
 * where the headroom went: the deuteranopia distance from the qalaqah comes
 * down from 0.030 to 0.027 against the 0.02 floor, and the closest normal
 * pair becomes the silent gray and this family at 0.096 against the 0.095
 * floor. Both floors still hold, on the machine (owner decision, 21.2).
 *
 * The palette has three rules, and all three are measured. Every tone clears
 * 4.5:1 on the page, the sheet, the floating control, the selected ayah's
 * wash, and the reciter's word mark. Every pair is far enough apart in OKLab
 * under normal vision (the 0.095 floor in [TajweedPaletteTest]) and under
 * protanopia, deuteranopia, and tritanopia (the 0.02 floor in
 * [TajweedColorVisionTest]). And the seven sit inside one narrow band of
 * contrast on the page, so a page of the Quran reads as one text in seven inks
 * rather than as a text with some words lit up (owner decision, 17.1).
 *
 * The earlier palette passed contrast, normal separation, and color vision
 * and still read unevenly: paper ran from 5.6:1 (ghunnah and idgham) to 9.9:1
 * (madda), a spread of 1.76, so the two lightest families looked like the only
 * colored words on the page while the two darkest read as emphasis on the ink.
 * Over an hour the page flickered in weight. The families now sit inside 6.2 to
 * 7.6 on paper and 6.7 to 8.1 on night, a spread of 1.23 and 1.21: the madda
 * and the qalaqah came down off the ink's own darkness, the quiet pair came
 * up, and the verse holds one rhythm from its first word to its medallion.
 *
 * The band cannot collapse to a single tone, and that is the color vision rule
 * speaking. Under deuteranopia the warm pair is one hue, and under tritanopia
 * the teal and the green nearly are, so those two pairs keep a lightness step:
 * the paper palette's smallest false-color distance is 0.027 (ghunnah and
 * qalaqah under deuteranopia) and the night's is 0.030 (ghunnah and idgham
 * under protanopia), against a 0.02 floor. Normal vision holds 0.096 on paper
 * (the silent gray and the ghunnah) and 0.106 on night (the warm pair). Every
 * family also sits at least 0.13 from the reading's ink, so no rule can read
 * as plain text.
 *
 * The night ground's seven are the same hues on the tones a dark page needs,
 * and they are no longer the brightest thing on it: the top came down from
 * 10.2 to 8.1, because a pale saturated letter on a charcoal page blooms in a
 * long evening read and pulls the eye off the verse. The green is a leaf green
 * now, 0.159 of chroma where it was 0.227: it was the one fully saturated tone
 * in the app, and on an OLED panel it read as a signal rather than as print.
 * The night palette is lighter by construction, because a dark page has more
 * gamut for a light tone; it is still the same seven families.
 *
 * No seven-hue palette is perfect for every form of color vision deficiency.
 * This one clears the simulated floor with half its margin to spare, keeps the
 * normal-vision separation, holds the contrast rule on every ground it is
 * drawn on, and holds the weight band, and all three tests keep it there. A
 * later quiet edit that collapses a pair, sinks a tone, or re-scatters the
 * weights fails on the machine rather than on a reader's eyes (owner decision,
 * 17.1).
 */
data class TajweedColors(
    val silent: Color,
    val ghunnah: Color,
    val qalaqah: Color,
    val ikhafa: Color,
    val iqlab: Color,
    val idgham: Color,
    val madda: Color,
) {
    /** The family a rule is drawn in. */
    fun of(family: TajweedFamily): Color = when (family) {
        TajweedFamily.SILENT -> silent
        TajweedFamily.GHUNNAH -> ghunnah
        TajweedFamily.QALAQAH -> qalaqah
        TajweedFamily.IKHAFA -> ikhafa
        TajweedFamily.IQLAB -> iqlab
        TajweedFamily.IDGHAM -> idgham
        TajweedFamily.MADDA -> madda
    }

    fun of(rule: TajweedRule): Color = of(TajweedFamily.of(rule))
}

val PaperTajweed = TajweedColors(
    silent = Color(0xFF635D55),
    ghunnah = Color(0xFF844800),
    qalaqah = Color(0xFFAC1C2B),
    ikhafa = Color(0xFF7F29B5),
    iqlab = Color(0xFF035967),
    idgham = Color(0xFF295B26),
    madda = Color(0xFF0C52B6),
)

/**
 * The night ground's seven, the same hues and the same order, on the lighter
 * tones a dark page needs. The gray is warm, the red is a soft coral, the teal
 * is a mint, and the green is a leaf; the lightness steps are what keep the
 * pairs apart when hue alone stops carrying the difference (owner decision,
 * 17.1).
 */
val NightTajweed = TajweedColors(
    silent = Color(0xFFAEA9A0),
    ghunnah = Color(0xFFE29A46),
    qalaqah = Color(0xFFF78A8B),
    ikhafa = Color(0xFFCF8EFF),
    iqlab = Color(0xFF29C5BD),
    idgham = Color(0xFF51B55A),
    madda = Color(0xFF72A9FE),
)
