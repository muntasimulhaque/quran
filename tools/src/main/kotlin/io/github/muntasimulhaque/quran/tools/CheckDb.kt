package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
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
        Expectation("translation", 6236),
        Expectation("tafsir_ayah", 12_472),
        Expectation("recitation_ayah", 24_944),
        Expectation("surah_info", 114),
        Expectation("pack", 3),
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
            // silent hole when that pack is chosen.
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
        }
        if (failures != 0) {
            println("checkdb: $failures problem(s)")
            return 1
        }
        println("checkdb: content/quran.db verified, ${database.length()} bytes, $actualHash")
        return 0
    }
}
