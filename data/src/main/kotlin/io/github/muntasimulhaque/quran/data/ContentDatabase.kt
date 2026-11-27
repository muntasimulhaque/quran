package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.database.Cursor
import android.database.CursorWrapper
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import io.github.muntasimulhaque.quran.core.GrammaticalCase
import io.github.muntasimulhaque.quran.core.PartOfSpeech
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.core.SearchQuery
import io.github.muntasimulhaque.quran.core.Suggest
import io.github.muntasimulhaque.quran.core.WordForm
import io.github.muntasimulhaque.quran.core.WordGrammar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.coroutines.coroutineContext
import java.util.concurrent.locks.ReentrantLock

/**
 * The read-only content database.
 *
 * The main file is the core pack: the Quran text and its words. Every other
 * pack (a translation, a tafsir, a word list, the chosen reciter's timings)
 * is attached beside it as its own schema, and only when the screen reading
 * it asks: [open] takes the installed set, which is the truth about the
 * reader's shelf, and the attached set, which is what this connection may
 * read (see [io.github.muntasimulhaque.quran.core.Attachments]). A pack on
 * the shelf that no surface is reading stays a file.
 *
 * The two sets are not the same thing. [installedPacks] answers whether the
 * reader has a pack; [attachedPack] answers whether this connection can read
 * it. The install flow, the rows, and the repair read the first; every query
 * reads the second, so a pack the reader did not turn on cannot take one of
 * SQLite's ten attachments and push a pack they are reading out of the
 * library (owner report, 20.5). A reciter's pack is attached only for the
 * voice the reader chose (owner decision, 20.0).
 */
class ContentDatabase private constructor(
    private val database: SQLiteDatabase,
    private val catalog: PackCatalog,
    val installedPacks: Set<String>,
    val attachedPacks: Set<String>,
) {

    /**
     * The language the surah names are read in: the app sets it once when the
     * library opens, from the reader's own choice, and every query that names
     * a surah answers in that language from then on. The reading, Browse, the
     * search results, the ayah card, the daily reminder, and the share card
     * all draw one name without each surface choosing a column (owner
     * decision, 8.2).
     */
    @Volatile
    private var displayLanguage: String = "en"

    /** Points every following surah name at [language]. */
    fun useLanguage(language: String) {
        displayLanguage = language
        surahCacheValue = null
    }

    /** The schema of one attached pack, quoted for SQL. */
    private fun schema(id: String): String = SafePaths.sqlIdentifier(id)

    /**
     * The word list that speaks a language, when the reader has it. The id
     * itself is the companion's own convention, so the language's shape is
     * written once (owner decision, 20.2).
     */
    private fun wordsPack(language: String): String =
        if (SafePaths.validLanguage(language)) wordsPackId(language) else "words-__invalid__"

    private fun has(id: String): Boolean = id in attachedPacks && id != PackCatalog.CORE_ID

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

    /**
     * Whether this connection can read the pack. It is not the same question
     * as [packOnDevice]: a pack the reader has but did not turn on is on the
     * shelf and not attached, so no query names its schema.
     */
    fun attachedPack(id: String): Boolean = id == PackCatalog.CORE_ID || has(id)

    /**
     * Whether a pack's file is on the device, attached or not. The catalog
     * this database was opened with already reports that set: every pack the
     * reader has, including the ones no screen is reading, and every
     * reciter's pack (owner decision, 20.0). The listening price reads it, so
     * a voice whose timings are here but are not attached is never offered as
     * a download again; [attachedPack] stays the query guard, so an unchosen
     * voice's segments still read as absent until the choice attaches them
     * (owner decision, 20.4).
     */
    fun packOnDevice(id: String): Boolean = catalog.get(id)?.installed == true

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
     * opens nothing (owner decision, 6.6). The translation, the
     * tafsir, and the word meanings keep the strict rule and never fall back.
     */
    fun surahInfo(surah: Int, language: String = "en"): String? {
        // The reader's language's list carries an introduction when that
        // language has one, and the core's English is the fallback. A
        // language whose list has none (Bangla today) reads the English
        // introduction, the one documented exception. The language is asked
        // first so a Bangla introduction can arrive in words-bn without this
        // function having to be changed again (owner decision, 19.7).
        val preferred = wordsPack(language)
        val pack = when {
            hasTable(preferred, "surah_info") -> preferred
            hasTable(null, "surah_info") -> null
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
     * Kotlin (owner decision, 6.6).
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
        if (ayahNumbers.isEmpty() || !attachedPack(pack)) return emptyMap()
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
                val meaning = meaningfulGloss(meanings[ayah]?.get(position))
                out.getOrPut(ayah) { mutableListOf() }.add(WordMeaning(text, meaning, slice))
                starts[ayah] = end + 1
            }
            out
        }
    }

    /**
     * The installed root pack, or null: it has no language of its own, and a
     * reader without it gets a sheet with no root rather than an empty line.
     */
    fun rootPack(): String? = if (attachedPack(WORD_ROOT_PACK)) WORD_ROOT_PACK else null

    /** Every word's root in a set of ayahs: ayah to position to the root. */
    fun wordRoots(ayahNumbers: List<Int>): Map<Int, Map<Int, WordRoot>> {
        if (ayahNumbers.isEmpty()) return emptyMap()
        val chosen = rootPack() ?: return emptyMap()
        val inClause = ayahNumbers.joinToString(",")
        return query(
            "SELECT ayah_number, position, arabic, latin, words_count FROM ${schema(chosen)}.word_root " +
                "WHERE ayah_number IN ($inClause) ORDER BY ayah_number, position",
            null,
        ).use { cursor ->
            val out = HashMap<Int, MutableMap<Int, WordRoot>>()
            while (cursor.moveToNext()) {
                out.getOrPut(cursor.getInt(0)) { HashMap() }[cursor.getInt(1)] =
                    WordRoot(
                        cursor.getString(2) ?: "",
                        cursor.getString(3) ?: "",
                        cursor.getInt(4),
                    )
            }
            out
        }
    }

    /**
     * Every word's grammar and lemma in a set of ayahs: ayah to position to
     * the grammar. Read with the list the sheet belongs to, never on the tap.
     *
     * A held word list from before the grammar table answers with nothing
     * rather than an exception, and the sheet draws the word, its meaning, and
     * its root as it did, until the pack refreshes
     * (owner decision, 18.0).
     */
    fun wordGrammar(
        ayahNumbers: List<Int>,
        language: String = "en",
    ): Map<Int, Map<Int, WordGrammar>> {
        if (ayahNumbers.isEmpty()) return emptyMap()
        val chosen = meaningPack(language) ?: return emptyMap()
        if (!hasTable(chosen, "word_grammar") || !hasTable(chosen, "grammar_lemma")) return emptyMap()
        val pack = schema(chosen)
        val inClause = ayahNumbers.joinToString(",")
        return query(
            "SELECT g.ayah_number, g.position, l.text, g.pos, g.inflection, g.form, g.passive, " +
                "g.pattern, l.words_count, l.forms_count FROM $pack.word_grammar g " +
                "LEFT JOIN $pack.grammar_lemma l ON l.id = g.lemma_id " +
                "WHERE g.ayah_number IN ($inClause) ORDER BY g.ayah_number, g.position",
            null,
        ).use { cursor ->
            val out = HashMap<Int, MutableMap<Int, WordGrammar>>()
            while (cursor.moveToNext()) {
                out.getOrPut(cursor.getInt(0)) { HashMap() }[cursor.getInt(1)] = WordGrammar(
                    lemma = cursor.getString(2),
                    partOfSpeech = PartOfSpeech.ofOrNull(cursor.getString(3)),
                    grammaticalCase = GrammaticalCase.ofOrNull(cursor.getString(4)),
                    form = WordForm.ofOrNull(cursor.getString(5)),
                    passive = cursor.getInt(6) != 0,
                    pattern = cursor.getString(7),
                    lemmaWords = cursor.getInt(8),
                    lemmaForms = cursor.getInt(9),
                )
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
                    add(WordMeaning(text, meaningfulGloss(meanings[position]), slice))
                    start = end + 1
                }
            }
        }
    }

    /**
     * The installed word list that speaks a language, and nothing else. There
     * is no fallback: a Bangla reader never meets English meanings under the
     * Arabic because the Bangla list has not landed yet, exactly as search
     * never falls back (owner decision, 6.6). A language with no
     * list installed answers null, and the reading draws the words without
     * meanings rather than the wrong language's meanings.
     */
    fun meaningPack(language: String): String? {
        val wanted = wordsPack(language)
        return if (attachedPack(wanted)) wanted else null
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
        if (!attachedPack(pack)) return null
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

    /** The first ayah of a surah, or null when the number is not one. */
    fun firstAyahOfSurah(surah: Int): Int? {
        if (surah !in 1..114) return null
        return query("SELECT MIN(number) FROM ayah WHERE surah = ?", arrayOf(surah.toString()))
            .use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) else null }
    }

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

    /** How many ayahs a surah has, without loading the text of any of them. */
    fun ayahCount(surah: Int): Int =
        query("SELECT COUNT(*) FROM ayah WHERE surah = ?", arrayOf(surah.toString())).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
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
        if (!attachedPack(pack)) return null
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

    /**
     * The audio paths of one surah, in ayah order, for the whole-surah check
     * the listening offer makes. The chosen voice's attached pack answers
     * from its own rows; a voice whose pack is installed but not attached
     * names its files from the core pack's recitation map, the same names its
     * rows carry, so every voice's surahs list and its offer stay honest
     * without spending one of SQLite's ten attached schemas on a voice that
     * is not being heard. The store's own file check still decides whether
     * the audio is on the device, so a voice with nothing here reads as
     * nothing here.
     */
    fun recitationAudioPaths(recitation: String, surah: Int): List<String> {
        val pack = reciterPack(recitation)
        if (attachedPack(pack)) {
            return query(
                "SELECT ra.audio_path FROM ${schema(pack)}.recitation_ayah ra " +
                    "JOIN ayah a ON a.number = ra.ayah_number " +
                    "WHERE a.surah = ? ORDER BY a.ayah",
                arrayOf(surah.toString()),
            ).use { cursor ->
                buildList(cursor.count) {
                    while (cursor.moveToNext()) add(cursor.getString(0) ?: "")
                }
            }
        }
        val count = ayahCount(surah)
        if (count <= 0) return emptyList()
        val folder = recitationFolder(recitation) ?: return emptyList()
        // The names are the print's own coordinates, the same bytes the
        // pack's rows carry, and the digits are the file system's own: a
        // Bangla or Arabic locale must not write its numerals into a name.
        return (1..count).map { ayah ->
            "$folder/" + String.format(java.util.Locale.ROOT, "%03d%03d.mp3", surah, ayah)
        }
    }

    /**
     * The folder a voice's audio lives under, read from the recitation map the
     * core pack carries, so any voice's files can be named without attaching
     * that voice's timing pack. Null when the core pack predates the map or
     * the voice is not in it.
     */
    fun recitationFolder(recitation: String): String? {
        if (!hasTable(null, "recitation")) return null
        val base = query(
            "SELECT base_path FROM recitation WHERE id = ?",
            arrayOf(recitation),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) ?: "" else ""
        }
        // The map's row is a bare folder name; the rule that guards an audio
        // path guards it, asked of a path that names one.
        return SafePaths.folderOf("$base/001001.mp3")
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
        val counts = intArrayOf(0, 0, 0, 0, 0, 0) // arabic, translation, tafsir, words, surahs, notes
        coroutineContext.ensureActive()

        if (sources.surahs) {
            leading += surahHits(query).also { counts[4] = it.size }
        }

        val landed = if (sources.references) referencedAyahNumber(query.input) else null
        val referenceHit = landed?.let { place ->
            ayah(place.ayah)?.let { SearchHit.ReferenceHit(it, place.juz) }
        }
        referenceHit?.let { leading.add(0, it) }
        if (landed != null) {
            // A typed reference is the answer: the text, the translations,
            // the tafsir, and the word list are not asked, so a number in an
            // unrelated passage cannot become a "match" beside the verse the
            // reader named. The Search in row is how the reader asks for the
            // text instead (owner decision, 19.7).
            val (hits, capped) = orderSearchHits(leading, emptyList(), emptyList(), limit)
            return SearchResults(
                hits = hits,
                counts = SearchCounts(
                    surahs = counts[4],
                    reference = if (referenceHit == null) 0 else 1,
                ),
                capped = capped,
            )
        }
        coroutineContext.ensureActive()

        // The Arabic text, matched on the normalized column, and only for a
        // query that could match it: a Latin query carries no Arabic letter,
        // and the column carries no Latin one. The exact index is asked
        // first; the recall shape is asked only when it names no ayah, so a
        // query the print already answers keeps its precise result and never
        // gains the broader spellings beside it (owner decision, 19.7).
        val arabicMatch = if (sources.text && query.arabic) {
            arabicMatches(query, limit).also { counts[0] = it.numbers.size }
        } else {
            ArabicMatch(emptyList(), skeleton = false)
        }
        val arabicNumbers = arabicMatch.numbers
        val arabicWords = matchedWords(
            arabicNumbers,
            if (arabicMatch.skeleton) Search.skeletonTerms(query.terms) else query.terms,
            if (arabicMatch.skeleton) "text_skeleton" else "text_search",
        )
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
                val numbers = packMatches(pack, query.terms, query.phrases, limit)
                counts[1] += numbers.size
                for (number in numbers) translationByNumber.putIfAbsent(number, pack)
            }
        }
        val translationTexts = translationTexts(translationByNumber)
        coroutineContext.ensureActive()

        // The reader's own notes. They are the one source that is not in the
        // content database: the note is the reader's work, stored beside it,
        // and the sheet hands the rows in with the request. A note is matched
        // with the same folding as the rest, and only the note's own excerpt
        // becomes the row's evidence. A note is never allowed to cost a full
        // note read per keystroke beyond the saved rows the reader has, and
        // the list is capped like every other source.
        val noteMatches = if (sources.notes && !query.terms.isEmpty()) {
            request.notes.asSequence()
                .mapNotNull { saved ->
                    val note = saved.note?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val (excerpt, ranges) = Search.excerpt(
                        note,
                        query.terms,
                        arabic = query.arabic,
                        window = 160,
                    )
                    if (ranges.isEmpty()) null else saved.ayahNumber to (excerpt to ranges)
                }
                .take(limit)
                .toList()
        } else {
            emptyList()
        }
        counts[5] = noteMatches.size
        coroutineContext.ensureActive()

        // One read for every ayah any source touched, instead of one read per
        // row: a search that matched two hundred ayahs must not cost two
        // hundred queries.
        val wanted = LinkedHashSet<Int>(arabicNumbers.size + wordMatches.size + translationByNumber.size)
        wanted += arabicNumbers
        wanted += wordMatches.keys
        wanted += translationByNumber.keys
        wanted += noteMatches.map { it.first }
        val rows = loadRows(wanted)
        for ((number, matched) in arabicWords) {
            rows[number]?.matchedWords = matched
        }
        for ((number, match) in wordMatches) {
            val row = rows[number] ?: continue
            row.meaning = match.meaning
            row.matchedWordText = meaningWords[number].orEmpty()
        }
        for ((number, note) in noteMatches) {
            val row = rows[number] ?: continue
            row.note = note.first
            row.noteRanges = note.second
        }
        for ((number, pack) in translationByNumber) {
            val row = rows[number] ?: continue
            val translation = translationTexts[pack]?.get(number) ?: continue
            // A search row is prose, not the reading: it has no marker to
            // tap, so the stored sentence's bracketed numbers go and the
            // highlight is measured on the words that remain. Before this,
            // a result for an ayah with a note printed "Allāh,[2]" at the
            // reader (owner report, 14.5).
            val readable = RichText.plain(translation.text)
            row.translation = TranslationHit(
                pack = pack,
                packName = request.packNames[pack] ?: pack,
                text = readable,
                ranges = Search.matchRanges(readable, query.terms, arabic = false),
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
            if (!attachedPack(pack)) continue
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
                note = row.note,
                noteRanges = row.noteRanges,
            )
        }
        // The kinds run from the verse outward, each still in Quran order
        // inside itself; the pure function carries the reasoning and the
        // test, so this call site only has to hand it what it found.
        val (hits, capped) = orderSearchHits(leading, ayahHits, tafsirHits, limit)
        val counted = SearchCounts(counts[0], counts[1], counts[2], counts[3], counts[4], counts[5])
        if (counted.total > 0) {
            return SearchResults(hits = hits, counts = counted, capped = capped)
        }
        // Nothing matched. A reference past its surah is owed that surah's own
        // count, and a misspelled word is owed the word itself. Both are read
        // here, after every source has already answered and only when the
        // answer was empty, so no other query pays for them, and the reader's
        // library is read for a vocabulary at most once per connection
        // (owner decision, 22.3).
        val note = referenceNote(query)
        val suggestion = if (note == null) suggestionFor(query, request) else null
        return SearchResults(
            hits = hits,
            counts = counted,
            capped = capped,
            suggestion = suggestion,
            referenceNote = note,
        )
    }

    private class Row(val ayah: Ayah) {
        var matchedWords: Set<Int> = emptySet()
        var translation: TranslationHit? = null
        var meaning: String? = null
        var matchedWordText: List<String> = emptyList()
        var note: String? = null
        var noteRanges: List<IntRange> = emptyList()
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
    private fun referencedAyahNumber(input: String): Landed? {
        Search.reference(input)?.let { reference ->
            return referenceAyahNumber(reference)?.let { Landed(it) }
        }
        // A juz is asked before a name, because "para 30" is a juz and never
        // a surah named para. It lands on the ayah the juz opens, and the row
        // says the word the reader used (owner decision, 22.3).
        Search.juzReference(input)?.let { juz ->
            return juzFirstAyah[juz]?.let { Landed(it, juz) }
        }
        val named = Search.nameReference(input) ?: return null
        val surah = surahByName(named.name) ?: return null
        return referenceAyahNumber(Search.Reference(surah, named.ayah))?.let { Landed(it) }
    }

    /** Where a typed reference lands, and the juz when the reader named one. */
    private class Landed(val ayah: Int, val juz: Int? = null)

    /**
     * The count a reference past the Book is owed instead of "No matches".
     *
     * Only a well-formed reference reaches this: the grammar has already
     * refused nothing here, it read a surah and an ayah that do not meet. The
     * answer is the content's own count, never a guess, and it is drawn in the
     * reader's digits and language by the sheet (owner decision, 22.3).
     */
    private fun referenceNote(query: SearchQuery): ReferenceNote? {
        Search.reference(query.input)?.let { reference ->
            val ayah = reference.ayah ?: return null
            val surah = surahCache[reference.surah] ?: return null
            return if (ayah > surah.versesCount) {
                ReferenceNote.SurahAyahs(surah.number, surah.versesCount)
            } else {
                null
            }
        }
        Search.nameReference(query.input)?.let { named ->
            val surah = surahByName(named.name)?.let { surahCache[it] }
            if (surah != null && named.ayah > surah.versesCount) {
                return ReferenceNote.SurahAyahs(surah.number, surah.versesCount)
            }
        }
        Search.referenceBeyondBook(query.input)?.let {
            return ReferenceNote.BookSurahs(surahCache.size)
        }
        return null
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
            val forms = surahNames.forms[number] ?: continue
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
    private fun indexedMatch(
        terms: List<String>,
        phrases: List<List<String>> = emptyList(),
    ): String? = Search.matchExpression(terms, phrases)

    private class ArabicMatch(val numbers: List<Int>, val skeleton: Boolean)

    /**
     * The ayahs the exact index names, and, only when it names none, the
     * ayahs the recall index names.
     *
     * The exact index stores the print's own search shape; the recall index
     * stores that shape with the long vowels and the hamza seat removed. The
     * exact answer is never widened with the recall one: a reader who typed a
     * word the print already holds gets those verses and no others, and only
     * a query that finds no verse at all falls back to the broader shape
     * (owner decision, 19.7). [skeleton] tells the caller which column the
     * matched words must be read from, so the highlight and the row cannot
     * disagree.
     */
    private fun arabicMatches(query: SearchQuery, limit: Int): ArabicMatch {
        if (hasTable(null, "ayah_fts")) {
            indexedMatch(query.terms, query.phrases)?.let { match ->
                val exact = ftsAyahs("ayah_fts", match, limit)
                if (exact.isNotEmpty()) return ArabicMatch(exact, skeleton = false)
            }
            if (hasTable(null, "ayah_skeleton_fts")) {
                Search.skeletonExpression(query.terms)?.let { match ->
                    val recall = ftsAyahs("ayah_skeleton_fts", match, limit)
                    if (recall.isNotEmpty()) return ArabicMatch(recall, skeleton = true)
                }
            }
            return ArabicMatch(emptyList(), skeleton = false)
        }
        // The old LIKE path, for a core pack built before the index existed.
        val condition = query.terms.joinToString(" AND ") { "text_search LIKE ? ESCAPE '\\'" }
        val numbers = query(
            "SELECT number FROM ayah WHERE $condition ORDER BY number LIMIT ?",
            (query.terms.map { Search.pattern(it) } + limit.toString()).toTypedArray(),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) add(cursor.getInt(0))
            }
        }
        return ArabicMatch(numbers, skeleton = false)
    }

    /** The running numbers one index names, in the Book's own order. */
    private fun ftsAyahs(table: String, match: String, limit: Int): List<Int> =
        query(
            "SELECT number FROM ayah WHERE rowid IN " +
                "(SELECT docid FROM $table WHERE $table MATCH ?) " +
                "ORDER BY number LIMIT ?",
            arrayOf(match, limit.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) add(cursor.getInt(0))
            }
        }

    private fun matchedWords(
        numbers: List<Int>,
        terms: List<String>,
        column: String = "text_search",
    ): Map<Int, Set<Int>> {
        if (numbers.isEmpty() || terms.isEmpty()) return emptyMap()
        val inClause = numbers.joinToString(",")
        val condition = terms.joinToString(" OR ") { "$column LIKE ? ESCAPE '\\'" }
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
        if (query.arabic || !attachedPack(wordsPack)) return emptyMap()
        val rows = if (hasTable(wordsPack, "word_meaning_fts")) {
            indexedMatch(query.terms, query.phrases)?.let { match ->
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
    private fun packMatches(
        schemaId: String,
        terms: List<String>,
        phrases: List<List<String>>,
        limit: Int,
    ): List<Int> {
        if (!attachedPack(schemaId)) return emptyList()
        if (hasTable(schemaId, "translation_fts")) {
            indexedMatch(terms, phrases)?.let { match ->
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
                if (!attachedPack(id)) continue
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

    /**
     * The surahs' names as the search reads them, built once from the table's
     * own columns: every key a name may be typed by, and the display name each
     * key is offered as when a suggestion names it.
     *
     * The columns are read raw and never through [surahOf], whose nameSimple
     * follows the interface language: a name typed in one language must find
     * its surah whatever the interface is drawing, and a name index built
     * while Bangla was on screen must not leave the English names behind
     * (owner decision, 22.5).
     */
    private class SurahNames(
        val forms: Map<Int, Set<String>>,
        val displays: Map<String, String>,
    )

    private val surahNames: SurahNames by lazy {
        val forms = HashMap<Int, Set<String>>(114)
        val displays = HashMap<String, String>()
        query(
            "SELECT number, name_simple, name_latin, name_arabic, name_bangla FROM surah",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getInt(0)
                val names = listOf(
                    cursor.getString(1) ?: "",
                    cursor.getString(2) ?: "",
                    cursor.getString(3) ?: "",
                    cursor.getString(4) ?: "",
                )
                val keys = buildSet {
                    for (name in names) addAll(Search.surahNameForms(name))
                    addAll(Search.surahNameAliases(number))
                }
                forms[number] = keys
                for (name in names) {
                    if (name.isEmpty()) continue
                    for (form in Search.surahNameForms(name)) {
                        displays.putIfAbsent(form, name)
                    }
                }
            }
        }
        SurahNames(forms, displays)
    }

    private val firstAyahCache: Map<Int, Int?> by lazy {
        surahCache.keys.associateWith { firstAyahOfSurah(it) }
    }

    /**
     * The ayah each juz opens on, read once: a typed juz lands there. The
     * library's own thirty boundaries are the truth here, not an arithmetic
     * the app would carry beside them (owner decision, 22.3).
     */
    private val juzFirstAyah: Map<Int, Int> by lazy {
        juzStarts().associate { it.juz to it.ayah }
    }

    private fun tafsirMatches(pack: String, query: SearchQuery, limit: Int): List<PassageRow> {
        val mine = if (hasTable(pack, "tafsir_passage_fts")) {
            ftsTafsirMatches(pack, query.terms, query.phrases, limit)
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
    private fun ftsTafsirMatches(
        pack: String,
        terms: List<String>,
        phrases: List<List<String>>,
        limit: Int,
    ): List<TafsirEntry> {
        val match = indexedMatch(terms, phrases) ?: return emptyList()
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
        if (sourceIds.isEmpty() || !attachedPack(pack)) return emptyMap()
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

    /**
     * The vocabularies this connection has read for the suggester, one per
     * shape of search.
     *
     * A lexicon is built on the first query that finds nothing and kept for the
     * life of the library, because a reader who mistyped one word may mistype
     * another, and rebuilding a vocabulary on every miss would spend the
     * reader's time twice. Two searches racing the first build may both read
     * it; the later answer is the one kept, and both are correct, which is
     * cheaper than holding a lock across the reads. The cache is cleared rather
     * than grown: a vocabulary is the one part of a search that stays in
     * memory, and a reader who changes packs often should not accumulate them.
     */
    private val lexicons = java.util.concurrent.ConcurrentHashMap<String, Suggest.Lexicon>()

    /**
     * The one word a query that found nothing may have meant, or null.
     *
     * The vocabulary is the library's own, and only a source the reader turned
     * on can have matched: an Arabic query reads the Book's words, and a Latin
     * one reads the enabled translations and the word list. The tafsirs are
     * not read here: they are tens of megabytes of commentary for the rarest
     * vocabulary, and a misspelled word is not hiding there (owner decision,
     * 22.3).
     */
    private suspend fun suggestionFor(
        query: SearchQuery,
        request: SearchRequest,
    ): SearchSuggestion? {
        val sources = request.sources
        val readable = if (query.arabic) {
            sources.text
        } else {
            // A name can now be the one word offered when only the surahs
            // source is on, so the gate reads that source too (owner decision,
            // 22.5).
            sources.translations || sources.words || sources.surahs
        }
        if (!readable) return null
        if (query.terms.none { it.codePointCount(0, it.length) >= Suggest.MIN_CULPRIT_LENGTH }) {
            return null
        }
        val lexicon = lexiconFor(request.translationPacks, request.wordsPack, sources)
        val candidate = Suggest.suggest(query.terms, lexicon) ?: return null
        val corrected = Suggest.correction(
            input = query.input,
            culprit = candidate.culprit,
            word = candidate.word,
            arabic = query.arabic,
        ) ?: return null
        return SearchSuggestion(candidate.word, corrected)
    }

    /**
     * Warms the vocabulary the suggester reads, for the sheet about to be
     * typed into.
     *
     * The sheet calls this when it opens, the way it warms the tafsir, so a
     * reader's first misspelling is answered in a frame instead of after a
     * read of the library. The read costs a few hundred milliseconds on a
     * worker and touches nothing on the screen; a reader who opens the sheet
     * and types nothing has paid for a read and nothing else (owner decision,
     * 22.3). A miss before the warm finishes builds the same vocabulary on the
     * spot, and the two racing answers are both correct.
     */
    suspend fun warmSuggestion(
        translations: List<String>,
        wordsPack: String,
        sources: SearchSources,
    ) = withContext(Dispatchers.IO) { lexiconFor(translations, wordsPack, sources) }

    /**
     * The vocabulary one shape of search reads, built once.
     *
     * The Quran's own words come from the ayahs' indexed column rather than
     * from the word table: the same vocabulary in ninety times fewer rows. The
     * counts are ayahs carrying the word, and a translation's are rows carrying
     * it; either way the likelier word is the one the library says more often.
     *
     * One vocabulary serves both scripts, because the sheet cannot know which
     * one is about to be typed when it opens and warming two would read the
     * library twice for the same answer. Each word keeps its own script's
     * shelf, so a Latin culprit never compares against the Book's Arabic and
     * a reader pays only for the words that could be theirs.
     *
     * A token enters the Latin half only when it carries a letter and no Arabic
     * one: an Arabic quotation inside an English translation is not a spelling
     * of an English word.
     */
    private suspend fun lexiconFor(
        translations: List<String>,
        wordsPack: String,
        sources: SearchSources,
    ): Suggest.Lexicon {
        val key = lexiconKey(translations, wordsPack, sources)
        lexicons[key]?.let { return it }
        val counts = HashMap<String, Int>()
        if (sources.text) {
            query("SELECT text_search FROM ayah", null).use { cursor ->
                while (cursor.moveToNext()) addTokens(cursor.getString(0), counts, latin = false)
            }
        }
        if (sources.translations) {
            for (pack in translations) {
                coroutineContext.ensureActive()
                if (!attachedPack(pack)) continue
                query("SELECT text_search FROM ${schema(pack)}.translation", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        addTokens(cursor.getString(0), counts, latin = true)
                    }
                }
            }
        }
        if (sources.words && attachedPack(wordsPack)) {
            coroutineContext.ensureActive()
            // The glosses repeat: seventy-seven thousand rows carry about
            // twenty-two thousand different ones, and a vocabulary does not
            // learn anything from the second copy of a word (owner decision,
            // 22.3).
            query(
                "SELECT DISTINCT meaning_search FROM ${schema(wordsPack)}.word_meaning",
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    addTokens(cursor.getString(0), counts, latin = true)
                }
            }
        }
        // The surahs' own names are the reader's navigation vocabulary: a reader
        // who misspells one is naming a surah, and the names are already in the
        // core pack's one small table, so every shape of search can offer them
        // back. A form's count is the surahs that carry it: the Book's own
        // frequency breaks the tie, and a name that names a surah is likelier
        // than a hapax the translation says once (owner decision, 22.5).
        for (forms in surahNames.forms.values) {
            for (form in forms) {
                val seen = counts[form]
                counts[form] = if (seen == null) 1 else seen + 1
            }
        }
        val lexicon = Suggest.Lexicon(counts, surahNames.displays)
        if (lexicons.size >= LEXICON_CACHE) lexicons.clear()
        lexicons[key] = lexicon
        return lexicon
    }

    /** The shape one vocabulary serves: the packs it reads and the sources it may answer for. */
    private fun lexiconKey(
        translations: List<String>,
        wordsPack: String,
        sources: SearchSources,
    ): String = buildString {
        append(wordsPack)
        append('|').append(translations.sorted().joinToString(","))
        append(if (sources.text) 't' else '-')
        append(if (sources.translations) 'r' else '-')
        append(if (sources.words) 'w' else '-')
    }

    /**
     * One indexed text's words, counted. The tokens are the index's own, read
     * through [Search.indexTokens], because a count is only honest when it
     * counts what the search matches: a vocabulary that held "muhammad]" as a
     * word offered brackets back to the reader and split one word's frequency
     * across its punctuation (owner decision, 22.5).
     *
     * A one-letter token is kept out: no culprit is near enough to it to be
     * worth naming, and it would only weigh the scan.
     */
    private fun addTokens(text: String?, counts: HashMap<String, Int>, latin: Boolean) {
        if (text.isNullOrEmpty()) return
        for (token in Search.indexTokens(text)) {
            if (token.length < 2) continue
            var letters = false
            var arabic = false
            for (character in token) {
                if (character.isLetter()) letters = true
                if (latin && RichText.isArabic(character.code)) arabic = true
                if (letters && (!latin || arabic)) break
            }
            if (!letters) continue
            if (latin && arabic) continue
            val seen = counts[token]
            counts[token] = if (seen == null) 1 else seen + 1
        }
    }

    private fun surahHits(query: SearchQuery): List<SearchHit.SurahHit> {
        // "surah baqarah" is a reader naming a surah with the word for one in
        // front: the qualifier is dropped from the name match and kept in the
        // text search (owner decision, 22.5).
        val names = query.terms.filterNot { Search.isNameQualifier(it) }
        // The match runs against the keys a name may be typed by, so "yasin"
        // finds Ya-Sin and "albaqarah" finds Al-Baqarah, whose stored names
        // carry a hyphen the reader did not type. A query of qualifiers alone
        // names no surah, because its list of keys is empty (owner decision,
        // 22.5).
        val keys = names.map { Search.nameKey(it) }.filter { it.isNotEmpty() }
        val number = query.terms.singleOrNull()
        return surahCache.values.asSequence()
            .filter { surah ->
                surah.number.toString() == number ||
                    (keys.isNotEmpty() && keys.all { key ->
                        surahNames.forms[surah.number].orEmpty().any { it.contains(key) }
                    })
            }
            .take(8)
            .map { SearchHit.SurahHit(it) }
            .toList()
    }

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

        /** How many vocabularies one connection keeps before it starts again. */
        private const val LEXICON_CACHE = 4

        const val WORDS_PACK = "words-en"
        const val RECITER_PREFIX = "reciter-"

        fun reciterPack(recitation: String): String = RECITER_PREFIX + recitation

        /** The id of the word list that speaks a language. */
        fun wordsPackId(language: String): String = "words-$language"

        /** The word root pack, one table and no language. */
        const val WORD_ROOT_PACK = "word-root"

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
            /**
             * The packs this connection may read, in priority order: what the
             * screen is showing, never the whole shelf. A pack the reader has
             * but did not turn on stays a file, so it cannot spend one of
             * SQLite's ten attachments on content nothing is drawing (see
             * [io.github.muntasimulhaque.quran.core.Attachments]).
             */
            working: List<String>,
            /** The reciter whose timing pack is attached, or null for every working voice. */
            recitation: String? = null,
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
            for (id in working) {
                if (id == PackCatalog.CORE_ID) continue
                // Only a pack the reader has can be attached, whatever a
                // caller's set says.
                if (id !in installed) continue
                val pack = catalog.get(id) ?: continue
                // A recitation's timings are one voice's own schema, and the
                // voice the reader chose is the one attached. SQLite's
                // ceiling is ten attached databases, and a library that held
                // the core, both translations, both tafsirs, both word
                // lists, the root, and both voices would cross it: the last
                // pack would fall to the limit and read as not installed,
                // which is what the capture leg met as the word list (owner
                // report, 20.0). The voice the reader did not choose keeps
                // its files and its listing; only its segment rows wait for
                // their turn. A null [recitation] keeps the older shape,
                // every installed voice attached, which the tests hold.
                if (recitation != null &&
                    pack.type == PackType.Recitation &&
                    id != reciterPack(recitation)
                ) {
                    continue
                }
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
            // A pack the surface asked for that could not be attached reads
            // as not installed, so its row offers Add and one tap repairs it;
            // the store still sees the file. A pack that is merely off is
            // still the reader's, and its row says so.
            // The catalog also says installed for every reciter's pack on the
            // device, attached or not: a voice the reader did not choose is
            // still on the phone, still lists its bundled and downloaded
            // surahs, and is attached by the one tap that chooses it. The
            // query guard stays the attached set, so an unchosen voice's
            // segments read as absent until their turn.
            val unusable = working.filter { it !in attached }.toSet()
            val marked = (installed - unusable) + installed.filter { id ->
                catalog.get(id)?.type == PackType.Recitation
            }
            ContentDatabase(database, catalog.withInstalled(marked), installed, attached)
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
            PackType.WordRoot -> mapOf(
                "word_root" to setOf("ayah_number", "position", "arabic", "latin"),
            )
            PackType.Recitation -> mapOf(
                "recitation" to setOf("id", "name", "credit"),
                "recitation_ayah" to setOf("ayah_number", "audio_path", "segments"),
            )
            PackType.Script -> mapOf(
                "ayah" to setOf("number", "surah", "ayah", "verse_key", "text", "tajweed"),
            )
            // The word pronunciation pack is a manifest of ZIPs and a folder
            // of Opus files, not a SQLite database; its own store reads it and
            // it never reaches the content database.
            PackType.WordAudio -> emptyMap()
        }
    }
}

/** The Quran text could not be opened, so the reader is offered a way back. */
class ContentUnavailableException(message: String) : Exception(message)
