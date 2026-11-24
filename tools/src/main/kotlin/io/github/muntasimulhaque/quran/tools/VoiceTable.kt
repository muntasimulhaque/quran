package io.github.muntasimulhaque.quran.tools

import java.io.File

/**
 * `content/audio-edges.txt`, as the build and the gates read it.
 *
 * One line per recitation and ayah: `reciter ayah firstMs lastMs durationMs`,
 * measured from the shipped audio by `audio edges`. The file is the build's one
 * source for where a file's voice actually starts and stops, and two steps read
 * it: the content build, which needs the true span of a one-word ayah, and
 * `checkdb`, which compares every first region against the voice.
 *
 * A missing or unreadable file is an empty table rather than a crash: the
 * pipeline reports the absence where it matters (the head gate says so) instead
 * of failing on a line it does not understand.
 */
object VoiceTable {

    /** One ayah file's measured voice. */
    data class Voice(val firstMs: Long, val lastMs: Long, val durationMs: Long)

    /** Every measured voice, as reciter to ayah to its edge. */
    fun read(root: File): Map<String, Map<Int, Voice>> {
        val file = File(root, "content/audio-edges.txt")
        if (!file.isFile) return emptyMap()
        val table = HashMap<String, MutableMap<Int, Voice>>()
        file.forEachLine { line ->
            if (line.isBlank() || line.startsWith("#")) return@forEachLine
            val parts = line.trim().split(' ', '\t').filter { it.isNotEmpty() }
            if (parts.size < 5) return@forEachLine
            val ayah = parts[1].toIntOrNull() ?: return@forEachLine
            val first = parts[2].toLongOrNull() ?: return@forEachLine
            val last = parts[3].toLongOrNull() ?: return@forEachLine
            val duration = parts[4].toLongOrNull() ?: return@forEachLine
            if (last <= first) return@forEachLine
            table.getOrPut(parts[0]) { HashMap() }[ayah] = Voice(first, last, duration)
        }
        return table
    }
}
