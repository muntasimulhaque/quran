package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reader's pack choices, as facts rather than lines on a screen.
 *
 * These five answers used to live in the settings module, whose suite the
 * release gates never ran. They are the module's own now, and every rule they
 * carry is pinned here: a chosen pack that is not on the device is not drawn,
 * a tafsir is not a translation, and the word list follows the reading
 * language with no English fallback (owner decision, 21.5, moving 14.8 and
 * 6.6 into the gate).
 */
class PackChoicesTest {

    @Test
    fun `a chosen translation that is not installed does not count as one to draw`() {
        val packs = listOf(
            pack("translation-a", PackType.Translation, installed = false),
            pack("translation-b", PackType.Translation, installed = true),
        )
        val chosen = chosenTranslations(
            settings(translations = setOf("translation-a", "translation-b")),
            packs,
        )
        assertEquals(listOf("translation-b"), chosen.map { it.id })
    }

    @Test
    fun `chosen translations keep the catalog order`() {
        val packs = listOf(
            pack("translation-b", PackType.Translation),
            pack("translation-a", PackType.Translation),
        )
        val chosen = chosenTranslations(
            settings(translations = setOf("translation-a", "translation-b")),
            packs,
        )
        assertEquals(listOf("translation-b", "translation-a"), chosen.map { it.id })
    }

    @Test
    fun `a chosen tafsir is drawn only when installed and chosen`() {
        val packs = listOf(
            pack("tafsir-en", PackType.Tafsir, installed = true),
            pack("tafsir-bn", PackType.Tafsir, installed = false),
            pack("translation-en", PackType.Translation, installed = true),
        )
        val chosen = chosenTafsirs(
            settings(tafsirs = setOf("tafsir-en", "tafsir-bn"), translations = setOf("translation-en")),
            packs,
        )
        assertEquals(listOf("tafsir-en"), chosen.map { it.id })
    }

    @Test
    fun `hasChosenTranslation is the reader's answer even when the pack is missing`() {
        val packs = listOf(pack("translation-a", PackType.Translation, installed = false))
        val chosenButMissing = settings(translations = setOf("translation-a"))
        assertTrue(hasChosenTranslation(chosenButMissing, packs))
        assertFalse(hasInstalledTranslation(chosenButMissing, packs))
        assertFalse(hasChosenTranslation(settings(), packs))
    }

    @Test
    fun `the words pack follows the reader's language with no English fallback`() {
        val packs = listOf(
            pack("words-en", PackType.Words, language = "en"),
            pack("words-bn", PackType.Words, language = "bn"),
        )
        val bangla = wantedWordsPack(settings(language = "bn"), packs)
        assertEquals("words-bn", bangla?.id)
        val english = wantedWordsPack(settings(language = "en"), packs)
        assertEquals("words-en", english?.id)
    }

    @Test
    fun `a language whose list is not carried gets null, not a promise`() {
        val packs = listOf(pack("words-en", PackType.Words, language = "en"))
        assertNull(wantedWordsPack(settings(language = "bn"), packs))
    }

    private fun pack(
        id: String,
        type: PackType,
        language: String = "en",
        installed: Boolean = true,
    ) = ContentPack(
        id = id,
        type = type,
        name = id,
        language = language,
        credit = "",
        license = "",
        version = "1",
        shipped = false,
        ayahs = 6236,
        bytes = 1024,
        installed = installed,
    )

    private fun settings(
        translations: Set<String> = emptySet(),
        tafsirs: Set<String> = emptySet(),
        language: String = "en",
    ) = AppSettings(
        uiLanguage = language,
        translationPacks = translations,
        tafsirPacks = tafsirs,
    )
}
