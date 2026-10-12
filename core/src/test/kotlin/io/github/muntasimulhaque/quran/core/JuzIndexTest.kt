package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The juz label on the reading's top bar follows the reader's place, and the
 * place moves on every scroll settle. The lookup that turns an ayah into its
 * juz is pinned here by the Book's own boundaries, because an off-by-one at
 * a boundary shows the reader the wrong juz at the moment they cross it.
 */
class JuzIndexTest {

    @Test
    fun everyAyahTakesTheJuzThatStartsAtOrBeforeIt() {
        val at = JuzIndex.of(listOf(1, 4, 7), 9)
        assertEquals(
            "an ayah on a boundary belongs to the juz that begins there",
            listOf(1, 1, 1, 2, 2, 2, 3, 3, 3),
            (1..9).map { at[it] },
        )
    }

    @Test
    fun theBoundaryTurnsWhereTheBookTurns() {
        // The shipped content's own boundaries at the start of the Book:
        // juz 1 ends at 2:141, the Book's ayah number 148, and juz 2 begins
        // at 2:142, number 149 (owner report).
        val at = JuzIndex.of(listOf(1, 149, 260), 6236)
        assertEquals(1, at[148])
        assertEquals(2, at[149])
    }

    @Test
    fun theLastBoundaryCarriesToTheEndOfTheBook() {
        val at = JuzIndex.of(listOf(1, 149, 260), 6236)
        assertEquals(3, at[260])
        assertEquals(3, at[6236])
    }

    @Test
    fun aBoundaryListThatNeverArrivesLeavesEveryAyahInJuzOne() {
        assertTrue(JuzIndex.of(emptyList(), 10).all { it == 1 })
    }
}
