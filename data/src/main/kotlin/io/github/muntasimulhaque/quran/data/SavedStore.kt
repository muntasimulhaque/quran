package io.github.muntasimulhaque.quran.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
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
    /**
     * When the current Save mark was made, or null when the ayah is not
     * saved. Each mark carries its own moment: the row's own `createdAt` is
     * only the first of the two to arrive, so a note written first would
     * otherwise date the save. The Saved list reads newest first by this,
     * and a re-save after an unsave writes a new moment.
     */
    val savedAt: Long? = null,
    /**
     * True when the reader saved the ayah itself. Writing a note does not
     * save it: Save and Note are the reader's two separate actions, so the
     * Saved list holds what was saved and the Notes list holds what was
     * written on, and an ayah the reader did both to appears in both.
     */
    val saved: Boolean = true,
)

/**
 * The reader's own database: one row per ayah they marked, with the two marks
 * an ayah can carry, each with its own moment. Save keeps the ayah; Note
 * keeps the reader's own words about it; neither action performs the other,
 * so a note alone never turns up in Saved. Newest first.
 */
class SavedStore(context: Context) {

    private val helper = Helper(context.applicationContext)
    private val _saved = MutableStateFlow<List<SavedAyah>>(emptyList())
    val saved: StateFlow<List<SavedAyah>> = _saved.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) { refresh() }

    /** Saves the ayah if it was not saved, unsaves it if it was. */
    suspend fun toggle(ayahNumber: Int) = withContext(Dispatchers.IO) {
        val row = row(ayahNumber)
        when {
            row == null -> {
                val now = System.currentTimeMillis()
                write(ayahNumber, note = null, createdAt = now, savedAt = now, saved = true)
            }
            !row.saved -> setSaved(ayahNumber, saved = true, at = System.currentTimeMillis())
            // Unsaving an ayah with a note keeps the note: it belongs to
            // Notes until the reader clears it there.
            row.note != null -> setSaved(ayahNumber, saved = false)
            else -> delete(ayahNumber)
        }
        refresh()
    }

    /**
     * Writes a note, leaving the ayah's saved state exactly as it was; a
     * blank note clears it. A note on an ayah the reader never saved keeps
     * the ayah in Notes alone.
     */
    suspend fun setNote(ayahNumber: Int, note: String?) = withContext(Dispatchers.IO) {
        val clean = note?.trim()?.takeIf { it.isNotEmpty() }
        val row = row(ayahNumber)
        if (row == null) {
            if (clean != null) {
                val now = System.currentTimeMillis()
                write(ayahNumber, clean, createdAt = now, noteAt = now, saved = false)
            }
        } else {
            val values = ContentValues().apply {
                if (clean == null) {
                    putNull(COLUMN_NOTE)
                    putNull(COLUMN_NOTE_AT)
                } else {
                    put(COLUMN_NOTE, clean)
                    put(COLUMN_NOTE_AT, System.currentTimeMillis())
                }
            }
            database().update(TABLE, values, "$COLUMN_AYAH = ?", arrayOf(ayahNumber.toString()))
            // A cleared note on an ayah that was never saved leaves nothing
            // behind; on a saved ayah it leaves the save.
            if (clean == null && !row.saved) delete(ayahNumber)
        }
        refresh()
    }

    /** Removes the Save mark. The ayah's note, if any, stays in Notes. */
    suspend fun unsave(ayahNumber: Int) = withContext(Dispatchers.IO) {
        val row = row(ayahNumber) ?: return@withContext
        if (row.note == null) delete(ayahNumber) else setSaved(ayahNumber, false)
        refresh()
    }

    /** Removes the note. The ayah's save, if any, stays in Saved. */
    suspend fun clearNote(ayahNumber: Int) = withContext(Dispatchers.IO) {
        val row = row(ayahNumber) ?: return@withContext
        if (row.saved) {
            val values = ContentValues().apply {
                putNull(COLUMN_NOTE)
                putNull(COLUMN_NOTE_AT)
            }
            database().update(TABLE, values, "$COLUMN_AYAH = ?", arrayOf(ayahNumber.toString()))
        } else {
            delete(ayahNumber)
        }
        refresh()
    }

    fun close() = helper.close()

    private fun row(ayahNumber: Int): SavedAyah? =
        database().rawQuery(
            "SELECT $COLUMN_AYAH, $COLUMN_NOTE, $COLUMN_CREATED, $COLUMN_NOTE_AT, $COLUMN_SAVED, $COLUMN_SAVED_AT " +
                "FROM $TABLE WHERE $COLUMN_AYAH = ?",
            arrayOf(ayahNumber.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) rowOf(cursor) else null }

    private fun setSaved(ayahNumber: Int, saved: Boolean, at: Long? = null) {
        val values = ContentValues().apply {
            put(COLUMN_SAVED, if (saved) 1 else 0)
            if (saved && at != null) put(COLUMN_SAVED_AT, at) else putNull(COLUMN_SAVED_AT)
        }
        database().update(TABLE, values, "$COLUMN_AYAH = ?", arrayOf(ayahNumber.toString()))
    }

    private fun write(
        ayahNumber: Int,
        note: String?,
        createdAt: Long,
        noteAt: Long? = null,
        saved: Boolean,
        savedAt: Long? = null,
    ) {
        val values = ContentValues().apply {
            put(COLUMN_AYAH, ayahNumber)
            if (note == null) putNull(COLUMN_NOTE) else put(COLUMN_NOTE, note)
            put(COLUMN_CREATED, createdAt)
            if (noteAt == null) putNull(COLUMN_NOTE_AT) else put(COLUMN_NOTE_AT, noteAt)
            put(COLUMN_SAVED, if (saved) 1 else 0)
            if (savedAt == null) putNull(COLUMN_SAVED_AT) else put(COLUMN_SAVED_AT, savedAt)
        }
        database().insert(TABLE, null, values)
    }

    private fun delete(ayahNumber: Int) {
        database().delete(TABLE, "$COLUMN_AYAH = ?", arrayOf(ayahNumber.toString()))
    }

    private fun refresh() {
        _saved.value = database().rawQuery(
            "SELECT $COLUMN_AYAH, $COLUMN_NOTE, $COLUMN_CREATED, $COLUMN_NOTE_AT, $COLUMN_SAVED, $COLUMN_SAVED_AT " +
                "FROM $TABLE ORDER BY $COLUMN_CREATED DESC, $COLUMN_AYAH DESC",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) add(rowOf(cursor))
            }
        }
    }

    private fun rowOf(cursor: Cursor) = SavedAyah(
        ayahNumber = cursor.getInt(0),
        note = if (cursor.isNull(1)) null else cursor.getString(1),
        createdAt = cursor.getLong(2),
        noteAt = if (cursor.isNull(3)) null else cursor.getLong(3),
        saved = cursor.getInt(4) == 1,
        savedAt = if (cursor.isNull(5)) null else cursor.getLong(5),
    )

    private fun database(): SQLiteDatabase = helper.writableDatabase

    private class Helper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE $TABLE (" +
                    "$COLUMN_AYAH INTEGER PRIMARY KEY, " +
                    "$COLUMN_NOTE TEXT, " +
                    "$COLUMN_CREATED INTEGER NOT NULL, " +
                    "$COLUMN_NOTE_AT INTEGER, " +
                    "$COLUMN_SAVED INTEGER NOT NULL DEFAULT 0, " +
                    "$COLUMN_SAVED_AT INTEGER)",
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
            // Version 3 separated Save from Note: until now, writing a note
            // saved the ayah behind it, and every row turned up in Saved. A
            // row whose note carries the row's own moment was written by the
            // note alone (one insert set both), so it leaves Saved; a note
            // written after a save cannot be told from an edited note, and it
            // stays, so nothing the reader saved is lost by the migration.
            if (oldVersion < 3) {
                db.execSQL(
                    "ALTER TABLE $TABLE ADD COLUMN $COLUMN_SAVED INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "UPDATE $TABLE SET $COLUMN_SAVED = 0 WHERE $COLUMN_NOTE IS NOT NULL " +
                        "AND $COLUMN_NOTE_AT IS NOT NULL AND $COLUMN_NOTE_AT = $COLUMN_CREATED",
                )
            }
            // Version 4 gave the Save mark its own moment. Until now the row
            // carried one moment, whichever mark made it; a saved row starts
            // with that moment, the closest true answer left on the device,
            // and a note-only row keeps none.
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COLUMN_SAVED_AT INTEGER")
                db.execSQL(
                    "UPDATE $TABLE SET $COLUMN_SAVED_AT = $COLUMN_CREATED " +
                        "WHERE $COLUMN_SAVED = 1",
                )
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "saved.db"
        const val DATABASE_VERSION = 4
        const val TABLE = "saved"
        const val COLUMN_AYAH = "ayah_number"
        const val COLUMN_NOTE = "note"
        const val COLUMN_CREATED = "created_at"
        const val COLUMN_NOTE_AT = "note_at"
        const val COLUMN_SAVED = "saved"
        const val COLUMN_SAVED_AT = "saved_at"
    }
}
