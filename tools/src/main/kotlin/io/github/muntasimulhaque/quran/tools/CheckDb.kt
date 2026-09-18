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
        Expectation("page_line", 9046),
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
            val catalogFile = File(root, "content/catalog.json")
            if (catalogFile.exists()) {
                val entries = Json.parseToJsonElement(catalogFile.readText()).jsonObject["packs"]
                    ?.jsonArray ?: JsonArray(emptyList())
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
}
