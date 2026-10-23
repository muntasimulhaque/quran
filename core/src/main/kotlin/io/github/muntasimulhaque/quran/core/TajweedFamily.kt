package io.github.muntasimulhaque.quran.core

/**
 * The seven color families the eighteen rules are drawn in, in the order the
 * legend names them and the reading paints them.
 *
 * The print groups the rules rather than giving each rule a hue of its own,
 * because a group is the unit a reader's eye can hold: the silent letters are
 * one family of three rules, the idgham one family of five. The family is
 * also what the settings legend samples, so the reader meets one word in a
 * color and the rules it covers under it (owner decision, the legend
 * session).
 */
enum class TajweedFamily {
    SILENT,
    GHUNNAH,
    QALAQAH,
    IKHAFA,
    IQLAB,
    IDGHAM,
    MADDA;

    /** The rules drawn in this family's color, in the order the code runs. */
    val rules: List<TajweedRule> get() = MEMBERS.getValue(this)

    companion object {
        /** The family a rule is drawn in. Every rule has exactly one. */
        fun of(rule: TajweedRule): TajweedFamily = when (rule) {
            TajweedRule.HAM_WASL,
            TajweedRule.LAAM_SHAMSIYAH,
            TajweedRule.SLNT,
            -> SILENT
            TajweedRule.GHUNNAH -> GHUNNAH
            TajweedRule.QALAQAH -> QALAQAH
            TajweedRule.IKHAFA,
            TajweedRule.IKHAFA_SHAFAWI,
            -> IKHAFA
            TajweedRule.IQLAB -> IQLAB
            TajweedRule.IDGHAM_GHUNNAH,
            TajweedRule.IDGHAM_WO_GHUNNAH,
            TajweedRule.IDGHAM_SHAFAWI,
            TajweedRule.IDGHAM_MUTAJANISAYN,
            TajweedRule.IDGHAM_MUTAQARIBAYN,
            -> IDGHAM
            TajweedRule.MADDA_NORMAL,
            TajweedRule.MADDA_PERMISSIBLE,
            TajweedRule.MADDA_OBLIGATORY_MONFASEL,
            TajweedRule.MADDA_OBLIGATORY_MOTTASEL,
            TajweedRule.MADDA_NECESSARY,
            -> MADDA
        }

        private val MEMBERS: Map<TajweedFamily, List<TajweedRule>> =
            TajweedRule.entries.groupBy { of(it) }
    }
}
