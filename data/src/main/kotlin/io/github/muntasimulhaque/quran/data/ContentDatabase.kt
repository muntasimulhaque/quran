package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.core.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * The read-only content database.
 *
 * The main file is the core pack: the Quran text, its words, and its page
 * layout. Every other pack (a translation, a tafsir, a word list, a reciter's
 * timings) is attached beside it as its own schema, so a reader's library is
 * exactly the packs on the device and nothing else. A pack that is not
 * installed is not attached, and a query for it never runs.
 */
class ContentDatabase private constructor(
    private val database: SQLiteDatabase,
    private val catalog: PackCatalog,
    val installedPacks: Set<String>,
) {

    /** The schema of one installed pack, quoted for SQL. */
    private fun schema(id: String): String = "\"" + id.replace("\"", "") + "\""

    /** The word list that speaks a language, when the reader has it. */
    private fun wordsPack(language: String): String = "words-$language"

    private fun has(id: String): Boolean = id in installedPacks && id != PackCatalog.CORE_ID

    fun installedPack(id: String): Boolean = id == PackCatalog.CORE_ID || has(id)

    fun catalog(): PackCatalog = catalog

    // ----------------------------------------------------------------- packs

    fun packs(): List<ContentPack> = catalog.all()

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
    fun surahInfo(surah: Int): String? {
        if (!installedPack(SURAH_INFO)) return null
        return database.rawQuery(
            "SELECT text FROM ${schema(SURAH_INFO)}.surah_info WHERE surah = ?",
            arrayOf(surah.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
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
        translation = null,
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

    fun translations(ayahNumbers: List<Int>, pack: String): Map<Int, TranslationText> {
        if (ayahNumbers.isEmpty() || !installedPack(pack)) return emptyMap()
        val placeholders = ayahNumbers.joinToString(",") { "?" }
        return database.rawQuery(
            "SELECT ayah_number, text, footnotes FROM ${schema(pack)}.translation " +
                "WHERE ayah_number IN ($placeholders)",
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

    /**
     * The word by word meanings, in the reader's language when that pack is
     * here and in English when it is not, so a reader who has any word list
     * always sees meanings rather than empty slots.
     *
     * This is the whole surah at once: the study list asks for every ayah it
     * is about to draw, so it never waits per ayah and never shifts under the
     * reader while it loads.
     */
    fun wordMeanings(ayahNumbers: List<Int>, language: String = "en"): Map<Int, List<WordMeaning>> {
        if (ayahNumbers.isEmpty()) return emptyMap()
        val chosen = meaningPack(language)
        val inClause = ayahNumbers.joinToString(",")
        val meanings = if (chosen != null) {
            database.rawQuery(
                "SELECT ayah_number, position, meaning FROM ${schema(chosen)}.word_meaning " +
                    "WHERE ayah_number IN ($inClause)",
                null,
            ).use { cursor ->
                val out = HashMap<Int, MutableMap<Int, String>>()
                while (cursor.moveToNext()) {
                    out.getOrPut(cursor.getInt(0)) { HashMap() }[cursor.getInt(1)] =
                        cursor.getString(2)
                }
                out
            }
        } else {
            emptyMap()
        }
        return database.rawQuery(
            "SELECT ayah_number, text, position FROM word " +
                "WHERE ayah_number IN ($inClause) AND marker = 0 ORDER BY ayah_number, position",
            null,
        ).use { cursor ->
            val out = HashMap<Int, MutableList<WordMeaning>>(ayahNumbers.size)
            while (cursor.moveToNext()) {
                val ayah = cursor.getInt(0)
                val text = cursor.getString(1)
                val meaning = meanings[ayah]?.get(cursor.getInt(2))?.trim()?.takeIf { it.isNotEmpty() }
                out.getOrPut(ayah) { mutableListOf() }.add(WordMeaning(text, meaning))
            }
            out
        }
    }

    /** The word by word aid for one ayah. */
    fun wordMeanings(ayahNumber: Int, language: String = "en"): List<WordMeaning> {
        val chosen = meaningPack(language)
        val meanings = if (chosen != null) {
            database.rawQuery(
                "SELECT position, meaning FROM ${schema(chosen)}.word_meaning " +
                    "WHERE ayah_number = ? ORDER BY position",
                arrayOf(ayahNumber.toString()),
            ).use { cursor ->
                val out = HashMap<Int, String>(cursor.count)
                while (cursor.moveToNext()) out[cursor.getInt(0)] = cursor.getString(1)
                out
            }
        } else {
            emptyMap()
        }
        return database.rawQuery(
            "SELECT text, position FROM word WHERE ayah_number = ? AND marker = 0 ORDER BY position",
            arrayOf(ayahNumber.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    val text = cursor.getString(0)
                    val position = cursor.getInt(1)
                    add(WordMeaning(text, meanings[position]?.trim()?.takeIf { it.isNotEmpty() }))
                }
            }
        }
    }

    /**
     * The installed word list that speaks a language, falling back to English
     * when that language has none, so meanings are never silently absent.
     */
    fun meaningPack(language: String): String? {
        val preferred = wordsPack(language)
        if (installedPack(preferred)) return preferred
        if (preferred != WORDS_PACK && installedPack(WORDS_PACK)) return WORDS_PACK
        return null
    }

    /** The words of a set of ayahs, in order, markers excluded. */
    fun wordsForAyahs(numbers: List<Int>): Map<Int, List<Word>> {
        if (numbers.isEmpty()) return emptyMap()
        val inClause = numbers.joinToString(",")
        return database.rawQuery(
            "SELECT ayah_number, id, position, text, glyph FROM word " +
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

    fun tafsir(ayahNumber: Int, pack: String): TafsirPassage? {
        if (!installedPack(pack)) return null
        val schema = schema(pack)
        return database.rawQuery(
            "SELECT p.surah, p.from_ayah, p.to_ayah, p.text FROM $schema.tafsir_ayah a " +
                "JOIN $schema.tafsir_passage p ON p.source_id = a.passage_id " +
                "WHERE a.ayah_number = ?",
            arrayOf(ayahNumber.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            TafsirPassage(
                source = pack,
                surah = cursor.getInt(0),
                fromAyah = cursor.getInt(1),
                toAyah = cursor.getInt(2),
                text = cursor.getString(3),
            )
        }
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

    fun firstAyahOfSurah(surah: Int): Int =
        database.rawQuery("SELECT MIN(number) FROM ayah WHERE surah = ?", arrayOf(surah.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 1 }

    /**
     * The first ayah of each juz, with the reference it lands on, for the juz
     * list. SQLite's bare columns beside MIN() come from the matching row, so
     * the whole list is one query.
     */
    fun juzStarts(): List<JuzStart> =
        database.rawQuery(
            "SELECT juz, MIN(number), surah, ayah, verse_key FROM ayah GROUP BY juz ORDER BY juz",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        JuzStart(
                            juz = cursor.getInt(0),
                            ayah = cursor.getInt(1),
                            surah = cursor.getInt(2),
                            verseKey = cursor.getString(4),
                        ),
                    )
                }
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
        catalog.ofType(PackType.Recitation).map { pack ->
            Recitation(
                id = pack.id.removePrefix(RECITER_PREFIX),
                name = pack.name,
                credit = pack.credit,
            )
        }

    fun recitationAyah(recitation: String, ayahNumber: Int): RecitationAyah? {
        val pack = reciterPack(recitation)
        if (!installedPack(pack)) return null
        return database.rawQuery(
            "SELECT audio_path, segments FROM ${schema(pack)}.recitation_ayah WHERE ayah_number = ?",
            arrayOf(ayahNumber.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            RecitationAyah(
                audioPath = cursor.getString(0),
                segments = segments(cursor.getString(1)),
            )
        }
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
        val sources = request.sources
        val leading = ArrayList<SearchHit>(9)
        val counts = intArrayOf(0, 0, 0, 0, 0) // arabic, translation, tafsir, words, surahs

        val reference = if (sources.references) Search.reference(query.input) else null
        if (reference != null) {
            val number = referenceAyahNumber(reference)
            if (number != null) {
                ayahWithPage(number)?.let { leading += SearchHit.ReferenceHit(it.first, it.second) }
            }
        }

        if (sources.surahs) {
            leading += surahHits(query).also { counts[4] = it.size }
        }

        // The Arabic text, matched on the normalized column.
        val arabicNumbers = if (sources.text) {
            arabicMatches(query, limit).also { counts[0] = it.size }
        } else {
            emptyList()
        }
        val arabicWords = matchedWords(arabicNumbers, query.terms)

        // Word meanings, in the reader's language, a single word finding its ayahs.
        val wordMatches = if (sources.words) {
            wordMeaningMatches(query, limit, request.wordsPack)
        } else {
            emptyMap()
        }
        counts[3] = wordMatches.size

        // Translations: the enabled packs, each with exact highlight ranges.
        val translationPacks = if (sources.translations) request.translationPacks else emptyList()
        val translationByNumber = LinkedHashMap<Int, String>()
        if (!query.arabic && translationPacks.isNotEmpty()) {
            for (pack in translationPacks) {
                val numbers = packMatches(pack, query.terms, limit)
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
        val tafsirPacks = if (sources.tafsirs) request.tafsirPacks else emptyList()
        synchronized(tafsirIndex) { tafsirIndex.ensure(tafsirPacks.toSet()) }
        val passageAyahs = LinkedHashMap<String, Int>()
        val matchedPassages = ArrayList<Pair<String, PassageRow>>()
        for (pack in tafsirPacks) {
            if (!installedPack(pack)) continue
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

    private fun wordMeaningMatches(query: SearchQuery, limit: Int, wordsPack: String): Map<Int, String> {
        if (query.arabic || !installedPack(wordsPack)) return emptyMap()
        val condition = query.terms.joinToString(" AND ") { "meaning_search LIKE ? ESCAPE '\\'" }
        return database.rawQuery(
            "SELECT ayah_number, meaning FROM ${schema(wordsPack)}.word_meaning WHERE $condition " +
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

    /** Matches inside one installed pack's own schema. */
    private fun packMatches(schemaId: String, terms: List<String>, limit: Int): List<Int> {
        if (!installedPack(schemaId)) return emptyList()
        val condition = terms.joinToString(" AND ") { "text_search LIKE ? ESCAPE '\\'" }
        return database.rawQuery(
            "SELECT ayah_number FROM ${schema(schemaId)}.translation WHERE $condition " +
                "ORDER BY ayah_number LIMIT ?",
            (terms.map { Search.pattern(it) } + limit.toString()).toTypedArray(),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) add(cursor.getInt(0))
            }
        }
    }

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
            val loaded = ArrayList<TafsirEntry>()
            for (id in wanted) {
                if (!installedPack(id)) continue
                database.rawQuery(
                    "SELECT source_id, surah, from_ayah, to_ayah, text_search " +
                        "FROM ${schema(id)}.tafsir_passage",
                    null,
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        loaded += TafsirEntry(
                            pack = id,
                            sourceId = cursor.getInt(0),
                            surah = cursor.getInt(1),
                            fromAyah = cursor.getInt(2),
                            toAyah = cursor.getInt(3),
                            folded = cursor.getString(4) ?: "",
                        )
                    }
                }
            }
            entries = loaded
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
        if (sourceIds.isEmpty() || !installedPack(pack)) return emptyMap()
        val out = HashMap<Int, String>(sourceIds.size)
        for (chunk in sourceIds.chunked(400)) {
            val placeholders = chunk.joinToString(",") { "?" }
            database.rawQuery(
                "SELECT source_id, text FROM ${schema(pack)}.tafsir_passage WHERE source_id IN ($placeholders)",
                chunk.map { it.toString() }.toTypedArray(),
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
        const val WORDS_PACK = "words-en"
        const val SURAH_INFO = "words-en"
        const val RECITER_PREFIX = "reciter-"

        fun reciterPack(recitation: String): String = RECITER_PREFIX + recitation

        /** The id of the word list that speaks a language. */
        fun wordsPackId(language: String): String = "words-$language"

        /**
         * Opens the reader's library: the core pack, which ships inside the
         * app, and every pack this device has installed. A pack that is not
         * installed is simply not there, so no query can reach content the
         * reader does not have.
         *
         * A copy of the core pack that cannot be opened is discarded and
         * copied again from the app itself, once. If that also fails, the
         * app is told the content is unavailable rather than crashing on a
         * corrupted file.
         */
        suspend fun open(
            context: Context,
            catalog: PackCatalog,
            installed: Set<String>,
        ): ContentDatabase = withContext(Dispatchers.IO) {
            val store = PackStore(context)
            val core = catalog.get(PackCatalog.CORE_ID)
                ?: throw ContentUnavailableException("the catalog has no core pack")
            val database = openCore(store, core)
                ?: run {
                    store.discardCore()
                    openCore(store, core)
                }
                ?: throw ContentUnavailableException("the Quran text could not be opened")
            for (id in installed) {
                if (id == PackCatalog.CORE_ID) continue
                val file = store.fileFor(id)
                if (!file.exists()) continue
                runCatching {
                    database.execSQL("ATTACH DATABASE ? AS \"" + id.replace("\"", "") + "\"", arrayOf(file.path))
                }.onFailure {
                    android.util.Log.w("ContentDatabase", "pack $id could not be attached", it)
                }
            }
            ContentDatabase(database, catalog.withInstalled(installed), installed)
        }

        private fun openCore(store: PackStore, core: ContentPack): SQLiteDatabase? = runCatching {
            SQLiteDatabase.openDatabase(
                store.coreFile(core).path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        }.getOrNull()
    }
}

/** The Quran text could not be opened, so the reader is offered a way back. */
class ContentUnavailableException(message: String) : Exception(message)
