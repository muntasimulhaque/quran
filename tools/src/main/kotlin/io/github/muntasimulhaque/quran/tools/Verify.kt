package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
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
            val file = File(root, dataset.path)
            if (!file.exists()) {
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
            val file = File(root, dataset.path)
            if (!file.exists()) continue
            val dir = extract(file, File(work, dataset.id))
            check(dataset, dir)
        }

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
            "quran-script-kfgqpc" -> checkKfgqpcScript(dataset.id, dir)
            "mushaf-glyph-v2" -> checkGlyphScript(dataset.id, dir)
            "mushaf-layout-v2" -> checkLayout(dataset.id, dir)
            "translation-saheeh" -> checkSaheeh(dataset.id, dir)
            "saheeh-qul-crosscheck" -> checkSimpleCount(dataset.id, dir, "translation", 6236)
            "tafsir-ibn-kathir-en" -> checkTafsirQul(dataset.id, dir)
            "tafsir-as-sadi-ar" -> checkSaadiQuranEnc(dataset.id, dir)
            "word-by-word-english" -> checkWordTranslation(dataset.id, dir)
            "recitation-minshawi", "recitation-husary", "recitation-husary-muallim",
            "recitation-husary-mujawwad" -> checkRecitation(dataset.id, dir)
            "topics" -> checkSimpleCount(dataset.id, dir, "topics", 2512)
            "metadata-surah-names" -> checkJsonKeys(dataset.id, dir, 114)
            "metadata-ayah" -> checkJsonKeys(dataset.id, dir, 6236)
            "metadata-sajda" -> checkJsonKeys(dataset.id, dir, 15)
            "metadata-juz" -> checkJsonKeys(dataset.id, dir, 30)
            "metadata-hizb" -> checkJsonKeys(dataset.id, dir, 60)
            "metadata-rub" -> checkJsonKeys(dataset.id, dir, 240)
            "metadata-manzil" -> checkJsonKeys(dataset.id, dir, 7)
            "metadata-ruku" -> checkJsonMinKeys(dataset.id, dir, 500)
            "mutashabihat" -> checkJsonMinKeys(dataset.id, dir, 100)
            "surah-info-en" -> checkJsonKeys(dataset.id, dir, 114)
            "tanzil-uthmani" -> checkTanzil(dataset.id, dir)
            "mushaf-fonts-v2" -> checkFonts(dataset.id, dir, expected = 604)
            "quran-font-hafs" -> checkFonts(dataset.id, dir, expected = 1)
        }
    }

    private fun db(dir: File): File =
        dir.firstWithExtension(".db", ".sqlite") ?: error("no sqlite database in ${dir.absolutePath}")

    private fun checkKfgqpcScript(id: String, dir: File) {
        openSqlite(db(dir)).use { connection ->
            val rows = connection.count("words")
            if (rows != 83_668L) fail(id, "expected 83668 words, found $rows")
            val surahs = connection.scalarLong("SELECT COUNT(DISTINCT surah) FROM words")
            if (surahs != 114L) fail(id, "expected 114 surahs, found $surahs")
            val ayahs = connection.scalarLong("SELECT COUNT(*) FROM (SELECT DISTINCT surah, ayah FROM words)")
            if (ayahs != 6236L) fail(id, "expected 6236 ayahs, found $ayahs")
            val markers = connection.scalarLong("SELECT COUNT(*) FROM words WHERE text GLOB '*[٠-٩]*'")
            if (markers != 6236L) fail(id, "expected 6236 ayah-number markers, found $markers")
            connection.each("SELECT location, text FROM words") { rs ->
                val location = rs.getString(1)
                if (!location.matches(Regex("^\\d+:\\d+:\\d+$"))) fail(id, "bad location: $location")
                val text = rs.getString(2) ?: ""
                if (text.isBlank()) fail(id, "blank text at $location")
                if (text.contains('\u25CC')) fail(id, "dotted circle artifact at $location")
            }
        }
    }

    private fun checkGlyphScript(id: String, dir: File) {
        openSqlite(db(dir)).use { connection ->
            val rows = connection.count("words")
            if (rows != 83_668L) fail(id, "expected 83668 words, found $rows")
            connection.each("SELECT text FROM words") { rs ->
                for (ch in rs.getString(1) ?: "") {
                    val cp = ch.code
                    if (cp < 0xFB50 || cp > 0xFEFF) {
                        fail(id, "glyph text contains U+${cp.toString(16).uppercase()}, outside presentation forms")
                        return@each
                    }
                }
            }
        }
        val script = openSqlite(
            File(work, "quran-script-kfgqpc").firstWithExtension(".db")
                ?: error("script database not extracted yet"),
        )
        openSqlite(db(dir)).use { glyph ->
            script.use { text ->
                val a = text.createStatement().executeQuery("SELECT id, location FROM words ORDER BY id")
                val b = glyph.createStatement().executeQuery("SELECT id, location FROM words ORDER BY id")
                var mismatches = 0
                while (a.next() && b.next()) {
                    if (a.getInt(1) != b.getInt(1) || a.getString(2) != b.getString(2)) {
                        mismatches++
                        if (mismatches <= 5) {
                            fail(id, "glyph row ${b.getInt(1)} ${b.getString(2)} does not match ${a.getInt(1)} ${a.getString(2)}")
                        }
                    }
                }
                if (mismatches > 5) fail(id, "$mismatches glyph rows do not align with the text rows")
            }
        }
    }

    private fun checkLayout(id: String, dir: File) {
        openSqlite(db(dir)).use { connection ->
            val pages = connection.scalarLong("SELECT COUNT(DISTINCT page_number) FROM pages")
            if (pages != 604L) fail(id, "expected 604 pages, found $pages")
            val rows = connection.count("pages")
            if (rows != 9046L) fail(id, "expected 9046 page lines, found $rows")
            val types = connection.scalarLong("SELECT COUNT(*) FROM pages WHERE line_type NOT IN ('ayah','basmallah','surah_name')")
            if (types != 0L) fail(id, "$types rows have an unknown line type")
            val surahNames = connection.scalarLong("SELECT COUNT(*) FROM pages WHERE line_type='surah_name'")
            if (surahNames != 114L) fail(id, "expected 114 surah name lines, found $surahNames")
            val basmallahs = connection.scalarLong("SELECT COUNT(*) FROM pages WHERE line_type='basmallah'")
            if (basmallahs != 112L) fail(id, "expected 112 basmala lines, found $basmallahs")
            connection.each(
                "SELECT page_number, line_number, line_type, first_word_id, last_word_id FROM pages",
            ) { rs ->
                val line = rs.getInt(2)
                if (line < 1 || line > 15) fail(id, "page ${rs.getInt(1)} has line $line")
                if (rs.getString(3) == "ayah") {
                    val first = rs.getLong(4)
                    val last = rs.getLong(5)
                    if (first < 1 || last > 83_668 || first > last) {
                        fail(id, "page ${rs.getInt(1)} line $line has bad word range $first..$last")
                    }
                }
            }
        }
    }

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
        }
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

    private fun checkSaadiQuranEnc(id: String, dir: File) {
        val file = dir.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".json") }
            ?: return fail(id, "no json file found")
        val element = Json.parseToJsonElement(file.readText())
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> (element["result"] ?: element["tafsirs"])?.jsonArray
            else -> null
        } ?: return fail(id, "unexpected json shape")
        if (array.size < 6500) fail(id, "expected at least 6500 passages, found ${array.size}")
        val counts = surahVerseCounts()
        val covered = BooleanArray(6237)
        for (entry in array) {
            val o = try {
                entry.jsonObject
            } catch (e: IllegalArgumentException) {
                continue
            }
            val sura = o["sura"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val from = o["from_aya"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val to = o["to_aya"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val last = counts[sura]
            if (sura !in 1..114 || from < 1 || to < from || to > last) {
                fail(id, "passage $sura:$from-$to is out of range")
                continue
            }
            for (ayah in from..to) covered[globalIndex(counts, sura, ayah)] = true
        }
        val missing = (1..6236).filter { !covered[it] }
        if (missing.isNotEmpty()) fail(id, "${missing.size} ayahs have no passage")
    }

    private fun surahVerseCounts(): IntArray {
        val counts = IntArray(115)
        val script = File(work, "quran-script-kfgqpc").firstWithExtension(".db")
            ?: error("script database not extracted yet")
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
        if (expected == 604) {
            val missing = (1..604).filter { page -> fonts.none { it.name == "p$page.ttf" } }
            if (missing.isNotEmpty()) fail(id, "missing page fonts: ${missing.take(5)}...")
        }
    }
}
