package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
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
}
