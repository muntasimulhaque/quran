package io.github.muntasimulhaque.quran.tools

import java.awt.Font
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tafsir's Arabic face is a prose face, not the Quran's Hafs face.
 *
 * Hafs is the right face for the Book itself, but its cmap sends U+060C,
 * U+061B, U+061F, U+066A to U+066C, and U+06D4 to the Arabic footnote
 * marker, so a comma drew as a dotted circle (owner report, the audio and
 * type audit session). Kitab is the body Naskh the tafsir reads in now, and
 * these are the marks a tafsir cannot lose: the prose punctuation, the
 * honorific, the ornate parentheses, and the alef with madda. A future font
 * swap that drops one of them fails here before it can reach a reader.
 */
class KitabFontTest {

    private val font = Font.createFont(
        Font.TRUETYPE_FONT,
        File(root(), "content-assets/src/main/res/font/kitab_regular.ttf"),
    )

    @Test
    fun prosePunctuationIsCarried() {
        for (codepoint in PROSE_PUNCTUATION) {
            assertTrue(
                "Kitab cannot draw U+${codepoint.toString(16).uppercase()}, " +
                    "the prose punctuation Hafs draws as a dotted circle",
                font.canDisplay(codepoint),
            )
        }
    }

    @Test
    fun theTafsirsOwnMarksAreCarried() {
        for (codepoint in TAFSIR_MARKS) {
            assertTrue(
                "Kitab cannot draw U+${codepoint.toString(16).uppercase()}",
                font.canDisplay(codepoint),
            )
        }
    }

    private fun root(): File {
        var directory = File(".").absoluteFile
        while (!File(directory, "settings.gradle.kts").isFile) {
            directory = directory.parentFile
                ?: error("no repository root above ${File(".").absolutePath}")
        }
        return directory
    }

    private companion object {
        /** The punctuation Hafs sends to its Arabic footnote marker. */
        val PROSE_PUNCTUATION = listOf(0x060C, 0x061B, 0x061F, 0x066A, 0x066B, 0x066C, 0x06D4)

        /** The ligature, the parentheses, and the alef the content carries. */
        val TAFSIR_MARKS = listOf(0x0622, 0xFD3E, 0xFD3F, 0xFDFA)
    }
}
