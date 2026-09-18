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
 *
 * Content is read through packs: a translation, a tafsir, or a word list is
 * chosen by pack id, so a second pack is another row, never another schema.
 */
class ContentDatabase private constructor(private val database: SQLiteDatabase) {

    // ----------------------------------------------------------------- packs

    fun packs(): List<ContentPack> =
        database.rawQuery(
            "SELECT id, type, name, language, credit, license, version, builtin, ayahs, bytes " +
                "FROM pack ORDER BY type, id",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        ContentPack(
                            id = cursor.getString(0),
                            type = PackType.of(cursor.getString(1)),
                            name = cursor.getString(2),
                            language = cursor.getString(3),
                            credit = cursor.getString(4),
                            license = cursor.getString(5),
                            version = cursor.getString(6),
                            builtIn = cursor.getInt(7) == 1,
                            ayahs = cursor.getInt(8),
                            bytes = cursor.getLong(9),
                        ),
                    )
                }
            }
        }

    // --------------------------------------------------------------- reading

    fun surahs(): List<Surah> =
        database.rawQuery(
            "SELECT number, name_arabic, name_simple, name_latin, revelation_place, verses_count, " +
                "bismillah_pre FROM surah ORDER BY number",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(surahOf(cursor))
                }
            }
        }

    fun surah(number: Int): Surah? =
        database.rawQuery(
            "SELECT number, name_arabic, name_simple, name_latin, revelation_place, verses_count, " +
                "bismillah_pre FROM surah WHERE number = ?",
            arrayOf(number.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            surahOf(cursor)
        }

    private fun surahOf(cursor: android.database.Cursor) = Surah(
        number = cursor.getInt(0),
        nameArabic = cursor.getString(1),
        nameSimple = cursor.getString(2),
        nameLatin = cursor.getString(3),
        revelationPlace = cursor.getString(4),
        versesCount = cursor.getInt(5),
        bismillahPre = cursor.getInt(6) == 1,
    )

    /** The English introduction to a surah, when the content carries one. */
    fun surahInfo(surah: Int): String? =
        database.rawQuery("SELECT text FROM surah_info WHERE surah = ?", arrayOf(surah.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

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
                    add(wordOf(cursor, offset = 0))
                }
            }
        }

    /** Every glyph on one page, in reading order, markers included. */
    fun pageWords(page: Int): List<PageWord> =
        database.rawQuery(
            "SELECT id, ayah_number, position, marker, glyph FROM word " +
                "WHERE page = ? ORDER BY line, line_position",
            arrayOf(page.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        PageWord(
                            id = cursor.getInt(0),
                            ayah = cursor.getInt(1),
                            position = cursor.getInt(2),
                            marker = cursor.getInt(3) == 1,
                            glyph = cursor.getString(4),
                        ),
                    )
                }
            }
        }

    fun firstAyahOfPage(page: Int): Int =
        database.rawQuery("SELECT MIN(number) FROM ayah WHERE page = ?", arrayOf(page.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 1 }

    fun pageOfAyah(number: Int): Int =
        database.rawQuery("SELECT page FROM ayah WHERE number = ?", arrayOf(number.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 1 }

    private fun wordOf(cursor: android.database.Cursor, offset: Int) = Word(
        id = cursor.getInt(offset + 0),
        position = cursor.getInt(offset + 1),
        text = cursor.getString(offset + 2),
        glyph = cursor.getString(offset + 3),
        translation = if (cursor.isNull(offset + 4)) null else cursor.getString(offset + 4),
    )

    /**
     * The basmallah glyphs, borrowed from Al-Fatihah's first ayah. The page
     * fonts carry no basmallah line of their own; the same four words open
     * every surah, so they are drawn with the same page font.
     */
    fun basmallahGlyphs(): String =
        database.rawQuery(
            "SELECT glyph FROM word WHERE ayah_number = 1 AND marker = 0 ORDER BY position",
            null,
        ).use { cursor ->
            buildString {
                while (cursor.moveToNext()) append(cursor.getString(0))
            }
        }

    fun ayahsForPage(page: Int): List<Ayah> =
        database.rawQuery(
            "SELECT number, surah, ayah, verse_key, text FROM ayah WHERE page = ? ORDER BY number",
            arrayOf(page.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) add(ayahOf(cursor))
            }
        }

    fun ayah(number: Int): Ayah? =
        database.rawQuery(
            "SELECT number, surah, ayah, verse_key, text FROM ayah WHERE number = ?",
            arrayOf(number.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) ayahOf(cursor) else null }

    private fun ayahOf(cursor: android.database.Cursor) = Ayah(
        number = cursor.getInt(0),
        surah = cursor.getInt(1),
        ayah = cursor.getInt(2),
        verseKey = cursor.getString(3),
        text = cursor.getString(4),
    )

    /** Every ayah's place, without its text: the study list's skeleton. */
    fun ayahHeaders(): List<AyahHeader> =
        database.rawQuery(
            "SELECT number, surah, ayah, verse_key, page, juz FROM ayah ORDER BY number",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        AyahHeader(
                            number = cursor.getInt(0),
                            surah = cursor.getInt(1),
                            ayah = cursor.getInt(2),
                            verseKey = cursor.getString(3),
                            page = cursor.getInt(4),
                            juz = cursor.getInt(5),
                        ),
                    )
                }
            }
        }

    fun translations(ayahNumbers: List<Int>, pack: String): Map<Int, TranslationText> {
        if (ayahNumbers.isEmpty()) return emptyMap()
        val placeholders = ayahNumbers.joinToString(",") { "?" }
        val args = (listOf(pack) + ayahNumbers.map { it.toString() }).toTypedArray()
        return database.rawQuery(
            "SELECT ayah_number, text, footnotes FROM translation WHERE pack = ? AND ayah_number IN ($placeholders)",
            args,
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

    /** The words of a set of ayahs, in order, markers excluded. */
    fun wordsForAyahs(numbers: List<Int>): Map<Int, List<Word>> {
        if (numbers.isEmpty()) return emptyMap()
        val inClause = numbers.joinToString(",")
        return database.rawQuery(
            "SELECT ayah_number, id, position, text, glyph, translation FROM word " +
                "WHERE ayah_number IN ($inClause) AND marker = 0 ORDER BY ayah_number, position",
            null,
        ).use { cursor ->
            val out = HashMap<Int, MutableList<Word>>()
            while (cursor.moveToNext()) {
                out.getOrPut(cursor.getInt(0)) { mutableListOf() }.add(wordOf(cursor, offset = 1))
            }
            out
        }
    }

    fun tafsir(ayahNumber: Int, pack: String): TafsirPassage? =
        database.rawQuery(
            "SELECT p.pack, p.surah, p.from_ayah, p.to_ayah, p.text FROM tafsir_ayah a " +
                "JOIN tafsir_passage p ON p.pack = a.pack AND p.source_id = a.passage_id " +
                "WHERE a.pack = ? AND a.ayah_number = ?",
            arrayOf(pack, ayahNumber.toString()),
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
     * Builds the tafsir search index ahead of the first query. Called after
     * the first page is on screen, on a worker thread, so the reader's first
     * search never pays for it.
     */
    fun prewarmSearch(tafsirPacks: List<String>) {
        synchronized(tafsirIndex) { tafsirIndex.ensure(tafsirPacks.toSet()) }
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

    fun firstAyahOfSurah(surah: Int): Int =
        database.rawQuery("SELECT MIN(number) FROM ayah WHERE surah = ?", arrayOf(surah.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 1 }

    /** The first ayah of each juz, for the juz list. */
    fun juzStarts(): List<Int> =
        database.rawQuery(
            "SELECT MIN(number) FROM ayah GROUP BY juz ORDER BY juz",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) add(cursor.getInt(0))
            }
        }

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
                            ayah = ayahOf(cursor),
                            page = cursor.getInt(5),
                        ),
                    )
                }
            }
        }
    }

    fun ayahsOfSurah(surah: Int): List<Ayah> =
        database.rawQuery(
            "SELECT number, surah, ayah, verse_key, text FROM ayah WHERE surah = ? ORDER BY ayah",
            arrayOf(surah.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) add(ayahOf(cursor))
            }
        }

    // -------------------------------------------------------------- reciters

    fun recitations(): List<Recitation> =
        database.rawQuery(
            "SELECT id, name, credit FROM recitation ORDER BY id",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        Recitation(
                            id = cursor.getString(0),
                            name = cursor.getString(1),
                            credit = cursor.getString(2),
                        ),
                    )
                }
            }
        }

    fun recitationAyah(recitation: String, ayahNumber: Int): RecitationAyah? =
        database.rawQuery(
            "SELECT audio_path, segments FROM recitation_ayah " +
                "WHERE recitation = ? AND ayah_number = ?",
            arrayOf(recitation, ayahNumber.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            RecitationAyah(
                audioPath = cursor.getString(0),
                segments = segments(cursor.getString(1)),
            )
        }

    private fun segments(json: String?): List<WordSegment> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val entry = array.optJSONArray(index) ?: continue
                if (entry.length() < 4) continue
                add(
                    WordSegment(
                        wordFrom = entry.optInt(0),
                        wordTo = entry.optInt(1),
                        startMs = entry.optLong(2),
                        endMs = entry.optLong(3),
                    ),
                )
            }
        }
    }

    // --------------------------------------------------------------- search

    /**
     * One pass over everything the reader enabled: the Arabic text, the
     * translation packs, the tafsir packs, the word meanings, the surah
     * names, and references typed as numbers. Every source is a prepared
     * statement against an index column shaped by the same normalizer the
     * query went through, so results are exact and no index has to be built
     * at search time.
     */
    fun search(request: SearchRequest): SearchResults {
        val query = request.query
        val limit = request.limit
        val leading = ArrayList<SearchHit>(9)
        val counts = intArrayOf(0, 0, 0, 0, 0) // arabic, translation, tafsir, words, surahs

        val reference = Search.reference(query.input)
        if (reference != null) {
            val number = referenceAyahNumber(reference)
            if (number != null) {
                ayahWithPage(number)?.let { leading += SearchHit.ReferenceHit(it.first, it.second) }
            }
        }

        leading += surahHits(query).also { counts[4] = it.size }

        // The Arabic text, matched on the normalized column.
        val arabicNumbers = arabicMatches(query, limit).also { counts[0] = it.size }
        val arabicWords = matchedWords(arabicNumbers, query.terms)

        // Word meanings, English only, a single word finding its ayahs.
        val wordMatches = wordMeaningMatches(query, limit)
        counts[3] = wordMatches.size

        // Translations: the enabled packs, each with exact highlight ranges.
        val translationPacks = request.translationPacks
        val translationByNumber = LinkedHashMap<Int, String>()
        if (!query.arabic && translationPacks.isNotEmpty()) {
            for (pack in translationPacks) {
                val numbers = packMatches("translation", pack, query.terms, limit)
                counts[1] += numbers.size
                for (number in numbers) translationByNumber.putIfAbsent(number, pack)
            }
        }

        // One read for every ayah any source touched, instead of one read per
        // row: a search that matched two hundred ayahs must not cost two
        // hundred queries.
        val wanted = LinkedHashSet<Int>(arabicNumbers.size + wordMatches.size + translationByNumber.size)
        wanted += arabicNumbers
        wanted += wordMatches.keys
        wanted += translationByNumber.keys
        val rows = loadRows(wanted)
        for ((number, matched) in arabicWords) {
            rows[number]?.matchedWords = matched
        }
        for ((number, meaning) in wordMatches) {
            rows[number]?.meaning = meaning
        }
        for ((number, pack) in translationByNumber) {
            val row = rows[number] ?: continue
            val translation = rowTranslation(number, pack) ?: continue
            row.translation = TranslationHit(
                pack = pack,
                packName = request.packNames[pack] ?: pack,
                text = translation.text,
                ranges = Search.matchRanges(translation.text, query.terms, arabic = false),
                footnotes = translation.footnotes,
            )
        }

        // Tafsir: each passage that matched, shown with its own range. The
        // index holds the enabled packs, folded, so this stays a memory scan,
        // and the passages and their ayahs are read in two batched queries.
        val tafsirHits = ArrayList<SearchHit.TafsirHitResult>()
        synchronized(tafsirIndex) { tafsirIndex.ensure(request.tafsirPacks.toSet()) }
        val passageAyahs = LinkedHashMap<String, Int>()
        val matchedPassages = ArrayList<Pair<String, PassageRow>>()
        for (pack in request.tafsirPacks) {
            val passages = tafsirMatches(pack, query, limit)
            counts[2] += passages.size
            for (passage in passages) {
                val number = firstAyahOfPassage(passage.surah, passage.fromAyah) ?: continue
                passageAyahs[passage.key] = number
                matchedPassages += pack to passage
            }
        }
        val passageRows = loadRows(passageAyahs.values.toList())
        for ((pack, passage) in matchedPassages) {
            val number = passageAyahs[passage.key] ?: continue
            val row = passageRows[number] ?: continue
            val language = request.packLanguages[pack]
            val (excerpt, ranges) = Search.excerpt(
                passage.text,
                query.terms,
                arabic = language == "ar",
                window = 200,
            )
            tafsirHits += SearchHit.TafsirHitResult(
                pack = pack,
                packName = request.packNames[pack] ?: pack,
                surah = passage.surah,
                surahName = surahCache[passage.surah]?.nameSimple ?: "Surah ${passage.surah}",
                fromAyah = passage.fromAyah,
                toAyah = passage.toAyah,
                ayah = row.ayah,
                page = row.page,
                text = excerpt,
                ranges = ranges,
            )
        }

        val ayahHits = rows.values.map { row ->
            SearchHit.AyahHit(
                ayah = row.ayah,
                page = row.page,
                arabicMatchedWords = row.matchedWords,
                translation = row.translation,
                wordMeaning = row.meaning,
            )
        }
        val merged = ArrayList<SearchHit>(leading.size + ayahHits.size + tafsirHits.size)
        merged += leading
        merged += (ayahHits + tafsirHits).sortedWith(
            compareBy({ hitAyahNumber(it) }, { if (it is SearchHit.TafsirHitResult) 1 else 0 }),
        )
        // A reader wants one row per passage, not one per ayah it covers.
        val deduped = ArrayList<SearchHit>(merged.size)
        var lastTafsir: SearchHit.TafsirHitResult? = null
        for (hit in merged) {
            if (hit is SearchHit.TafsirHitResult) {
                if (lastTafsir != null && lastTafsir.pack == hit.pack &&
                    lastTafsir.fromAyah == hit.fromAyah && lastTafsir.surah == hit.surah
                ) {
                    continue
                }
                lastTafsir = hit
            }
            deduped += hit
        }
        val capped = deduped.size > limit
        return SearchResults(
            hits = deduped.take(limit),
            counts = SearchCounts(counts[0], counts[1], counts[2], counts[3], counts[4]),
            capped = capped,
        )
    }

    private class Row(val ayah: Ayah, val page: Int) {
        var matchedWords: Set<Int> = emptySet()
        var translation: TranslationHit? = null
        var meaning: String? = null
    }

    private fun hitAyahNumber(hit: SearchHit): Int = when (hit) {
        is SearchHit.ReferenceHit -> hit.ayah.number
        is SearchHit.AyahHit -> hit.ayah.number
        is SearchHit.SurahHit -> 0
        is SearchHit.TafsirHitResult -> hit.ayah.number
    }

    private fun referenceAyahNumber(reference: Search.Reference): Int? {
        if (reference.surah !in 1..114) return null
        val first = firstAyahCache[reference.surah] ?: return null
        val ayah = reference.ayah ?: return first
        val surah = surahCache[reference.surah] ?: return null
        if (ayah > surah.versesCount) return null
        return first + ayah - 1
    }

    private fun firstAyahOfPassage(surah: Int, ayah: Int): Int? {
        val surahRow = surahCache[surah] ?: return null
        if (ayah !in 1..surahRow.versesCount) return null
        val first = firstAyahCache[surah] ?: return null
        return first + ayah - 1
    }

    private fun ayahWithPage(number: Int): Pair<Ayah, Int>? =
        database.rawQuery(
            "SELECT number, surah, ayah, verse_key, text, page FROM ayah WHERE number = ?",
            arrayOf(number.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else ayahOf(cursor) to cursor.getInt(5)
        }

    private fun arabicMatches(query: SearchQuery, limit: Int): List<Int> {
        val condition = query.terms.joinToString(" AND ") { "text_search LIKE ? ESCAPE '\\'" }
        return database.rawQuery(
            "SELECT number FROM ayah WHERE $condition ORDER BY number LIMIT ?",
            (query.terms.map { Search.pattern(it) } + limit.toString()).toTypedArray(),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) add(cursor.getInt(0))
            }
        }
    }

    private fun matchedWords(numbers: List<Int>, terms: List<String>): Map<Int, Set<Int>> {
        if (numbers.isEmpty()) return emptyMap()
        val inClause = numbers.joinToString(",")
        val condition = terms.joinToString(" OR ") { "text_search LIKE ? ESCAPE '\\'" }
        return database.rawQuery(
            "SELECT ayah_number, position FROM word WHERE ayah_number IN ($inClause) AND ($condition)",
            terms.map { Search.pattern(it) }.toTypedArray(),
        ).use { cursor ->
            val out = HashMap<Int, MutableSet<Int>>()
            while (cursor.moveToNext()) {
                out.getOrPut(cursor.getInt(0)) { mutableSetOf() }.add(cursor.getInt(1))
            }
            out
        }
    }

    private fun wordMeaningMatches(query: SearchQuery, limit: Int): Map<Int, String> {
        if (query.arabic) return emptyMap()
        val condition = query.terms.joinToString(" AND ") { "translation_search LIKE ? ESCAPE '\\'" }
        return database.rawQuery(
            "SELECT ayah_number, translation FROM word WHERE marker = 0 AND $condition " +
                "ORDER BY ayah_number LIMIT ?",
            (query.terms.map { Search.pattern(it) } + (limit * 4).toString()).toTypedArray(),
        ).use { cursor ->
            val out = LinkedHashMap<Int, String>()
            while (cursor.moveToNext()) {
                val number = cursor.getInt(0)
                val meaning = cursor.getString(1)?.trim().orEmpty()
                if (meaning.isNotEmpty() && out.size < limit) out.putIfAbsent(number, meaning)
            }
            out
        }
    }

    private fun packMatches(table: String, pack: String, terms: List<String>, limit: Int): List<Int> {
        val condition = terms.joinToString(" AND ") { "text_search LIKE ? ESCAPE '\\'" }
        val args = (listOf(pack) + terms.map { Search.pattern(it) } + limit.toString()).toTypedArray()
        return database.rawQuery(
            "SELECT ayah_number FROM $table WHERE pack = ? AND ($condition) ORDER BY ayah_number LIMIT ?",
            args,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) add(cursor.getInt(0))
            }
        }
    }

    private fun translationMatches(number: Int, pack: String, terms: List<String>): Boolean {
        val condition = terms.joinToString(" AND ") { "text_search LIKE ? ESCAPE '\\'" }
        val args = (listOf(pack, number.toString()) + terms.map { Search.pattern(it) }).toTypedArray()
        return database.rawQuery(
            "SELECT 1 FROM translation WHERE pack = ? AND ayah_number = ? AND ($condition)",
            args,
        ).use { it.moveToFirst() }
    }

    private fun translation(number: Int, pack: String): TranslationText? =
        translations(listOf(number), pack)[number]

    private fun loadRows(numbers: Collection<Int>): Map<Int, Row> {
        if (numbers.isEmpty()) return emptyMap()
        val out = LinkedHashMap<Int, Row>(numbers.size)
        for (chunk in numbers.toList().chunked(400)) {
            val placeholders = chunk.joinToString(",") { "?" }
            database.rawQuery(
                "SELECT number, surah, ayah, verse_key, text, page FROM ayah " +
                    "WHERE number IN ($placeholders) ORDER BY number",
                chunk.map { it.toString() }.toTypedArray(),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val ayah = ayahOf(cursor)
                    out[ayah.number] = Row(ayah, cursor.getInt(5))
                }
            }
        }
        return out
    }

    private fun rowTranslation(number: Int, pack: String): TranslationText? {
        val found = translations(listOf(number), pack)
        return found[number]
    }

    private data class PassageRow(
        val sourceId: Int,
        val surah: Int,
        val fromAyah: Int,
        val toAyah: Int,
        val text: String,
    ) {
        val key: String get() = "$surah:$fromAyah"
    }

    /**
     * Every tafsir passage of the enabled packs, folded once and held in
     * memory. A tafsir can be tens of megabytes of prose, and asking SQLite
     * to scan it with LIKE on every keystroke costs seconds; a folded copy
     * costs a few megabytes and turns a search into a substring scan.
     *
     * The index is built from the `text_search` column, which the content
     * build already folded, so building it is one query and no processing.
     */
    private data class TafsirEntry(
        val pack: String,
        val sourceId: Int,
        val surah: Int,
        val fromAyah: Int,
        val toAyah: Int,
        val folded: String,
    )

    private inner class TafsirIndex {
        private var packs: Set<String> = emptySet()
        private var entries: List<TafsirEntry> = emptyList()

        fun ensure(wanted: Set<String>) {
            if (wanted == packs && entries.isNotEmpty()) return
            val placeholders = wanted.joinToString(",") { "?" }
            entries = if (wanted.isEmpty()) {
                emptyList()
            } else {
                database.rawQuery(
                    "SELECT pack, source_id, surah, from_ayah, to_ayah, text_search " +
                        "FROM tafsir_passage WHERE pack IN ($placeholders)",
                    wanted.toTypedArray(),
                ).use { cursor ->
                    buildList(cursor.count) {
                        while (cursor.moveToNext()) {
                            add(
                                TafsirEntry(
                                    pack = cursor.getString(0),
                                    sourceId = cursor.getInt(1),
                                    surah = cursor.getInt(2),
                                    fromAyah = cursor.getInt(3),
                                    toAyah = cursor.getInt(4),
                                    folded = cursor.getString(5) ?: "",
                                ),
                            )
                        }
                    }
                }
            }
            packs = wanted
        }

        fun matches(terms: List<String>, limit: Int): List<TafsirEntry> {
            if (entries.isEmpty() || terms.isEmpty()) return emptyList()
            val found = ArrayList<TafsirEntry>(limit)
            for (entry in entries) {
                var hit = true
                for (term in terms) {
                    if (!entry.folded.contains(term)) {
                        hit = false
                        break
                    }
                }
                if (hit) {
                    found += entry
                    if (found.size >= limit) break
                }
            }
            return found.sortedWith(compareBy({ it.surah }, { it.fromAyah }))
        }
    }

    private val tafsirIndex = TafsirIndex()

    /** The 114 surahs, read once: search resolves passages against them. */
    private val surahCache: Map<Int, Surah> by lazy { surahs().associateBy { it.number } }

    private val firstAyahCache: Map<Int, Int> by lazy {
        surahCache.keys.associateWith { firstAyahOfSurah(it) }
    }

    private fun tafsirMatches(pack: String, query: SearchQuery, limit: Int): List<PassageRow> {
        val matches = synchronized(tafsirIndex) {
            tafsirIndex.matches(query.terms, limit)
        }
        val mine = matches.filter { it.pack == pack }
        if (mine.isEmpty()) return emptyList()
        val texts = passageTexts(pack, mine.map { it.sourceId })
        return mine.mapNotNull { entry ->
            val text = texts[entry.sourceId] ?: return@mapNotNull null
            PassageRow(
                sourceId = entry.sourceId,
                surah = entry.surah,
                fromAyah = entry.fromAyah,
                toAyah = entry.toAyah,
                text = text,
            )
        }
    }

    /** The originals of the matched passages, read in one query per pack. */
    private fun passageTexts(pack: String, sourceIds: List<Int>): Map<Int, String> {
        if (sourceIds.isEmpty()) return emptyMap()
        val out = HashMap<Int, String>(sourceIds.size)
        for (chunk in sourceIds.chunked(400)) {
            val placeholders = chunk.joinToString(",") { "?" }
            database.rawQuery(
                "SELECT source_id, text FROM tafsir_passage WHERE pack = ? AND source_id IN ($placeholders)",
                (listOf(pack) + chunk.map { it.toString() }).toTypedArray(),
            ).use { cursor ->
                while (cursor.moveToNext()) out[cursor.getInt(0)] = cursor.getString(1)
            }
        }
        return out
    }

    private fun surahHits(query: SearchQuery): List<SearchHit.SurahHit> =
        surahCache.values.asSequence()
            .filter { surah ->
                val simple = Search.normalizeForIndex(surah.nameSimple)
                val latin = Search.normalizeForIndex(surah.nameLatin)
                val arabicName = Search.normalizeForIndex(surah.nameArabic)
                val numberMatch = query.terms.size == 1 && query.terms.first() == surah.number.toString()
                numberMatch || query.terms.all { term ->
                    simple.contains(term) || latin.contains(term) || arabicName.contains(term)
                }
            }
            .take(8)
            .map { SearchHit.SurahHit(it) }
            .toList()

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
