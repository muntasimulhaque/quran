package io.github.muntasimulhaque.quran.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * One reading the reader left, not one ayah: the surah it was in, the ayah
 * they were last at, the stretch of the surah the reading covers, and the
 * moment. The covered stretch is what tells a new visit whether it is the
 * same reading carried on or a new reading started.
 */
data class ReadPlace(
    val id: Long,
    val surah: Int,
    /** The ayah the reader was last at: the row shows and opens this one. */
    val ayahNumber: Int,
    /** The lowest ayah the reading covers. */
    val lowAyah: Int,
    /** The highest ayah the reading covers. */
    val highAyah: Int,
    val readAt: Long,
)

/**
 * Where the reader has been reading, one row per reading, newest first.
 *
 * The reader's place lives in the settings: the app opens on it. This is the
 * history around it.
 *
 * A reading is a stretch of one surah the reader moved through, and the two
 * ways a place is written say which way the reader meant it:
 *
 * - [extend] is reading on. The ayah the reader settled on joins the reading
 *   they are already in, wherever the scroll carried them, even a fling
 *   across the surah. Continuing from where they left off, one session or
 *   ten sessions later, keeps moving the same row, so An-Nahl 11 to 20 is
 *   one row at 20 and nothing before it is repeated.
 * - [record] is a deliberate going-to: a grid jump, a search result, a
 *   reference, the daily ayah, or the app opening on the stored place. It
 *   joins a reading it lands inside, or the ayah right beside one, because
 *   arriving at the next ayah after a reading is carrying on rather than a
 *   new start (owner decision). Anywhere else it starts a new reading, so a
 *   reader who has read Baqarah 34 to 120 and then reads Baqarah 255 keeps
 *   both places: the 34 to 120 reading and the 255 reading.
 *
 * The row shows the latest ayah alone, the place the reader was last at
 * (owner decision). The covered stretch is never shown; it exists only to
 * decide whether the next visit is the same reading.
 *
 * The list is capped, so a reader who moves around the Book keeps the recent
 * readings and not a log that grows without end.
 */
class LastReadStore internal constructor(
    private val helper: SQLiteOpenHelper,
) {

    constructor(context: Context) : this(Helper(context.applicationContext))

    private val _places = MutableStateFlow<List<ReadPlace>>(emptyList())
    val places: StateFlow<List<ReadPlace>> = _places.asStateFlow()

    /** One writer at a time: a settle and a jump can land together. */
    private val writes = Mutex()

    /**
     * A full disk or a damaged database is a message, never a crash. The
     * reader's place is written by the reading itself, on a settle and on a
     * jump, and a database that cannot take the row must not take the page
     * with it. Cancellation is the one exception: a cancelled write is the
     * caller's own decision and travels as itself (owner decision, 11.1).
     */
    private suspend fun <T> guarded(where: String, block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.e("LastReadStore", where, error)
        null
    }

    suspend fun load() {
        withContext(Dispatchers.IO) {
            guarded("load") { writes.withLock { refresh() } }
        }
    }

    /**
     * Reading on: the ayah the reader settled on joins the reading they are
     * in. [activeId] is the reading the view model believes they are in; when
     * it does not name a reading of this surah, the ayah is matched to a
     * reading the way [record] matches one, and starts a new reading when
     * there is none.
     */
    suspend fun extend(
        ayahNumber: Int,
        surah: Int,
        activeId: Long?,
        at: Long = System.currentTimeMillis(),
    ): Long? = withContext(Dispatchers.IO) {
        guarded("extend") {
            writes.withLock {
                if (ayahNumber !in AYAH_RANGE) return@withLock null
                val rows = readRows()
                val active = rows.firstOrNull { it.id == activeId && it.surah == surah }
                val place = active ?: foldTarget(rows, surah, ayahNumber)
                if (place == null) {
                    insert(surah, ayahNumber, at)
                } else {
                    update(place, ayahNumber, at)
                    place.id
                }
            }
        }
    }

    /**
     * A deliberate going-to: a jump, or the app opening on the stored place.
     * The visit joins a reading it lands inside or right beside, and starts
     * a new reading anywhere else.
     */
    suspend fun record(
        ayahNumber: Int,
        surah: Int,
        at: Long = System.currentTimeMillis(),
    ): Long? = withContext(Dispatchers.IO) {
        guarded("record") {
            writes.withLock {
                if (ayahNumber !in AYAH_RANGE) return@withLock null
                val rows = readRows()
                val place = foldTarget(rows, surah, ayahNumber)
                if (place == null) {
                    insert(surah, ayahNumber, at)
                } else {
                    update(place, ayahNumber, at)
                    place.id
                }
            }
        }
    }

    suspend fun remove(ayahNumber: Int) {
        withContext(Dispatchers.IO) {
            guarded("remove") {
                writes.withLock {
                    database().delete(TABLE, "$COLUMN_AYAH = ?", arrayOf(ayahNumber.toString()))
                    refresh()
                }
            }
        }
    }

    fun close() = helper.close()

    /**
     * The reading a visit belongs to: one whose stretch covers the ayah, or
     * the one whose continuation edge it lands on. The covered stretch is
     * read as it stands, so a reading that ran 34 to 120 then jumped back to
     * 100 still covers 120. When two readings both touch the ayah, the one
     * the reader was in most recently is the one meant.
     */
    private fun foldTarget(rows: List<ReadPlace>, surah: Int, target: Int): ReadPlace? {
        val sameSurah = rows.filter { it.surah == surah }
        return sameSurah.firstOrNull { target in it.lowAyah..it.highAyah }
            ?: sameSurah
                .filter { target == it.lowAyah - 1 || target == it.highAyah + 1 }
                .maxByOrNull { it.readAt }
    }

    private fun insert(surah: Int, ayahNumber: Int, at: Long): Long {
        val id = database().insert(
            TABLE,
            null,
            ContentValues().apply {
                put(COLUMN_SURAH, surah)
                put(COLUMN_AYAH, ayahNumber)
                put(COLUMN_LOW, ayahNumber)
                put(COLUMN_HIGH, ayahNumber)
                put(COLUMN_READ_AT, at)
            },
        )
        cap()
        refresh()
        return id
    }

    private fun update(place: ReadPlace, ayahNumber: Int, at: Long) {
        database().update(
            TABLE,
            ContentValues().apply {
                put(COLUMN_AYAH, ayahNumber)
                put(COLUMN_LOW, minOf(place.lowAyah, ayahNumber))
                put(COLUMN_HIGH, maxOf(place.highAyah, ayahNumber))
                put(COLUMN_READ_AT, at)
            },
            "$COLUMN_ID = ?",
            arrayOf(place.id.toString()),
        )
        cap()
        refresh()
    }

    /** The cap is the tail of the list: the readings nobody will look for. */
    private fun cap() {
        database().execSQL(
            "DELETE FROM $TABLE WHERE $COLUMN_ID NOT IN (" +
                "SELECT $COLUMN_ID FROM $TABLE ORDER BY $COLUMN_READ_AT DESC LIMIT $CAP)",
        )
    }

    private fun refresh() {
        _places.value = readRows().take(CAP)
    }

    private fun readRows(): List<ReadPlace> =
        database().rawQuery(
            "SELECT $COLUMN_ID, $COLUMN_SURAH, $COLUMN_AYAH, $COLUMN_LOW, $COLUMN_HIGH, " +
                "$COLUMN_READ_AT FROM $TABLE ORDER BY $COLUMN_READ_AT DESC",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        ReadPlace(
                            id = cursor.getLong(0),
                            surah = cursor.getInt(1),
                            ayahNumber = cursor.getInt(2),
                            lowAyah = cursor.getInt(3),
                            highAyah = cursor.getInt(4),
                            readAt = cursor.getLong(5),
                        ),
                    )
                }
            }
        }

    private fun database(): SQLiteDatabase = helper.writableDatabase

    private class Helper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE $TABLE (" +
                    "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_SURAH INTEGER NOT NULL, " +
                    "$COLUMN_AYAH INTEGER NOT NULL, " +
                    "$COLUMN_LOW INTEGER NOT NULL, " +
                    "$COLUMN_HIGH INTEGER NOT NULL, " +
                    "$COLUMN_READ_AT INTEGER NOT NULL)",
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // The list used to be one row per ayah, and there is no reader to
            // carry across: the app has not shipped, and the old rows cannot
            // be rebuilt into the readings they never recorded. The table is
            // made new, so the first reading after the update is the first
            // row in it (owner decision, this session).
            db.execSQL("DROP TABLE IF EXISTS $TABLE")
            onCreate(db)
        }
    }

    private companion object {
        const val DATABASE_NAME = "last-read.db"
        const val DATABASE_VERSION = 3
        const val TABLE = "last_read"

        const val COLUMN_ID = "id"
        const val COLUMN_SURAH = "surah"
        const val COLUMN_AYAH = "ayah"
        const val COLUMN_LOW = "low_ayah"
        const val COLUMN_HIGH = "high_ayah"
        const val COLUMN_READ_AT = "read_at"

        /**
         * How many readings are kept: twenty. One sitting is a handful of
         * them, so this is a fortnight of reading for a reader who moves
         * around, and a month for one who reads a surah at a time. It is
         * enough that a reading abandoned last week can still be found, and
         * short enough that the list stays a short scroll of recognisable
         * places instead of a log the reader would have to read rather than
         * scan.
         */
        const val CAP = 20

        val AYAH_RANGE = 1..6236
    }
}
