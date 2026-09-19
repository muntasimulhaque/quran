package io.github.muntasimulhaque.quran.data

/**
 * One piece of text the reader can size for themselves. A reader who needs
 * bigger Arabic does not necessarily want bigger footnotes, and a reader who
 * wants a bigger translation does not want the tafsir to move with it, so
 * each is its own choice.
 */
enum class TypeRole { Arabic, Translation, Tafsir, Words }

/**
 * The five steps every sized text is drawn at, as a multiplier over the base
 * the text was designed for.
 *
 * Arabic is set well above the Latin sizes on purpose: its letter bodies are
 * small next to the space the diacritics need, so at the same nominal size it
 * reads visibly smaller than Latin text.
 */
object TextSize {

    val STEPS = listOf(0.85f, 1f, 1.2f, 1.4f, 1.6f)

    /** The middle step: comfortable for most readers, on most screens. */
    const val DEFAULT = 1

    fun step(index: Int): Int = index.coerceIn(STEPS.indices)

    /** The size of one piece of text, in scale independent pixels. */
    fun sp(role: TypeRole, index: Int): Float = base(role) * STEPS[step(index)]

    /** The line height that lets the largest glyph of that text breathe. */
    fun lineSp(role: TypeRole, index: Int): Float = sp(role, index) * ratio(role)

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
