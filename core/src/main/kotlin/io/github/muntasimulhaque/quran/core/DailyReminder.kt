package io.github.muntasimulhaque.quran.core

import java.util.Calendar
import java.util.TimeZone

/**
 * When the daily reminder should arrive.
 *
 * The reminder is a quiet nudge, not an appointment: the reader picks an hour,
 * and the ayah arrives around it. The alarm is the platform's own batched one,
 * which needs no special access and can be late; the app says "around" and
 * never promises a minute (owner decision, 23.8). The exact alarm and its
 * permission were the old promise and were removed, because the one setup step
 * this app had was a trip to the phone's settings for a nudge.
 *
 * The moment itself is a minute from midnight, in the reader's own local time,
 * because the setting is still stored that way: a time set since 23.8 is the
 * top of an hour, and a stored half hour from before is honored until the
 * reader next sets it. A minute that has already passed today comes tomorrow.
 * The wall clock is the right clock here: a phone whose time or zone is
 * changed gets the reminder at the new local moment, and a change of zone is
 * corrected on the next launch of the app.
 *
 * What comes back is a moment, and nothing here says what the app does with
 * an alarm it finds already pending for a moment that has passed: that is
 * settled where the alarm is armed, by giving each day its own pending
 * intent, so a launch can put tomorrow's alarm back without touching the one
 * the phone is still holding.
 */
object DailyReminder {

    /**
     * The next moment the reminder should come: the reader's own minute today,
     * or the same minute tomorrow when today's has passed.
     *
     * [minuteOfDay] is minutes from midnight and is pulled back into the day
     * rather than wrapped, so a number past midnight is a number at the end
     * of the day and never the small hours of the next one.
     */
    fun nextOccurrence(
        minuteOfDay: Int,
        now: Long,
        lastMinuteOfDay: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long {
        val minute = minuteOfDay.coerceIn(0, lastMinuteOfDay)
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, minute / 60)
            set(Calendar.MINUTE, minute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // A day is a day of the reader's own calendar, not 86,400,000
            // milliseconds: on the morning the clocks change, adding a
            // calendar day keeps the reminder at the minute they chose and
            // adding a fixed length would not.
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
}