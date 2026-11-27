package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * The reminder's promise is a moment, and these are the numbers behind it.
 *
 * A reader who sets ten o'clock is owed the next ten o'clock in their own
 * local time; the platform's batched alarm decides how punctual it is, and the
 * page says "around" for that reason. The JVM suite pins the moment, the day
 * boundary, the late fire's anchor, and both ends of the day here, where a
 * test can be exact (owner decision, 23.8).
 */
class DailyReminderTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val lastMinute = 24 * 60 - 1

    @Test
    fun theMomentIsTheReadersMinuteToTheSecond() {
        val before = utc.millisOf(2026, Calendar.SEPTEMBER, 29, 9, 12)
        val at = DailyReminder.nextOccurrence(10 * 60, before, lastMinute, utc)
        val calendar = Calendar.getInstance(utc).apply { timeInMillis = at }
        assertEquals("the hour is the reader's", 10, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals("the minute is the reader's", 0, calendar.get(Calendar.MINUTE))
        assertEquals("and the second is the top of it", 0, calendar.get(Calendar.SECOND))
        assertEquals("and the millisecond too", 0, calendar.get(Calendar.MILLISECOND))
        assertTrue("and it is ahead of the reader", at > before)
    }

    @Test
    fun aMomentThatHasPassedComesTomorrow() {
        val now = utc.millisOf(2026, Calendar.SEPTEMBER, 29, 10, 30)
        val today = utc.millisOf(2026, Calendar.SEPTEMBER, 29, 10, 0)
        val at = DailyReminder.nextOccurrence(10 * 60, now, lastMinute, utc)
        val calendar = Calendar.getInstance(utc).apply { timeInMillis = at }
        assertEquals("the same hour", 10, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals("on the same minute", 0, calendar.get(Calendar.MINUTE))
        assertEquals("and a whole day after today's moment", today + 86_400_000L, at)
    }

    /**
     * A fire re-anchors on the reader's minute, not on the moment the platform
     * got round to delivering it. This is what stops a late delivery from
     * walking the hour forward a little every morning until the reminder is
     * arriving at lunchtime.
     */
    @Test
    fun aLateFireDoesNotWalkTheHourForward() {
        val due = utc.millisOf(2026, Calendar.SEPTEMBER, 29, 10, 0)
        val deliveredLate = due + 4 * 60_000L
        val next = DailyReminder.nextOccurrence(10 * 60, deliveredLate, lastMinute, utc)
        val calendar = Calendar.getInstance(utc).apply { timeInMillis = next }
        assertEquals("the next one is the reader's minute", 0, calendar.get(Calendar.MINUTE))
        assertEquals("at the reader's hour", 10, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals("a day after the moment that was due", due + 86_400_000L, next)
    }

    /**
     * Both ends of the day are reachable, and a number outside the day is
     * pulled back into it rather than wrapped: a reader who set 23:59 gets
     * 23:59, and a value past midnight never becomes the small hours of the
     * next morning.
     */
    @Test
    fun theEndsOfTheDayAreReachableAndNothingWraps() {
        val now = utc.millisOf(2026, Calendar.SEPTEMBER, 29, 12, 0)
        for (minute in listOf(0, 1, 23 * 60 + 59, lastMinute)) {
            val at = DailyReminder.nextOccurrence(minute, now, lastMinute, utc)
            val calendar = Calendar.getInstance(utc).apply { timeInMillis = at }
            assertEquals("minute $minute lands on itself", minute, calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE))
        }
        val past = DailyReminder.nextOccurrence(lastMinute + 400, now, lastMinute, utc)
        val calendar = Calendar.getInstance(utc).apply { timeInMillis = past }
        assertTrue(
            "a number past the end of the day is pulled back, not wrapped",
            calendar.get(Calendar.HOUR_OF_DAY) in 0..23,
        )
        assertEquals(
            "and pulled back to the end of the day",
            lastMinute,
            calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE),
        )
        val negative = DailyReminder.nextOccurrence(-5, now, lastMinute, utc)
        val negativeCalendar = Calendar.getInstance(utc).apply { timeInMillis = negative }
        assertEquals(
            "a negative minute is the first minute of the day, not yesterday",
            0,
            negativeCalendar.get(Calendar.HOUR_OF_DAY) * 60 + negativeCalendar.get(Calendar.MINUTE),
        )
    }

    /**
     * The reminder is anchored on the wall clock, so a phone whose clock or
     * zone is moved gets the reminder at its new local minute, and the same
     * number of minutes past midnight means the same minute of the day in
     * every zone.
     */
    @Test
    fun theMomentIsLocalInEveryZone() {
        val dhaka = TimeZone.getTimeZone("Asia/Dhaka")
        val noonUtc = utc.millisOf(2026, Calendar.SEPTEMBER, 29, 12, 0)
        for (zone in listOf(utc, dhaka, TimeZone.getTimeZone("America/New_York"))) {
            val now = noonUtc - zone.getOffset(noonUtc)
            val at = DailyReminder.nextOccurrence(10 * 60, now, lastMinute, zone)
            val calendar = Calendar.getInstance(zone).apply { timeInMillis = at }
            assertEquals(
                "ten o'clock is ten o'clock in ${zone.id}",
                10 * 60,
                calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE),
            )
        }
    }

    private fun TimeZone.millisOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(this).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
