package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import io.github.muntasimulhaque.quran.core.TajweedCodec
import java.io.File

/**
 * Verifies the committed content database against the committed build report:
 * same size, same SHA-256, and the row counts the app relies on. This runs
 * without any raw sources, so a fresh clone and CI can prove the shipped
 * database is exactly the one the pipeline built.
 */
class CheckDb(private val root: File) {

    private data class Expectation(val table: String, val rows: Long)

    private val expectations = listOf(
        Expectation("surah", 114),
        Expectation("ayah", 6236),
        Expectation("word", 83_664),
        // The database carries the two recitations the app ships, Minshawi
        // and Husary, 6,236 rows each.
        Expectation("recitation_ayah", 12_472),
        Expectation("surah_info", 114),
    )

    fun run(): Int {
        val reportFile = File(root, "content/build-report.json")
        if (!reportFile.exists()) {
            println("checkdb: content/build-report.json is missing")
            return 2
        }
        val report = Json.parseToJsonElement(reportFile.readText()).jsonObject
        val database = File(root, "content/quran.db")
        if (!database.exists()) {
            println("checkdb: content/quran.db is missing; run ./gradlew :tools:run --args=fetch")
            return 2
        }
        var failures = 0
        val catalogFile = File(root, "content/catalog.json")
        val entries = if (catalogFile.exists()) {
            Json.parseToJsonElement(catalogFile.readText()).jsonObject["packs"]
                ?.jsonArray ?: JsonArray(emptyList())
        } else {
            JsonArray(emptyList())
        }
        // The segment gate follows the reciter packs the catalog actually
        // ships. The database carries the other exports the manifest lists,
        // and a source gap in one the app never installs is not a ship stop.
        val catalogReciters = entries
            .map { it.jsonObject }
            .filter { it["type"]?.jsonPrimitive?.content == "recitation" }
            .mapNotNull { it["id"]?.jsonPrimitive?.content?.removePrefix("reciter-") }
            .toSet()
        val expectedBytes = report["databaseBytes"]?.jsonPrimitive?.longOrNull
        val expectedHash = report["databaseSha256"]?.jsonPrimitive?.content
        if (expectedBytes != null && database.length() != expectedBytes) {
            println("checkdb: size ${database.length()} does not match $expectedBytes")
            failures++
        }
        val actualHash = sha256(database)
        if (expectedHash != null && actualHash != expectedHash) {
            println("checkdb: sha256 $actualHash does not match $expectedHash")
            failures++
        }
        // The pack gates read the reader's surah boundary from the core pack,
        // so the number they expect and the number the page draws come from
        // one table.
        val surahByAyah = HashMap<Int, Int>(6236)
        openSqlite(database).use { connection ->
            connection.each("SELECT number, surah FROM ayah") { rs ->
                surahByAyah[rs.getInt(1)] = rs.getInt(2)
            }
            for (expectation in expectations) {
                val rows = connection.count(expectation.table)
                if (rows != expectation.rows) {
                    println("checkdb: ${expectation.table} has $rows rows, expected ${expectation.rows}")
                    failures++
                }
            }
            var badTajweed = 0
            connection.each("SELECT number, text, tajweed FROM ayah") { rs ->
                val number = rs.getInt(1)
                val text = rs.getString(2) ?: ""
                val code = rs.getString(3) ?: ""
                if (code.length != text.length) {
                    println("checkdb: ayah $number has ${code.length} tajweed codes for ${text.length} characters")
                    badTajweed++
                } else if (!TajweedCodec.isWellFormed(code)) {
                    println("checkdb: ayah $number has an unknown tajweed code")
                    badTajweed++
                }
            }
            failures += badTajweed
            if (badTajweed == 0) println("checkdb: every ayah's tajweed code matches its text")
            failures += checkWordsAndMeanings(connection)
            failures += checkRecitationSegments(connection, catalogReciters)
            failures += checkBundledAudio(connection)
            // Every pack must cover every ayah, or the reader would meet a
            // silent hole when that pack is chosen. The number of packs is not
            // pinned: a pack is content, not structure, and the catalog below
            // is what proves the library is complete.
            val packs = mutableListOf<Pair<String, String>>()
            connection.each("SELECT id, type FROM pack ORDER BY id") { rs ->
                packs += rs.getString(1) to rs.getString(2)
            }
            for ((id, type) in packs) {
                val table = if (type == "translation") "translation" else "tafsir_ayah"
                val rows = connection.scalarLong("SELECT COUNT(*) FROM $table WHERE pack = ?", id)
                if (rows != 6236L) {
                    println("checkdb: pack $id covers $rows ayahs through $table, expected 6236")
                    failures++
                }
            }
            println("checkdb: ${packs.size} pack(s): " + packs.joinToString { it.first })
            failures += checkTranslationFootnotes(connection)

            // The catalog the app reads must describe exactly these packs, and
            // the pack files it points at must be the ones on this machine.
            if (catalogFile.exists()) {
                // The database carries the text packs (translations and tafsirs);
                // the catalog also names the core, the word lists, and reciters.
                val catalogIds = entries
                    .map { it.jsonObject }
                    .filter { it["type"]?.jsonPrimitive?.content in listOf("translation", "tafsir") }
                    .mapNotNull { it["id"]?.jsonPrimitive?.content }
                if (catalogIds.toSet() != packs.map { it.first }.toSet()) {
                    println(
                        "checkdb: the catalog names $catalogIds, the database has " +
                            packs.map { it.first },
                    )
                    failures++
                }
                for (entry in entries) {
                    val pack = entry.jsonObject
                    val id = pack["id"]?.jsonPrimitive?.content ?: continue
                    val file = File(root, "content/packs/$id.db")
                    val expectedHash = pack["sha256"]?.jsonPrimitive?.content
                    if (!file.exists()) {
                        println("checkdb: pack file content/packs/$id.db is missing")
                        failures++
                        continue
                    }
                    if (expectedHash != null && sha256(file) != expectedHash) {
                        println("checkdb: pack file content/packs/$id.db does not match the catalog hash")
                        failures++
                    }
                }
                println("checkdb: catalog matches, ${entries.size} pack file(s) verified")
            }
        }
        failures += checkSaheehPack(surahByAyah)
        failures += checkBayanPack(surahByAyah)
        if (failures != 0) {
            println("checkdb: $failures problem(s)")
            return 1
        }
        println("checkdb: content/quran.db verified, ${database.length()} bytes, $actualHash")
        return 0
    }

    /** The ayah-number rows in the QUL word lists, in either numeral set. */
    private val markerMeaning = Regex("^\\((?:(?:\\d+)|[\u0660-\u0669]+)\\)$")

    /**
     * The reading's two halves must meet: the ayah's text is its drawn words
     * joined by single spaces, and every drawn word has a meaning in both word
     * lists. The merge that joins two source words must carry its source
     * position into the meaning lookup, and no ayah-number row may become a
     * gloss (owner report, the QPC rebuild audit).
     */
    private fun checkWordsAndMeanings(connection: java.sql.Connection): Int {
        var failures = 0
        val joined = HashMap<Int, StringBuilder>()
        connection.each("SELECT ayah_number, text FROM word WHERE marker = 0 ORDER BY ayah_number, position") { rs ->
            val ayah = rs.getInt(1)
            val word = rs.getString(2) ?: ""
            val line = joined.getOrPut(ayah) { StringBuilder() }
            if (line.isNotEmpty()) line.append(' ')
            line.append(word)
        }
        connection.each("SELECT number, text FROM ayah ORDER BY number") { rs ->
            val number = rs.getInt(1)
            val text = rs.getString(2) ?: ""
            if (joined[number]?.toString() != text) {
                println("checkdb: ayah $number text does not join its word rows")
                failures++
            }
        }
        // The meaning list is read once into a set. The correlated NOT EXISTS
        // this replaced scanned the whole meaning table for every word row, so
        // a green gate took a quarter of an hour; the set is the same check in
        // one pass (owner report, the QPC rendering audit).
        val meanings = HashSet<String>(200_000)
        connection.each("SELECT ayah_number, position, language FROM word_meaning") { rs ->
            meanings += "${rs.getInt(1)}:${rs.getInt(2)}:${rs.getString(3)}"
        }
        for (language in listOf("en", "bn")) {
            var missing = 0
            connection.each("SELECT ayah_number, position FROM word WHERE marker = 0") { rs ->
                if ("${rs.getInt(1)}:${rs.getInt(2)}:$language" !in meanings) missing++
            }
            if (missing != 0) {
                println("checkdb: $missing drawn word(s) have no $language meaning")
                failures++
            }
        }
        var markersAsMeanings = 0
        connection.each("SELECT language, meaning FROM word_meaning") { rs ->
            val meaning = rs.getString(2)?.trim() ?: ""
            if (markerMeaning.matches(meaning)) {
                if (markersAsMeanings < 5) {
                    println("checkdb: ${rs.getString(1)} meaning is an ayah number: $meaning")
                }
                markersAsMeanings++
            }
        }
        if (markersAsMeanings != 0) failures += markersAsMeanings
        if (failures == 0) println("checkdb: every word joins its ayah and has a meaning in both lists")
        return failures
    }

    /**
     * The database's translation rows still in their source shape: the
     * Bangla rows number each ayah from 1, so every marker and every note
     * must run 1..n. The Saheeh rows keep the publisher's book-wide
     * numbering. The reader's surah numbering is written into the pack files,
     * so it is checked there by [checkSaheehPack] and [checkBayanPack].
     */
    private fun checkTranslationFootnotes(connection: java.sql.Connection): Int {
        var failures = 0
        var ayahs = 0
        var notes = 0
        connection.each(
            "SELECT pack, ayah_number, text, footnotes FROM translation " +
                "WHERE pack <> 'translation-saheeh-en' ORDER BY pack, ayah_number",
        ) { rs ->
            ayahs++
            val pack = rs.getString(1) ?: ""
            val number = rs.getInt(2)
            val text = rs.getString(3) ?: ""
            val noteNumbers = runCatching {
                Json.parseToJsonElement(rs.getString(4) ?: "[]").jsonArray.map {
                    it.jsonObject["n"]?.jsonPrimitive?.content?.toIntOrNull()
                        ?: error("a note has no number")
                }
            }.getOrElse {
                println("checkdb: $pack $number has unreadable footnotes")
                failures++
                emptyList()
            }
            val markerNumbers = Regex("\\[(\\d+)]").findAll(text)
                .map { it.groupValues[1].toInt() }
                .toList()
            if (markerNumbers != (1..noteNumbers.size).toList()) {
                println("checkdb: $pack $number draws $markerNumbers for ${noteNumbers.size} note(s)")
                failures++
            }
            if (noteNumbers != (1..noteNumbers.size).toList()) {
                println("checkdb: $pack $number numbers its notes $noteNumbers")
                failures++
            }
            notes += noteNumbers.size
        }
        if (failures == 0) println("checkdb: $ayahs translation ayahs, $notes notes, all numbered within their ayah")
        return failures
    }

    /**
     * One translation pack's notes climb through each surah and start the
     * next surah at one. [surahOf] is the core pack's ayah-to-surah map, so
     * the gate reads the boundary the reader reads. The function reports its
     * own failures; the caller names the pack and its extra rules.
     */
    private fun checkSurahNumberedPack(
        file: File,
        label: String,
        expectedNotes: Int,
        surahOf: Map<Int, Int>,
    ): Int {
        var failures = 0
        var ayahs = 0
        var notes = 0
        var surah = 0
        var surahNotes = 0
        openSqlite(file).use { connection ->
            connection.each("SELECT ayah_number, text, footnotes FROM translation ORDER BY ayah_number") { rs ->
                ayahs++
                val number = rs.getInt(1)
                val text = rs.getString(2) ?: ""
                val noteNumbers = runCatching {
                    Json.parseToJsonElement(rs.getString(3) ?: "[]").jsonArray.map {
                        it.jsonObject["n"]?.jsonPrimitive?.content?.toIntOrNull()
                            ?: error("a note has no number")
                    }
                }.getOrElse {
                    println("checkdb: $label pack $number has unreadable footnotes")
                    failures++
                    emptyList()
                }
                val markerNumbers = Regex("\\[(\\d+)]").findAll(text)
                    .map { it.groupValues[1].toInt() }
                    .toList()
                val ayahSurah = surahOf[number]
                if (ayahSurah == null) {
                    println("checkdb: $label pack ayah $number is not in the core pack")
                    failures++
                    return@each
                }
                if (ayahSurah != surah) {
                    surah = ayahSurah
                    surahNotes = 0
                }
                val expected = (surahNotes + 1..surahNotes + noteNumbers.size).toList()
                if (markerNumbers != expected) {
                    println(
                        "checkdb: $label pack $number draws $markerNumbers " +
                            "for ${noteNumbers.size} note(s), expected $expected",
                    )
                    failures++
                }
                if (noteNumbers != expected) {
                    println(
                        "checkdb: $label pack $number numbers its notes $noteNumbers, " +
                            "expected $expected",
                    )
                    failures++
                }
                surahNotes += noteNumbers.size
                notes += noteNumbers.size
            }
            if (ayahs != 6236) {
                println("checkdb: $label pack has $ayahs ayahs, expected 6236")
                failures++
            }
            if (notes != expectedNotes) {
                println("checkdb: $label pack carries $notes notes, expected $expectedNotes")
                failures++
            }
        }
        return failures
    }

    /**
     * The Saheeh pack is the reader's shape: each surah's notes run from 1
     * and reset at the next surah, 3:32 reads the published "they", and
     * 7:143 no longer draws the marker whose note the export lost. The pack
     * file is what the app installs, so this is the gate that proves the
     * reader's page, not the database's source shape.
     */
    private fun checkSaheehPack(surahOf: Map<Int, Int>): Int {
        val file = File(root, "content/packs/translation-saheeh-en.db")
        if (!file.exists()) return 0
        var failures = checkSurahNumberedPack(file, "Saheeh", 1903, surahOf)
        openSqlite(file).use { connection ->
            val first = connection.scalarLong(
                "SELECT COUNT(*) FROM translation WHERE ayah_number = 1 " +
                    "AND text LIKE '%[1]%' AND text LIKE '%[2]%'",
            )
            if (first != 1L) {
                println("checkdb: Saheeh pack 1:1 does not start its notes at 1")
                failures++
            }
            val corrected = connection.scalarLong(
                "SELECT COUNT(*) FROM translation WHERE ayah_number = 325 " +
                    "AND text LIKE '%if they turn away%' AND text NOT LIKE '%if you turn away%'",
            )
            if (corrected != 1L) {
                println("checkdb: Saheeh pack 3:32 does not carry the published reading")
                failures++
            }
            val dead = connection.scalarLong(
                "SELECT COUNT(*) FROM translation WHERE ayah_number = 1097 AND text LIKE '%[403]%'",
            )
            if (dead != 0L) {
                println("checkdb: Saheeh pack 7:143 still draws the source marker with no note")
                failures++
            }
        }
        if (failures == 0) {
            println("checkdb: the Saheeh pack numbers 1903 notes within their surahs, 3:32 corrected")
        }
        return failures
    }

    /**
     * The Bangla pack is the reader's shape too: each surah's notes run from
     * 1 and reset at the next surah. The database keeps the QUL export's
     * per-ayah numbers; the pack is what the app installs.
     */
    private fun checkBayanPack(surahOf: Map<Int, Int>): Int {
        val file = File(root, "content/packs/translation-al-bayan-bn.db")
        if (!file.exists()) return 0
        val failures = checkSurahNumberedPack(file, "Bangla", 206, surahOf)
        if (failures == 0) {
            println("checkdb: the Bangla pack numbers 206 notes within their surahs")
        }
        return failures
    }

    /**
     * Every region the app reads must name a word the reading draws, and every
     * drawn word must have at least one region. The build folds QUL's leading
     * fragment and its ayah number into that range; this gate is what proves it
     * did, on the database alone and without the raw sources.
     */
    private fun checkRecitationSegments(connection: java.sql.Connection, reciters: Set<String>): Int {
        if (reciters.isEmpty()) {
            println("checkdb: no recitation packs in the catalog; segments not checked")
            return 0
        }
        var failures = 0
        var rows = 0
        val seen = HashMap<String, Int>(reciters.size)
        val placeholders = reciters.joinToString(",") { "?" }
        val sql = "SELECT ra.recitation, ra.ayah_number, ra.segments, " +
            "(SELECT COUNT(*) FROM word w WHERE w.ayah_number = ra.ayah_number AND w.marker = 0) " +
            "FROM recitation_ayah ra WHERE ra.recitation IN ($placeholders) " +
            "ORDER BY ra.recitation, ra.ayah_number"
        connection.prepareStatement(sql).use { statement ->
            reciters.forEachIndexed { index, reciter -> statement.setString(index + 1, reciter) }
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    rows++
                    val recitation = rs.getString(1)
                    seen[recitation] = (seen[recitation] ?: 0) + 1
                    val ayah = rs.getInt(2)
                    val regions = RecitationSegments.parse(rs.getString(3) ?: "")
                    val words = rs.getInt(4)
                    if (regions == null) {
                        println("checkdb: $recitation $ayah has unreadable segments")
                        failures++
                        continue
                    }
                    if (words <= 0) {
                        println("checkdb: $recitation $ayah has no drawn words")
                        failures++
                        continue
                    }
                    val covered = HashSet<Int>(words)
                    var outside = false
                    for (region in regions) {
                        if (region.wordTo !in 1..words) {
                            println("checkdb: $recitation $ayah has word ${region.wordTo} outside 1..$words")
                            failures++
                            outside = true
                            break
                        }
                        covered += region.wordTo
                    }
                    if (outside) continue
                    val missing = (1..words).filterNot { it in covered }
                    if (missing.isNotEmpty()) {
                        println("checkdb: $recitation $ayah has no region for word(s) ${missing.take(5)}")
                        failures++
                    }
                }
            }
        }
        for (reciter in reciters) {
            val count = seen[reciter] ?: 0
            if (count != 6236) {
                println("checkdb: $reciter has $count recitation rows, expected 6236")
                failures++
            }
        }
        if (failures == 0) println("checkdb: $rows recitation rows, every word in range and covered")
        return failures
    }

    /**
     * The app's bundled Al-Fatiha files must be exactly the surah 1 paths the
     * database names, for every reciter, and nothing else. A missing file is a
     * first Play that falls back to a download; an extra file is content the
     * app carries without the database saying so.
     */
    private fun checkBundledAudio(connection: java.sql.Connection): Int {
        val expected = mutableSetOf<String>()
        connection.each(
            "SELECT ra.audio_path FROM recitation_ayah ra " +
                "JOIN ayah a ON a.number = ra.ayah_number " +
                "WHERE a.surah = 1 ORDER BY ra.recitation, a.ayah",
        ) { rs -> expected += rs.getString(1) ?: "" }
        val bundle = File(root, "content/bundled/audio/recitation")
        val found = bundle.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(bundle).invariantSeparatorsPath }
            .toSet()
        if (expected != found) {
            println("checkdb: bundled audio has $found, the database names $expected")
            return 1
        }
        println("checkdb: ${found.size} bundled ayah file(s) match the database's surah 1 paths")
        return 0
    }
}
