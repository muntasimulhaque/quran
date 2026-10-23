package io.github.muntasimulhaque.quran.data

/**
 * The two short regions QUL sometimes writes at the edges of an ayah.
 *
 * The export uses a region of 90 ms or less as a marker: the first one
 * opens the ayah, and the last one closes it. The build already folds the
 * leading marker when the source word numbers run one past the drawn words,
 * and it clamps the ayah number onto the last drawn word. This is the other
 * half, read on the device so an already installed pack is corrected too:
 *
 * - a short first region ends where the second word begins, so the first
 *   word is washed for its own speech rather than for 70 ms of silence;
 * - a short last region begins where the previous word ends and ends where
 *   the marker begins, so the last word is washed for its speech rather than
 *   for the closing silence.
 *
 * A real word is never that short in the export. The correction leaves every
 * region it cannot explain exactly as it was.
 */
object RecitationTiming {

    const val MARKER_MS = 90L

    fun normalize(regions: List<WordSegment>): List<WordSegment> {
        if (regions.size < 2) return regions
        val out = regions.toMutableList()

        val first = out.first()
        val next = out[1]
        if (first.endMs >= first.startMs &&
            first.endMs - first.startMs <= MARKER_MS &&
            next.startMs > first.endMs
        ) {
            out[0] = first.copy(endMs = next.startMs)
        }

        val last = out.last()
        val previous = out[out.size - 2]
        if (last.endMs >= last.startMs &&
            last.endMs - last.startMs <= MARKER_MS &&
            last.startMs > previous.endMs
        ) {
            out[out.size - 1] = last.copy(startMs = previous.endMs, endMs = last.startMs)
        }
        return out
    }
}
