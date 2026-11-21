package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * The clock the reminder's page reads, as arithmetic instead of a screenshot.
 *
 * The platform's Material clock showed Latin digits in a Bangla interface, so
 * every number here is formatted with the reader's own locale. The hours fold
 * to 1 through 12 on a 12 hour clock (midnight and noon are both 12), a 24
 * hour clock always carries two digits, and the period marker is the locale's
 * own (owner decision, 8.2).
 */
class ClockTextTest {

    private val english = Locale.US

    @Test
    fun `the twelve hour clock reads midnight and noon as twelve`() {
        assertEquals("12", clockHourText(0, twentyFour = false, locale = english))
        assertEquals("12", clockHourText(12, twentyFour = false, locale = english))
        assertEquals("11", clockHourText(23, twentyFour = false, locale = english))
    }

    @Test
    fun `one in the afternoon reads one`() {
        assertEquals("1", clockHourText(13, twentyFour = false, locale = english))
        assertEquals("6", clockHourText(6, twentyFour = false, locale = english))
    }

    @Test
    fun `the twenty four hour clock pads to two digits`() {
        assertEquals("00", clockHourText(0, twentyFour = true, locale = english))
        assertEquals("07", clockHourText(7, twentyFour = true, locale = english))
        assertEquals("23", clockHourText(23, twentyFour = true, locale = english))
    }

    @Test
    fun `minutes always carry two digits`() {
        assertEquals("00", clockMinuteText(0, english))
        assertEquals("07", clockMinuteText(7, english))
        assertEquals("59", clockMinuteText(59, english))
    }

    @Test
    fun `the period marker is the locale's own`() {
        assertEquals("AM", clockPeriodText(pm = false, locale = english))
        assertEquals("PM", clockPeriodText(pm = true, locale = english))
        val bangla = Locale.forLanguageTag("bn")
        val morning = clockPeriodText(pm = false, locale = bangla)
        val evening = clockPeriodText(pm = true, locale = bangla)
        assertTrue(morning.isNotBlank())
        assertTrue(evening.isNotBlank())
        assertNotEquals(morning, evening)
    }

    @Test
    fun `a Bangla clock writes the locale's own digits`() {
        val bangla = Locale.forLanguageTag("bn")
        val zero = DecimalFormatSymbols.getInstance(bangla).zeroDigit
        val expectedHour = "${zero}${zero + 7}"
        assertEquals(expectedHour, clockHourText(7, twentyFour = true, locale = bangla))
        val expectedMinute = "${zero}${zero + 5}"
        assertEquals(expectedMinute, clockMinuteText(5, bangla))
    }
}
