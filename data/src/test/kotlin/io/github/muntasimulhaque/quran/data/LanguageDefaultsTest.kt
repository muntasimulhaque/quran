package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What a language reads, resolved from the catalog, pinned without a device.
 *
 * A language's own translation, tafsir, and word list used to be four
 * constants in `UiLanguage`, so a second English or a translation in a
 * language the app had no interface for was a code edit. They are the
 * catalog's `defaults` block now, and this is the rule that reads it: the
 * named pack when the catalog carries it, otherwise the first pack of the
 * right type in that language with a bundled one before a downloaded one,
 * otherwise nothing (owner decision, 20.2).
 */
class LanguageDefaultsTest {

    @Test
    fun aNamedDefaultIsWhatTheLanguageReads() {
        val content = CatalogContent(
            packs = listOf(
                pack("translation-saheeh-en", PackType.Translation, "en"),
                pack("tafsir-ibn-kathir-en", PackType.Tafsir, "en"),
                pack("words-en", PackType.Words, "en"),
            ),
            defaults = mapOf(
                "en" to LanguageContent("translation-saheeh-en", "tafsir-ibn-kathir-en", "words-en"),
            ),
        )
        assertEquals(
            LanguageContent("translation-saheeh-en", "tafsir-ibn-kathir-en", "words-en"),
            content.of(UiLanguage.English),
        )
    }

    @Test
    fun aLanguageTheCatalogDoesNotNameResolvesThroughItsOwnPacks() {
        // A catalog written before the defaults block, or one whose row for
        // this language was never written: the language still reads a real
        // pack of its own rather than nothing.
        val content = CatalogContent(
            packs = listOf(
                pack("translation-second-en", PackType.Translation, "en"),
                pack("translation-first-en", PackType.Translation, "en", shipped = true),
                pack("tafsir-ibn-kathir-en", PackType.Tafsir, "en"),
            ),
            defaults = emptyMap(),
        )
        assertEquals(
            // The bundled translation leads the downloaded one, whatever the
            // order they arrived in.
            "translation-first-en",
            content.of(UiLanguage.English).translation,
        )
        assertEquals("tafsir-ibn-kathir-en", content.of(UiLanguage.English).tafsir)
    }

    @Test
    fun aDefaultTheCatalogDoesNotCarryIsIgnoredAndTheSlotStillResolves() {
        // One typo in the catalog must never select a pack that is not there
        // and must never take anything down.
        val content = CatalogContent(
            packs = listOf(pack("translation-saheeh-en", PackType.Translation, "en")),
            defaults = mapOf(
                "en" to LanguageContent("translation-typo-en", "tafsir-typo-en", "words-typo-en"),
            ),
        )
        assertEquals("translation-saheeh-en", content.of(UiLanguage.English).translation)
        assertEquals("", content.of(UiLanguage.English).tafsir)
        assertEquals("", content.of(UiLanguage.English).words)
    }

    @Test
    fun aLanguageWithNoPackOfATypeReadsNone() {
        val content = CatalogContent(
            packs = listOf(pack("translation-saheeh-en", PackType.Translation, "en")),
            defaults = mapOf(
                "bn" to LanguageContent("translation-al-bayan-bn", "tafsir-ibn-kathir-bn", "words-bn"),
            ),
        )
        assertEquals(LanguageContent("", "", ""), content.of(UiLanguage.Bangla))
    }

    @Test
    fun anEmptyCatalogReadsNothing() {
        assertEquals(LanguageContent("", "", ""), CatalogContent().of(UiLanguage.English))
        assertEquals(LanguageContent("", "", ""), CatalogContent().of(UiLanguage.Bangla))
    }

    @Test
    fun onlyTheLanguagesOwnPacksAnswerForIt() {
        // A translation the reader added by hand in another language is never
        // the language's default, however few packs its own language has.
        val content = CatalogContent(
            packs = listOf(
                pack("translation-al-bayan-bn", PackType.Translation, "bn"),
                pack("translation-saheeh-en", PackType.Translation, "en"),
            ),
            defaults = emptyMap(),
        )
        assertEquals("translation-saheeh-en", content.of(UiLanguage.English).translation)
        assertEquals("translation-al-bayan-bn", content.of(UiLanguage.Bangla).translation)
    }

    @Test
    fun twoPacksOfATypeInOneLanguageKeepTheCatalogsOrder() {
        val content = CatalogContent(
            packs = listOf(
                pack("translation-first-en", PackType.Translation, "en"),
                pack("translation-second-en", PackType.Translation, "en"),
            ),
            defaults = emptyMap(),
        )
        assertEquals("translation-first-en", content.of(UiLanguage.English).translation)
    }

    private fun pack(
        id: String,
        type: PackType,
        language: String,
        shipped: Boolean = false,
    ) = ContentPack(
        id = id,
        type = type,
        name = id,
        language = language,
        credit = "test",
        license = "test",
        version = "1",
        shipped = shipped,
        ayahs = 6236,
        bytes = 1,
    )
}
