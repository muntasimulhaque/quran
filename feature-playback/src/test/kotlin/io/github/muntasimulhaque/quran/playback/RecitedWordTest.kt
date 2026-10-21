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
    fun `the last word stays marked to the end of the ayah`() {
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

    private fun segment(word: Int, startMs: Long, endMs: Long) =
        WordSegment(wordFrom = word - 1, wordTo = word, startMs = startMs, endMs = endMs)
}
