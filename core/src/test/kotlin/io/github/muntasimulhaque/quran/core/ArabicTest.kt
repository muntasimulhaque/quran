package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicTest {

    @Test
    fun `marks, superscript alef and madda fold to the same skeleton`() {
        assertEquals("الرحمن", Arabic.skeleton("ٱلرَّحْمَٰنِ"))
        assertEquals("الرحيم", Arabic.skeleton("ٱلرَّحِيمِ"))
        assertEquals("بسم", Arabic.skeleton("بِسۡمِ"))
    }

    @Test
    fun `hamza forms fold to their base letters`() {
        // alef with hamza above folds to alef, so the skeleton keeps the alef
        assertEquals("امنوا", Arabic.skeleton("آمَنُوا"))
        assertEquals("ياكلوا", Arabic.skeleton("يَأْكُلُواْ"))
    }

    @Test
    fun `ta marbuta folds to heh and alef maksura folds to yeh`() {
        assertEquals("صلاه", Arabic.skeleton("صَلَاةً"))
        assertEquals("هدي", Arabic.skeleton("هُدًى"))
    }

    @Test
    fun `the QPC print's letters fold to the reader's spelling for search`() {
        // The QPC print writes the alef maksura base as U+066E, and its small
        // waw and yeh as U+06E5 and U+06E6. A reader types the ordinary
        // letters; the search must find the print's spelling.
        assertEquals("هديكم", Arabic.normalizeForSearch("هَدَٮٰكُمۡ"))
        assertEquals("التوريه", Arabic.normalizeForSearch("ٱلتَّوۡرَٮٰةَ"))
        assertEquals("ادريك", Arabic.normalizeForSearch("أَدۡرَٮٰكَ"))
        assertEquals("داود", Arabic.normalizeForSearch("دَاوُۥدَ"))
        assertEquals("به", Arabic.normalizeForSearch("بِهِۦ"))
    }

    @Test
    fun `the audit skeleton keeps the QPC print's own letters`() {
        // The audit reports the print's spelling against Tanzil, so the
        // skeleton must not hide the letters the print chose.
        assertEquals("هدٮكم", Arabic.skeleton("هَدَٮٰكُمۡ"))
        assertEquals("داوۥد", Arabic.skeleton("دَاوُۥدَ"))
    }

    @Test
    fun `tatweel and Quranic signs are removed`() {
        assertEquals("بسمالله", Arabic.skeleton("بِسْـــمِ ٱللَّهِ ۖ"))
    }

    @Test
    fun `normalizeForSearch keeps word boundaries and folds digits`() {
        assertEquals("بسم الله الرحمن الرحيم", Arabic.normalizeForSearch("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"))
        assertEquals("255", Arabic.normalizeForSearch("٢٥٥"))
    }

    @Test
    fun `dotted circle is an artifact and never survives`() {
        assertEquals("ياكلوا", Arabic.skeleton("يَأْكُلُواْ \u25CC"))
    }

    @Test
    fun `the portable form folds the print to a plain Arabic face`() {
        // U+066E becomes alef maksura, the small waw and yeh are dropped, the
        // kashida and the print's separator are dropped, and the print's sukun
        // becomes the standard one. Everything a reader hears stays.
        assertEquals("فَسَوَّىٰهُنَّ", Arabic.portableText("فَسَوَّٮٰهُنَّ"))
        assertEquals("بِهِ", Arabic.portableText("بِهِۦ"))
        assertEquals("دَاوُدَ", Arabic.portableText("دَاوُۥدَ"))
        assertEquals("ٱلرَّحْمَٰنِ", Arabic.portableText("ٱلرَّحۡمَـٰنِ"))
        assertEquals("بِسْمِ", Arabic.portableText("بِسۡمِ"))
        assertEquals("يَسْجُدُونَ", Arabic.portableText("يَسۡجُدُونَ ۩"))
        assertEquals("يَسْجُدُونَ وَمَن", Arabic.portableText("يَسۡجُدُونَ ۩ وَمَن"))
    }

    @Test
    fun `the portable form drops the print's page and annotation signs`() {
        val source = "۞إِنَّ ٱللَّهَ لَا يَسۡتَحۡىِۦٓ أَن يَضۡرِبَ مَثَلاً مَّا بَعُوضَةً\u200cۚ " +
            "فَسَوَّٮٰهُنَّ سَبۡعَ سَمَـٰوٰتٍ دَاوُۥدَ ٱلرَّحۡمَـٰنِ يَسۡجُدُونَ ۩"
        val portable = Arabic.portableText(source)
        for (codePoint in listOf(
            0x0640, // the print's kashida
            0x066E, // the print's dotless alef-maksura base
            0x06D6, // a waqf sign
            0x06D9, // a tajweed ligature
            0x06DA, // the small high jeem pause sign
            0x06DE, // the rub el hizb
            0x06E2, // a tajweed meem
            0x06E5, // the small waw
            0x06E6, // the small yeh
            0x06E9, // the sajdah
            0x06ED, // the small low meem
            0x200C, // the print's separator
        )) {
            assertFalse(
                "U+${codePoint.toString(16).uppercase()} survived the portable form",
                portable.indexOf(codePoint.toChar()) >= 0,
            )
        }
        assertTrue("the harakat must stay", portable.indexOf('\u064E') >= 0)
        assertTrue("the sukun must stay as the standard one", portable.indexOf('\u0652') >= 0)
    }

    @Test
    fun `the ayah number is written in Arabic-Indic digits`() {
        // The reading closes a verse with its own number, and the KFGQPC Hafs
        // face draws the print's medallion around a run of these digits, so
        // the number and the sign are one glyph. The number is the reader's
        // own: 1, 255, and 6236 are the shortest, the famous middle, and the
        // last.
        assertEquals("\u0661", Arabic.ayahNumber(1))
        assertEquals("\u0662\u0665\u0665", Arabic.ayahNumber(255))
        assertEquals("\u0666\u0662\u0663\u0666", Arabic.ayahNumber(6236))
    }
}
