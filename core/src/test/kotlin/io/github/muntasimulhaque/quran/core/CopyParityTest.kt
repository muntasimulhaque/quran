package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The house rule that a sentence is written once in each language, wherever a
 * module draws it.
 *
 * Each module keeps its own `strings.xml`, because the module that draws a
 * sentence owns its words. Two modules drawing the same English sentence is
 * therefore not a mistake on its own: "Retry", "Saved", and "Makki" each stand
 * in three or four modules, and each module names its own. What is a mistake is
 * the two copies drifting in Bangla, where a reader meets the same English word
 * as two different Bangladeshi words: the note on a saved ayah was `নোট` in the
 * reading and `টীকা` in search, which is the word for a commentary (owner
 * decision, 19.6). The gate reads every module's two files and holds them to
 * one answer, and no exception stands: the one phrase that used to be allowed
 * apart now reads the same in both modules, so the list of differences the
 * app may say twice on purpose is empty.
 */
class CopyParityTest {

    @Test
    fun everyTranslatableStringHasItsBangla() {
        val missing = mutableListOf<String>()
        val modules = englishFiles()
        assertTrue(
            "no module's strings were found under the repository root",
            modules.isNotEmpty() && modules.any { read(it).containsKey("app_name") },
        )
        for (english in modules) {
            val bangla = read(banglaBeside(english))
            for ((name, entry) in read(english)) {
                if (entry.translatableFalse) continue
                if (bangla[name] == null) missing += where(english) + ":" + name
            }
        }
        assertTrue(
            "a translatable sentence with no Bangla twin: " + missing.joinToString(", "),
            missing.isEmpty(),
        )
    }

    @Test
    fun aBanglaStringNamesAnEnglishOne() {
        val orphans = mutableListOf<String>()
        for (bangla in banglaFiles()) {
            val english = read(englishBeside(bangla))
            for (name in read(bangla).keys) {
                if (english[name] == null) orphans += where(bangla) + ":" + name
            }
        }
        assertTrue(
            "a Bangla sentence no English one names: " + orphans.joinToString(", "),
            orphans.isEmpty(),
        )
    }

    @Test
    fun oneEnglishLiteralReadsTheSameInBanglaEverywhere() {
        val readings = mutableMapOf<String, MutableMap<String, String>>()
        for (english in englishFiles()) {
            val bangla = read(banglaBeside(english))
            for ((name, entry) in read(english)) {
                val twin = bangla[name] ?: continue
                val key = where(english) + ":" + name
                readings.getOrPut(entry.text) { mutableMapOf() }[key] = twin.text
            }
        }
        val offenders = mutableListOf<String>()
        for ((literal, byName) in readings) {
            if (byName.values.distinct().size <= 1) continue
            offenders += literal + " -> " + byName.entries.joinToString(" | ") { it.key + " = " + it.value }
        }
        assertTrue(
            "one English sentence read two ways in Bangla: " + offenders.joinToString(" ;; "),
            offenders.isEmpty(),
        )
    }

    private data class Entry(val text: String, val translatableFalse: Boolean)

    private fun read(file: File?): Map<String, Entry> {
        if (file == null || !file.isFile) return emptyMap()
        val text = runCatching { file.readText() }.getOrNull() ?: return emptyMap()
        return STRING.findAll(text).associate { match ->
            match.groupValues[1] to Entry(
                text = match.groupValues[3].trim(),
                translatableFalse = match.groupValues[2].contains("translatable=\"false\""),
            )
        }
    }

    private fun englishFiles(): List<File> = resources().filter { it.parentFile.name == "values" }

    private fun banglaFiles(): List<File> = resources().filter { it.parentFile.name == "values-bn" }

    private fun banglaBeside(english: File): File =
        File(english.parentFile.parentFile, "values-bn/strings.xml")

    private fun englishBeside(bangla: File): File =
        File(bangla.parentFile.parentFile, "values/strings.xml")

    private fun resources(): List<File> =
        repositoryRoot().walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRECTORIES }
            .filter { it.isFile && it.name == "strings.xml" && isModuleResource(it) }
            .toList()

    /** True for a module's own `src/main/res/values(-bn)` file and nothing else. */
    private fun isModuleResource(file: File): Boolean {
        val path = file.path.replace('\\', '/')
        return path.contains("/src/main/res/values") &&
            !path.contains("/build/") &&
            (file.parentFile.name == "values" || file.parentFile.name == "values-bn")
    }

    /** The module that owns the file, which is the name its copies are read by. */
    private fun where(file: File): String {
        val path = file.path.replace('\\', '/')
        return File(path.substringBefore("/src/main/")).name
    }

    /** The module this test runs in sits one level under the repository root. */
    private fun repositoryRoot(): File {
        val working = File(System.getProperty("user.dir") ?: ".")
        val root = if (working.name == "core") working.parentFile ?: working else working
        assertTrue("the repository root was not found from $working", root.isDirectory)
        return root
    }

    private companion object {
        val STRING = Regex("<string name=\"([^\"]+)\"([^>]*)>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)

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
    }
}
