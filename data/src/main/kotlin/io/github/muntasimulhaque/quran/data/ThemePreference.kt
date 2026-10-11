package io.github.muntasimulhaque.quran.data

import android.content.Context

/**
 * The page the reader reads on, in the one place a launch can read it before
 * any coroutine runs.
 *
 * The settings DataStore is the source of truth for the app's state, but it
 * is asynchronous, and the first frame the reader sees is composed before
 * its read has necessarily landed: without this mirror a Night reader would
 * open on the Paper of the default settings for a frame or two before the
 * stored page arrived. The choice is mirrored here, synchronously, and
 * every write goes through [SettingsStore] so the two can never drift.
 *
 * The theme and its automatic switch are carried together because the page
 * drawn at launch is the stored page resolved against the system's own day
 * and night, and a reader who reads both values here is owed the same answer
 * the app settles on once the DataStore read lands.
 *
 * A stored "black" is the page that was removed and reads as Night
 * here, so an install that chose it lands on the page that replaced it.
 */
class ThemePreference(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences("ui-theme", Context.MODE_PRIVATE)

    /**
     * The stored page, or Paper before any choice has been made. The theme
     * has always had a default, so this never answers null.
     */
    fun theme(): AppTheme = when (preferences.getString(KEY_THEME, null)) {
        "sepia" -> AppTheme.Sepia
        "night", "black" -> AppTheme.Night
        else -> AppTheme.Paper
    }

    /** Whether the page turns over with the system's own day and night. */
    fun autoNight(): Boolean = preferences.getBoolean(KEY_AUTO_NIGHT, false)

    /**
     * The settings as they stood at launch: the page and its switch, read
     * before any coroutine runs, so the first frame is composed on the page
     * the reader last chose. Everything else stays at its default until the
     * DataStore read lands, which costs the reader nothing because the
     * reading itself does not draw until the library is open.
     */
    fun snapshot(): AppSettings = AppSettings(
        theme = theme(),
        autoNight = autoNight(),
    )

    /** Writes the page for the next launch; the in-memory value is immediate. */
    fun setTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.name.lowercase()).apply()
    }

    /** Writes the switch for the next launch; the in-memory value is immediate. */
    fun setAutoNight(follow: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_NIGHT, follow).apply()
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_AUTO_NIGHT = "auto_night"
    }
}
