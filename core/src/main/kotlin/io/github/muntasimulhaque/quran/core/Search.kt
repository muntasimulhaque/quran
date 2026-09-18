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
