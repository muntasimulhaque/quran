package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AyahListTest {

    private val surah = (8 until 8 + 286).toList()

    @Test
    fun theOpeningItemComesBeforeTheFirstAyah() {
        assertEquals(0, AyahList.indexOf(surah, 7))
        assertEquals(1, AyahList.indexOf(surah, 8))
        assertEquals(2, AyahList.indexOf(surah, 9))
        assertEquals(286, AyahList.indexOf(surah, 293))
    }

    @Test
    fun theOpeningItemShowsTheFirstAyah() {
        assertEquals(8, AyahList.ayahAt(surah, 0))
        assertEquals(8, AyahList.ayahAt(surah, 1))
        assertEquals(9, AyahList.ayahAt(surah, 2))
    }

    @Test
    fun theClosingLineKeepsThePlace() {
        assertNull(AyahList.ayahAt(surah, 287))
        assertNull(AyahList.ayahAt(surah, 288))
    }

    @Test
    fun everyAyahRoundTripsThroughTheList() {
        for (ayah in surah) {
            assertEquals(ayah, AyahList.ayahAt(surah, AyahList.indexOf(surah, ayah)))
        }
    }

    @Test
    fun anAyahFromAnotherSurahOpensAtTheTop() {
        assertEquals(0, AyahList.indexOf(surah, 3000))
    }

    /**
     * The reading writes the ayah it settled on, so the place the list is
     * handed back is the ayah it is already showing. This is the rule that
     * keeps the page still: whatever the width, the text size, or the
     * translation, a settle asks for no move at all.
     */
    @Test
    fun aPlaceTheListAlreadyShowsMovesNothing() {
        for (index in 1..surah.size) {
            assertNull("index $index", AyahList.moveTarget(surah, surah[index - 1], index))
        }
    }

    /**
     * The opening item sits one slot above the surah's first ayah, so a settle
     * at the top of a surah writes that ayah while the list is on the opening.
     * The opening is the truthful place for it and must not be scrolled away
     * by the reader's own gesture.
     */
    @Test
    fun theSurahsOpeningKeepsItsFirstAyah() {
        assertNull(AyahList.moveTarget(surah, place = 8, firstVisible = 0))
    }

    @Test
    fun aPlaceFromElsewhereMovesThePage() {
        assertEquals(3, AyahList.moveTarget(surah, place = 10, firstVisible = 1))
        assertEquals(1, AyahList.moveTarget(surah, place = 8, firstVisible = 5))
        assertEquals(0, AyahList.moveTarget(surah, place = 3000, firstVisible = 5))
    }
}
