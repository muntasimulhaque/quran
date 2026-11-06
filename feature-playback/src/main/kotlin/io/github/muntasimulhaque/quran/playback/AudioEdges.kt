package io.github.muntasimulhaque.quran.playback

import android.content.Context

/**
 * Where the voice starts and stops in each ayah's own file.
 *
 * The recitation export starts almost every first word at 0 and lets the last
 * word run to the end of the file, so a wash taken from it alone sits on a
 * word through the lead-in and through the closing pause. The content build
 * listens to every shipped file and writes this table: two numbers per ayah,
 * the first and last voiced millisecond. The app reads it from its own assets
 * and the wash holds its tongue outside those edges.
 *
 * The table is `content/audio-edges.txt`, one line per recitation and ayah:
 * `reciter ayah firstMs lastMs durationMs`. The duration is kept for the
 * build's gate and ignored here. A file with no line, or a line the audio
 * cannot explain, has no edge and reads exactly as it did before (owner
 * decision, this session).
 */
internal class AudioEdges private constructor(
    private val recitations: Map<String, LongArray>,
) {

    fun firstMs(recitation: String, ayahNumber: Int): Long {
        val packed = recitations[recitation]?.getOrNull(ayahNumber) ?: return NO_EDGE_MS
        if (packed == NO_EDGE_MS) return NO_EDGE_MS
        return packed ushr 32
    }

    fun lastMs(recitation: String, ayahNumber: Int): Long {
        val packed = recitations[recitation]?.getOrNull(ayahNumber) ?: return NO_EDGE_MS
        if (packed == NO_EDGE_MS) return NO_EDGE_MS
        return packed and 0xffff_ffffL
    }

    companion object {

        const val ASSET = "content/audio-edges.txt"

        private const val AYAH_COUNT = 6236

        /** The longest an ayah's file can be. 2:282 is a few minutes, never an hour. */
        private const val MAX_EDGE_MS = 3_600_000L

        /**
         * Reads the table, ignoring anything it cannot explain. A missing or
         * corrupt asset leaves every ayah without an edge, which is the old
         * reading and never a crash.
         */
        fun load(context: Context): AudioEdges =
            runCatching {
                context.assets.open(ASSET).bufferedReader().useLines { parse(it) }
            }.getOrElse { AudioEdges(emptyMap()) }

        fun parse(lines: Sequence<String>): AudioEdges {
            val recitations = HashMap<String, LongArray>()
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val parts = line.split(' ', '\t').filter { it.isNotEmpty() }
                if (parts.size < 4) continue
                val reciter = parts[0]
                val ayah = parts[1].toIntOrNull() ?: continue
                val first = parts[2].toLongOrNull() ?: continue
                val last = parts[3].toLongOrNull() ?: continue
                if (ayah !in 1..AYAH_COUNT) continue
                if (first < 0 || last <= first || last > MAX_EDGE_MS) continue
                val edges = recitations.getOrPut(reciter) {
                    LongArray(AYAH_COUNT + 1) { NO_EDGE_MS }
                }
                edges[ayah] = (first shl 32) or (last and 0xffff_ffffL)
            }
            return AudioEdges(recitations)
        }
    }
}
