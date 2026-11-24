package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Writes the voice edges of every shipped recitation file.
 *
 * The recitation file begins with almost every ayah's first word marked at 0
 * and lets the last word run to the end of it, so a wash taken from the word
 * timings alone sits on a word through the lead-in and through the closing
 * pause. This step listens to the files the reader actually gets, with the same
 * `audio packs` output the build publishes, and records the first and the last
 * voiced millisecond of each one.
 *
 * The measurement is [AudioProbe]'s: the file decoded once, whole, a
 * ten-millisecond RMS envelope over its full length, and speech is ten percent
 * of the file's own loud end, with a run of three frames required so one click
 * is not a word.
 *
 * The line is the file's own because a window's is not: the first table was
 * measured with the threshold taken from a 2.5 s head window, and a file that
 * opens with more silence than that turned the window's loud end into the room
 * floor, so the decoder's own noise crossed the line and the table read a
 * first voice at the file's first noise. Minshawi 25:21 and 27:65 recorded
 * 50 ms where the speech begins at 2560 and 2870, and the tail windows of the
 * same table measured the last voice hundreds of milliseconds past its end,
 * into the closing pause. One line taken from one whole file cannot be fooled
 * by either end (owner decision, 22.7).
 *
 * The output is `content/audio-edges.txt`, one line per recitation and ayah:
 * `reciter ayah firstMs lastMs durationMs`. It is read by the app from its
 * assets, and the build's gate checks that every recitation row has a line.
 * The step needs ffmpeg, so it runs on the owner's machine, like the gates
 * that read `content/raw`; the committed table is what CI and the app read.
 *
 * The same table is the head gate's reference: the content build's
 * `checkdb` compares every first word region against the voice this step
 * measured, so a recitation whose timings disagree with its audio stops the
 * release rather than reaching the reader (owner decision, 22.7).
 */
class AudioEdges(private val root: File) {

    private val database = File(root, "content/quran.db")
    private val packs = File(root, "content/work/audio-packs")
    private val work = File(root, "content/work/audio-edges")
    private val output = File(root, "content/audio-edges.txt")

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
        val whole = AudioProbe.probe(file)
        val duration = whole.durationMs ?: return null
        val line = threshold(whole.frames)
        val first = firstSpeech(line, whole.frames)
        val last = lastSpeech(line, whole.frames)
        if (first < 0 || last < 0 || last <= first || last > duration + 100) return null
        return Edge(row.reciter, row.ayah, first, last, duration)
    }

    internal fun threshold(frames: DoubleArray): Double = AudioProbe.threshold(frames)

    internal fun firstSpeech(threshold: Double, frames: DoubleArray): Long =
        AudioProbe.firstSpeech(threshold, frames)

    internal fun lastSpeech(threshold: Double, frames: DoubleArray): Long =
        AudioProbe.lastSpeech(threshold, frames)

    internal fun envelope(bytes: ByteArray): DoubleArray = AudioProbe.envelope(bytes)

    internal fun parseDurationMs(text: String): Long? = AudioProbe.parseDurationMs(text)

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

    private companion object {
        const val WORKERS = 10
    }
}
