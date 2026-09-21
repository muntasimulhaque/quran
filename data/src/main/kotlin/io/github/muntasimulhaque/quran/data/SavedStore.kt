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

data class SavedAyah(
    val ayahNumber: Int,
    val note: String?,
    val createdAt: Long,
    /**
     * When the note was last written, or null when there is no note. The notes
     * list reads newest first by this and not by the ayah's own moment: a note
     * written today on an ayah saved last year is today's note.
     */
    val noteAt: Long? = null,
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
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            if (clean == null) {
                putNull(COLUMN_NOTE)
                putNull(COLUMN_NOTE_AT)
            } else {
                put(COLUMN_NOTE, clean)
                put(COLUMN_NOTE_AT, now)
            }
        }
        val updated = database().update(
            TABLE,
            values,
            "$COLUMN_AYAH = ?",
            arrayOf(ayahNumber.toString()),
        )
        if (updated == 0) {
            write(ayahNumber, clean, now, clean?.let { now })
        }
        refresh()
    }

    suspend fun remove(ayahNumber: Int) = withContext(Dispatchers.IO) {
        delete(ayahNumber)
        refresh()
    }

    fun close() = helper.close()

    private fun write(ayahNumber: Int, note: String?, createdAt: Long, noteAt: Long? = null) {
        val values = ContentValues().apply {
            put(COLUMN_AYAH, ayahNumber)
            if (note == null) putNull(COLUMN_NOTE) else put(COLUMN_NOTE, note)
            put(COLUMN_CREATED, createdAt)
            if (noteAt == null) putNull(COLUMN_NOTE_AT) else put(COLUMN_NOTE_AT, noteAt)
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
            "SELECT $COLUMN_AYAH, $COLUMN_NOTE, $COLUMN_CREATED, $COLUMN_NOTE_AT FROM $TABLE " +
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
                            noteAt = if (cursor.isNull(3)) null else cursor.getLong(3),
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
                    "$COLUMN_CREATED INTEGER NOT NULL, " +
                    "$COLUMN_NOTE_AT INTEGER)",
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Version 2 added the note's own moment, so the notes list can read
            // newest first. A note written before it keeps its ayah's own
            // moment, which is the closest true answer left on the device.
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COLUMN_NOTE_AT INTEGER")
                db.execSQL(
                    "UPDATE $TABLE SET $COLUMN_NOTE_AT = $COLUMN_CREATED " +
                        "WHERE $COLUMN_NOTE IS NOT NULL",
                )
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "saved.db"
        const val DATABASE_VERSION = 2
        const val TABLE = "saved"
        const val COLUMN_AYAH = "ayah_number"
        const val COLUMN_NOTE = "note"
        const val COLUMN_CREATED = "created_at"
        const val COLUMN_NOTE_AT = "note_at"
    }
}
