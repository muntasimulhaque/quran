package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.database.Cursor
import android.database.CursorWrapper
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.core.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.coroutines.coroutineContext
import java.util.concurrent.locks.ReentrantLock

/**
 * The read-only content database.
 *
 * The main file is the core pack: the Quran text and its words. Every other pack (a translation, a tafsir, a word list, a reciter's
 * timings) is attached beside it as its own schema, so a reader's library is
 * exactly the packs on the device and nothing else. A pack that is not
 * installed is not attached, and a query for it never runs.
 */
class ContentDatabase private constructor(
    private val database: SQLiteDatabase,
    private val catalog: PackCatalog,
    val installedPacks: Set<String>,
) {

    /**
     * The language the surah names are read in: the app sets it once when the
     * library opens, from the reader's own choice, and every query that names
     * a surah answers in that language from then on. The reading, Browse, the
     * search results, the ayah card, the daily reminder, and the share card
     * all draw one name without each surface choosing a column (owner
     * decision, this session).
     */
    @Volatile
    private var displayLanguage: String = "en"

    /** Points every following surah name at [language]. */
    fun useLanguage(language: String) {
        displayLanguage = language
        surahCacheValue = null
    }

    /** The schema of one installed pack, quoted for SQL. */
    private fun schema(id: String): String = SafePaths.sqlIdentifier(id)

    /** The word list that speaks a language, when the reader has it. */
    private fun wordsPack(language: String): String =
        if (SafePaths.validLanguage(language)) "words-$language" else "words-__invalid__"

    private fun has(id: String): Boolean = id in installedPacks && id != PackCatalog.CORE_ID

    // ------------------------------------------------------- swapping safely

    /**
     * The gate that lets the library be replaced under a running query.
     *
     * The reader's library is reopened when a pack arrives, leaves, or the
     * interface language changes the packs it reads. The old database used to
     * be closed the moment the new one was ready, which closed it under any
     * query still running on a worker (the reading list loading, a tafsir index
     * warming, a Browse column) and took the process down. Every query now
     * holds a read ticket from the moment it starts until its cursor is
     * closed, and [close] waits for every ticket to be returned before it
     * touches the connection. A swap can no longer close a database a reader
     * is still reading.
     */
    private val swapLock = ReentrantLock()
    private val swapIdle = swapLock.newCondition()
    private var activeReads = 0
    private var retired = false
    private var connectionClosed = false

    /** Runs one raw query, holding a ticket until the returned cursor closes. */
    private fun query(sql: String, args: Array<String>?): Cursor {
        swapLock.lock()
        try {
            // A database whose turn has passed yields nothing rather than
            // touching a closed connection. This can only happen in the one
            // frame between a swap and the recomposition that hands the new
            // library down; the recomposition replaces the empty result.
            if (retired) return MatrixCursor(emptyArray())
            activeReads++
        } finally {
            swapLock.unlock()
        }
        val cursor = try {
            database.rawQuery(sql, args)
        } catch (failure: Throwable) {
            returnTicket()
            throw failure
        }
        return GuardedCursor(cursor)
    }

    private fun returnTicket() {
        var closeNow = false
        swapLock.lock()
        try {
            activeReads--
            if (activeReads <= 0) {
                swapIdle.signalAll()
                if (retired) closeNow = true
            }
        } finally {
            swapLock.unlock()
        }
        if (closeNow) closeConnection()
    }

    /**
     * A query's cursor that hands its ticket back when it is closed. A caller
     * that forgets to close its cursor would hold the swap open, so every call
     * site reads through `use`, which always closes.
     */
    private inner class GuardedCursor(cursor: Cursor) : CursorWrapper(cursor) {
        private var returned = false

        override fun close() {
            try {
                super.close()
            } finally {
                if (!returned) {
                    returned = true
                    returnTicket()
                }
            }
        }
    }

    fun installedPack(id: String): Boolean = id == PackCatalog.CORE_ID || has(id)

    // ----------------------------------------------------------------- packs

    fun packs(): List<ContentPack> = catalog.all()

    // --------------------------------------------------------------- reading

    fun surahs(): List<Surah> =
        query(
            "SELECT number, name_arabic, name_simple, name_latin, name_bangla, revelation_place, verses_count, " +
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
        query(
            "SELECT number, name_arabic, name_simple, name_latin, name_bangla, revelation_place, verses_count, " +
                "bismillah_pre FROM surah WHERE number = ?",
            arrayOf(number.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            surahOf(cursor)
        }

    /**
     * One surah, with its name in the reader's own language. The Latin and
     * Arabic names are always kept, because search reads them in every
     * language; [Surah.nameSimple] carries the name the interface draws.
     */
    private fun surahOf(cursor: android.database.Cursor): Surah {
        val bangla = cursor.getString(4) ?: ""
        return Surah(
            number = cursor.getInt(0),
            nameArabic = cursor.getString(1) ?: "",
            nameSimple = if (displayLanguage.startsWith("bn") && bangla.isNotBlank()) {
                bangla
            } else {
                cursor.getString(2) ?: ""
            },
            nameLatin = cursor.getString(3) ?: "",
            nameBangla = bangla,
            revelationPlace = cursor.getString(5) ?: "",
            versesCount = cursor.getInt(6),
            bismillahPre = cursor.getInt(7) == 1,
        )
    }

    /**
     * The introduction to a surah, in the reader's own language when the
     * content carries one for it, and in English when it does not. The
     * introduction is the one deliberate exception to the one-language rule:
     * there is no Bangla introduction yet, and a Bangla reader is owed the
     * introduction in the one language it exists in rather than a door that
     * opens nothing (owner decision, this session). The translation, the
     * tafsir, and the word meanings keep the strict rule and never fall back.
     */
    fun surahInfo(surah: Int, language: String = "en"): String? {
        // The core pack carries the introductions now, so a fresh install and
        // a Bangla word list both have the door. A held core from before the
        // move still falls back to the English word list.
        val preferred = wordsPack(language)
        val pack = when {
            hasTable(null, "surah_info") -> null
            hasTable(preferred, "surah_info") -> preferred
            preferred != WORDS_PACK && hasTable(WORDS_PACK, "surah_info") -> WORDS_PACK
            else -> return null
        }
        val table = if (pack == null) "surah_info" else "${schema(pack)}.surah_info"
        return query(
            "SELECT text FROM $table WHERE surah = ?",
            arrayOf(surah.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst() || cursor.isNull(0)) null else cursor.getString(0)
        }
    }

    private fun wordOf(cursor: android.database.Cursor, offset: Int) = Word(
        id = cursor.getInt(offset + 0),
        position = cursor.getInt(offset + 1),
        text = cursor.getString(offset + 2) ?: "",
        translation = null,
    )

    fun ayah(number: Int): Ayah? =
        query(
            "SELECT number, surah, ayah, verse_key, text, tajweed FROM ayah WHERE number = ?",
            arrayOf(number.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) ayahOf(cursor) else null }

    /**
     * The basmalah that opens a surah: Al-Fatiha's own first ayah, read from
     * the content the rest of the reading is read from. It used to be a
     * literal in the reading's own source, and it was one mark away from the
     * audited text: the reader met a basmalah the audit never verified. The
     * one text is read here and drawn everywhere, so no Quran text lives in
     * Kotlin (owner decision, this session).
     */
    fun basmalah(): Ayah? = basmalahAyah

    private val basmalahAyah: Ayah? by lazy { ayah(1) }

    private fun ayahOf(cursor: android.database.Cursor) = Ayah(
        number = cursor.getInt(0),
        surah = cursor.getInt(1),
        ayah = cursor.getInt(2),
        verseKey = cursor.getString(3) ?: "",
        text = cursor.getString(4) ?: "",
        tajweed = cursor.getString(5) ?: "",
    )

    fun translations(ayahNumbers: List<Int>, pack: String): Map<Int, TranslationText> {
        if (ayahNumbers.isEmpty() || !installedPack(pack)) return emptyMap()
        val placeholders = ayahNumbers.joinToString(",") { "?" }
        return query(
            "SELECT ayah_number, text, footnotes FROM ${schema(pack)}.translation " +
                "WHERE ayah_number IN ($placeholders)",
            ayahNumbers.map { it.toString() }.toTypedArray(),
        ).use { cursor ->
            val out = HashMap<Int, TranslationText>(ayahNumbers.size)
            while (cursor.moveToNext()) {
                out[cursor.getInt(0)] = TranslationText(
                    text = cursor.getString(1) ?: "",
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
     * This is the whole surah at once: the reading list asks for every ayah it
     * is about to draw, so it never waits per ayah and never shifts under the
     * reader while it loads.
     */
    fun wordMeanings(ayahNumbers: List<Int>, language: String = "en"): Map<Int, List<WordMeaning>> {
        if (ayahNumbers.isEmpty()) return emptyMap()
        val chosen = meaningPack(language)
        val inClause = ayahNumbers.joinToString(",")
        val meanings = if (chosen != null) {
            query(
                "SELECT ayah_number, position, meaning FROM ${schema(chosen)}.word_meaning " +
                    "WHERE ayah_number IN ($inClause)",
                null,
            ).use { cursor ->
                val out = HashMap<Int, MutableMap<Int, String>>()
                while (cursor.moveToNext()) {
                    out.getOrPut(cursor.getInt(0)) { HashMap() }[cursor.getInt(1)] =
                        cursor.getString(2) ?: ""
                }
                out
            }
        } else {
            emptyMap()
        }
        return query(
            "SELECT w.ayah_number, w.text, w.position, a.tajweed FROM word w " +
                "JOIN ayah a ON a.number = w.ayah_number " +
                "WHERE w.ayah_number IN ($inClause) AND w.marker = 0 ORDER BY w.ayah_number, w.position",
            null,
        ).use { cursor ->
            val out = HashMap<Int, MutableList<WordMeaning>>(ayahNumbers.size)
            val starts = HashMap<Int, Int>(ayahNumbers.size)
            while (cursor.moveToNext()) {
                val ayah = cursor.getInt(0)
                val text = cursor.getString(1) ?: ""
                val position = cursor.getInt(2)
                val code = cursor.getString(3) ?: ""
                val start = starts[ayah] ?: 0
                val end = start + text.length
                val slice = if (end <= code.length) code.substring(start, end) else ""
                val meaning = meanings[ayah]?.get(position)?.trim()?.takeIf { it.isNotEmpty() }
                out.getOrPut(ayah) { mutableListOf() }.add(WordMeaning(text, meaning, slice))
                starts[ayah] = end + 1
            }
            out
        }
    }

    /** The word by word aid for one ayah. */
    fun wordMeanings(ayahNumber: Int, language: String = "en"): List<WordMeaning> {
        val chosen = meaningPack(language)
        val meanings = if (chosen != null) {
            query(
                "SELECT position, meaning FROM ${schema(chosen)}.word_meaning " +
                    "WHERE ayah_number = ? ORDER BY position",
                arrayOf(ayahNumber.toString()),
            ).use { cursor ->
                val out = HashMap<Int, String>(cursor.count)
                while (cursor.moveToNext()) out[cursor.getInt(0)] = cursor.getString(1) ?: ""
                out
            }
        } else {
            emptyMap()
        }
        return query(
            "SELECT w.text, w.position, a.tajweed FROM word w " +
                "JOIN ayah a ON a.number = w.ayah_number " +
                "WHERE w.ayah_number = ? AND w.marker = 0 ORDER BY w.position",
            arrayOf(ayahNumber.toString()),
        ).use { cursor ->
            var start = 0
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    val text = cursor.getString(0) ?: ""
                    val position = cursor.getInt(1)
                    val code = cursor.getString(2) ?: ""
                    val end = start + text.length
                    val slice = if (end <= code.length) code.substring(start, end) else ""
                    add(WordMeaning(text, meanings[position]?.trim()?.takeIf { it.isNotEmpty() }, slice))
                    start = end + 1
                }
            }
        }
    }

    /**
     * The installed word list that speaks a language, and nothing else. There
     * is no fallback: a Bangla reader never meets English meanings under the
     * Arabic because the Bangla list has not landed yet, exactly as search
     * never falls back (owner decision, this session). A language with no
     * list installed answers null, and the reading draws the words without
     * meanings rather than the wrong language's meanings.
     */
    fun meaningPack(language: String): String? {
        val wanted = wordsPack(language)
        return if (installedPack(wanted)) wanted else null
    }

    /** The words of a set of ayahs, in order, markers excluded. */
    fun wordsForAyahs(numbers: List<Int>): Map<Int, List<Word>> {
        if (numbers.isEmpty()) return emptyMap()
        val inClause = numbers.joinToString(",")
        return query(
            "SELECT ayah_number, id, position, text FROM word " +
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
        return query(
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
                text = cursor.getString(3) ?: "",
            )
        }
    }

    /**
     * Builds the tafsir search index ahead of the first query. Called after
     * the first page is on screen, on a worker thread, so the reader's first
     * search never pays for it.
     *
     * The build is speculative and it can outlive the library: a reader who
     * leaves the app, or who adds a pack while it runs, closes this database
     * underneath it. A query against a closed pool throws, and the index has
     * nowhere to go then, so the build stops where it is instead of taking
     * the process down. A failure while the library is open is a real one and
     * still propagates.
     */
    fun prewarmSearch(tafsirPacks: List<String>) {
        try {
            // A pack that carries its own index does not need the in-memory
            // copy, and loading it would spend the memory the index just
            // saved (owner decision, this session).
            val withoutIndex = tafsirPacks.filterNot { hasTable(it, "tafsir_passage_fts") }.toSet()
            if (withoutIndex.isNotEmpty()) {
                synchronized(tafsirIndex) { tafsirIndex.ensure(withoutIndex) }
            }
        } catch (failure: Throwable) {
            // The build is speculative, so a pack that will not read costs
            // the reader tafsir hits until it is repaired, never a crash.
            if (closed) {
                Log.i(TAG, "the tafsir index stopped early: the library closed", failure)
            } else {
                Log.e(TAG, "the tafsir index could not be built", failure)
            }
        }
    }

    fun firstAyahOfSurah(surah: Int): Int =
        query("SELECT MIN(number) FROM ayah WHERE surah = ?", arrayOf(surah.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 1 }

    /**
     * The first ayah of each juz, with the reference it lands on, for the juz
     * list. SQLite's bare columns beside MIN() come from the matching row, so
     * the whole list is one query.
     */
    fun juzStarts(): List<JuzStart> =
        query(
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
                            surahAyah = cursor.getInt(3),
                            verseKey = cursor.getString(4) ?: "",
                        ),
                    )
                }
            }
        }

    fun ayahsByNumbers(numbers: List<Int>): List<Ayah> {
        if (numbers.isEmpty()) return emptyList()
        val inClause = numbers.joinToString(",")
        return query(
            "SELECT number, surah, ayah, verse_key, text, tajweed FROM ayah " +
                "WHERE number IN ($inClause) ORDER BY number",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(ayahOf(cursor))
                }
            }
        }
    }

    fun ayahsOfSurah(surah: Int): List<Ayah> =
        query(
            "SELECT number, surah, ayah, verse_key, text, tajweed FROM ayah WHERE surah = ? ORDER BY ayah",
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
        return query(
            "SELECT audio_path, segments FROM ${schema(pack)}.recitation_ayah WHERE ayah_number = ?",
            arrayOf(ayahNumber.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            RecitationAyah(
                audioPath = cursor.getString(0) ?: "",
                segments = segments(cursor.getString(1)),
            )
        }
    }

    private fun segments(json: String?): List<WordSegment> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return RecitationTiming.normalize(buildList(array.length()) {
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
        })
    }

    // --------------------------------------------------------------- search

    /**
     * One pass over everything the reader enabled: the Arabic text, the
     * translation packs, the tafsir packs, the word meanings, the surah
     * names, and references typed as numbers or as a name and an ayah
     * ("2:255", "baqara 255"). Every source is a prepared statement against
     * an index column shaped by the same normalizer the query went through,
     * so results are exact and no index has to be built at search time.
     *
     * This is a suspending function because it is called per keystroke and a
     * query the reader has already moved on from must stop rather than finish:
     * every source checks for that between passes, so a superseded search gives
     * up at the next source instead of holding a worker's core to the end
     * (owner report).
     *
     * Two rules keep the pass itself short, and both are measured facts about
     * the content rather than guesses:
     *
     * - one read per source, never per row. A common word matches a hundred
     *   and forty ayahs, and asking the database once for each of them, or
     *   once for each of the word lists behind them, cost more than every
     *   scan this function does put together (the word table has no index on
     *   `ayah_number`, so each of those reads was a whole pass over it).
     * - a source a query cannot match is not read. The Quran's own text
     *   holds no Latin letter and no digit, and a query that is not Arabic is
     *   made of Latin letters and digits alone, so "mercy" cannot match a row
     *   of the Arabic column; the `search` gate in `tools` is what holds that
     *   about the content.
     */
    suspend fun search(request: SearchRequest): SearchResults {
        val query = request.query
        val limit = request.limit
        val sources = request.sources
        val leading = ArrayList<SearchHit>(9)
        val counts = intArrayOf(0, 0, 0, 0, 0) // arabic, translation, tafsir, words, surahs
        coroutineContext.ensureActive()

        if (sources.surahs) {
            leading += surahHits(query).also { counts[4] = it.size }
        }

        val referenceNumber = if (sources.references) referencedAyahNumber(query.input) else null
        if (referenceNumber != null) {
            ayah(referenceNumber)?.let { leading += SearchHit.ReferenceHit(it) }
        }
        coroutineContext.ensureActive()

        // The Arabic text, matched on the normalized column, and only for a
        // query that could match it: a Latin query carries no Arabic letter,
        // and the column carries no Latin one.
        val arabicNumbers = if (sources.text && query.arabic) {
            arabicMatches(query, limit).also { counts[0] = it.size }
        } else {
            emptyList()
        }
        val arabicWords = matchedWords(arabicNumbers, query.terms)
        coroutineContext.ensureActive()

        // Word meanings, in the reader's language, a single word finding its ayahs.
        val wordMatches = if (sources.words) {
            wordMeaningMatches(query, limit, request.wordsPack)
        } else {
            emptyMap()
        }
        counts[3] = wordMatches.size
        val meaningWords = matchedWords(wordMatches.mapValues { it.value.positions })
        coroutineContext.ensureActive()

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
        val translationTexts = translationTexts(translationByNumber)
        coroutineContext.ensureActive()

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
        for ((number, match) in wordMatches) {
            val row = rows[number] ?: continue
            row.meaning = match.meaning
            row.matchedWordText = meaningWords[number].orEmpty()
        }
        for ((number, pack) in translationByNumber) {
            val row = rows[number] ?: continue
            val translation = translationTexts[pack]?.get(number) ?: continue
            row.translation = TranslationHit(
                pack = pack,
                packName = request.packNames[pack] ?: pack,
                text = translation.text,
                ranges = Search.matchRanges(translation.text, query.terms, arabic = false),
                footnotes = translation.footnotes,
            )
        }
        coroutineContext.ensureActive()

        // Tafsir: each passage that matched, shown with its own range. A pack
        // that carries the search index answers from it, and one built before
        // the index answers from the folded copy held in memory; either way
        // the passages and their ayahs are read in two batched queries.
        val tafsirHits = ArrayList<SearchHit.TafsirHitResult>()
        val tafsirPacks = if (sources.tafsirs) request.tafsirPacks else emptyList()
        // Only the packs without an index of their own are folded into
        // memory; a pack that carries one answers from its own file.
        synchronized(tafsirIndex) {
            val withoutIndex = tafsirPacks.filterNot { hasTable(it, "tafsir_passage_fts") }.toSet()
            if (withoutIndex.isNotEmpty()) tafsirIndex.ensure(withoutIndex)
        }
        val passageAyahs = LinkedHashMap<String, Int>()
        val matchedPassages = ArrayList<Pair<String, PassageRow>>()
        for (pack in tafsirPacks) {
            coroutineContext.ensureActive()
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
            // The query's script decides the highlighter, not the pack's
            // language: an Arabic query into an English tafsir still has to
            // find and wash the Arabic quotation, and an English query into
            // an Arabic tafsir still has to find the English words.
            val (excerpt, ranges) = Search.excerpt(
                passage.text,
                Search.expandedTerms(query.terms),
                arabic = query.arabic,
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
                text = excerpt,
                ranges = ranges,
            )
        }

        val ayahHits = rows.values.map { row ->
            SearchHit.AyahHit(
                ayah = row.ayah,
                arabicMatchedWords = row.matchedWords,
                translation = row.translation,
                wordMeaning = row.meaning,
                matchedWordText = row.matchedWordText,
            )
        }
        // The kinds run from the verse outward, each still in Quran order
        // inside itself; the pure function carries the reasoning and the
        // test, so this call site only has to hand it what it found.
        val (hits, capped) = orderSearchHits(leading, ayahHits, tafsirHits, limit)
        return SearchResults(
            hits = hits,
            counts = SearchCounts(counts[0], counts[1], counts[2], counts[3], counts[4]),
            capped = capped,
        )
    }

    private class Row(val ayah: Ayah) {
        var matchedWords: Set<Int> = emptySet()
        var translation: TranslationHit? = null
        var meaning: String? = null
        var matchedWordText: List<String> = emptyList()
    }

    private fun referenceAyahNumber(reference: Search.Reference): Int? {
        if (reference.surah !in 1..114) return null
        val first = firstAyahCache[reference.surah] ?: return null
        val ayah = reference.ayah ?: return first
        val surah = surahCache[reference.surah] ?: return null
        if (ayah > surah.versesCount) return null
        return first + ayah - 1
    }

    /**
     * The ayah a typed reference points at, whether the surah was given as a
     * number or as a name: "2:255", "2 255", "baqara 255", "al kahf 10".
     * A named surah that matches nothing, or an ayah past the surah's end,
     * is not a reference: the query stays whatever else it was searched as.
     */
    private fun referencedAyahNumber(input: String): Int? {
        Search.reference(input)?.let { return referenceAyahNumber(it) }
        val named = Search.nameReference(input) ?: return null
        val surah = surahByName(named.name) ?: return null
        return referenceAyahNumber(Search.Reference(surah, named.ayah))
    }

    /**
     * The surah a reader named, for a reference typed in words. The match
     * prefers a name over a longer name that merely begins with it ("nas"
     * is An-Nas, not An-Nasr), and the shortest of those when several
     * remain; ties fall to the earlier surah. The names and their articles
     * are the ones the reader sees, in all three scripts the content
     * carries.
     */
    private fun surahByName(name: String): Int? {
        val key = Search.nameKey(name)
        if (key.isEmpty()) return null
        var bestScore = Int.MAX_VALUE
        var bestSurah: Int? = null
        for (number in surahCache.keys.sorted()) {
            val forms = surahNameKeys[number] ?: continue
            val score = when {
                key in forms -> 0
                else -> {
                    val shortest = forms.asSequence()
                        .filter { it.startsWith(key) }
                        .minOfOrNull { it.length - key.length }
                        ?: continue
                    shortest + 1
                }
            }
            if (score < bestScore) {
                bestScore = score
                bestSurah = number
            }
        }
        return bestSurah
    }

    private fun firstAyahOfPassage(surah: Int, ayah: Int): Int? {
        val surahRow = surahCache[surah] ?: return null
        if (ayah !in 1..surahRow.versesCount) return null
        val first = firstAyahCache[surah] ?: return null
        return first + ayah - 1
    }

    /**
     * Whether one installed pack carries [table], or the core pack when [id]
     * is null. The packs built after the search index shipped carry their
     * own index tables; a pack installed by an earlier build does not, and
     * every query falls back to the scan it always did, so an old install
     * keeps working and gets faster when its pack is refreshed. The check is
     * the same one the introductions use, because a table is a table: what
     * is asked here is whether the pack carries it at all.
     */
    private fun hasTable(id: String?, table: String): Boolean {
        // A pack that is not attached has no schema to ask: the honest answer
        // is that it carries no such table, not an exception. A reader with
        // no word list opens a surah and asks for its introduction, and the
        // question must be answered rather than take the process down.
        if (id != null && !has(id)) return false
        return tables.computeIfAbsent("${id ?: ""}.$table") {
            val master = if (id == null) "sqlite_master" else "${schema(id)}.sqlite_master"
            query("SELECT 1 FROM $master WHERE type = 'table' AND name = ?", arrayOf(table))
                .use { cursor -> cursor.moveToFirst() }
        }
    }

    private val tables = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /**
     * The reader's library, read through the pack's own full text index when
     * it has one, and through the scan it always had when it does not.
     * [Search.matchExpression] turns the reader's terms into the index's own
     * language; the fallback keeps the exact substring behavior of an install
     * whose packs predate the index.
     */
    private fun indexedMatch(terms: List<String>): String? = Search.matchExpression(terms)

    private fun arabicMatches(query: SearchQuery, limit: Int): List<Int> {
        if (hasTable(null, "ayah_fts")) {
            indexedMatch(query.terms)?.let { match ->
                return query(
                    "SELECT number FROM ayah WHERE rowid IN " +
                        "(SELECT docid FROM ayah_fts WHERE ayah_fts MATCH ?) " +
                        "ORDER BY number LIMIT ?",
                    arrayOf(match, limit.toString()),
                ).use { cursor ->
                    buildList(cursor.count) {
                        while (cursor.moveToNext()) add(cursor.getInt(0))
                    }
                }
            }
        }
        val condition = query.terms.joinToString(" AND ") { "text_search LIKE ? ESCAPE '\\'" }
        return query(
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
        return query(
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

    /**
     * The ayahs whose word *meanings* matched, each with the meaning and the
     * positions of the words that carried it. The words themselves are read
     * separately, for every matched ayah at once.
     *
     * One query finds the meanings: row values in an `IN` clause are not on
     * every SQLite this app runs on, and a cross product that the reader would
     * see as words that never matched is worse than no words at all.
     */
    private fun wordMeaningMatches(
        query: SearchQuery,
        limit: Int,
        wordsPack: String,
    ): Map<Int, WordMatch> {
        if (query.arabic || !installedPack(wordsPack)) return emptyMap()
        val rows = if (hasTable(wordsPack, "word_meaning_fts")) {
            indexedMatch(query.terms)?.let { match ->
                val pack = schema(wordsPack)
                query(
                    "SELECT m.ayah_number, m.position, m.meaning FROM $pack.word_meaning m " +
                        "WHERE m.rowid IN (SELECT docid FROM $pack.word_meaning_fts " +
                        "WHERE word_meaning_fts MATCH ?) ORDER BY m.ayah_number LIMIT ?",
                    arrayOf(match, (limit * 4).toString()),
                )
            }
        } else {
            null
        } ?: run {
            val condition = query.terms.joinToString(" AND ") { "meaning_search LIKE ? ESCAPE '\\'" }
            query(
                "SELECT ayah_number, position, meaning FROM ${schema(wordsPack)}.word_meaning WHERE $condition " +
                    "ORDER BY ayah_number LIMIT ?",
                (query.terms.map { Search.pattern(it) } + (limit * 4).toString()).toTypedArray(),
            )
        }
        val meanings = LinkedHashMap<Int, String>()
        val positions = LinkedHashMap<Int, MutableSet<Int>>()
        rows.use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getInt(0)
                val meaning = cursor.getString(2)?.trim().orEmpty()
                if (meaning.isEmpty()) continue
                if (meanings.size >= limit && number !in meanings) continue
                meanings.putIfAbsent(number, meaning)
                positions.getOrPut(number) { LinkedHashSet() }.add(cursor.getInt(1))
            }
        }
        return meanings.mapValues { (number, meaning) -> WordMatch(meaning, positions[number].orEmpty()) }
    }

    /**
     * The Arabic of the given words, for every ayah at once, each ayah's in
     * its own order.
     *
     * The word table has no index on `ayah_number`, so one read per ayah is a
     * whole pass over eighty thousand rows: a search for a common word asked
     * the database a hundred and forty times for a hundred and forty ayahs and
     * spent three seconds doing it, which is the search the owner reported as
     * slow (owner report). One read for all of them, filtered back to
     * the exact ayah and position pairs in memory, is one pass instead.
     */
    private fun matchedWords(wanted: Map<Int, Set<Int>>): Map<Int, List<String>> {
        if (wanted.isEmpty()) return emptyMap()
        val ayahs = wanted.filterValues { it.isNotEmpty() }
        if (ayahs.isEmpty()) return emptyMap()
        val out = LinkedHashMap<Int, MutableList<String>>(ayahs.size)
        for (chunk in ayahs.keys.chunked(400)) {
            val placeholders = chunk.joinToString(",") { "?" }
            query(
                "SELECT ayah_number, position, text FROM word " +
                    "WHERE ayah_number IN ($placeholders) AND marker = 0 ORDER BY ayah_number, position",
                chunk.map { it.toString() }.toTypedArray(),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val number = cursor.getInt(0)
                    if (cursor.getInt(1) !in ayahs.getValue(number)) continue
                    val text = cursor.getString(2).orEmpty().trim()
                    if (text.isEmpty()) continue
                    out.getOrPut(number) { ArrayList() }.add(text)
                }
            }
        }
        return out
    }

    /** One ayah's matched meaning and the word positions that carried it. */
    private class WordMatch(val meaning: String, val positions: Set<Int>)

    /**
     * The translations of every ayah one search matched, one read per pack.
     *
     * A pack's ayahs are read together rather than one at a time: a common
     * word matched a hundred and forty ayahs, and a hundred and forty
     * statements is a hundred and forty round trips into the database for
     * what one statement answers (owner report).
     */
    private fun translationTexts(byPack: Map<Int, String>): Map<String, Map<Int, TranslationText>> {
        if (byPack.isEmpty()) return emptyMap()
        val out = HashMap<String, Map<Int, TranslationText>>(byPack.size)
        for (pack in byPack.values.toSet()) {
            val numbers = byPack.filterValues { it == pack }.keys.toList()
            out[pack] = translations(numbers, pack)
        }
        return out
    }

    /** Matches inside one installed pack's own schema. */
    private fun packMatches(schemaId: String, terms: List<String>, limit: Int): List<Int> {
        if (!installedPack(schemaId)) return emptyList()
        if (hasTable(schemaId, "translation_fts")) {
            indexedMatch(terms)?.let { match ->
                val pack = schema(schemaId)
                return query(
                    "SELECT ayah_number FROM $pack.translation WHERE rowid IN " +
                        "(SELECT docid FROM $pack.translation_fts WHERE translation_fts MATCH ?) " +
                        "ORDER BY ayah_number LIMIT ?",
                    arrayOf(match, limit.toString()),
                ).use { cursor ->
                    buildList(cursor.count) {
                        while (cursor.moveToNext()) add(cursor.getInt(0))
                    }
                }
            }
        }
        val condition = terms.joinToString(" AND ") { "text_search LIKE ? ESCAPE '\\'" }
        return query(
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
            query(
                "SELECT number, surah, ayah, verse_key, text, tajweed FROM ayah " +
                    "WHERE number IN ($placeholders) ORDER BY number",
                chunk.map { it.toString() }.toTypedArray(),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val ayah = ayahOf(cursor)
                    out[ayah.number] = Row(ayah)
                }
            }
        }
        return out
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
        private var byPack: Map<String, List<TafsirEntry>> = emptyMap()

        fun ensure(wanted: Set<String>) {
            if (wanted == packs && byPack.isNotEmpty()) return
            val loaded = LinkedHashMap<String, MutableList<TafsirEntry>>(wanted.size)
            for (id in wanted) {
                if (!installedPack(id)) continue
                val entries = loaded.getOrPut(id) { ArrayList() }
                query(
                    "SELECT source_id, surah, from_ayah, to_ayah, text_search " +
                        "FROM ${schema(id)}.tafsir_passage",
                    null,
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        entries += TafsirEntry(
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
            byPack = loaded
            packs = wanted
        }

        /**
         * One pack's passages that carry every term, in the Book's own order
         * and no further than [limit].
         *
         * Each pack is scanned on its own entries. They used to be scanned as
         * one list and then split, which meant every pack paid for every other
         * pack's prose and the second tafsir a reader had installed was shown
         * only what was left of the limit after the first had filled it
         * (owner report).
         */
        fun matches(pack: String, terms: List<String>, limit: Int): List<TafsirEntry> {
            val entries = byPack[pack] ?: return emptyList()
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

    /**
     * Builds the tafsir index for the packs a search will read, away from the
     * keystroke's own path.
     *
     * The index is the only part of a search that reads tens of megabytes, and
     * it is read once and kept, so the moment to pay for it is the moment the
     * reader says they are going to search: the search sheet asks for this
     * when it opens, and the first query they type finds it already built
     * (owner report, "the search takes time"). Nothing here touches
     * the screen, and a reader who opens the sheet and types nothing has paid
     * for a read of prose and nothing else.
     */
    suspend fun warmTafsir(packs: Collection<String>) = withContext(Dispatchers.IO) {
        val withoutIndex = packs.filterNot { hasTable(it, "tafsir_passage_fts") }.toSet()
        if (withoutIndex.isNotEmpty()) {
            synchronized(tafsirIndex) { tafsirIndex.ensure(withoutIndex) }
        }
    }

    private val tafsirIndex = TafsirIndex()

    /** The 114 surahs, read once: search resolves passages against them. */
    private var surahCacheValue: Map<Int, Surah>? = null

    private val surahCache: Map<Int, Surah>
        get() = surahCacheValue ?: synchronized(this) {
            surahCacheValue ?: surahs().associateBy { it.number }.also { surahCacheValue = it }
        }

    /** Every key each surah's name may be typed by, built once with the names. */
    private val surahNameKeys: Map<Int, Set<String>> by lazy {
        surahCache.values.associate { surah ->
            surah.number to buildSet {
                addAll(Search.surahNameForms(surah.nameSimple))
                addAll(Search.surahNameForms(surah.nameLatin))
                addAll(Search.surahNameForms(surah.nameArabic))
                addAll(Search.surahNameForms(surah.nameBangla))
            }
        }
    }

    private val firstAyahCache: Map<Int, Int> by lazy {
        surahCache.keys.associateWith { firstAyahOfSurah(it) }
    }

    private fun tafsirMatches(pack: String, query: SearchQuery, limit: Int): List<PassageRow> {
        val mine = if (hasTable(pack, "tafsir_passage_fts")) {
            ftsTafsirMatches(pack, query.terms, limit)
        } else {
            synchronized(tafsirIndex) { tafsirIndex.matches(pack, query.terms, limit) }
        }
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

    /**
     * The passages of one pack that carry every term, read from the pack's own
     * full text index: one indexed read answers what the in-memory scan
     * answered only after loading tens of megabytes per pack. The set is the
     * first passages in the Book's own order, and their texts are read exactly
     * as before.
     */
    private fun ftsTafsirMatches(pack: String, terms: List<String>, limit: Int): List<TafsirEntry> {
        val match = indexedMatch(terms) ?: return emptyList()
        val schema = schema(pack)
        return query(
            "SELECT p.source_id, p.surah, p.from_ayah, p.to_ayah FROM $schema.tafsir_passage p " +
                "WHERE p.rowid IN (SELECT docid FROM $schema.tafsir_passage_fts " +
                "WHERE tafsir_passage_fts MATCH ?) ORDER BY p.surah, p.from_ayah LIMIT ?",
            arrayOf(match, limit.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        TafsirEntry(
                            pack = pack,
                            sourceId = cursor.getInt(0),
                            surah = cursor.getInt(1),
                            fromAyah = cursor.getInt(2),
                            toAyah = cursor.getInt(3),
                            folded = "",
                        ),
                    )
                }
            }
        }
    }

    /** The originals of the matched passages, read in one query per pack. */
    private fun passageTexts(pack: String, sourceIds: List<Int>): Map<Int, String> {
        if (sourceIds.isEmpty() || !installedPack(pack)) return emptyMap()
        val out = HashMap<Int, String>(sourceIds.size)
        for (chunk in sourceIds.chunked(400)) {
            val placeholders = chunk.joinToString(",") { "?" }
            query(
                "SELECT source_id, text FROM ${schema(pack)}.tafsir_passage WHERE source_id IN ($placeholders)",
                chunk.map { it.toString() }.toTypedArray(),
            ).use { cursor ->
                while (cursor.moveToNext()) out[cursor.getInt(0)] = cursor.getString(1) ?: ""
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
                val bangla = Search.normalizeForIndex(surah.nameBangla)
                val numberMatch = query.terms.size == 1 && query.terms.first() == surah.number.toString()
                numberMatch || query.terms.all { term ->
                    simple.contains(term) || latin.contains(term) || arabicName.contains(term) ||
                        bangla.contains(term)
                }
            }
            .take(8)
            .map { SearchHit.SurahHit(it) }
            .toList()

    /**
     * Lets the database go without waiting: the last query still reading it
     * closes the connection when it finishes, and one with no readers closes
     * it here. Used when the view model is cleared, where blocking the main
     * thread on a slow search would be worse than the delayed close.
     */
    fun retire() {
        closed = true
        swapLock.lock()
        try {
            retired = true
            if (activeReads <= 0) closeConnectionLocked()
        } finally {
            swapLock.unlock()
        }
    }

    /**
     * Retires the database and waits for every reader to finish before closing
     * the connection. Call it off the main thread: a swap is rare, and the wait
     * is exactly the in-flight query it must not kill.
     */
    fun close() {
        closed = true
        swapLock.lock()
        try {
            retired = true
            while (activeReads > 0) swapIdle.await()
            closeConnectionLocked()
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            swapLock.unlock()
        }
    }

    private fun closeConnection() {
        swapLock.lock()
        try {
            closeConnectionLocked()
        } finally {
            swapLock.unlock()
        }
    }

    private fun closeConnectionLocked() {
        if (connectionClosed) return
        connectionClosed = true
        database.close()
    }

    /** True once [close] has run; an in-flight speculative query reads it. */
    @Volatile
    private var closed = false

    companion object {
        private const val TAG = "ContentDatabase"

        const val WORDS_PACK = "words-en"
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
            runCatching { database.execSQL("PRAGMA query_only=ON") }.getOrElse { failure ->
                database.close()
                throw ContentUnavailableException("the content database could not be made read-only")
            }
            val attached = LinkedHashSet<String>()
            attached += PackCatalog.CORE_ID
            for (id in installed) {
                if (id == PackCatalog.CORE_ID) continue
                val pack = catalog.get(id) ?: continue
                val file = store.fileFor(id)
                if (!file.exists()) continue
                if (!SafePaths.validId(id)) continue
                val verifier = PackVerifier(context)
                if (!verifier.trustedForOpen(pack)) {
                    android.util.Log.w("ContentDatabase", "pack $id failed its checksum; left out of the library")
                    continue
                }
                val schema = SafePaths.sqlIdentifier(id)
                val usable = runCatching {
                    database.execSQL("ATTACH DATABASE ? AS $schema", arrayOf(file.path))
                    val tables = database.rawQuery(
                        "SELECT name FROM $schema.sqlite_master WHERE type='table'",
                        null,
                    ).use { cursor ->
                        buildSet {
                            while (cursor.moveToNext()) add(cursor.getString(0) ?: "")
                        }
                    }
                    val required = expectedColumns(pack.type)
                    tables.containsAll(required.keys) && required.all { (table, columns) ->
                        database.rawQuery("PRAGMA $schema.table_info(\"$table\")", null).use { cursor ->
                            val present = buildSet {
                                while (cursor.moveToNext()) add(cursor.getString(1) ?: "")
                            }
                            present.containsAll(columns)
                        }
                    }
                }.getOrElse { failure ->
                    android.util.Log.w("ContentDatabase", "pack $id could not be attached", failure)
                    false
                }
                if (usable) {
                    attached += id
                } else {
                    // A SQLite file can be attached even when it is truncated,
                    // empty, or another pack's file with the wrong name; the
                    // attach alone is not proof that the schema the app is
                    // about to query is inside it. Leaving it out of the
                    // library keeps every surface on the packs that can be
                    // read, while the file stays in the store, so the pack's
                    // own row reads Add and one tap repairs it.
                    android.util.Log.w(
                        "ContentDatabase",
                        "pack $id has no ${expectedTables(pack.type).joinToString(", ")} table; left out of the library",
                    )
                    runCatching { database.execSQL("DETACH DATABASE $schema") }
                }
            }
            // A pack whose attach failed, or whose file does not carry the
            // tables its type promises, is not in the library: a query for
            // its schema would throw, and every surface that reads it would
            // have to catch the same failure. The catalog therefore reports
            // the attached set, not the raw files: a broken pack reads as not
            // installed, its Add opens a repair, and the store still sees the
            // file.
            ContentDatabase(database, catalog.withInstalled(attached), attached)
        }

        private fun openCore(store: PackStore, core: ContentPack): SQLiteDatabase? = runCatching {
            SQLiteDatabase.openDatabase(
                store.coreFile(core).path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        }.getOrNull()

        /** The tables a pack must carry before any query may reach it. */
        private fun expectedTables(type: PackType): Set<String> = expectedColumns(type).keys

        /** The columns each required table must carry before a query reads it. */
        private fun expectedColumns(type: PackType): Map<String, Set<String>> = when (type) {
            PackType.Translation -> mapOf(
                "translation" to setOf("ayah_number", "text", "footnotes"),
            )
            PackType.Tafsir -> mapOf(
                "tafsir_ayah" to setOf("ayah_number", "passage_id"),
                "tafsir_passage" to setOf("source_id", "surah", "from_ayah", "to_ayah", "text"),
            )
            PackType.Words -> mapOf(
                "word_meaning" to setOf("ayah_number", "position", "meaning"),
            )
            PackType.Recitation -> mapOf(
                "recitation" to setOf("id", "name", "credit"),
                "recitation_ayah" to setOf("ayah_number", "audio_path", "segments"),
            )
            PackType.Script -> mapOf(
                "ayah" to setOf("number", "surah", "ayah", "verse_key", "text", "tajweed"),
            )
        }
    }
}

/** The Quran text could not be opened, so the reader is offered a way back. */
class ContentUnavailableException(message: String) : Exception(message)
