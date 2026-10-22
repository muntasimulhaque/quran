package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecitationSegmentsTest {

    @Test
    fun `a leading fragment shifts the whole ayah back one`() {
        val json = "[[0,1,0,70],[1,2,40,3240],[2,3,3920,4520]]"
        val correction = correct(json, words = 2)
        assertTrue(correction.shifted)
        assertEquals(listOf(1, 1, 2), wordPositions(correction.json))
    }

    @Test
    fun `a 90 ms fragment is the marker and a 91 ms one is a word`() {
        val marker = correct("[[0,1,0,90],[1,2,100,3000],[2,3,3100,4000]]", words = 2)
        assertTrue(marker.shifted)
        assertEquals(listOf(1, 1, 2), wordPositions(marker.json))

        val word = correct("[[0,1,0,91],[1,2,100,3000],[2,3,3100,4000]]", words = 2)
        assertFalse(word.shifted)
        assertEquals(listOf(1, 2, 2), wordPositions(word.json))
    }

    @Test
    fun `an ayah the export wrote correctly keeps its own bytes`() {
        val json = "[[0,1,0,1000],[1,2,1000,2000]]"
        val correction = correct(json, words = 2)
        assertFalse(correction.shifted)
        assertEquals(json, correction.json)
    }

    @Test
    fun `the ayah number is folded onto the last drawn word`() {
        val correction = correct("[[0,1,0,1000],[1,2,1000,2000],[2,3,2000,3000]]", words = 2)
        assertFalse(correction.shifted)
        assertEquals(listOf(1, 2, 2), wordPositions(correction.json))
        assertEquals(1, correction.clamped)
    }

    @Test
    fun `a QPC merge folds the second half and shifts the rest`() {
        val json = "[[0,1,0,1000],[1,2,1000,2000],[2,3,2000,3000],[3,4,3000,4000],[4,5,4000,5000]]"
        val regions = requireNotNull(RecitationSegments.parse(json))
        val correction = RecitationSegments.correct(json, regions, wordCount = 4) {
            when {
                it == 4 -> 3
                it > 4 -> it - 1
                else -> it
            }
        }
        assertEquals(listOf(1, 2, 3, 3, 4), wordPositions(correction.json))
        assertEquals(2, correction.mapped)
        assertEquals(0, correction.clamped)
    }

    @Test
    fun `a three number region is read as the older QUL shape`() {
        val correction = correct("[[1,0,1000],[2,1000,2000]]", words = 2)
        assertEquals(listOf(1, 2), wordPositions(correction.json))
        assertEquals(listOf(0, 1), wordFroms(correction.json))
    }

    private fun correct(json: String, words: Int): RecitationSegments.Correction {
        val regions = requireNotNull(RecitationSegments.parse(json))
        return RecitationSegments.correct(json, regions, words)
    }

    private fun wordPositions(json: String): List<Int> =
        requireNotNull(RecitationSegments.parse(json)).map { it.wordTo }

    private fun wordFroms(json: String): List<Int> =
        requireNotNull(RecitationSegments.parse(json)).map { it.wordFrom }
}
