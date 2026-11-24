package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecitationSegmentsTest {

    @Test
    fun `a four number region is read as the word it names`() {
        val regions = requireNotNull(RecitationSegments.parse("[[0,1,1200,2040],[1,2,2050,3930]]"))
        assertEquals(listOf(0, 1), regions.map { it.wordFrom })
        assertEquals(listOf(1, 2), regions.map { it.wordTo })
        assertEquals(listOf(1200L, 2050L), regions.map { it.startMs })
        assertEquals(listOf(2040L, 3930L), regions.map { it.endMs })
    }

    @Test
    fun `a three number region is read as the older shape`() {
        val regions = requireNotNull(RecitationSegments.parse("[[1,0,1000],[2,1000,2000]]"))
        assertEquals(listOf(0, 1), regions.map { it.wordFrom })
        assertEquals(listOf(1, 2), regions.map { it.wordTo })
    }

    @Test
    fun `an empty list is a readable ayah with no regions`() {
        assertEquals(emptyList<RecitationSegments.Region>(), RecitationSegments.parse("[]"))
    }

    @Test
    fun `a region with no drawn word is not a readable list`() {
        assertNull(RecitationSegments.parse("[[0,0,0,1000]]"))
    }

    @Test
    fun `a row that is not a list of regions is refused`() {
        assertNull(RecitationSegments.parse("not json"))
        assertNull(RecitationSegments.parse("[[0,1,1000]]"))
        assertNull(RecitationSegments.parse("[[0,1,\"soon\",1000]]"))
    }
}
