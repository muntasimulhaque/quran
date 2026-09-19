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

/** One place the reader paused: the ayah, the moment, and how they were reading it. */
data class ReadPlace(
    val ayahNumber: Int,
    val mode: ReadingMode,
    val readAt: Long,
)

/**
 * Where the reader has been reading, newest first.
 *
 * The reader's *place* is one thing and lives in the settings: the app opens
 * on it, and it is written whenever they settle on a new ayah. This is the
 * history around that place, so a reader who jumped to Al-Kahf on a Tuesday
 * can find the ayah they were actually reading on the Monday before it.
 *
 * One row per ayah, never two: returning to an ayah moves its visit to the
 * top instead of adding a copy, so the list is places, not a log. It is
 * capped, and one place per sitting is enough: a reader scrolling through a
 * surah passes a hundred ayahs and does not need a hundred rows.
 */
class LastReadStore(context: Context) {

    private val helper = Helper(context.applicationContext)
    private val _places = MutableStateFlow<List<ReadPlace>>(emptyList())
    val places: StateFlow<List<ReadPlace>> = _places.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) { refresh() }

    /** Notes that the reader rested on this ayah, at this moment. */
    suspend fun record(ayahNumber: Int, mode: ReadingMode, at: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            if (ayahNumber !in 1..6236) return@withContext
            database().insertWithOnConflict(
                TABLE,
                null,
                ContentValues().apply {
                    put(COLUMN_AYAH, ayahNumber)
                    put(COLUMN_MODE, if (mode == ReadingMode.Study) MODE_STUDY else MODE_MUSHAF)
                    put(COLUMN_READ_AT, at)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
            // The cap is the tail of the list: the visits nobody will look
            // for. It runs on every write, and it is one small statement.
            database().execSQL(
                "DELETE FROM $TABLE WHERE $COLUMN_AYAH NOT IN (" +
                    "SELECT $COLUMN_AYAH FROM $TABLE ORDER BY $COLUMN_READ_AT DESC LIMIT $CAP)",
            )
            refresh()
        }

    suspend fun remove(ayahNumber: Int) = withContext(Dispatchers.IO) {
        database().delete(TABLE, "$COLUMN_AYAH = ?", arrayOf(ayahNumber.toString()))
        refresh()
    }

    fun close() = helper.close()

    private fun refresh() {
        _places.value = database().rawQuery(
            "SELECT $COLUMN_AYAH, $COLUMN_MODE, $COLUMN_READ_AT FROM $TABLE " +
                "ORDER BY $COLUMN_READ_AT DESC LIMIT $CAP",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        ReadPlace(
                            ayahNumber = cursor.getInt(0),
                            mode = if (cursor.getString(1) == MODE_STUDY) {
                                ReadingMode.Study
                            } else {
                                ReadingMode.Mushaf
                            },
                            readAt = cursor.getLong(2),
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
                    "$COLUMN_MODE TEXT NOT NULL, " +
                    "$COLUMN_READ_AT INTEGER NOT NULL)",
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Version 1 is the first schema; later versions write their steps here.
        }
    }

    private companion object {
        const val DATABASE_NAME = "last-read.db"
        const val DATABASE_VERSION = 1
        const val TABLE = "last_read"
        const val COLUMN_AYAH = "ayah_number"
        const val COLUMN_MODE = "mode"
        const val COLUMN_READ_AT = "read_at"
        const val MODE_STUDY = "study"
        const val MODE_MUSHAF = "mushaf"

        /**
         * How many places are kept: twenty. One sitting is a handful of them,
         * so this is a fortnight of reading for a reader who moves around,
         * and a month for one who reads a surah at a time. It is enough that
         * a place abandoned last week can still be found, and short enough
         * that the list stays a short scroll of recognisable places instead of
         * a log the reader would have to read rather than scan.
         */
        const val CAP = 20
    }
}
