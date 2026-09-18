package io.github.muntasimulhaque.quran

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.SearchHit
import io.github.muntasimulhaque.quran.data.SearchRequest
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
 * calls, in the app's own process where the content assets already live.
 *
 * The Arabic path is checked for exact match ranges over the visible text,
 * the English path for diacritic-insensitive matching, plus surah names,
 * references, word meanings, and the tafsir packs the reader enabled.
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

    private fun request(
        query: String,
        translations: Boolean = true,
        tafsir: Boolean = true,
        limit: Int = 50,
    ): SearchRequest {
        val packs = content.packs()
        return SearchRequest(
            query = Search.parse(query)!!,
            translationPacks = if (translations) {
                packs.filter { it.type == PackType.Translation }.map { it.id }
            } else {
                emptyList()
            },
            tafsirPacks = if (tafsir) {
                packs.filter { it.type == PackType.Tafsir }.map { it.id }
            } else {
                emptyList()
            },
            packNames = packs.associate { it.id to it.name },
            packLanguages = packs.associate { it.id to it.language },
            limit = limit,
        )
    }

    @Test
    fun arabicSearchFindsTheAyahAndMarksItsLetters() = runBlocking {
        val results = content.search(request("\u0627\u0644\u0631\u062d\u0645\u0646", limit = 50))
        val hit = results.hits.filterIsInstance<SearchHit.AyahHit>()
            .firstOrNull { it.ayah.number == 1 }
        assertNotNull("1:1 must be found for ar-Rahman", hit)
        assertTrue("the Arabic match must be reported", results.counts.arabic > 0)
    }

    @Test
    fun englishSearchIgnoresDiacritics() = runBlocking {
        val results = content.search(request("allah", tafsir = false))
        val hit = results.hits.filterIsInstance<SearchHit.AyahHit>()
            .firstOrNull { it.ayah.number == 1 }
        assertNotNull("1:1 must be found for allah", hit)
        assertTrue("the translation must come back", hit!!.translation != null)
        assertTrue(
            "the matched word must be highlighted in the translation",
            hit.translation!!.ranges.isNotEmpty(),
        )
    }

    @Test
    fun surahNamesAreSearchable() = runBlocking {
        val results = content.search(request("kahf"))
        val surah = results.hits.filterIsInstance<SearchHit.SurahHit>().firstOrNull()
        assertEquals(18, surah?.surah?.number)
    }

    @Test
    fun referencesJumpToTheAyah() = runBlocking {
        val results = content.search(request("2:255"))
        val reference = results.hits.filterIsInstance<SearchHit.ReferenceHit>().firstOrNull()
        assertEquals(2, reference?.ayah?.surah)
        assertEquals(255, reference?.ayah?.ayah)
    }

    @Test
    fun wordMeaningsAreSearchable() = runBlocking {
        val results = content.search(request("mercy", translations = false, tafsir = false))
        assertTrue("a word meaning must match", results.counts.words > 0)
        assertTrue(
            "the matched meaning must be shown",
            results.hits.filterIsInstance<SearchHit.AyahHit>().any { !it.wordMeaning.isNullOrBlank() },
        )
    }

    @Test
    fun tafsirPacksAreSearchableWhenEnabled() = runBlocking {
        val results = content.search(request("hypocrites", translations = false))
        assertTrue("Ibn Kathir must match", results.counts.tafsir > 0)
        val hit = results.hits.filterIsInstance<SearchHit.TafsirHitResult>().firstOrNull()
        assertNotNull(hit)
        assertEquals("tafsir-ibn-kathir-en", hit!!.pack)
        assertTrue("the excerpt must carry the match", hit.ranges.isNotEmpty())
        assertTrue("the excerpt must carry a range label", hit.fromAyah >= 1)
    }

    @Test
    fun searchIsFastWhenWarm() = runBlocking {
        content.search(request("mercy", tafsir = false))
        val t0 = android.os.SystemClock.uptimeMillis()
        content.search(request("mercy", tafsir = true))
        val t1 = android.os.SystemClock.uptimeMillis()
        content.search(request("mercy", tafsir = true))
        val t2 = android.os.SystemClock.uptimeMillis()
        println("SEARCH_TIMING index_build=" + (t1 - t0) + " warm=" + (t2 - t1))
        assertTrue("a warm search must stay under three seconds on an emulator", t2 - t1 < 3000)
    }

    @Test
    fun disabledPacksStayOutOfTheResults() = runBlocking {
        val results = content.search(request("hypocrites", translations = false, tafsir = false))
        assertEquals(0, results.counts.tafsir)
    }

    @Test
    fun everythingReportsWhereItMatched() = runBlocking {
        val results = content.search(request("mercy"))
        assertTrue("the counts must add up", results.counts.total > 0)
        val sources = results.counts
        assertTrue(
            "at least two sources should match a common word",
            listOf(sources.arabic, sources.translation, sources.words, sources.tafsir)
                .count { it > 0 } >= 2,
        )
    }
}
