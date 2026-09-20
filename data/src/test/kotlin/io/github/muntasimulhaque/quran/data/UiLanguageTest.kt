package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The language choice is one decision with three content consequences, and
 * this is the only place that maps one to the other. A fresh install gets
 * exactly its language's three packs; moving between the offered languages
 * swaps the old language's defaults for the new one's; and a pack the reader
 * added by hand is never touched by a language change.
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
    fun aHandAddedPackSurvivesTheMove() {
        val before = AppSettings(
            uiLanguage = "en",
            translationPacks = setOf("translation-saheeh-en"),
            tafsirPacks = setOf("tafsir-ibn-kathir-en", "tafsir-as-sadi-ar"),
        )
        val after = before.withLanguage(UiLanguage.Bangla)
        assertTrue("As-Sa'di was added by hand and must stay", "tafsir-as-sadi-ar" in after.tafsirPacks)
        assertTrue("the Bangla tafsir joins it", "tafsir-ibn-kathir-bn" in after.tafsirPacks)
        assertTrue("the English default steps aside", "tafsir-ibn-kathir-en" !in after.tafsirPacks)
        assertEquals(setOf("translation-taisirul-quran-bn"), after.translationPacks)
    }
}
