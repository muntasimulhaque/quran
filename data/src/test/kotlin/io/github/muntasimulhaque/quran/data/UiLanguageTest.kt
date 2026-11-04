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
                translation = "translation-al-bayan-bn",
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
    fun searchReadsOnlyTheChosenLanguagesPacks() {
        // The reader's own on switches can hold a pack of another language
        // (one they added by hand); search must still answer in the language
        // they chose, or one field would speak in two voices.
        val packs = listOf(
            pack("translation-saheeh-en", "en"),
            pack("translation-al-bayan-bn", "bn"),
            pack("tafsir-ibn-kathir-en", "en"),
            pack("tafsir-ibn-kathir-bn", "bn"),
        )
        assertEquals(
            listOf("translation-saheeh-en", "tafsir-ibn-kathir-en"),
            packs.forLanguage("en").map { it.id },
        )
        assertEquals(
            listOf("translation-al-bayan-bn", "tafsir-ibn-kathir-bn"),
            packs.forLanguage("bn").map { it.id },
        )
        assertTrue(packs.forLanguage("ar").isEmpty())
    }

    private fun pack(id: String, language: String) = ContentPack(
        id = id,
        type = if (id.startsWith("tafsir")) PackType.Tafsir else PackType.Translation,
        name = id,
        language = language,
        credit = "test",
        license = "test",
        version = "1",
        shipped = false,
        ayahs = 6236,
        bytes = 1,
    )

    @Test
    fun aFreshChoiceSelectsTheLanguagesOwnPacks() {
        val chosen = AppSettings().withLanguage(UiLanguage.Bangla)
        assertEquals("bn", chosen.uiLanguage)
        assertEquals(setOf("translation-al-bayan-bn"), chosen.translationPacks)
        assertEquals(setOf("tafsir-ibn-kathir-bn"), chosen.tafsirPacks)
    }

    @Test
    fun movingLanguageSwapsTheDefaults() {
        val english = AppSettings().withLanguage(UiLanguage.English)
        val bangla = english.withLanguage(UiLanguage.Bangla)
        assertEquals(setOf("translation-al-bayan-bn"), bangla.translationPacks)
        assertEquals(setOf("tafsir-ibn-kathir-bn"), bangla.tafsirPacks)
    }

    @Test
    fun aRetiredTranslationIsReplacedByTheLanguagesOwn() {
        val kept = translationSelectionAfterRemoval(
            language = "bn",
            selected = setOf("translation-retired-bn"),
            offered = setOf("translation-al-bayan-bn", "translation-saheeh-en"),
        )
        assertEquals(setOf("translation-al-bayan-bn"), kept)
    }

    @Test
    fun aReaderWhoKeptAnotherTranslationKeepsItAndGetsTheDefaultBack() {
        val kept = translationSelectionAfterRemoval(
            language = "bn",
            selected = setOf("translation-retired-bn", "translation-saheeh-en"),
            offered = setOf("translation-al-bayan-bn", "translation-saheeh-en"),
        )
        assertEquals(setOf("translation-saheeh-en", "translation-al-bayan-bn"), kept)
    }

    @Test
    fun aReaderWhoRemovedTheDefaultThemselvesIsNotGivenItBack() {
        val kept = translationSelectionAfterRemoval(
            language = "bn",
            selected = setOf("translation-saheeh-en"),
            offered = setOf("translation-al-bayan-bn", "translation-saheeh-en"),
        )
        assertEquals(setOf("translation-saheeh-en"), kept)
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
        assertEquals(setOf("translation-al-bayan-bn"), after.translationPacks)
    }

    @Test
    fun theQueueFollowsTheLanguageThatAskedForIt() {
        // A language's packs are asked for once. When the reader moves on,
        // the packs the old language asked for leave the queue with it, so
        // an update that detaches one cannot fetch it and turn it on behind
        // the reader's back (owner report, this session).
        val after = pendingAfterLanguage(
            previous = UiLanguage.English,
            next = UiLanguage.Bangla,
            pending = setOf("translation-saheeh-en", "tafsir-ibn-kathir-en", "words-en"),
        )
        assertEquals(
            setOf("translation-al-bayan-bn", "tafsir-ibn-kathir-bn", "words-en"),
            after,
        )
    }

    @Test
    fun aHandAddedPackStaysInTheQueueAcrossALanguageChange() {
        val after = pendingAfterLanguage(
            previous = UiLanguage.English,
            next = UiLanguage.Bangla,
            pending = setOf("translation-added-by-hand"),
        )
        assertEquals(
            setOf("translation-added-by-hand", "translation-al-bayan-bn", "tafsir-ibn-kathir-bn"),
            after,
        )
    }

    @Test
    fun aFirstChoiceAsksOnlyForItsOwnPacks() {
        val after = pendingAfterLanguage(
            previous = null,
            next = UiLanguage.English,
            pending = emptySet(),
        )
        assertEquals(setOf("translation-saheeh-en", "tafsir-ibn-kathir-en"), after)
    }

    @Test
    fun theReadersLanguageLeadsTheTranslationColumns() {
        // The catalog sorts by pack name, so Al-Bayan came before Saheeh and
        // the Font size sample, which draws the first line, previewed Bangla
        // under an English interface (owner report, this session).
        val catalog = listOf(
            pack("translation-al-bayan-bn", "bn"),
            pack("translation-saheeh-en", "en"),
        )
        assertEquals(
            listOf("translation-saheeh-en", "translation-al-bayan-bn"),
            catalog.leadingWith("en").map { it.id },
        )
        assertEquals(
            listOf("translation-al-bayan-bn", "translation-saheeh-en"),
            catalog.leadingWith("bn").map { it.id },
        )
    }

    @Test
    fun oneLanguagesPacksKeepTheCatalogsOrder() {
        // Only the language is moved. Inside a group, the order the catalog
        // gave the packs is the order the reader reads them in.
        val catalog = listOf(
            pack("translation-first-en", "en"),
            pack("translation-second-en", "en"),
            pack("translation-al-bayan-bn", "bn"),
        )
        assertEquals(
            listOf("translation-first-en", "translation-second-en"),
            catalog.leadingWith("en").filter { it.language == "en" }.map { it.id },
        )
        assertEquals(
            listOf("translation-first-en", "translation-second-en"),
            catalog.leadingWith("bn").filter { it.language == "en" }.map { it.id },
        )
    }

    @Test
    fun anUnchosenLanguageReadsEnglish() {
        assertEquals("en", AppSettings().readingLanguage)
        assertEquals("bn", AppSettings(uiLanguage = "bn").readingLanguage)
    }
}
