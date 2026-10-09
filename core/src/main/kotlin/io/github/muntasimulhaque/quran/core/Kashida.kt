package io.github.muntasimulhaque.quran.core

/**
 * The tatweel: where a word of the Book can carry one, and how one is put
 * into it.
 *
 * The printed page justifies a line by stretching its letters, not by opening
 * its word gaps: a calligrapher lengthens the connecting stroke of a word, and
 * the space between two words stays the space it was written with. A page set
 * from a typeface cannot stretch a letterform without distorting it, so the
 * app writes the stretch instead, which is the same mark the calligrapher
 * draws: the tatweel, U+0640, a stroke that connects to the letter before it
 * and the letter after it.
 *
 * The word is never edited. The tatweel is a rendering device: the Book's own
 * text stands in the content database untouched, and the tatweel exists only
 * in the string the canvas is asked to draw, for the line the print fills.
 */
object Kashida {

    /** The tatweel itself, the elongation stroke. */
    const val TATWEEL = '\u0640'

    /**
     * The most tatweels one word may carry. A word that cannot reach its
     * share of a line's slack leaves it to the word gaps, which is a page set
     * with a little air rather than a page set with a stroke through it.
     */
    const val MAX_PER_WORD = 8

    /**
     * The letters a following letter will not join to. A tatweel put after one
     * of these is a stroke hanging off a dead end, so the engine looks
     * elsewhere in the word; it is a guard on the shape, not on the width,
     * because a hanging tatweel is the one thing that reads as a mistake.
     */
    private val NOT_LEFT_JOINING = setOf(
        0x0621, // hamza
        0x0622, // alef with madda above
        0x0623, // alef with hamza above
        0x0625, // alef with hamza below
        0x0627, // alef
        0x062F, // dal
        0x0630, // thal
        0x0631, // reh
        0x0632, // zain
        0x0648, // waw
        0x0649, // alef maksura
        0x0674, // high hamza
        0x0675, // high hamza waw
        0x0676, // u with hamza above
        0x06CD, // yeh with tail
        0x06D0, // yeh barree with hamza above
        0x06D2, // yeh barree
        0x06D3, // yeh with hamza below
    )

    /**
     * The character indices of [word] at which a tatweel may be put: the
     * boundaries between two of its letters, where the letter before the
     * boundary joins to the left. A mark never carries a boundary, because a
     * tatweel between a letter and its mark belongs to neither.
     */
    fun positions(word: String): List<Int> {
        val letters = ArrayList<Int>(word.length)
        var index = 0
        while (index < word.length) {
            val codePoint = word.codePointAt(index)
            if (!Arabic.isMark(codePoint)) letters.add(index)
            index += Character.charCount(codePoint)
        }
        if (letters.size < 2) return emptyList()
        val out = ArrayList<Int>(letters.size - 1)
        for (i in 0 until letters.size - 1) {
            if (word.codePointAt(letters[i]) in NOT_LEFT_JOINING) continue
            out += letters[i + 1]
        }
        return out
    }

    /**
     * [word] with [count] tatweels put at [position]. A word put at once is
     * shaped once, so the face sees the whole word and the strokes it draws
     * are the strokes a stretched word would carry.
     */
    fun insert(word: String, position: Int, count: Int = 1): String =
        word.substring(0, position) + TATWEEL.toString().repeat(count) + word.substring(position)
}
