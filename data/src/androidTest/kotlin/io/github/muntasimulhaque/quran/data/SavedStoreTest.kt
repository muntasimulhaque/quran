package io.github.muntasimulhaque.quran.data

import android.content.Context
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
}
