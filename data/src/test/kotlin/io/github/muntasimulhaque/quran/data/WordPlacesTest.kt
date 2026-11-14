package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The place list's two pure rules: the surah groups and the item the reader's
 * own place stands on.
 *
 * The page draws a head for every surah and one row for every place, so the
 * current place's item index is the arithmetic of that drawing. It is pinned
 * here, on the JVM, because a compose test cannot read a scroll position
 * honestly and the reader's own place must open the list without a frame at
 * the top (owner decision, this session).
 */
class WordPlacesTest {

    private val places = listOf(
        place(ayahNumber = 1, surah = 1, ayah = 1, position = 1),
        place(ayahNumber = 2, surah = 1, ayah = 2, position = 2),
        place(ayahNumber = 8, surah = 2, ayah = 1, position = 1),
        place(ayahNumber = 262, surah = 2, ayah = 255, position = 1),
        place(ayahNumber = 262, surah = 2, ayah = 255, position = 2),
    )

    @Test
    fun thePlacesGroupBySurahInTheBooksOrder() {
        val groups = wordPlaceGroups(places)
        assertEquals(listOf(1, 2), groups.map { it.surah })
        assertEquals(listOf(1, 2, 8, 262, 262), groups.flatMap { group -> group.places.map { it.ayahNumber } })
    }

    @Test
    fun theListOpensUnderTheSurahsHeadTheReaderIsIn() {
        val groups = wordPlaceGroups(places)
        // Al-Fatiha's head is item 0 and Al-Baqarah's is item 3; the reader's
        // own row stands one item under its surah's head.
        assertEquals(0, wordPlaceOpenIndex(groups, ayahNumber = 1, position = 1))
        assertEquals(0, wordPlaceOpenIndex(groups, ayahNumber = 2, position = 2))
        assertEquals(3, wordPlaceOpenIndex(groups, ayahNumber = 8, position = 1))
        assertEquals(3, wordPlaceOpenIndex(groups, ayahNumber = 262, position = 2))
        assertEquals(0, wordPlaceOpenIndex(groups, ayahNumber = 99, position = 1))
    }

    @Test
    fun theCurrentPlaceStandsOneRowUnderItsHead() {
        val groups = wordPlaceGroups(places)
        // Item 0 is Al-Fatiha's head, items 1 and 2 are its two places; item 3
        // is Al-Baqarah's head, so 2:255's second word is the sixth item.
        assertEquals(1, wordPlaceItemIndex(groups, ayahNumber = 1, position = 1))
        assertEquals(2, wordPlaceItemIndex(groups, ayahNumber = 2, position = 2))
        assertEquals(4, wordPlaceItemIndex(groups, ayahNumber = 8, position = 1))
        assertEquals(6, wordPlaceItemIndex(groups, ayahNumber = 262, position = 2))
    }

    @Test
    fun aPlaceThatIsNotInTheListOpensTheTop() {
        val groups = wordPlaceGroups(places)
        assertEquals(0, wordPlaceItemIndex(groups, ayahNumber = 99, position = 1))
    }

    private fun place(
        ayahNumber: Int,
        surah: Int,
        ayah: Int,
        position: Int,
    ) = WordPlace(
        ayahNumber = ayahNumber,
        surah = surah,
        ayah = ayah,
        position = position,
        word = "word",
    )
}
