package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
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
        Expectation("word", 83_668),
        Expectation("recitation_ayah", 24_944),
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
        openSqlite(database).use { connection ->
            for (expectation in expectations) {
                val rows = connection.count(expectation.table)
                if (rows != expectation.rows) {
                    println("checkdb: ${expectation.table} has $rows rows, expected ${expectation.rows}")
                    failures++
                }
            }
            failures += checkRecitationSegments(connection, catalogReciters)
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
        if (failures != 0) {
            println("checkdb: $failures problem(s)")
            return 1
        }
        println("checkdb: content/quran.db verified, ${database.length()} bytes, $actualHash")
        return 0
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
}
