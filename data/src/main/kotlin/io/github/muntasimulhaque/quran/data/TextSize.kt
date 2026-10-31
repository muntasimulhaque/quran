package io.github.muntasimulhaque.quran.data

import kotlin.math.abs

/**
 * One piece of text the reader can size for themselves. A reader who needs
 * bigger Arabic does not necessarily want bigger footnotes, and a reader who
 * wants a bigger translation does not want the tafsir to move with it, so
 * each is its own choice.
 *
 */
enum class TypeRole { Arabic, Translation, Tafsir, Words }

/**
 * The five scales every sized text is drawn at, as a multiplier over the base
 * the text was designed for.
 *
 * The steps are evenly spaced, 0.8 through 1.2 with 0.1 between them, and the
 * default is 1, the middle step (owner decision, this session). The ladder
 * before this was 0.75, 0.85, 1, 1.1, 1.2, whose gap under the default was
 * 0.15 while every other gap was 0.1: the one step down from the shipped
 * default read heavier than the rest, and an even ladder also makes "one step
 * smaller" the same number as the step itself, which is why the base below is
 * 0.9 of what it was and not 0.85.
 *
 * The prose sits at 16 sp on a 1.5 line. It stood at 17, then 16, then 15;
 * the owner asked for the translation and the tafsir to come down one more
 * step, and the next audit brought them back to 16, because 15 read as a
 * subhead and the page had lost its body. 16 is Material's body size and
 * the face's own reading size; the reading face and the interface face keep
 * the Book apart from the chrome even at the same size. The five steps are
 * unchanged and the default is still the middle of them (owner decision,
 * this session).
 *
 * Arabic is set above the Latin sizes on purpose: its letter bodies are
 * small next to the space the diacritics need, so at the same nominal size it
 * reads visibly smaller than Latin text. The base is 26 sp, 1.625 times the
 * 16 sp translation: the owner's eye asked for the Quran one step larger
 * while the prose stayed at the size the page needs, and this is the ratio
 * that did it. It is more contrast than the field (Quran for Android 1.4,
 * Quran.com 1.28), and less than the app's own 1.8 before the field audit;
 * the reader can still go down to 20.8 at the smallest step and up to 31.2
 * at the largest (owner decision, this session).
 *
 * The largest step stops at 1.2. Past that the reading turns into a page of
 * headings. The smallest is 0.8, the compact choice, at 12.8 sp of translation
 * and 11.5 sp of meaning (owner decision, this session). The earlier session
 * pinned 0.75 against the larger base as the readability floor; the two sit
 * within a hair of each other at the default, and the floor stands.
 */
object TextSize {

    val STEPS = listOf(0.8f, 0.9f, 1f, 1.1f, 1.2f)

    /** The middle step: the shipped default, one step under where it was. */
    const val DEFAULT = 1f

    /**
     * The line a Latin paragraph of reading takes per em of text: 1.5, the
     * leading a book sets a serif at. Its Bangla counterpart is
     * [BENGALI_PROSE_RATIO], because the two scripts do not share a line box;
     * 1.5 would sit under Bengali's own 1.594 ink and is never asked of it.
     */
    const val LATIN_PROSE_RATIO = 1.5f

    /**
     * The line a Bangla paragraph of reading takes per em of text: 31 over
     * 18. Noto Serif Bengali's own box is 1.594 of the em and the script
     * stacks vowel marks above and below the line, so a 1.6 line borrowed from
     * Literata would be a hair under the face's own ink (owner decision, this
     * session: the Bangla reading takes its own line at last).
     */
    const val BENGALI_PROSE_RATIO = 31f / 18f

    /**
     * The scales the scales have been, by the index that was stored for them
     * when a step was stored as an index. The first list is the five the
     * setting was born with; a reader who chose the top of it lands on the
     * top of today's list, and so on down.
     */
    val LEGACY_STEPS = listOf(0.85f, 1f, 1.2f, 1.4f, 1.6f)

    /**
     * The step nearest a stored value. A choice made under an older list of
     * scales keeps its meaning: 1.6 becomes 1.2, the largest step now, not
     * whatever index it was. A tie goes to the larger step, so the retired
     * 0.85 lands on 0.9, the place it held under the new ladder, and never on
     * 0.8 (owner decision, this session).
     */
    fun step(value: Float): Float {
        var nearest = DEFAULT
        var distance = Float.MAX_VALUE
        for (candidate in STEPS) {
            val gap = abs(candidate - value)
            // `<=` so the larger step wins a tie: the retired 0.85 sits
            // exactly between 0.8 and 0.9 and belongs on the step it held.
            if (gap <= distance) {
                nearest = candidate
                distance = gap
            }
        }
        return nearest
    }

    /** The number of the step that carries [step], one based, for the reader's row. */
    fun stepNumber(step: Float): Int = STEPS.indexOf(step(step)).coerceAtLeast(0) + 1

    /** The number of steps every sized text offers. */
    val stepCount: Int get() = STEPS.size

    /** The size of one piece of text, in scale independent pixels. */
    fun sp(role: TypeRole, step: Float): Float = base(role) * step(step)

    /** The line height that lets the largest glyph of that text breathe. */
    fun lineSp(role: TypeRole, step: Float): Float = sp(role, step) * lineRatio(role)

    /**
     * The meaning under a word in the word by word aid: the size the meaning
     * is read at, 14.4 sp at the middle step. It is nine tenths of the
     * translation's 16, a step the eye reads as a gloss rather than as a
     * second translation. It stood five percent under the translation, which
     * read as one size used twice; a tenth under is the distance that says
     * which line is the verse's meaning and which is the reading's own (owner
     * decision, this session). At the smallest step the gloss is 11.5 sp.
     */
    fun meaningSp(step: Float): Float = MEANING_BASE * step(step)

    /**
     * The line the meaning under a word gets: the prose ratio, the same 1.5
     * every Latin paragraph on the page wears.
     */
    fun meaningLineSp(step: Float): Float = meaningSp(step) * lineRatio(TypeRole.Translation)

    /**
     * The Arabic word in the word by word aid: 22 sp at the middle step, four
     * points under the Quran's own 26, so the aid reads as a small copy of the
     * page and not as a footnote to itself.
     *
     * It is a copy of the verse's own word, so it follows the Quran's size and
     * never the gloss's. It used to be derived from the meaning by a ratio,
     * which was one rule while both took the Latin base; the two scripts now
     * share one size ladder, so the word stays here on both languages (owner
     * decision, this session).
     */
    fun wordSp(step: Float): Float = WORD_BASE * step(step)

    /**
     * The one rule for Arabic beside prose, in the translation and in the
     * tafsir both: 0.9 of the reader's own Quran size. It is the stand at
     * which Amiri measures the same as the Hafs face (its alef is 0.696 em
     * against Hafs's 0.636), so an inline quotation reads as the Quran's own
     * text. One rule replaces the two that had drifted, 0.8 of the Arabic in
     * the translation and 1.5 of the tafsir prose in the tafsir, which
     * landed on the same number at the default and hid the fact that they
     * were two (owner decision, the theme and consistency session).
     */
    fun inlineArabic(step: Float): Float = sp(TypeRole.Arabic, step) * INLINE_ARABIC_RATIO

    private fun base(role: TypeRole): Float = when (role) {
        TypeRole.Arabic -> ARABIC_BASE
        TypeRole.Translation -> 16f
        // The tafsir is the longest prose the app draws, read in the same
        // sitting as the translation it opens from, so it stands at the
        // translation's own size and not a point under it (owner decision,
        // this session; the two sizes read as one voice now).
        TypeRole.Tafsir -> 16f
        // The aid's Arabic word, the stand the Quran's own text takes four
        // points down; see [wordSp].
        TypeRole.Words -> WORD_BASE
    }

    /**
     * Arabic carries diacritics above and below every line, so it needs more.
     * The ratio is public because the search results draw the Quran's own
     * line outside the reading's sizes and must breathe at the reading's own
     * ratio, one rule on both surfaces rather than two copies that can drift
     * (owner decision, this session).
     */
    fun lineRatio(role: TypeRole): Float = when (role) {
        // 2.0, the Quran Foundation's own leading for the QPC Hafs face in its
        // font guide. At 31.2 the app carried 1.9, measured against the marks;
        // with the base at 26 the official 2.0 gives the marks their room and
        // matches the field the app was compared with (owner decision, this
        // session).
        TypeRole.Arabic -> 2f
        TypeRole.Words -> 1.8f
        else -> LATIN_PROSE_RATIO
    }

    /**
     * The size the meaning under a word is read at, before any scaling: nine
     * tenths of the translation's base. See [meaningSp].
     */
    private const val MEANING_BASE = 14.4f

    /** The Quran's own base, the anchor every other reading size is read against. */
    private const val ARABIC_BASE = 26f

    /**
     * The Arabic word in the aid, before any scaling: four points under the
     * Quran's [ARABIC_BASE]. See [wordSp].
     */
    private const val WORD_BASE = ARABIC_BASE - 4f

    /**
     * The one stand for Arabic beside prose, in the translation and the
     * tafsir alike: 0.9 of the Quran's own size. It is the size at which
     * Amiri's alef (0.696 em) measures the same as Hafs's (0.636 em), so an
     * inline quotation reads as the Quran's own text (owner decision, the
     * theme and consistency session).
     */
    private const val INLINE_ARABIC_RATIO = 0.9f
}
