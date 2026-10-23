package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecitationTimingTest {

    @Test
    fun `a short first region ends where the second word begins`() {
        val regions = listOf(
            WordSegment(0, 1, 0, 70),
            WordSegment(1, 2, 3_010, 3_110),
        )
        val normalized = RecitationTiming.normalize(regions)
        assertEquals(3_010, normalized[0].endMs)
        assertEquals(0, normalized[0].startMs)
    }

    @Test
    fun `a short last region covers the last word, not the closing silence`() {
        val regions = listOf(
            WordSegment(27, 28, 56_320, 57_920),
            WordSegment(28, 29, 63_330, 63_400),
        )
        val normalized = RecitationTiming.normalize(regions)
        assertEquals(57_920, normalized[1].startMs)
        assertEquals(63_330, normalized[1].endMs)
    }

    @Test
    fun `a real short word is left alone`() {
        val regions = listOf(
            WordSegment(0, 1, 1_000, 1_200),
            WordSegment(1, 2, 1_300, 2_000),
        )
        assertEquals(regions, RecitationTiming.normalize(regions))
    }

    @Test
    fun `one region is left alone`() {
        val regions = listOf(WordSegment(0, 1, 0, 5_000))
        assertEquals(regions, RecitationTiming.normalize(regions))
    }
}
