package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Writes the voice edges of every shipped recitation file.
 *
 * The recitation export marks almost every first word at 0 and lets the last
 * word run to the end of the file, so a wash taken from it alone sits on a
 * word through the lead-in and through the closing pause. This step listens to
 * the files the reader actually gets, with the same `audio packs` output the
 * build publishes, and records the first and the last voiced millisecond of
 * each one.
 *
 * The measurement is the file's own energy: a ten-millisecond RMS envelope,
 * with speech being ten percent of the window's loud end. A fixed decibel
 * threshold was fooled by a quiet room tone on one recitation and by a long
 * madd on another; the relative line moves with the recording, which is what
 * a reader's ear does. A run of three frames is required, so one click is not
 * a word.
 *
 * The head window is 2.5 s and grows when the file opens with a long silence,
 * so a split that kept six seconds before the first word still finds it. The
 * tail starts at 4 s and grows the same way, so a long closing pause still
 * finds the last word.
 *
 * The output is `content/audio-edges.txt`, one line per recitation and ayah:
 * `reciter ayah firstMs lastMs durationMs`. It is read by the app from its
 * assets, and the build's gate checks that every recitation row has a line.
 * The step needs ffmpeg, so it runs on the owner's machine, like the gates
 * that read `content/raw`; the committed table is what CI and the app read.
 */
class AudioEdges(private val root: File) {

    private val database = File(root, "content/quran.db")
    private val packs = File(root, "content/work/audio-packs")
    private val work = File(root, "content/work/audio-edges")
    private val output = File(root, "content/audio-edges.txt")

    private val headWindows = listOf(2.5, 5.0, 10.0, 20.0, 40.0)
    private val tailWindows = listOf(4, 8, 16, 32, 64, 128, 256)

    fun run(resume: Boolean = false): Int {
        if (!database.exists()) {
            println("edges: content/quran.db is missing; run build first")
            return 1
        }
        if (!packs.isDirectory) {
            println("edges: ${packs.relativeTo(root)} is missing; run tools audio packs first")
            return 1
        }
        val rows = readRows()
        if (rows.isEmpty()) {
            println("edges: the database names no recitation rows")
            return 1
        }
        val byReciterAndName = rows.groupBy { it.reciter }
            .mapValues { (_, list) -> list.associateBy { it.name } }
        val existing = if (resume) readTable() else emptyMap()
        if (existing.size == rows.size) {
            println("edges: the table already covers all ${rows.size} ayahs")
            return 0
        }
        val results = java.util.Collections.synchronizedList(mutableListOf<Edge>())
        val pool = Executors.newFixedThreadPool(WORKERS)
        val done = AtomicInteger()
        try {
            for (reciter in byReciterAndName.keys.sorted()) {
                val folder = File(packs, reciter)
                if (!folder.isDirectory) {
                    println("edges: $reciter has no audio packs; skipping")
                    continue
                }
                val known = byReciterAndName.getValue(reciter)
                val zips = folder.listFiles { file -> file.isFile && file.name.endsWith(".zip") }
                    ?.sortedBy { it.name }.orEmpty()
                for (zip in zips) {
                    val entries = runCatching {
                        java.util.zip.ZipFile(zip).use { archive ->
                            archive.entries().asSequence()
                                .filter { entry -> !entry.isDirectory && entry.name.endsWith(".mp3") }
                                .map { entry -> entry.name }
                                .toList()
                        }
                    }.getOrElse { failure ->
                        println("edges: ${zip.name} cannot be read: ${failure.message}")
                        emptyList()
                    }
                    val pending = entries.filter { name ->
                        val row = known[name] ?: return@filter false
                        "${row.reciter}/${row.ayah}" !in existing
                    }
                    if (pending.isEmpty()) continue
                    val extracted = extract(zip, File(work, "$reciter/${zip.nameWithoutExtension}"))
                    for (file in extracted.listFiles { f -> f.isFile && f.name.endsWith(".mp3") }.orEmpty()) {
                        if (file.name !in pending) continue
                        val row = known[file.name] ?: continue
                        pool.submit {
                            val edge = measure(file, row)
                            if (edge == null) {
                                println("edges: ${reciter}/${file.name} has no readable voice edge")
                            } else {
                                results += edge
                                val count = done.incrementAndGet()
                                if (count % 500 == 0) println("edges: $count/${rows.size}")
                            }
                        }
                    }
                }
            }
        } finally {
            pool.shutdown()
            pool.awaitTermination(12, TimeUnit.HOURS)
        }
        val all = existing.values.toList() + results
        fileLine(rows, all)
        return if (all.size == rows.size) 0 else 1
    }

    /** The rows a previous run already wrote, so a resume only measures the gaps. */
    private fun readTable(): Map<String, Edge> {
        if (!output.isFile) return emptyMap()
        val edges = HashMap<String, Edge>()
        output.forEachLine { line ->
            if (line.isBlank() || line.startsWith("#")) return@forEachLine
            val parts = line.split(' ', '\t').filter { it.isNotEmpty() }
            if (parts.size < 5) return@forEachLine
            val reciter = parts[0]
            val ayah = parts[1].toIntOrNull() ?: return@forEachLine
            val first = parts[2].toLongOrNull() ?: return@forEachLine
            val last = parts[3].toLongOrNull() ?: return@forEachLine
            val duration = parts[4].toLongOrNull() ?: return@forEachLine
            if (last <= first) return@forEachLine
            edges["$reciter/$ayah"] = Edge(reciter, ayah, first, last, duration)
        }
        return edges
    }

    private fun readRows(): List<Row> {
        val rows = mutableListOf<Row>()
        openSqlite(database).use { connection ->
            connection.each(
                "SELECT ra.recitation, ra.audio_path, ra.ayah_number FROM recitation_ayah ra ORDER BY ra.recitation, ra.ayah_number",
            ) { rs ->
                val reciter = rs.getString(1) ?: return@each
                val path = rs.getString(2) ?: return@each
                val ayah = rs.getInt(3)
                val name = path.substringAfterLast('/')
                if (name.isNotBlank()) rows += Row(reciter, name, ayah)
            }
        }
        return rows
    }

    private fun measure(file: File, row: Row): Edge? {
        val head = runFfmpeg(file, headWindows.first(), fromEnd = false)
        val duration = head.durationMs ?: return null
        var first = firstSpeech(threshold(head.frames), head.frames)
        if (first < 0) {
            for (window in headWindows.drop(1)) {
                val wider = runFfmpeg(file, window, fromEnd = false)
                first = firstSpeech(threshold(wider.frames), wider.frames)
                if (first >= 0) break
            }
        }
        if (first < 0) return null
        var last = -1L
        for (window in tailWindows) {
            val windowStart = (duration - (window * 1000).toLong()).coerceAtLeast(0L)
            val tail = runFfmpeg(file, window.toDouble(), fromEnd = true)
            val at = lastSpeech(threshold(tail.frames), tail.frames)
            if (at < 0) continue
            last = (windowStart + at).coerceAtMost(duration)
            break
        }
        if (last < 0 || last <= first || last > duration + 100) return null
        return Edge(row.reciter, row.ayah, first, last, duration)
    }

    /** The line a word has to cross to be heard rather than a room. */
    internal fun threshold(frames: DoubleArray): Double {
        if (frames.isEmpty()) return 1.0
        val sorted = frames.sortedArray()
        val loud = sorted[((sorted.size - 1) * 0.9).toInt()]
        return maxOf(loud * 0.10, MIN_RMS)
    }

    /** The first voiced millisecond, or -1 when the window never reaches the line. */
    internal fun firstSpeech(threshold: Double, frames: DoubleArray): Long {
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
    internal fun lastSpeech(threshold: Double, frames: DoubleArray): Long {
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

    private fun runFfmpeg(file: File, seconds: Double, fromEnd: Boolean): Probe {
        val args = mutableListOf("ffmpeg", "-hide_banner", "-v", "info")
        if (fromEnd) {
            args += listOf("-sseof", "-${formatSeconds(seconds)}")
        } else {
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

    /** One RMS value per 10 ms, on a 0..1 scale. */
    internal fun envelope(bytes: ByteArray): DoubleArray {
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

    /** `Duration: 00:00:04.97, start: ...` in the input's own header. */
    internal fun parseDurationMs(text: String): Long? {
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

    private fun fileLine(rows: List<Row>, results: List<Edge>) {
        val byKey = results.associateBy { "${it.reciter}/${it.ayah}" }
        val lines = rows.sortedWith(compareBy({ it.reciter }, { it.ayah })).mapNotNull { row ->
            val edge = byKey["${row.reciter}/${row.ayah}"] ?: return@mapNotNull null
            "${edge.reciter} ${edge.ayah} ${edge.firstMs} ${edge.lastMs} ${edge.durationMs}"
        }
        output.writeText(
            buildString {
                appendLine("# reciter ayah firstMs lastMs durationMs")
                appendLine("# read from the shipped audio by tools audio edges; do not edit by hand")
                lines.forEach { appendLine(it) }
            },
        )
        println("edges: ${lines.size} of ${rows.size} ayahs from ${rows.map { it.reciter }.distinct().size} recitations")
        println("edges: ${output.relativeTo(root)}, sha256 ${sha256(output)}")
    }

    private data class Row(val reciter: String, val name: String, val ayah: Int)

    private data class Edge(val reciter: String, val ayah: Int, val firstMs: Long, val lastMs: Long, val durationMs: Long)

    private data class Probe(val durationMs: Long?, val frames: DoubleArray)

    private companion object {
        const val WORKERS = 10
        const val SAMPLE_RATE = 16_000
        const val HOP_MS = 10
        const val RUN = 3
        const val MIN_RMS = 0.004

        private val DURATION = Regex("""Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)""")
    }
}
