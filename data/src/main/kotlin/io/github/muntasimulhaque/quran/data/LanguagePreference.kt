package io.github.muntasimulhaque.quran.data

import android.content.Context

/**
 * The interface language in the one place a launch can read it before any
 * coroutine runs.
 *
 * The settings DataStore is the source of truth for the app's state, but it
 * is asynchronous, and the Activity must already carry the reader's locale in
 * `attachBaseContext` or the first frame comes up in the system's language.
 * The same tag is mirrored here, synchronously, and every write goes through
 * [SettingsStore] so the two can never drift.
 */
class LanguagePreference(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences("ui-language", Context.MODE_PRIVATE)

    /** The stored tag, or null before the reader has chosen. */
    fun tag(): String? = preferences.getString(KEY, null)

    /** Writes the tag for the next launch; the in-memory value is immediate. */
    fun set(tag: String) {
        preferences.edit().putString(KEY, tag).apply()
    }

    private companion object {
        const val KEY = "tag"
    }
}

