package io.github.muntasimulhaque.quran.core

import kotlin.math.abs

/**
 * The one word a reader meant to type.
 *
 * A query that finds nothing anywhere is the worst moment of a search: the
 * reader is told only that the Book does not say what they asked. Mostly it
 * does, and the word is one letter away. This reads the words the reader's own
 * library already holds and names the nearest one, so the sheet can offer it
 * back as a word to tap rather than as a lesson in query syntax (owner
 * decision, 22.3).
 *
 * **One suggestion, never a list.** A list of candidates is a page, and a page
 * at the moment a search failed is noise: the reader either takes the word or
 * rephrases, and one word makes that choice. It is the *best* word by the rule
 * in [Lexicon], not merely the first one found.
 *
 * **The Book's own frequency breaks the tie.** Of two words equally far from
 * what was typed, the one the Quran says more often is the one a reader is
 * likelier to have meant. That number is in the content already, so nothing
 * has to be guessed or stored.
 *
 * **The spoken shape outranks the letters.** A reader typing a
 * transliteration is not making two mistakes: k for q is one sound, the
 * vowel a transliteration never agrees on is none, and the final h of a
 * ta marbuta is furniture. A Latin term of five letters and up is ranked
 * by the distance between spoken shapes before the letter distance is
 * read, and a word that only sounds near (banana for bakara) is never
 * offered at all. This is the tier that made "bakara" offer "Al-Baqarah"
 * instead of the fruit the alphabet kept picking (owner decision, 22.5).
 *
 * **What the spoken shape cannot do is cross into the word a translation
 * chose instead.** "musa" finds its glosses and "moosa" is offered "musa",
 * but a reader whose word for prayer is "namaz" is owed the translation's
 * "prayer", and no shape reaches across that gap. The list of equivalents
 * that would close it is deferred (owner decision, 22.5).
 *
 * **Only a whole token is corrected.** A term with inner punctuation (`god's`,
 * `al-kahf`) is a phrase of fragments in the index, and its pieces are what
 * the reader meant; it is never a misspelling. [Search.isSingleToken] is the
 * one rule for that, shared with the expression builder so the two cannot
 * disagree.
 *
 * **The lexicon is the library, not the tafsirs.** The candidate words are the
 * reader's word list, translations, and the Quran's own words. The tafsirs are
 * tens of megabytes of commentary, and a reader who misspells a name is not
 * looking for the rarest word in a commentary; reading them on the zero-match
 * frame would cost seconds to buy almost nothing. The reader's own notes are
 * not a vocabulary to correct against either.
 */
object Suggest {

    /** A term shorter than this is too little to be a spelling. */
    const val MIN_CULPRIT_LENGTH = 3

    /** Terms up to this length earn one edit, longer ones two. */
    private const val ONE_EDIT_LENGTH = 4

    /**
     * Terms of five letters and up are ranked by how they sound. Five is the
     * same boundary as [ONE_EDIT_LENGTH]: it is where a term earns two edits,
     * and two edits are where the spoken shape earns its say, because a term
     * one edit away can only be one sound away already (owner decision, 22.5).
     */
    private const val SPOKEN_TERM_LENGTH = 5

    /** A spoken shape shorter than this matches too much to rank by. */
    private const val SPOKEN_SHAPE_LENGTH = 2

    /** How far a word may sound from the term and still be offered. */
    private const val SPOKEN_DISTANCE_LIMIT = 1

    /** The vowels a transliteration never agrees on. */
    private const val SPOKEN_VOWELS = "aeiou"

    /**
     * The nearest word to one of the reader's terms.
     *
     * [culprit] is the term the reader typed, already in the query's
     * normalized shape, so the caller can point the correction at the same
     * letters in the raw text. A null result is an honest answer: no word in
     * the reader's library is close enough to be worth offering.
     */
    data class Candidate(
        val culprit: String,
        val word: String,
        val distance: Int,
        val count: Int,
        /**
         * How far the word sounds from the culprit, or null when the letters
         * alone ranked it. Null for Arabic terms and Latin terms shorter than
         * [SPOKEN_TERM_LENGTH].
         */
        val spoken: Int? = null,
    )

    /**
     * The words one search could have meant, with how often each appears.
     *
     * The counts come from the indexed columns, so a word's frequency is the
     * library's own fact and nothing is built beside the content. The lexicon
     * is made once per connection and searched only after a query has already
     * found nothing, which is why a scan of the words near the reader's own
     * length and script is the right shape here: it costs nothing on the
     * keystroke path and answers in a frame on the only frame that asks.
     *
     * [names] maps a folded name form to the display name it is offered as:
     * the surahs' names are seeded by the caller as vocabulary, and the word
     * that comes back to the reader is the name the interface draws, so
     * "bakara" is offered "Al-Baqarah" and the tap searches a name the reader
     * can read (owner decision, 22.5).
     */
    class Lexicon(counts: Map<String, Int>, names: Map<String, String> = emptyMap()) {

        private class Entry(
            val word: String,
            val display: String,
            val count: Int,
            val length: Int,
            val points: IntArray,
            val spoken: IntArray,
        )

        /** Words grouped by their length in code points: a near word's length is near. */
        private val latinByLength: Map<Int, List<Entry>>

        private val arabicByLength: Map<Int, List<Entry>>

        private val counts: Map<String, Int>

        init {
            val latin = HashMap<Int, MutableList<Entry>>()
            val arabic = HashMap<Int, MutableList<Entry>>()
            for ((word, count) in counts) {
                if (word.isEmpty()) continue
                val length = word.codePointCount(0, word.length)
                val points = word.codePoints().toArray()
                val spoken = spokenShape(word).codePoints().toArray()
                val entry = Entry(word, names[word] ?: word, count, length, points, spoken)
                val script = if (word.any { RichText.isArabic(it.code) }) arabic else latin
                script.getOrPut(length) { ArrayList() } += entry
            }
            latinByLength = latin
            arabicByLength = arabic
            this.counts = counts
        }

        /** How many words the reader's library can have meant. */
        val size: Int get() = counts.size

        fun holds(term: String): Boolean = counts.containsKey(term)

        /** The best word for the reader's terms, or null when none is near enough. */
        fun suggest(terms: List<String>): Candidate? {
            if (counts.isEmpty()) return null
            var best: Candidate? = null
            for (term in terms) {
                val candidate = suggestTerm(term) ?: continue
                val current = best
                if (current == null || better(candidate, current)) best = candidate
            }
            return best
        }

        /**
         * The best word for one term, or null when the term is too short,
         * already held, punctuated, or near nothing. A Latin term of five
         * letters and up is ranked by how it sounds first, and a word that
         * sounds further than [SPOKEN_DISTANCE_LIMIT] is refused even when its
         * letters are near: the word the reader meant sounds like what they
         * typed, and a word that does not is a wrong answer wearing a near
         * spelling (owner decision, 22.5).
         */
        private fun suggestTerm(term: String): Candidate? {
            val length = term.codePointCount(0, term.length)
            if (length < MIN_CULPRIT_LENGTH) return null
            if (counts.containsKey(term)) return null
            if (!Search.isSingleToken(term)) return null
            val limit = if (length <= ONE_EDIT_LENGTH) 1 else 2
            val arabic = term.any { RichText.isArabic(it.code) }
            val points = term.codePoints().toArray()
            // A word in the other script is never one or two edits away,
            // so the scan reads the reader's own script and the rest of the
            // vocabulary costs the build nothing and the scan less.
            val buckets = if (arabic) arabicByLength else latinByLength
            val spokenPoints = if (arabic) {
                null
            } else {
                val shape = spokenShape(term)
                if (length >= SPOKEN_TERM_LENGTH &&
                    shape.codePointCount(0, shape.length) >= SPOKEN_SHAPE_LENGTH
                ) {
                    shape.codePoints().toArray()
                } else {
                    null
                }
            }
            var best: Candidate? = null
            for (candidateLength in maxOf(1, length - limit)..(length + limit)) {
                val entries = buckets[candidateLength] ?: continue
                for (entry in entries) {
                    val distance = distance(points, entry.points, limit)
                    if (distance > limit) continue
                    val spoken = if (spokenPoints != null) {
                        val value = distance(spokenPoints, entry.spoken, SPOKEN_DISTANCE_LIMIT)
                        if (value > SPOKEN_DISTANCE_LIMIT) continue
                        value
                    } else {
                        null
                    }
                    val candidate = Candidate(term, entry.display, distance, entry.count, spoken)
                    val current = best
                    if (current == null || betterWithinTerm(candidate, current)) best = candidate
                }
            }
            return best
        }

        /**
         * The order one term's candidates are chosen by: how they sound, then
         * the order the term would use without the spoken tier. The candidates
         * of one term always share the tier, so the spoken distance decides
         * before the letters do and never after them.
         */
        private fun betterWithinTerm(candidate: Candidate, current: Candidate): Boolean {
            if (candidate.spoken != null && current.spoken != null) {
                if (candidate.spoken != current.spoken) return candidate.spoken < current.spoken
            }
            return better(candidate, current)
        }

        /**
         * The order the one suggestion is chosen by: the shortest distance, the
         * likelier word, the shorter spelling, then the alphabet.
         *
         * The last two are not taste: two words can be equally near and equally
         * common, and a search that answered differently on two runs would be a
         * defect nobody could reproduce. The earlier culprit wins a tie across
         * terms because the caller only replaces on a strict improvement.
         */
        private fun better(candidate: Candidate, current: Candidate): Boolean {
            if (candidate.distance != current.distance) return candidate.distance < current.distance
            if (candidate.count != current.count) return candidate.count > current.count
            val candidateLength = candidate.word.codePointCount(0, candidate.word.length)
            val currentLength = current.word.codePointCount(0, current.word.length)
            if (candidateLength != currentLength) return candidateLength < currentLength
            return candidate.word < current.word
        }
    }

    /** The best word for [terms], or null. Kept here so a caller names the act. */
    fun suggest(terms: List<String>, lexicon: Lexicon): Candidate? = lexicon.suggest(terms)

    /**
     * The shape a transliterated word keeps when a reader spells it their own
     * way: `q` folds into `k`, the vowels fall away, and a final `h` (the ta
     * marbuta) or apostrophe falls away with them. "bakara" and "baqarah" are
     * one shape, and so are "koran" and "quran".
     *
     * This is the Latin twin of the recall shape: the Arabic one lets spellings
     * meet inside the index, and this one lets them meet inside the suggester's
     * ranking. It is never an index and no column carries it; only the
     * suggester reads it, and only for a Latin term (owner decision, 22.5).
     */
    fun spokenShape(word: String): String {
        val out = StringBuilder(word.length)
        for (character in word) {
            when {
                character == 'q' -> out.append('k')
                character in SPOKEN_VOWELS -> Unit
                else -> out.append(character)
            }
        }
        while (out.isNotEmpty() && (out.last() == 'h' || out.last() == '\'')) {
            out.deleteCharAt(out.length - 1)
        }
        return out.toString()
    }

    /**
     * The reader's own query with the mistyped word replaced, or null when the
     * word cannot be found again in the raw text.
     *
     * The range comes from [Search.matchRanges], the same folding every
     * highlight uses, so a correction lands on the letters the reader can see
     * and leaves the rest of the query exactly as it was: a reader who typed
     * three words and misspelled one taps once, not three times (owner
     * decision, 22.3). A null is the honest answer when the letters cannot be
     * located, and the sheet then offers nothing rather than a rewording the
     * reader never wrote.
     */
    fun correction(input: String, culprit: String, word: String, arabic: Boolean): String? {
        val range = Search.matchRanges(input, listOf(culprit), arabic).firstOrNull() ?: return null
        return input.replaceRange(range, word)
    }

    /**
     * The edit distance between two words, or a number past [limit] when it is
     * certainly more. One swap of two neighbours counts as one edit, because a
     * transposition is a finger landing early and not two mistakes.
     *
     * Only alignments inside the diagonal band of [limit] are computed: a
     * word more than [limit] positions out of step cannot be [limit] edits
     * away, and leaving those cells alone is what keeps a scan over a whole
     * vocabulary to a frame.
     */
    fun damerau(a: String, b: String, limit: Int): Int =
        distance(a.codePoints().toArray(), b.codePoints().toArray(), limit)

    private fun distance(x: IntArray, y: IntArray, limit: Int): Int {
        val rows = x.size
        val columns = y.size
        if (abs(rows - columns) > limit) return limit + 1
        if (rows == 0) return columns
        if (columns == 0) return rows
        val saturated = limit + 1
        var prevPrev = IntArray(columns + 1) { saturated }
        var prev = IntArray(columns + 1) { saturated }
        var current = IntArray(columns + 1) { saturated }
        for (column in 0..minOf(columns, limit)) prev[column] = column
        for (row in 1..rows) {
            current.fill(saturated)
            current[0] = if (row <= limit) row else saturated
            var rowMinimum = current[0]
            val from = maxOf(1, row - limit)
            val to = minOf(columns, row + limit)
            for (column in from..to) {
                val cost = if (x[row - 1] == y[column - 1]) 0 else 1
                var value = minOf(prev[column] + 1, current[column - 1] + 1, prev[column - 1] + cost)
                if (row > 1 && column > 1 &&
                    x[row - 1] == y[column - 2] && x[row - 2] == y[column - 1]
                ) {
                    value = minOf(value, prevPrev[column - 2] + 1)
                }
                if (value > limit) value = saturated
                current[column] = value
                if (value < rowMinimum) rowMinimum = value
            }
            // Every cell inside the band is already past the limit, and a
            // later row only ever derives from this one, so the answer is
            // settled: no word this far out can still be near enough.
            if (rowMinimum > limit) return saturated
            val spent = prevPrev
            prevPrev = prev
            prev = current
            current = spent
        }
        return if (prev[columns] > limit) saturated else prev[columns]
    }
}
