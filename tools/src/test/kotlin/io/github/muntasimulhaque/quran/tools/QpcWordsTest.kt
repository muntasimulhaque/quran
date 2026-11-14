package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class QpcWordsTest {

    @Test
    fun theFourQpcMergesAreTheOnesTheBuildUses() {
        assertEquals(4, QpcWords.merges.size)
        assertEquals(
            listOf(
                QpcWords.Merge(2, 181, 3),
                QpcWords.Merge(8, 6, 4),
                QpcWords.Merge(13, 37, 8),
                QpcWords.Merge(37, 130, 3),
            ),
            QpcWords.merges,
        )
    }

    @Test
    fun aJoinedPairNamesTheJoinedWordAndTheRestMoveDown() {
        // 37:130 keeps "إِلۡ يَاسِين" as one drawn word.
        assertEquals(1, QpcWords.position(37, 130, 1))
        assertEquals(2, QpcWords.position(37, 130, 2))
        assertEquals(3, QpcWords.position(37, 130, 3))
        assertEquals(3, QpcWords.position(37, 130, 4))
        assertEquals(4, QpcWords.position(37, 130, 5))
        assertEquals(5, QpcWords.position(37, 130, 6))
    }

    @Test
    fun aWordOutsideTheMergedAyahIsUntouched() {
        assertEquals(1, QpcWords.position(1, 1, 1))
        assertEquals(4, QpcWords.position(1, 7, 4))
        assertEquals(17, QpcWords.position(2, 181, 18))
    }
}
