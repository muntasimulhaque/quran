package io.github.muntasimulhaque.quran.core

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The digits a reference is drawn with follow the interface's language, and
 * nothing else in the line moves with them.
 */
class DigitsTest {

    private val bangla = Locale.forLanguageTag("bn")

    @Test
    fun aReferenceIsWrittenInTheLanguagesDigits() {
        assertEquals("\u09EB\u09EC:\u09EB\u09EF", localizedDigits("56:59", bangla))
    }

    @Test
    fun aRangeKeepsItsHyphenAndItsSurahName() {
        assertEquals(
            "Al-Baqarah \u09E8:\u09E8\u09EB\u09EB-\u09E8\u09EC\u09E6",
            localizedDigits("Al-Baqarah 2:255-260", bangla),
        )
    }

    @Test
    fun latinDigitsAreAlreadyTheAnswer() {
        val reference = "Al-Waqi'ah 56:58"
        assertSame(reference, localizedDigits(reference, Locale.US))
    }

    @Test
    fun aStringWithNoDigitIsReturnedUnchanged() {
        val text = "Al-Fatihah"
        assertSame(text, localizedDigits(text, bangla))
    }
}
