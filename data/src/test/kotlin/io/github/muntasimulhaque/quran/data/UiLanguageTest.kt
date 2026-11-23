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
            catalog().of(UiLanguage.English),
        )
        assertEquals(
            LanguageContent(
                translation = "translation-al-bayan-bn",
                tafsir = "tafsir-ibn-kathir-bn",
                words = "words-bn",
            ),
            catalog().of(UiLanguage.Bangla),
        )
    }

    @Test
    fun aLanguageTheCatalogReadsNothingForChoosesNothing() {
        // An empty catalog names no pack, and a language choice must not put
        // an empty id into the reader's settings where no catalog names it.
        val chosen = AppSettings().withLanguage(UiLanguage.Bangla, CatalogContent())
        assertEquals("bn", chosen.uiLanguage)
        assertTrue(chosen.translationPacks.isEmpty())
        assertTrue(chosen.tafsirPacks.isEmpty())
        assertTrue(pendingAfterLanguage(null, UiLanguage.Bangla, setOf("kept"), CatalogContent(), emptySet()) == setOf("kept"))
    }

    @Test
    fun aStoredTagResolvesToItsLanguage() {
        assertEquals(UiLanguage.English, UiLanguage.of("en"))
        assertEquals(UiLanguage.Bangla, UiLanguage.of("bn"))
        assertNull(UiLanguage.of("ar"))
        assertNull(UiLanguage.of(null))
    }

    @Test
    fun thePhoneLanguageIsTheDefaultAndEnglishIsTheFallback() {
        assertEquals(UiLanguage.Bangla, UiLanguage.ofSystem("bn"))
        assertEquals(UiLanguage.English, UiLanguage.ofSystem("en"))
        assertEquals(UiLanguage.English, UiLanguage.ofSystem("ar"))
        assertEquals(UiLanguage.English, UiLanguage.ofSystem(null))
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

    private fun pack(
        id: String,
        language: String,
        shipped: Boolean = false,
    ) = ContentPack(
        id = id,
        type = when {
            id.startsWith("tafsir") -> PackType.Tafsir
            id.startsWith("words") -> PackType.Words
            else -> PackType.Translation
        },
        name = id,
        language = language,
        credit = "test",
        license = "test",
        version = "1",
        shipped = shipped,
        ayahs = 6236,
        bytes = 1,
    )

    /**
     * The catalog as the content build writes it: every pack the app carries,
     * and the `defaults` block that names what each interface language reads
     * (owner decision, 20.2).
     */
    internal fun catalog(): CatalogContent = CatalogContent(
        packs = listOf(
            pack("translation-saheeh-en", "en", shipped = true),
            pack("translation-al-bayan-bn", "bn", shipped = true),
            pack("tafsir-ibn-kathir-en", "en"),
            pack("tafsir-ibn-kathir-bn", "bn"),
            pack("words-en", "en"),
            pack("words-bn", "bn"),
        ),
        defaults = mapOf(
            "en" to LanguageContent("translation-saheeh-en", "tafsir-ibn-kathir-en", "words-en"),
            "bn" to LanguageContent("translation-al-bayan-bn", "tafsir-ibn-kathir-bn", "words-bn"),
        ),
    )

    @Test
    fun aFreshInstallStartsAtAlFatihaWithThePhonesLanguage() {
        val fresh = AppSettings().bootstrapFor(UiLanguage.Bangla, catalog())
        assertEquals("bn", fresh.uiLanguage)
        assertEquals(1, fresh.ayah)
        assertEquals(setOf("translation-al-bayan-bn"), fresh.translationPacks)
        assertTrue("the tafsir is a download the reader asks for", fresh.tafsirPacks.isEmpty())
        assertTrue("the reminder is offered after the first surah, not before", !fresh.reminderAnswered)
        assertTrue("the reminder starts off", !fresh.dailyAyah)
    }

    @Test
    fun aFreshChoiceSelectsTheLanguagesOwnTranslation() {
        val chosen = AppSettings().withLanguage(UiLanguage.Bangla, catalog())
        assertEquals("bn", chosen.uiLanguage)
        assertEquals(setOf("translation-al-bayan-bn"), chosen.translationPacks)
        assertTrue("a language choice never introduces a tafsir", chosen.tafsirPacks.isEmpty())
    }

    @Test
    fun movingLanguageSwapsTheTranslationDefault() {
        val english = AppSettings().withLanguage(UiLanguage.English, catalog())
        val bangla = english.withLanguage(UiLanguage.Bangla, catalog())
        assertEquals(setOf("translation-al-bayan-bn"), bangla.translationPacks)
        assertTrue("a reader who never chose a tafsir keeps none", bangla.tafsirPacks.isEmpty())
    }

    @Test
    fun aChosenTafsirMovesWithTheReader() {
        val before = AppSettings(
            uiLanguage = "en",
            translationPacks = setOf("translation-saheeh-en"),
            tafsirPacks = setOf("tafsir-ibn-kathir-en"),
        )
        val after = before.withLanguage(UiLanguage.Bangla, catalog())
        assertEquals(setOf("tafsir-ibn-kathir-bn"), after.tafsirPacks)
        val queue = pendingAfterLanguage(
            previous = UiLanguage.English,
            next = UiLanguage.Bangla,
            pending = setOf("translation-saheeh-en", "tafsir-ibn-kathir-en"),
            content = catalog(),
            chosenTafsir = after.tafsirPacks,
        )
        assertEquals(setOf("translation-al-bayan-bn", "tafsir-ibn-kathir-bn"), queue)
    }

    @Test
    fun aReaderWhoNeverChoseATafsirIsNotGivenOneByALanguageChange() {
        val english = AppSettings().withLanguage(UiLanguage.English, catalog())
        val bangla = english.withLanguage(UiLanguage.Bangla, catalog())
        assertTrue(bangla.tafsirPacks.isEmpty())
        val queue = pendingAfterLanguage(
            previous = UiLanguage.English,
            next = UiLanguage.Bangla,
            pending = setOf("translation-saheeh-en", "tafsir-ibn-kathir-en"),
            content = catalog(),
            chosenTafsir = bangla.tafsirPacks,
        )
        assertEquals(
            "the old language's tafsir leaves the queue and none takes its place",
            setOf("translation-al-bayan-bn"),
            queue,
        )
    }

    @Test
    fun aRetiredTranslationIsReplacedByTheLanguagesOwn() {
        val kept = translationSelectionAfterRemoval(
            language = "bn",
            selected = setOf("translation-retired-bn"),
            offered = setOf("translation-al-bayan-bn", "translation-saheeh-en"),
            content = catalog(),
        )
        assertEquals(setOf("translation-al-bayan-bn"), kept)
    }

    @Test
    fun aReaderWhoKeptAnotherTranslationKeepsItAndGetsTheDefaultBack() {
        val kept = translationSelectionAfterRemoval(
            language = "bn",
            selected = setOf("translation-retired-bn", "translation-saheeh-en"),
            offered = setOf("translation-al-bayan-bn", "translation-saheeh-en"),
            content = catalog(),
        )
        assertEquals(setOf("translation-saheeh-en", "translation-al-bayan-bn"), kept)
    }

    @Test
    fun aReaderWhoRemovedTheDefaultThemselvesIsNotGivenItBack() {
        val kept = translationSelectionAfterRemoval(
            language = "bn",
            selected = setOf("translation-saheeh-en"),
            offered = setOf("translation-al-bayan-bn", "translation-saheeh-en"),
            content = catalog(),
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
        val after = before.withLanguage(UiLanguage.Bangla, catalog())
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
        // the reader's back (owner report, 14.8).
        val after = pendingAfterLanguage(
            previous = UiLanguage.English,
            next = UiLanguage.Bangla,
            pending = setOf("translation-saheeh-en", "tafsir-ibn-kathir-en", "words-en"),
            content = catalog(),
            chosenTafsir = setOf("tafsir-ibn-kathir-bn"),
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
            content = catalog(),
            chosenTafsir = emptySet(),
        )
        assertEquals(
            setOf("translation-added-by-hand", "translation-al-bayan-bn"),
            after,
        )
    }

    @Test
    fun aFirstChoiceWithoutATafsirAsksOnlyForTheTranslation() {
        val after = pendingAfterLanguage(
            previous = null,
            next = UiLanguage.English,
            pending = emptySet(),
            content = catalog(),
            chosenTafsir = emptySet(),
        )
        assertEquals(setOf("translation-saheeh-en"), after)
    }

    @Test
    fun theReadersLanguageLeadsTheTranslationColumns() {
        // The catalog sorts by pack name, so Al-Bayan came before Saheeh and
        // the Font size sample, which draws the first line, previewed Bangla
        // under an English interface (owner report, 15.3).
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
