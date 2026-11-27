package io.github.muntasimulhaque.quran.ui.settings

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import io.github.muntasimulhaque.quran.core.clockHourOfDayText

/**
 * The stored minute as the hour the page names, in the phone's own clock.
 *
 * The reminder is an hour now (owner decision, 23.8), so the hub row and the
 * page's Time row read the hour a stored minute falls in: "8 AM" on a twelve
 * hour phone, "08:00" on a twenty-four hour one, in the reader's own digits.
 * A time set since 23.8 is the top of its hour; a stored half hour from before
 * reads as its hour and keeps its moment until the reader next sets it.
 */
@Composable
internal fun hourOfDay(minuteOfDay: Int): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val twentyFour = remember(context, locale) { DateFormat.is24HourFormat(context) }
    return remember(minuteOfDay, twentyFour, locale) {
        clockHourOfDayText(minuteOfDay, twentyFour, locale)
    }
}
