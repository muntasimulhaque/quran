package io.github.muntasimulhaque.quran.data

import kotlin.math.abs

/**
 * One piece of text the reader can size for themselves. A reader who needs
 * bigger Arabic does not necessarily want bigger footnotes, and a reader who
 * wants a bigger translation does not want the tafsir to move with it, so
 * each is its own choice.
 */
enum class TypeRole { Arabic, Translation, Tafsir, Words }

/**
 * The five scales every sized text is drawn at, as a multiplier over the base
 * the text was designed for.
 *
 * Arabic is set well above the Latin sizes on purpose: its letter bodies are
 * small next to the space the diacritics need, so at the same nominal size it
 * reads visibly smaller than Latin text.
 *
 * The largest step stops at 1.4. Past that the study reading turns into a
 * page of headings, and the Mushaf page, which does not scale, is left
 * behind. The smallest, 0.75, is there for a reader who wants more of an
 * ayah on one screen than 0.85 gives.
 */
object TextSize {

    val STEPS = listOf(0.75f, 0.85f, 1f, 1.2f, 1.4f)

    /** The middle step: comfortable for most readers, on most screens. */
    const val DEFAULT = 1f

    /**
     * The scales as they were before the smallest arrived and the largest
     * was let go, by the index that was stored for them then.
     */
    val LEGACY_STEPS = listOf(0.85f, 1f, 1.2f, 1.4f, 1.6f)

    /**
     * The step nearest a stored value. A choice made under an older list of
     * scales keeps its meaning: 1.6 becomes 1.4, not whatever index it was.
     */
    fun step(value: Float): Float = STEPS.minByOrNull { abs(it - value) } ?: DEFAULT

    /** The size of one piece of text, in scale independent pixels. */
    fun sp(role: TypeRole, step: Float): Float = base(role) * step(step)

    /** The line height that lets the largest glyph of that text breathe. */
    fun lineSp(role: TypeRole, step: Float): Float = sp(role, step) * ratio(role)

    private fun base(role: TypeRole): Float = when (role) {
        TypeRole.Arabic -> 30f
        TypeRole.Translation -> 17f
        TypeRole.Tafsir -> 16f
        TypeRole.Words -> 14f
    }

    /** Arabic carries diacritics above and below every line, so it needs more. */
    private fun ratio(role: TypeRole): Float = when (role) {
        TypeRole.Arabic -> 2f
        else -> 1.6f
    }
}
