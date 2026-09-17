package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.Arabic
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.sql.Connection

/**
 * Builds the read-only content database the app ships: the canonical text
 * with its geometry, the translation, both tafsirs with group resolution,
 * search columns, recitations with segments, and the navigation metadata.
 *
 * Every insert is deterministic, ordered, and inside one transaction, so the
 * same sources produce the same database bytes. The build fails on any
 * inconsistency it finds rather than shipping a guess.
 */
class Build(private val root: File) {

    private val verify = File(root, "content/work/verify")
    private val output = File(root, "content/build")
    private val problems = mutableListOf<String>()

    private data class Word(
        val id: Int,
        val surah: Int,
        val ayah: Int,
        val position: Int,
        val text: String,
        val glyph: String,
        val marker: Boolean,
        var page: Int = 0,
        var line: Int = 0,
        var linePosition: Int = 0,
    )

    fun run(): Int {
        val manifest = loadManifest(root)
        output.mkdirs()
        val database = File(output, "quran.db")
        if (database.exists()) database.delete()

        val words = readWords()
        val pageLines = readPageLines()
        assignGeometry(words, pageLines)
        val ayahNumbers = indexAyahs(words)
        val translation = readTranslation()
        val wordTranslations = readWordTranslations()
        val surahs = readSurahs()
        val navigation = readNavigation(ayahNumbers)

        openSqlite(database).use { connection ->
            connection.createStatement().use { it.execute("PRAGMA journal_mode=OFF") }
            connection.createStatement().use { it.execute("PRAGMA synchronous=OFF") }
            connection.autoCommit = false
            schema(connection)
            insertSurahs(connection, surahs)
            val inserted = insertAyahsAndWords(connection, words, ayahNumbers, wordTranslations, navigation)
            insertPageLines(connection, pageLines)
            insertTranslation(connection, translation)
            insertTafsirIbnKathir(connection, ayahNumbers)
            insertTafsirSaadi(connection, ayahNumbers)
            insertRecitations(connection, manifest, ayahNumbers)
            insertSurahInfo(connection)
            insertMeta(connection, manifest, inserted)
            connection.commit()
            connection.autoCommit = true
            connection.createStatement().use { it.execute("VACUUM") }
        }

        val report = report(database, words, ayahNumbers, translation.size)
        File(root, "content/build-report.json").writeText(report)
        if (problems.isEmpty()) {
            println("build: ${words.size} words, ${ayahNumbers.size} ayahs, ${translation.size} translations")
            println("build: database ${database.length() / 1024 / 1024} MB")
            println("build: sha256 ${sha256(database)}")
            return 0
        }
        println("BUILD FAILURES (${problems.size})")
        problems.forEach { println("  - $it") }
        return 1
    }

    private fun fail(message: String) {
        problems += message
    }

    // ---------------------------------------------------------------- sources

    private fun sourceDir(id: String) = File(verify, id)

    private fun sourceDb(id: String): File =
        sourceDir(id).firstWithExtension(".db", ".sqlite")
            ?: error("no database for $id; run verify first")

    private fun readWords(): List<Word> {
        val script = openSqlite(sourceDb("quran-script-kfgqpc"))
        val glyph = openSqlite(sourceDb("mushaf-glyph-v2"))
        val glyphs = HashMap<Int, String>(90_000)
        glyph.use { it.each("SELECT id, text FROM words") { rs -> glyphs[rs.getInt(1)] = rs.getString(2) } }
        val words = ArrayList<Word>(83_668)
        script.use { connection ->
            connection.each("SELECT id, surah, ayah, word, text FROM words ORDER BY id") { rs ->
                val text = rs.getString(5) ?: ""
                val id = rs.getInt(1)
                words += Word(
                    id = id,
                    surah = rs.getInt(2),
                    ayah = rs.getInt(3),
                    position = rs.getInt(4),
                    text = text,
                    glyph = glyphs[id] ?: "",
                    marker = Arabic.skeleton(text).isEmpty(),
                )
            }
        }
        glyph.close()
        if (words.size != 83_668) fail("expected 83668 words, found ${words.size}")
        if (words.any { it.glyph.isBlank() }) fail("a word has no glyph text")
        return words
    }

    private data class PageLine(
        val page: Int,
        val line: Int,
        val type: String,
        val centered: Boolean,
        val firstWordId: Int,
        val lastWordId: Int,
        val surah: Int,
    )

    private fun readPageLines(): List<PageLine> {
        val lines = ArrayList<PageLine>(9046)
        openSqlite(sourceDb("mushaf-layout-v2")).use { connection ->
            connection.each(
                "SELECT page_number, line_number, line_type, is_centered, first_word_id, last_word_id, surah_number " +
                    "FROM pages ORDER BY page_number, CAST(line_number AS INTEGER)",
            ) { rs ->
                val first = rs.getString(5)?.toIntOrNull() ?: 0
                val last = rs.getString(6)?.toIntOrNull() ?: 0
                lines += PageLine(
                    page = rs.getInt(1),
                    line = rs.getInt(2),
                    type = rs.getString(3),
                    centered = rs.getInt(4) == 1,
                    firstWordId = first,
                    lastWordId = last,
                    surah = rs.getString(7)?.toIntOrNull() ?: 0,
                )
            }
        }
        return lines
    }

    private fun assignGeometry(words: List<Word>, pageLines: List<PageLine>) {
        val byId = words.associateBy { it.id }
        for (line in pageLines) {
            if (line.type != "ayah") continue
            var position = 0
            for (id in line.firstWordId..line.lastWordId) {
                val word = byId[id]
                if (word == null) {
                    fail("layout references unknown word $id")
                    continue
                }
                word.page = line.page
                word.line = line.line
                word.linePosition = position++
            }
        }
        val unplaced = words.count { it.page == 0 }
        if (unplaced != 0) fail("$unplaced words were not placed on a page")
    }

    private fun indexAyahs(words: List<Word>): LinkedHashMap<String, Int> {
        val ayahs = LinkedHashMap<String, Int>(7000)
        for (word in words) {
            val key = "${word.surah}:${word.ayah}"
            ayahs.getOrPut(key) { ayahs.size + 1 }
        }
        if (ayahs.size != 6236) fail("expected 6236 ayahs, found ${ayahs.size}")
        return ayahs
    }

    private data class Translation(val number: Int, val text: String, val footnotes: String)

    private fun readTranslation(): Map<Int, Translation> {
        val out = HashMap<Int, Translation>(7000)
        openSqlite(sourceDb("translation-saheeh")).use { connection ->
            connection.each("SELECT id, translation, footnotes FROM translations ORDER BY id") { rs ->
                val raw = rs.getString(2) ?: ""
                val text = raw.replace(Regex("^\\(\\d+\\)\\s*"), "")
                out[rs.getInt(1)] = Translation(rs.getInt(1), text, rs.getString(3) ?: "")
            }
        }
        return out
    }

    private fun readWordTranslations(): Map<String, String> {
        val out = HashMap<String, String>(90_000)
        openSqlite(sourceDb("word-by-word-english")).use { connection ->
            connection.each("SELECT surah_number, ayah_number, word_number, text FROM word_translation") { rs ->
                out["${rs.getInt(1)}:${rs.getInt(2)}:${rs.getString(3)}"] = rs.getString(4) ?: ""
            }
        }
        return out
    }

    private data class Surah(
        val number: Int,
        val nameArabic: String,
        val nameSimple: String,
        val nameLatin: String,
        val revelationPlace: String,
        val revelationOrder: Int,
        val versesCount: Int,
        val bismillahPre: Boolean,
    )

    private fun readSurahs(): List<Surah> {
        val json = Json.parseToJsonElement(
            sourceDir("metadata-surah-names").firstWithExtension(".json")!!.readText(),
        ).jsonObject
        val out = ArrayList<Surah>(114)
        for (number in 1..114) {
            val o = json[number.toString()]?.jsonObject ?: continue
            out += Surah(
                number = number,
                nameArabic = o["name_arabic"]?.jsonPrimitive?.contentOrNull ?: "",
                nameSimple = o["name_simple"]?.jsonPrimitive?.contentOrNull ?: "",
                nameLatin = o["name"]?.jsonPrimitive?.contentOrNull ?: "",
                revelationPlace = o["revelation_place"]?.jsonPrimitive?.contentOrNull ?: "",
                revelationOrder = o["revelation_order"]?.jsonPrimitive?.intOrNull ?: 0,
                versesCount = o["verses_count"]?.jsonPrimitive?.intOrNull ?: 0,
                bismillahPre = o["bismillah_pre"]?.jsonPrimitive?.contentOrNull == "true",
            )
        }
        if (out.size != 114) fail("expected 114 surahs, found ${out.size}")
        return out
    }

    private class Navigation {
        val juz = IntArray(6237)
        val hizb = IntArray(6237)
        val rub = IntArray(6237)
        val manzil = IntArray(6237)
        val ruku = IntArray(6237)
        val sajdah = IntArray(6237)
        val sajdahType = arrayOfNulls<String>(6237)
    }

    private fun readNavigation(ayahNumbers: Map<String, Int>): Navigation {
        val nav = Navigation()
        fun assign(file: String, field: (Navigation) -> IntArray, valueOf: (JsonObject) -> Int) {
            val json = Json.parseToJsonElement(
                sourceDir(file).firstWithExtension(".json")!!.readText(),
            ).jsonObject
            for (entry in json.values) {
                val o = entry.jsonObject
                val value = valueOf(o)
                val mapping = o["verse_mapping"]?.jsonObject ?: continue
                for ((surahText, rangeText) in mapping) {
                    val surah = surahText.toIntOrNull() ?: continue
                    val parts = rangeText.jsonPrimitive.contentOrNull?.split("-") ?: continue
                    val from = parts.getOrNull(0)?.toIntOrNull() ?: continue
                    val to = parts.getOrNull(1)?.toIntOrNull() ?: continue
                    for (ayah in from..to) {
                        val number = ayahNumbers["$surah:$ayah"] ?: continue
                        field(nav)[number] = value
                    }
                }
            }
        }
        assign("metadata-juz", Navigation::juz) { it["juz_number"]?.jsonPrimitive?.intOrNull ?: 0 }
        assign("metadata-hizb", Navigation::hizb) { it["hizb_number"]?.jsonPrimitive?.intOrNull ?: 0 }
        assign("metadata-rub", Navigation::rub) { it["rub_number"]?.jsonPrimitive?.intOrNull ?: 0 }
        assign("metadata-manzil", Navigation::manzil) { it["manzil_number"]?.jsonPrimitive?.intOrNull ?: 0 }
        assign("metadata-ruku", Navigation::ruku) { it["ruku_number"]?.jsonPrimitive?.intOrNull ?: 0 }
        val sajda = Json.parseToJsonElement(
            sourceDir("metadata-sajda").firstWithExtension(".json")!!.readText(),
        ).jsonObject
        for (entry in sajda.values) {
            val o = entry.jsonObject
            val key = o["verse_key"]?.jsonPrimitive?.contentOrNull ?: continue
            val number = ayahNumbers[key] ?: continue
            nav.sajdah[number] = o["sajdah_number"]?.jsonPrimitive?.intOrNull ?: 0
            nav.sajdahType[number] = o["sajdah_type"]?.jsonPrimitive?.contentOrNull
        }
        for (number in 1..6236) {
            if (nav.juz[number] == 0) fail("ayah $number has no juz")
            if (nav.hizb[number] == 0) fail("ayah $number has no hizb")
            if (nav.rub[number] == 0) fail("ayah $number has no rub")
            if (nav.manzil[number] == 0) fail("ayah $number has no manzil")
        }
        return nav
    }

    // ---------------------------------------------------------------- writing

    private fun schema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
            statement.execute(
                "CREATE TABLE surah (number INTEGER PRIMARY KEY, name_arabic TEXT NOT NULL, " +
                    "name_simple TEXT NOT NULL, name_latin TEXT NOT NULL, revelation_place TEXT NOT NULL, " +
                    "revelation_order INTEGER NOT NULL, verses_count INTEGER NOT NULL, bismillah_pre INTEGER NOT NULL)",
            )
            statement.execute(
                "CREATE TABLE ayah (number INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, " +
                    "verse_key TEXT NOT NULL, words_count INTEGER NOT NULL, page INTEGER NOT NULL, juz INTEGER NOT NULL, " +
                    "hizb INTEGER NOT NULL, rub INTEGER NOT NULL, manzil INTEGER NOT NULL, ruku INTEGER NOT NULL, " +
                    "sajdah INTEGER NOT NULL, sajdah_type TEXT, text TEXT NOT NULL, text_search TEXT NOT NULL)",
            )
            statement.execute("CREATE INDEX ayah_surah ON ayah(surah, ayah)")
            statement.execute("CREATE INDEX ayah_page ON ayah(page)")
            statement.execute(
                "CREATE TABLE word (id INTEGER PRIMARY KEY, ayah_number INTEGER NOT NULL, surah INTEGER NOT NULL, " +
                    "ayah INTEGER NOT NULL, position INTEGER NOT NULL, marker INTEGER NOT NULL, text TEXT NOT NULL, " +
                    "glyph TEXT NOT NULL, text_search TEXT NOT NULL, translation TEXT, page INTEGER NOT NULL, " +
                    "line INTEGER NOT NULL, line_position INTEGER NOT NULL)",
            )
            statement.execute("CREATE INDEX word_ref ON word(surah, ayah, position)")
            statement.execute("CREATE INDEX word_page ON word(page, line, line_position)")
            statement.execute(
                "CREATE TABLE page_line (page INTEGER NOT NULL, line INTEGER NOT NULL, type TEXT NOT NULL, " +
                    "centered INTEGER NOT NULL, first_word_id INTEGER NOT NULL, last_word_id INTEGER NOT NULL, " +
                    "surah INTEGER NOT NULL, PRIMARY KEY (page, line))",
            )
            statement.execute(
                "CREATE TABLE translation (ayah_number INTEGER PRIMARY KEY, text TEXT NOT NULL, " +
                    "footnotes TEXT NOT NULL, text_search TEXT NOT NULL)",
            )
            statement.execute(
                "CREATE TABLE tafsir_passage (source TEXT NOT NULL, source_id INTEGER NOT NULL, surah INTEGER NOT NULL, " +
                    "from_ayah INTEGER NOT NULL, to_ayah INTEGER NOT NULL, text TEXT NOT NULL, " +
                    "PRIMARY KEY (source, source_id))",
            )
            statement.execute(
                "CREATE TABLE tafsir_ayah (source TEXT NOT NULL, ayah_number INTEGER NOT NULL, " +
                    "passage_id INTEGER NOT NULL, PRIMARY KEY (source, ayah_number))",
            )
            statement.execute("CREATE INDEX tafsir_ayah_passage ON tafsir_ayah(passage_id)")
            statement.execute(
                "CREATE TABLE surah_info (surah INTEGER PRIMARY KEY, text TEXT NOT NULL)",
            )
            statement.execute(
                "CREATE TABLE recitation (id TEXT PRIMARY KEY, name TEXT NOT NULL, credit TEXT NOT NULL, " +
                    "base_path TEXT NOT NULL)",
            )
            statement.execute(
                "CREATE TABLE recitation_ayah (recitation TEXT NOT NULL, ayah_number INTEGER NOT NULL, " +
                    "audio_path TEXT NOT NULL, segments TEXT NOT NULL, PRIMARY KEY (recitation, ayah_number))",
            )
        }
    }

    private fun insertMeta(connection: Connection, manifest: Manifest, words: Int) {
        connection.prepareStatement("INSERT INTO meta(key, value) VALUES(?, ?)").use { statement ->
            fun put(key: String, value: String) {
                statement.setString(1, key)
                statement.setString(2, value)
                statement.addBatch()
            }
            put("content_version", manifest.contentVersion)
            put("words", words.toString())
            put("ayahs", "6236")
            put("text_source", "KFGQPC Hafs word by word, audited against Tanzil Uthmani 1.1")
            put("translation_source", "Saheeh International, Noor International Center, QuranEnc version 1.1.2")
            put("tafsir_english", "Tafsir Ibn Kathir, via the Quranic Universal Library")
            put("tafsir_arabic", "Tafsir As-Sa'di, QuranEnc arabic_saadi version 1.0.0")
            put("reciters", "Muhammad Siddiq Al-Minshawi, Mahmoud Khalil Al-Husary")
            statement.executeBatch()
        }
    }

    private fun insertSurahs(connection: Connection, surahs: List<Surah>) {
        connection.prepareStatement(
            "INSERT INTO surah(number, name_arabic, name_simple, name_latin, revelation_place, " +
                "revelation_order, verses_count, bismillah_pre) VALUES(?,?,?,?,?,?,?,?)",
        ).use { statement ->
            for (surah in surahs) {
                statement.setInt(1, surah.number)
                statement.setString(2, surah.nameArabic)
                statement.setString(3, surah.nameSimple)
                statement.setString(4, surah.nameLatin)
                statement.setString(5, surah.revelationPlace)
                statement.setInt(6, surah.revelationOrder)
                statement.setInt(7, surah.versesCount)
                statement.setInt(8, if (surah.bismillahPre) 1 else 0)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun insertAyahsAndWords(
        connection: Connection,
        words: List<Word>,
        ayahNumbers: Map<String, Int>,
        wordTranslations: Map<String, String>,
        nav: Navigation,
    ): Int {
        val grouped = LinkedHashMap<String, MutableList<Word>>(7000)
        for (word in words) grouped.getOrPut("${word.surah}:${word.ayah}") { mutableListOf() }.add(word)

        connection.prepareStatement(
            "INSERT INTO ayah(number, surah, ayah, verse_key, words_count, page, juz, hizb, rub, manzil, ruku, " +
                "sajdah, sajdah_type, text, text_search) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        ).use { statement ->
            for ((key, group) in grouped) {
                val number = ayahNumbers[key] ?: continue
                val textWords = group.filterNot { it.marker }
                val text = textWords.joinToString(" ") { it.text }
                val page = group.minOf { it.page }
                statement.setInt(1, number)
                statement.setInt(2, group.first().surah)
                statement.setInt(3, group.first().ayah)
                statement.setString(4, key)
                statement.setInt(5, textWords.size)
                statement.setInt(6, page)
                statement.setInt(7, nav.juz[number])
                statement.setInt(8, nav.hizb[number])
                statement.setInt(9, nav.rub[number])
                statement.setInt(10, nav.manzil[number])
                statement.setInt(11, nav.ruku[number])
                statement.setInt(12, nav.sajdah[number])
                statement.setString(13, nav.sajdahType[number])
                statement.setString(14, text)
                statement.setString(15, Arabic.normalizeForSearch(text))
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement(
            "INSERT INTO word(id, ayah_number, surah, ayah, position, marker, text, glyph, text_search, " +
                "translation, page, line, line_position) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
        ).use { statement ->
            for (word in words) {
                statement.setInt(1, word.id)
                statement.setInt(2, ayahNumbers["${word.surah}:${word.ayah}"] ?: 0)
                statement.setInt(3, word.surah)
                statement.setInt(4, word.ayah)
                statement.setInt(5, word.position)
                statement.setInt(6, if (word.marker) 1 else 0)
                statement.setString(7, word.text)
                statement.setString(8, word.glyph)
                statement.setString(9, if (word.marker) "" else Arabic.normalizeForSearch(word.text))
                statement.setString(10, wordTranslations["${word.surah}:${word.ayah}:${word.position}"])
                statement.setInt(11, word.page)
                statement.setInt(12, word.line)
                statement.setInt(13, word.linePosition)
                statement.addBatch()
            }
            statement.executeBatch()
        }
        return grouped.size
    }

    private fun insertPageLines(connection: Connection, lines: List<PageLine>) {
        connection.prepareStatement(
            "INSERT INTO page_line(page, line, type, centered, first_word_id, last_word_id, surah) VALUES(?,?,?,?,?,?,?)",
        ).use { statement ->
            for (line in lines) {
                statement.setInt(1, line.page)
                statement.setInt(2, line.line)
                statement.setString(3, line.type)
                statement.setInt(4, if (line.centered) 1 else 0)
                statement.setInt(5, line.firstWordId)
                statement.setInt(6, line.lastWordId)
                statement.setInt(7, line.surah)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun insertTranslation(connection: Connection, translations: Map<Int, Translation>) {
        connection.prepareStatement(
            "INSERT INTO translation(ayah_number, text, footnotes, text_search) VALUES(?,?,?,?)",
        ).use { statement ->
            for (number in 1..6236) {
                val translation = translations[number] ?: run {
                    fail("ayah $number has no translation")
                    continue
                }
                statement.setInt(1, number)
                statement.setString(2, translation.text)
                statement.setString(3, footnotesJson(translation.footnotes))
                statement.setString(4, translation.text.lowercase(java.util.Locale.ROOT))
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun footnotesJson(raw: String): String {
        val entries = Regex("\\[(\\d+)]-\\s*").findAll(raw).toList()
        val array = StringBuilder("[")
        entries.forEachIndexed { index, match ->
            val start = match.range.last + 1
            val end = entries.getOrNull(index + 1)?.range?.first ?: raw.length
            val text = raw.substring(start, end).trim()
            if (index > 0) array.append(',')
            array.append(JsonObject(mapOf(
                "n" to JsonPrimitive(match.groupValues[1]),
                "text" to JsonPrimitive(text),
            )))
        }
        array.append(']')
        return array.toString()
    }

    private data class IbnRow(
        val key: String,
        val group: String,
        val from: String,
        val to: String,
        val text: String,
    )

    private fun insertTafsirIbnKathir(connection: Connection, ayahNumbers: Map<String, Int>) {
        val db = sourceDb("tafsir-ibn-kathir-en")
        val passageIds = HashMap<String, Int>(2000)
        connection.prepareStatement(
            "INSERT INTO tafsir_passage(source, source_id, surah, from_ayah, to_ayah, text) VALUES(?,?,?,?,?,?)",
        ).use { insertPassage ->
            connection.prepareStatement(
                "INSERT INTO tafsir_ayah(source, ayah_number, passage_id) VALUES(?,?,?)",
            ).use { insertMapping ->
                var nextId = 1
                openSqlite(db).use { source ->
                    val rows = ArrayList<IbnRow>(6236)
                    source.each(
                        "SELECT ayah_key, group_ayah_key, from_ayah, to_ayah, text FROM tafsir ORDER BY ayah_key",
                    ) { rs ->
                        rows += IbnRow(
                            key = rs.getString(1),
                            group = rs.getString(2),
                            from = rs.getString(3) ?: rs.getString(1),
                            to = rs.getString(4) ?: rs.getString(1),
                            text = rs.getString(5) ?: "",
                        )
                    }
                    for (row in rows) {
                        if (row.text.isBlank()) continue
                        val id = nextId++
                        passageIds[row.key] = id
                        insertPassage.setString(1, "ibn-kathir")
                        insertPassage.setInt(2, id)
                        insertPassage.setInt(3, row.from.substringBefore(':').toInt())
                        insertPassage.setInt(4, row.from.substringAfter(':').toInt())
                        insertPassage.setInt(5, row.to.substringAfter(':').toInt())
                        insertPassage.setString(6, sanitize(row.text))
                        insertPassage.addBatch()
                    }
                    insertPassage.executeBatch()
                    for (row in rows) {
                        val target = if (passageIds.containsKey(row.key)) row.key else row.group
                        val passage = passageIds[target] ?: run {
                            fail("Ibn Kathir: ${row.key} does not resolve to a passage")
                            continue
                        }
                        val number = ayahNumbers[row.key] ?: continue
                        insertMapping.setString(1, "ibn-kathir")
                        insertMapping.setInt(2, number)
                        insertMapping.setInt(3, passage)
                        insertMapping.addBatch()
                    }
                    insertMapping.executeBatch()
                }
            }
        }
    }

    private fun insertTafsirSaadi(connection: Connection, ayahNumbers: Map<String, Int>) {
        val file = sourceDir("tafsir-as-sadi-ar").firstWithExtension(".json") ?: return fail("no As-Sa'di json")
        val element = Json.parseToJsonElement(file.readText())
        val array: JsonArray = when (element) {
            is JsonArray -> element
            is JsonObject -> (element["result"] ?: element["tafsirs"])?.jsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        connection.prepareStatement(
            "INSERT INTO tafsir_passage(source, source_id, surah, from_ayah, to_ayah, text) VALUES(?,?,?,?,?,?)",
        ).use { insertPassage ->
            data class Entry(val id: Int, val surah: Int, val from: Int, val to: Int, val text: String)
            val entries = ArrayList<Entry>(6600)
            for (entry in array) {
                val o = entry.jsonObject
                val id = o["id"]?.jsonPrimitive?.intOrNull ?: continue
                val surah = o["sura"]?.jsonPrimitive?.intOrNull ?: continue
                val from = o["from_aya"]?.jsonPrimitive?.intOrNull ?: continue
                val to = o["to_aya"]?.jsonPrimitive?.intOrNull ?: continue
                val text = o["text"]?.jsonPrimitive?.contentOrNull ?: ""
                entries += Entry(id, surah, from, to, text)
                insertPassage.setString(1, "as-sadi")
                insertPassage.setInt(2, id)
                insertPassage.setInt(3, surah)
                insertPassage.setInt(4, from)
                insertPassage.setInt(5, to)
                insertPassage.setString(6, sanitize(text))
                insertPassage.addBatch()
            }
            insertPassage.executeBatch()

            // Passage ranges overlap. The commentary the reader should see for
            // an ayah is the shortest passage that contains it, which is the
            // most specific one.
            val chosen = HashMap<Int, Pair<Int, Int>>(7000)
            for (entry in entries) {
                val length = entry.to - entry.from
                for (ayah in entry.from..entry.to) {
                    val number = ayahNumbers["${entry.surah}:$ayah"] ?: continue
                    val current = chosen[number]
                    if (current == null || length < current.second) {
                        chosen[number] = entry.id to length
                    }
                }
            }
            connection.prepareStatement(
                "INSERT INTO tafsir_ayah(source, ayah_number, passage_id) VALUES(?,?,?)",
            ).use { insertMapping ->
                for (number in 1..6236) {
                    val passage = chosen[number] ?: run {
                        fail("As-Sa'di: ayah $number has no passage")
                        continue
                    }
                    insertMapping.setString(1, "as-sadi")
                    insertMapping.setInt(2, number)
                    insertMapping.setInt(3, passage.first)
                    insertMapping.addBatch()
                }
                insertMapping.executeBatch()
            }
        }
    }

    private fun insertRecitations(
        connection: Connection,
        manifest: Manifest,
        ayahNumbers: Map<String, Int>,
    ) {
        val recitations = manifest.datasets.filter { it.id.startsWith("recitation-") }
        connection.prepareStatement(
            "INSERT INTO recitation(id, name, credit, base_path) VALUES(?,?,?,?)",
        ).use { insertRecitation ->
            connection.prepareStatement(
                "INSERT INTO recitation_ayah(recitation, ayah_number, audio_path, segments) VALUES(?,?,?,?)",
            ).use { insertAyah ->
                for (dataset in recitations) {
                    val directory = sourceDb(dataset.id)
                    var inserted = 0
                    openSqlite(directory).use { source ->
                        var basePath = ""
                        source.each("SELECT surah_number, ayah_number, audio_url, segments FROM verses ORDER BY ayah_number") { rs ->
                            val url = rs.getString(3) ?: ""
                            val path = url.substringAfter("/quran/", "")
                            val file = path.substringAfterLast('/')
                            // The recitation export's ayah_number is a global counter;
                            // the real coordinates are the three-digit surah and ayah
                            // in the audio file name, for example 002001.mp3.
                            val match = Regex("^(\\d{3})(\\d{3})\\.[a-z0-9]+$").find(file)
                            if (match == null) {
                                fail("recitation ${dataset.id}: cannot read coordinates from $file")
                                return@each
                            }
                            val surah = match.groupValues[1].toInt()
                            val ayah = match.groupValues[2].toInt()
                            if (surah != rs.getInt(1)) {
                                fail("recitation ${dataset.id}: $file does not match surah ${rs.getInt(1)}")
                                return@each
                            }
                            if (basePath.isEmpty()) basePath = path.substringBeforeLast('/')
                            val number = ayahNumbers["$surah:$ayah"] ?: run {
                                fail("recitation ${dataset.id}: $surah:$ayah has no ayah")
                                return@each
                            }
                            insertAyah.setString(1, dataset.id.removePrefix("recitation-"))
                            insertAyah.setInt(2, number)
                            insertAyah.setString(3, path)
                            insertAyah.setString(4, rs.getString(4) ?: "[]")
                            insertAyah.addBatch()
                            inserted += 1
                        }
                        insertRecitation.setString(1, dataset.id.removePrefix("recitation-"))
                        insertRecitation.setString(2, dataset.name)
                        insertRecitation.setString(3, dataset.credit)
                        insertRecitation.setString(4, basePath)
                        insertRecitation.addBatch()
                    }
                    if (inserted != 6236) fail("recitation ${dataset.id}: inserted $inserted rows, expected 6236")
                }
                insertRecitation.executeBatch()
                insertAyah.executeBatch()
            }
        }
    }

    private fun insertSurahInfo(connection: Connection) {
        val file = sourceDir("surah-info-en").firstWithExtension(".json") ?: return fail("no surah info")
        val json = Json.parseToJsonElement(file.readText()).jsonObject
        connection.prepareStatement("INSERT INTO surah_info(surah, text) VALUES(?,?)").use { statement ->
            for (entry in json.values) {
                val o = entry.jsonObject
                val surah = o["surah_number"]?.jsonPrimitive?.intOrNull ?: continue
                statement.setInt(1, surah)
                statement.setString(2, sanitize(o["text"]?.jsonPrimitive?.contentOrNull ?: ""))
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    /**
     * Keeps the structure the app renders, drops every attribute except a link
     * target, and turns the Arabic quote spans into `q` elements. Letters are
     * never touched; gray spans in Ibn Kathir are parenthetical inline text and
     * keep their content with the span dropped.
     */
    private val decodeEntities = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&nbsp;" to " ",
    )

    private fun sanitize(html: String): String {
        var out = html
        val span = Regex("<span([^>]*)>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
        while (true) {
            val next = span.replace(out) { match ->
                val attributes = match.groupValues[1]
                val content = match.groupValues[2]
                if (attributes.contains("arabic")) "<q>$content</q>" else content
            }
            if (next == out) break
            out = next
        }
        out = out.replace(Regex("<(div|span)[^>]*>"), "")
        out = out.replace("</div>", "").replace("</span>", "")
        val tag = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>")
        val keep = setOf("p", "br", "h1", "h2", "h3", "h4", "strong", "b", "em", "i", "q", "a")
        out = tag.replace(out) { match ->
            val closing = match.groupValues[1]
            val name = match.groupValues[2].lowercase()
            val attributes = match.groupValues[3]
            if (name !in keep) return@replace ""
            if (name == "a" && closing.isEmpty()) {
                val href = Regex("href=\"([^\"]*)\"").find(attributes)?.groupValues?.get(1)
                if (href != null) "<a href=\"$href\">" else "<a>"
            } else {
                "<$closing$name>"
            }
        }
        for ((entity, character) in decodeEntities) out = out.replace(entity, character)
        return out
    }

    private fun report(
        database: File,
        words: List<Word>,
        ayahs: Map<String, Int>,
        translations: Int,
    ): String {
        val markers = words.count { it.marker }
        return buildString {
            appendLine("{")
            appendLine("  \"contentVersion\": \"1.0.0\",")
            appendLine("  \"ayahs\": ${ayahs.size},")
            appendLine("  \"words\": ${words.size - markers},")
            appendLine("  \"markers\": $markers,")
            appendLine("  \"translations\": $translations,")
            appendLine("  \"problems\": ${problems.size},")
            appendLine("  \"databaseBytes\": ${database.length()},")
            appendLine("  \"databaseSha256\": \"${sha256(database)}\"")
            appendLine("}")
        }
    }
}
