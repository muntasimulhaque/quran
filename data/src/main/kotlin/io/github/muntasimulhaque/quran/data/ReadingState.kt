package io.github.muntasimulhaque.quran.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readingStore: DataStore<Preferences> by preferencesDataStore("reading")

enum class ReadingMode { Mushaf, Study }

data class ReadingSnapshot(
    val page: Int,
    val mode: ReadingMode,
)

/**
 * Remembers the one thing the app must never forget: where the reader was.
 * Page first, mode second, nothing else decided before reading.
 */
class ReadingState(private val context: Context) {

    val snapshot: Flow<ReadingSnapshot> = context.readingStore.data.map { preferences ->
        ReadingSnapshot(
            page = preferences[PAGE] ?: 1,
            mode = when (preferences[MODE]) {
                "study" -> ReadingMode.Study
                else -> ReadingMode.Mushaf
            },
        )
    }

    suspend fun setPage(page: Int) {
        context.readingStore.edit { it[PAGE] = page.coerceIn(1, 604) }
    }

    suspend fun setMode(mode: ReadingMode) {
        context.readingStore.edit { it[MODE] = if (mode == ReadingMode.Study) "study" else "mushaf" }
    }

    private companion object {
        val PAGE = intPreferencesKey("page")
        val MODE = stringPreferencesKey("mode")
    }
}
