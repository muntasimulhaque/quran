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
 */
internal fun recitedWordAt(segments: List<WordSegment>, positionMs: Long): Int? {
    var word: Int? = null
    for (segment in segments) {
        if (segment.startMs > positionMs) break
        word = segment.wordTo
    }
    return word
}
