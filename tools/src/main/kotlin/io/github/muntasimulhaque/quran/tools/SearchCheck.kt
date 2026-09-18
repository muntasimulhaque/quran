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
 */
class SearchCheck(private val root: File) {

    private val problems = mutableListOf<String>()

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
        auditPlainWords(translations)

        if (problems.isEmpty()) {
            println("search: $arabicChecked arabic round trips passed")
            println("search: ${nonAscii.size} distinct non-ASCII translation codepoints fold cleanly")
            println("search: plain english words match")
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
