package io.github.muntasimulhaque.quran.ui.kit

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.core.localizedDigits as coreLocalizedDigits
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.displayName
import io.github.muntasimulhaque.quran.data.packName
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
 * 8.3). The pure rule and its own test live in `core`.
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
 * decision, 8.7: the unit used to stay KB and MB in every language,
 * and a reader reading Bengali meets Bengali).
 *
 * The unit words come from the interface's own strings, and the locale is a
 * parameter rather than the process default because a size is drawn in the
 * language the surface is drawn in, which is not always the process default.
 * The composable below is the app's one caller; a test can pin the rule
 * without a window.
 *
 * The units are decimal: one MB is a million bytes, the way Android's own
 * data use and a carrier report it. A reader deciding about cellular data is
 * owed the number the carrier bills, not the binary one under the same name
 * (owner decision, 19.7).
 */
fun formatBytes(bytes: Long, locale: Locale, kb: String, mb: String): String = when {
    bytes <= 0 -> ""
    bytes < 1_000_000 -> String.format(locale, "%d %s", (bytes + 500) / 1000, kb)
    bytes < 10_000_000 -> String.format(locale, "%.1f %s", bytes / 1_000_000.0, mb)
    else -> String.format(locale, "%.0f %s", bytes / 1_000_000.0, mb)
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
 * A pack's name in the interface's own language, read where it is drawn.
 *
 * The names are content: the catalog carries each pack's English name and its
 * Bangla one, and [packName] is the one rule that picks between them. The
 * reciters are packs too, so the pill, the offer, the settings list, the
 * About page, and the reading's footnote line all name a pack through here
 * and a pack added later is named without a line of app code. A composable
 * because the language is the composition's, and the pure rule is tested
 * without a window, the way [speedText] is (owner decision, 20.2).
 */
@Composable
fun contentPackName(pack: ContentPack): String = contentPackName(pack.name, pack.nameBn)

/**
 * [contentPackName] for the two names a surface holds rather than the pack
 * itself: the playback models carry the catalog's pair, because the pill and
 * the offer are built away from the catalog and must still wear the
 * reader's own language (owner decision, 20.2).
 */
@Composable
fun contentPackName(name: String, nameBn: String): String {
    val language = LocalConfiguration.current.locales[0].language
    return remember(name, nameBn, language) { packName(name, nameBn, language) }
}

/**
 * One reciter's pack, the one the catalog ids `reciter-<voice>`. The voice
 * list, the pill, and the settings row all hold a voice id while the catalog
 * holds the pack, so the lookup stands here once and no surface spells the
 * id's shape itself (owner decision, 20.2).
 */
fun List<ContentPack>.reciterPack(voice: String): ContentPack? =
    firstOrNull { it.id == ContentDatabase.reciterPack(voice) }

/**
 * [contentPackName] for a voice a surface knows by id alone, with the name
 * the content carries as the answer when the catalog has no such pack: a
 * voice whose catalog entry is missing is still named rather than blank
 * (owner decision, 20.2).
 */
@Composable
fun reciterName(packs: List<ContentPack>, voice: String, fallback: String): String {
    val pack = packs.reciterPack(voice)
    return if (pack == null) fallback else contentPackName(pack)
}

/**
 * Whether a translation column says which pack it is. It is named when the
 * reader sees more than one, and also when the only column is in a language
 * other than the interface's, so a line the reader did not expect is never
 * left unannounced under the ayah. The reading and the Font size sample ask
 * this one question, so the sample can never preview a column the page would
 * not draw (owner decision, 15.3).
 */
fun translationIsNamed(drawn: Int, language: String, readingLanguage: String): Boolean =
    drawn > 1 || language != readingLanguage

/**
 * A playback pace as the reader reads it: "1x", "0.75x", never a trailing
 * run of digits. It lives here because the settings page that chooses the
 * pace and the pill that reports it must name it the same way; a reader who
 * chose 0.75x in settings and sees 0.7500001x on the pill has been told two
 * different things about one choice. The digits follow the interface's own
 * language, so a Bangla reading reads ০.৭৫x (owner decision, 8.3).
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
