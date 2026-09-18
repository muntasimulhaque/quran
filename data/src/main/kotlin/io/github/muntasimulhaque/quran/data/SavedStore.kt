package io.github.muntasimulhaque.quran.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SavedAyah(
    val ayahNumber: Int,
    val note: String?,
    val createdAt: Long,
)

/**
 * The reader's own database: one row per saved ayah, with an optional note and
 * the moment it was saved. Hand-rolled SQLite on purpose, so the project stays
 * free of code generation and the whole schema is one small table any session
 * can read. Newest first.
 */
class SavedStore(context: Context) {

    private val helper = Helper(context.applicationContext)
    private val _saved = MutableStateFlow<List<SavedAyah>>(emptyList())
    val saved: StateFlow<List<SavedAyah>> = _saved.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) { refresh() }

    /** Saves the ayah if it was not saved, removes it if it was. */
    suspend fun toggle(ayahNumber: Int) = withContext(Dispatchers.IO) {
        if (exists(ayahNumber)) {
            delete(ayahNumber)
        } else {
            write(ayahNumber, note = null, createdAt = System.currentTimeMillis())
        }
        refresh()
    }

    /** Writes a note, saving the ayah first if needed; a blank note clears it. */
    suspend fun setNote(ayahNumber: Int, note: String?) = withContext(Dispatchers.IO) {
        val clean = note?.trim()?.takeIf { it.isNotEmpty() }
        val values = ContentValues().apply {
            if (clean == null) putNull(COLUMN_NOTE) else put(COLUMN_NOTE, clean)
        }
        val updated = database().update(
            TABLE,
            values,
            "$COLUMN_AYAH = ?",
            arrayOf(ayahNumber.toString()),
        )
        if (updated == 0) {
            write(ayahNumber, clean, System.currentTimeMillis())
        }
        refresh()
    }

    suspend fun remove(ayahNumber: Int) = withContext(Dispatchers.IO) {
        delete(ayahNumber)
        refresh()
    }

    /**
     * The reader's saved ayahs and notes as one small JSON document, so their
     * own work can move to another phone. Nothing leaves the device on its
     * own: this only runs when the reader asks for the file.
     */
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val array = JSONArray()
        for (row in _saved.value.sortedBy { it.ayahNumber }) {
            array.put(
                JSONObject().apply {
                    put(FIELD_AYAH, row.ayahNumber)
                    if (row.note != null) put(FIELD_NOTE, row.note)
                    put(FIELD_CREATED, row.createdAt)
                },
            )
        }
        JSONObject()
            .put(FIELD_FORMAT, FORMAT)
            .put(FIELD_VERSION, FORMAT_VERSION)
            .put(FIELD_SAVED, array)
            .toString(2)
    }

    /**
     * Merges a document written by [exportJson]: an ayah that is already here
     * keeps its own note, and a date in the future is brought back to now so
     * one bad file cannot pin itself to the top of the list forever.
     */
    suspend fun importJson(json: String): Result<Int> = withContext(Dispatchers.IO) {
        val root = runCatching { JSONObject(json) }.getOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("not a saved ayahs file"))
        if (root.optString(FIELD_FORMAT) != FORMAT) {
            return@withContext Result.failure(IllegalArgumentException("not a saved ayahs file"))
        }
        val rows = root.optJSONArray(FIELD_SAVED)
            ?: return@withContext Result.failure(IllegalArgumentException("no saved ayahs"))
        val now = System.currentTimeMillis()
        var added = 0
        for (index in 0 until rows.length()) {
            val entry = rows.optJSONObject(index) ?: continue
            val number = entry.optInt(FIELD_AYAH, 0)
            if (number !in 1..6236) continue
            val note = entry.optString(FIELD_NOTE).trim().take(MAX_NOTE_LENGTH).takeIf { it.isNotEmpty() }
            val created = entry.optLong(FIELD_CREATED, now).coerceIn(0L, now)
            if (exists(number)) {
                if (note != null && currentNote(number) == null) {
                    val values = ContentValues().apply { put(COLUMN_NOTE, note) }
                    database().update(TABLE, values, "$COLUMN_AYAH = ?", arrayOf(number.toString()))
                }
                continue
            }
            write(number, note, created)
            added++
        }
        refresh()
        Result.success(added)
    }

    private fun currentNote(ayahNumber: Int): String? =
        database().rawQuery(
            "SELECT $COLUMN_NOTE FROM $TABLE WHERE $COLUMN_AYAH = ?",
            arrayOf(ayahNumber.toString()),
        ).use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null }

    fun close() = helper.close()

    private fun write(ayahNumber: Int, note: String?, createdAt: Long) {
        val values = ContentValues().apply {
            put(COLUMN_AYAH, ayahNumber)
            if (note == null) putNull(COLUMN_NOTE) else put(COLUMN_NOTE, note)
            put(COLUMN_CREATED, createdAt)
        }
        database().insert(TABLE, null, values)
    }

    private fun delete(ayahNumber: Int) {
        database().delete(TABLE, "$COLUMN_AYAH = ?", arrayOf(ayahNumber.toString()))
    }

    private fun exists(ayahNumber: Int): Boolean =
        database().rawQuery(
            "SELECT 1 FROM $TABLE WHERE $COLUMN_AYAH = ?",
            arrayOf(ayahNumber.toString()),
        ).use { cursor -> cursor.moveToFirst() }

    private fun refresh() {
        _saved.value = database().rawQuery(
            "SELECT $COLUMN_AYAH, $COLUMN_NOTE, $COLUMN_CREATED FROM $TABLE " +
                "ORDER BY $COLUMN_CREATED DESC, $COLUMN_AYAH DESC",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        SavedAyah(
                            ayahNumber = cursor.getInt(0),
                            note = if (cursor.isNull(1)) null else cursor.getString(1),
                            createdAt = cursor.getLong(2),
                        ),
                    )
                }
            }
        }
    }

    private fun database(): SQLiteDatabase = helper.writableDatabase

    private class Helper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE $TABLE (" +
                    "$COLUMN_AYAH INTEGER PRIMARY KEY, " +
                    "$COLUMN_NOTE TEXT, " +
                    "$COLUMN_CREATED INTEGER NOT NULL)",
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Version 1 is the first schema; later versions write their steps here.
        }
    }

    private companion object {
        const val DATABASE_NAME = "saved.db"
        const val DATABASE_VERSION = 1
        const val TABLE = "saved"
        const val COLUMN_AYAH = "ayah_number"
        const val COLUMN_NOTE = "note"
        const val COLUMN_CREATED = "created_at"
        const val FIELD_FORMAT = "format"
        const val FIELD_VERSION = "version"
        const val FIELD_SAVED = "saved"
        const val FIELD_AYAH = "ayah"
        const val FIELD_NOTE = "note"
        const val FIELD_CREATED = "createdAt"
        const val FORMAT = "quran-saved-ayahs"
        const val FORMAT_VERSION = 1
        const val MAX_NOTE_LENGTH = 10_000
    }
}
