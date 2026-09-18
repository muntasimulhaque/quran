package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The house rule is that no em dash appears anywhere in the repository, so a
 * release note, a comment, or a string can never carry one into a store page
 * or a screenshot. The character is written here as an escape, so this file
 * is not its own offender.
 */
class NoEmDashTest {

    private val emDash = '\u2014'

    @Test
    fun noEmDashInAnyTextFile() {
        val root = repositoryRoot()
        val offenders = mutableListOf<String>()
        root.walkTopDown()
            .onEnter { directory -> directory.name !in SKIPPED_DIRECTORIES }
            .filter { file -> file.isFile && file.extension.lowercase() in TEXT_EXTENSIONS }
            .forEach { file ->
                val text = runCatching { file.readText() }.getOrNull() ?: return@forEach
                if (text.contains(emDash)) {
                    offenders += file.relativeTo(root).path
                }
            }
        assertTrue(
            "an em dash was found in: " + offenders.joinToString(", "),
            offenders.isEmpty(),
        )
    }

    /** The module this test runs in sits one level under the repository root. */
    private fun repositoryRoot(): File {
        val working = File(System.getProperty("user.dir") ?: ".")
        val root = if (working.name == "core") working.parentFile ?: working else working
        assertTrue("the repository root was not found from $working", root.isDirectory)
        return root
    }

    private companion object {
        val SKIPPED_DIRECTORIES = setOf(
            ".git",
            ".gradle",
            ".kotlin",
            "build",
            "node_modules",
            "raw",
            "work",
            "packs",
            "screenshots",
        )

        val TEXT_EXTENSIONS = setOf(
            "kt", "kts", "md", "xml", "json", "yml", "yaml", "html", "css", "js", "toml",
            "properties", "txt", "prof", "pro", "gradle", "gitignore", "gitattributes",
        )
    }
}
