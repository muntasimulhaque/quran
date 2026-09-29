package io.github.muntasimulhaque.quran.core

import java.util.Calendar
import java.util.TimeZone

/**
 * When the daily reminder should arrive, as a moment and as a tolerance.
 *
 * The reminder is the only thing in the app with a life outside it, so the
 * moment it is promised for is a promise and not a preference. The alarm the
 * app arms used to be the system's most inexact one
 * (`AlarmManager.set`), which the platform is free to deliver at any time
 * after the trigger, with no upper bound: a reader who asked for ten o'clock
 * was told they had it, and the shade lit up at ten past, and the app had
 * kept the promise in the only sense the platform allowed (owner report, 37th
 * session).
 *
 * A windowed alarm was tried as the answer and measured, and it is not one:
 * the platform has a floor for the window of ten minutes on Android 15, so
 * the app's one minute ask comes back as ten, and the windowed path would have
 * been no better than the one it replaced (found by `DailyReminderTest`
 * reading the phone's own alarm record, 37th session). What is left is the
 * honest pair: an exact alarm where the reader has granted the phone's own
 * exact time, which is the moment to the second even on a phone that is locked
 * and asleep, and the platform's own batched alarm where they have not, which
 * can still reach a sleeping phone and can be minutes late.
 *
 * The moment itself is the reader's minute to the second, in their own local
 * time, and a minute that has already passed today comes tomorrow. The wall
 * clock is the right clock here: a phone whose time or zone is changed gets
 * the reminder at the new local minute, and a change of zone is corrected on
 * the next launch of the app.
 *
 * Which of the two alarms is available is the reader's own setting in the
 * phone, never the app's to assume, so [plan] is where the choice is made and
 * it is a pure function: the JVM suite pins both halves, and the instrumented
 * suite pins the alarm the platform actually holds (owner decision, D-114).
 */
object DailyReminder {

    /**
     * The window the app asks for when the phone will not give it an exact
     * time: one minute, the narrowest ask the platform allows, and still a
     * request and not a promise, because the platform's own floor is ten
     * minutes on Android 15. The inexact path is therefore never described to
     * the reader as arriving to the minute (D-114).
     */
    const val WINDOW_MILLIS = 60_000L

    /**
     * Which kind of alarm a reminder should be, and when. The two paths are
     * the same moment asked for in two ways: the exact alarm lands at the
     * minute the reader chose even on a phone that is locked and idle then,
     * and the inexact one is the platform's own batched alarm, which can
     * reach a phone that is asleep and can be minutes late. Which one is
     * available is the reader's own setting in the phone, never the app's to
     * assume.
     */
    data class AlarmPlan(
        val kind: Kind,
        /** The reader's minute, to the second, in their own local time. */
        val triggerAtMillis: Long,
        /** How late the platform was asked to be. Zero for an exact alarm. */
        val windowMillis: Long,
    )

    /** The two shapes a daily reminder can take on a given phone. */
    enum class Kind {
        /** The minute itself, waking the device if it is asleep. */
        Exact,

        /**
         * The platform's own batched alarm, delivered inside a window the
         * phone decides, and able to reach a phone that is asleep.
         */
        Batched,
    }

    /**
     * The alarm to arm: the reader's minute either way, and the best shape
     * the phone will give us for it.
     *
     * [canScheduleExact] is the phone's own answer, read from
     * `AlarmManager.canScheduleExactAlarms()` on Android 12 and later and
     * always true before it, where exact alarms were never gated. The two
     * kinds differ only in the window, never in the moment, so a reader who
     * grants the permission and one who does not are both reminded at the
     * minute they chose, with only the punctuality left to the phone
     * (owner decision, D-114).
     */
    fun plan(
        canScheduleExact: Boolean,
        minuteOfDay: Int,
        now: Long,
        lastMinuteOfDay: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): AlarmPlan {
        val at = nextOccurrence(minuteOfDay, now, lastMinuteOfDay, timeZone)
        return if (canScheduleExact) {
            AlarmPlan(Kind.Exact, at, 0L)
        } else {
            AlarmPlan(Kind.Batched, at, WINDOW_MILLIS)
        }
    }

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

    /**
     * Whether the platform may still be holding today's alarm.
     *
     * The app re-arms the reminder at every launch, because it asks for no
     * boot permission and a reboot is the one thing it cannot hear about
     * (D-097). Re-arming replaces the pending alarm, and the moment the reader
     * set has just passed, so a launch inside that minute throws away a
     * delivery the platform is still going to make and the morning's reminder
     * never arrives at all (owner report, 37th session). The platform is never
     * late by more than the window, so "the moment passed less than a window
     * ago" is the whole test: leave the alarm exactly where it is.
     *
     * The cost is honest and small: an alarm that really was lost, on a phone
     * that really did reboot, and a reader who opens the app within a minute
     * of their own moment, loses that one morning. The other way round loses
     * the same morning to a habit, which is the commoner of the two.
     */
    fun stillDueToday(
        minuteOfDay: Int,
        now: Long,
        lastMinuteOfDay: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Boolean {
        val moment = todayAt(minuteOfDay, now, lastMinuteOfDay, timeZone)
        val late = now - moment
        return late in 0 until WINDOW_MILLIS
    }

    /** Today's moment, at the reader's minute to the second, past or not. */
    private fun todayAt(
        minuteOfDay: Int,
        now: Long,
        lastMinuteOfDay: Int,
        timeZone: TimeZone,
    ): Long {
        val minute = minuteOfDay.coerceIn(0, lastMinuteOfDay)
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, minute / 60)
            set(Calendar.MINUTE, minute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}