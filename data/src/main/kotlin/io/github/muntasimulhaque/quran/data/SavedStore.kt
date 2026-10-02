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
     * True when the ayah is in Saved. A note is written on a kept ayah, so
     * writing one keeps the ayah too: the reader's own words and the ayah
     * they are about belong together, and the Saved list is the one list
     * that holds both. Removing the save removes the note with it, which the
     * screen asks about first when a note exists.
     */
    val saved: Boolean = true,
    /**
     * True when the reader pressed Save. A note keeps the ayah, and the keep
     * it makes is not the Save mark: the pill lights the note the reader
     * wrote and the bookmark the reader asked for, and one tap of a note
     * never claims to be the other (owner report). It is this mark, and not
     * [saved], that says the ayah may be unsaved: a note alone is the
     * reader's own writing and only their own hand takes it away.
     */
    val marked: Boolean = false,
)

/**
 * The reader's own database: one row per ayah they kept, with the note they
 * wrote on it when there is one, each mark carrying its own moment. A note
 * is written on a kept ayah, so a note alone keeps the ayah too; removing
 * the save removes the note, and the screen asks before that when a note is
 * there. Newest first.
 *
 * The row keeps the two marks apart, because the pill does: a note brings
 * the ayah into Saved without ever being the reader's Save, so [SavedAyah.marked]
 * records the tap of the Save action and nothing else writes it.
 */
class SavedStore(context: Context) {

    private val helper = Helper(context.applicationContext)
    private val _saved = MutableStateFlow<List<SavedAyah>>(emptyList())
    val saved: StateFlow<List<SavedAyah>> = _saved.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) { refresh() }

    /**
     * The reader's own Save: the mark is made on an unmarked ayah, and taken
     * back off a marked one. An ayah that only a note brought into Saved is
     * unmarked, so one tap of Save marks it rather than asking to take away a
     * keep the reader never made, and the question is asked only where there
     * is a mark of their own to remove (owner report).
     */
    suspend fun toggle(ayahNumber: Int) = withContext(Dispatchers.IO) {
        val row = row(ayahNumber)
        when {
            row == null -> {
                val now = System.currentTimeMillis()
                write(
                    ayahNumber = ayahNumber,
                    note = null,
                    createdAt = now,
                    savedAt = now,
                    saved = true,
                    marked = true,
                )
            }
            !row.marked -> setMark(ayahNumber, at = System.currentTimeMillis())
            // Removing the save removes the row, and the note written on it
            // with it. The screen asks before this when a note exists, so
            // the reader's own words are never lost to one tap.
            else -> delete(ayahNumber)
        }
        refresh()
    }

    /**
     * Writes a note, and keeps the ayah with it: a note is written on a kept
     * ayah, so writing one keeps the ayah in Saved. It is not the reader's
     * Save, and it never becomes one: a note written on a saved ayah leaves
     * the mark where the reader put it.
     *
     * A blank note clears the note, and the keep the note made goes with it: a
     * row the reader never pressed Save on has nothing else holding it in
     * Saved, and a keep that outlived the words it was made for is a row no
     * mark and no note can explain. A save the reader did press stays, and so
     * does everything they wrote about it.
     */
    suspend fun setNote(ayahNumber: Int, note: String?) = withContext(Dispatchers.IO) {
        val clean = note?.trim()?.takeIf { it.isNotEmpty() }
        val row = row(ayahNumber)
        if (row == null) {
            if (clean != null) {
                val now = System.currentTimeMillis()
                // One insert, both moments, and no mark: the note is what
                // brought the ayah in, and the pill says so (owner report).
                write(
                    ayahNumber = ayahNumber,
                    note = clean,
                    createdAt = now,
                    noteAt = now,
                    saved = true,
                    savedAt = now,
                )
            }
        } else {
            val now = System.currentTimeMillis()
            val values = ContentValues().apply {
                if (clean == null) {
                    putNull(COLUMN_NOTE)
                    putNull(COLUMN_NOTE_AT)
                } else {
                    put(COLUMN_NOTE, clean)
                    put(COLUMN_NOTE_AT, now)
                    // A note keeps the ayah. A row from before this rule was
                    // only a note; it becomes a keep with this note's moment,
                    // the closest true answer for when it was kept, and
                    // without a mark, because no Save was ever pressed.
                    if (!row.saved) {
                        put(COLUMN_SAVED, 1)
                        put(COLUMN_SAVED_AT, now)
                    }
                }
            }
            database().update(TABLE, values, "$COLUMN_AYAH = ?", arrayOf(ayahNumber.toString()))
            // A cleared note takes the keep it made with it, and nothing else:
            // the row goes where no Save was pressed, and stays where one was.
            if (clean == null && !row.marked) delete(ayahNumber)
        }
        refresh()
    }

    /**
     * Removes the ayah from Saved, and the note written on it with it. The
     * screen asks before this when a note exists.
     */
    suspend fun unsave(ayahNumber: Int) = withContext(Dispatchers.IO) {
        delete(ayahNumber)
        refresh()
    }

    fun close() = helper.close()

    private fun row(ayahNumber: Int): SavedAyah? =
        database().rawQuery(
            "SELECT $COLUMN_AYAH, $COLUMN_NOTE, $COLUMN_CREATED, $COLUMN_NOTE_AT, " +
                "$COLUMN_SAVED, $COLUMN_SAVED_AT, $COLUMN_MARKED " +
                "FROM $TABLE WHERE $COLUMN_AYAH = ?",
            arrayOf(ayahNumber.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) rowOf(cursor) else null }

    /** The reader's own mark, made now: the ayah is kept, and the mark says so. */
    private fun setMark(ayahNumber: Int, at: Long) {
        val values = ContentValues().apply {
            put(COLUMN_SAVED, 1)
            put(COLUMN_SAVED_AT, at)
            put(COLUMN_MARKED, 1)
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
        marked: Boolean = false,
    ) {
        val values = ContentValues().apply {
            put(COLUMN_AYAH, ayahNumber)
            if (note == null) putNull(COLUMN_NOTE) else put(COLUMN_NOTE, note)
            put(COLUMN_CREATED, createdAt)
            if (noteAt == null) putNull(COLUMN_NOTE_AT) else put(COLUMN_NOTE_AT, noteAt)
            put(COLUMN_SAVED, if (saved) 1 else 0)
            if (savedAt == null) putNull(COLUMN_SAVED_AT) else put(COLUMN_SAVED_AT, savedAt)
            put(COLUMN_MARKED, if (marked) 1 else 0)
        }
        database().insert(TABLE, null, values)
    }

    private fun delete(ayahNumber: Int) {
        database().delete(TABLE, "$COLUMN_AYAH = ?", arrayOf(ayahNumber.toString()))
    }

    private fun refresh() {
        _saved.value = database().rawQuery(
            "SELECT $COLUMN_AYAH, $COLUMN_NOTE, $COLUMN_CREATED, $COLUMN_NOTE_AT, " +
                "$COLUMN_SAVED, $COLUMN_SAVED_AT, $COLUMN_MARKED " +
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
        marked = cursor.getInt(6) == 1,
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
                    "$COLUMN_SAVED_AT INTEGER, " +
                    "$COLUMN_MARKED INTEGER NOT NULL DEFAULT 0)",
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
            // Version 5 makes a note a thing written on a kept ayah. A row
            // that was only a note enters Saved, carrying the note's own
            // moment as when it was kept, so no reader loses a note to the
            // merge.
            if (oldVersion < 5) {
                db.execSQL(
                    "UPDATE $TABLE SET $COLUMN_SAVED = 1, " +
                        "$COLUMN_SAVED_AT = COALESCE($COLUMN_NOTE_AT, $COLUMN_CREATED) " +
                        "WHERE $COLUMN_NOTE IS NOT NULL AND $COLUMN_SAVED = 0",
                )
            }
            // Version 6 kept Save and Note as two marks. A note brings its
            // ayah into Saved, and until now that keep could not be told from
            // the reader's own Save, so the pill lit both on a note written
            // alone (owner report). The mark the reader made is the one whose
            // keep is older than the note on it, and a row with no note at all
            // was only ever saved by hand: those two are the rows the column
            // can honestly say yes to. A keep that is not older than its note
            // was written in the same moment as it, or dated by the
            // migrations above, so it stays unmarked rather than claim a tap
            // the app cannot prove.
            if (oldVersion < 6) {
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COLUMN_MARKED INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE $TABLE SET $COLUMN_MARKED = 1 WHERE $COLUMN_NOTE IS NULL " +
                        "OR ($COLUMN_SAVED_AT IS NOT NULL AND $COLUMN_NOTE_AT IS NOT NULL " +
                        "AND $COLUMN_SAVED_AT < $COLUMN_NOTE_AT)",
                )
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "saved.db"
        const val DATABASE_VERSION = 6
        const val TABLE = "saved"
        const val COLUMN_AYAH = "ayah_number"
        const val COLUMN_NOTE = "note"
        const val COLUMN_CREATED = "created_at"
        const val COLUMN_NOTE_AT = "note_at"
        const val COLUMN_SAVED = "saved"
        const val COLUMN_SAVED_AT = "saved_at"
        const val COLUMN_MARKED = "marked"
    }
}
