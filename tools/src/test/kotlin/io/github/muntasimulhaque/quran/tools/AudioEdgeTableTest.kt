package io.github.muntasimulhaque.quran.tools

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gate on the voice edge table.
 *
 * Every recitation row in the content database must have exactly one line, the
 * line must name the same ayah, and the two numbers must describe a file that
 * can be played: the first sound before the last, and both inside the file's
 * own duration. A table that misses a row would leave that ayah on the old
 * reading, which is a quiet regression; this test makes it a red build.
 */
class AudioEdgeTableTest {

    private val root = projectRoot()

    @Test
    fun `every recitation ayah has one measured edge`() {
        val table = File(root, "content/audio-edges.txt")
        assertTrue("content/audio-edges.txt is missing; run tools audio edges", table.isFile)
        val edges = table.readLines()
            .filter { line -> line.isNotBlank() && !line.startsWith("#") }
            .map { line ->
                val parts = line.split(' ', '\t').filter { it.isNotEmpty() }
                assertEquals("a table line has five fields: $line", 5, parts.size)
                val reciter = parts[0]
                val ayah = parts[1].toIntOrNull() ?: error("not an ayah: $line")
                val first = parts[2].toLongOrNull() ?: error("not a first edge: $line")
                val last = parts[3].toLongOrNull() ?: error("not a last edge: $line")
                val duration = parts[4].toLongOrNull() ?: error("not a duration: $line")
                assertTrue("$line: the first sound is before zero", first >= 0)
                assertTrue("$line: the last sound is before the first", last > first)
                assertTrue("$line: the last sound is past the file", last <= duration)
                assertTrue("$line: the first sound is past the head windows", first <= 60_000)
                Edge(reciter, ayah, first, last, duration)
            }

        val rows = mutableSetOf<Pair<String, Int>>()
        openSqlite(File(root, "content/quran.db")).use { connection ->
            connection.each("SELECT recitation, ayah_number FROM recitation_ayah") { rs ->
                rows += (rs.getString(1) ?: "") to rs.getInt(2)
            }
        }
        val keys = edges.map { it.reciter to it.ayah }.toSet()
        assertEquals("the table and the recitation rows name different ayahs", rows, keys)
        assertEquals("the table must be one line per recitation and ayah", 12_472, edges.size)
    }

    private data class Edge(
        val reciter: String,
        val ayah: Int,
        val first: Long,
        val last: Long,
        val duration: Long,
    )

    private fun projectRoot(): File {
        var directory = File(".").canonicalFile
        while (directory.parentFile != null && !File(directory, "content/quran.db").isFile) {
            directory = directory.parentFile ?: break
        }
        return directory
    }
}
