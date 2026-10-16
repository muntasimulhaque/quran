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
 * Arabic is set well above the Latin sizes on purpose: its letter bodies are
 * small next to the space the diacritics need, so at the same nominal size it
 * reads visibly smaller than Latin text. The base is 32 sp rather than 30
 * (owner decision, this session): the KFGQPC face reads small for its em,
 * the ayah is the one text every reader reads most, and the room the larger
 * base takes is paid for in the scroll, which costs a reader nothing.
 *
 * The largest step stops at 1.2. Past that the reading turns into a page of
 * headings. The default step is 1, the middle of the five (owner decision):
 * a reader who opens the text page can go two steps either way, smaller or
 * larger, from where every text already stands. The smallest was 0.65 and is
 * 0.75 (owner decision, the audio and
 * type audit session): at 0.65 a translation stood at 11 sp and a word
 * meaning at 9 sp, under every platform floor for text read at length, while
 * 0.75 keeps the compact choice and puts the translation at 12.75 sp. A
 * reader who once chose 1.4 keeps the largest step, which is now 1.2, so no
 * choice ever loses its meaning.
 */
object TextSize {

    val STEPS = listOf(0.75f, 0.85f, 1f, 1.1f, 1.2f)

    /** The middle step: comfortable for most readers, on most screens. */
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
     * whatever index it was.
     */
    fun step(value: Float): Float = STEPS.minByOrNull { abs(it - value) } ?: DEFAULT

    /** The size of one piece of text, in scale independent pixels. */
    fun sp(role: TypeRole, step: Float): Float = base(role) * step(step)

    /** The line height that lets the largest glyph of that text breathe. */
    fun lineSp(role: TypeRole, step: Float): Float = sp(role, step) * lineRatio(role)

    /**
     * The meaning under a word in the word by word aid. This is the size the
     * meaning is read at; the Arabic word above it is derived from it, so the
     * two can never drift apart. 16 sp (owner decision, the theme and
     * consistency session): the gloss at 15 sat closer to a footnote than to
     * the step under the translation it means to be, and the session lifted
     * it one point while keeping it under the translation's 17.
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
     * step the word stands at 28 sp, the 7/8 of the Quran's 32 that the
     * session pinned as "one size under" (owner decision, the theme and
     * consistency session); the aid reads as a small copy of the page, not
     * as a footnote to itself.
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
        TypeRole.Arabic -> 32f
        TypeRole.Translation -> 17f
        // The tafsir is the longest prose the app draws, read in the same
        // sitting as the translation it opens from, so it stands at the
        // translation's own size and not a point under it (owner decision,
        // this session; the two sizes read as one voice now).
        TypeRole.Tafsir -> 17f
        // The word list is derived from [meaningSp] at [WORD_MEANING_RATIO],
        // the 28-over-16 stand the aid reads at; this entry is that
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
     * The size the meaning under a word is read at, before any scaling. 16 sp
     * (owner decision, the theme and consistency session): the meaning is
     * read at length under every word, and 16 keeps the gloss a step under
     * the translation's 17 while sitting closer to "two sizes under" its own
     * Arabic than the 15 it replaced.
     */
    private const val MEANING_BASE = 16f

    /**
     * The Arabic word's stand over its meaning: 28 over 16, the 7/8 of the
     * Quran the session pinned as "one size under" (owner decision, the
     * theme and consistency session).
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
