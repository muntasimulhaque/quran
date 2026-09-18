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

    @Test
    fun exportAndImportRoundTripToAnotherPhone() = runBlocking {
        store.setNote(262, "The Throne verse")
        store.toggle(1)
        val document = store.exportJson()
        store.close()

        context.deleteDatabase("saved.db")
        val fresh = SavedStore(context)
        fresh.load()
        assertTrue(fresh.saved.value.isEmpty())
        val imported = fresh.importJson(document)
        assertEquals(2, imported.getOrThrow())
        assertEquals(setOf(262, 1), fresh.saved.value.map { it.ayahNumber }.toSet())
        assertEquals("The Throne verse", fresh.saved.value.first { it.ayahNumber == 262 }.note)
        fresh.close()
    }

    @Test
    fun importKeepsTheExistingNoteAndIgnoresJunk() = runBlocking {
        store.setNote(262, "mine")
        val document = """
            {
              "format": "quran-saved-ayahs",
              "version": 1,
              "saved": [
                { "ayah": 262, "note": "theirs", "createdAt": 1 },
                { "ayah": 999999, "note": "out of range" },
                { "ayah": 2, "note": "kept" }
              ]
            }
        """.trimIndent()
        val imported = store.importJson(document)
        assertEquals(1, imported.getOrThrow())
        assertEquals("mine", store.saved.value.first { it.ayahNumber == 262 }.note)
        assertEquals("kept", store.saved.value.first { it.ayahNumber == 2 }.note)
    }

    @Test
    fun aForeignFileIsRefusedWithoutTouchingAnything() = runBlocking {
        store.toggle(1)
        val refused = store.importJson("{ \"format\": \"something-else\" }")
        assertTrue(refused.isFailure)
        assertEquals(listOf(1), store.saved.value.map { it.ayahNumber })
    }
}
