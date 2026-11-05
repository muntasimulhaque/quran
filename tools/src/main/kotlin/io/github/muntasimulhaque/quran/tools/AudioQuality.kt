package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The numbers one recitation transcode has to keep.
 *
 * The timing segments in the database are positions inside the ayah's file.
 * An encoder that changes the file's start, its length, or its loudness moves
 * every word wash with it, so the build measures the decoded Opus against the
 * decoded source: the same sample rate, the same duration, the same level,
 * and a waveform that still follows the recitation. The waveform's drift is
 * measured at the start, the middle, and the end of the ayah, which is the
 * three-point check the owner asked for, and it is done for every file rather
 * than a sample of them.
 *
 * These are engineering gates, not a listening test. The owner's ear is the
 * only thing that decides whether the recitation still sounds like the
 * recitation, and the blind sheet is built for that separately (owner
 * decision, the Opus migration).
 */
internal const val OPUS_FORMAT = "opus"
internal const val OPUS_SAMPLE_RATE = 48_000
internal const val OPUS_CHANNELS = 1
internal const val OPUS_STANDARD_KBPS = 64
internal const val OPUS_RAISED_KBPS = 96

/** The longest a transcode may differ from its source before a player drifts. */
internal const val MAX_DURATION_DELTA_MS = 50.0

/** A transcode below this has lost the recitation, not just its air. */
internal const val MIN_SNR_DB = 12.0

/** The same floor, averaged over 20 ms frames so a quiet ayah cannot hide. */
internal const val MIN_SEGMENTAL_SNR_DB = 10.0

/** Equal-weight mono should sit at the source's own level, not 3 dB over it. */
internal const val MAX_LEVEL_DELTA_DB = 3.0

/** The owner's follow-recitation bound, at the start, middle, and end. */
internal const val MAX_DRIFT_MS = 100.0

internal data class AyahQuality(
    val snrDb: Double,
    val segmentalSnrDb: Double,
    val levelDeltaDb: Double,
    val durationDeltaMs: Double,
    val driftMs: Double,
    val sourceMs: Double,
    val opusMs: Double,
) {
    val passed: Boolean
        get() = durationDeltaMs <= MAX_DURATION_DELTA_MS &&
            snrDb >= MIN_SNR_DB &&
            segmentalSnrDb >= MIN_SEGMENTAL_SNR_DB &&
            abs(levelDeltaDb) <= MAX_LEVEL_DELTA_DB &&
            driftMs <= MAX_DRIFT_MS

    fun sentence(): String =
        "snr %.1f dB, segmental %.1f dB, level %.2f dB, duration %.1f ms, drift %.0f ms".format(
            snrDb,
            segmentalSnrDb,
            levelDeltaDb,
            durationDeltaMs,
            driftMs,
        )
}

internal data class OpusHead(val channels: Int, val inputRate: Int, val preSkip: Int)

/**
 * Reads the Opus identification header from the first Ogg page. The payload
 * follows the page's segment table, not a fixed offset, so a future muxer
 * that writes a larger first page still reads the same fields.
 */
internal fun readOpusHead(file: File): OpusHead? {
    if (file.length() < 28) return null
    val head = ByteArray(128)
    var read = 0
    file.inputStream().use { input ->
        while (read < head.size) {
            val count = input.read(head, read, head.size - read)
            if (count < 0) break
            read += count
        }
    }
    if (read < 44) return null
    if (String(head, 0, 4, Charsets.US_ASCII) != "OggS") return null
    if (head[4].toInt() != 0) return null
    val segments = head[26].toInt() and 0xFF
    if (segments < 1) return null
    val payload = 27 + segments
    if (payload + 19 > read) return null
    if (String(head, payload, 8, Charsets.US_ASCII) != "OpusHead") return null
    val version = head[payload + 8].toInt() and 0xFF
    if (version > 15) return null
    val channels = head[payload + 9].toInt() and 0xFF
    val preSkip = (head[payload + 10].toInt() and 0xFF) or
        ((head[payload + 11].toInt() and 0xFF) shl 8)
    val rate = (head[payload + 12].toInt() and 0xFF) or
        ((head[payload + 13].toInt() and 0xFF) shl 8) or
        ((head[payload + 14].toInt() and 0xFF) shl 16) or
        ((head[payload + 15].toInt() and 0xFF) shl 24)
    return OpusHead(channels, rate, preSkip)
}

internal fun isMonoOpus48(file: File): Boolean {
    val head = readOpusHead(file) ?: return false
    return head.channels == OPUS_CHANNELS && head.inputRate == OPUS_SAMPLE_RATE
}

/**
 * Decodes one source MP3 and one Opus file and measures the transcode.
 *
 * [scratch] is a directory this call owns; the two PCM files are removed
 * before it returns. The source is decoded through the same equal-weight
 * mono mix and 48 kHz resample the encoder used, so a difference in the
 * measurement is the encoder's, not the reference's.
 */
internal fun measureAyah(source: File, opus: File, scratch: File): AyahQuality {
    val head = readOpusHead(opus)
        ?: throw IllegalStateException("the encoded file is not an Ogg Opus file")
    if (head.channels != OPUS_CHANNELS || head.inputRate != OPUS_SAMPLE_RATE) {
        throw IllegalStateException(
            "the encoded file is ${head.channels} channel(s) at ${head.inputRate} Hz",
        )
    }
    val sourceRaw = File(scratch, "source.raw")
    val opusRaw = File(scratch, "opus.raw")
    try {
        decode(source, sourceRaw, downmix = true)
        decode(opus, opusRaw, downmix = false)
        return compare(sourceRaw, opusRaw)
    } finally {
        sourceRaw.delete()
        opusRaw.delete()
    }
}

private fun decode(file: File, target: File, downmix: Boolean) {
    val command = mutableListOf(
        "ffmpeg",
        "-hide_banner",
        "-loglevel", "error",
        "-y",
        "-i", file.absolutePath,
    )
    if (downmix) {
        command += listOf(
            "-map", "0:a:0",
            "-af", "aresample=48000:resampler=soxr:precision=28,pan=mono|c0=0.5*c0+0.5*c1",
        )
    }
    command += listOf("-f", "s16le", "-acodec", "pcm_s16le", target.absolutePath)
    val process = ProcessBuilder(command)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .start()
    val error = process.errorStream.bufferedReader().readText()
    val finished = process.waitFor(10, TimeUnit.MINUTES)
    if (!finished) process.destroyForcibly()
    if (!finished || process.exitValue() != 0) {
        throw IllegalStateException(
            "ffmpeg could not decode ${file.name}: ${error.trim().take(300)}",
        )
    }
}

private class BlockReader(file: File) : AutoCloseable {
    private val input = BufferedInputStream(FileInputStream(file), 1 shl 16)
    private val bytes = ByteArray(2 * BLOCK_SAMPLES)
    val samples = ShortArray(BLOCK_SAMPLES)
    var total = 0L
        private set

    /** Fills [samples] with the next block; returns its sample count. */
    fun read(): Int {
        var read = 0
        while (read < bytes.size) {
            val count = input.read(bytes, read, bytes.size - read)
            if (count < 0) break
            read += count
        }
        val samplesRead = read / 2
        for (index in 0 until samplesRead) {
            samples[index] = (
                (bytes[2 * index].toInt() and 0xFF) or
                    (bytes[2 * index + 1].toInt() shl 8)
                ).toShort()
        }
        total += samplesRead
        return samplesRead
    }

    override fun close() {
        input.close()
    }

    private companion object {
        const val BLOCK_SAMPLES = 48_000
    }
}

private fun compare(sourceRaw: File, opusRaw: File): AyahQuality {
    val sourceSamples = sourceRaw.length() / 2
    val opusSamples = opusRaw.length() / 2
    var sumSource = 0.0
    var sumEncoded = 0.0
    var sumDifference = 0.0
    var segmentSum = 0.0
    var segmentCount = 0
    var frameSource = 0.0
    var frameDifference = 0.0
    var frameCount = 0

    BlockReader(sourceRaw).use { source ->
        BlockReader(opusRaw).use { opus ->
            while (true) {
                val sourceCount = source.read()
                val opusCount = opus.read()
                if (sourceCount == 0 && opusCount == 0) break
                val count = min(sourceCount, opusCount)
                for (index in 0 until count) {
                    val x = source.samples[index].toDouble()
                    val y = opus.samples[index].toDouble()
                    val difference = x - y
                    sumSource += x * x
                    sumEncoded += y * y
                    sumDifference += difference * difference
                    frameSource += x * x
                    frameDifference += difference * difference
                    frameCount++
                    if (frameCount == FRAME_SAMPLES) {
                        if (frameSource / FRAME_SAMPLES > SILENCE_MEAN_SQUARE) {
                            segmentSum += (
                                10 * log10(frameSource / max(1.0, frameDifference))
                                ).coerceIn(-10.0, 35.0)
                            segmentCount++
                        }
                        frameSource = 0.0
                        frameDifference = 0.0
                        frameCount = 0
                    }
                }
            }
        }
    }
    // The last frame of a file may be shorter than 20 ms. It is still heard,
    // so it is measured rather than dropped.
    if (frameCount > 0 && frameSource / frameCount > SILENCE_MEAN_SQUARE) {
        segmentSum += (
            10 * log10(frameSource / max(1.0, frameDifference))
            ).coerceIn(-10.0, 35.0)
        segmentCount++
    }

    val snr = 10 * log10(sumSource / max(1.0, sumDifference))
    val segmental = if (segmentCount == 0) 0.0 else segmentSum / segmentCount
    val level = 10 * log10(sumEncoded / max(1.0, sumSource))
    val sourceMs = sourceSamples / (OPUS_SAMPLE_RATE / 1000.0)
    val opusMs = opusSamples / (OPUS_SAMPLE_RATE / 1000.0)
    val drift = waveformDrift(sourceRaw, opusRaw)
    return AyahQuality(
        snrDb = snr,
        segmentalSnrDb = segmental,
        levelDeltaDb = level,
        durationDeltaMs = abs(sourceMs - opusMs),
        driftMs = drift,
        sourceMs = sourceMs,
        opusMs = opusMs,
    )
}

/**
 * The waveform's own lag at 10, 50, and 90 percent of the ayah.
 *
 * A 1 s window of the decoded source is slid past the decoded Opus in 10 ms
 * steps over +/-200 ms and the normalized correlation is read at each step.
 * The alignment is real only when the shifted waveform is clearly better
 * than the unshifted one; a quiet tail can make a distant lag look better by
 * a fraction without any shift, and that is not a drift (owner report, the
 * Opus migration). A true 100 ms shift is found because the correlation at
 * 100 ms is near 1 while the correlation at 0 has collapsed.
 */
private fun waveformDrift(sourceRaw: File, opusRaw: File): Double {
    val sourceSamples = sourceRaw.length() / 2
    val opusSamples = opusRaw.length() / 2
    val common = min(sourceSamples, opusSamples)
    if (common < 2L * OPUS_SAMPLE_RATE) return 0.0
    val window = min(OPUS_SAMPLE_RATE.toLong(), common / 3).toInt()
    val maxLagSamples = 200 * OPUS_SAMPLE_RATE / 1000
    var worst = 0.0
    RandomAccessFile(sourceRaw, "r").use { source ->
        RandomAccessFile(opusRaw, "r").use { opus ->
            for (fraction in doubleArrayOf(0.1, 0.5, 0.9)) {
                val start = ((common - window) * fraction).toLong()
                val sourceWindow = readWindow(source, start, window)
                // A quiet window is a breath or a room, not a phrase: its
                // waveform correlation is noise and cannot prove a shift.
                val sourceEnergySum = sourceWindow.sumOf { sample ->
                    sample.toDouble() * sample.toDouble()
                }
                if (sourceEnergySum / window < MIN_WINDOW_MEAN_SQUARE) continue
                val rangeStart = max(0L, start - maxLagSamples)
                val rangeEnd = min(common, start + window + maxLagSamples)
                val opusRange = readWindow(opus, rangeStart, (rangeEnd - rangeStart).toInt())
                var best = -1.0
                var bestLag = 0
                var zero = -1.0
                for (lagMs in -200..200 step 10) {
                    val lag = lagMs.toLong() * OPUS_SAMPLE_RATE / 1000
                    val opusStart = start + lag
                    if (opusStart < 0 || opusStart + window > common) continue
                    val offset = (opusStart - rangeStart).toInt()
                    if (offset < 0 || offset + window > opusRange.size) continue
                    var dot = 0.0
                    var opusEnergy = 0.0
                    for (index in 0 until window) {
                        val x = sourceWindow[index].toDouble()
                        val y = opusRange[offset + index].toDouble()
                        dot += x * y
                        opusEnergy += y * y
                    }
                    if (opusEnergy / window < MIN_WINDOW_MEAN_SQUARE) continue
                    val score = dot / sqrt(sourceEnergySum * opusEnergy)
                    if (lagMs == 0) zero = score
                    if (score > best) {
                        best = score
                        bestLag = lagMs
                    }
                }
                if (best < 0.0) continue
                // A real shift leaves the unshifted correlation well below
                // the shifted one. A periodic madd can score highly at both,
                // so a high zero-lag score always means no measurable shift.
                val chosen = if (zero < 0.0 || zero >= 0.9 || zero >= best - 0.02) 0 else bestLag
                worst = max(worst, abs(chosen).toDouble())
            }
        }
    }
    return worst
}

private fun readWindow(file: RandomAccessFile, startSample: Long, count: Int): ShortArray {
    if (count <= 0) return ShortArray(0)
    file.seek(startSample * 2)
    val bytes = ByteArray(count * 2)
    file.readFully(bytes)
    return ShortArray(count) { index ->
        ((bytes[2 * index].toInt() and 0xFF) or
            (bytes[2 * index + 1].toInt() shl 8)).toShort()
    }
}

/**
 * Reads the quality report this build wrote. The verifier refuses to report
 * a pass for a surah the build did not measure, so an empty or old report is
 * not treated as good news.
 */
internal fun readQualityReport(file: File): MutableMap<Pair<String, Int>, SurahQuality> {
    if (!file.exists()) return mutableMapOf()
    val root = runCatching {
        Json.parseToJsonElement(file.readText()).jsonObject
    }.getOrNull() ?: return mutableMapOf()
    if (root["format"]?.jsonPrimitive?.content != OPUS_FORMAT) return mutableMapOf()
    val out = mutableMapOf<Pair<String, Int>, SurahQuality>()
    val entries = root["surahs"] as? JsonArray ?: return out
    for (entry in entries) {
        val row = entry.jsonObject
        val reciter = row["reciter"]?.jsonPrimitive?.content ?: continue
        val surah = row["surah"]?.jsonPrimitive?.content?.toIntOrNull() ?: continue
        val failed = (row["failed"] as? JsonArray).orEmpty()
            .mapNotNull { it.jsonPrimitive.contentOrNull }
        out[reciter to surah] = SurahQuality(
            reciter = reciter,
            surah = surah,
            bitrate = row["bitrate"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: OPUS_STANDARD_KBPS,
            ayahs = row["ayahs"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            totalDurationMs = row["totalDurationMs"]
                ?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            minSnrDb = row["minSnrDb"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            minSegmentalSnrDb = row["minSegmentalSnrDb"]
                ?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            maxLevelDeltaDb = row["maxLevelDeltaDb"]
                ?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            maxDurationDeltaMs = row["maxDurationDeltaMs"]
                ?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            maxDriftMs = row["maxDriftMs"]
                ?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            failed = failed,
        )
    }
    return out
}

private const val FRAME_SAMPLES = 960

/** -45 dBFS, the point below which a frame of recitation is a breath. */
private const val SILENCE_MEAN_SQUARE = 33_856.0

/** -40 dBFS: a window quieter than this is a breath, not a phrase. */
private const val MIN_WINDOW_MEAN_SQUARE = 90_000.0
