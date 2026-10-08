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
 * One rule, three checks:
 * - every non-digit codepoint of the canonical Unicode text is drawable by
 *   the KFGQPC Uthmanic Hafs font, which draws the study reading and the
 *   Mushaf page's own words both, so a page can never show tofu or a
 *   missing word;
 * - the page's own furniture (every ayah's roundel number, the page number,
 *   the juz, and each surah's band) is drawable by the face that draws it,
 *   which is the ornament, because that furniture is drawn here rather than
 *   carried in a font;
 * - every run of reading text is drawable by the font the app draws it with,
 *   with a script the app hands to the platform allowed only by name here.
 *
 * The third check is the one that keeps the app honest as content grows: a
 * translation or tafsir in a script no bundled face carries is not a tofu bug
 * (Android draws it with the system font), but it is a decision, and the
 * allow-list is where that decision is written down.
 *
 * The page's own text size (MushafText.DEFAULT_SCALE) is pinned by the
 * instrumented MushafPagesTest, which lays out every page of the Book on a
 * real device with the face's real metrics: the calibration is a property of
 * this font, and the font is hash-pinned in the manifest, so the two move
 * never.
 */
class Fonts(private val root: File) {

    private val verify = File(root, "content/work/verify")
    private val problems = mutableListOf<String>()

    /**
     * Scripts the app draws with the platform's own font instead of a bundled
     * one, because the platform covers them on every device the app supports
     * and bundling another writing system for one tafsir would not earn its
     * megabytes. Each entry is a Unicode block the platform owns, named once.
     */
    private val platformScripts = mapOf(
        // Bangla: the Taisirul Quran translation and the Bangla Ibn Kathir.
        // Android ships Noto Sans Bengali on every device since API 21,
        // which is what draws them.
        0x0980..0x09FF to "Bangla",
        // Arabic-Indic and extended digits, punctuation, and the shared marks
        // that travel between scripts.
        0x0964..0x0965 to "shared Indic punctuation",
        0x2000..0x206F to "general punctuation",
        0x20A0..0x20CF to "currency symbols",
        0x2100..0x214F to "letterlike symbols",
        0x2190..0x21FF to "arrows",
        0x2200..0x22FF to "mathematical operators",
        0x25A0..0x25FF to "geometric shapes",
        0x2600..0x26FF to "miscellaneous symbols",
        0xFB50..0xFDFF to "Arabic presentation forms",
        0xFE70..0xFEFF to "Arabic presentation forms",
    )

    private fun isPlatformScript(codepoint: Int): Boolean =
        platformScripts.keys.any { codepoint in it }

    fun run(): Int {
        checkUnicodeCoverage()
        checkPageFurniture()
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
        // The one Arabic face: the study reading's, and the page's own.
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

    /**
     * The page's furniture, drawn here rather than carried in a font: the
     * ornament face draws every ayah's roundel number and the page's own
     * number, the juz, and each surah's band. A digit or a name the ornament
     * cannot draw would be a hole in the printed page's furniture.
     */
    private fun checkPageFurniture() {
        val amiri = loadFontFile(File(root, "content-assets/src/main/res/font/amiri_quran.ttf"))
        val uncovered = mutableSetOf<Int>()
        fun probe(text: String, what: String) {
            for (codepoint in text.codePoints()) {
                if (codepoint <= 32) continue
                if (!amiri.canDisplay(codepoint)) {
                    uncovered += codepoint
                    problems += "page furniture ($what) cannot draw U+" +
                        codepoint.toString(16).uppercase()
                }
            }
        }
        // Arabic-Indic digits: every ayah's number, the page number, the juz.
        for (ch in "٠١٢٣٤٥٦٧٨٩") probe(
            ch.toString(),
            "ayah number",
        )
        // The juz's own label and the surahs' names, drawn in the band.
        probe("الجزء", "juz")
        val packs = File(root, "content/packs")
        if (packs.isDirectory) {
            openSqlite(File(packs, "core.db")).use { connection ->
                connection.each("SELECT name_arabic FROM surah") { rs ->
                    probe(rs.getString(1) ?: "", "surah band")
                }
            }
        }
        if (uncovered.isEmpty()) {
            println("fonts: page furniture checked: digits, juz, surah bands")
        }
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
     *
     * What the reader can see is what ships: the core pack carries the surah
     * names and the canonical words, each translation pack carries its own
     * footnotes and its own text, each tafsir pack its own prose. The gate
     * therefore reads the built pack databases and not a monolith, because a
     * translation added in a later session must be checked the day it lands.
     */
    private fun checkReadingFonts() {
        val packs = File(root, "content/packs")
        if (!packs.isDirectory) {
            problems += "content/packs is missing; run build before fonts"
            return
        }
        val literata = loadFontFile(File(root, "content-assets/src/main/res/font/literata_variable.ttf"))
        val amiri = loadFontFile(File(root, "content-assets/src/main/res/font/amiri_quran.ttf"))
        val hafs = loadFontFile(File(root, "content/work/fonts-hafs/UthmanicHafs_V22.ttf"))
        val inter = loadFontFile(File(root, "content-assets/src/main/res/font/inter_variable.ttf"))

        val uncovered = TreeMap<String, Int>()
        var codepoints = 0

        fun probe(font: Font, text: String, label: String) {
            for (codepoint in text.codePoints()) {
                if (codepoint <= 32) continue
                codepoints++
                if (!font.canDisplay(codepoint) && !isPlatformScript(codepoint)) {
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
                // does it. Anything neither font nor the platform can draw
                // fails the gate.
                for (codepoint in run.text.codePoints()) {
                    if (codepoint <= 32) continue
                    codepoints++
                    if (!amiri.canDisplay(codepoint) &&
                        !hafs.canDisplay(codepoint) &&
                        !isPlatformScript(codepoint)
                    ) {
                        uncovered.merge("$label U+${codepoint.toString(16).uppercase()}", 1, Int::plus)
                    }
                }
            }
        }

        // The surah names and the word meanings: names are drawn in Inter, and
        // each word list's meanings in Literata, exactly as the study view
        // draws them.
        openSqlite(File(packs, "core.db")).use { connection ->
            connection.each("SELECT name_simple, name_latin FROM surah") { rs ->
                probe(inter, rs.getString(1) ?: "", "surah")
                probe(inter, rs.getString(2) ?: "", "surah")
            }
        }
        for (file in packs.listFiles { it -> it.isFile && it.name.startsWith("words-") }.orEmpty()) {
            openSqlite(file).use { connection ->
                connection.each("SELECT meaning FROM word_meaning") { rs ->
                    probe(literata, rs.getString(1) ?: "", "word")
                }
                // The English list also carries the surah introductions.
                if (file.name == "words-en.db") {
                    connection.each("SELECT text FROM surah_info") { rs ->
                        probe(inter, RichText.plain(rs.getString(1) ?: ""), "surah-info")
                    }
                }
            }
        }
        for (file in packs.listFiles { it -> it.isFile && it.name.startsWith("translation-") }.orEmpty()) {
            openSqlite(file).use { connection ->
                connection.each("SELECT text, footnotes FROM translation") { rs ->
                    probeRuns(RichText.runs(rs.getString(1) ?: ""), literata, file.name)
                    probeRuns(RichText.footnotes(rs.getString(2) ?: ""), literata, file.name)
                }
            }
        }
        for (file in packs.listFiles { it -> it.isFile && it.name.startsWith("tafsir-") }.orEmpty()) {
            // A tafsir whose own language is Arabic is quoted prose, drawn in
            // Amiri like the rest of the Arabic outside the Mushaf; the
            // offered tafsirs are Latin, paragraphs and headings drawn in
            // Literata and Inter.
            val arabic = file.name.endsWith("-ar.db")
            openSqlite(file).use { connection ->
                connection.each("SELECT text FROM tafsir_passage") { rs ->
                    val text = rs.getString(1) ?: ""
                    if (arabic) {
                        probeRuns(RichText.quotes(text), amiri, file.name)
                    } else {
                        for (block in RichText.parseHtml(text)) {
                            val latin = if (block.kind == TextBlockKind.HEADING) inter else literata
                            probeRuns(block.runs, latin, file.name)
                        }
                    }
                }
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
