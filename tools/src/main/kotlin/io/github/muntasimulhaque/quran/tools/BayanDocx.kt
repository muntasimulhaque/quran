package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/**
 * One paragraph of the publisher's Word edition: its text and, in order, the
 * footnotes it points at.
 */
internal data class DocxParagraph(val text: String, val notes: List<Int>)

/**
 * One ayah of the QUL export, reduced to what the matcher reads: the key and
 * the sentence with its stars and note body removed.
 */
internal data class QulVerse(val key: String, val text: String)

/**
 * The publisher's footnotes, by ayah. [allTexts] is the normalized form of
 * every note, so the build can tell a QUL-only note that the publisher keeps
 * somewhere else (34:54) from one the publisher does not have at all (9:1).
 */
internal data class BayanNotes(
    val byAyah: Map<String, List<String>>,
    val allTexts: Set<String>,
)

private const val WORDPROCESSING =
    "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

/**
 * Reads the publisher's 205 footnotes out of the extracted Word edition and
 * places each one on its ayah. The notes are the reason this file exists:
 * QUL keeps one note per ayah, the publisher keeps all of them.
 */
internal fun readBayanNotes(directory: File, qul: List<QulVerse>): BayanNotes {
    val document = File(directory, "word/document.xml")
    val footnotes = File(directory, "word/footnotes.xml")
    if (!document.isFile || !footnotes.isFile) {
        error("the Al-Bayan Word edition has no word/document.xml or word/footnotes.xml")
    }
    val paragraphs = document.inputStream().use(::readDocxParagraphs)
    val notes = footnotes.inputStream().use(::readDocxNotes)
    val matched = matchBayanNotes(paragraphs, notes, qul)
    if (matched.byAyah.size != 183) {
        error("the Al-Bayan Word edition has ${matched.byAyah.size} note-bearing ayahs, expected 183")
    }
    val total = matched.byAyah.values.sumOf { it.size }
    if (total != 205) error("the Al-Bayan Word edition has $total notes, expected 205")
    return matched
}

/**
 * Places the publisher's note-bearing paragraphs on their ayahs.
 *
 * Both files are the same translation from the same page, so a paragraph and
 * its QUL sentence share most of their characters. The best match wins; the
 * matches must arrive in Quran order, every note must find a home, and the
 * count is the publisher's 205. A failure is a stop, not a guess.
 */
internal fun matchBayanNotes(
    paragraphs: List<DocxParagraph>,
    notes: Map<Int, String>,
    qul: List<QulVerse>,
): BayanNotes {
    val noteParagraphs = paragraphs.filter {
        it.notes.isNotEmpty() && it.text.any { char -> char in '\u0980'..'\u09FF' }
    }
    val referenced = noteParagraphs.flatMap { it.notes }
    if (referenced.size != referenced.distinct().size) {
        error("the Al-Bayan Word edition references a footnote twice")
    }
    val candidates = qul.map { it.key to bigrams(normalizeBayanText(it.text)) }
    val byAyah = LinkedHashMap<String, List<String>>(200)
    var last = -1
    for (paragraph in noteParagraphs) {
        val grams = bigrams(normalizeBayanText(paragraph.text))
        var best = -1.0
        var bestKey: String? = null
        for ((key, verse) in candidates) {
            val score = dice(grams, verse)
            if (score > best) {
                best = score
                bestKey = key
            }
        }
        if (best < 0.5 || bestKey == null) {
            error(
                "cannot place an Al-Bayan note paragraph (best ${"%.2f".format(best)}): " +
                    paragraph.text.take(60),
            )
        }
        val order = ayahOrder(bestKey)
        if (order <= last) error("the Al-Bayan notes are out of order at $bestKey")
        last = order
        val texts = paragraph.notes.map { id ->
            notes[id] ?: error("the Al-Bayan Word edition points at missing footnote $id")
        }
        if (byAyah.put(bestKey, texts) != null) {
            error("the Al-Bayan Word edition places two paragraphs on $bestKey")
        }
    }
    val total = byAyah.values.sumOf { it.size }
    if (total != notes.size) {
        error("the Al-Bayan Word edition has $total placed notes, expected ${notes.size}")
    }
    val allTexts = byAyah.values.flatten().map(::normalizeNote).toSet()
    return BayanNotes(byAyah, allTexts)
}

private fun ayahOrder(key: String): Int {
    val surah = key.substringBefore(':').toIntOrNull()
        ?: error("the Al-Bayan match has no surah: $key")
    val ayah = key.substringAfter(':').toIntOrNull()
        ?: error("the Al-Bayan match has no ayah: $key")
    return surah * 1000 + ayah
}

/** The Bengali, Latin letters, and digits of a sentence, lowercased. */
private fun normalizeBayanText(text: String): String =
    text.replace('\u00AC', ' ')
        .replace(Regex("[^\\u0980-\\u09FFA-Za-z0-9]+"), " ")
        .lowercase()
        .trim()

/** A sentence's character pairs, sorted, for the Dice coefficient. */
private fun bigrams(text: String): IntArray {
    val set = HashSet<Int>(text.length)
    for (index in 0 until text.length - 1) {
        set += (text[index].code shl 16) or text[index + 1].code
    }
    return set.toIntArray().apply { sort() }
}

/** Twice the shared pairs over both sizes, the same rule the JVM tests use. */
private fun dice(first: IntArray, second: IntArray): Double {
    var i = 0
    var j = 0
    var shared = 0
    while (i < first.size && j < second.size) {
        when {
            first[i] == second[j] -> {
                shared++
                i++
                j++
            }
            first[i] < second[j] -> i++
            else -> j++
        }
    }
    val total = first.size + second.size
    return if (total == 0) 0.0 else 2.0 * shared / total
}

/**
 * Reads word/document.xml into paragraphs. A footnote reference keeps its id
 * in the paragraph's list and adds no text, so the paragraph still reads as
 * the sentence the note explains.
 */
internal fun readDocxParagraphs(input: InputStream): List<DocxParagraph> {
    val factory = XMLInputFactory.newFactory()
    factory.setProperty(XMLInputFactory.IS_COALESCING, true)
    val out = ArrayList<DocxParagraph>(14_000)
    val reader = factory.createXMLStreamReader(input)
    try {
        var text: StringBuilder? = null
        var notes: MutableList<Int>? = null
        var depth = 0
        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> when (reader.localName) {
                    "p" -> {
                        if (depth == 0) {
                            text = StringBuilder()
                            notes = ArrayList(2)
                        }
                        depth++
                    }
                    "footnoteReference" -> {
                        val id = reader.getAttributeValue(WORDPROCESSING, "id")
                            ?: reader.getAttributeValue(null, "id")
                        id?.toIntOrNull()?.let { notes?.add(it) }
                    }
                    "tab", "br", "cr" -> text?.append(' ')
                }
                XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> text?.append(reader.text)
                XMLStreamConstants.END_ELEMENT -> if (reader.localName == "p") {
                    depth--
                    if (depth == 0) {
                        out += DocxParagraph(
                            text = (text?.toString() ?: "").replace(Regex("\\s+"), " ").trim(),
                            notes = notes?.toList().orEmpty(),
                        )
                        text = null
                        notes = null
                    }
                }
            }
        }
    } finally {
        reader.close()
    }
    return out
}

/**
 * Reads word/footnotes.xml into its real notes. The separator and
 * continuation entries carry a type and are not notes; every real note must
 * have text, or the file is not the edition this build reads.
 */
internal fun readDocxNotes(input: InputStream): Map<Int, String> {
    val factory = XMLInputFactory.newFactory()
    factory.setProperty(XMLInputFactory.IS_COALESCING, true)
    val out = LinkedHashMap<Int, String>(220)
    val reader = factory.createXMLStreamReader(input)
    try {
        var id = -1
        var type: String? = null
        var text: StringBuilder? = null
        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> when (reader.localName) {
                    "footnote" -> {
                        id = reader.getAttributeValue(WORDPROCESSING, "id")
                            ?.toIntOrNull() ?: -1
                        type = reader.getAttributeValue(WORDPROCESSING, "type")
                        text = StringBuilder()
                    }
                    "tab", "br", "cr" -> text?.append(' ')
                }
                XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> text?.append(reader.text)
                XMLStreamConstants.END_ELEMENT -> if (reader.localName == "footnote") {
                    if (id > 1 && type == null) {
                        val note = cleanNote(text?.toString().orEmpty())
                        if (note.isEmpty()) {
                            error("the Al-Bayan Word edition has an empty footnote $id")
                        }
                        out[id] = note
                    }
                    id = -1
                    type = null
                    text = null
                }
            }
        }
    } finally {
        reader.close()
    }
    return out
}
