package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * The ayah of the day is one ayah per local day, deterministic, in the
 * Book's own order, and never out of range.
 */
class DailyAyahTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun theSameDayAlwaysShowsTheSameAyah() {
        val noon = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 12)
        assertEquals(
            DailyAyah.numberFor(noon, utc),
            DailyAyah.numberFor(noon + 60_000L, utc),
        )
    }

    @Test
    fun theAyahWalksTheBookInOrder() {
        val day = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 12)
        val today = DailyAyah.numberFor(day, utc)
        val tomorrow = DailyAyah.numberFor(day + 86_400_000L, utc)
        val next = if (today == DailyAyah.TOTAL_AYAHS) 1 else today + 1
        assertEquals("the walk runs forward, wrapping at the end", next, tomorrow)
    }

    @Test
    fun everyAyahIsInRangeAndTheWholeBookIsCovered() {
        val start = utc.millisOf(2026, Calendar.JANUARY, 1, 12)
        val seen = HashSet<Int>(DailyAyah.TOTAL_AYAHS)
        for (day in 0 until DailyAyah.TOTAL_AYAHS) {
            val number = DailyAyah.numberFor(start + day * 86_400_000L, utc)
            assertTrue("$number is outside 1..${DailyAyah.TOTAL_AYAHS}", number in 1..DailyAyah.TOTAL_AYAHS)
            seen += number
        }
        assertEquals(
            "a full rotation must reach every ayah exactly once",
            DailyAyah.TOTAL_AYAHS,
            seen.size,
        )
    }

    @Test
    fun midnightIsWhereTheDayTurns() {
        val justBefore = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 23) + 59 * 60_000L
        val justAfter = justBefore + 2 * 60_000L
        val before = DailyAyah.numberFor(justBefore, utc)
        val after = DailyAyah.numberFor(justAfter, utc)
        assertEquals(
            "the turn wraps at the Book's end, never past it",
            if (before == DailyAyah.TOTAL_AYAHS) 1 else before + 1,
            after,
        )
    }

    @Test
    fun theReaderSTimezoneDecidesTheDay() {
        // Two zones whose local days differ at one instant: the reader's
        // today is their own, not UTC's. Dhaka is ahead of Los Angeles, so
        // its day number is the larger one, and the two are one ayah apart.
        val instant = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 20)
        val east = TimeZone.getTimeZone("Asia/Dhaka")
        val west = TimeZone.getTimeZone("America/Los_Angeles")
        val dhaka = DailyAyah.numberFor(instant, east)
        val la = DailyAyah.numberFor(instant, west)
        assertEquals(
            "one local day apart means one ayah apart",
            dhaka,
            if (la == DailyAyah.TOTAL_AYAHS) 1 else la + 1,
        )
    }

    private fun TimeZone.millisOf(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(this).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
}
