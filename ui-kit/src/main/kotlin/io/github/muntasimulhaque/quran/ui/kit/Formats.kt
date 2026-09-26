package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Human sizes for a download the reader is about to approve. The numbers are
 * printed in US English like every other string in the interface, so a device
 * set to a locale with a decimal comma cannot put one inside a sentence the
 * store or a screenshot shows.
 */
fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 * 1024 -> "${(bytes + 512) / 1024} KB"
    bytes < 10 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
    else -> String.format(Locale.US, "%.0f MB", bytes / 1048576.0)
}

/** The reciters, by the names readers use for them. */
fun shortReciterName(id: String, fallback: String): String = when (id) {
    "minshawi" -> "Minshawi"
    "husary" -> "Husary"
    "husary-muallim" -> "Husary Muallim"
    "husary-mujawwad" -> "Husary Mujawwad"
    else -> fallback
}

/**
 * A playback pace as the reader reads it: "1x", "0.75x", never a trailing
 * run of digits. It lives here because the settings page that chooses the
 * pace and the pill that reports it must name it the same way; a reader who
 * chose 0.75x in settings and sees 0.7500001x on the pill has been told two
 * different things about one choice.
 */
fun speedText(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}x" else "${speed}x"

/**
 * The paces a reader may choose, from the slowest to the quickest. One list,
 * because the Listening page and the playing pill offer the same five and set
 * the same value: a pace that appeared in one and not the other would be two
 * answers to one question.
 */
val SpeedSteps = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f)

/**
 * A moment of the day as the reader reads a clock: "6:45 AM", or "06:45" on a
 * phone set to the 24-hour clock, in the interface's own digits.
 *
 * The reader's own 12/24-hour setting decides the shape, because a reminder
 * written in the other shape has to be translated in the head before it can
 * be understood, and the whole point of naming the time is that the reader
 * does not have to think about it. The digits follow the interface's locale,
 * the way every numbered list in the app does, so a Bangla reading reads
 * ৬:৪৫ and not 6:45.
 *
 * [minuteOfDay] is minutes from midnight, the one number the daily reminder
 * keeps, so no caller has to split an hour and a minute and put them back
 * together differently.
 */
@Composable
fun clockText(minuteOfDay: Int): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(minuteOfDay, locale, context) { clockText(minuteOfDay, context, locale) }
}

/** The clock text itself, so a test can ask for one locale without a window. */
internal fun clockText(minuteOfDay: Int, context: android.content.Context, locale: Locale): String {
    val minute = minuteOfDay.coerceIn(0, 24 * 60 - 1)
    val twentyFour = android.text.format.DateFormat.is24HourFormat(context)
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minute / 60)
        set(Calendar.MINUTE, minute % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    // The pattern is written out rather than taken from the locale's short
    // format because the short format drops the leading zero of an hour and
    // the day's first minute, and a time that moves its own digits is harder
    // to read than one that stands still.
    val pattern = if (twentyFour) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, locale).format(calendar.time)
}
