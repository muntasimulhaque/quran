package io.github.muntasimulhaque.quran.playback

import io.github.muntasimulhaque.quran.data.WordSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecitedWordTest {

    @Test
    fun `nothing is marked before the recitation starts`() {
        val segments = listOf(segment(1, 720, 1200), segment(2, 1280, 1920))
        assertNull(recitedWordAt(segments, 0))
        assertNull(recitedWordAt(segments, 719))
    }

    @Test
    fun `the word stays marked through the gap after its region`() {
        val segments = listOf(segment(1, 0, 840), segment(2, 1040, 1440))
        assertEquals(1, recitedWordAt(segments, 500))
        assertEquals(1, recitedWordAt(segments, 900))
        assertEquals(2, recitedWordAt(segments, 1040))
    }

    @Test
    fun `the last word stays marked to the end of the ayah without an edge`() {
        val segments = listOf(segment(1, 0, 1000), segment(2, 1000, 2000))
        assertEquals(2, recitedWordAt(segments, 1500))
        assertEquals(2, recitedWordAt(segments, 5000))
    }

    @Test
    fun `a repeated phrase walks back with the reciter`() {
        val segments = listOf(
            segment(1, 0, 1000),
            segment(2, 1000, 2000),
            segment(1, 2000, 3000),
            segment(2, 3000, 4000),
        )
        assertEquals(1, recitedWordAt(segments, 2500))
        assertEquals(2, recitedWordAt(segments, 3500))
    }

    @Test
    fun `no word is marked before the first sound, whatever the export says`() {
        val segments = listOf(segment(1, 0, 840), segment(2, 1040, 1440))
        assertNull(recitedWordAt(segments, 0, firstSpeechMs = 540))
        assertNull(recitedWordAt(segments, 539, firstSpeechMs = 540))
        assertEquals(1, recitedWordAt(segments, 540, firstSpeechMs = 540))
    }

    @Test
    fun `no word is marked after the last sound`() {
        val segments = listOf(segment(1, 0, 840), segment(2, 2880, 4920))
        assertEquals(2, recitedWordAt(segments, 4200, lastSpeechMs = 4200))
        assertNull(recitedWordAt(segments, 4201, lastSpeechMs = 4200))
        assertNull(recitedWordAt(segments, 4900, lastSpeechMs = 4200))
    }

    @Test
    fun `a gap the export cannot explain rests instead of lying`() {
        val segments = listOf(segment(1, 0, 1000), segment(2, 20_000, 21_000))
        assertEquals(1, recitedWordAt(segments, 5_900))
        assertNull(recitedWordAt(segments, 6_100))
        assertNull(recitedWordAt(segments, 15_000))
        assertEquals(2, recitedWordAt(segments, 20_000))
    }

    @Test
    fun `an edge pair that contradicts itself is no edge`() {
        val segments = listOf(segment(1, 0, 840), segment(2, 1040, 1440))
        assertEquals(1, recitedWordAt(segments, 500, firstSpeechMs = 900, lastSpeechMs = 300))
    }

    @Test
    fun `the first sound at zero is still an edge`() {
        val segments = listOf(segment(1, 0, 840), segment(2, 1040, 1440))
        assertEquals(1, recitedWordAt(segments, 0, firstSpeechMs = 0, lastSpeechMs = 2000))
    }

    private fun segment(word: Int, startMs: Long, endMs: Long) =
        WordSegment(wordFrom = word - 1, wordTo = word, startMs = startMs, endMs = endMs)
}
