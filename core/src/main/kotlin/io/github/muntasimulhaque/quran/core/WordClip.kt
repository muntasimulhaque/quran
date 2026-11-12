package io.github.muntasimulhaque.quran.core

/**
 * The millisecond span one word's own voice is played over.
 *
 * The regions the timing exports carry were drawn for highlighting, where a
 * syllable of error cannot be seen, and they are read back for sound, where it
 * can: a region often opens inside the word before it and closes on the moment
 * the next one begins. The span is therefore clamped to what is playable
 * rather than trusted, so a region that is early, empty, or far too wide
 * cannot turn a tap on one word into a verse, or into a click. Where the
 * boundaries themselves should be is the timings' and the pack's to answer;
 * this is the floor under them (owner decision, this session).
 */
data class WordClip(val startMs: Long, val endMs: Long) {

    val durationMs: Long get() = endMs - startMs

    companion object {

        /**
         * A word shorter than this is heard as a click rather than as a word.
         * The shortest drawn word on the page is about four times it.
         */
        const val SHORTEST_MS = 80L

        /**
         * A span this wide is not one word: it is a region that lost its
         * boundary, or a joined pair read as a single word. The longest drawn
         * word with its madds is under four seconds, so nothing true is
         * refused and nothing runaway is played (owner decision, this
         * session).
         */
        const val LONGEST_MS = 8_000L

        /**
         * The span, from the file's own first millisecond, no shorter than a
         * word and no longer than a verse's worth of one.
         */
        fun of(startMs: Long, endMs: Long): WordClip {
            val start = startMs.coerceAtLeast(0L)
            val end = endMs.coerceAtLeast(start + SHORTEST_MS)
            return WordClip(start, end.coerceAtMost(start + LONGEST_MS))
        }
    }
}
