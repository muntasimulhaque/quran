package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rule that names a pack, pinned without a window.
 *
 * A pack used to be named by a `when` over its id inside the app's interface,
 * so a new translation or reciter was a code edit before it could have a name.
 * The names are content now, and this is the one rule that reads them: the
 * catalog's English name or its Bangla one, by the interface's language, never
 * blank (owner decision, 20.2).
 */
class PackNamesTest {

    @Test
    fun anEnglishInterfaceReadsTheEnglishName() {
        assertEquals(
            "Saheeh International",
            packName("Saheeh International", "সহীহ ইন্টারন্যাশনাল", "en"),
        )
    }

    @Test
    fun aBanglaInterfaceReadsTheBanglaName() {
        assertEquals(
            "সহীহ ইন্টারন্যাশনাল",
            packName("Saheeh International", "সহীহ ইন্টারন্যাশনাল", "bn"),
        )
    }

    @Test
    fun aPackWithoutABanglaNameIsStillNamed() {
        assertEquals("Word pronunciation", packName("Word pronunciation", "", "bn"))
        assertEquals("Word pronunciation", packName("Word pronunciation", "   ", "bn"))
    }

    @Test
    fun aLanguageTheAppDoesNotCarryReadsTheEnglishName() {
        assertEquals("Ibn Kathir", packName("Ibn Kathir", "ইবনে কাসীর", "fr"))
        assertEquals("Ibn Kathir", packName("Ibn Kathir", "ইবনে কাসীর", ""))
    }

    @Test
    fun aBanglaLineIsDrawnOnlyWhenItWasWritten() {
        assertEquals("সহীহ ইন্টারন্যাশনাল", localizedPackLine("Saheeh International", "সহীহ ইন্টারন্যাশনাল", "bn"))
        assertEquals("Word pronunciation", localizedPackLine("Word pronunciation", "   ", "bn"))
        assertEquals("Word pronunciation", localizedPackLine("Word pronunciation", "", "bn"))
    }

    @Test
    fun anEnglishReaderKeepsTheEnglishLineEvenWhenABanglaLineExists() {
        assertEquals(
            "King Fahd Glorious Quran Printing Complex",
            localizedPackLine("King Fahd Glorious Quran Printing Complex", "বাদশাহ ফাহদ", "en"),
        )
        assertEquals(
            "King Fahd Glorious Quran Printing Complex",
            localizedPackLine("King Fahd Glorious Quran Printing Complex", "বাদশাহ ফাহদ", "fr"),
        )
    }

    @Test
    fun aPackNamesItselfThroughItsOwnEntry() {
        val pack = ContentPack(
            id = "reciter-minshawi",
            type = PackType.Recitation,
            name = "Minshawi",
            nameBn = "মিনশাবি",
            language = "ar",
            credit = "",
            license = "",
            version = "1",
            shipped = true,
            ayahs = 6236,
            bytes = 0,
        )
        assertEquals("Minshawi", pack.displayName("en"))
        assertEquals("মিনশাবি", pack.displayName("bn"))
    }
}
