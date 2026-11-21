package io.github.muntasimulhaque.quran.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * The hour, the minute, and the AM or PM marker as the reader's clock and
 * language read them.
 *
 * The platform's own clock control is not used: its Material clock showed
 * Latin digits in a Bangla interface. Every number here is formatted with the
 * reader's locale, which is where the digits come from, so a Bangla reader
 * sees Bangla digits on the 24 hour clock as well as the 12 hour one. The
 * period marker is the locale's own, the same one the reading's clock line
 * shows (owner decision, 8.2).
 *
 * These were private functions of the settings module's time picker; they
 * live here because their arithmetic is a rule, and a rule the gates can
 * measure (owner decision, 21.5).
 *
 * The hour is two digits on a 24 hour clock and the phone's own 1 to 12 on
 * the other; the minute is always two digits.
 */
fun clockHourText(hour: Int, twentyFour: Boolean, locale: Locale): String =
    if (twentyFour) {
        String.format(locale, "%02d", hour)
    } else {
        String.format(locale, "%d", ((hour + 11) % 12) + 1)
    }

/** The minute, always two digits, in the reader's language. */
fun clockMinuteText(minute: Int, locale: Locale): String =
    String.format(locale, "%02d", minute)

/** The locale's own AM or PM marker, the same one `clockText` shows. */
fun clockPeriodText(pm: Boolean, locale: Locale): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, if (pm) 13 else 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    return SimpleDateFormat("a", locale).format(calendar.time)
}
