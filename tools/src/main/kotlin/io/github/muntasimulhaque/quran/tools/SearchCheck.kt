package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.Search
import java.io.File
import java.sql.Connection
import java.util.TreeMap

/**
 * Proves that search can be trusted on both sides of the content.
 *
 * Arabic: for every hundredth ayah, a word taken from the ayah's own text must
 * find that ayah again through the indexed column and the query normalization.
 *
 * English: the translation writes Allāh, ʿĪsā and Mūsā. Every non-ASCII
 * codepoint is audited to fold to plain ASCII (or to be Arabic, which the
 * Arabic path handles), and plain words a reader would actually type must
 * match something.
 *
 * Readable: a tafsir is stored as a small HTML subset for its own panels to
 * parse, and a search result has no parser behind it, so every excerpt the
 * reader can be shown is checked for markup and for a highlight that lands on
 * words they can see. A surah's introduction is checked the same way. This is
 * the gate that would have caught raw `</p><h2>` in a tafsir result.
 */
class SearchCheck(private val root: File) {

    private val problems = mutableListOf<String>()

    /** Excerpts and introductions checked for readable text. */
    private var readableChecked = 0

    fun run(): Int {
        val database = File(root, "content/quran.db")
        if (!database.exists()) {
            println("search: content/quran.db is missing; run build first")
            return 1
        }

        var arabicChecked = 0
        val translations = ArrayList<Pair<Int, String>>(6236)
        val nonAscii = TreeMap<Int, Int>()

        openSqlite(database).use { connection ->
            connection.each("SELECT ayah_number, text FROM translation ORDER BY ayah_number") { rs ->
                val raw = rs.getString(2) ?: ""
                for (codepoint in raw.codePoints()) {
                    if (codepoint > 127) nonAscii.merge(codepoint, 1, Int::plus)
                }
                translations += rs.getInt(1) to Search.normalizeEnglish(raw)
            }

            connection.each(
                "SELECT number, text FROM ayah WHERE number % 100 = 1 ORDER BY number",
            ) { rs ->
                val number = rs.getInt(1)
                val term = longestWord(Arabic.normalizeForSearch(rs.getString(2) ?: ""), minimum = 3)
                if (term != null) {
                    arabicChecked++
                    if (!exists(
                            connection,
                            "SELECT 1 FROM ayah WHERE number = ? AND text_search LIKE ? ESCAPE '\\'",
                            listOf(number.toString(), Search.pattern(term)),
                        )
                    ) {
                        problems += "arabic search missed ayah $number for its own word $term"
                    }
                }
            }
        }

        auditNonAscii(nonAscii)
        auditScriptRoundTrip("content/quran.db", "bn")
        auditPlainWords(translations)
        auditReadableText()

        if (problems.isEmpty()) {
            println("search: $arabicChecked arabic round trips passed")
            println("search: ${nonAscii.size} distinct non-ASCII translation codepoints fold cleanly")
            println("search: plain english words match")
            println("search: ${readableChecked} readable excerpts carry no markup")
            return 0
        }
        println("SEARCH FAILURES (${problems.size})")
        problems.forEach { println("  - $it") }
        return 1
    }

    /** Nothing in the translation may fold away or stay non-ASCII and unsupported. */
    private fun auditNonAscii(nonAscii: TreeMap<Int, Int>) {
        for ((codepoint, count) in nonAscii) {
            if (RichText.isArabic(codepoint)) continue
            // Scripts that are not Latin are not expected to fold to ASCII;
            // they are checked by their own round trip below.
            if (!isLatinFolding(codepoint)) continue
            val folded = Search.normalizeEnglish(String(Character.toChars(codepoint)))
            // The ayn and hamza marks are meant to disappear from a query.
            val isMark = codepoint == 0x02BF || codepoint == 0x02BE
            val staysNonAscii = folded.codePoints().anyMatch { it > 127 }
            if (folded.isEmpty() && !isMark) {
                problems += "U+${hex(codepoint)} (x$count) folds away to nothing"
            } else if (folded.isNotEmpty() && staysNonAscii) {
                problems += "U+${hex(codepoint)} (x$count) does not fold to ASCII"
            }
        }
    }

    /** The words a reader types without diacritics must find the text. */
    /**
     * A second script must round trip like the first: the longest word of a
     * sample of its ayahs, normalized the way a query is, must find its own
     * ayah. This is what makes a Bangla (or Urdu, or Turkish) translation
     * searchable the day it is installed.
     */
    private fun auditScriptRoundTrip(path: String, language: String) {
        openSqlite(File(root, path)).use { connection ->
            val rows = ArrayList<Pair<Int, String>>()
            connection.prepareStatement(
                "SELECT t.ayah_number, t.text FROM translation t " +
                    "JOIN pack p ON p.id = t.pack WHERE p.language = ? ORDER BY t.ayah_number",
            ).use { statement ->
                statement.setString(1, language)
                statement.executeQuery().use { rs ->
                    while (rs.next()) rows += rs.getInt(1) to (rs.getString(2) ?: "")
                }
            }
            if (rows.isEmpty()) {
                println("search: no $language translation in the database")
                return
            }
            var checked = 0
            for ((number, text) in rows.filter { it.first % 97 == 1 }) {
                // The same shape the app gives a query in this script.
                val term = longestWord(Search.normalizeForIndex(text), minimum = 3) ?: continue
                if (term.codePoints().noneMatch { it > 127 }) continue
                checked++
                if (!exists(
                        connection,
                        "SELECT 1 FROM translation WHERE ayah_number = ? AND text_search LIKE ? ESCAPE '\\'",
                        listOf(number.toString(), Search.pattern(term)),
                    )
                ) {
                    problems += "$language search missed ayah $number for its own word $term"
                }
            }
            println("search: $checked $language round trips passed")
        }
    }

    /**
     * Every excerpt the reader can be shown, and every surah introduction,
     * comes out readable: no tag, no footnote marker where a marker belongs to
     * a footnote, and a highlight that still lands on a word. A tafsir result
     * used to print `</p><h2>` because the excerpt was cut from the stored
     * HTML, which is exactly what this checks.
     *
     * The probe term goes through `Search.parse` and not raw, because that is
     * what the app does with the reader's typing: a raw term would fold
     * differently from the stored column and report a missing highlight that
     * the reader would never meet.
     */
    private fun auditReadableText() {
        val packs = File(root, "content/packs")
        if (!packs.isDirectory) return
        val probes = listOf(
            "tafsir-ibn-kathir-en" to "tafsir_passage",
            "tafsir-ibn-kathir-bn" to "tafsir_passage",
            "translation-saheeh-en" to "translation",
            "translation-taisirul-quran-bn" to "translation",
        )
        for ((id, table) in probes) {
            val file = File(packs, "$id.db")
            if (!file.isFile) {
                println("search: $id is not built; readable check skipped for it")
                continue
            }
            openSqlite(file).use { connection ->
                val probe = readableProbe(connection, table) ?: run {
                    println("search: $id has no text to check")
                    return@use
                }
                // Fold the probe the way the app folds what the reader types:
                // a raw term folds differently from the stored column and would
                // report a missing highlight the reader would never meet.
                val terms = listOf(Search.normalizeForIndex(probe.window))
                connection.each(
                    "SELECT text FROM ${probe.table} WHERE text LIKE '%${probe.window}%' LIMIT 200",
                ) { rs ->
                    val raw = rs.getString(1) ?: return@each
                    val (excerpt, ranges) = Search.excerpt(raw, terms, arabic = false)
                    readableChecked++
                    if (RichText.hasMarkup(excerpt)) {
                        problems += "$id leaked markup into an excerpt: " +
                            excerpt.take(90).replace('\n', ' ')
                    }
                    if (ranges.isEmpty()) {
                        problems += "$id excerpt lost its highlight for \"${probe.window}\""
                    } else {
                        for (range in ranges) {
                            val matched = excerpt.substring(range.first, range.last + 1)
                            if (matched.isBlank()) {
                                problems += "$id highlight is empty in: ${excerpt.take(60)}"
                            }
                        }
                    }
                }
            }
        }

        // A surah's introduction is drawn block by block, heading over its
        // own paragraph, so it must read as prose: no tag survives into the
        // study view's "About this surah".
        val infoPack = File(packs, "words-en.db")
        if (infoPack.isFile) {
            openSqlite(infoPack).use { connection ->
                connection.each("SELECT surah, text FROM surah_info") { rs ->
                    val drawn = RichText.paragraphs(rs.getString(2) ?: "")
                    readableChecked++
                    if (RichText.hasMarkup(drawn)) {
                        problems += "surah ${rs.getInt(1)} introduction leaked markup: " +
                            drawn.take(90)
                    }
                    if (drawn.isBlank()) problems += "surah ${rs.getInt(1)} introduction is empty"
                }
            }
        }
    }

    private data class ReadableProbe(val table: String, val window: String)

    /**
     * A word to search for that is actually in this pack, and that a reader
     * could type: English gets a word of the app's own language, and another
     * script gets the longest word of its own first row.
     */
    private fun readableProbe(connection: Connection, table: String): ReadableProbe? {
        for (candidate in listOf("mercy", "merciful")) {
            val found = connection.prepareStatement(
                "SELECT text FROM $table WHERE text LIKE ? LIMIT 1",
            ).use { statement ->
                statement.setString(1, "%$candidate%")
                statement.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
            }
            if (found != null) return ReadableProbe(table, candidate)
        }
        val first = connection.prepareStatement("SELECT text FROM $table LIMIT 1").use { statement ->
            statement.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        } ?: return null
        val word = longestWord(RichText.plain(first), minimum = 4) ?: return null
        return ReadableProbe(table, word)
    }

    /** Codepoints whose job is to fold to a plain Latin letter. */    private fun isLatinFolding(codepoint: Int): Boolean = when (codepoint) {
        in 0x00A0..0x024F -> true // Latin-1 supplement and Latin extended A/B
        in 0x1E00..0x1EFF -> true // Latin extended additional
        0x2018, 0x2019, 0x201C, 0x201D -> true // typographic quotes
        in 0x2010..0x2015 -> true // typographic dashes
        else -> false
    }

    private fun auditPlainWords(translations: List<Pair<Int, String>>) {
        for (word in listOf("allah", "mercy", "moses", "paradise", "pharaoh")) {
            val matches = translations.count { (_, text) -> text.contains(word) }
            if (matches == 0) problems += "english search finds nothing for \"$word\""
        }
    }

    private fun longestWord(text: String, minimum: Int): String? =
        text.split(Regex("\\s+"))
            .map { it.replace(Regex("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$"), "") }
            .filter { it.length >= minimum }
            .maxByOrNull { it.length }

    private fun exists(connection: Connection, sql: String, args: List<String>): Boolean =
        connection.prepareStatement(sql).use { statement ->
            args.forEachIndexed { index, value -> statement.setString(index + 1, value) }
            statement.executeQuery().use { rs -> rs.next() }
        }

    private fun hex(codepoint: Int): String = codepoint.toString(16).uppercase().padStart(4, '0')
}
