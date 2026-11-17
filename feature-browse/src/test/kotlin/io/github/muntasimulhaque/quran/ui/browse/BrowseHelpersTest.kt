package io.github.muntasimulhaque.quran.ui.browse

import io.github.muntasimulhaque.quran.data.Surah
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The turning of a running ayah number into the surah that holds it, which is
 * what Browse asks when the reader arrives from a saved ayah or the reading's
 * own place: the list opens on the surah the reader was in, never on the one
 * before it.
 *
 * The numbers are the Book's own, so the test uses the Book's own boundaries
 * rather than invented ones: 2:255 is the 262nd ayah and falls in Al-Baqarah,
 * and the first ayah of a surah belongs to that surah.
 */
class BrowseHelpersTest {

    @Test
    fun anAyahOnASurahStartBelongsToTheSurahThatBeginsThere() {
        assertEquals(
            "the ayah a surah begins at is that surah's, never the one before it",
            2,
            surahOfAyah(surahs(), starts(), 262),
        )
        assertEquals("Al-Baqarah begins at 8 in this list", 2, surahOfAyah(surahs(), starts(), 8))
    }

    @Test
    fun theLastAyahOfASurahBelongsToIt() {
        assertEquals(
            "the last ayah before the next surah's start is still this surah's",
            1,
            surahOfAyah(surahs(), starts(), 7),
        )
    }

    @Test
    fun anAyahBeforeTheFirstStartHasNoSurah() {
        assertEquals(
            "nothing holds an ayah that stands before the Book's first start",
            null,
            surahOfAyah(surahs(), starts(), 0),
        )
    }

    @Test
    fun aSurahWhoseStartIsUnknownIsPassedOver() {
        val unknown = starts() - 2
        assertEquals(
            "a surah with no start cannot be the answer, so the last known start answers",
            1,
            surahOfAyah(surahs(), unknown, 262),
        )
    }

    /** Al-Fatihah, Al-Baqarah, and Aal-Imran, the three the boundaries name. */
    private fun surahs() = listOf(1, 2, 3).map { number ->
        Surah(
            number = number,
            nameArabic = "",
            nameSimple = "Surah $number",
            nameLatin = "Surah $number",
            revelationPlace = "makkah",
            versesCount = 7,
        )
    }

    /** Where each of the three begins in the Book's running count. */
    private fun starts() = mapOf(1 to 1, 2 to 8, 3 to 294)
}
