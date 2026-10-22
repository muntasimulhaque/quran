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
 * are measured in the contrast test.
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
    // Darkened from #6F6B64 after the reading's selected and reciter washes
    // dropped it to 4.14:1 on paper. It now holds 4.74:1 on the reciter's
    // wash and 5.06:1 on the selected one (owner report, the QPC rebuild
    // audit).
    silent = Color(0xFF66625B),
    ghunnah = Color(0xFF7A5A12),
    qalaqah = Color(0xFFA32020),
    ikhafa = Color(0xFF6B2A86),
    iqlab = Color(0xFF0F6B74),
    idgham = Color(0xFF2F6B32),
    madda = Color(0xFF1B4D7A),
)

val NightTajweed = TajweedColors(
    silent = Color(0xFFAAA59C),
    ghunnah = Color(0xFFDCB870),
    qalaqah = Color(0xFFFFA096),
    ikhafa = Color(0xFFD9A6EA),
    iqlab = Color(0xFF8AD8E0),
    idgham = Color(0xFF9CD8A0),
    madda = Color(0xFF95C0EA),
)
