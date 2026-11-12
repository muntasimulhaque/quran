package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.github.muntasimulhaque.quran.core.TajweedCodec
import java.io.File
import java.util.zip.GZIPInputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/**
 * Verifies every raw source in the manifest: checksum first, then the
 * structural invariants the rest of the pipeline relies on. Any failure is
 * collected and reported together, so one run shows every problem.
 */
class Verify(private val root: File) {

    private val work = File(root, "content/work/verify")
    private val failures = mutableListOf<String>()

    fun run(): Int {
        val manifest = loadManifest(root)
        for (dataset in manifest.datasets) {
            val file = SafeFiles.resolveUnder(root, dataset.path)
            if (file == null) {
                failures += "${dataset.id}: unsafe dataset path: ${dataset.path}"
                continue
            }
            if (!file.exists()) {
                if (dataset.pending) {
                    println("verify: waiting for a manual download: ${dataset.id} -> ${dataset.path}")
                    continue
                }
                failures += "${dataset.id}: file is missing: ${dataset.path}"
                continue
            }
            if (file.length() != dataset.bytes) {
                failures += "${dataset.id}: size ${file.length()} does not match ${dataset.bytes}"
            }
            val actual = sha256(file)
            if (actual != dataset.sha256) {
                failures += "${dataset.id}: sha256 $actual does not match ${dataset.sha256}"
            }
        }
        if (failures.isEmpty()) {
            println("checksums: ${manifest.datasets.size} datasets verified")
        }

        for (dataset in manifest.datasets) {
            val file = SafeFiles.resolveUnder(root, dataset.path) ?: continue
            if (!file.exists()) continue
            val dir = extract(file, File(work, dataset.id))
            check(dataset, dir)
        }

        checkSaheehCrosscheck()

        if (failures.isEmpty()) {
            println("structure: all datasets verified")
            return 0
        }
        println()
        println("FAILURES (${failures.size})")
        failures.forEach { println("  - $it") }
        return 1
    }

    private fun fail(id: String, message: String) {
        failures += "$id: $message"
    }

    private fun check(dataset: Dataset, dir: File) {
        when (dataset.id) {
            "quran-script-qpc-hafs" -> checkQpcScript(dataset.id, dir)
            "quran-script-qpc-hafs-ayah" -> checkQulAyah(dataset.id, dir)
            "translation-saheeh" -> checkSaheeh(dataset.id, dir)
            "saheeh-qul-crosscheck" -> checkSimpleCount(dataset.id, dir, "translation", 6236)
            "translation-al-bayan-bn" -> checkAlBayanJson(dataset.id, dir)
            "translation-al-bayan-bn-notes" -> checkAlBayanDocx(dataset.id, dir)
            "tafsir-ibn-kathir-en", "tafsir-ibn-kathir-bn" -> checkTafsirQul(dataset.id, dir)
            "word-by-word-english", "word-by-word-bengali" -> checkWordTranslation(dataset.id, dir)
            "word-morphology" -> checkMorphology(dataset.id, dir)
            "recitation-minshawi", "recitation-husary" -> checkRecitation(dataset.id, dir)
            "metadata-surah-names" -> checkJsonKeys(dataset.id, dir, 114)
            "surah-names-bn" -> checkSurahNamesBn(dataset.id, dir)
            "metadata-ayah" -> checkJsonKeys(dataset.id, dir, 6236)
            "metadata-sajda" -> checkJsonKeys(dataset.id, dir, 15)
            "metadata-juz" -> checkJsonKeys(dataset.id, dir, 30)
            "metadata-hizb" -> checkJsonKeys(dataset.id, dir, 60)
            "metadata-rub" -> checkJsonKeys(dataset.id, dir, 240)
            "metadata-manzil" -> checkJsonKeys(dataset.id, dir, 7)
            "metadata-ruku" -> checkJsonMinKeys(dataset.id, dir, 500)
            "surah-info-en" -> checkJsonKeys(dataset.id, dir, 114)
            "tanzil-uthmani" -> checkTanzil(dataset.id, dir)
            "quran-font-hafs" -> checkFonts(dataset.id, dir, expected = 1)
        }
    }

    private fun db(dir: File): File =
        dir.firstWithExtension(".db", ".sqlite") ?: error("no sqlite database in ${dir.absolutePath}")

    /**
     * The QPC Hafs word export: 83,668 rows, 114 surahs, 6,236 ayahs, and a
     * tag on every word whose class the app knows. The one HTML entity in the
     * source is the escaped `&gt;` in 32:3, an export artifact the codec
     * strips; anything else is a lost letter and a failure.
     */
    private fun checkQpcScript(id: String, dir: File) {
        openSqlite(db(dir)).use { connection ->
            val rows = connection.count("words")
            if (rows != 83_668L) fail(id, "expected 83668 words, found $rows")
            val surahs = connection.scalarLong("SELECT COUNT(DISTINCT surah) FROM words")
            if (surahs != 114L) fail(id, "expected 114 surahs, found $surahs")
            val ayahs = connection.scalarLong("SELECT COUNT(*) FROM (SELECT DISTINCT surah, ayah FROM words)")
            if (ayahs != 6236L) fail(id, "expected 6236 ayahs, found $ayahs")
            val markers = connection.scalarLong("SELECT COUNT(*) FROM words WHERE text GLOB '*[٠-٩]*'")
            if (markers != 6_235L) fail(id, "expected 6235 ayah-number markers, found $markers")
            connection.each("SELECT location, text FROM words") { rs ->
                val location = rs.getString(1)
                if (!location.matches(Regex("^\\d+:\\d+:\\d+$"))) fail(id, "bad location: $location")
                val text = rs.getString(2) ?: ""
                if (text.isBlank()) fail(id, "blank text at $location")
                if (text.contains('\u25CC')) fail(id, "dotted circle artifact at $location")
                checkTajweed(id, location, text)
            }
        }
    }

    /** The ayah-level QUL reference: 6,236 verses, every one tagged and clean. */
    private fun checkQulAyah(id: String, dir: File) {
        openSqlite(db(dir)).use { connection ->
            val rows = connection.count("verses")
            if (rows != 6236L) fail(id, "expected 6236 verses, found $rows")
            val surahs = connection.scalarLong("SELECT COUNT(DISTINCT surah) FROM verses")
            if (surahs != 114L) fail(id, "expected 114 surahs, found $surahs")
            connection.each("SELECT verse_key, surah, ayah, text FROM verses") { rs ->
                val key = rs.getString(1) ?: ""
                val surah = rs.getInt(2)
                val ayah = rs.getInt(3)
                if (key != "$surah:$ayah") fail(id, "verse key $key does not match $surah:$ayah")
                val text = rs.getString(4) ?: ""
                if (text.isBlank()) fail(id, "blank verse $key")
                checkTajweed(id, key, text)
            }
        }
    }

    private fun checkTajweed(id: String, where: String, tagged: String) {
        for (match in entity.findAll(tagged)) {
            if (match.value != "&gt;") fail(id, "unknown entity ${match.value} in $where")
        }
        val parsed = runCatching { TajweedCodec.parse(tagged) }.getOrElse { failure ->
            fail(id, "unreadable tajweed at $where: ${failure.message}")
            return
        }
        if (parsed.code.length != parsed.text.length) {
            fail(id, "tajweed code ${parsed.code.length} characters for ${parsed.text.length} at $where")
        }
        if (!TajweedCodec.isWellFormed(parsed.code)) fail(id, "unknown tajweed code at $where")
    }

    private val entity = Regex("&(?:[a-zA-Z]+|#[0-9]+|#x[0-9A-Fa-f]+);")

    /**
     * The Saheeh export's own footnote shape, before the build applies either
     * of this app's rules. The publisher numbers across the book and its
     * first note belongs to the surah title, so the ayah export leaves one
     * number unused at every surah start. This gate pins that shape: 1,903
     * note bodies, 1,904 markers, 113 unused numbers, and exactly one marker
     * whose body the export lost (7:143's 403). It also pins the one sentence
     * the build corrects, so a source update that fixes it makes the build
     * fail and the correction can be removed deliberately.
     */
    private fun checkSaheeh(id: String, dir: File) {
        openSqlite(db(dir)).use { connection ->
            val rows = connection.count("translations")
            if (rows != 6236L) fail(id, "expected 6236 translations, found $rows")
            val bad = connection.scalarLong(
                "SELECT COUNT(*) FROM translations WHERE sura IS NULL OR aya IS NULL OR translation IS NULL OR translation=''",
            )
            if (bad != 0L) fail(id, "$bad translations are incomplete")
            val max = connection.scalarLong("SELECT COUNT(*) FROM translations WHERE id < 1 OR id > 6236")
            if (max != 0L) fail(id, "$max translation ids are out of range")

            var notes = 0
            var markers = 0
            val dangling = mutableListOf<Pair<Int, Int>>()
            val unused = mutableListOf<Pair<Int, Int>>()
            val markerNumbers = HashSet<Int>()
            connection.each("SELECT id, translation, footnotes FROM translations ORDER BY id") { rs ->
                val ayah = rs.getInt(1)
                val text = rs.getString(2) ?: ""
                val bodies = saheehNotes(rs.getString(3) ?: "")
                val drawn = Regex("\\[(\\d+)]").findAll(text)
                    .map { it.groupValues[1].toInt() }
                    .toList()
                notes += bodies.size
                markers += drawn.size
                for (number in drawn) {
                    markerNumbers += number
                    if (number !in bodies) dangling += ayah to number
                }
                for (number in bodies.keys) {
                    if (number !in drawn) unused += ayah to number
                }
            }
            if (notes != 1903) fail(id, "expected 1903 notes, found $notes")
            if (markers != 1904) fail(id, "expected 1904 markers, found $markers")
            if (dangling != listOf(1097 to 403)) {
                fail(id, "unexpected marker(s) without a note: $dangling")
            }
            if (unused.isNotEmpty()) fail(id, "note(s) without a marker: $unused")
            // The numbers the export never uses are the surah-title notes,
            // which an ayah-keyed file cannot carry. The 403 hole is the
            // lost body the build drops, not a title note, so it is counted
            // by [dangling] above and not here.
            val missing = (1..markerNumbers.max()).filterNot { it in markerNumbers }
            if (missing.size != 113) {
                fail(id, "expected 113 surah-title numbers, found ${missing.size}")
            }
            if (1 !in missing) fail(id, "the first note is no longer the surah-title hole")

            val correction = saheehCorrections.firstOrNull { it.ayah == 325 }
            val target = connection.scalarLong(
                "SELECT COUNT(*) FROM translations WHERE id = 325 AND translation LIKE ?",
                "%${correction?.from.orEmpty()}%",
            )
            if (target != 1L) fail(id, "the 3:32 sentence the build corrects is no longer in the source")
        }
    }

    /**
     * The independent copy of the same translation, compared once at build
     * time. QUL's file is not shipped and no app code reads it; it is here to
     * catch a source update that changes a word the publisher did not intend
     * to change. The known differences are pinned, so a new one fails the
     * build and an old one cannot be forgotten.
     *
     * The copy is the same 1,903 notes. Its 3:32 already reads "they", which
     * is why the build corrects the publisher's export. Its 4:12 note has a
     * missing space ("fromthe"), and 192 notes differ only in spacing; the
     * app does not use QUL's note text, so only the one real typo is pinned.
     */
    private fun checkSaheehCrosscheck() {
        val sourceDir = File(work, "translation-saheeh")
        val qulDir = File(work, "saheeh-qul-crosscheck")
        val sourceDb = sourceDir.firstWithExtension(".db", ".sqlite")
        val qulDb = qulDir.firstWithExtension(".db", ".sqlite")
        if (sourceDb == null || qulDb == null) {
            println("cross-check: the two translation copies are not both extracted; skipped")
            return
        }
        val source = LinkedHashMap<String, String>()
        val sourceNotes = HashMap<String, List<String>>()
        openSqlite(sourceDb).use { connection ->
            connection.each("SELECT sura, aya, translation, footnotes FROM translations ORDER BY id") { rs ->
                val key = "${rs.getInt(1)}:${rs.getInt(2)}"
                source[key] = rs.getString(3) ?: ""
                sourceNotes[key] = saheehNotes(rs.getString(4) ?: "").values.toList()
            }
        }
        val qul = LinkedHashMap<String, String>()
        val qulNotes = HashMap<String, List<String>>()
        openSqlite(qulDb).use { connection ->
            connection.each("SELECT sura, ayah, text FROM translation ORDER BY sura, ayah") { rs ->
                val key = "${rs.getInt(1)}:${rs.getInt(2)}"
                val raw = rs.getString(3) ?: ""
                val decoded = runCatching {
                    Json.parseToJsonElement(raw).jsonPrimitive.content
                }.getOrElse { raw }
                qul[key] = decoded.replace(Regex("\\[\\[.*?]]", RegexOption.DOT_MATCHES_ALL), "")
                qulNotes[key] = Regex("\\[\\[(.*?)]]", RegexOption.DOT_MATCHES_ALL)
                    .findAll(decoded)
                    .map { it.groupValues[1].replaceFirst(Regex("^[-\\u2013\\u2014]\\s*"), "").trim() }
                    .toList()
            }
        }
        if (source.size != qul.size) {
            fail("saheeh-qul-crosscheck", "the copies have ${source.size} and ${qul.size} ayahs")
            return
        }
        val whitespace = Regex("\\s+")
        fun fold(text: String) = whitespace.replace(text, " ").trim()
        val textDifferences = LinkedHashMap<String, Pair<String, String>>()
        var textSpacing = 0
        for ((key, text) in source) {
            val readable = text.replace(Regex("\\(\\d+\\)\\s*"), "").replace(Regex("\\[\\d+]"), "")
            val a = fold(readable)
            val b = fold(qul[key].orEmpty())
            if (a == b) {
                if (readable.trim() != qul[key].orEmpty().trim()) textSpacing++
                continue
            }
            textDifferences[key] = a to b
        }
        if (textDifferences.keys != setOf("3:32")) {
            fail("saheeh-qul-crosscheck", "unexpected ayah differences: ${textDifferences.keys}")
        } else {
            val (a, b) = textDifferences.getValue("3:32")
            if (!a.contains("But if you turn away") || !b.contains("But if they turn away")) {
                fail("saheeh-qul-crosscheck", "3:32 changed in a way the build does not expect")
            }
        }
        if (textSpacing != 0) println("cross-check: $textSpacing ayah(s) differ only in spacing")

        val noteDifferences = LinkedHashMap<String, Pair<String, String>>()
        var noteSpacing = 0
        var counts = 0
        for ((key, notes) in sourceNotes) {
            val other = qulNotes[key].orEmpty()
            if (notes.size != other.size) {
                fail("saheeh-qul-crosscheck", "$key has ${notes.size} and ${other.size} notes")
                counts++
                continue
            }
            for (index in notes.indices) {
                val rawA = notes[index].trim()
                val rawB = other[index].trim()
                val a = fold(rawA)
                val b = fold(rawB)
                if (a == b) {
                    if (rawA != rawB) noteSpacing++
                    continue
                }
                noteDifferences["$key #${index + 1}"] = a to b
            }
        }
        if (counts == 0 && noteDifferences.keys != setOf("4:12 #1")) {
            fail("saheeh-qul-crosscheck", "unexpected note differences: ${noteDifferences.keys}")
        } else if (noteDifferences.isNotEmpty()) {
            val (a, b) = noteDifferences["4:12 #1"] ?: ("" to "")
            if (!a.contains("from the") || !b.contains("fromthe")) {
                fail("saheeh-qul-crosscheck", "4:12 changed in a way the build does not expect")
            }
        }
        if (noteSpacing != 0) println("cross-check: $noteSpacing note(s) differ only in spacing")
        println("cross-check: pinned: 3:32 reads \"they\" in the QUL copy; 4:12's note reads \"fromthe\"")
        println("cross-check: ${source.size} ayahs and ${sourceNotes.values.sumOf { it.size }} notes compared with QUL")
    }

    private fun checkTafsirQul(id: String, dir: File) {
        openSqlite(db(dir)).use { connection ->
            val rows = connection.count("tafsir")
            if (rows != 6236L) fail(id, "expected 6236 tafsir rows, found $rows")
            val badKey = connection.scalarLong("SELECT COUNT(*) FROM tafsir WHERE ayah_key NOT GLOB '[0-9]*:[0-9]*'")
            if (badKey != 0L) fail(id, "$badKey tafsir rows have a bad ayah key")
            val text = connection.scalarLong("SELECT COUNT(*) FROM tafsir WHERE text IS NOT NULL AND text <> ''")
            if (text < 1900L) fail(id, "only $text passages carry text, expected at least 1900")
            // Every blank row must resolve to a passage through its group key.
            val orphans = connection.scalarLong(
                "SELECT COUNT(*) FROM tafsir b WHERE (b.text IS NULL OR b.text='') AND NOT EXISTS " +
                    "(SELECT 1 FROM tafsir g WHERE g.ayah_key=b.group_ayah_key AND g.text IS NOT NULL AND g.text<>'')",
            )
            if (orphans != 0L) fail(id, "$orphans ayahs have no tafsir passage to resolve to")
        }
    }

    private fun surahVerseCounts(): IntArray {
        val counts = IntArray(115)
        val script = File(work, "quran-script-qpc-hafs").firstWithExtension(".db")
            ?: return counts
        openSqlite(script).use { connection ->
            connection.each("SELECT surah, MAX(ayah) FROM words GROUP BY surah") { rs ->
                counts[rs.getInt(1)] = rs.getInt(2)
            }
        }
        return counts
    }

    private fun globalIndex(counts: IntArray, surah: Int, ayah: Int): Int {
        var index = 0
        for (s in 1 until surah) index += counts[s]
        return index + ayah
    }

    private fun checkWordTranslation(id: String, dir: File) {
        openSqlite(db(dir)).use { connection ->
            val rows = connection.count("word_translation")
            if (rows < 83_000L) fail(id, "unexpectedly few word translations: $rows")
        }
    }

    /**
     * The morphology export: the whole Book, word for word, and every word a
     * segment the sheet can name. The build reads the labels; this proves the
     * shape they arrive in before anything is written.
     */
    private fun checkMorphology(id: String, dir: File) {
        val file = dir.firstWithExtension(".json.gz", ".json")
            ?: return fail(id, "no morphology export")
        val text = if (file.name.endsWith(".gz")) {
            GZIPInputStream(file.inputStream().buffered()).bufferedReader().use { it.readText() }
        } else {
            file.readText()
        }
        val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrElse { failure ->
            return fail(id, "the export could not be read: ${failure.message}")
        }
        val ayahs = root["data"]?.jsonArray ?: return fail(id, "the export has no data array")
        if (ayahs.size != 6236) fail(id, "the export has ${ayahs.size} ayahs, expected 6236")
        var words = 0
        var withoutSegments = 0
        var withoutPos = 0
        for (ayah in ayahs) {
            val list = ayah.jsonObject["morphology"]?.jsonObject?.get("words")?.jsonArray
                ?: continue
            for (word in list) {
                words++
                val segments = word.jsonObject["segments"]?.jsonArray
                if (segments.isNullOrEmpty()) {
                    withoutSegments++
                    continue
                }
                val stem = segments.map { it.jsonObject }.firstOrNull {
                    it["role"]?.jsonPrimitive?.contentOrNull == "stem"
                } ?: segments.first().jsonObject
                if (stem["pos"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) withoutPos++
            }
        }
        if (words != 77_429) fail(id, "the export has $words words, expected 77,429")
        if (withoutSegments != 0) fail(id, "$withoutSegments words have no segments")
        if (withoutPos != 0) fail(id, "$withoutPos words have no part of speech")
        if (withoutSegments == 0 && withoutPos == 0) {
            println("verify: $id: ${ayahs.size} ayahs, $words words")
        }
    }

    private fun checkRecitation(id: String, dir: File) {
        openSqlite(db(dir)).use { connection ->
            val rows = connection.count("verses")
            if (rows != 6236L) fail(id, "expected 6236 recitation rows, found $rows")
            val badUrl = connection.scalarLong(
                "SELECT COUNT(*) FROM verses WHERE audio_url NOT LIKE 'https://audio-cdn.tarteel.ai/%'",
            )
            if (badUrl != 0L) fail(id, "$badUrl recitation rows have an unexpected audio URL")
            val badSegments = connection.scalarLong(
                "SELECT COUNT(*) FROM verses WHERE segments IS NULL OR segments NOT LIKE '[%'",
            )
            if (badSegments != 0L) fail(id, "$badSegments recitation rows have no segments")
            // The export's ayah_number column is a global counter. The real
            // surah and ayah are the three-digit groups in the file name, so
            // they are checked against the surah column and for full coverage.
            val seen = HashSet<String>(7000)
            val counts = surahVerseCounts()
            // The counter check needs the QPC script, which is one of the
            // large manual exports. A machine without it still gets the
            // file-name and coverage checks; the missing dataset is already
            // reported by the manifest pass (owner decision, this session).
            val hasCounts = counts.any { it > 0 }
            if (!hasCounts) {
                println("verify: $id: the QPC script is not extracted; ayah counters skipped")
            }
            var badFile = 0
            var badCounter = 0
            connection.each("SELECT surah_number, ayah_number, audio_url FROM verses") { rs ->
                val file = (rs.getString(3) ?: "").substringAfterLast('/')
                val match = Regex("^(\\d{3})(\\d{3})\\.[a-z0-9]+$").find(file)
                if (match == null) {
                    badFile++
                    return@each
                }
                val surah = match.groupValues[1].toInt()
                val ayah = match.groupValues[2].toInt()
                if (surah != rs.getInt(1)) {
                    badFile++
                    return@each
                }
                if (hasCounts && globalIndex(counts, surah, ayah) != rs.getInt(2)) badCounter++
                seen += "$surah:$ayah"
            }
            if (badFile != 0) fail(id, "$badFile rows whose file name does not match their surah")
            if (badCounter != 0) fail(id, "$badCounter rows whose ayah_number is not the global ayah index")
            if (seen.size != 6236) fail(id, "file names cover ${seen.size} ayahs, expected 6236")
        }
    }

    private fun checkSimpleCount(id: String, dir: File, table: String, expected: Long) {
        openSqlite(db(dir)).use { connection ->
            val rows = connection.count(table)
            if (rows != expected) fail(id, "expected $expected rows in $table, found $rows")
        }
    }

    private fun jsonFile(dir: File): File =
        dir.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".json") }
            ?: error("no json file in ${dir.absolutePath}")

    /**
     * The QUL translation JSON: one row per ayah, keyed 1:1 to 114:6, with
     * no blank text. The build keeps the source text whole, footnote mark
     * included; whether that footnote stays in the reading is a content
     * choice made when the pack is built.
     */
    private fun checkAlBayanJson(id: String, dir: File) {
        val root = runCatching { Json.parseToJsonElement(jsonFile(dir).readText()).jsonObject }
            .getOrElse { failure ->
                fail(id, "cannot read JSON: ${failure.message}")
                return
            }
        val rows = bayanRows(root)
        if (rows.size != 6236) fail(id, "expected 6236 ayahs, found ${rows.size}")
        val keys = HashSet<String>(7000)
        for ((key, text) in rows) {
            if (key.isBlank() || text.isBlank()) {
                fail(id, "an ayah has no key or no text")
                continue
            }
            keys += key
        }
        if (keys.size != 6236) fail(id, "expected 6236 distinct ayah keys, found ${keys.size}")
    }

    /**
     * The publisher's Word edition: 205 real footnotes, 205 references in the
     * sentences, and 183 paragraphs that carry them. The build reads these
     * counts, so the gate pins them before a missing note can ship.
     */
    private fun checkAlBayanDocx(id: String, dir: File) {
        val document = File(dir, "word/document.xml")
        val footnotes = File(dir, "word/footnotes.xml")
        if (!document.isFile || !footnotes.isFile) {
            fail(id, "the Word edition has no word/document.xml or word/footnotes.xml")
            return
        }
        val paragraphs = runCatching { document.inputStream().use(::readDocxParagraphs) }
            .getOrElse { failure ->
                fail(id, "cannot read document.xml: ${failure.message}")
                return
            }
        val notes = runCatching { footnotes.inputStream().use(::readDocxNotes) }
            .getOrElse { failure ->
                fail(id, "cannot read footnotes.xml: ${failure.message}")
                return
            }
        val references = paragraphs.sumOf { it.notes.size }
        val distinctReferences = paragraphs.flatMap { it.notes }.distinct().size
        val withNotes = paragraphs.count {
            it.notes.isNotEmpty() && it.text.any { char -> char in '\u0980'..'\u09FF' }
        }
        if (notes.size != 205) fail(id, "expected 205 footnotes, found ${notes.size}")
        if (references != 205) fail(id, "expected 205 footnote references, found $references")
        if (distinctReferences != 205) {
            fail(id, "expected 205 distinct footnote references, found $distinctReferences")
        }
        if (withNotes != 183) fail(id, "expected 183 note-bearing paragraphs, found $withNotes")
    }

    private fun checkJsonKeys(id: String, dir: File, expected: Int) {
        val element = Json.parseToJsonElement(jsonFile(dir).readText())
        val size = (element as? JsonObject)?.size ?: (element as? JsonArray)?.size ?: -1
        if (size != expected) fail(id, "expected $expected entries, found $size")
    }

    private fun checkJsonMinKeys(id: String, dir: File, minimum: Int) {
        val element = Json.parseToJsonElement(jsonFile(dir).readText())
        val size = (element as? JsonObject)?.size ?: (element as? JsonArray)?.size ?: -1
        if (size < minimum) fail(id, "expected at least $minimum entries, found $size")
    }

    /**
     * The reviewed Bangla names: one per surah, none blank, and no name for a
     * number the Book does not have. The build reads this file word for word,
     * so a hole here is a hole the reader would meet in Bangla (owner
     * decision, this session).
     */
    private fun checkSurahNamesBn(id: String, dir: File) {
        val element = Json.parseToJsonElement(jsonFile(dir).readText())
        val names = element as? JsonObject ?: return fail(id, "expected a JSON object of surah names")
        if (names.size != 114) fail(id, "expected 114 surah names, found ${names.size}")
        for ((number, value) in names) {
            if (number.toIntOrNull() !in 1..114) fail(id, "bad surah number: $number")
            if (value.jsonPrimitive.contentOrNull.isNullOrBlank()) fail(id, "surah $number has no name")
        }
    }

    private fun checkTanzil(id: String, dir: File) {
        val xml = dir.walkTopDown().firstOrNull { it.name.endsWith(".xml") }
            ?: return fail(id, "no XML file found")
        var suras = 0
        var ayas = 0
        val factory = XMLInputFactory.newInstance()
        val reader = factory.createXMLStreamReader(xml.inputStream())
        try {
            while (reader.hasNext()) {
                when (reader.next()) {
                    XMLStreamConstants.START_ELEMENT -> when (reader.localName) {
                        "sura" -> suras++
                        "aya" -> {
                            ayas++
                            val text = reader.getAttributeValue(null, "text")
                            if (text.isNullOrBlank()) fail(id, "an aya has no text")
                        }
                    }
                }
            }
        } finally {
            reader.close()
        }
        if (suras != 114) fail(id, "expected 114 suras, found $suras")
        if (ayas != 6236) fail(id, "expected 6236 ayas, found $ayas")
    }

    private fun checkFonts(id: String, dir: File, expected: Int) {
        val fonts = dir.walkTopDown().filter { it.isFile && it.name.endsWith(".ttf") }.toList()
        if (fonts.size != expected) fail(id, "expected $expected font files, found ${fonts.size}")
    }
}
