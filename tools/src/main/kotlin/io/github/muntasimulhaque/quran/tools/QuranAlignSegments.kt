package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * The word regions of one recitation, read from cpfair's quran-align timing
 * file and written as the reading draws them.
 *
 * The QUL export this replaced was cut against a different edit of the same
 * recordings. Measured against the app's own audio (the files the reader gets,
 * fetched and hash-checked against `content/audio-sources.json`), it began 284
 * Husary and 283 Minshawi ayahs some hundreds of milliseconds after the voice,
 * and its mid-ayah boundaries wandered by a median of about half a second. A
 * head that starts late cannot be repaired downstream, and the reader hears a
 * word the wash has not reached. The quran-align timings were cut against the
 * same takes and their heads land within tens of milliseconds of the measured
 * voice (owner decision, 22.7).
 *
 * The file's indexes are the Tanzil quran-uthmani split, which counts no pause
 * mark and does not join the words the QPC print joins. [position] is the whole
 * of that difference, and it is three rules: the joined name of 37:130, the
 * five muqata'ah heads the aligner counted as separate words, and nothing else.
 *
 * A region the aligner could not explain (one whose end is not after its start)
 * is a failed match, not a measurement: it is dropped. A region that spans more
 * than one word names the word it starts with and is held until the next
 * measured region, because the boundary inside the span was never measured and
 * a split would be invented. Every word that ends up with no region of its own
 * is pinned in [UNCOVERED], and the build and `checkdb` both read that list:
 * a new hole is a data surprise for the owner, never a row added to make a gate
 * pass (owner decision, 22.7).
 */
object QuranAlignSegments {

    /** One region as the export writes it: [wordStart, wordEnd, startMs, endMs]. */
    data class ExportSegment(
        val wordStart: Int,
        val wordEnd: Int,
        val startMs: Long,
        val endMs: Long,
    ) {
        /** True when the aligner could not bound this region at all. */
        val failed: Boolean get() = endMs <= startMs

        /** True when the aligner could not split the words inside this region. */
        val spans: Boolean get() = wordEnd - wordStart > 1
    }

    /** One ayah's export regions, in the file's own order. */
    data class Ayah(val surah: Int, val ayah: Int, val segments: List<ExportSegment>) {
        val key: String get() = "$surah:$ayah"
    }

    /** One region as the reading reads it: the drawn word, and its time. */
    data class Region(val word: Int, val startMs: Long, val endMs: Long)

    /**
     * A whole timing file, indexed by `surah:ayah`. Null when the file is not
     * the shape the aligner writes, which the build reports rather than ships.
     */
    fun parse(text: String): List<Ayah>? {
        val array = runCatching { Json.parseToJsonElement(text).jsonArray }.getOrNull() ?: return null
        val ayahs = ArrayList<Ayah>(array.size)
        for (element in array) {
            val entry = runCatching { element.jsonObject }.getOrNull() ?: return null
            val surah = entry["surah"]?.jsonPrimitive?.longOrNull?.toInt() ?: return null
            val ayah = entry["ayah"]?.jsonPrimitive?.longOrNull?.toInt() ?: return null
            val segments = ArrayList<ExportSegment>()
            for (raw in entry["segments"]?.jsonArray.orEmpty()) {
                val parts = runCatching { raw.jsonArray }.getOrNull() ?: return null
                if (parts.size < 4) return null
                segments += ExportSegment(
                    wordStart = parts[0].jsonPrimitive.longOrNull?.toInt() ?: return null,
                    wordEnd = parts[1].jsonPrimitive.longOrNull?.toInt() ?: return null,
                    startMs = parts[2].jsonPrimitive.longOrNull ?: return null,
                    endMs = parts[3].jsonPrimitive.longOrNull ?: return null,
                )
            }
            ayahs += Ayah(surah, ayah, segments)
        }
        return ayahs
    }

    /**
     * The drawn word an export index names, 1-based.
     *
     * The three rules and their evidence:
     *
     * - The joined name of 37:130 is one word in the printed text and two in
     *   the export, so it moves through the same merge the QUL pipeline used.
     * - The five muqata'ah heads in [MUQATA_GAPS] are counted as internal words
     *   by the aligner, so a hole stands in the indexes after the head. The
     *   words after it move back by the number of letters the head was counted
     *   as; the head's own region is word one.
     * - The three ayahs where the QPC print joins a pair the word list keeps
     *   apart (2:181, 8:6, 13:37) need nothing here: the aligner merged those
     *   pairs itself, and its indexes already name the drawn words.
     */
    fun position(reciter: String, surah: Int, ayah: Int, wordStart: Int): Int {
        val skip = MUQATA_GAPS[reciter to "$surah:$ayah"] ?: 0
        if (skip > 0) return if (wordStart == 0) 1 else wordStart - skip + 1
        if (surah == 37 && ayah == 130) return QpcWords.position(37, 130, wordStart + 1)
        return wordStart + 1
    }

    /**
     * The regions the reading uses: each survives [position], a failed region
     * is dropped, a span names its first word, and the list is in time order.
     * [QuranAlignSegments.parse] hands the file back in its own order, which is
     * the ayah's, but a failed region's removal can leave two starts that the
     * reader's walk cannot order, so the sort is the guarantee the runtime
     * reads on.
     */
    fun regions(reciter: String, surah: Int, ayah: Int, segments: List<ExportSegment>): List<Region> =
        segments
            .filterNot { it.failed }
            .map { Region(position(reciter, surah, ayah, it.wordStart), it.startMs, it.endMs) }
            .sortedBy { it.startMs }

    /** The regions as the content database stores them. */
    fun json(regions: List<Region>): String =
        buildJsonArray {
            for (region in regions) {
                add(
                    buildJsonArray {
                        add(JsonPrimitive(region.word - 1))
                        add(JsonPrimitive(region.word))
                        add(JsonPrimitive(region.startMs))
                        add(JsonPrimitive(region.endMs))
                    },
                )
            }
        }.toString()

    /** The drawn words in `1..wordCount` that no region names, in order. */
    fun uncovered(regions: List<Region>, wordCount: Int): List<Int> {
        val named = regions.mapTo(HashSet(regions.size)) { it.word }
        return (1..wordCount).filterNot { it in named }
    }

    /**
     * The muqata'ah heads the aligner counted as separate words, and how many,
     * per reciter and ayah. Each was found by measuring the mapped words'
     * times against the file's own voice (a mapped word whose region began
     * before the head's speech ended was a hole, not an extra word), and each
     * is the only such ayah in the whole file for the reason it is here: the
     * letters of the head are words to the aligner and not to the page.
     */
    val MUQATA_GAPS: Map<Pair<String, String>, Int> = mapOf(
        ("husary" to "12:1") to 1,
        ("husary" to "15:1") to 1,
        ("minshawi" to "15:1") to 1,
        ("minshawi" to "10:1") to 2,
        ("minshawi" to "14:1") to 1,
    )

    /**
     * Every drawn word that ends with no region of its own, per reciter: an
     * aligner hole, the unfilled tail of a span, or the word whose only region
     * was a failed match. The wash holds the word before each of them, the same
     * rest it gives a madd, and both the build and `checkdb` read this list
     * rather than accepting a bare count: a word that leaves it without a
     * reason is a failure, and so is a word that joins it without the owner
     * seeing it (owner decision, 22.7).
     */
    val UNCOVERED: Map<String, Map<String, Set<Int>>> = mapOf(
        "husary" to mapOf(
            "2:125" to setOf(18),
            "3:73" to setOf(16),
            "3:153" to setOf(20),
            "3:154" to setOf(28),
            "4:12" to setOf(19, 68),
            "6:165" to setOf(13),
            "9:66" to setOf(7),
            "9:94" to setOf(6),
            "9:102" to setOf(7),
            "10:50" to setOf(2),
            "12:77" to setOf(2),
            "13:31" to setOf(15, 24),
            "17:95" to setOf(1),
            "20:14" to setOf(4),
            "20:98" to setOf(5),
            "23:44" to setOf(9),
            "26:143" to setOf(2),
            "27:56" to setOf(14),
            "29:8" to setOf(9),
            "29:38" to setOf(6),
            "30:2" to setOf(2),
            "34:30" to setOf(1),
            "39:6" to setOf(2),
            "45:34" to setOf(13),
            "52:33" to setOf(4),
            "56:22" to setOf(2),
            "65:7" to setOf(19),
            "69:2" to setOf(2),
            "74:41" to setOf(1),
        ),
        "minshawi" to mapOf(
            "2:88" to setOf(3),
            "2:157" to setOf(4),
            "2:182" to setOf(3),
            "2:282" to setOf(42),
            "3:7" to setOf(38),
            "3:28" to setOf(15),
            "3:51" to setOf(3, 4, 5, 6, 7),
            "3:167" to setOf(27, 28),
            "4:4" to setOf(14),
            "4:11" to setOf(48, 49),
            "4:43" to setOf(24),
            "5:16" to setOf(2, 3, 4),
            "5:36" to setOf(8),
            "5:106" to setOf(51),
            "5:116" to setOf(23),
            "6:19" to setOf(26),
            "6:58" to setOf(1),
            "6:76" to setOf(13),
            "6:91" to setOf(37),
            "8:52" to setOf(17),
            "10:61" to setOf(25),
            "11:26" to setOf(2),
            "12:87" to setOf(18),
            "15:22" to setOf(9),
            "17:100" to setOf(1),
            "17:107" to setOf(3, 4),
            "20:18" to setOf(10),
            "20:135" to setOf(2),
            "22:41" to setOf(2),
            "25:12" to setOf(3),
            "26:63" to setOf(4),
            "26:161" to setOf(3),
            "28:27" to setOf(25),
            "28:79" to setOf(14),
            "30:8" to setOf(21),
            "30:28" to setOf(8),
            "31:17" to setOf(11),
            "34:30" to setOf(1),
            "41:47" to setOf(8),
            "44:8" to setOf(1),
            "46:14" to setOf(7, 8, 9),
            "46:17" to setOf(27, 28),
            "47:13" to setOf(7),
            "56:52" to setOf(5),
            "58:10" to setOf(10),
            "63:10" to setOf(5),
            "65:8" to setOf(2),
            "68:22" to setOf(1),
            "81:21" to setOf(2, 3),
            "105:4" to setOf(2),
        ),
    )

    /** The timing file each recitation reads, as the build's own map. */
    val DATASETS: Map<String, String> = mapOf(
        "husary" to "quran-align-husary",
        "minshawi" to "quran-align-minshawi",
    )
}
