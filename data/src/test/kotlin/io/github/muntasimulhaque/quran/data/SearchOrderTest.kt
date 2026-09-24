package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The order a reader reads search results in, pinned without a device.
 *
 * The first cut of this contract was written as an instrumented assertion on
 * one query, and the query returned so many translation matches that the cap
 * hid the other kinds, so the assertion failed on the test's own assumption
 * rather than on the order. The order is a pure function now; this is its
 * test (D-090).
 */
class SearchOrderTest {

    private fun ayah(number: Int, arabic: Int = 0, translation: Boolean = false, meaning: String? = null) =
        SearchHit.AyahHit(
            ayah = Ayah(number, 1, number, "1:$number", "text $number"),
            page = 1,
            arabicMatchedWords = (1..arabic).toSet(),
            translation = if (translation) {
                TranslationHit("p", "Pack", "text", emptyList(), emptyList())
            } else {
                null
            },
            wordMeaning = meaning,
        )

    private fun tafsir(from: Int, pack: String = "p") = SearchHit.TafsirHitResult(
        pack = pack,
        packName = pack,
        surah = 1,
        surahName = "Al-Fatihah",
        fromAyah = from,
        toAyah = from,
        ayah = Ayah(from, 1, from, "1:$from", "text"),
        page = 1,
        text = "excerpt",
        ranges = emptyList(),
    )

    @Test
    fun theKindsRunFromTheVerseOutward() {
        val (hits, capped) = orderSearchHits(
            leading = emptyList(),
            ayahHits = listOf(
                ayah(5, translation = true),
                ayah(3, arabic = 1),
                ayah(9, meaning = "mercy"),
                ayah(1, arabic = 1),
            ),
            tafsirHits = listOf(tafsir(7), tafsir(2)),
            limit = 50,
        )
        assertFalse(capped)
        val numbers = hits.filterIsInstance<SearchHit.AyahHit>().map { it.ayah.number }
        // Arabic 1 and 3 first, then the translation 5, then the meaning 9.
        assertEquals(listOf(1, 3, 5, 9), numbers)
        val tafsirNumbers = hits.filterIsInstance<SearchHit.TafsirHitResult>().map { it.ayah.number }
        assertEquals(listOf(2, 7), tafsirNumbers)
        assertEquals("the tafsir is the last group", 6, hits.size)
    }

    @Test
    fun aReferenceAndASurahComeBeforeEveryAyah() {
        val reference = SearchHit.ReferenceHit(Ayah(255, 2, 255, "2:255", "text"), 42)
        val surah = SearchHit.SurahHit(Surah(2, "البقرة", "Al-Baqarah", "Al-Baqarah", "madinah", 286))
        val (hits, _) = orderSearchHits(
            leading = listOf(reference, surah),
            ayahHits = listOf(ayah(1, arabic = 1)),
            tafsirHits = emptyList(),
            limit = 50,
        )
        assertEquals(reference, hits[0])
        assertEquals(surah, hits[1])
        assertEquals(1, (hits[2] as SearchHit.AyahHit).ayah.number)
    }

    @Test
    fun anAyahMatchedTwiceRanksByItsStrongestSource() {
        // Matched in both the Arabic and the translation: it ranks with the
        // Arabic group but still carries its translation, so the row loses
        // nothing it used to show.
        val (hits, _) = orderSearchHits(
            leading = emptyList(),
            ayahHits = listOf(ayah(10, arabic = 1, translation = true), ayah(2, translation = true)),
            tafsirHits = emptyList(),
            limit = 50,
        )
        val numbers = hits.filterIsInstance<SearchHit.AyahHit>().map { it.ayah.number }
        assertEquals(listOf(10, 2), numbers)
        assertTrue((hits[0] as SearchHit.AyahHit).translation != null)
    }

    @Test
    fun aPassageIsNotShownOncePerAyahItCovers() {
        val (hits, _) = orderSearchHits(
            leading = emptyList(),
            ayahHits = emptyList(),
            tafsirHits = listOf(tafsir(2), tafsir(2), tafsir(4, pack = "q"), tafsir(4, pack = "q")),
            limit = 50,
        )
        assertEquals(2, hits.size)
    }

    @Test
    fun theCapKeepsTheOrderAndSaysItWasCapped() {
        val (hits, capped) = orderSearchHits(
            leading = emptyList(),
            ayahHits = (1..10).map { ayah(it, arabic = 1) },
            tafsirHits = emptyList(),
            limit = 4,
        )
        assertTrue(capped)
        assertEquals(listOf(1, 2, 3, 4), hits.filterIsInstance<SearchHit.AyahHit>().map { it.ayah.number })
    }
}
