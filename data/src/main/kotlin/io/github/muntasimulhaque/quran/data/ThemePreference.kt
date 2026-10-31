package io.github.muntasimulhaque.quran.data

import android.content.Context

/**
 * The page the reader reads on, in the one place a launch can read it before
 * any coroutine runs.
 *
 * The settings DataStore is the source of truth for the app's state, but it
 * is asynchronous, and the first frame the reader sees is composed before
 * its read has necessarily landed: without this mirror a Dark reader would
 * open on the Light of the default settings for a frame or two before the
 * stored page arrived. The choice is mirrored here, synchronously, and
 * every write goes through [SettingsStore] so the two can never drift.
 *
 * The theme is one of three values now, System among them, and the page
 * drawn at launch is that value resolved against the system's own day and
 * night, which the caller reads where the configuration is known. A stored
 * pair from before the third value (a ground and an automatic switch) is
 * read the way [SettingsStore] reads it: the switch on is System, the
 * switch off is the ground, the old Paper as Light and Night as Dark, and
 * the removed "sepia" and "black" as the ground that replaced them.
 */
class ThemePreference(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences("ui-theme", Context.MODE_PRIVATE)

    /**
     * The stored page, or System before any choice has been made. The theme
     * has always had a default, so this never answers null: a fresh install
     * follows the phone's own day and night until the reader picks a ground
     * (owner decision, this session).
     */
    fun theme(): AppTheme {
        val auto = preferences.getBoolean(KEY_AUTO_NIGHT, false)
        return when (preferences.getString(KEY_THEME, null)) {
            null -> AppTheme.System
            "system" -> AppTheme.System
            "light", "paper", "sepia" -> if (auto) AppTheme.System else AppTheme.Light
            "dark", "night", "black" -> if (auto) AppTheme.System else AppTheme.Dark
            else -> if (auto) AppTheme.System else AppTheme.Light
        }
    }

    /**
     * The settings as they stood at launch: the page, read before any
     * coroutine runs, so the first frame is composed on the page the reader
     * last chose. Everything else stays at its default until the DataStore
     * read lands, which costs the reader nothing because the reading itself
     * does not draw until the library is open.
     */
    fun snapshot(): AppSettings = AppSettings(theme = theme())

    /** Writes the page for the next launch; the in-memory value is immediate. */
    fun setTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.name.lowercase()).apply()
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_AUTO_NIGHT = "auto_night"
    }
}
