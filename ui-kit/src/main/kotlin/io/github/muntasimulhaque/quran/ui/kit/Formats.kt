package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.core.localizedDigits as coreLocalizedDigits
import io.github.muntasimulhaque.quran.uikit.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * A string with its ASCII digits written in the interface's own digits, read
 * where it is drawn: a reference ("56:59"), a size ("2.8 MB"), and a pace
 * ("0.75x") all pass through here, so one language's digits reach every
 * number the reader meets and no surface can forget one (owner decision,
 * this session). The pure rule and its own test live in `core`.
 */
@Composable
fun localizedDigits(text: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(text, locale) { coreLocalizedDigits(text, locale) }
}

/**
 * Human sizes for a download the reader is about to approve. The digits and
 * the decimal mark follow the interface's own language, so a Bangla reading
 * reads ২.৮ MB while an English one reads 2.8 MB. The unit's letters do not
 * change: KB and MB are the same unit in every language, and a translated
 * unit would be a second name for one fact.
 *
 * The locale is a parameter rather than the process default because a size is
 * also drawn outside an Activity, where the reader's own language has not been
 * written onto the default locale (owner decision, this session).
 */
fun formatBytes(bytes: Long, locale: Locale): String = when {
    bytes <= 0 -> ""
    bytes < 1024 * 1024 -> String.format(locale, "%d KB", (bytes + 512) / 1024)
    bytes < 10 * 1024 * 1024 -> String.format(locale, "%.1f MB", bytes / 1048576.0)
    else -> String.format(locale, "%.0f MB", bytes / 1048576.0)
}

/** [formatBytes] in the interface's own digits, read where it is drawn. */
@Composable
fun formatBytes(bytes: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(bytes, locale) { formatBytes(bytes, locale) }
}

/**
 * The reciters, by the names readers use for them, in the reader's own
 * language. It is a composable because the name is a string resource: the
 * pill, the offer, and the settings list draw the reciter, and a Bangla
 * reader used to meet the Latin name in the pill (owner decision, the theme
 * and consistency session).
 */
@Composable
fun reciterShortName(id: String, fallback: String): String = when (id) {
    "minshawi" -> stringResource(R.string.reciter_minshawi)
    "husary" -> stringResource(R.string.reciter_husary)
    "husary-muallim" -> stringResource(R.string.reciter_husary_muallim)
    "husary-mujawwad" -> stringResource(R.string.reciter_husary_mujawwad)
    else -> fallback
}

/**
 * The names readers use for the app's own packs, in the reader's own language.
 * The catalog is content, and its names are English; the display name is the
 * interface's, so a Bangla reader meets "সহীহ ইন্টারন্যাশনাল", "তাইসিরুল
 * কুরআন", and "ইবনে কাসীর" where an English one meets the Latin names. A pack
 * the app does not know keeps its own catalog name, so a pack added later is
 * never nameless (owner decision, this session).
 */
@Composable
fun contentPackName(id: String, fallback: String): String = when (id) {
    "translation-saheeh-en" -> stringResource(R.string.pack_name_saheeh_international)
    "translation-taisirul-quran-bn" -> stringResource(R.string.pack_name_taisirul_quran)
    "tafsir-ibn-kathir-en", "tafsir-ibn-kathir-bn" -> stringResource(R.string.pack_name_ibn_kathir)
    "words-en", "words-bn" -> stringResource(R.string.pack_name_word_by_word)
    else -> fallback
}

/**
 * A playback pace as the reader reads it: "1x", "0.75x", never a trailing
 * run of digits. It lives here because the settings page that chooses the
 * pace and the pill that reports it must name it the same way; a reader who
 * chose 0.75x in settings and sees 0.7500001x on the pill has been told two
 * different things about one choice. The digits follow the interface's own
 * language, so a Bangla reading reads ০.৭৫x (owner decision, this session).
 */
fun speedText(speed: Float, locale: Locale): String {
    val plain = if (speed == speed.toInt().toFloat()) "${speed.toInt()}x" else "${speed}x"
    return coreLocalizedDigits(plain, locale)
}

/** [speedText] in the interface's own digits, read where it is drawn. */
@Composable
fun speedText(speed: Float): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(speed, locale) { speedText(speed, locale) }
}

/**
 * The paces a reader may choose, from the slowest to the quickest. One list,
 * because the pill's listening menu offers the five and the player keeps the
 * value they set: a pace that appeared in the menu and not in playback would
 * be two answers to one question.
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
