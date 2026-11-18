package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * One recitation export's word regions, read as the reading draws them.
 *
 * The export writes a region as [wordFrom, wordTo, startMs, endMs], with
 * [wordTo] the 1-based word. Some ayahs open with a 70 ms (sometimes 90 ms)
 * fragment of the first word as a region of its own, and the word itself is
 * the next region, numbered 2. The whole list is then one word ahead, the
 * last drawn word loses its region, and a phone that reads [wordTo] as it
 * stands washes the wrong word for the whole ayah. A real first word is
 * never that short in the export (the shortest is 100 ms), so the fragment
 * is the one shape this corrects.
 *
 * The correction is made once, here, so every reader of the content gets the
 * same word order and the app's player stays a plain read of the data (owner
 * decision, 9.8). The ayah's number, which the export keeps as the
 * last word past the drawn words, is folded onto the last drawn word so the
 * verse keeps its mark through the closing silence.
 */
object RecitationSegments {

    /** The fragment QUL sometimes opens an ayah with. */
    private const val LEADING_FRAGMENT_MS = 90L

    /**
     * The one recording whose export stops short of an ayah's last drawn
     * word, with the last voiced millisecond of that ayah's own file,
     * measured from the published audio the reader gets.
     *
     * QUL's aligner segmented Al-Mujadila 22 into fifty words where the print
     * draws fifty-one, so the Abdul Basit export's last region ends at word
     * 50 and the closing word, al-muflihun, has no region to wash (owner
     * report, 20.0: checkdb's segment gate named the one ayah). The other
     * voices carry the same tail through QUL's number-marker region, which
     * the build clamps onto the last drawn word; this export has no region
     * past word 50 at all. The erratum gives the word the span between the
     * export's own last region and this measurement, and the app's
     * voice-edge table holds the same 81,140 ms, so the wash still stops
     * with the voice. `AudioEdgeTableTest` pins the agreement so the
     * recorded number can never drift from the audio it was measured from.
     */
    val TAIL_VOICE_EDGES: Map<Pair<String, Int>, Long> = mapOf(
        ("abdulbasit" to 5126) to 81_140L,
    )

    /**
     * Extends a corrected segment list onto the ayah's last drawn word when
     * the export stopped at the word before it. A list that already covers
     * the last word is returned as it stands, so only a measured row ever
     * changes. A list short of more than the last word, or a short list with
     * no measurement on record, is an error the build stops on: a region
     * that cannot be measured is never invented.
     */
    fun extendTail(recitation: String, ayahNumber: Int, json: String, wordCount: Int): String {
        if (wordCount <= 0) return json
        val regions = parse(json) ?: return json
        if (regions.isEmpty()) return json
        val covered = regions.maxOf { it.wordTo }
        if (covered >= wordCount) return json
        if (covered != wordCount - 1) {
            error("recitation $recitation ayah $ayahNumber covers only word $covered of $wordCount")
        }
        val measured = TAIL_VOICE_EDGES[recitation to ayahNumber]
            ?: error("recitation $recitation ayah $ayahNumber ends a word short with no measured tail")
        val start = regions.maxOf { it.endMs }
        val extended = buildJsonArray {
            for (region in regions) {
                add(
                    buildJsonArray {
                        add(JsonPrimitive(region.wordFrom))
                        add(JsonPrimitive(region.wordTo))
                        add(JsonPrimitive(region.startMs))
                        add(JsonPrimitive(region.endMs))
                    },
                )
            }
            add(
                buildJsonArray {
                    add(JsonPrimitive(wordCount - 1))
                    add(JsonPrimitive(wordCount))
                    add(JsonPrimitive(start))
                    add(JsonPrimitive(measured))
                },
            )
        }
        return extended.toString()
    }

    data class Region(
        val wordFrom: Int,
        val wordTo: Int,
        val startMs: Long,
        val endMs: Long,
        /** True when the source row was the older three-number QUL shape. */
        val legacy: Boolean = false,
    )

    data class Correction(
        val json: String,
        val shifted: Boolean,
        val clamped: Int,
        /** Regions whose source word number moved through the QPC merge. */
        val mapped: Int = 0,
    )

    /**
     * Reads the export's regions. A three-number region is the older QUL
     * shape [word, start, end]; the four-number shape is
     * [wordFrom, wordTo, start, end]. Null when the row is not a readable
     * list of regions, which the build reports rather than ships.
     */
    fun parse(json: String): List<Region>? {
        val array = runCatching { Json.parseToJsonElement(json).jsonArray }.getOrNull() ?: return null
        val regions = ArrayList<Region>(array.size)
        for (element in array) {
            val entry = runCatching { element.jsonArray }.getOrNull() ?: return null
            val region = when (entry.size) {
                3 -> {
                    val word = entry[0].jsonPrimitive.longOrNull?.toInt() ?: return null
                    Region(
                        wordFrom = word - 1,
                        wordTo = word,
                        startMs = entry[1].jsonPrimitive.longOrNull ?: return null,
                        endMs = entry[2].jsonPrimitive.longOrNull ?: return null,
                        legacy = true,
                    )
                }
                4 -> Region(
                    wordFrom = entry[0].jsonPrimitive.longOrNull?.toInt() ?: return null,
                    wordTo = entry[1].jsonPrimitive.longOrNull?.toInt() ?: return null,
                    startMs = entry[2].jsonPrimitive.longOrNull ?: return null,
                    endMs = entry[3].jsonPrimitive.longOrNull ?: return null,
                )
                else -> return null
            }
            if (region.wordTo < 1) return null
            regions += region
        }
        return regions
    }

    /**
     * The normalized JSON for one ayah. The original string is returned when
     * nothing needs to move, so an ayah the export wrote correctly keeps its
     * exact bytes.
     *
     * [remap] carries the QPC word merges into the recitation. The export's
     * word numbers are the words of the word list the recitation was timed
     * against; when two of those words are joined in the printed text, both
     * of their regions must name the joined word, and the words after the
     * join move down by one. Without the map the wash is one word ahead from
     * the join to the end of the ayah (owner report, the QPC rebuild audit).
     */
    fun correct(
        json: String,
        regions: List<Region>,
        wordCount: Int,
        remap: (Int) -> Int = { it },
    ): Correction {
        if (wordCount <= 0 || regions.isEmpty()) return Correction(json, shifted = false, clamped = 0)
        val first = regions.first()
        val maxWord = regions.maxOf { it.wordTo }
        val shifted = first.endMs >= first.startMs &&
            first.endMs - first.startMs <= LEADING_FRAGMENT_MS &&
            maxWord > wordCount
        var mapped = 0
        for (region in regions) {
            val raw = region.wordTo - if (shifted) 1 else 0
            if (remap(raw) != raw) mapped++
        }
        val rewrite = shifted || maxWord > wordCount || mapped > 0 ||
            regions.any { it.legacy || it.wordFrom != it.wordTo - 1 }
        if (!rewrite) return Correction(json, shifted = false, clamped = 0)
        var clamped = 0
        val normalized = buildJsonArray {
            for (region in regions) {
                val raw = region.wordTo - if (shifted) 1 else 0
                val target = remap(raw)
                val word = target.coerceIn(1, wordCount)
                if (word != target) clamped++
                add(
                    buildJsonArray {
                        add(JsonPrimitive(word - 1))
                        add(JsonPrimitive(word))
                        add(JsonPrimitive(region.startMs))
                        add(JsonPrimitive(region.endMs))
                    },
                )
            }
        }
        return Correction(normalized.toString(), shifted, clamped, mapped)
    }
}
