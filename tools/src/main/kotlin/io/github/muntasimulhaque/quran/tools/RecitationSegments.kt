package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * One stored recitation ayah's word regions, read back for the content gate.
 *
 * The regions are written by [QuranAlignSegments] and read here in the one
 * shape the database holds: `[wordFrom, wordTo, startMs, endMs]`, with
 * `wordTo` the 1-based drawn word a region names. `checkdb` uses this to prove
 * that every region names a word the reading draws and that no drawn word has
 * gone missing from the file (owner decision, 22.7).
 *
 * The three-number shape here is the older QUL export's, kept readable so a
 * database built before the timing change still parses under the gate. No
 * builder writes it any more.
 */
object RecitationSegments {

    data class Region(
        val wordFrom: Int,
        val wordTo: Int,
        val startMs: Long,
        val endMs: Long,
    )

    /**
     * The regions of one ayah, or null when the row is not a readable list of
     * regions. The build reports an unreadable row rather than shipping it.
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
}
