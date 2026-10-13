package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The sacred text is content, and content only comes through `tools/`. The
 * basmalah at the head of every surah used to be a Kotlin literal, and it was
 * one mark away from the audited text: the reader met a basmalah the audit
 * never verified. This gate is what keeps that from happening again: no
 * Quranic string literal, escaped or not, can live in the interface modules.
 *
 * The ornament furniture is not text and is not scanned: the end-of-ayah
 * roundel and its digits live in `core/Arabic.kt`, they are drawn as
 * furniture, and they carry no verse. The scan reads string literals only,
 * so a KDoc example written in Arabic is not an offender either; what it
 * catches is a verse, or a piece of one, compiled into the app.
 */
class NoArabicTextTest {

    @Test
    fun noQuranTextInTheInterfaceModules() {
        val root = repositoryRoot()
        val offenders = mutableListOf<String>()
        for (module in MODULES) {
            val directory = File(root, module)
            if (!directory.isDirectory) continue
            directory.walkTopDown()
                .filter { file -> file.isFile && file.extension == "kt" }
                .forEach { file ->
                    val text = runCatching { file.readText() }.getOrNull() ?: return@forEach
                    if (text.carriesArabicText()) {
                        offenders += file.relativeTo(root).path
                    }
                }
        }
        assertTrue(
            "Quranic text was found in the interface code: " + offenders.joinToString(", "),
            offenders.isEmpty(),
        )
    }

    /**
     * Whether a Kotlin source carries Arabic inside a string literal. Line
     * and block comments are skipped, so documentation may quote Arabic; the
     * scan only cares about what the compiler would put in the app.
     */
    private fun String.carriesArabicText(): Boolean {
        var i = 0
        while (i < length) {
            val c = this[i]
            when {
                c == '/' && i + 1 < length && this[i + 1] == '/' -> {
                    while (i < length && this[i] != '\n') i++
                }
                c == '/' && i + 1 < length && this[i + 1] == '*' -> {
                    i += 2
                    while (i + 1 < length && !(this[i] == '*' && this[i + 1] == '/')) i++
                    i += 2
                }
                c == '"' -> {
                    i++
                    // A single letter is a sample, not a text: the settings
                    // page draws one to show the Arabic size, and the gate
                    // is about words and verses. Two or more Arabic
                    // characters together is text, and text belongs to the
                    // content.
                    var arabic = 0
                    while (i < length && this[i] != '"' && this[i] != '\n') {
                        if (this[i] == '\\' && i + 5 < length && this[i + 1] == 'u') {
                            val hex = substring(i + 2, i + 6)
                            if (hex.startsWith("06") && hex.all { it.isHexDigit() }) {
                                arabic++
                                if (arabic >= 2) return true
                            }
                            i += 6
                            continue
                        }
                        val codePoint = codePointAt(i)
                        if (codePoint in 0x0600..0x06FF) {
                            arabic++
                            if (arabic >= 2) return true
                        }
                        i += Character.charCount(codePoint)
                    }
                    i++
                }
                else -> i++
            }
        }
        return false
    }

    private fun Char.isHexDigit(): Boolean =
        isDigit() || this in 'a'..'f' || this in 'A'..'F'

    /** The module this test runs in sits one level under the repository root. */
    private fun repositoryRoot(): File {
        val working = File(System.getProperty("user.dir") ?: ".")
        val root = if (working.name == "core") working.parentFile ?: working else working
        assertTrue("the repository root was not found from $working", root.isDirectory)
        return root
    }

    private companion object {
        /**
         * The modules that draw the app. `tools/` and `benchmark/` are
         * deliberately out of scope: the pipeline works with the sources'
         * own text, and the benchmark never ships.
         */
        val MODULES = listOf(
            "app/src/main/kotlin",
            "feature-browse/src/main/kotlin",
            "feature-playback/src/main/kotlin",
            "feature-reading/src/main/kotlin",
            "feature-search/src/main/kotlin",
            "feature-settings/src/main/kotlin",
            "ui-kit/src/main/kotlin",
        )
    }
}
