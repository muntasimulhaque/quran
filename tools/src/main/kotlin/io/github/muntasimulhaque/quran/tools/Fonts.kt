package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.TextBlockKind
import io.github.muntasimulhaque.quran.core.TextRun
import java.awt.Font
import java.io.File
import java.util.TreeMap

/**
 * Proves that the bundled fonts can draw the text they will be asked to draw.
 *
 * Two checks, both mandatory:
 * - every non-digit codepoint of the canonical Unicode text is drawable by
 *   the KFGQPC Uthmanic Hafs font used in study mode;
 * - every codepoint of every word's glyph sequence is drawable by that page's
 *   own font, so a Mushaf page can never show tofu or a missing word.
 */
class Fonts(private val root: File) {

    private val verify = File(root, "content/work/verify")
    private val problems = mutableListOf<String>()

    fun run(): Int {
        checkUnicodeCoverage()
        checkGlyphCoverage()
        checkReadingFonts()
        if (problems.isEmpty()) {
            println("fonts: all coverage checks passed")
            return 0
        }
        println("FONT FAILURES (${problems.size})")
        problems.forEach { println("  - $it") }
        return 1
    }

    private fun checkUnicodeCoverage() {
        val hafs = loadFont(File(verify, "quran-font-hafs"))
        val uncovered = TreeMap<Int, Int>()
        var codepoints = 0
        var digits = 0
        openSqlite(sourceDb("quran-script-kfgqpc")).use { connection ->
            connection.each("SELECT text FROM words") { rs ->
                for (cp in (rs.getString(1) ?: "").codePoints()) {
                    if (Character.isDigit(cp)) {
                        digits++
                        continue
                    }
                    codepoints++
                    if (!hafs.canDisplay(cp)) uncovered[cp] = (uncovered[cp] ?: 0) + 1
                }
            }
        }
        if (uncovered.isNotEmpty()) {
            val sample = uncovered.entries.take(12).joinToString(" ") {
                "U+${it.key.toString(16).uppercase()}(x${it.value})"
            }
            problems += "study font cannot draw ${uncovered.size} codepoints: $sample"
        }
        println("fonts: study text codepoints checked: $codepoints, digits skipped: $digits")
    }

    private fun checkGlyphCoverage() {
        val wordsByPage = wordsByPage()
        var pages = 0
        var codepoints = 0
        for ((page, words) in wordsByPage) {
            val fontFile = File(verify, "mushaf-fonts-v2").walkTopDown()
                .firstOrNull { it.isFile && it.name == "p$page.ttf" }
                ?: run {
                    problems += "page $page has no font file"
                    continue
                }
            val font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
            val uncovered = mutableSetOf<Int>()
            for (text in words) {
                for (cp in text.codePoints()) {
                    codepoints++
                    if (!font.canDisplay(cp)) uncovered += cp
                }
            }
            if (uncovered.isNotEmpty()) {
                val sample = uncovered.take(8).joinToString(" ") { "U+${it.toString(16).uppercase()}" }
                problems += "page $page font cannot draw ${uncovered.size} codepoints: $sample"
            }
            pages++
        }
        println("fonts: mushaf pages checked: $pages, glyph codepoints: $codepoints")
    }

    private fun wordsByPage(): Map<Int, List<String>> {
        val pageOfWord = HashMap<Int, Int>(90_000)
        openSqlite(sourceDb("mushaf-layout-v2")).use { connection ->
            connection.each(
                "SELECT page_number, first_word_id, last_word_id FROM pages WHERE line_type='ayah'",
            ) { rs ->
                val page = rs.getInt(1)
                for (id in rs.getInt(2)..rs.getInt(3)) pageOfWord[id] = page
            }
        }
        val out = HashMap<Int, MutableList<String>>(700)
        openSqlite(sourceDb("mushaf-glyph-v2")).use { connection ->
            connection.each("SELECT id, text FROM words") { rs ->
                val page = pageOfWord[rs.getInt(1)] ?: return@each
                out.getOrPut(page) { mutableListOf() }.add(rs.getString(2) ?: "")
            }
        }
        return out
    }

    private fun loadFont(directory: File): Font {
        val file = directory.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".ttf") }
            ?: error("no ttf under ${directory.absolutePath}")
        return Font.createFont(Font.TRUETYPE_FONT, file)
    }

    /**
     * The app draws four voices: Inter for interface and headings, Literata for
     * English reading, Amiri Quran for Arabic outside the Mushaf, and the Hafs
     * font for the canonical words already checked above. Every run the reader
     * will actually see is probed against the font it will be drawn with,
     * script by script, so a tofu box can never reach a screen.
     */
    private fun checkReadingFonts() {
        val database = File(root, "content/quran.db")
        if (!database.exists()) {
            problems += "content/quran.db is missing; run build before fonts"
            return
        }
        val literata = loadFontFile(File(root, "app/src/main/res/font/literata_variable.ttf"))
        val amiri = loadFontFile(File(root, "app/src/main/res/font/amiri_quran.ttf"))
        val hafs = loadFontFile(File(root, "content/work/fonts-hafs/UthmanicHafs_V22.ttf"))
        val inter = loadFontFile(File(root, "app/src/main/res/font/inter_variable.ttf"))

        val uncovered = TreeMap<String, Int>()
        var codepoints = 0

        fun probe(font: Font, text: String, label: String) {
            for (codepoint in text.codePoints()) {
                if (codepoint <= 32) continue
                codepoints++
                if (!font.canDisplay(codepoint)) {
                    uncovered.merge("$label U+${codepoint.toString(16).uppercase()}", 1, Int::plus)
                }
            }
        }

        fun probeRuns(runs: List<TextRun>, latin: Font, label: String) {
            for (run in runs) {
                if (run.marker != null) continue
                if (!run.arabic) {
                    probe(latin, run.text, label)
                    continue
                }
                // Arabic is drawn in Amiri Quran, and a letter Amiri does not
                // have falls back to the bundled Hafs font, exactly as the app
                // does it. Anything neither font can draw fails the gate.
                for (codepoint in run.text.codePoints()) {
                    if (codepoint <= 32) continue
                    codepoints++
                    if (!amiri.canDisplay(codepoint) && !hafs.canDisplay(codepoint)) {
                        uncovered.merge("$label U+${codepoint.toString(16).uppercase()}", 1, Int::plus)
                    }
                }
            }
        }

        openSqlite(database).use { connection ->
            connection.each("SELECT text, footnotes FROM translation") { rs ->
                probeRuns(RichText.footnotes(rs.getString(1) ?: ""), literata, "translation")
                probeRuns(RichText.runs(rs.getString(2) ?: ""), literata, "footnote")
            }
            connection.each("SELECT text FROM tafsir_passage WHERE source='ibn-kathir'") { rs ->
                for (block in RichText.parseHtml(rs.getString(1) ?: "")) {
                    val latin = if (block.kind == TextBlockKind.HEADING) inter else literata
                    probeRuns(block.runs, latin, "ibn-kathir")
                }
            }
            connection.each("SELECT text FROM tafsir_passage WHERE source='as-sadi'") { rs ->
                probeRuns(RichText.quotes(rs.getString(1) ?: ""), amiri, "as-sadi")
            }
            connection.each("SELECT translation FROM word WHERE marker=0") { rs ->
                val meaning = rs.getString(1)
                if (!meaning.isNullOrBlank()) probe(literata, meaning, "word")
            }
            connection.each("SELECT name_simple, name_latin FROM surah") { rs ->
                probe(inter, rs.getString(1) ?: "", "surah")
                probe(inter, rs.getString(2) ?: "", "surah")
            }
        }

        if (uncovered.isNotEmpty()) {
            val sample = uncovered.entries.take(12).joinToString(" ") { "${it.key}(x${it.value})" }
            problems += "reading fonts cannot draw ${uncovered.size} codepoints: $sample"
        }
        println("fonts: reading text codepoints checked: $codepoints")
    }

    private fun loadFontFile(file: File): Font {
        if (!file.exists()) error("missing font ${file.absolutePath}")
        return Font.createFont(Font.TRUETYPE_FONT, file)
    }

    private fun sourceDb(id: String): File =
        File(verify, id).firstWithExtension(".db", ".sqlite")
            ?: error("no database for $id; run verify first")
}
