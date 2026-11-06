package io.github.muntasimulhaque.quran.playback

import io.github.muntasimulhaque.quran.data.WordSegment

/**
 * The word under the reciter at [positionMs].
 *
 * The content build has already folded QUL's leading fragment and its ayah
 * number into the drawn words, so this is only the reading: the last region
 * that has started is the word being recited, and it stays marked through a
 * breath or a madd until the next region starts. [segments] is in the
 * export's own order, which is the ayah's order.
 *
 * [firstSpeechMs] and [lastSpeechMs] are the file's own voice edges, read from
 * the audio and shipped beside the timings: the export starts almost every
 * ayah's first word at 0 and lets the last one run to the end of the file, so
 * the wash used to sit on a word through the lead-in and the closing pause.
 * Before the first sound and after the last one there is no word. [NO_EDGE_MS]
 * means the file has no measured edge, and the old reading stands.
 *
 * [holdMaxMs] is the longest a word may stay marked without the next one
 * starting. The export sometimes leaves a gap of twenty seconds where the
 * reciter repeats a phrase; holding the word that was last named is a lie the
 * reader can hear, so past this many milliseconds the wash rests instead. A
 * real word is never this long, including its madd. The last word of an ayah
 * has no next region and is ended by [lastSpeechMs] instead (owner decision,
 * this session).
 */
internal fun recitedWordAt(
    segments: List<WordSegment>,
    positionMs: Long,
    firstSpeechMs: Long = NO_EDGE_MS,
    lastSpeechMs: Long = NO_EDGE_MS,
    holdMaxMs: Long = HOLD_MAX_MS,
): Int? {
    if (firstSpeechMs != NO_EDGE_MS && lastSpeechMs != NO_EDGE_MS && lastSpeechMs < firstSpeechMs) {
        // An edge pair the audio cannot explain is no edge at all.
        return recitedWordAt(segments, positionMs, NO_EDGE_MS, NO_EDGE_MS, holdMaxMs)
    }
    if (firstSpeechMs != NO_EDGE_MS && positionMs < firstSpeechMs) return null
    if (lastSpeechMs != NO_EDGE_MS && positionMs > lastSpeechMs) return null
    var word: Int? = null
    var wordStart = 0L
    var nextStart: Long? = null
    for (segment in segments) {
        if (segment.startMs > positionMs) {
            nextStart = segment.startMs
            break
        }
        word = segment.wordTo
        wordStart = segment.startMs
    }
    if (word != null && nextStart != null &&
        nextStart - wordStart > holdMaxMs &&
        positionMs - wordStart > holdMaxMs
    ) {
        return null
    }
    return word
}

/**
 * How long the wash may sleep before its next change, in media milliseconds.
 *
 * The wash changes only at a word boundary, at the first voice, at the last,
 * and at the resting hold, so asking the player every frame is work for a
 * change that comes every half second. This is the wait to the next of those
 * moments; null means nothing can change until the player moves to another
 * item. The reading scales it by the reader's pace and sleeps exactly that
 * long, which keeps a 1.5x recitation on time without a poll (owner decision,
 * this session).
 */
internal fun nextWordChangeMs(
    segments: List<WordSegment>,
    positionMs: Long,
    firstSpeechMs: Long = NO_EDGE_MS,
    lastSpeechMs: Long = NO_EDGE_MS,
    holdMaxMs: Long = HOLD_MAX_MS,
): Long? {
    if (segments.isEmpty()) return null
    var current: WordSegment? = null
    var nextStart: Long? = null
    for (segment in segments) {
        if (segment.startMs > positionMs) {
            nextStart = segment.startMs
            break
        }
        current = segment
    }
    val candidates = ArrayList<Long>(4)
    if (firstSpeechMs != NO_EDGE_MS && firstSpeechMs > positionMs) candidates += firstSpeechMs
    if (lastSpeechMs != NO_EDGE_MS && lastSpeechMs > positionMs) candidates += lastSpeechMs
    if (nextStart != null) candidates += nextStart
    val now = current
    if (now != null && nextStart != null && nextStart - now.startMs > holdMaxMs) {
        val cap = now.startMs + holdMaxMs
        if (cap > positionMs) candidates += cap
    }
    return candidates.filter { it > positionMs }.minOrNull()?.let { it - positionMs }
}

/** No measured voice edge for this file. */
internal const val NO_EDGE_MS = -1L

/**
 * The longest a word may stay marked while the reciter has moved on in the
 * audio but the export has not named the next word. Six seconds is longer
 * than the longest madd in either recitation and shorter than the export's
 * repeated-phrase holes, the largest of which is over twenty seconds.
 */
internal const val HOLD_MAX_MS = 6_000L
