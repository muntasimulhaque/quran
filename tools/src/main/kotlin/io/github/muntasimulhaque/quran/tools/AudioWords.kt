package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Measures a recitation's word regions against its own audio, on a sample.
 *
 * The phone's own ear cannot run in CI, and a timing file can disagree with the
 * recording it was cut from without anything failing: that is how the QUL
 * export shipped for two recitations with 284 and 283 heads beginning after
 * their first voice. This step listens to the files the reader gets, on the
 * first ayah of every surah, every twentieth ayah of the Book, and every ayah
 * the pipeline pins as an exception, and reports two things per reciter:
 *
 * - every sampled ayah whose first region begins more than [HEAD_TRAIL_MS]
 *   after the file's measured first voice, with the distance;
 * - over the sampled word boundaries that sit in or beside a measurable pause,
 *   the median and ninetieth-percentile distance from the region's start to the
 *   voice's onset, split into early and late.
 *
 * The second number is the one a person cannot read off a table: a boundary
 * that sits in a pause is a boundary the audio can adjudicate, and the distance
 * is what the wash would show. Boundaries inside continuous speech are not
 * counted, because no energy envelope can say where one word ends in legato.
 *
 * It needs ffmpeg and `content/work/audio-packs`, so like `audio edges` it runs
 * on the owner's machine. It is a measurement, never a release gate: `checkdb`
 * holds the line that ships.
 */
class AudioWords(private val root: File) {

    private val database = File(root, "content/quran.db")
    private val packs = File(root, "content/work/audio-packs")
    private val work = File(root, "content/work/audio-words")

    fun run(): Int {
        if (!database.exists()) {
            println("words: content/quran.db is missing; run build first")
            return 1
        }
        if (!packs.isDirectory) {
            println("words: ${packs.relativeTo(root)} is missing; run tools audio packs first")
            return 1
        }
        val rows = readRows()
        if (rows.isEmpty()) {
            println("words: the database names no recitation rows")
            return 1
        }
        val wanted = sampleAyahs()
        var failures = 0
        for (reciter in rows.keys.sorted()) {
            val here = rows.getValue(reciter).filterKeys { it in wanted }
            if (here.isEmpty()) {
                println("words: $reciter has no sampled ayah in the database")
                failures++
                continue
            }
            val measured = measureReciter(reciter, here)
            if (measured.isEmpty()) {
                println("words: $reciter has no readable audio in ${packs.relativeTo(root)}")
                failures++
                continue
            }
            report(reciter, measured)
        }
        return failures
    }

    /** One sampled ayah's regions, and what the audio said around them. */
    private data class Sample(val key: String, val headGap: Long, val boundary: Long?)

    /** One recitation row the sample read: the file's name and the ayah's regions. */
    private data class Row(val name: String, val key: String, val regions: List<RecitationSegments.Region>)

    private fun measureReciter(reciter: String, ayahs: Map<Int, Row>): List<Sample> {
        val folder = File(packs, reciter)
        val zips = folder.listFiles { file -> file.isFile && file.name.endsWith(".zip") }
            ?.sortedBy { it.name }.orEmpty()
        if (zips.isEmpty()) return emptyList()
        val extracted = File(work, reciter)
        val found = HashMap<Int, Pair<Row, File>>()
        for (zip in zips) {
            val names = runCatching {
                java.util.zip.ZipFile(zip).use { archive ->
                    archive.entries().asSequence()
                        .filter { !it.isDirectory && it.name.endsWith(".mp3") }
                        .map { it.name }
                        .toList()
                }
            }.getOrElse { emptyList() }
            val pending = ayahs.filter { (ayah, row) -> row.name in names && ayah !in found }
            if (pending.isEmpty()) continue
            val out = extract(zip, File(extracted, zip.nameWithoutExtension))
            for ((ayah, row) in pending) {
                val file = File(out, row.name)
                if (file.isFile) found[ayah] = row to file
            }
        }
        if (found.isEmpty()) return emptyList()

        val results = java.util.Collections.synchronizedList(mutableListOf<Sample>())
        val pool = Executors.newFixedThreadPool(WORKERS)
        val done = AtomicInteger()
        try {
            for ((_, pair) in found) {
                pool.submit {
                    val (row, file) = pair
                    val sample = measure(file, row)
                    if (sample != null) results += sample
                    val count = done.incrementAndGet()
                    if (count % 100 == 0) println("words: $reciter $count/${found.size}")
                }
            }
        } finally {
            pool.shutdown()
            pool.awaitTermination(4, TimeUnit.HOURS)
        }
        return results.toList()
    }

    private fun measure(file: File, row: Row): Sample? {
        val frames = AudioProbe.probe(file).frames
        if (frames.isEmpty()) return null
        val line = AudioProbe.threshold(frames)
        val voice = AudioProbe.firstSpeech(line, frames)
        if (voice < 0) return null
        val runs = AudioProbe.speechRuns(frames, line)
        val onsets = runs.filter { it.voiced }.map { it.startMs }
        val headGap = row.regions.first().startMs - voice

        // A boundary is adjudicable when it sits in a quiet run that a voice
        // follows within two seconds. Anything else is legato, and no energy
        // envelope can say where one word ends in continuous speech.
        var boundary: Long? = null
        for (region in row.regions.drop(1)) {
            val start = region.startMs
            val quiet = runs.firstOrNull { !it.voiced && start >= it.startMs && start < it.endMs }
            if (quiet == null) continue
            val onset = onsets.filter { abs(it - start) <= 2_000 }.minByOrNull { abs(it - start) }
            if (onset != null) {
                boundary = start - onset
                break
            }
        }
        return Sample(row.key, headGap, boundary)
    }

    private fun report(reciter: String, measured: List<Sample>) {
        println()
        println("words: $reciter, ${measured.size} sampled ayah(s)")
        val trailing = measured.filter { it.headGap > HEAD_TRAIL_MS }.sortedByDescending { it.headGap }
        if (trailing.isEmpty()) {
            println("  heads: every sampled first region stands within $HEAD_TRAIL_MS ms of its voice")
        } else {
            println("  heads: ${trailing.size} sampled ayah(s) begin after their voice")
            for (sample in trailing.take(10)) {
                println("    ${sample.key} begins ${sample.headGap} ms after its first voice")
            }
        }
        val signed = measured.mapNotNull { it.boundary }
        val absolute = signed.map { abs(it) }.sorted()
        if (absolute.isEmpty()) {
            println("  boundaries: no sampled pause could adjudicate a boundary")
            return
        }
        val median = absolute[(absolute.size - 1) * 50 / 100]
        val p90 = absolute[(absolute.size - 1) * 90 / 100]
        val early = signed.count { it < 0 }
        val late = signed.count { it > 0 }
        println("  boundaries: ${absolute.size} adjudicable, |error| median $median ms, p90 $p90 ms")
        println("  boundaries: $early jump before their word, $late after it")
    }

    /** Every recitation row once, with the ayah's own key and its regions. */
    private fun readRows(): Map<String, Map<Int, Row>> {
        val keys = HashMap<Int, String>()
        val rows = HashMap<String, HashMap<Int, Row>>()
        openSqlite(database).use { connection ->
            connection.each("SELECT number, surah, ayah FROM ayah") { rs ->
                keys[rs.getInt(1)] = "${rs.getInt(2)}:${rs.getInt(3)}"
            }
            connection.each(
                "SELECT ra.recitation, ra.audio_path, ra.ayah_number, ra.segments FROM recitation_ayah ra",
            ) { rs ->
                val reciter = rs.getString(1) ?: return@each
                val name = rs.getString(2)?.substringAfterLast('/') ?: return@each
                val ayah = rs.getInt(3)
                val key = keys[ayah] ?: return@each
                val regions = RecitationSegments.parse(rs.getString(4) ?: "") ?: return@each
                if (name.isBlank() || regions.isEmpty()) return@each
                rows.getOrPut(reciter) { HashMap() }[ayah] = Row(name, key, regions)
            }
        }
        return rows
    }

    /**
     * The sampled ayahs: the first ayah of every surah, every twentieth ayah of
     * the Book, and every ayah the pipeline pins, so a change to a pinned row is
     * measured by the step that would notice it.
     */
    private fun sampleAyahs(): Set<Int> {
        val sample = HashSet<Int>()
        openSqlite(database).use { connection ->
            connection.each("SELECT number, ayah FROM ayah ORDER BY number") { rs ->
                val number = rs.getInt(1)
                if (rs.getInt(2) == 1 || number % 20 == 0) sample += number
            }
            connection.each("SELECT number, surah, ayah FROM ayah") { rs ->
                val key = "${rs.getInt(2)}:${rs.getInt(3)}"
                if (key in PINNED) sample += rs.getInt(1)
            }
        }
        return sample
    }

    private companion object {
        const val WORKERS = 6

        /** A region this far past its voice is a head the table cannot explain. */
        const val HEAD_TRAIL_MS = 400L

        /** Every ayah either recitation's timing file leaves a word resting in. */
        val PINNED: Set<String> by lazy {
            (QuranAlignSegments.UNCOVERED.values.flatMap { it.keys } +
                QuranAlignSegments.MUQATA_GAPS.keys.map { (_, key) -> key }).toSet()
        }
    }
}
