package io.github.muntasimulhaque.quran

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.SearchHit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Search runs against the shipped database, through the same code the app
 * calls, in the app's own process where the content assets already live. The
 * Arabic path is checked for matched words and word positions, the English
 * path for diacritic-insensitive matching, and surah names for the obvious hit.
 */
@RunWith(AndroidJUnit4::class)
class ContentSearchTest {

    private lateinit var content: ContentDatabase

    @Before
    fun setUp() {
        content = runBlocking { ContentDatabase.open(ApplicationProvider.getApplicationContext()) }
    }

    @After
    fun tearDown() {
        content.close()
    }

    @Test
    fun arabicSearchFindsTheAyahAndItsWords() = runBlocking {
        val hits = content.search(Search.parse("الرحمن")!!, 50).filterIsInstance<SearchHit.AyahHit>()
        val first = hits.firstOrNull { it.ayah.number == 1 }
        assertNotNull("1:1 must be found for الرحمن", first)
        assertTrue("matched words must come back", first!!.matchedPositions.isNotEmpty())
        assertTrue("words must come back for highlighting", first.words.isNotEmpty())
    }

    @Test
    fun englishSearchIgnoresDiacritics() = runBlocking {
        val hits = content.search(Search.parse("allah")!!, 50).filterIsInstance<SearchHit.AyahHit>()
        assertTrue("allah must find something", hits.isNotEmpty())
        val first = hits.firstOrNull { it.ayah.number == 1 }
        assertNotNull("1:1 must be found for allah", first)
        assertTrue("the translation must come back", first!!.translation!!.contains("All"))
    }

    @Test
    fun surahNamesAreSearchable() = runBlocking {
        val hits = content.search(Search.parse("kahf")!!, 20)
        val surah = hits.filterIsInstance<SearchHit.SurahHit>().firstOrNull()
        assertEquals(18, surah?.surah?.number)
    }
}
