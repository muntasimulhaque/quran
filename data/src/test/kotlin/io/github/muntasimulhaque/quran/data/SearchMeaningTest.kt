package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
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
        words: List<String> = emptyList(),
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
        matchedWordText = words,
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

    /**
     * The Arabic a row draws, decided by the same reasoning in the other
     * direction: four lines of text that never matched must not stand above a
     * one-line highlight, and a word-meaning row must point at its word.
     */
    @Test
    fun theAyahIsDrawnWholeWhenTheArabicIsTheMatch() {
        assertEquals(SearchArabicLine.Ayah, arabicLineFor(hit(arabic = 2)))
    }

    @Test
    fun aWordOnlyRowDrawsTheWordAndNotTheAyah() {
        val line = arabicLineFor(hit(meaning = "and His Mercy", words = listOf("\u0631\u062d\u0645\u0629")))
        assertEquals(
            "the row must answer why it is here with the word, not with a page",
            SearchArabicLine.Words(listOf("\u0631\u062d\u0645\u0629")),
            line,
        )
    }

    @Test
    fun aTranslationRowDrawsNoArabicAtAll() {
        assertEquals(
            "the Arabic matched nothing here, so it is decoration",
            SearchArabicLine.None,
            arabicLineFor(hit(translationRanges = listOf(4..8), meaning = "and His Mercy")),
        )
        assertEquals(
            "and the words that carried the meaning are not a second copy of the wash",
            SearchArabicLine.None,
            arabicLineFor(
                hit(
                    translationRanges = listOf(4..8),
                    meaning = "and His Mercy",
                    words = listOf("\u0631\u062d\u0645\u0629"),
                ),
            ),
        )
    }

    @Test
    fun aReferenceRowStillShowsTheAyahItNames() {
        assertEquals(
            "with no other evidence the Arabic is all the row has to say",
            SearchArabicLine.Ayah,
            arabicLineFor(hit()),
        )
    }
}
