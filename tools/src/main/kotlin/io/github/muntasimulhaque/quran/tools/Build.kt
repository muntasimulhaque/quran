package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.core.TajweedCodec
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
 * word by word, the translation, both tafsirs with group resolution,
 * search columns, recitations with segments, and the navigation metadata.
 *
 * Every insert is deterministic, ordered, and inside one transaction, so the
 * same sources produce the same database bytes. The build fails on any
 * inconsistency it finds rather than shipping a guess.
 */
class Build(private val root: File) {

    private val verify = File(root, "content/work/verify")
    private val output = File(root, "content")
    private val problems = mutableListOf<String>()

    /**
     * How many recitation rows the build moved back one word, and how many
     * regions it folded onto the last drawn word. They are reported with the
     * database hash so a release can say exactly what changed.
     */
    private var recitationSegmentsShifted = 0
    private var recitationSegmentsClamped = 0
    private var recitationSegmentsMapped = 0

    private data class Word(
        val id: Int,
        val surah: Int,
        val ayah: Int,
        /** The position the reading draws, after the QPC merges. */
        val position: Int,
        /** The position the source and the word lists use, before the merges. */
        val sourcePosition: Int,
        val text: String,
        val marker: Boolean,
        val code: String,
    )

    fun run(): Int {
        val manifest = loadManifest(root)
        output.mkdirs()
        val database = File(output, "quran.db")
        if (database.exists()) database.delete()

        val words = readWords()
        val ayahNumbers = indexAyahs(words)
        // The number of words the reading draws for each ayah, the ayah number
        // excluded. The segments are normalized against it, so a region can
        // never name a word the page does not have.
        val wordCounts = words.filter { !it.marker }
            .groupingBy { ayahNumbers["${it.surah}:${it.ayah}"] ?: 0 }
            .eachCount()
        val translation = readTranslation()
        val wordLists = buildMap {
            put("en", readWordList("word-by-word-english"))
            put("bn", readWordList("word-by-word-bengali"))
        }
        val surahs = readSurahs()
        val navigation = readNavigation(ayahNumbers)

        openSqlite(database).use { connection ->
            connection.createStatement().use { it.execute("PRAGMA journal_mode=OFF") }
            connection.createStatement().use { it.execute("PRAGMA synchronous=OFF") }
            connection.autoCommit = false
            schema(connection)
            insertSurahs(connection, surahs)
            val inserted = insertAyahsAndWords(connection, words, ayahNumbers, wordLists, navigation)
            insertTranslation(connection, translation)
            insertTafsirIbnKathir(connection, ayahNumbers)
            // The Bangla translation, built when its source is present, so a
            // fresh clone without it still produces a complete English app.
            if (sourceDir("translation-al-bayan-bn").firstWithExtension(".json") != null) {
                val bangla = readAlBayanTranslation("translation-al-bayan-bn", ayahNumbers)
                insertTranslation(connection, bangla, packId = "translation-al-bayan-bn")
                println("build: Al-Bayan Simple Translation (Bangla): ${bangla.size} ayahs, ${bangla.values.count { it.footnotes.isNotEmpty() }} notes")
            } else {
                println("build: skipping translation-al-bayan-bn (source not present)")
            }
            insertTafsirIbnKathir(
                connection,
                ayahNumbers,
                datasetId = "tafsir-ibn-kathir-bn",
                packId = "tafsir-ibn-kathir-bn",
            )
            insertRecitations(connection, manifest, ayahNumbers, wordCounts)
            insertSurahInfo(connection)
            insertPacks(connection, manifest)
            insertMeta(connection, manifest, inserted)
            connection.commit()
            connection.autoCommit = true
            connection.createStatement().use { it.execute("VACUUM") }
        }

        val report = report(database, manifest.contentVersion, words, ayahNumbers, translation.size)
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
        // The Book's own text, word by word, straight from the QPC Hafs
        // source: the reading is drawn from these words and from nothing
        // else. The source carries the tajweed annotation on the same words,
        // so the code is read beside the text and never inferred from it.
        val script = openSqlite(sourceDb("quran-script-qpc-hafs"))
        val words = ArrayList<Word>(83_668)
        script.use { connection ->
            connection.each("SELECT id, surah, ayah, word, text FROM words ORDER BY id") { rs ->
                val tagged = rs.getString(5) ?: ""
                val parsed = runCatching { TajweedCodec.parse(tagged) }.getOrElse { failure ->
                    fail("${rs.getInt(2)}:${rs.getInt(3)} word ${rs.getInt(4)}: ${failure.message}")
                    return@each
                }
                words += Word(
                    id = rs.getInt(1),
                    surah = rs.getInt(2),
                    ayah = rs.getInt(3),
                    position = rs.getInt(4),
                    sourcePosition = rs.getInt(4),
                    text = parsed.text,
                    marker = Arabic.skeleton(parsed.text).isEmpty(),
                    code = parsed.code,
                )
            }
        }
        val merged = mergeQpcWords(words)
        if (merged.size != 83_664) fail("expected 83664 words, found ${merged.size}")
        return merged
    }

    /**
     * The four places where QUL's word-by-word export splits a pair that the
     * QUL word list and the recitation keep as one word. The pair is joined
     * so the reading, the word meanings, and the recitation continue to share
     * one word number. The joined word's code keeps a zero for the space, so
     * its code and text lengths still agree.
     *
     * The list is derived from the word list, not from Tanzil: 37:130 has four
     * words in both the QPC print and Tanzil, but the word list and the
     * recitation keep "إِلۡ يَاسِين" as one word. The old text merged that
     * name; the QPC export split it (owner report, the QPC rebuild audit).
     */
    private data class QpcMerge(val surah: Int, val ayah: Int, val position: Int)

    private val qpcMerges = listOf(
        QpcMerge(2, 181, 3),
        QpcMerge(8, 6, 4),
        QpcMerge(13, 37, 8),
        QpcMerge(37, 130, 3),
    )

    private fun mergeQpcWords(words: List<Word>): List<Word> {
        val out = words.toMutableList()
        for (merge in qpcMerges) {
            val index = out.indexOfFirst {
                it.surah == merge.surah && it.ayah == merge.ayah && it.position == merge.position
            }
            if (index < 0) {
                fail("QPC merge ${merge.surah}:${merge.ayah}:${merge.position} was not found")
                continue
            }
            val first = out[index]
            val second = out.getOrNull(index + 1)
            if (second == null || second.surah != first.surah || second.ayah != first.ayah ||
                second.position != first.position + 1
            ) {
                fail("QPC merge ${merge.surah}:${merge.ayah}:${merge.position} has no pair")
                continue
            }
            out[index] = first.copy(
                text = first.text + " " + second.text,
                code = first.code + "0" + second.code,
            )
            out.removeAt(index + 1)
            var position = first.position + 1
            for (j in index + 1 until out.size) {
                val word = out[j]
                if (word.surah != first.surah || word.ayah != first.ayah) break
                out[j] = word.copy(position = position++)
            }
        }
        return out
    }

    /**
     * The source word number a recitation segment names, moved through the
     * same merge the reading word went through. The second half of a joined
     * pair names the joined word; every word after it moves down by one. The
     * recitation export was timed against the word list, so its numbers are
     * the source positions, not the reading's renumbered ones.
     */
    private fun recitationPosition(surah: Int, ayah: Int, source: Int): Int {
        var position = source
        for (merge in qpcMerges) {
            if (merge.surah != surah || merge.ayah != ayah) continue
            if (source == merge.position + 1) return merge.position
            if (source > merge.position + 1) position -= 1
        }
        return position
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

    /**
     * The QUL JSON shape: one object per ayah with its verse key and its
     * text. The first star in the sentence becomes the reader's footnote
     * marker, the body after the blank line becomes the note, and every
     * other star is a reference QUL did not fill. [bayanAyah] holds that
     * rule and its tests.
     */
    private fun readAlBayanTranslation(datasetId: String, ayahNumbers: Map<String, Int>): Map<Int, Translation> {
        val file = sourceDir(datasetId).firstWithExtension(".json")
            ?: error("no JSON for $datasetId; run verify first")
        val root = runCatching { Json.parseToJsonElement(file.readText()).jsonObject }
            .getOrElse { failure -> error("cannot read $datasetId: ${failure.message}") }
        val rows = root["translations"]?.jsonArray
            ?: error("$datasetId has no translations array")
        val out = HashMap<Int, Translation>(7000)
        for (row in rows) {
            val entry = row.jsonObject
            val key = entry["verse_key"]?.jsonPrimitive?.contentOrNull ?: continue
            val parsed = bayanAyah(sanitize(entry["text"]?.jsonPrimitive?.contentOrNull ?: ""))
            if (parsed.text.isBlank()) continue
            val surah = key.substringBefore(':').toIntOrNull() ?: continue
            val ayah = key.substringAfter(':').toIntOrNull() ?: continue
            val first = ayahNumbers["$surah:1"] ?: continue
            val number = first + ayah - 1
            val note = parsed.note?.let { "[1]- $it" }.orEmpty()
            out[number] = Translation(number, parsed.text, note)
        }
        return out
    }

    /** The ayah-number rows in the QUL word lists, in either numeral set. */
    private val markerMeaning = Regex("^\\((?:(?:\\d+)|[\u0660-\u0669]+)\\)$")

    /** The word list's meaning for a source word, before the QPC renumbering. */
    private fun meaningAt(list: Map<String, String>, word: Word): String =
        list["${word.surah}:${word.ayah}:${word.sourcePosition}"].orEmpty()

    private fun readWordList(datasetId: String): Map<String, String> {
        if (sourceDb(datasetId) == null) {
            println("build: skipping $datasetId (source not present)")
            return emptyMap()
        }
        val out = HashMap<String, String>(90_000)
        openSqlite(sourceDb(datasetId)).use { connection ->
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
        val nameBangla: String,
        val revelationPlace: String,
        val revelationOrder: Int,
        val versesCount: Int,
        val bismillahPre: Boolean,
    )

    private fun readSurahs(): List<Surah> {
        val json = Json.parseToJsonElement(
            sourceDir("metadata-surah-names").firstWithExtension(".json")!!.readText(),
        ).jsonObject
        // The Bangla names are a dataset of their own, compiled and reviewed
        // for this app. Every surah must carry one: the Bangla interface
        // reads its names from here and never falls back to the Latin line
        // (owner decision, this session).
        val bangla = Json.parseToJsonElement(
            sourceDir("surah-names-bn").firstWithExtension(".json")!!.readText(),
        ).jsonObject
        val out = ArrayList<Surah>(114)
        for (number in 1..114) {
            val o = json[number.toString()]?.jsonObject ?: continue
            out += Surah(
                number = number,
                nameArabic = o["name_arabic"]?.jsonPrimitive?.contentOrNull ?: "",
                nameSimple = o["name_simple"]?.jsonPrimitive?.contentOrNull ?: "",
                nameLatin = o["name"]?.jsonPrimitive?.contentOrNull ?: "",
                nameBangla = bangla[number.toString()]?.jsonPrimitive?.contentOrNull.orEmpty(),
                revelationPlace = o["revelation_place"]?.jsonPrimitive?.contentOrNull ?: "",
                revelationOrder = o["revelation_order"]?.jsonPrimitive?.intOrNull ?: 0,
                versesCount = o["verses_count"]?.jsonPrimitive?.intOrNull ?: 0,
                bismillahPre = o["bismillah_pre"]?.jsonPrimitive?.contentOrNull == "true",
            )
        }
        if (out.size != 114) fail("expected 114 surahs, found ${out.size}")
        for (surah in out) {
            if (surah.nameBangla.isBlank()) fail("surah ${surah.number} has no Bangla name")
        }
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
                    "name_simple TEXT NOT NULL, name_latin TEXT NOT NULL, name_bangla TEXT NOT NULL, " +
                    "revelation_place TEXT NOT NULL, " +
                    "revelation_order INTEGER NOT NULL, verses_count INTEGER NOT NULL, bismillah_pre INTEGER NOT NULL)",
            )
            statement.execute(
                "CREATE TABLE ayah (number INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, " +
                    "verse_key TEXT NOT NULL, words_count INTEGER NOT NULL, juz INTEGER NOT NULL, " +
                    "hizb INTEGER NOT NULL, rub INTEGER NOT NULL, manzil INTEGER NOT NULL, ruku INTEGER NOT NULL, " +
                    "sajdah INTEGER NOT NULL, sajdah_type TEXT, text TEXT NOT NULL, text_search TEXT NOT NULL, " +
                    "tajweed TEXT NOT NULL)",
            )
            statement.execute("CREATE INDEX ayah_surah ON ayah(surah, ayah)")
            statement.execute(
                "CREATE TABLE word (id INTEGER PRIMARY KEY, ayah_number INTEGER NOT NULL, surah INTEGER NOT NULL, " +
                    "ayah INTEGER NOT NULL, position INTEGER NOT NULL, marker INTEGER NOT NULL, text TEXT NOT NULL, " +
                    "text_search TEXT NOT NULL)",
            )
            // Every read of a surah's words filters on the ayah number, and
            // the word table is the largest one in the core pack at over
            // eighty thousand rows. The index this replaces led on the surah
            // and the ayah inside it, so it could not answer a filter on the
            // running number, and each of those reads was a whole pass over
            // the table: opening a surah cost 11.2 ms however many ayahs it
            // holds, and a search that matched a hundred and forty ayahs
            // cost another 10.2 ms in reading their words back. Measured
            // with this index in place, both land under a tenth of a
            // millisecond, which is a hundredfold on the reading's own
            // critical path, and the `ORDER BY ayah_number, position` every
            // one of those reads carries is the index's own order, so the
            // sort goes with it. Every other pack already indexes its
            // `ayah_number` this way; the core pack was the one place it was
            // missing.
            statement.execute("CREATE INDEX word_ayah ON word(ayah_number, position)")
            statement.execute(
                "CREATE TABLE word_meaning (word_id INTEGER NOT NULL, ayah_number INTEGER NOT NULL, " +
                    "position INTEGER NOT NULL, language TEXT NOT NULL, meaning TEXT NOT NULL, " +
                    "meaning_search TEXT NOT NULL, PRIMARY KEY (language, word_id))",
            )
            statement.execute(
                "CREATE TABLE pack (id TEXT PRIMARY KEY, type TEXT NOT NULL, name TEXT NOT NULL, " +
                    "language TEXT NOT NULL, credit TEXT NOT NULL, license TEXT NOT NULL, version TEXT NOT NULL, " +
                    "builtin INTEGER NOT NULL, ayahs INTEGER NOT NULL, bytes INTEGER NOT NULL)",
            )
            statement.execute(
                "CREATE TABLE translation (pack TEXT NOT NULL, ayah_number INTEGER NOT NULL, text TEXT NOT NULL, " +
                    "footnotes TEXT NOT NULL, text_search TEXT NOT NULL, PRIMARY KEY (pack, ayah_number))",
            )
            statement.execute(
                "CREATE TABLE tafsir_passage (pack TEXT NOT NULL, source_id INTEGER NOT NULL, surah INTEGER NOT NULL, " +
                    "from_ayah INTEGER NOT NULL, to_ayah INTEGER NOT NULL, text TEXT NOT NULL, text_search TEXT NOT NULL, " +
                    "PRIMARY KEY (pack, source_id))",
            )
            statement.execute(
                "CREATE TABLE tafsir_ayah (pack TEXT NOT NULL, ayah_number INTEGER NOT NULL, " +
                    "passage_id INTEGER NOT NULL, PRIMARY KEY (pack, ayah_number))",
            )
            statement.execute("CREATE INDEX tafsir_ayah_passage ON tafsir_ayah(pack, passage_id)")
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
            put("text_source", "QPC Hafs word by word, distributed via QUL, audited against the QUL QPC text")
            put("translation_source", "Saheeh International, Noor International Center, QuranEnc version 1.1.2")
            put("tafsir_english", "Tafsir Ibn Kathir, via the Quranic Universal Library")
            put("reciters", "Muhammad Siddiq Al-Minshawi, Mahmoud Khalil Al-Husary")
            put("packs", "translation-saheeh-en tafsir-ibn-kathir-en")
            statement.executeBatch()
        }
    }

    /**
     * The packs this build carries. A pack is a unit of content the reader
     * can turn on, and later replace or add to: a translation, a tafsir, a
     * script. Installed packs outside the build carry the same shape, so the
     * app needs one vocabulary for all of them.
     */
    private fun insertPacks(connection: Connection, manifest: Manifest) {
        // A pack's credit and license are its datasets', resolved through
        // PackSources: the pack id and the dataset id do not share names
        // (the Saheeh pack is built from the translation dataset and its
        // cross-check), so a lookup by pack id alone silently fell back to
        // "Built in" for the one translation every reader starts with.
        fun creditOf(id: String): String =
            PackSources.datasets(id)
                .mapNotNull { dataset -> manifest.datasets.firstOrNull { it.id == dataset }?.credit }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" · ")

        fun licenseOf(id: String): String =
            PackSources.licenses(manifest, id).joinToString(" · ")

        data class Pack(
            val id: String,
            val type: String,
            val name: String,
            val language: String,
            val version: String,
        )

        val packs = listOf(
            Pack("translation-saheeh-en", "translation", "Saheeh International", "en", "1.1.2"),
            Pack("tafsir-ibn-kathir-en", "tafsir", "Ibn Kathir", "en", "QUL"),
            Pack("translation-al-bayan-bn", "translation", "Al-Bayan Simple Translation", "bn", "QUL"),
            Pack("tafsir-ibn-kathir-bn", "tafsir", "Ibn Kathir", "bn", "QUL"),
        )
        connection.prepareStatement(
            "INSERT INTO pack(id, type, name, language, credit, license, version, builtin, ayahs, bytes) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?)",
        ).use { statement ->
            for (pack in packs) {
                val bytes = when (pack.type) {
                    "translation" -> connection.scalarLong(
                        "SELECT SUM(LENGTH(text)) FROM translation WHERE pack = ?", pack.id,
                    )
                    else -> connection.scalarLong(
                        "SELECT SUM(LENGTH(text)) FROM tafsir_passage WHERE pack = ?", pack.id,
                    )
                }
                val ayahs = when (pack.type) {
                    "translation" -> connection.scalarLong(
                        "SELECT COUNT(*) FROM translation WHERE pack = ?", pack.id,
                    )
                    else -> connection.scalarLong(
                        "SELECT COUNT(*) FROM tafsir_ayah WHERE pack = ?", pack.id,
                    )
                }
                statement.setString(1, pack.id)
                statement.setString(2, pack.type)
                statement.setString(3, pack.name)
                statement.setString(4, pack.language)
                statement.setString(5, creditOf(pack.id).ifEmpty { "No credit recorded" })
                statement.setString(6, licenseOf(pack.id).ifEmpty { "No license recorded" })
                statement.setString(7, pack.version)
                statement.setInt(8, 1)
                statement.setInt(9, ayahs.toInt())
                statement.setInt(10, bytes.toInt())
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun insertSurahs(connection: Connection, surahs: List<Surah>) {
        connection.prepareStatement(
            "INSERT INTO surah(number, name_arabic, name_simple, name_latin, name_bangla, revelation_place, " +
                "revelation_order, verses_count, bismillah_pre) VALUES(?,?,?,?,?,?,?,?,?)",
        ).use { statement ->
            for (surah in surahs) {
                statement.setInt(1, surah.number)
                statement.setString(2, surah.nameArabic)
                statement.setString(3, surah.nameSimple)
                statement.setString(4, surah.nameLatin)
                statement.setString(5, surah.nameBangla)
                statement.setString(6, surah.revelationPlace)
                statement.setInt(7, surah.revelationOrder)
                statement.setInt(8, surah.versesCount)
                statement.setInt(9, if (surah.bismillahPre) 1 else 0)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun insertAyahsAndWords(
        connection: Connection,
        words: List<Word>,
        ayahNumbers: Map<String, Int>,
        wordLists: Map<String, Map<String, String>>,
        nav: Navigation,
    ): Int {
        val grouped = LinkedHashMap<String, MutableList<Word>>(7000)
        for (word in words) grouped.getOrPut("${word.surah}:${word.ayah}") { mutableListOf() }.add(word)

        connection.prepareStatement(
            "INSERT INTO ayah(number, surah, ayah, verse_key, words_count, juz, hizb, rub, manzil, ruku, " +
                "sajdah, sajdah_type, text, text_search, tajweed) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        ).use { statement ->
            for ((key, group) in grouped) {
                val number = ayahNumbers[key] ?: continue
                val textWords = group.filterNot { it.marker }
                val text = textWords.joinToString(" ") { it.text }
                val tajweed = textWords.joinToString("0") { it.code }
                statement.setInt(1, number)
                statement.setInt(2, group.first().surah)
                statement.setInt(3, group.first().ayah)
                statement.setString(4, key)
                statement.setInt(5, textWords.size)
                statement.setInt(6, nav.juz[number])
                statement.setInt(7, nav.hizb[number])
                statement.setInt(8, nav.rub[number])
                statement.setInt(9, nav.manzil[number])
                statement.setInt(10, nav.ruku[number])
                statement.setInt(11, nav.sajdah[number])
                statement.setString(12, nav.sajdahType[number])
                statement.setString(13, text)
                statement.setString(14, Search.normalizeForIndex(text))
                statement.setString(15, tajweed)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        for ((language, list) in wordLists) {
            if (list.isEmpty()) continue
            connection.prepareStatement(
                "INSERT INTO word_meaning(word_id, ayah_number, position, language, meaning, meaning_search) " +
                    "VALUES(?,?,?,?,?,?)",
            ).use { meanings ->
                for (word in words) {
                    if (word.marker) continue
                    // The lists include the ayah number as a row of their own.
                    // The reading draws that number itself; a marker text must
                    // never become a word's gloss (owner report, 37:130).
                    val meaning = meaningAt(list, word)
                    if (meaning.isBlank()) continue
                    if (markerMeaning.matches(meaning.trim())) continue
                    meanings.setInt(1, word.id)
                    meanings.setInt(2, ayahNumbers["${word.surah}:${word.ayah}"] ?: 0)
                    meanings.setInt(3, word.position)
                    meanings.setString(4, language)
                    meanings.setString(5, meaning)
                    meanings.setString(6, Search.normalizeForIndex(meaning))
                    meanings.addBatch()
                }
                meanings.executeBatch()
            }
            println("build: word meanings in $language: ${list.size}")
        }

        connection.prepareStatement(
            "INSERT INTO word(id, ayah_number, surah, ayah, position, marker, text, text_search) " +
                "VALUES(?,?,?,?,?,?,?,?)",
        ).use { statement ->
            for (word in words) {
                statement.setInt(1, word.id)
                statement.setInt(2, ayahNumbers["${word.surah}:${word.ayah}"] ?: 0)
                statement.setInt(3, word.surah)
                statement.setInt(4, word.ayah)
                statement.setInt(5, word.position)
                statement.setInt(6, if (word.marker) 1 else 0)
                statement.setString(7, word.text)
                statement.setString(8, if (word.marker) "" else Search.normalizeForIndex(word.text))
                statement.addBatch()
            }
            statement.executeBatch()
        }
        return grouped.size
    }

    private fun insertTranslation(
        connection: Connection,
        translations: Map<Int, Translation>,
        packId: String = "translation-saheeh-en",
    ) {
        connection.prepareStatement(
            "INSERT INTO translation(pack, ayah_number, text, footnotes, text_search) VALUES(?,?,?,?,?)",
        ).use { statement ->
            for (number in 1..6236) {
                val translation = translations[number] ?: run {
                    fail("ayah $number has no translation")
                    continue
                }
                statement.setString(1, packId)
                statement.setInt(2, number)
                statement.setString(3, translation.text)
                statement.setString(4, footnotesJson(translation.footnotes))
                statement.setString(5, Search.normalizeForIndex(translation.text))
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

    private fun insertTafsirIbnKathir(
        connection: Connection,
        ayahNumbers: Map<String, Int>,
        datasetId: String = "tafsir-ibn-kathir-en",
        packId: String = "tafsir-ibn-kathir-en",
    ) {
        val source = sourceDir(datasetId).walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".db") }
        if (source == null) {
            println("build: skipping $datasetId (source not present)")
            return
        }
        val db = source
        val passageIds = HashMap<String, Int>(2000)
        connection.prepareStatement(
            "INSERT INTO tafsir_passage(pack, source_id, surah, from_ayah, to_ayah, text, text_search) " +
                "VALUES(?,?,?,?,?,?,?)",
        ).use { insertPassage ->
            connection.prepareStatement(
                "INSERT INTO tafsir_ayah(pack, ayah_number, passage_id) VALUES(?,?,?)",
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
                        insertPassage.setString(1, packId)
                        insertPassage.setInt(2, id)
                        insertPassage.setInt(3, row.from.substringBefore(':').toInt())
                        insertPassage.setInt(4, row.from.substringAfter(':').toInt())
                        insertPassage.setInt(5, row.to.substringAfter(':').toInt())
                        insertPassage.setString(6, sanitize(row.text))
                        insertPassage.setString(7, Search.normalizeForIndex(sanitize(row.text)))
                        insertPassage.addBatch()
                    }
                    insertPassage.executeBatch()
                    for (row in rows) {
                        val target = if (passageIds.containsKey(row.key)) row.key else row.group
                        val passage = passageIds[target] ?: run {
                            fail("$datasetId: ${row.key} does not resolve to a passage")
                            continue
                        }
                        val number = ayahNumbers[row.key] ?: continue
                        insertMapping.setString(1, packId)
                        insertMapping.setInt(2, number)
                        insertMapping.setInt(3, passage)
                        insertMapping.addBatch()
                    }
                    insertMapping.executeBatch()
                }
            }
        }
    }

    private fun insertRecitations(
        connection: Connection,
        manifest: Manifest,
        ayahNumbers: Map<String, Int>,
        wordCounts: Map<Int, Int>,
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
                            val raw = rs.getString(4) ?: "[]"
                            val regions = RecitationSegments.parse(raw)
                            if (regions == null) {
                                fail("recitation ${dataset.id}: $surah:$ayah segments are not a readable word list")
                                insertAyah.setString(4, raw)
                            } else {
                                val correction = RecitationSegments.correct(
                                    raw,
                                    regions,
                                    wordCounts[number] ?: 0,
                                ) { source -> recitationPosition(surah, ayah, source) }
                                if (correction.shifted) recitationSegmentsShifted += 1
                                recitationSegmentsClamped += correction.clamped
                                recitationSegmentsMapped += correction.mapped
                                insertAyah.setString(4, correction.json)
                            }
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
        contentVersion: String,
        words: List<Word>,
        ayahs: Map<String, Int>,
        translations: Int,
    ): String {
        val markers = words.count { it.marker }
        return buildString {
            appendLine("{")
            appendLine("  \"contentVersion\": \"$contentVersion\",")
            appendLine("  \"ayahs\": ${ayahs.size},")
            appendLine("  \"words\": ${words.size - markers},")
            appendLine("  \"markers\": $markers,")
            appendLine("  \"translations\": $translations,")
            appendLine("  \"recitationSegmentsShifted\": $recitationSegmentsShifted,")
            appendLine("  \"recitationSegmentsMapped\": $recitationSegmentsMapped,")
            appendLine("  \"recitationSegmentsClamped\": $recitationSegmentsClamped,")
            appendLine("  \"problems\": ${problems.size},")
            appendLine("  \"databaseBytes\": ${database.length()},")
            appendLine("  \"databaseSha256\": \"${sha256(database)}\"")
            appendLine("}")
        }
    }
}
