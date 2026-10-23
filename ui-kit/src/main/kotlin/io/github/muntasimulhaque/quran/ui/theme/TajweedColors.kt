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
 * the rules page names every rule inside its group.
 *
 * Two grounds, like every other page color: the paper tones are dark enough
 * for 4.5:1 on paper, the night tones light enough for 4.5:1 on night. Both
 * are measured in the contrast test, on the paper, the sheet, the selected
 * ayah's 8% lapis wash, and the reciter's 12% wash.
 *
 * The 4.5:1 rule is a floor, not a target (owner decision, the palette
 * session). The first palette pushed every family to within 0.3 of the floor
 * on the reciter's wash, which flattened all seven into one lightness band:
 * the page had no value channel, and four of the families sat within 0.1 of
 * each other. This palette spends the band instead. Each family takes a
 * deliberate step, from the quiet gray (lightest on paper, darkest on night)
 * through the warm and green tones to the dark crimson and violet, so a rule
 * is separated by value as well as by hue. The rarest and most important
 * rules, qalqalah, ghunnah, and iqlab, are the ones a reader looks for; the
 * common madda and the silent letters stay calm.
 *
 * Only 16.5% of the reading's characters carry a color, and the madda alone
 * is a third of those, so the common families set the page's tint and the
 * rare ones are the events. The madda is a deep indigo rather than the exact
 * lapis of an action, so a colored letter never reads as a button.
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
    silent = Color(0xFF65625E),
    ghunnah = Color(0xFF834D13),
    qalaqah = Color(0xFF93001A),
    ikhafa = Color(0xFF6C008A),
    iqlab = Color(0xFF026668),
    idgham = Color(0xFF006821),
    madda = Color(0xFF224F90),
)

/**
 * The night ground's seven, the same hues and the same order, on the lighter
 * tones a dark page needs. The teal sits at the light end and the green a
 * step under it, because that pair is the one the 4.5:1 gamut squeezes
 * hardest; the crimson and the warm gold are separated by hue and by a
 * value step so neither reads as the other.
 */
val NightTajweed = TajweedColors(
    silent = Color(0xFFA8A5A0),
    ghunnah = Color(0xFFEEB669),
    qalaqah = Color(0xFFFE9A9D),
    ikhafa = Color(0xFFDF9FF3),
    iqlab = Color(0xFF73DFDF),
    idgham = Color(0xFF95D587),
    madda = Color(0xFF94BEFC),
)
