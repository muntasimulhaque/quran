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
 *
 * [phrases] holds the words a reader wrapped in double quotes, each span in
 * the reader's own order, because those words must sit beside one another and
 * not merely appear in the same ayah (owner decision, 22.3). The same words
 * stay in [terms]: a phrase narrows a search, it never removes a word from
 * the highlights, the excerpts, or the fallback shape an old pack reads.
 */
data class SearchQuery(
    val input: String,
    val terms: List<String>,
    val arabic: Boolean,
    val phrases: List<List<String>> = emptyList(),
)

object Search {

    /**
     * A run of two or more Arabic letters, their marks allowed between them.
     *
     * The property matches letters and not the harakat, so a vocalized word
     * is a letter, a mark, a letter, and a plain {4,} never matches one: the
     * excerpt kept its quotation. Two letters are enough to call a run a
     * quotation in a Latin excerpt, and the window is cut at the run's first
     * letter so no half-word is left behind (owner report, this build).
     */
    private val arabicQuote = Regex("""\p{IsArabic}(?:\p{M}*\p{IsArabic}){1,}""")

    /** One letter matches nearly everything; two is the shortest useful term. */
    const val MIN_TERM_LENGTH = 2

    /** Long queries are narrowed to their first terms rather than refused. */
    const val MAX_TERMS = 6

    /** A pasted paragraph is a search, not a document to scan. */
    const val MAX_INPUT_LENGTH = 200

    /** One term longer than this cannot be a useful word. */
    const val MAX_TERM_LENGTH = 64

    private val whitespace = Regex("\\s+")

    /**
     * Punctuation around a term goes; punctuation inside it (God's) stays. A
     * combining mark is part of its word and is never trimmed: a Bengali word
     * may end in a vowel sign (কি), and trimming it would leave a term shorter
     * than [MIN_TERM_LENGTH] and drop the whole query.
     */
    private val edges = Regex("^[^\\p{L}\\p{N}\\p{M}]+|[^\\p{L}\\p{N}\\p{M}]+$")

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

    private val referencePrefixes = listOf("surah ", "sura ", "surat ", "s ", "সূরা ")
    private val referencePattern = Regex("^(\\d{1,3})(?:[:.]\\s*(\\d{1,3})|\\s+(\\d{1,3}))?$")

    /**
     * Reads "2:255", "2.255", "2 255", "surah 2", and their Arabic and Bengali
     * digit forms. A bare number up to 114 is a surah. Anything else is text.
     */
    fun reference(input: String): Reference? {
        var normalized = Arabic.normalizeForSearch(asciiDigits(input.trim())).lowercase(Locale.ROOT)
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

    private val juzPrefixes = listOf("juz ", "para ", "পারা ", "جزء ", "الجزء ")

    /**
     * Reads the juz a reader named: "juz 30", "para 30", "পারা ৩০", "جزء 30".
     *
     * The juz is a reference like any other and lands on its own first ayah,
     * so a reader who thinks in paras does not have to know which surah one
     * opens (owner decision, 22.3). The word is required: a bare "30" is
     * surah 30 and has been since the field first read references. `পারা` is
     * the app's own Bangla word for it, the one the Browse tab already wears.
     */
    fun juzReference(input: String): Int? {
        val normalized = Arabic.normalizeForSearch(asciiDigits(input.trim())).lowercase(Locale.ROOT)
        val rest = juzPrefixes
            .firstOrNull { normalized.startsWith(it) }
            ?.let { normalized.removePrefix(it).trim() }
            ?: return null
        if (rest.isEmpty() || rest.any { !it.isDigit() }) return null
        return rest.toIntOrNull()?.takeIf { it in 1..30 }
    }

    /**
     * The surah a numbered reference names when that number is past the end of
     * the Book: "115", "999:1".
     *
     * It is not a place, so the reference grammar refuses it and the query
     * falls through to the words like any other text. The number is kept here
     * anyway, because the sheet's one honest answer to it is the Book's own
     * count, not "No matches" (owner decision, 22.3).
     */
    fun referenceBeyondBook(input: String): Int? {
        var normalized = Arabic.normalizeForSearch(asciiDigits(input.trim())).lowercase(Locale.ROOT)
        for (prefix in referencePrefixes) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.removePrefix(prefix).trim()
                break
            }
        }
        val match = referencePattern.find(normalized) ?: return null
        val surah = match.groupValues[1].toIntOrNull() ?: return null
        return surah.takeIf { it !in 1..114 }
    }

    /**
     * A surah named in words, with the ayah the reader gave: "baqara 255",
     * "al kahf 10", "النور 24". The last word must be the ayah number, so a
     * name of several words keeps them, and the leading "surah" the numbered
     * form accepts is dropped the same way. The name itself is resolved by
     * the content, which owns the surah names.
     */
    data class NameReference(val name: String, val ayah: Int)

    /**
     * Reads a reference whose surah is named rather than numbered. The last
     * word must be an ayah number and what precedes it must carry a letter:
     * "2 255" belongs to the numbered form, and "mercy 2" stays text.
     */
    fun nameReference(input: String): NameReference? {
        var rest = input.trim()
        val lowered = rest.lowercase(Locale.ROOT)
        for (prefix in referencePrefixes) {
            if (lowered.startsWith(prefix)) {
                rest = rest.substring(prefix.length).trim()
                break
            }
        }
        val words = rest.split(whitespace)
        if (words.size < 2) return null
        val number = Arabic.normalizeForSearch(asciiDigits(words.last()))
        if (number.isEmpty() || number.any { !it.isDigit() }) return null
        val ayah = number.toIntOrNull() ?: return null
        if (ayah < 1) return null
        val name = words.dropLast(1).joinToString(" ")
        if (name.none { it.isLetter() }) return null
        return NameReference(name, ayah)
    }

    /**
     * Articles a reader drops from a surah's name: "kahf" finds Al-Kahf and
     * "বাকারা" finds আল-বাকারা. The list is script-aware, holding the Latin
     * transliteration, its assimilated forms, the same article in Bengali,
     * and the Arabic article itself. Each entry is reduced through [nameKey]
     * first, so the article and the stored name meet in the same shape.
     */
    private val nameArticles =
        listOf(
            "ash", "ath", "adh", "al", "an", "as", "at", "ar", "az", "ad",
            "আল", "আলে", "আন", "আর", "আস", "আত", "আশ", "আয", "আদ",
            "ال",
        ).map { nameKey(it) }.filter { it.isNotEmpty() }.distinct()

    /**
     * The one shape a typed surah name and a stored one meet in: letters,
     * digits, and the marks a script uses inside a word, so case, hyphens,
     * apostrophes, and the space between two words never block a match ("an
     * nas" and "An-Nas" are one key). Bengali vowel signs are spacing marks,
     * not letters: dropping them made আন-নিসা and আন-নাস the same key, and a
     * reference to one opened the other.
     */
    fun nameKey(input: String): String = normalizeForIndex(input).filter {
        it.isLetterOrDigit() ||
            Character.getType(it) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(it) == Character.COMBINING_SPACING_MARK.toInt()
    }

    /**
     * The words a reader puts in front of a surah's name: "surah baqarah" is a
     * reader naming a surah, and the word for one is not part of its name. The
     * surah match drops these terms; the text search keeps them, because
     * "surah" is also a word the Book says and a reader may be looking for it
     * (owner decision, 22.5).
     */
    private val nameQualifiers = setOf("surah", "sura", "surat", "সূরা", "سورة")

    /** Whether [term] is the word for a surah rather than a surah's name. */
    fun isNameQualifier(term: String): Boolean = term in nameQualifiers

    /**
     * Every key a surah's name may be typed by: the whole key, and the same
     * with a leading article removed, because readers say "baqara" for
     * "Al-Baqarah" and "rahman" for "Ar-Rahman".
     */
    fun surahNameForms(name: String): Set<String> {
        val key = nameKey(name)
        if (key.isEmpty()) return emptySet()
        val forms = LinkedHashSet<String>()
        forms += key
        for (article in nameArticles) {
            if (key.startsWith(article) && key.length > article.length) {
                forms += key.removePrefix(article)
                break
            }
        }
        return forms
    }

    /**
     * The spellings a surah carried before its English name was reviewed:
     * "Al-Anbya" still finds Al-Anbiya, and the four other QUL variants find
     * the surahs they name. A reader who learned a name from an older release
     * is not left with a search that no longer knows it (owner decision,
     * 19.7).
     */
    private val surahAliases = mapOf(
        21 to listOf("Al-Anbya"),
        42 to listOf("Ash-Shuraa"),
        58 to listOf("Al-Mujadila"),
        61 to listOf("As-Saf"),
        93 to listOf("Ad-Duhaa"),
    )

    /** Every key an older spelling of a surah's name may be typed by. */
    fun surahNameAliases(number: Int): Set<String> =
        surahAliases[number].orEmpty().flatMap { surahNameForms(it) }.toSet()

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
        // Folding one code point at a time is what keeps the highlight on the
        // letters the reader can see, and an excerpt can be a page of prose:
        // the folded shape of a code point is remembered for the length of the
        // call, so the normalizer runs once per distinct character instead of
        // once per character (owner report, 6.5).
        val latinFolds = if (arabic) null else HashMap<Int, String>()
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
            if (arabic) {
                if (Arabic.isSkippedForSearch(codePoint)) continue
                folded.appendCodePoint(Arabic.foldForSearch(codePoint))
                starts.add(start)
            } else {
                val piece = latinFolds?.getOrPut(codePoint) {
                    normalizeEnglish(String(Character.toChars(codePoint)))
                } ?: normalizeEnglish(String(Character.toChars(codePoint)))
                for (character in piece) {
                    folded.append(character)
                    starts.add(start)
                }
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
     *
     * The window is taken from the readable form of the text, never the stored
     * one: a tafsir is kept as a small HTML subset for its panels to parse, and
     * a search result has no parser behind it, so a raw window would print
     * `</p><h2>` at the reader. Matching runs on that same readable form, which
     * keeps the highlight on the words the reader can see.
     *
     * The window is widened, never narrowed, to the nearest word boundaries,
     * and it always contains the first match: a highlight that points at
     * nothing, or a sentence that starts in the middle of a word, would be
     * worse than a window a few characters wider than asked for.
     */
    fun excerpt(
        text: String,
        terms: List<String>,
        arabic: Boolean,
        window: Int = 180,
    ): Pair<String, List<IntRange>> {
        val readable = RichText.plain(text)
        val ranges = matchRanges(readable, terms, arabic)
        if (ranges.isEmpty() || readable.length <= window * 2) return readable to ranges
        // The window is one match's, not the span of every match: a tafsir
        // passage can carry the query in two places thousands of letters
        // apart, and a window that reached from the first to the last was
        // the whole passage, with a Quran quotation in the middle of it that
        // the trim never looked at (owner report, this build).
        val first = ranges.first().first
        val last = ranges.first().last + 1
        var start = (first - window).coerceAtLeast(0)
        var end = (last + window).coerceAtMost(readable.length)
        while (start > 0 && !readable[start - 1].isWhitespace()) start--
        while (end < readable.length && !readable[end].isWhitespace()) end++
        // A Latin excerpt keeps Latin. A tafsir's Quran quotation is a block
        // of its own in the sheet, and inline in a one-line snippet it
        // collides with the sentence around it: the Arabic runs right to left
        // across the Latin line and the punctuation lands on the wrong side.
        // The window therefore stops at the nearest quotation rather than
        // starting in the middle of one. A short term stays, because a term
        // is not a passage, and an Arabic query keeps its Arabic (owner
        // decision, this build).
        if (!arabic) {
            val quotes = arabicQuote.findAll(readable, start)
                .takeWhile { it.range.first < end }
                .toList()
            quotes.lastOrNull { it.range.last < first }?.let { quote ->
                start = maxOf(start, quote.range.last + 1)
            }
            quotes.firstOrNull { it.range.first > last }?.let { quote ->
                end = minOf(end, quote.range.first)
            }
            while (start < first && readable[start].isWhitespace()) start++
            while (end > last && readable[end - 1].isWhitespace()) end--
        }
        start = minOf(start, first)
        end = maxOf(end, last)
        if (start >= end) return readable to ranges
        // One ellipsis character, not three periods: the mark carries its own
        // spacing, and the reader sees the cut as the cut it is. The prefix is
        // two characters and the suffix two, so the highlight offsets shift by
        // one and no more.
        val prefix = if (start > 0) "… " else ""
        val suffix = if (end < readable.length) " …" else ""
        val rebased = ranges.filter { it.first >= start && it.last < end }
            .map { (it.first - start + prefix.length)..(it.last - start + prefix.length) }
        return (prefix + readable.substring(start, end) + suffix) to rebased
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

    /**
     * Reads the query, with the words a reader wrapped in quotes kept apart.
     *
     * The quote marks are the keyboard's own and the typographic pair a phone
     * editor inserts; both pair the same way, and the marks themselves never
     * reach the normalized text. An unclosed quote is dropped and no span
     * comes from it, because a reader who has not finished typing has not
     * finished a phrase either (owner decision, 22.3). A span of one word is
     * no phrase: it is the word the reader quoted, searched as that word.
     *
     * The reference grammar still reads the raw input, so a quoted reference
     * is a phrase of its words and not a place. A reader who wants the place
     * types it unquoted, which is how they found it in the first place.
     */
    fun parse(input: String): SearchQuery? {
        val trimmed = input.trim().take(MAX_INPUT_LENGTH)
        if (trimmed.isEmpty()) return null
        val arabic = trimmed.codePoints().anyMatch { RichText.isArabic(it) }
        val plain = trimmed.filterNot { it.isQuoteMark() }
        val terms = words(plain, arabic).distinct().take(MAX_TERMS)
        if (terms.isEmpty()) return null
        val phrases = quotedSpans(trimmed)
            .map { span -> words(span, arabic) }
            .filter { it.size >= 2 }
        return SearchQuery(input = trimmed, terms = terms, arabic = arabic, phrases = phrases)
    }

    /** One text's terms in the shape the content's indexed columns hold. */
    private fun words(input: String, arabic: Boolean): List<String> {
        val normalized = if (arabic) {
            Arabic.normalizeForSearch(input)
        } else {
            normalizeEnglish(input)
        }
        return normalized.split(whitespace)
            .map { edges.replace(it, "").take(MAX_TERM_LENGTH) }
            .filter { it.length >= MIN_TERM_LENGTH }
    }

    /** The text between each pair of quote marks, in the reader's order. */
    private fun quotedSpans(input: String): List<String> {
        val marks = input.indices.filter { input[it].isQuoteMark() }
        val spans = ArrayList<String>(marks.size / 2)
        var pair = 0
        while (pair + 1 < marks.size) {
            val from = marks[pair] + 1
            val to = marks[pair + 1]
            if (to > from) spans += input.substring(from, to)
            pair += 2
        }
        return spans
    }

    private fun Char.isQuoteMark(): Boolean =
        this == '"' || this == '\u201C' || this == '\u201D'

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

    /**
     * The same terms as an expression the content's own full text index
     * understands: each term as a token prefix, every term required, joined
     * with AND. A term that carries punctuation (God's, al-kahf) is matched as
     * the phrase of its own fragments, because the index breaks an ASCII
     * non-alphanumeric into a token boundary: the fragments are adjacent
     * tokens in the index, in that order.
     *
     * A phrase the reader quoted is asked precisely: its words as they were
     * typed, adjacent in that order, with no prefix star and no hamza-seat
     * variant, because quotes are the reader asking for this wording and not
     * a word that begins this way (owner decision, 22.3). Its words are still
     * in [terms], so they keep their highlight and their place in the recall
     * shape; they are simply not asked a second time as prefixes beside the
     * phrase that already requires them.
     *
     * The index folds nothing itself; the content build writes the same
     * normalized shape [normalizeForIndex] gives a query into the indexed
     * column, so the two sides meet. The expression is built here, beside
     * [parse], so the app's matching and the index's matching cannot drift
     * (owner decision, 6.5).
     */
    /**
     * The query terms plus the spellings an older index may hold for a hamza
     * seat. The Quran index was rebuilt with the bare seat; the tafsir index
     * still carries the precomposed seat folded to a second waw or yeh. A
     * query for شيا must find both, or the Quran fix would hide the tafsir
     * passages that used the other spelling.
     */
    fun expandedTerms(terms: List<String>): List<String> {
        val out = LinkedHashSet<String>()
        for (term in terms) {
            if (term.length > MAX_TERM_LENGTH) {
                out += term
                continue
            }
            out += term
            // The Prophet's blessing is one ligature in the tafsir and
            // three words in a reader's query. The old index keeps the
            // ligature; a query for صلى must still reach it.
            if (term == "صلي") out += "ﷺ"
            if (term.any { it == 'ي' || it == 'و' }) {
                for (index in 0..term.length) {
                    out += term.substring(0, index) + 'ي' + term.substring(index)
                    out += term.substring(0, index) + 'و' + term.substring(index)
                }
            }
        }
        return out.toList()
    }

    fun matchExpression(
        terms: List<String>,
        phrases: List<List<String>> = emptyList(),
    ): String? {
        val inPhrase = phrases.flatten().toHashSet()
        val parts = ArrayList<String>(terms.size + phrases.size)
        for (phrase in phrases) {
            val fragments = phrase
                .flatMap { word -> word.split(SEPARATORS) }
                .filter { it.isNotEmpty() }
            if (fragments.size >= 2) parts += "\"" + fragments.joinToString(" ") + "\""
        }
        for (term in terms) {
            if (term in inPhrase) continue
            val alternatives = expandedTerms(listOf(term)).mapNotNull { variant ->
                val fragments = variant.split(SEPARATORS).filter { it.isNotEmpty() }
                when {
                    fragments.isEmpty() -> null
                    fragments.size == 1 -> fragments[0] + "*"
                    else -> "\"" + fragments.joinToString(" ") + "\""
                }
            }
            parts += when (alternatives.size) {
                0 -> continue
                1 -> alternatives[0]
                else -> "(" + alternatives.joinToString(" OR ") + ")"
            }
        }
        if (parts.isEmpty()) return null
        return parts.joinToString(" AND ")
    }

    /**
     * The recall shape of each term, in order and once each. A shape shorter
     * than [MIN_TERM_LENGTH] is refused: two letters is the shortest useful
     * term, and a one-letter skeleton would match a large part of the Book.
     */
    fun skeletonTerms(terms: List<String>): List<String> =
        terms.map { Arabic.recallShape(it) }
            .filter { it.length >= MIN_TERM_LENGTH }
            .distinct()

    /**
     * The fallback expression: every term in the recall shape, as a token
     * prefix, all required. The exact [matchExpression] is asked first; this
     * one is asked only when the exact index names no ayah, so a query the
     * print already answers keeps its precise result and never gains the
     * broader spellings beside it (owner decision, 19.7).
     *
     * The seat expansion in [expandedTerms] is deliberately not used here:
     * the recall shape has already removed the seats and the long vowels, and
     * putting them back would undo the fallback.
     */
    fun skeletonExpression(terms: List<String>): String? {
        val parts = skeletonTerms(terms).map { "$it*" }
        return if (parts.isEmpty()) null else parts.joinToString(" AND ")
    }

    /**
     * The shape a text is indexed under for the recall fallback. The same
     * rule as [skeletonTerms], applied to a whole ayah or word at build time,
     * so the indexed column and a typed query cannot be two stories (owner
     * decision, 19.7).
     */
    fun skeletonForIndex(input: String): String = Arabic.recallShape(normalizeForIndex(input))

    /**
     * Whether a term is one token of the index: it carries no character the
     * index would break on. A term that does is a phrase of its own fragments,
     * and the suggester never offers to correct one, because its pieces are
     * what the reader meant (owner decision, 22.3).
     */
    fun isSingleToken(term: String): Boolean = !SEPARATORS.containsMatchIn(term)

    /**
     * What the index's tokenizer treats as a break: an ASCII character that is
     * not a letter or a digit. SQLite's simple tokenizer keeps every non-ASCII
     * code point, a combining mark included, inside its token, and the app must
     * meet it there. Splitting on marks turned a Bengali term into the phrase
     * of its letter fragments, which could never match the single token the
     * index holds (owner report, 8.5).
     */
    private val SEPARATORS = Regex("[\\x00-\\x7F&&[^A-Za-z0-9]]+")

    /**
     * The tokens the index's own tokenizer holds in one text: every ASCII
     * character that is not a letter or a digit is a break, and every other
     * code point stays inside its token, a combining mark included.
     *
     * The suggester's vocabulary reads these tokens rather than the text's
     * whitespace words, because a count is only honest when it counts what the
     * search matches: a lexicon that held "muhammad]" as a word offered brackets
     * back to the reader and split one word's frequency across its punctuation
     * (owner decision, 22.5). The walk is written out rather than handed to a
     * regular expression, the way the vocabulary's walk always has been: a
     * vocabulary reads a few hundred thousand tokens once per connection, and a
     * matcher per token would cost the reader a second on the one frame that
     * asks for it.
     */
    fun indexTokens(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val tokens = ArrayList<String>()
        val length = text.length
        var index = 0
        while (index < length) {
            while (index < length && isIndexSeparator(text[index])) index++
            val start = index
            while (index < length && !isIndexSeparator(text[index])) index++
            if (index > start) tokens += text.substring(start, index)
        }
        return tokens
    }

    /** One character the index's tokenizer breaks on: an ASCII non-alphanumeric. */
    private fun isIndexSeparator(character: Char): Boolean =
        character.code < 0x80 && !character.isLetterOrDigit()
}
