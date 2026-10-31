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
 * the rules page names every rule inside its group. The hues are the print's,
 * so a reader who learned a color coded mushaf is not relearning the page.
 *
 * The palette has two rules, and both are measured. Every tone clears 4.5:1
 * on the page, the sheet, the selected ayah's wash, and the reciter's wash.
 * Every pair is also far enough apart in OKLab under normal vision (the 0.095
 * floor in [TajweedPaletteTest]) and under protanopia, deuteranopia, and
 * tritanopia (the 0.02 floor in [TajweedColorVisionTest]).
 *
 * The earlier palette passed contrast and normal separation and still failed
 * color vision: on paper, ghunnah and qalaqah measured 0.003 under simulated
 * deuteranopia, and iqlab and idgham measured 0.012 under tritanopia; on night
 * the same pairs measured 0.002 and 0.000. They were effectively one color to
 * the readers who have those deficiencies. The owner does not want a second,
 * non-color channel, so the colors themselves had to carry the difference.
 *
 * The new palette spaces lightness as well as hue. The warm pair and the
 * teal and green pair are the two that needed it: the amber and the red are
 * separated by weight as well as hue, and the teal sits well below the green
 * on paper and well above it on night. The rest keep their hue identity, and
 * the paper palette stays a print's palette rather than becoming a ladder of
 * pastels. The night palette is lighter by construction, because a dark page
 * has more gamut for a light tone; it is still the same seven families.
 *
 * No seven-hue palette is perfect for every form of color vision deficiency.
 * This one clears the simulated floor, keeps the normal-vision separation,
 * and holds the contrast rule, and both tests keep it there. A later quiet
 * edit that collapses a pair fails on the machine rather than on a reader's
 * eyes (owner decision, this session).
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
    silent = Color(0xFF4A463F),
    ghunnah = Color(0xFF9B4E07),
    qalaqah = Color(0xFF8E081D),
    ikhafa = Color(0xFF7A22AF),
    iqlab = Color(0xFF1A626D),
    idgham = Color(0xFF1C7335),
    madda = Color(0xFF003B8F),
)

/**
 * The night ground's seven, the same hues and the same order, on the lighter
 * tones a dark page needs. The gray is warm, the red is a soft coral, the
 * teal is a mint, and the green is the one family that stays a saturated
 * green; the lightness steps are what keep the pairs apart when hue alone
 * stops carrying the difference (owner decision, this session).
 */
val NightTajweed = TajweedColors(
    silent = Color(0xFFA19B92),
    ghunnah = Color(0xFFFFB86B),
    qalaqah = Color(0xFFFF9A9A),
    ikhafa = Color(0xFFC87FFC),
    iqlab = Color(0xFF6ED0C8),
    idgham = Color(0xFF00C93A),
    madda = Color(0xFF6AA0F5),
)
