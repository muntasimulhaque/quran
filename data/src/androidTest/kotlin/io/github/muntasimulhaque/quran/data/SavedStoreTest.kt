package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The saved-ayah database is the reader's own work, so its behavior is pinned:
 * Save and Note are two separate marks on one ayah, a toggle saves and
 * unsaves, a note stays with its ayah and never enters Saved by itself,
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
    fun toggleSavesAndUnsaves() = runBlocking {
        store.load()
        store.toggle(262)
        assertEquals(listOf(262), store.saved.value.map { it.ayahNumber })
        assertTrue(store.saved.value.single().saved)
        store.toggle(262)
        assertTrue(store.saved.value.isEmpty())
    }

    @Test
    fun aNoteDoesNotSaveTheAyah() = runBlocking {
        store.setNote(262, "The Throne verse")
        val row = store.saved.value.single()
        assertEquals(262, row.ayahNumber)
        assertEquals("The Throne verse", row.note)
        assertFalse("a note never enters Saved by itself", row.saved)
    }

    @Test
    fun savingAnAyahKeepsItsNote() = runBlocking {
        store.setNote(262, "The Throne verse")
        store.toggle(262)
        val row = store.saved.value.single()
        assertTrue(row.saved)
        assertEquals("The Throne verse", row.note)
    }

    @Test
    fun unsavingAnAyahKeepsItsNote() = runBlocking {
        store.toggle(262)
        store.setNote(262, "The Throne verse")
        store.unsave(262)
        val row = store.saved.value.single()
        assertFalse(row.saved)
        assertEquals("The Throne verse", row.note)
    }

    @Test
    fun clearingANoteKeepsTheSave() = runBlocking {
        store.toggle(262)
        store.setNote(262, "The Throne verse")
        store.clearNote(262)
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
    fun clearingANoteWithNoSaveDropsTheRow() = runBlocking {
        store.setNote(1, "a passing thought")
        store.clearNote(1)
        assertTrue(store.saved.value.isEmpty())
    }

    @Test
    fun blankNoteClearsTheRowItAloneCreated() = runBlocking {
        store.setNote(262, "a note")
        store.setNote(262, "   ")
        assertTrue(store.saved.value.isEmpty())
    }

    @Test
    fun blankNoteOnASavedAyahKeepsTheSave() = runBlocking {
        store.toggle(262)
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
    fun theSaveMomentBelongsToTheCurrentSave() = runBlocking {
        store.toggle(262)
        val first = store.saved.value.single().savedAt
        assertNotNull(first)
        store.setNote(262, "kept")
        store.unsave(262)
        assertNull("an unsaved row carries no save moment", store.saved.value.single().savedAt)
        store.toggle(262)
        val second = store.saved.value.single().savedAt
        assertNotNull(second)
        assertTrue("a re-save writes its own, later moment", second!! >= first!!)
    }

    /**
     * A reader who updates the app keeps the notes they wrote before the
     * notes list existed. The moment of the note was not recorded then, so
     * the ayah's own moment is the closest true answer left on the device,
     * and that is what the migration writes.
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
        assertFalse("a note written alone never enters Saved", row.saved)
        reopened.close()
    }

    /**
     * The version 3 migration has one question it can answer: a row whose note
     * was written in the same insert as the row itself (the two moments are
     * one) was written by the note alone; a note written later than the row
     * cannot be told from a note the reader edited, and that row stays saved,
     * so nothing the reader saved is lost.
     */
    @Test
    fun theSaveNoteMigrationLeavesNoteOnlyRowsOutOfSaved() = runBlocking {
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
        assertFalse(byAyah.getValue(262).saved)
        assertTrue(byAyah.getValue(263).saved)
        reopened.close()
    }

    /**
     * A version 3 database kept one moment for the row and one for the note,
     * so a row saved before the save's own column existed starts with its
     * row's moment, the closest true answer left on the device, and a
     * note-only row keeps none.
     */
    @Test
    fun theSaveMomentMigrationBackfillsOnlySavedRows() = runBlocking {
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
        assertNull(byAyah.getValue(263).savedAt)
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
