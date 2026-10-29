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
 * The Arabic keeps the 0.9 base the session that read large settled on, and
 * the prose is the one exception: the translation and the tafsir stand at
 * 16.2 sp, a hair under the platform's own body size, because a whole page of
 * 15.3 tired the eye in a long sitting and a reader should not have to find
 * the size page to read a surah comfortably. The five steps are unchanged,
 * the default is still the middle of them, and the reader can go two steps
 * either way from where every text now stands (owner decision, this session).
 *
 * Arabic is set well above the Latin sizes on purpose: its letter bodies are
 * small next to the space the diacritics need, so at the same nominal size it
 * reads visibly smaller than Latin text. The base is 28.8 sp rather than 32
 * (owner decision, this session): the KFGQPC face reads small for its em, the
 * ayah is the one text every reader reads most, and the room the larger base
 * takes is paid for in the scroll, which costs a reader nothing.
 *
 * The largest step stops at 1.2. Past that the reading turns into a page of
 * headings. The smallest is 0.8, the compact choice, at 12.96 sp of
 * translation and 12.24 sp of meaning (owner decision, this session). The
 * earlier session pinned 0.75 against the larger base as the readability
 * floor; the two sit within a hair of each other at the default, and the
 * floor stands.
 */
object TextSize {

    val STEPS = listOf(0.8f, 0.9f, 1f, 1.1f, 1.2f)

    /** The middle step: the shipped default, one step under where it was. */
    const val DEFAULT = 1f

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
     * The meaning under a word in the word by word aid. This is the size the
     * meaning is read at; the Arabic word above it is derived from it, so the
     * two can never drift apart. 15.3 sp (owner decision, this session): the
     * gloss holds the same stand under the translation's 16.2 that 16 held
     * under 17, one point under the translation it means to be, so it reads
     * as a small gloss and not as a footnote.
     */
    fun meaningSp(step: Float): Float = MEANING_BASE * step(step)

    /**
     * The line the meaning under a word gets: the prose ratio, the same 1.6
     * every Latin paragraph on the page wears.
     */
    fun meaningLineSp(step: Float): Float = meaningSp(step) * lineRatio(TypeRole.Translation)

    /**
     * The Arabic word in the word by word aid, derived from its meaning by
     * [WORD_MEANING_RATIO] so the two can never drift apart. At the middle
     * step the word stands at 26.8 sp, one point under the Quran's own 28.8,
     * so the aid reads as a small copy of the page and not as a footnote to
     * itself.
     */
    fun wordSp(step: Float): Float = meaningSp(step) * WORD_MEANING_RATIO

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
        TypeRole.Arabic -> 28.8f
        TypeRole.Translation -> 16.2f
        // The tafsir is the longest prose the app draws, read in the same
        // sitting as the translation it opens from, so it stands at the
        // translation's own size and not a point under it (owner decision,
        // this session; the two sizes read as one voice now).
        TypeRole.Tafsir -> 16.2f
        // The word list is derived from [meaningSp] at [WORD_MEANING_RATIO],
        // the 26.8-over-15.3 stand the aid reads at; this entry is that
        // derivation, written as the number it comes to.
        TypeRole.Words -> MEANING_BASE * WORD_MEANING_RATIO
    }

    /**
     * Arabic carries diacritics above and below every line, so it needs more.
     * The ratio is public because the search results draw the Quran's own
     * line outside the reading's sizes and must breathe at the reading's own
     * ratio, one rule on both surfaces rather than two copies that can drift
     * (owner decision, this session).
     */
    fun lineRatio(role: TypeRole): Float = when (role) {
        // 1.9, decided by the audio and type audit (owner decision, this
        // session): the Hafs face's own line box is 1.758 em (2400 over 1200,
        // at 2048 upm), and at 1.85 the frame of Al-Baqarah 2:254, the ayah
        // that carries every mark class on consecutive lines, left about
        // 0.7 dp of air at its tightest point (2 pixels on the phone
        // capture). 1.9 lifts that air to about 2.3 dp for about 2.6 percent
        // of the page, which is the one place in the app that sat within a
        // hair of the face's own ink.
        TypeRole.Arabic -> 1.9f
        TypeRole.Words -> 1.8f
        else -> 1.6f
    }

    /**
     * The size the meaning under a word is read at, before any scaling. 15.3
     * sp (owner decision, this session): the meaning is read at length under
     * every word, and it keeps the same stand under the translation's 16.2
     * that 16 held under 17, a small reading rather than a footnote.
     */
    private const val MEANING_BASE = 15.3f

    /**
     * The Arabic word's stand over its meaning: 26.8 over 15.3, the same
     * one-point step over the meaning that the meaning keeps under the
     * translation (owner decision, the theme and consistency session).
     */
    private const val WORD_MEANING_RATIO = 1.75f

    /**
     * The one stand for Arabic beside prose, in the translation and the
     * tafsir alike: 0.9 of the Quran's own size. It is the size at which
     * Amiri's alef (0.696 em) measures the same as Hafs's (0.636 em), so an
     * inline quotation reads as the Quran's own text (owner decision, the
     * theme and consistency session).
     */
    private const val INLINE_ARABIC_RATIO = 0.9f
}
