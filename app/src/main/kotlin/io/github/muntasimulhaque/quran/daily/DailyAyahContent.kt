package io.github.muntasimulhaque.quran.daily

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.muntasimulhaque.quran.MainActivity
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.DailyAyah
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.SettingsStore
import kotlinx.coroutines.flow.first

/**
 * Everything one morning's reminder says: the day's ayah, its place, and its
 * translation when the reader reads with one.
 *
 * The read is deliberately narrow. It opens the same library the reading
 * does, reads exactly one ayah and at most one translation from packs that
 * are already on the device, and closes the library again; nothing is
 * fetched, and nothing is kept. The first enabled translation is the one, the
 * same pack share and search read, so the reminder speaks with the reader's
 * own voice and only one translation ever appears (owner decision, 30).
 */
internal data class DailyAyahContent(
    /** The running number (1..6236), the key the tap and the reading share. */
    val ayah: Int,
    /** The ayah's own number inside its surah, which is what the title says. */
    val ayahInSurah: Int,
    val surah: Int,
    val surahName: String,
    val arabic: String,
    val translation: String?,
) {

    /**
     * The tap that opens this ayah. The extra is read by the activity on
     * cold start; the task is cleared so a tap is always delivered rather
     * than replayed onto the screen the reader left.
     */
    fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_AYAH, ayah)
        }
        return PendingIntent.getActivity(
            context,
            ayah,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /**
         * Reads the day's ayah. Null when the content cannot be opened, which
         * the caller treats as a skipped morning.
         */
        suspend fun load(context: Context, now: Long = System.currentTimeMillis()): DailyAyahContent? {
            val settings = runCatching { SettingsStore(context).settings.first() }.getOrNull()
                ?: return null
            val wanted = DailyAyah.numberFor(now)
            val application = context.applicationContext
            val catalog = PackCatalog.load(application)
            val installed = runCatching { PackStore(application).installed() }.getOrNull()
                ?: return null
            val database = runCatching {
                ContentDatabase.open(application, catalog.withInstalled(installed), installed)
            }.getOrNull() ?: return null
            database.useLanguage(settings.uiLanguage ?: "en")
            return try {
                val ayah = database.ayah(wanted) ?: return null
                val surah = database.surah(ayah.surah)
                val name = surah?.nameSimple
                    ?: context.getString(R.string.surah_fallback_name, ayah.surah)
                DailyAyahContent(
                    ayah = wanted,
                    ayahInSurah = ayah.ayah,
                    surah = ayah.surah,
                    surahName = name,
                    // The reminder is drawn by the system's notification font,
                    // never the Hafs face, so it takes the portable form: the
                    // print's kashida and small letters would be drawn by a
                    // face that was not set for them (owner decision, the
                    // palette session).
                    arabic = Arabic.portableText(ayah.text),
                    translation = firstTranslation(database, settings.translationPacks, wanted),
                )
            } finally {
                database.close()
            }
        }

        /**
         * The first enabled translation's line for this ayah, in plain words:
         * a notification cannot draw a footnote door, so the markers are
         * dropped and the note they belong to is left for the reading.
         */
        private fun firstTranslation(
            database: ContentDatabase,
            enabled: Set<String>,
            ayah: Int,
        ): String? {
            val pack = database.packs()
                .firstOrNull {
                    it.type == PackType.Translation && it.installed && it.id in enabled
                }
                ?.id ?: return null
            val text = database.translations(listOf(ayah), pack)[ayah]?.text ?: return null
            val plain = RichText.plain(text).trim()
            return plain.ifBlank { null }
        }
    }
}
