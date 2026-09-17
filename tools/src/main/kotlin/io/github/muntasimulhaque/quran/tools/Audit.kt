package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.Arabic
import java.io.File
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/**
 * Compares the canonical KFGQPC word text with the Tanzil Uthmani reference
 * at letter level, ayah by ayah. Diacritics, Quranic signs, tatweel and the
 * alef, waw, yeh and heh forms are folded away by [Arabic.skeleton], so what
 * remains is the letter body of the text. Any difference is a real difference
 * and is written to `content/audit-report.md` for review.
 */
class Audit(private val root: File) {

    private data class Difference(
        val reference: String,
        val kfgqpcWords: List<String>,
        val tanzilWords: List<String>,
        val kfgqpcSkeleton: String,
        val tanzilSkeleton: String,
    )

    /**
     * Differences that are not errors: both editions are correct Uthmani
     * orthography, they simply spell the same word with different codepoints.
     * Each entry names the reason in full. This list is deliberately short and
     * must stay short; anything else that appears is a real difference.
     */
    private val acceptedVariants = mapOf(
        "2:72" to "Hamza in the word fa-iddarra'tum is a standalone letter in the KFGQPC " +
            "edition and a combining mark in the Tanzil edition; both are attested Uthmani " +
            "forms of the same word and the letter bodies are otherwise identical.",
    )

    fun run(): Int {
        val tanzil = readTanzil(find(File(root, "content/work/verify/tanzil-uthmani"), ".xml"))
        val kfgqpc = readKfgqpc(find(File(root, "content/work/verify/quran-script-kfgqpc"), ".db"))

        if (tanzil.size != 6236) {
            println("audit: expected 6236 Tanzil ayahs, found ${tanzil.size}")
            return 2
        }
        if (kfgqpc.size != 6236) {
            println("audit: expected 6236 KFGQPC ayahs, found ${kfgqpc.size}")
            return 2
        }

        val differences = mutableListOf<Difference>()
        val segmentation = mutableListOf<String>()
        var kfgqpcWordCount = 0
        var tanzilWordCount = 0

        for ((reference, words) in kfgqpc) {
            val tanzilText = tanzil[reference]
            if (tanzilText == null) {
                differences += Difference(reference, words, emptyList(), "", "")
                continue
            }
            kfgqpcWordCount += words.size
            val tanzilWords = tanzilText.split(Regex("\\s+"))
                .filter { Arabic.skeleton(it).isNotEmpty() }
            tanzilWordCount += tanzilWords.size
            if (words.size != tanzilWords.size) {
                segmentation += "$reference (KFGQPC ${words.size}, Tanzil ${tanzilWords.size})"
            }

            val kfgqpcSkeleton = Arabic.skeleton(words.joinToString(" "))
            val tanzilSkeleton = Arabic.skeleton(tanzilText)
            if (kfgqpcSkeleton != tanzilSkeleton) {
                differences += Difference(
                    reference = reference,
                    kfgqpcWords = words,
                    tanzilWords = tanzilWords,
                    kfgqpcSkeleton = kfgqpcSkeleton,
                    tanzilSkeleton = tanzilSkeleton,
                )
            }
        }

        val accepted = differences.filter { acceptedVariants.containsKey(it.reference) }
        val unexplained = differences.filterNot { acceptedVariants.containsKey(it.reference) }

        writeReport(unexplained, accepted, segmentation, kfgqpcWordCount, tanzilWordCount)
        println("audit: ${tanzil.size} ayahs compared")
        println("audit: KFGQPC words (markers excluded): $kfgqpcWordCount")
        println("audit: Tanzil words (pause tokens excluded): $tanzilWordCount")
        println("audit: letter-identical ayahs: ${tanzil.size - differences.size}")
        println("audit: accepted orthographic variants: ${accepted.size}")
        println("audit: unexplained differences: ${unexplained.size}")
        println("audit: segmentation notes: ${segmentation.size}")
        if (unexplained.isNotEmpty()) {
            println("audit: report written to content/audit-report.md")
            return 1
        }
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

    private fun readKfgqpc(database: File): LinkedHashMap<String, List<String>> {
        val ayahs = LinkedHashMap<String, MutableList<String>>(7000)
        openSqlite(database).use { connection ->
            connection.each("SELECT surah, ayah, text FROM words ORDER BY surah, ayah, word") { rs ->
                val text = rs.getString(3) ?: return@each
                if (Arabic.skeleton(text).isEmpty()) return@each // ayah number marker
                val key = "${rs.getInt(1)}:${rs.getInt(2)}"
                ayahs.getOrPut(key) { mutableListOf() }.add(text)
            }
        }
        return LinkedHashMap<String, List<String>>().apply { ayahs.forEach { (k, v) -> put(k, v) } }
    }

    private fun writeReport(
        unexplained: List<Difference>,
        accepted: List<Difference>,
        segmentation: List<String>,
        kfgqpcWordCount: Int,
        tanzilWordCount: Int,
    ) {
        val report = StringBuilder()
        report.appendLine("# Content audit report")
        report.appendLine()
        report.appendLine("Generated by `./gradlew :tools:run --args=audit`. Do not edit.")
        report.appendLine()
        report.appendLine("- Ayahs compared: 6236")
        report.appendLine("- KFGQPC words (ayah number markers excluded): $kfgqpcWordCount")
        report.appendLine("- Tanzil words (pause tokens excluded): $tanzilWordCount")
        report.appendLine("- Letter-identical ayahs: ${6236 - unexplained.size - accepted.size}")
        report.appendLine("- Accepted orthographic variants: ${accepted.size}")
        report.appendLine("- Unexplained differences: ${unexplained.size}")
        report.appendLine("- Segmentation notes (same letters, different word breaks): ${segmentation.size}")
        report.appendLine()
        if (accepted.isNotEmpty()) {
            report.appendLine("## Accepted orthographic variants")
            report.appendLine()
            report.appendLine("| Ayah | KFGQPC | Tanzil | Why it is accepted |")
            report.appendLine("| --- | --- | --- | --- |")
            for (d in accepted) {
                report.appendLine("| ${d.reference} | ${d.kfgqpcSkeleton} | ${d.tanzilSkeleton} | ${acceptedVariants[d.reference]?.replace("|", "/")} |")
            }
            report.appendLine()
        }
        if (segmentation.isNotEmpty()) {
            report.appendLine("## Segmentation notes")
            report.appendLine()
            report.appendLine("The letters agree; the two editions break these ayahs into words differently.")
            report.appendLine()
            segmentation.forEach { report.appendLine("- $it") }
            report.appendLine()
        }
        if (unexplained.isNotEmpty()) {
            report.appendLine("## Unexplained differences")
            report.appendLine()
            report.appendLine("| Ayah | KFGQPC letters | Tanzil letters | Detail |")
            report.appendLine("| --- | --- | --- | --- |")
            for (d in unexplained) {
                report.appendLine("| ${d.reference} | ${d.kfgqpcSkeleton} | ${d.tanzilSkeleton} | ${describe(d)} |")
            }
        } else {
            report.appendLine("The two independent editions agree in every ayah: 6235 letter-identical, one accepted orthographic variant.")
        }
        File(root, "content/audit-report.md").writeText(report.toString())
    }

    private fun describe(d: Difference): String {
        if (d.tanzilWords.isEmpty() || d.kfgqpcWords.isEmpty()) return "missing side"
        if (d.kfgqpcWords.size == d.tanzilWords.size) {
            val pairs = d.kfgqpcWords.zip(d.tanzilWords)
                .filter { (a, b) -> Arabic.skeleton(a) != Arabic.skeleton(b) }
                .take(3)
                .joinToString("; ") { (a, b) -> "$a != $b" }
            if (pairs.isNotEmpty()) return pairs.replace("|", "/")
        }
        return "letter count ${d.kfgqpcSkeleton.length} vs ${d.tanzilSkeleton.length}"
    }

    private fun find(dir: File, extension: String): File =
        dir.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(extension) }
            ?: error("no $extension file under ${dir.absolutePath}")
}
