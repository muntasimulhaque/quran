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
 * The four scales every sized text is drawn at, as a multiplier over the base
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
 * headings. The smallest was 0.65 and is 0.75 (owner decision, the audio and
 * type audit session): at 0.65 a translation stood at 11 sp and a word
 * meaning at 9 sp, under every platform floor for text read at length, while
 * 0.75 keeps the compact choice and puts the translation at 12.75 sp. A
 * reader who once chose 1.4 keeps the largest step, which is now 1.2, so no
 * choice ever loses its meaning.
 */
object TextSize {

    val STEPS = listOf(0.75f, 0.85f, 1f, 1.2f)

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
    fun lineSp(role: TypeRole, step: Float): Float = sp(role, step) * ratio(role)

    /**
     * The meaning under a word in the word by word aid. This is the size the
     * meaning is read at; the Arabic word above it is derived from it, so the
     * two can never drift apart.
     */
    fun meaningSp(step: Float): Float = MEANING_BASE * step(step)

    /**
     * The Arabic word in the word by word aid, standing to its meaning as the
     * ayah stands to its translation (32 to 17). A word list that kept the
     * old flat 14 sp drew its Arabic smaller than its Latin meaning, which
     * read as a footnote to itself; the proportion is the one the reading
     * already uses, so the aid looks like a small copy of the page.
     */
    fun wordSp(step: Float): Float = meaningSp(step) * base(TypeRole.Arabic) / base(TypeRole.Translation)

    /**
     * The Arabic inside a Latin tafsir. Arabic set at the Latin size reads
     * visibly smaller, so a quoted passage disappears into the prose around
     * it; the quotation is the Quran or the Prophet's words and must read as
     * the reading, not as a footnote. The stand is the one the study
     * translation already gives its inline Arabic (0.8 of the 32 sp Arabic
     * against the 17 sp translation), so the app has one answer to how much
     * larger the Arabic is than the text beside it.
     */
    fun tafsirArabic(step: Float): Float = sp(TypeRole.Tafsir, step) * INLINE_ARABIC_RATIO

    private fun base(role: TypeRole): Float = when (role) {
        TypeRole.Arabic -> 32f
        TypeRole.Translation -> 17f
        // The tafsir is the longest prose the app draws, read in the same
        // sitting as the translation it opens from, so it stands at the
        // translation's own size and not a point under it (owner decision,
        // this session; the two sizes read as one voice now).
        TypeRole.Tafsir -> 17f
        // The word list is derived from [meaningSp] at the ayah's own ratio;
        // this entry is that derivation, written as the number it comes to.
        TypeRole.Words -> MEANING_BASE * 32f / 17f
    }

    /** Arabic carries diacritics above and below every line, so it needs more. */
    private fun ratio(role: TypeRole): Float = when (role) {
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
     * The size the meaning under a word is read at, before any scaling. 15 sp
     * rather than 14 (owner decision, the audio and type audit session): the
     * meaning is read at length under every word, and at the smallest step
     * the old 14 sp put it at 10.5 while the translation stood at 12.75; 15
     * keeps the gloss a step under the translation rather than a footnote to
     * it.
     */
    private const val MEANING_BASE = 15f

    /**
     * Arabic beside Latin, at the stand the reading already uses: the study
     * translation draws its inline Arabic at 0.8 of the 32 sp Arabic against
     * the 17 sp translation, which is 1.5 to 1. The tafsir's quotations are
     * drawn in the same Hafs face since the audit, so the stand is the same
     * 1.5 rather than the 1.4 that stood when the quotations wore Amiri,
     * whose letters are a size larger (owner decision, the audio and type
     * audit session).
     */
    private const val INLINE_ARABIC_RATIO = 1.5f
}
