package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The language choice is one decision with three content consequences, and
 * this is the only place that maps one to the other. A fresh install gets
 * exactly its language's three packs; moving between the offered languages
 * swaps the old language's defaults for the new one's; and a pack that is
 * not one of those defaults is never touched by a language change.
 */
class UiLanguageTest {

    @Test
    fun eachOfferedLanguageNamesItsOwnPacks() {
        assertEquals(
            LanguageContent(
                translation = "translation-saheeh-en",
                tafsir = "tafsir-ibn-kathir-en",
                words = "words-en",
            ),
            UiLanguage.English.content,
        )
        assertEquals(
            LanguageContent(
                translation = "translation-taisirul-quran-bn",
                tafsir = "tafsir-ibn-kathir-bn",
                words = "words-bn",
            ),
            UiLanguage.Bangla.content,
        )
    }

    @Test
    fun aStoredTagResolvesToItsLanguage() {
        assertEquals(UiLanguage.English, UiLanguage.of("en"))
        assertEquals(UiLanguage.Bangla, UiLanguage.of("bn"))
        assertNull(UiLanguage.of("ar"))
        assertNull(UiLanguage.of(null))
    }

    @Test
    fun theSystemLanguageIsSuggestedWhenOffered() {
        assertEquals(UiLanguage.Bangla, UiLanguage.suggested("bn"))
        assertEquals(UiLanguage.English, UiLanguage.suggested("en"))
        assertEquals(UiLanguage.English, UiLanguage.suggested("ar"))
        assertEquals(UiLanguage.English, UiLanguage.suggested(null))
    }

    @Test
    fun aFreshChoiceSelectsTheLanguagesOwnPacks() {
        val chosen = AppSettings().withLanguage(UiLanguage.Bangla)
        assertEquals("bn", chosen.uiLanguage)
        assertEquals(setOf("translation-taisirul-quran-bn"), chosen.translationPacks)
        assertEquals(setOf("tafsir-ibn-kathir-bn"), chosen.tafsirPacks)
    }

    @Test
    fun movingLanguageSwapsTheDefaults() {
        val english = AppSettings().withLanguage(UiLanguage.English)
        val bangla = english.withLanguage(UiLanguage.Bangla)
        assertEquals(setOf("translation-taisirul-quran-bn"), bangla.translationPacks)
        assertEquals(setOf("tafsir-ibn-kathir-bn"), bangla.tafsirPacks)
    }

    @Test
    fun aPackThatIsNotTheOtherLanguagesDefaultSurvivesTheMove() {
        // Only the two packs the leaving language owns step aside. Anything
        // else in the reader's list is not one of them and stays exactly
        // where they put it. The id is synthetic because the offered packs
        // are only the two defaults; the rule is about the next pack the
        // catalog offers.
        val before = AppSettings(
            uiLanguage = "en",
            translationPacks = setOf("translation-saheeh-en"),
            tafsirPacks = setOf("tafsir-ibn-kathir-en", "tafsir-added-by-hand"),
        )
        val after = before.withLanguage(UiLanguage.Bangla)
        assertTrue("the added tafsir must stay", "tafsir-added-by-hand" in after.tafsirPacks)
        assertTrue("the Bangla tafsir joins it", "tafsir-ibn-kathir-bn" in after.tafsirPacks)
        assertTrue("the English default steps aside", "tafsir-ibn-kathir-en" !in after.tafsirPacks)
        assertEquals(setOf("translation-taisirul-quran-bn"), after.translationPacks)
    }
}
