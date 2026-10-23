package io.github.muntasimulhaque.quran.core

/**
 * One word for each family, cut out of an ayah that carries all seven, so the
 * settings legend can show a color on a real word at the reading's own size
 * instead of on a dot (owner decision, the legend session).
 *
 * The word keeps its own characters and its own code, so the reading's own
 * painter draws the sample exactly the way it draws the page: the rule's
 * letters in the family color and the rest in ink. Nothing is typed here; the
 * caller passes the ayah's text and the code that sits under it.
 *
 * A family takes the word that carries only its own color when the ayah
 * offers one, so the sample teaches one color and not two; then the word with
 * the most of the family's own letters, since a marked harf carries a color
 * on a mark where a letter carries it on the stroke; then the shortest word;
 * then the first in the ayah. A code that does not run one character per
 * character of the text is not a line this app can draw, so it yields
 * nothing, and a family the ayah does not carry yields no sample rather than
 * a blank one.
 *
 * The print glues a pause sign to the end of some words. A sample is the
 * word, not the page's furniture, so the sign goes with the space.
 */
object TajweedSamples {

    /** A family's word, and the code that runs under it. */
    data class Sample(val family: TajweedFamily, val text: String, val code: String)

    /**
     * The samples [text] and [code] can give, in the families' own order.
     */
    fun of(text: String, code: String): List<Sample> {
        if (text.isEmpty() || text.length != code.length) return emptyList()
        val words = split(text, code)
        return TajweedFamily.entries.mapNotNull { family -> best(words, family) }
    }

    private class Word(val text: String, val code: String, val index: Int)

    /** The ayah's words, each with the slice of code that belongs to it. */
    private fun split(text: String, code: String): List<Word> {
        val words = ArrayList<Word>(32)
        var start = 0
        for (index in 0..text.length) {
            if (index == text.length || text[index] == ' ') {
                var end = index
                while (end > start && isFurniture(text[end - 1])) end--
                if (end > start) {
                    words += Word(text.substring(start, end), code.substring(start, end), words.size)
                }
                start = index + 1
            }
        }
        return words
    }

    private fun best(words: List<Word>, family: TajweedFamily): Sample? {
        var best: Word? = null
        var bestKey: IntArray? = null
        for (word in words) {
            val families = word.code.mapNotNull { familyOf(it) }.toSet()
            if (family !in families) continue
            val key = intArrayOf(
                if (families.size == 1) 1 else 0,
                letters(word, family),
                word.code.count { familyOf(it) == family },
                -word.text.length,
                -word.index,
            )
            if (bestKey == null || compare(key, bestKey) > 0) {
                best = word
                bestKey = key
            }
        }
        return best?.let { Sample(family, it.text, it.code) }
    }

    /** The family's colored characters that are letters and not marks. */
    private fun letters(word: Word, family: TajweedFamily): Int =
        word.code.indices.count { index ->
            familyOf(word.code[index]) == family && !Arabic.isMark(word.text[index].code)
        }

    private fun compare(first: IntArray, second: IntArray): Int {
        for (index in first.indices) {
            if (first[index] != second[index]) return first[index].compareTo(second[index])
        }
        return 0
    }

    private fun familyOf(code: Char): TajweedFamily? =
        TajweedRule.fromCode(code)?.let(TajweedFamily::of)

    /** The print's pause signs, and the joiners it glues to a word. */
    private fun isFurniture(ch: Char): Boolean =
        ch == '\u200C' || ch == '\u200D' || ch == '\u06DD' || ch == '\u06DE' ||
            ch == '\u06E9' || ch in '\u06D6'..'\u06DC'
}
