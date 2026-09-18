package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.core.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/**
 * The read-only content database. It is copied once from the app's assets
 * into private storage, named by the content version, and opened read-only.
 * A new content version replaces the old copy on the next launch.
 */
class ContentDatabase private constructor(private val database: SQLiteDatabase) {

    fun surahs(): List<Surah> =
        database.rawQuery(
            "SELECT number, name_arabic, name_simple, name_latin, revelation_place, verses_count " +
                "FROM surah ORDER BY number",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        Surah(
                            number = cursor.getInt(0),
                            nameArabic = cursor.getString(1),
                            nameSimple = cursor.getString(2),
                            nameLatin = cursor.getString(3),
                            revelationPlace = cursor.getString(4),
                            versesCount = cursor.getInt(5),
                        ),
                    )
                }
            }
        }

    fun surah(number: Int): Surah? =
        database.rawQuery(
            "SELECT number, name_arabic, name_simple, name_latin, revelation_place, verses_count " +
                "FROM surah WHERE number = ?",
            arrayOf(number.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            Surah(
                number = cursor.getInt(0),
                nameArabic = cursor.getString(1),
                nameSimple = cursor.getString(2),
                nameLatin = cursor.getString(3),
                revelationPlace = cursor.getString(4),
                versesCount = cursor.getInt(5),
            )
        }

    fun pageLines(page: Int): List<PageLine> =
        database.rawQuery(
            "SELECT line, type, centered, first_word_id, last_word_id, surah FROM page_line " +
                "WHERE page = ? ORDER BY line",
            arrayOf(page.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        PageLine(
                            line = cursor.getInt(0),
                            type = cursor.getString(1),
                            centered = cursor.getInt(2) == 1,
                            firstWordId = cursor.getInt(3),
                            lastWordId = cursor.getInt(4),
                            surah = cursor.getInt(5),
                        ),
                    )
                }
            }
        }

    fun words(firstId: Int, lastId: Int): List<Word> =
        database.rawQuery(
            "SELECT id, position, text, glyph, translation FROM word WHERE id BETWEEN ? AND ? ORDER BY id",
            arrayOf(firstId.toString(), lastId.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        Word(
                            id = cursor.getInt(0),
                            position = cursor.getInt(1),
                            text = cursor.getString(2),
                            glyph = cursor.getString(3),
                            translation = if (cursor.isNull(4)) null else cursor.getString(4),
                        ),
                    )
                }
            }
        }

    fun ayahsForPage(page: Int): List<Ayah> =
        database.rawQuery(
            "SELECT number, surah, ayah, verse_key, text FROM ayah WHERE page = ? ORDER BY number",
            arrayOf(page.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        Ayah(
                            number = cursor.getInt(0),
                            surah = cursor.getInt(1),
                            ayah = cursor.getInt(2),
                            verseKey = cursor.getString(3),
                            text = cursor.getString(4),
                        ),
                    )
                }
            }
        }

    fun translations(ayahNumbers: List<Int>): Map<Int, TranslationText> {
        if (ayahNumbers.isEmpty()) return emptyMap()
        val placeholders = ayahNumbers.joinToString(",") { "?" }
        return database.rawQuery(
            "SELECT ayah_number, text, footnotes FROM translation WHERE ayah_number IN ($placeholders)",
            ayahNumbers.map { it.toString() }.toTypedArray(),
        ).use { cursor ->
            val out = HashMap<Int, TranslationText>(ayahNumbers.size)
            while (cursor.moveToNext()) {
                out[cursor.getInt(0)] = TranslationText(
                    text = cursor.getString(1),
                    footnotes = footnotes(cursor.getString(2)),
                )
            }
            out
        }
    }

    private fun footnotes(json: String?): List<Footnote> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val number = entry.optString("n").toIntOrNull() ?: continue
                add(Footnote(number = number, text = entry.optString("text")))
            }
        }
    }

    fun wordMeanings(ayahNumber: Int): List<WordMeaning> =
        database.rawQuery(
            "SELECT text, translation FROM word WHERE ayah_number = ? AND marker = 0 ORDER BY position",
            arrayOf(ayahNumber.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        WordMeaning(
                            word = cursor.getString(0),
                            meaning = cursor.getString(1)?.trim()?.takeIf { it.isNotEmpty() },
                        ),
                    )
                }
            }
        }

    /** The ayahs behind a list of numbers, each with the page it lives on. */
    fun ayahsWithPages(numbers: List<Int>): List<AyahLocation> {
        if (numbers.isEmpty()) return emptyList()
        val inClause = numbers.joinToString(",")
        return database.rawQuery(
            "SELECT number, surah, ayah, verse_key, text, page FROM ayah " +
                "WHERE number IN ($inClause) ORDER BY number",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        AyahLocation(
                            ayah = Ayah(
                                number = cursor.getInt(0),
                                surah = cursor.getInt(1),
                                ayah = cursor.getInt(2),
                                verseKey = cursor.getString(3),
                                text = cursor.getString(4),
                            ),
                            page = cursor.getInt(5),
                        ),
                    )
                }
            }
        }
    }

    fun tafsir(ayahNumber: Int, source: String): TafsirPassage? =
        database.rawQuery(
            "SELECT p.source, p.surah, p.from_ayah, p.to_ayah, p.text FROM tafsir_ayah a " +
                "JOIN tafsir_passage p ON p.source = a.source AND p.source_id = a.passage_id " +
                "WHERE a.source = ? AND a.ayah_number = ?",
            arrayOf(source, ayahNumber.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            TafsirPassage(
                source = cursor.getString(0),
                surah = cursor.getInt(1),
                fromAyah = cursor.getInt(2),
                toAyah = cursor.getInt(3),
                text = cursor.getString(4),
            )
        }

    /**
     * One search across what is indexed: the Arabic text (SQL, on the
     * normalized column), the translation (folded in memory so "allah" finds
     * "Allāh"), and the surah names. Results stay in Mushaf order.
     */
    fun search(query: SearchQuery, limit: Int): List<SearchHit> {
        val hits = mutableListOf<SearchHit>()
        if (!query.arabic) hits += surahHits(query.terms)
        hits += if (query.arabic) arabicHits(query.terms, limit) else englishHits(query.terms, limit)
        return hits
    }

    private data class HitRow(
        val number: Int,
        val surah: Int,
        val ayah: Int,
        val verseKey: String,
        val text: String,
        val page: Int,
        val translation: String?,
    )

    private fun arabicHits(terms: List<String>, limit: Int): List<SearchHit.AyahHit> {
        val condition = terms.joinToString(" AND ") { "a.text_search LIKE ? ESCAPE '\\'" }
        val rows = readHits(
            "FROM ayah a LEFT JOIN translation t ON t.ayah_number = a.number WHERE $condition " +
                "ORDER BY a.number LIMIT ?",
            terms.map { Search.pattern(it) } + limit.toString(),
        )
        val matched = matchedWords(rows.map { it.number }, terms)
        val words = wordsByAyah(rows.map { it.number })
        return rows.map { row ->
            SearchHit.AyahHit(
                ayah = row.ayah(),
                page = row.page,
                translation = row.translation,
                words = words[row.number].orEmpty(),
                matchedPositions = matched[row.number].orEmpty(),
            )
        }
    }

    /**
     * The whole translation, folded once per session: about a megabyte, and
     * the price of matching a reader's plain typing against a scholar's
     * transliteration. Rebuilt with the database, not with each query.
     */
    private val englishIndex: List<Pair<Int, String>> by lazy {
        database.rawQuery(
            "SELECT ayah_number, text FROM translation ORDER BY ayah_number",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(cursor.getInt(0) to Search.normalizeEnglish(cursor.getString(1)))
                }
            }
        }
    }

    private val surahIndex: List<Surah> by lazy { surahs() }

    private fun englishHits(terms: List<String>, limit: Int): List<SearchHit.AyahHit> {
        val numbers = englishIndex.asSequence()
            .filter { (_, text) -> terms.all { text.contains(it) } }
            .map { (number, _) -> number }
            .take(limit)
            .toList()
        if (numbers.isEmpty()) return emptyList()
        val rows = readHits(
            "FROM ayah a JOIN translation t ON t.ayah_number = a.number " +
                "WHERE a.number IN (${numbers.joinToString(",")}) ORDER BY a.number",
            emptyList(),
        )
        return rows.map { row ->
            SearchHit.AyahHit(
                ayah = row.ayah(),
                page = row.page,
                translation = row.translation,
                words = emptyList(),
                matchedPositions = emptySet(),
            )
        }
    }

    private fun readHits(fromWhere: String, args: List<String>): List<HitRow> =
        database.rawQuery(
            "SELECT a.number, a.surah, a.ayah, a.verse_key, a.text, a.page, t.text $fromWhere",
            args.toTypedArray(),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        HitRow(
                            number = cursor.getInt(0),
                            surah = cursor.getInt(1),
                            ayah = cursor.getInt(2),
                            verseKey = cursor.getString(3),
                            text = cursor.getString(4),
                            page = cursor.getInt(5),
                            translation = cursor.getString(6),
                        ),
                    )
                }
            }
        }

    private fun HitRow.ayah() = Ayah(
        number = number,
        surah = surah,
        ayah = ayah,
        verseKey = verseKey,
        text = text,
    )

    private fun surahHits(terms: List<String>): List<SearchHit.SurahHit> =
        surahIndex.asSequence()
            .filter { surah ->
                val simple = Search.normalizeEnglish(surah.nameSimple)
                val latin = Search.normalizeEnglish(surah.nameLatin)
                terms.all { term -> simple.contains(term) || latin.contains(term) }
            }
            .take(8)
            .map { SearchHit.SurahHit(it) }
            .toList()

    private fun matchedWords(numbers: List<Int>, terms: List<String>): Map<Int, Set<Int>> {
        if (numbers.isEmpty()) return emptyMap()
        val inClause = numbers.joinToString(",")
        val condition = terms.joinToString(" OR ") { "text_search LIKE ? ESCAPE '\\'" }
        val args = numbers.map { it.toString() } + terms.map { Search.pattern(it) }
        return database.rawQuery(
            "SELECT ayah_number, position FROM word WHERE ayah_number IN ($inClause) AND ($condition)",
            args.toTypedArray(),
        ).use { cursor ->
            val out = HashMap<Int, MutableSet<Int>>()
            while (cursor.moveToNext()) {
                out.getOrPut(cursor.getInt(0)) { mutableSetOf() }.add(cursor.getInt(1))
            }
            out
        }
    }

    private fun wordsByAyah(numbers: List<Int>): Map<Int, List<Word>> {
        if (numbers.isEmpty()) return emptyMap()
        val inClause = numbers.joinToString(",")
        return database.rawQuery(
            "SELECT ayah_number, id, position, text, glyph, translation FROM word " +
                "WHERE ayah_number IN ($inClause) AND marker = 0 ORDER BY ayah_number, position",
            numbers.map { it.toString() }.toTypedArray(),
        ).use { cursor ->
            val out = HashMap<Int, MutableList<Word>>()
            while (cursor.moveToNext()) {
                out.getOrPut(cursor.getInt(0)) { mutableListOf() }.add(
                    Word(
                        id = cursor.getInt(1),
                        position = cursor.getInt(2),
                        text = cursor.getString(3),
                        glyph = cursor.getString(4),
                        translation = if (cursor.isNull(5)) null else cursor.getString(5),
                    ),
                )
            }
            out
        }
    }

    fun pagePosition(page: Int): PagePosition? =
        database.rawQuery(
            "SELECT surah, juz, hizb FROM ayah WHERE page = ? ORDER BY number LIMIT 1",
            arrayOf(page.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            PagePosition(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2))
        }

    fun firstPageOfSurah(surah: Int): Int =
        database.rawQuery("SELECT MIN(page) FROM ayah WHERE surah = ?", arrayOf(surah.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 1 }

    fun close() = database.close()

    companion object {
        suspend fun open(context: Context): ContentDatabase = withContext(Dispatchers.IO) {
            val version = context.assets.open("content/version.txt")
                .bufferedReader().use { it.readText().trim() }
            val directory = File(context.filesDir, "content").apply { mkdirs() }
            val target = File(directory, "quran-$version.db")
            if (!target.exists() || target.length() == 0L) {
                val temporary = File(directory, "quran-$version.db.part")
                context.assets.open("content/quran.db").use { input ->
                    temporary.outputStream().buffered().use { output -> input.copyTo(output) }
                }
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
                directory.listFiles { file -> file.name != target.name }?.forEach { it.delete() }
            }
            ContentDatabase(
                SQLiteDatabase.openDatabase(
                    target.path,
                    null,
                    SQLiteDatabase.OPEN_READONLY,
                ),
            )
        }
    }
}
