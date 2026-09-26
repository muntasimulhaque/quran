package io.github.muntasimulhaque.quran

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.SearchHit
import io.github.muntasimulhaque.quran.data.SearchRequest
import io.github.muntasimulhaque.quran.data.SearchSources
import io.github.muntasimulhaque.quran.data.shouldShowWordMeaning
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A search row never says the same match twice.
 *
 * The owner searched "mercy" and saw the translation's highlight and, under
 * it, a labelled Word meaning block carrying the same match (owner report,
 * D-097). Against the shipped database the two sources are both real: the
 * translation matches hundreds of ayahs and the word list matches its own,
 * and a row can carry both. The rule is pinned pure in the data unit tests;
 * this proves the real query produces rows the rule reads as one-evidence
 * rows.
 */
@RunWith(AndroidJUnit4::class)
class SearchMeaningTest {

    private lateinit var content: ContentDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val catalog = PackCatalog.load(context)
        val store = PackStore(context)
        for (id in listOf("translation-saheeh-en", "words-en")) {
            store.install(id)
        }
        content = runBlocking { ContentDatabase.open(context, catalog, store.installed()) }
    }

    @After
    fun tearDown() {
        content.close()
    }

    @Test
    fun aTranslationMatchIsNeverRepeatedAsAMeaning() = runBlocking {
        val packs = content.packs().filter { it.installed }
        val results = content.search(
            SearchRequest(
                query = Search.parse("mercy")!!,
                translationPacks = packs.filter { it.type == PackType.Translation }.map { it.id },
                tafsirPacks = emptyList(),
                packNames = packs.associate { it.id to it.name },
                packLanguages = packs.associate { it.id to it.language },
                wordsPack = "words-en",
                sources = SearchSources.ALL.copy(tafsirs = false),
                limit = 50,
            ),
        )
        val rows = results.hits.filterIsInstance<SearchHit.AyahHit>()
        assertTrue("the query must find rows", rows.isNotEmpty())

        val doubled = rows.filter { row ->
            row.wordMeaning != null && shouldShowWordMeaning(row) &&
                row.translation?.ranges?.isNotEmpty() == true
        }
        assertTrue(
            "a meaning must never be drawn beside a translation that already matched, " +
                "found ${doubled.size} doubled rows",
            doubled.isEmpty(),
        )

        // And a row whose only match is a meaning still carries it.
        val meaningOnly = rows.firstOrNull { shouldShowWordMeaning(it) }
        assertTrue(
            "a meaning-only row must keep its meaning",
            meaningOnly == null || meaningOnly.wordMeaning != null,
        )
    }

    @Test
    fun theMeaningOnlyRowIsStillDrawn() = runBlocking {
        val packs = content.packs().filter { it.installed }
        val results = content.search(
            SearchRequest(
                query = Search.parse("mercy")!!,
                translationPacks = emptyList(),
                tafsirPacks = emptyList(),
                packNames = packs.associate { it.id to it.name },
                packLanguages = packs.associate { it.id to it.language },
                wordsPack = "words-en",
                sources = SearchSources.ALL.copy(translations = false, tafsirs = false),
                limit = 50,
            ),
        )
        val rows = results.hits.filterIsInstance<SearchHit.AyahHit>()
        assertTrue("the word list must match", rows.any { it.wordMeaning != null })
        assertTrue(
            "with no other source on, every meaning row is the row's only evidence",
            rows.filter { it.wordMeaning != null }.all { shouldShowWordMeaning(it) },
        )
        assertFalse(
            "but a row whose meaning is absent never grows one",
            rows.any { it.wordMeaning == null && shouldShowWordMeaning(it) },
        )
        assertTrue(
            "a meaning row names the word that carried it, so the row can be read",
            rows.filter { it.wordMeaning != null }.all { it.matchedWordText.isNotEmpty() },
        )
        assertTrue(
            "and the words are the ayah's own, not the meaning's spelling",
            rows.filter { it.wordMeaning != null }.all { hit ->
                hit.matchedWordText.all { word -> hit.ayah.text.contains(word) }
            },
        )
    }
}
