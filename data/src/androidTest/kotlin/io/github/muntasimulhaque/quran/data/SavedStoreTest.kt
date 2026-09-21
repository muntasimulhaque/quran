package io.github.muntasimulhaque.quran.data

import android.content.Context
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
 * The saved-ayah database is the reader's own work, so its behavior is pinned:
 * a toggle saves and unsaves, a note stays with its ayah, a blank note clears
 * without dropping the ayah, newest comes first, and everything survives the
 * app being closed.
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
        store.toggle(262)
        assertTrue(store.saved.value.isEmpty())
    }

    @Test
    fun noteIsKeptWithItsAyah() = runBlocking {
        store.setNote(262, "The Throne verse")
        val row = store.saved.value.single()
        assertEquals(262, row.ayahNumber)
        assertEquals("The Throne verse", row.note)
    }

    @Test
    fun blankNoteClearsButKeepsTheAyah() = runBlocking {
        store.setNote(262, "a note")
        store.setNote(262, "   ")
        val row = store.saved.value.single()
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
    fun removeDropsTheRow() = runBlocking {
        store.toggle(1)
        store.remove(1)
        assertTrue(store.saved.value.isEmpty())
    }

    @Test
    fun aNoteKeepsTheMomentItWasWritten() = runBlocking {
        store.setNote(262, "written now")
        val row = store.saved.value.single()
        assertNotNull("a written note carries its own moment", row.noteAt)
    }

    @Test
    fun aClearedNoteLosesItsMoment() = runBlocking {
        store.setNote(262, "a note")
        store.setNote(262, null)
        assertNull(store.saved.value.single().noteAt)
    }

    /**
     * A reader who updates the app keeps the notes they wrote before the
     * notes list existed. The moment of the note was not recorded then, so
     * the ayah's own moment is the closest true answer left on the device,
     * and that is what the migration writes.
     */
    @Test
    fun aNoteFromBeforeTheNotesListKeepsItsAyahsMoment() = runBlocking {
        store.close()
        context.deleteDatabase("saved.db")
        val path = context.getDatabasePath("saved.db")
        path.parentFile?.mkdirs()
        val legacy = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null)
        legacy.execSQL(
            "CREATE TABLE saved (" +
                "ayah_number INTEGER PRIMARY KEY, note TEXT, created_at INTEGER NOT NULL)",
        )
        legacy.execSQL("INSERT INTO saved VALUES (262, 'an old note', 1234)")
        legacy.version = 1
        legacy.close()

        val reopened = SavedStore(context)
        reopened.load()
        val row = reopened.saved.value.single()
        assertEquals("an old note", row.note)
        assertEquals(1234L, row.noteAt)
        reopened.close()
    }
}
