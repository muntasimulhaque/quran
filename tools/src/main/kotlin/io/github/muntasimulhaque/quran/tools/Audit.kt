package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.TajweedCodec
import java.io.File
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/**
 * Audits the QPC Hafs text the app reads against two references.
 *
 * QUL's own ayah export (resource 87) is the same QPC text and tajweed
 * annotation the word-by-word export (resource 58) carries, and it is the one
 * that must agree exactly: every letter, every mark, every waqf sign, and
 * every tajweed code character. A difference there is a build error. Tanzil
 * Uthmani 1.1 is the independent edition; the QPC print spells a number of
 * words in its own attested way (U+066E as the alef maksura base,
 * `مِيكَٰلَ` without Tanzil's extra `ى`), so the Tanzil differences are
 * reported as print variants and do not fail the build. The reader is owed
 * the QPC print, and the report says so.
 */
class Audit(private val root: File) {

    private data class AyahLine(val text: String, val code: String)

    private data class Difference(
        val reference: String,
        val qpcWords: List<String>,
        val tanzilWords: List<String>,
        val qpcSkeleton: String,
        val tanzilSkeleton: String,
    )

    fun run(): Int {
        val qul = readQul(find(File(root, "content/work/verify/quran-script-qpc-hafs-ayah"), ".db"))
        val qpc = readQpcAyahs(File(root, "content/quran.db"))
        val qpcWords = readQpcWords(File(root, "content/quran.db"))
        val tanzil = readTanzil(find(File(root, "content/work/verify/tanzil-uthmani"), ".xml"))

        val qulProblems = mutableListOf<String>()
        var textDifferences = 0
        var codeDifferences = 0
        for ((reference, line) in qpc) {
            val expected = qul[reference] ?: run {
                qulProblems += "$reference is missing from the QUL reference"
                continue
            }
            if (line.text != expected.text) {
                textDifferences++
                qulProblems += "$reference text: ${firstDifference(line.text, expected.text)}"
            }
            if (line.code != expected.code) {
                codeDifferences++
                qulProblems += "$reference tajweed: ${firstDifference(line.code, expected.code)}"
            }
        }
        if (qul.size != 6236) qulProblems += "QUL reference has ${qul.size} ayahs, expected 6236"
        if (qpc.size != 6236) qulProblems += "the app has ${qpc.size} ayahs, expected 6236"

        val differences = mutableListOf<Difference>()
        val segmentation = mutableListOf<String>()
        var qpcWordCount = 0
        for ((reference, words) in qpcWords) {
            val tanzilText = tanzil[reference] ?: continue
            qpcWordCount += words.size
            val tanzilWords = tanzilText.split(Regex("\\s+"))
                .filter { Arabic.skeleton(it).isNotEmpty() }
            if (words.size != tanzilWords.size) {
                segmentation += "$reference (QPC ${words.size}, Tanzil ${tanzilWords.size})"
            }
            val qpcSkeleton = Arabic.skeleton(words.joinToString(" "))
            val tanzilSkeleton = Arabic.skeleton(tanzilText)
            if (qpcSkeleton != tanzilSkeleton) {
                differences += Difference(reference, words, tanzilWords, qpcSkeleton, tanzilSkeleton)
            }
        }

        writeReport(qulProblems, differences, segmentation, qpcWordCount, textDifferences, codeDifferences)
        println("audit: ${qpc.size} ayahs compared")
        println("audit: QPC vs QUL 87: ${if (qulProblems.isEmpty()) "6236 character-identical, marks and tajweed code included" else "${qulProblems.size} differences"}")
        println("audit: QPC vs Tanzil: ${differences.size} print variants, ${segmentation.size} segmentation notes")
        if (qulProblems.isNotEmpty()) {
            println("audit: FAILED against QUL")
            qulProblems.take(10).forEach { println("  - $it") }
            return 1
        }
        println("audit: the QPC text and its tajweed code agree with QUL in every ayah")
        return 0
    }

    private fun readTanzil(xml: File): LinkedHashMap<String, String> {
        val ayahs = LinkedHashMap<String, String>(7000)
        val factory = XMLInputFactory.newInstance()
        val reader = factory.createXMLStreamReader(xml.inputStream())
        try {
            var surah = 0
            while (reader.hasNext()) {
                when (reader.next()) {
                    XMLStreamConstants.START_ELEMENT -> when (reader.localName) {
                        "sura" -> surah = reader.getAttributeValue(null, "index")?.toIntOrNull() ?: 0
                        "aya" -> {
                            val index = reader.getAttributeValue(null, "index")?.toIntOrNull() ?: 0
                            val text = reader.getAttributeValue(null, "text") ?: ""
                            ayahs["$surah:$index"] = text
                        }
                    }
                }
            }
        } finally {
            reader.close()
        }
        return ayahs
    }

    private fun readQul(database: File): LinkedHashMap<String, AyahLine> {
        val ayahs = LinkedHashMap<String, AyahLine>(7000)
        openSqlite(database).use { connection ->
            connection.each("SELECT verse_key, text FROM verses ORDER BY id") { rs ->
                val parsed = TajweedCodec.parse(rs.getString(2) ?: "")
                ayahs[rs.getString(1)] = withoutNumber(parsed.text, parsed.code)
            }
        }
        return ayahs
    }

    private fun readQpcAyahs(database: File): LinkedHashMap<String, AyahLine> {
        val ayahs = LinkedHashMap<String, AyahLine>(7000)
        openSqlite(database).use { connection ->
            connection.each("SELECT verse_key, text, tajweed FROM ayah ORDER BY number") { rs ->
                ayahs[rs.getString(1)] = AyahLine(
                    text = rs.getString(2) ?: "",
                    code = rs.getString(3) ?: "",
                )
            }
        }
        return ayahs
    }

    private fun readQpcWords(database: File): LinkedHashMap<String, List<String>> {
        val ayahs = LinkedHashMap<String, MutableList<String>>(7000)
        openSqlite(database).use { connection ->
            connection.each(
                "SELECT surah, ayah, text FROM word WHERE marker = 0 ORDER BY ayah_number, position",
            ) { rs ->
                val text = rs.getString(3) ?: return@each
                ayahs.getOrPut("${rs.getInt(1)}:${rs.getInt(2)}") { mutableListOf() }.add(text)
            }
        }
        return LinkedHashMap<String, List<String>>().apply { ayahs.forEach { (k, v) -> put(k, v) } }
    }

    /**
     * The QUL ayah export carries the ayah number as its last word. The app
     * draws the number itself, so the reference's number and its zero code are
     * removed before the exact comparison.
     */
    private fun withoutNumber(text: String, code: String): AyahLine {
        val match = Regex("[\\u0660-\\u0669]+\\s*$").find(text) ?: return AyahLine(text, code)
        val end = match.range.first
        val trimmed = text.substring(0, end).trimEnd()
        return AyahLine(trimmed, code.substring(0, minOf(code.length, trimmed.length)))
    }

    private fun firstDifference(actual: String, expected: String): String {
        val limit = minOf(actual.length, expected.length)
        for (index in 0 until limit) {
            if (actual[index] != expected[index]) {
                return "offset $index U+${actual[index].code.toString(16).uppercase()} != " +
                    "U+${expected[index].code.toString(16).uppercase()}"
            }
        }
        return "length ${actual.length} != ${expected.length}"
    }

    private fun writeReport(
        qulProblems: List<String>,
        differences: List<Difference>,
        segmentation: List<String>,
        qpcWordCount: Int,
        textDifferences: Int,
        codeDifferences: Int,
    ) {
        val report = StringBuilder()
        report.appendLine("# Content audit report")
        report.appendLine()
        report.appendLine("Generated by `./gradlew :tools:run --args=audit`. Do not edit.")
        report.appendLine()
        report.appendLine("- Ayahs compared: 6236")
        report.appendLine("- QPC words (ayah number markers excluded): $qpcWordCount")
        report.appendLine(
            if (qulProblems.isEmpty()) {
                "- QPC vs QUL 87: 6236 character-identical, marks and tajweed code included"
            } else {
                "- QPC vs QUL 87: ${qulProblems.size} differences ($textDifferences text, $codeDifferences tajweed)"
            },
        )
        report.appendLine("- QPC vs Tanzil: ${differences.size} print variants")
        report.appendLine("- Segmentation notes against Tanzil: ${segmentation.size}")
        report.appendLine()
        report.appendLine("The QPC text and its tajweed code are the QUL QPC Hafs print, compared")
        report.appendLine("character for character. Tanzil is an independent Uthmani edition; where")
        report.appendLine("the QPC print spells a word its own attested way, the difference is listed")
        report.appendLine("below as a print variant, not an error.")
        report.appendLine()
        if (qulProblems.isNotEmpty()) {
            report.appendLine("## QUL differences (build errors)")
            report.appendLine()
            qulProblems.forEach { report.appendLine("- $it") }
            report.appendLine()
        }
        if (segmentation.isNotEmpty()) {
            report.appendLine("## Tanzil segmentation notes")
            report.appendLine()
            report.appendLine("The letters agree; the two editions break these ayahs into words differently.")
            report.appendLine()
            segmentation.forEach { report.appendLine("- $it") }
            report.appendLine()
        }
        if (differences.isNotEmpty()) {
            report.appendLine("## QPC print variants from Tanzil")
            report.appendLine()
            report.appendLine("| Ayah | QPC letters | Tanzil letters | Detail |")
            report.appendLine("| --- | --- | --- | --- |")
            for (d in differences.take(40)) {
                report.appendLine("| ${d.reference} | ${d.qpcSkeleton} | ${d.tanzilSkeleton} | ${describe(d)} |")
            }
            if (differences.size > 40) {
                report.appendLine()
                report.appendLine("The table shows the first 40 of ${differences.size} variants.")
            }
        } else {
            report.appendLine("The QPC print and Tanzil agree in every ayah.")
        }
        File(root, "content/audit-report.md").writeText(report.toString())
    }

    private fun describe(d: Difference): String {
        if (d.tanzilWords.isEmpty() || d.qpcWords.isEmpty()) return "missing side"
        if (d.qpcWords.size == d.tanzilWords.size) {
            val pairs = d.qpcWords.zip(d.tanzilWords)
                .filter { (a, b) -> Arabic.skeleton(a) != Arabic.skeleton(b) }
                .take(2)
                .joinToString("; ") { (a, b) -> "$a != $b" }
            if (pairs.isNotEmpty()) return pairs.replace("|", "/")
        }
        return "letter count ${d.qpcSkeleton.length} vs ${d.tanzilSkeleton.length}"
    }

    private fun find(dir: File, extension: String): File =
        dir.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(extension) }
            ?: error("no $extension file under ${dir.absolutePath}")
}
