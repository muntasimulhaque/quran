package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Last Read is the history around the reader's place, so its behavior is
 * pinned: one row per ayah, newest first, a return moves a place to the top
 * rather than adding a copy, and the list stays a list of places rather than a
 * log that grows without end.
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
    fun newestPlaceComesFirst() = runBlocking {
        store.load()
        store.record(262, at = 1_000)
        store.record(1, at = 2_000)
        assertEquals(listOf(1, 262), store.places.value.map { it.ayahNumber })
    }

    @Test
    fun returningToAPlaceMovesItUpInsteadOfAddingACopy() = runBlocking {
        store.record(262, at = 1_000)
        store.record(1, at = 2_000)
        store.record(262, at = 3_000)
        assertEquals(listOf(262, 1), store.places.value.map { it.ayahNumber })
    }

    @Test
    fun theListIsCappedAtTwentyPlaces() = runBlocking {
        repeat(30) { index -> store.record(index + 1, at = (index + 1) * 1_000L) }
        assertEquals(20, store.places.value.size)
        // The twenty most recent, so ayah 30 is first and ayah 11 is last.
        assertEquals(30, store.places.value.first().ayahNumber)
        assertEquals(11, store.places.value.last().ayahNumber)
    }

    @Test
    fun anAyahOutsideTheQuranIsIgnored() = runBlocking {
        store.record(0)
        store.record(9999)
        assertTrue(store.places.value.isEmpty())
    }

    @Test
    fun aPlaceCanBeForgotten() = runBlocking {
        store.record(262)
        store.remove(262)
        assertTrue(store.places.value.isEmpty())
    }

    @Test
    fun placesSurviveReopening() = runBlocking {
        store.record(262, at = 5_000)
        store.close()
        val reopened = LastReadStore(context)
        reopened.load()
        val place = reopened.places.value.single()
        assertEquals(262, place.ayahNumber)
        assertEquals(5_000, place.readAt)
        reopened.close()
    }

    /**
     * The version 1 table carried a mode column nothing read. The upgrade
     * rebuilds the table without it, and the reader's places and moments
     * come across whole: a place abandoned before the update is still a
     * place after it (owner decision, this session).
     */
    @Test
    fun aPlaceFromBeforeTheRebuildKeepsItsAyahAndMoment() = runBlocking {
        store.close()
        writeVersionOneDatabase()
        store = LastReadStore(context)
        store.load()
        val place = store.places.value.single()
        assertEquals(262, place.ayahNumber)
        assertEquals(5_000L, place.readAt)
    }

    /** A version 1 database, written by hand, with one place in it. */
    private fun writeVersionOneDatabase() {
        val path = context.getDatabasePath("last-read.db")
        path.parentFile?.mkdirs()
        val legacy = SQLiteDatabase.openOrCreateDatabase(path, null)
        legacy.execSQL(
            "CREATE TABLE last_read (" +
                "ayah_number INTEGER PRIMARY KEY, " +
                "mode TEXT NOT NULL, " +
                "read_at INTEGER NOT NULL)",
        )
        legacy.execSQL(
            "INSERT INTO last_read (ayah_number, mode, read_at) VALUES (262, 'single', 5000)",
        )
        legacy.version = 1
        legacy.close()
    }
}
