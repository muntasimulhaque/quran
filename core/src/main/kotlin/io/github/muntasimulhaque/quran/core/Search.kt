package io.github.muntasimulhaque.quran.core

import java.text.Normalizer
import java.util.Locale

/**
 * A typed query turned into terms the content database can match.
 *
 * The same normalization runs at build time on the indexed columns and here on
 * the query, so the two always meet: Arabic queries fold letter forms and lose
 * their marks, English queries lowercase. A query is Arabic when it contains
 * any Arabic letter; everything else is treated as English.
 */
data class SearchQuery(
    val input: String,
    val terms: List<String>,
    val arabic: Boolean,
)

object Search {

    /** One letter matches nearly everything; two is the shortest useful term. */
    const val MIN_TERM_LENGTH = 2

    /** Long queries are narrowed to their first terms rather than refused. */
    const val MAX_TERMS = 6

    private val whitespace = Regex("\\s+")

    /** Punctuation around a term goes; punctuation inside it (God's) stays. */
    private val edges = Regex("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$")

    /**
     * English is matched without its diacritics: a reader typing "allah"
     * expects to find "Allāh", and "isa" to find "ʿĪsā". Combining marks
     * fall away, the transliteration marks fold to nothing, and curly
     * apostrophes become straight ones. Word length may shrink here, which is
     * why matching works on whole folded words rather than on raw offsets.
     */
    fun normalizeEnglish(input: String): String {
        val lowered = Normalizer.normalize(input, Normalizer.Form.NFKD).lowercase(Locale.ROOT)
        val out = StringBuilder(lowered.length)
        var index = 0
        while (index < lowered.length) {
            val codepoint = lowered.codePointAt(index)
            index += Character.charCount(codepoint)
            when {
                Character.getType(codepoint) == Character.NON_SPACING_MARK.toInt() -> Unit
                codepoint == 0x02BF || codepoint == 0x02BE -> Unit // ayn and hamza marks
                codepoint == 0x2019 || codepoint == 0x2018 -> out.append('\'')
                codepoint in 0x2010..0x2015 -> out.append('-') // typographic dashes
                else -> out.appendCodePoint(codepoint)
            }
        }
        return out.toString().trim()
    }

    /**
     * The shape every indexed column carries. Arabic is stripped of its marks
     * and folded to base letter forms; Latin is folded to its plain letters.
     * Applying both in this order lets one column answer a query in either
     * script, which is what the reader expects from one search field.
     */
    fun normalizeForIndex(input: String): String = normalizeEnglish(Arabic.normalizeForSearch(input))

    /** A surah number, with an ayah when the reader gave one. */
    data class Reference(val surah: Int, val ayah: Int?)

    private val referencePrefixes = listOf("surah ", "sura ", "surat ", "s ")
    private val referencePattern = Regex("^(\\d{1,3})(?:[:.]\\s*(\\d{1,3})|\\s+(\\d{1,3}))?$")

    /**
     * Reads "2:255", "2.255", "2 255", "surah 2", and their Arabic digit
     * forms. A bare number up to 114 is a surah. Anything else is text.
     */
    fun reference(input: String): Reference? {
        var normalized = Arabic.normalizeForSearch(input.trim()).lowercase(Locale.ROOT)
        for (prefix in referencePrefixes) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.removePrefix(prefix).trim()
                break
            }
        }
        val match = referencePattern.find(normalized) ?: return null
        val surah = match.groupValues[1].toIntOrNull() ?: return null
        if (surah !in 1..114) return null
        val ayah = match.groupValues[2].ifEmpty { match.groupValues[3] }.toIntOrNull()
        if (ayah != null && ayah < 1) return null
        return Reference(surah, ayah)
    }

    /**
     * Where each term appears in [text], as offsets into the original text.
     * The text is folded once with an offsets map, so a match survives
     * diacritics, letter form differences, and transliteration marks while
     * the caller still highlights the letters the reader can see.
     */
    fun matchRanges(text: String, terms: List<String>, arabic: Boolean): List<IntRange> {
        if (text.isEmpty() || terms.isEmpty()) return emptyList()
        val folded = StringBuilder(text.length)
        val starts = ArrayList<Int>(text.length)
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val start = index
            index += Character.charCount(codePoint)
            if (Character.isWhitespace(codePoint)) {
                if (folded.isNotEmpty() && folded.last() != ' ') {
                    folded.append(' ')
                    starts.add(start)
                }
                continue
            }
            val piece = if (arabic) {
                Arabic.normalizeForSearch(String(Character.toChars(codePoint)))
            } else {
                normalizeEnglish(String(Character.toChars(codePoint)))
            }
            for (character in piece) {
                folded.append(character)
                starts.add(start)
            }
        }
        if (folded.isEmpty()) return emptyList()
        val haystack = folded.toString()
        val ranges = ArrayList<IntRange>()
        for (term in terms) {
            if (term.isEmpty()) continue
            var from = haystack.indexOf(term)
            var found = 0
            while (from >= 0 && found < 12) {
                val firstStart = starts[from]
                val lastStart = starts[from + term.length - 1]
                var end = lastStart + Character.charCount(text.codePointAt(lastStart))
                // Keep the marks that belong to the last matched letter inside
                // the highlight, so the wash covers the whole visible word.
                while (end < text.length && isMark(text.codePointAt(end))) {
                    end += Character.charCount(text.codePointAt(end))
                }
                ranges += firstStart until end
                found++
                from = haystack.indexOf(term, from + 1)
            }
        }
        return mergeRanges(ranges)
    }

    /**
     * A readable window of [text] around the first match of [terms], with the
     * matched ranges rebased onto the window. Used for tafsir results, where
     * the passage can be many pages long and only the sentence matters.
     */
    fun excerpt(
        text: String,
        terms: List<String>,
        arabic: Boolean,
        window: Int = 180,
    ): Pair<String, List<IntRange>> {
        val ranges = matchRanges(text, terms, arabic)
        if (ranges.isEmpty() || text.length <= window * 2) return text to ranges
        val first = ranges.first().first
        var start = (first - window).coerceAtLeast(0)
        var end = (first + window).coerceAtMost(text.length)
        // Do not cut a word in half at either edge.
        while (start > 0 && !text[start - 1].isWhitespace()) start++
        while (end < text.length && !text[end].isWhitespace()) end--
        if (start > end) return text to ranges
        val prefix = if (start > 0) "... " else ""
        val suffix = if (end < text.length) " ..." else ""
        val rebased = ranges.filter { it.first >= start && it.last < end }
            .map { (it.first - start + prefix.length)..(it.last - start + prefix.length) }
        return (prefix + text.substring(start, end) + suffix) to rebased
    }

    private fun isMark(codePoint: Int): Boolean =
        Character.getType(codePoint) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(codePoint) == Character.COMBINING_SPACING_MARK.toInt() ||
            codePoint == 0x0640

    private fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
        if (ranges.size < 2) return ranges
        val sorted = ranges.sortedBy { it.first }
        val merged = ArrayList<IntRange>(sorted.size)
        var current = sorted.first()
        for (range in sorted.drop(1)) {
            current = if (range.first <= current.last + 1) {
                current.first..maxOf(current.last, range.last)
            } else {
                merged += current
                range
            }
        }
        merged += current
        return merged
    }

    fun parse(input: String): SearchQuery? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val arabic = trimmed.codePoints().anyMatch { RichText.isArabic(it) }
        val normalized = if (arabic) {
            Arabic.normalizeForSearch(trimmed)
        } else {
            normalizeEnglish(trimmed)
        }
        val terms = normalized.split(whitespace)
            .map { edges.replace(it, "") }
            .filter { it.length >= MIN_TERM_LENGTH }
            .distinct()
            .take(MAX_TERMS)
        if (terms.isEmpty()) return null
        return SearchQuery(input = trimmed, terms = terms, arabic = arabic)
    }

    /**
     * The LIKE pattern for one term. Percent and underscore are literal here,
     * not wildcards, and are escaped for a query that declares ESCAPE '\'.
     */
    fun pattern(term: String): String {
        val escaped = term
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return "%$escaped%"
    }
}
