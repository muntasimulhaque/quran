package io.github.muntasimulhaque.quran.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.playbackPositionStore: DataStore<Preferences> by preferencesDataStore("recitation")

/** Where the recitation was when it stopped: who was reciting, which ayah, and how far into it. */
data class PlaybackPosition(
    val recitation: String,
    val ayahNumber: Int,
    val positionMs: Long,
)

/**
 * The last place the recitation was heard, written down as it is heard.
 *
 * The reading's own place is written on every settled scroll, so the app
 * always opens where the reader stopped reading. The recitation had no such
 * memory: a process that died mid-ayah was answered by the top of that ayah
 * the next time the reader asked to hear it, because the playlist is built
 * fresh from the ayah files on the device and remembers nothing. This store
 * is that memory, and it holds the three things a resume needs and nothing
 * else.
 *
 * It is cleared when the reader stops the recitation, so a position is never
 * offered back to a reader who put it away themselves.
 */
class PlaybackPositionStore(private val context: Context) {

    /** The place the recitation was last heard, or null when it never was. */
    suspend fun read(): PlaybackPosition? {
        val preferences = context.playbackPositionStore.data.first()
        val recitation = preferences[RECITATION] ?: return null
        val ayah = preferences[AYAH] ?: return null
        return PlaybackPosition(recitation, ayah, preferences[POSITION] ?: 0L)
    }

    suspend fun write(position: PlaybackPosition) {
        context.playbackPositionStore.edit {
            it[RECITATION] = position.recitation
            it[AYAH] = position.ayahNumber
            it[POSITION] = position.positionMs
        }
    }

    suspend fun clear() {
        context.playbackPositionStore.edit { it.clear() }
    }

    private companion object {
        val RECITATION = stringPreferencesKey("recitation")
        val AYAH = intPreferencesKey("ayah")
        val POSITION = longPreferencesKey("position")
    }
}
