package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The word meaning is drawn only when it is a row's only evidence of the
 * match, and never as a second copy of a highlight the reader can already
 * see.
 */
class SearchMeaningTest {

    private fun hit(
        arabic: Int = 0,
        translationRanges: List<IntRange> = emptyList(),
        meaning: String? = null,
    ) = SearchHit.AyahHit(
        ayah = Ayah(1, 1, 1, "1:1", "text"),
        page = 1,
        arabicMatchedWords = (1..arabic).toSet(),
        translation = if (translationRanges.isEmpty()) {
            null
        } else {
            TranslationHit("p", "Pack", "text", translationRanges, emptyList())
        },
        wordMeaning = meaning,
    )

    @Test
    fun theMeaningIsDroppedWhenTheTranslationAlreadyShowsTheMatch() {
        assertFalse(
            "the translation's own wash is the evidence; the block is noise",
            shouldShowWordMeaning(hit(translationRanges = listOf(4..8), meaning = "and His Mercy")),
        )
    }

    @Test
    fun theMeaningIsDroppedWhenTheArabicMatched() {
        assertFalse(
            "the Arabic wash is the evidence",
            shouldShowWordMeaning(hit(arabic = 2, meaning = "and His Mercy")),
        )
    }

    @Test
    fun theMeaningIsDrawnWhenNothingElseOnTheRowMatched() {
        assertTrue(
            "a row that matched only a meaning has nothing else to show",
            shouldShowWordMeaning(hit(meaning = "and His Mercy")),
        )
        assertTrue(
            "a meaning on a row with a translation that did not match still shows",
            shouldShowWordMeaning(hit(translationRanges = emptyList(), meaning = "and His Mercy")),
        )
    }

    @Test
    fun noMeaningIsEverInvented() {
        assertFalse("a row with nothing on it draws nothing", shouldShowWordMeaning(hit()))
    }
}
