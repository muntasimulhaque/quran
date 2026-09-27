package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The saved-ayah database is the reader's own work, so its behavior is
 * pinned: a note is written on a kept ayah, so writing one keeps the ayah,
 * a toggle saves and removes, removing the save removes the note with it,
 * newest comes first, and everything survives the app being closed.
 */
@RunWith(AndroidJUnit4::class)
class SavedStoreTest {

    private lateinit var context: Context
    private lateinit var store: SavedStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("saved.db")
        store = SavedStore(context)
    }

    @After
    fun tearDown() {
        store.close()
        context.deleteDatabase("saved.db")
    }

    @Test
    fun toggleSavesAndRemoves() = runBlocking {
        store.load()
        store.toggle(262)
        assertEquals(listOf(262), store.saved.value.map { it.ayahNumber })
        assertTrue(store.saved.value.single().saved)
        store.toggle(262)
        assertTrue(store.saved.value.isEmpty())
    }

    @Test
    fun aNoteSavesTheAyah() = runBlocking {
        store.setNote(262, "The Throne verse")
        val row = store.saved.value.single()
        assertEquals(262, row.ayahNumber)
        assertEquals("The Throne verse", row.note)
        assertTrue("a note is written on a kept ayah, so it keeps the ayah", row.saved)
        assertNotNull("the keep carries its own moment", row.savedAt)
    }

    @Test
    fun savingAnAyahThenWritingANoteKeepsBoth() = runBlocking {
        store.toggle(262)
        store.setNote(262, "The Throne verse")
        val row = store.saved.value.single()
        assertTrue(row.saved)
        assertEquals("The Throne verse", row.note)
    }

    @Test
    fun removingASaveRemovesItsNote() = runBlocking {
        store.toggle(262)
        store.setNote(262, "The Throne verse")
        store.unsave(262)
        assertTrue(store.saved.value.isEmpty())
    }

    @Test
    fun clearingANoteKeepsTheSave() = runBlocking {
        store.toggle(262)
        store.setNote(262, "The Throne verse")
        store.setNote(262, null)
        val row = store.saved.value.single()
        assertTrue(row.saved)
        assertNull(row.note)
    }

    @Test
    fun unsavingAnAyahWithNoNoteDropsTheRow() = runBlocking {
        store.toggle(1)
        store.unsave(1)
        assertTrue(store.saved.value.isEmpty())
    }

    @Test
    fun blankNoteClearsTheNoteAndKeepsTheSave() = runBlocking {
        store.setNote(262, "a note")
        store.setNote(262, "   ")
        val row = store.saved.value.single()
        assertTrue(row.saved)
        assertNull(row.note)
    }

    @Test
    fun newestComesFirst() = runBlocking {
        store.toggle(1)
        store.toggle(262)
        assertEquals(listOf(262, 1), store.saved.value.map { it.ayahNumber })
    }

    @Test
    fun savedRowsSurviveReopening() = runBlocking {
        store.setNote(262, "kept")
        store.close()
        val reopened = SavedStore(context)
        reopened.load()
        assertEquals("kept", reopened.saved.value.single().note)
        reopened.close()
    }

    @Test
    fun aNoteKeepsTheMomentItWasWritten() = runBlocking {
        store.setNote(262, "written now")
        val row = store.saved.value.single()
        assertNotNull("a written note carries its own moment", row.noteAt)
    }

    @Test
    fun aClearedNoteLosesItsMoment() = runBlocking {
        store.toggle(262)
        store.setNote(262, "a note")
        store.setNote(262, null)
        val row = store.saved.value.single()
        assertNull(row.noteAt)
    }

    @Test
    fun aSaveKeepsItsOwnMoment() = runBlocking {
        store.toggle(262)
        assertNotNull("a save carries its own moment", store.saved.value.single().savedAt)
    }

    @Test
    fun aRemovedSaveReturnsWithItsOwnLaterMoment() = runBlocking {
        store.toggle(262)
        val first = store.saved.value.single().savedAt
        assertNotNull(first)
        store.setNote(262, "kept")
        store.unsave(262)
        assertTrue(store.saved.value.isEmpty())
        store.toggle(262)
        val second = store.saved.value.single().savedAt
        assertNotNull(second)
        assertTrue("a re-save writes its own, later moment", second!! >= first!!)
    }

    /**
     * A reader who updates the app keeps the notes they wrote before the
     * notes list existed. The moment of the note was not recorded then, so
     * the ayah's own moment is the closest true answer left on the device,
     * and that is what the migrations write.
     */
    @Test
    fun aNoteFromBeforeTheNotesListKeepsItsAyahsMoment() = runBlocking {
        writeLegacyDatabase(
            version = 1,
            create = "CREATE TABLE saved (" +
                "ayah_number INTEGER PRIMARY KEY, note TEXT, created_at INTEGER NOT NULL)",
            insert = "INSERT INTO saved VALUES (262, 'an old note', 1234)",
        )

        val reopened = SavedStore(context)
        reopened.load()
        val row = reopened.saved.value.single()
        assertEquals("an old note", row.note)
        assertEquals(1234L, row.noteAt)
        assertTrue("a note written alone enters Saved", row.saved)
        assertEquals("its keep carries the note's own moment", 1234L, row.savedAt)
        reopened.close()
    }

    /**
     * Version 5 brings note-only rows into Saved: a note is written on a kept
     * ayah, so a reader who only wrote notes keeps them all, each dated by
     * the note's own moment, the closest true answer for when it was kept.
     * A row that was already saved keeps the moment it already had.
     */
    @Test
    fun theNoteMergeBringsNoteOnlyRowsIntoSaved() = runBlocking {
        writeLegacyDatabase(
            version = 2,
            create = "CREATE TABLE saved (" +
                "ayah_number INTEGER PRIMARY KEY, note TEXT, created_at INTEGER NOT NULL, " +
                "note_at INTEGER)",
            insert = "INSERT INTO saved VALUES (262, 'a note alone', 1000, 1000), " +
                "(263, 'after a save', 500, 900)",
        )

        val reopened = SavedStore(context)
        reopened.load()
        val byAyah = reopened.saved.value.associateBy { it.ayahNumber }
        assertTrue(byAyah.getValue(262).saved)
        assertEquals(1000L, byAyah.getValue(262).savedAt)
        assertTrue(byAyah.getValue(263).saved)
        assertEquals(500L, byAyah.getValue(263).savedAt)
        reopened.close()
    }

    /**
     * A version 3 database kept one moment for the row and one for the note,
     * so a row saved before the save's own column existed starts with its
     * row's moment, the closest true answer left on the device, and a
     * note-only row is brought into Saved with the note's own moment.
     */
    @Test
    fun theSaveMomentMigrationBackfillsSavedAndNoteRows() = runBlocking {
        writeLegacyDatabase(
            version = 3,
            create = "CREATE TABLE saved (" +
                "ayah_number INTEGER PRIMARY KEY, note TEXT, created_at INTEGER NOT NULL, " +
                "note_at INTEGER, saved INTEGER NOT NULL DEFAULT 1)",
            insert = "INSERT INTO saved VALUES (262, NULL, 1000, NULL, 1), " +
                "(263, 'a note alone', 900, 900, 0)",
        )

        val reopened = SavedStore(context)
        reopened.load()
        val byAyah = reopened.saved.value.associateBy { it.ayahNumber }
        assertEquals(1000L, byAyah.getValue(262).savedAt)
        assertTrue(byAyah.getValue(263).saved)
        assertEquals(900L, byAyah.getValue(263).savedAt)
        reopened.close()
    }

    private fun writeLegacyDatabase(version: Int, create: String, insert: String) {
        store.close()
        context.deleteDatabase("saved.db")
        val path = context.getDatabasePath("saved.db")
        path.parentFile?.mkdirs()
        val legacy = SQLiteDatabase.openOrCreateDatabase(path, null)
        legacy.execSQL(create)
        legacy.execSQL(insert)
        legacy.version = version
        legacy.close()
    }
}
