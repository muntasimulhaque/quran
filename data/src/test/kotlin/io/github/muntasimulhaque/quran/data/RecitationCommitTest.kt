package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RecitationCommitTest {

    @Test
    fun `a second surah joins the first instead of replacing it`() {
        val imported = tempDirectory()
        try {
            commit(imported, "one", "001001.mp3", "001002.mp3")
            commit(imported, "two", "002001.mp3", "002002.mp3")

            assertEquals(
                listOf("001001.mp3", "001002.mp3", "002001.mp3", "002002.mp3"),
                File(imported, "husary").list()!!.sorted(),
            )
        } finally {
            imported.deleteRecursively()
        }
    }

    @Test
    fun `a repeated surah replaces its own files and leaves the others`() {
        val imported = tempDirectory()
        try {
            commit(imported, "old", "001001.mp3")
            commit(imported, "two", "002001.mp3")
            commit(imported, "new", "001001.mp3")

            val folder = File(imported, "husary")
            assertEquals(listOf("001001.mp3", "002001.mp3"), folder.list()!!.sorted())
            assertEquals("new", File(folder, "001001.mp3").readText())
            assertEquals("two", File(folder, "002001.mp3").readText())
        } finally {
            imported.deleteRecursively()
        }
    }

    @Test
    fun `a package with no files moves nothing`() {
        val imported = tempDirectory()
        try {
            commit(imported, "one", "001001.mp3")
            val staged = File(Files.createTempDirectory(imported.toPath(), "empty").toFile(), "husary")
            staged.mkdirs()
            var failed = false
            try {
                RecitationCommit.commit(imported, "husary", staged)
            } catch (expected: IllegalStateException) {
                failed = true
            }

            assertTrue(failed)
            assertEquals(listOf("001001.mp3"), File(imported, "husary").list()!!.sorted())
        } finally {
            imported.deleteRecursively()
        }
    }

    /** Stages [names] as one package with [content] and commits it. */
    private fun commit(imported: File, content: String, vararg names: String) {
        val staged = File(Files.createTempDirectory(imported.toPath(), "stage").toFile(), "husary")
        staged.mkdirs()
        for (name in names) File(staged, name).writeText(content)
        RecitationCommit.commit(imported, "husary", staged)
    }

    private fun tempDirectory(): File =
        Files.createTempDirectory("recitation-commit").toFile()
}
