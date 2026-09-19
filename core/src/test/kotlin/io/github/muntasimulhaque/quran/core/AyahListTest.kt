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
}
