package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteFullException
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Last Read is the history around the reader's place, one row per reading
 * rather than one row per ayah, so its behavior is pinned: reading on moves
 * the reading the reader is in, a deliberate jump joins a reading it lands in
 * or beside and starts one anywhere else, the row shows the latest ayah
 * alone, and the list stays a short list of readings rather than a log that
 * grows without end.
 */
@RunWith(AndroidJUnit4::class)
class LastReadStoreTest {

    private lateinit var context: Context
    private lateinit var store: LastReadStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("last-read.db")
        store = LastReadStore(context)
    }

    @After
    fun tearDown() {
        store.close()
        context.deleteDatabase("last-read.db")
    }

    @Test
    fun readingOnKeepsOneReadingAndMovesIt() = runBlocking {
        store.load()
        val id = store.extend(34, surah = 2, activeId = null, at = 1_000)
        store.extend(35, surah = 2, activeId = id, at = 2_000)
        store.extend(120, surah = 2, activeId = id, at = 3_000)
        val place = store.places.value.single()
        assertEquals(120, place.ayahNumber)
        assertEquals(34, place.lowAyah)
        assertEquals(120, place.highAyah)
    }

    @Test
    fun aJumpInsideAReadingKeepsItOneRow() = runBlocking {
        val id = store.extend(34, surah = 2, activeId = null, at = 1_000)
        store.extend(120, surah = 2, activeId = id, at = 2_000)
        store.record(100, surah = 2, at = 3_000)
        assertEquals(1, store.places.value.size)
        assertEquals(100, store.places.value.single().ayahNumber)
        assertEquals(34, store.places.value.single().lowAyah)
        assertEquals(120, store.places.value.single().highAyah)
        assertEquals(id, store.places.value.single().id)
    }

    @Test
    fun aJumpToTheReadingContinuationEdgeContinuesIt() = runBlocking {
        val id = store.extend(34, surah = 2, activeId = null, at = 1_000)
        store.extend(120, surah = 2, activeId = id, at = 2_000)
        store.record(121, surah = 2, at = 3_000)
        val place = store.places.value.single()
        assertEquals(121, place.ayahNumber)
        assertEquals(34, place.lowAyah)
        assertEquals(121, place.highAyah)
    }

    @Test
    fun aJumpAwayStartsASecondReading() = runBlocking {
        val id = store.extend(34, surah = 2, activeId = null, at = 1_000)
        store.extend(120, surah = 2, activeId = id, at = 2_000)
        store.record(255, surah = 2, at = 3_000)
        assertEquals(listOf(255, 120), store.places.value.map { it.ayahNumber })
    }

    @Test
    fun readingsInDifferentSurahsStandOnTheirOwnRows() = runBlocking {
        val first = store.extend(120, surah = 2, activeId = null, at = 1_000)
        store.extend(34, surah = 3, activeId = first, at = 2_000)
        assertEquals(listOf(34, 120), store.places.value.map { it.ayahNumber })
    }

    @Test
    fun theListIsCappedAtTwentyReadings() = runBlocking {
        repeat(30) { index ->
            store.record(ayahNumber = index + 1, surah = index + 1, at = (index + 1) * 1_000L)
        }
        assertEquals(20, store.places.value.size)
        // The twenty most recent readings, so the last one is first and the
        // eleventh is last.
        assertEquals(30, store.places.value.first().surah)
        assertEquals(11, store.places.value.last().surah)
    }

    @Test
    fun anAyahOutsideTheQuranIsIgnored() = runBlocking {
        store.record(0, surah = 1)
        store.record(9999, surah = 1)
        assertTrue(store.places.value.isEmpty())
    }

    @Test
    fun aReadingCanBeForgottenAndAnotherStays() = runBlocking {
        store.record(262, surah = 2, at = 1_000)
        store.record(255, surah = 2, at = 2_000)
        store.remove(255)
        assertEquals(listOf(262), store.places.value.map { it.ayahNumber })
    }

    @Test
    fun readingsSurviveReopening() = runBlocking {
        store.record(262, surah = 2, at = 5_000)
        store.close()
        val reopened = LastReadStore(context)
        reopened.load()
        val place = reopened.places.value.single()
        assertEquals(262, place.ayahNumber)
        assertEquals(2, place.surah)
        assertEquals(5_000, place.readAt)
        reopened.close()
    }

    /**
     * The list used to be one row per ayah, and the app has not shipped, so
     * the old table is made new on the upgrade rather than carried across
     * (owner decision, this session). The upgrade must still open cleanly:
     * a reader who had the old rows meets an empty history, not a crash.
     */
    @Test
    fun upgradingFromTheOldPerAyahShapeOpensEmpty() = runBlocking {
        store.close()
        writeVersionTwoDatabase()
        store = LastReadStore(context)
        store.load()
        assertTrue(store.places.value.isEmpty())
        store.record(262, surah = 2)
        assertEquals(262, store.places.value.single().ayahNumber)
    }

    /**
     * A disk with no room left is a message, not a crash. A last-read write
     * lands while the reader is moving through the page, so a database that
     * cannot take the row must leave the reading standing and say so in the
     * log, and cancellation is the one thing that still travels as itself
     * (owner decision, 11.1).
     */
    @Test
    fun aFullDiskIsLoggedInsteadOfCrashing() = runBlocking {
        val full = LastReadStore(FullDiskHelper(context))
        full.load()
        assertNull(
            "a reading that cannot be written returns nothing",
            full.extend(1, surah = 1, activeId = null),
        )
        assertNull(
            "a place that cannot be written returns nothing",
            full.record(1, surah = 1),
        )
        full.remove(1)
        assertTrue(full.places.value.isEmpty())
        full.close()
    }

    /** A helper whose database will not open, as a phone with no room left. */
    private class FullDiskHelper(context: Context) :
        SQLiteOpenHelper(context, "full-disk.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) = Unit
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        override fun getWritableDatabase(): SQLiteDatabase =
            throw SQLiteFullException("the disk is full")
    }

    /** A version 2 database, written by hand, with one place in it. */
    private fun writeVersionTwoDatabase() {
        val path = context.getDatabasePath("last-read.db")
        path.parentFile?.mkdirs()
        val legacy = SQLiteDatabase.openOrCreateDatabase(path, null)
        legacy.execSQL(
            "CREATE TABLE last_read (" +
                "ayah_number INTEGER PRIMARY KEY, " +
                "read_at INTEGER NOT NULL)",
        )
        legacy.execSQL(
            "INSERT INTO last_read (ayah_number, read_at) VALUES (262, 5000)",
        )
        legacy.version = 2
        legacy.close()
    }
}
