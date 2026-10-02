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
 * The saved-ayah database is the reader's own work, so its behavior is
 * pinned: a note is written on a kept ayah, so writing one keeps the ayah,
 * a toggle saves and removes, removing the save removes the note with it,
 * newest comes first, and everything survives the app being closed.
 *
 * Save and Note are two marks and the row keeps them apart: a note brings its
 * ayah into Saved and never presses Save, so the pill lights one and not the
 * other, and one tap of Save on a note-only ayah marks it rather than asking
 * to take away a keep the reader never made (owner report).
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
        assertTrue(store.saved.value.single().marked)
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

    /**
     * The two marks are the reader's own two hands, and a note is not a Save.
     * The note keeps the ayah in Saved and lights the note, never the
     * bookmark, and the Save that came from the note alone is a keep the
     * reader never pressed.
     */
    @Test
    fun aNoteAloneIsNotTheReadersSave() = runBlocking {
        store.setNote(262, "The Throne verse")
        val row = store.saved.value.single()
        assertTrue("the note keeps the ayah in Saved", row.saved)
        assertFalse("a note is not the reader pressing Save", row.marked)
    }

    /**
     * One tap of Save on an ayah a note brought in makes the mark, and asks
     * nothing: there is no save of the reader's own to take away yet. The
     * note stays exactly where it was, and the keep takes the mark's moment.
     */
    @Test
    fun savingAnAyahThatOnlyANoteKeptMarksIt() = runBlocking {
        store.setNote(262, "The Throne verse")
        store.toggle(262)
        val row = store.saved.value.single()
        assertTrue(row.marked)
        assertTrue(row.saved)
        assertEquals("the note is untouched by the mark", "The Throne verse", row.note)
    }

    /**
     * A save the reader made keeps its mark, and so does a note written on it
     * afterwards: the note adds its own words and never takes the mark away.
     */
    @Test
    fun aNoteOnASavedAyahLeavesTheMarkAlone() = runBlocking {
        store.toggle(262)
        store.setNote(262, "The Throne verse")
        val row = store.saved.value.single()
        assertTrue("the reader pressed Save on this one", row.marked)
        assertEquals("The Throne verse", row.note)
    }

    /**
     * The second tap of Save takes the mark and the note with it, and it is
     * the screen that asks first, never this database.
     */
    @Test
    fun aSecondSaveTakesTheMarkAndTheNote() = runBlocking {
        store.toggle(262)
        store.setNote(262, "The Throne verse")
        store.toggle(262)
        assertTrue(store.saved.value.isEmpty())
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
        assertTrue("the reader pressed Save, so the mark stays", row.marked)
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
        store.toggle(262)
        store.setNote(262, "a note")
        store.setNote(262, "   ")
        val row = store.saved.value.single()
        assertTrue(row.saved)
        assertTrue("a save the reader made is theirs to keep", row.marked)
        assertNull(row.note)
    }

    /**
     * A note the reader erased takes the keep it made with it: nothing else
     * is holding that ayah in Saved, and a row with no note and no mark of
     * their own is a row nothing on the pill could ever explain (owner
     * report).
     */
    @Test
    fun aClearedNoteOnANoteOnlyAyahLeavesNothingBehind() = runBlocking {
        store.setNote(262, "a note")
        store.setNote(262, "   ")
        assertTrue(store.saved.value.isEmpty())
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
        // The row a note alone kept keeps no mark, all the way through: the
        // note and the keep were written in the same moment, and a mark is
        // the one thing on this row that no moment of its own can prove.
        assertTrue("a save the reader made", byAyah.getValue(262).marked)
        assertFalse("a note alone", byAyah.getValue(263).marked)
        reopened.close()
    }

    /**
     * Version 6 tells the reader's Save from the keep a note made. A row that
     * was only ever saved by hand is the mark, a row a note alone kept is
     * not, and a row that was saved first and noted afterwards is the mark
     * again: the keep is older than the note on it. Those three are the
     * shapes a device can hold, and each one keeps the mark the pill shows.
     */
    @Test
    fun theSaveMarkMigrationReadsTheKeepTheReaderMade() = runBlocking {
        writeLegacyDatabase(
            version = 5,
            create = "CREATE TABLE saved (" +
                "ayah_number INTEGER PRIMARY KEY, note TEXT, created_at INTEGER NOT NULL, " +
                "note_at INTEGER, saved INTEGER NOT NULL DEFAULT 1, saved_at INTEGER)",
            insert = "INSERT INTO saved VALUES (262, NULL, 1000, NULL, 1, 1000), " +
                "(263, 'a note alone', 1000, 1000, 1, 1000), " +
                "(264, 'after a save', 1000, 2000, 1, 1000)",
        )

        val reopened = SavedStore(context)
        reopened.load()
        val byAyah = reopened.saved.value.associateBy { it.ayahNumber }
        assertTrue("saved by hand, and never noted", byAyah.getValue(262).marked)
        assertFalse("a note alone never pressed Save", byAyah.getValue(263).marked)
        assertTrue("saved first, noted after", byAyah.getValue(264).marked)
        assertTrue("every row stays in Saved", byAyah.values.all { it.saved })
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
