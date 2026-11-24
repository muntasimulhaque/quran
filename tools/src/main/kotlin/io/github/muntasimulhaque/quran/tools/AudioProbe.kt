package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * What a recitation file's own energy says about where its voice is.
 *
 * One measurement serves two steps: `audio edges` reads a file's first and
 * last voiced millisecond, and `audio words` reads where its words begin and
 * whether a word region agrees. Both ask the same question of the same decoded
 * audio, so they ask it here.
 *
 * The line a word has to cross is relative to the frames' own loud end, which
 * is what a reader's ear does: a fixed decibel threshold was fooled by a quiet
 * room tone on one recitation and by a long madd on another. A run of three
 * frames is required, so one click is not a word.
 */
object AudioProbe {

    /** One decode's header duration and its ten-millisecond energy frames. */
    data class Probe(val durationMs: Long?, val frames: DoubleArray)

    /** One stretch of the file that is either voiced or not. */
    data class Run(val voiced: Boolean, val startMs: Long, val endMs: Long) {
        val lengthMs: Long get() = endMs - startMs
    }

    /**
     * The frames of one file, or the first [seconds] of it, or its last
     * [seconds] when [fromEnd] is set. Null frames when ffmpeg cannot read the
     * file, which every caller reports rather than guesses around.
     */
    fun probe(file: File, seconds: Double? = null, fromEnd: Boolean = false): Probe {
        val args = mutableListOf("ffmpeg", "-hide_banner", "-v", "info")
        if (fromEnd) {
            args += listOf("-sseof", "-${formatSeconds(seconds ?: return Probe(null, DoubleArray(0)))}")
        } else if (seconds != null) {
            args += listOf("-t", formatSeconds(seconds))
        }
        args += listOf("-i", file.absolutePath, "-ac", "1", "-ar", "$SAMPLE_RATE", "-f", "s16le", "-")
        val process = ProcessBuilder(args).start()
        val bytes = process.inputStream.readBytes()
        val error = process.errorStream.bufferedReader().use { it.readText() }
        process.waitFor(2, TimeUnit.MINUTES)
        if (process.isAlive) process.destroyForcibly()
        return Probe(parseDurationMs(error), envelope(bytes))
    }

    /** One RMS value per ten milliseconds, on a 0..1 scale. */
    fun envelope(bytes: ByteArray): DoubleArray {
        val samples = bytes.size / 2
        val hop = SAMPLE_RATE / (1000 / HOP_MS)
        val frames = ArrayList<Double>(samples / hop + 1)
        var index = 0
        while (index + hop <= samples) {
            var sum = 0.0
            for (offset in 0 until hop) {
                val at = (index + offset) * 2
                val value = ((bytes[at + 1].toInt() shl 8) or (bytes[at].toInt() and 0xff)).toShort()
                val normalized = value / 32768.0
                sum += normalized * normalized
            }
            frames += sqrt(sum / hop)
            index += hop
        }
        return frames.toDoubleArray()
    }

    /** The line a word has to cross to be heard rather than a room. */
    fun threshold(frames: DoubleArray): Double {
        if (frames.isEmpty()) return 1.0
        val sorted = frames.sortedArray()
        val loud = sorted[((sorted.size - 1) * 0.9).toInt()]
        return maxOf(loud * 0.10, MIN_RMS)
    }

    /** The first voiced millisecond, or -1 when the window never reaches the line. */
    fun firstSpeech(threshold: Double, frames: DoubleArray): Long {
        var run = 0
        for (index in frames.indices) {
            if (frames[index] > threshold) {
                run++
                if (run >= RUN) return (index - RUN + 1).toLong() * HOP_MS
            } else {
                run = 0
            }
        }
        return -1
    }

    /** The end of the last voiced millisecond, or -1 when nothing is voiced. */
    fun lastSpeech(threshold: Double, frames: DoubleArray): Long {
        var run = 0
        for (index in frames.indices.reversed()) {
            if (frames[index] > threshold) {
                run++
                if (run >= RUN) return (index + RUN).toLong() * HOP_MS
            } else {
                run = 0
            }
        }
        return -1
    }

    /**
     * The file as runs of voice and quiet, each at least [MIN_RUN_MS] long so a
     * single frame of room tone is not a pause and a single frame of breath is
     * not a word. `audio words` reads the onsets it finds in these.
     */
    fun speechRuns(frames: DoubleArray, threshold: Double): List<Run> {
        val runs = ArrayList<Run>(frames.size / 8)
        var current: Run? = null
        for (index in frames.indices) {
            val voiced = frames[index] > threshold
            val at = index.toLong() * HOP_MS
            val open = current
            if (open != null && open.voiced == voiced) {
                current = open.copy(endMs = at + HOP_MS)
            } else {
                if (open != null && open.lengthMs >= MIN_RUN_MS) runs += open
                current = Run(voiced, at, at + HOP_MS)
            }
        }
        current?.let { if (it.lengthMs >= MIN_RUN_MS) runs += it }
        return runs
    }

    /** `Duration: 00:00:04.97, start: ...` in the input's own header. */
    fun parseDurationMs(text: String): Long? {
        val match = DURATION.find(text) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: return null
        val minutes = match.groupValues[2].toLongOrNull() ?: return null
        val seconds = match.groupValues[3].toDoubleOrNull() ?: return null
        return ((hours * 3600 + minutes * 60) * 1000 + (seconds * 1000).roundToLong())
    }

    private fun formatSeconds(seconds: Double): String {
        val whole = seconds.toInt()
        val fraction = ((seconds - whole) * 10).roundToLong()
        return if (fraction == 0L) "$whole" else "$whole.$fraction"
    }

    private const val SAMPLE_RATE = 16_000
    private const val HOP_MS = 10
    private const val RUN = 3
    private const val MIN_RUN_MS = 30L
    private const val MIN_RMS = 0.004

    private val DURATION = Regex("""Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)""")
}
