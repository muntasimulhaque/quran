package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color
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
 * Two grounds, like every other page color: the paper tones are dark enough
 * for 4.5:1 on paper, the night tones light enough for 4.5:1 on night. Both
 * are measured in the contrast test, on the paper, the sheet, the selected
 * ayah's 8% lapis wash, and the reciter's 12% wash.
 *
 * Identity first (owner decision, the palette session). On a cream ground at
 * 4.5:1 the sRGB gamut tops out at about C 0.12 for amber and 0.09 for teal,
 * so those two can never be as loud as the blue, and a palette that spends
 * its lightness budget evenly pays for a channel the eye reads second. The
 * 10.6 ladder did that: it pinned every family's weight on a ladder from
 * quiet gray to dark crimson, and its closest pair still sat at 0.089. On the
 * page the owner reported one dark band, with two warm tones that had to be
 * compared before either could be named.
 *
 * This palette spends chroma instead. Every family sits at the edge of the
 * gamut for its hue, and its lightness is the place where that hue has the
 * most color to give, L 0.483 to 0.502 on paper and 0.742 to 0.788 on night.
 * The result is a lighter and a more marked page than 10.7: the blue is a
 * true blue at C 0.188, sixty percent more chroma than the 10.7 tone, the
 * red is a red and not a maroon, the violet carries 0.243, and the green
 * 0.153. Mean chroma moves from 0.115 to 0.141 on paper and from 0.100 to
 * 0.144 on night.
 *
 * The gray is the one family that leaves the band (owner decision, this
 * session). The silent letters are the only family with no hue, and the teal
 * carries only 0.084 of chroma, so on paper the two were 0.089 apart, the
 * closest pair in the palette. The gray steps down to L 0.435 on paper and
 * 0.735 on night, which is one full weight away from the teal, and the pair
 * measures 0.101 on paper and 0.143 on night. A quiet letter stays quiet by
 * having no hue, not by being faint.
 *
 * The warm pair is the one place the weight still does the work. Red and
 * amber are one hue to a reader with red-green deficiency, so ghunnah at
 * L 0.502 and qalaqah at L 0.493 are a step apart on the page, and the ochre
 * and the red are told apart by hue on a color screen and by weight on a
 * grayscale one.
 *
 * Only 16.5% of the reading's characters carry a color, and the madda alone
 * is a third of those, so the common families set the page's tint and the
 * rare ones are the events. The madda is a true blue rather than the exact
 * lapis of an action, so a colored letter never reads as a button.
 *
 * The road not taken: the printed mushaf's brightness (amber near #B76903,
 * teal near #058B89, red near #F00B38) needs a 3.2:1 floor, and the smallest
 * reading step is 23 sp, where 4.5:1 is the rule this app set for itself
 * (owner decision, this session).
 *
 * The palette's separation, not only its contrast, is pinned by
 * `TajweedPaletteTest`: every pair stays at a measurable distance so a later
 * "make it quieter" edit fails on the machine rather than on a reader's eyes.
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
    fun of(rule: TajweedRule): Color = when (rule) {
        TajweedRule.HAM_WASL,
        TajweedRule.LAAM_SHAMSIYAH,
        TajweedRule.SLNT,
        -> silent
        TajweedRule.GHUNNAH -> ghunnah
        TajweedRule.QALAQAH -> qalaqah
        TajweedRule.IKHAFA,
        TajweedRule.IKHAFA_SHAFAWI,
        -> ikhafa
        TajweedRule.IQLAB -> iqlab
        TajweedRule.IDGHAM_GHUNNAH,
        TajweedRule.IDGHAM_WO_GHUNNAH,
        TajweedRule.IDGHAM_SHAFAWI,
        TajweedRule.IDGHAM_MUTAJANISAYN,
        TajweedRule.IDGHAM_MUTAQARIBAYN,
        -> idgham
        TajweedRule.MADDA_NORMAL,
        TajweedRule.MADDA_PERMISSIBLE,
        TajweedRule.MADDA_OBLIGATORY_MONFASEL,
        TajweedRule.MADDA_OBLIGATORY_MOTTASEL,
        TajweedRule.MADDA_NECESSARY,
        -> madda
    }
}

val PaperTajweed = TajweedColors(
    silent = Color(0xFF535250),
    ghunnah = Color(0xFF925100),
    qalaqah = Color(0xFFB80027),
    ikhafa = Color(0xFF9400BC),
    iqlab = Color(0xFF00706F),
    idgham = Color(0xFF00731A),
    madda = Color(0xFF005CCB),
)

/**
 * The night ground's seven, the same hues and the same order, on the lighter
 * tones a dark page needs. The night page has more room than paper does: a
 * light tone gains chroma until about L 0.8, where a cream page at 4.5:1
 * stops at 0.5, so the night palette is the louder of the two by
 * construction. The blue is the one family that cannot follow the others to
 * the light end, since blue loses chroma as it brightens; it sits at L 0.746,
 * a step under the rest, and keeps 0.131.
 */
val NightTajweed = TajweedColors(
    silent = Color(0xFFABA8A1),
    ghunnah = Color(0xFFFFA142),
    qalaqah = Color(0xFFFF7F7E),
    ikhafa = Color(0xFFDE87FF),
    iqlab = Color(0xFF01D0CD),
    idgham = Color(0xFF00D73C),
    madda = Color(0xFF77AEFF),
)
