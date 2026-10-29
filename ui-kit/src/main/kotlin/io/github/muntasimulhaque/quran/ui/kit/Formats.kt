package io.github.muntasimulhaque.quran.ui.kit

import android.content.Context
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
 * where it is drawn: a reference ("56:59") and a pace ("0.75x") pass through
 * here, and a size's digits take the same road inside [formatBytes], which
 * formats with the interface's own locale. One language's digits reach every
 * number the reader meets, and no surface can forget one (owner decision,
 * this session). The pure rule and its own test live in `core`.
 */
@Composable
fun localizedDigits(text: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(text, locale) { coreLocalizedDigits(text, locale) }
}

/**
 * Human sizes for a download the reader is about to approve. The digits, the
 * decimal mark, and the unit's own name follow the interface's language, so a
 * Bangla reading reads ২.৮ এমবি while an English one reads 2.8 MB (owner
 * decision, this session: the unit used to stay KB and MB in every language,
 * and a reader reading Bengali meets Bengali).
 *
 * The unit words come from the interface's own strings, and the locale is a
 * parameter rather than the process default because a size is drawn in the
 * language the surface is drawn in, which is not always the process default.
 * The composable below is the app's one caller; a test can pin the rule
 * without a window.
 */
fun formatBytes(bytes: Long, locale: Locale, kb: String, mb: String): String = when {
    bytes <= 0 -> ""
    bytes < 1024 * 1024 -> String.format(locale, "%d %s", (bytes + 512) / 1024, kb)
    bytes < 10 * 1024 * 1024 -> String.format(locale, "%.1f %s", bytes / 1048576.0, mb)
    else -> String.format(locale, "%.0f %s", bytes / 1048576.0, mb)
}

/** [formatBytes] in the interface's own digits and words, read where it is drawn. */
@Composable
fun formatBytes(bytes: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val kb = stringResource(R.string.unit_kb)
    val mb = stringResource(R.string.unit_mb)
    return remember(bytes, locale, kb, mb) { formatBytes(bytes, locale, kb, mb) }
}

/**
 * The reciters, by the names readers use for them, in the reader's own
 * language. It is a composable because the name is a string resource: the
 * pill, the offer, and the settings list draw the reciter, and a Bangla
 * reader used to meet the Latin name in the pill (owner decision, the theme
 * and consistency session).
 *
 * [reciterName] is the same rule for the one caller with no composition, the
 * playback service's own notification, which takes a context wearing the
 * reader's locale (owner decision, this session).
 */
fun reciterName(context: Context, id: String, fallback: String): String = when (id) {
    "minshawi" -> context.getString(R.string.reciter_minshawi)
    "husary" -> context.getString(R.string.reciter_husary)
    else -> fallback
}

/** [reciterName] in the composition's own language, read where it is drawn. */
@Composable
fun reciterShortName(id: String, fallback: String): String {
    val context = LocalContext.current
    return remember(id, fallback, context) { reciterName(context, id, fallback) }
}

/**
 * The names readers use for the app's own packs, in the reader's own language.
 * The catalog is content, and its names are English; the display name is the
 * interface's, so a Bangla reader meets "সহীহ ইন্টারন্যাশনাল", "আল-বায়ান
 * সরল অর্থানুবাদ", and "ইবনে কাসীর" where an English one meets the Latin names. The
 * core text and the reciters are named here too, because the About page
 * credits them from the same catalog and a Bangla reader should not meet
 * "Quran text" or "Husary" on it (owner decision, this session). A pack the
 * app does not know keeps its own catalog name, so a pack added later is
 * never nameless.
 */
@Composable
fun contentPackName(id: String, fallback: String): String = when (id) {
    "core" -> stringResource(R.string.pack_name_quran_text)
    "translation-saheeh-en" -> stringResource(R.string.pack_name_saheeh_international)
    "translation-al-bayan-bn" -> stringResource(R.string.pack_name_al_bayan)
    "tafsir-ibn-kathir-en", "tafsir-ibn-kathir-bn" -> stringResource(R.string.pack_name_ibn_kathir)
    "words-en", "words-bn" -> stringResource(R.string.pack_name_word_by_word)
    "reciter-minshawi" -> stringResource(R.string.reciter_minshawi)
    "reciter-husary" -> stringResource(R.string.reciter_husary)
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
