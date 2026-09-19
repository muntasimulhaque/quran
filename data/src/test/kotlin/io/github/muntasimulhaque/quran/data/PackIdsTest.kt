package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The ids that name a pack are the contract between the catalog, the content
 * build, and the app. The word list is the one that has been wrong before:
 * it ignored its language, so a Bangla reader who had installed the Bangla
 * word list was shown an empty meaning panel.
 */
class PackIdsTest {

    @Test
    fun wordListIdCarriesItsLanguage() {
        assertEquals("words-en", ContentDatabase.wordsPackId("en"))
        assertEquals("words-bn", ContentDatabase.wordsPackId("bn"))
        assertEquals("words-ar", ContentDatabase.wordsPackId("ar"))
    }

    @Test
    fun theEnglishWordListIsAlsoTheSurahIntroductionPack() {
        assertEquals(ContentDatabase.WORDS_PACK, ContentDatabase.wordsPackId("en"))
        assertEquals(ContentDatabase.WORDS_PACK, ContentDatabase.SURAH_INFO)
    }

    @Test
    fun reciterPackIdCarriesTheReciter() {
        assertEquals("reciter-minshawi", ContentDatabase.reciterPack("minshawi"))
        assertEquals("reciter-husary", ContentDatabase.reciterPack("husary"))
    }

    @Test
    fun packTypeRoundTripsThroughItsWireName() {
        val names = mapOf(
            "translation" to PackType.Translation,
            "tafsir" to PackType.Tafsir,
            "script" to PackType.Script,
            "recitation" to PackType.Recitation,
            "words" to PackType.Words,
        )
        names.forEach { (wire, type) ->
            assertEquals(type, PackType.of(wire))
        }
    }
}
