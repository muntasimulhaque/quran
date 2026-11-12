package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The span one word's own voice is played over, pinned here because it is the
 * arithmetic a reader's tap lands on and an instrumented run can only hear the
 * result of it.
 *
 * The regions come from the packs and are not this module's to correct, so the
 * test pins only the floor: a real word passes through untouched, and the
 * three shapes a broken region can take (before the file, empty, wider than a
 * verse) are read as a playable word instead of as silence or as a verse.
 */
class WordClipTest {

    @Test
    fun aDrawnWordIsPlayedAsItWasDrawn() {
        val clip = WordClip.of(600L, 1_000L)
        assertEquals(600L, clip.startMs)
        assertEquals(1_000L, clip.endMs)
        assertEquals(400L, clip.durationMs)
    }

    @Test
    fun aRegionThatStartsBeforeTheFileStartsAtItsFirstMillisecond() {
        val clip = WordClip.of(-400L, 500L)
        assertEquals(0L, clip.startMs)
        assertEquals(500L, clip.endMs)
    }

    @Test
    fun anEmptyRegionIsTheShortestWordRatherThanSilence() {
        assertEquals(WordClip.SHORTEST_MS, WordClip.of(1_000L, 1_000L).durationMs)
        assertEquals(WordClip.SHORTEST_MS, WordClip.of(1_000L, 980L).durationMs)
        assertEquals(WordClip.SHORTEST_MS, WordClip.of(1_000L, 400L).durationMs)
        assertEquals(1_000L, WordClip.of(1_000L, 400L).startMs)
    }

    @Test
    fun aRegionThatLostItsEndIsRefusedAtAVersesWorth() {
        assertEquals(WordClip.LONGEST_MS, WordClip.of(2_000L, 90_000L).durationMs)
        assertEquals(2_000L, WordClip.of(2_000L, 90_000L).startMs)
    }

    @Test
    fun anEmptyRegionBeforeTheFileStillHasALength() {
        val clip = WordClip.of(-50L, -50L)
        assertEquals(0L, clip.startMs)
        assertEquals(WordClip.SHORTEST_MS, clip.durationMs)
    }
}
