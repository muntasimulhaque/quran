package io.github.muntasimulhaque.quran

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.PackStore
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
 * The one language rule the reading and search share: the reader's own
 * language decides the content, and nothing falls back to another language.
 *
 * The surah introductions are carried by the word list of the language, and
 * only the English list carries any today. The introduction is the one
 * deliberate exception to the no-fallback rule: a Bangla reader is shown the
 * English introduction rather than no door at all (owner decision, 6.6). The
 * word meanings keep the strict rule and never fall back, and
 * that is pinned here too.
 */
@RunWith(AndroidJUnit4::class)
class SurahInfoTest {

    private lateinit var content: ContentDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val catalog = PackCatalog.load(context)
        val store = PackStore(context)
        // Development and test builds carry every pack inside the app, so
        // both word lists can be placed without a network. The library is
        // opened over these two alone: the rule under test is which list a
        // language reads, and the packs the rest of the run installed are not
        // part of that question. Handing over the whole store made the rule
        // depend on how many packs happened to be there, and past SQLite's
        // ten-attachment ceiling the last one fell out of the library and
        // read as missing (owner decision, 20.0).
        for (id in listOf("words-en", "words-bn")) {
            assertTrue(
                "the build must carry $id in its own assets",
                store.install(id),
            )
        }
        content = runBlocking {
            ContentDatabase.open(
                context,
                catalog,
                setOf("words-en", "words-bn"),
                listOf("words-en", "words-bn"),
            )
        }
    }

    @After
    fun tearDown() {
        content.close()
    }

    @Test
    fun theIntroductionFallsBackToEnglishWhenTheLanguageHasNone() {
        val english = content.surahInfo(1, "en")
        assertNotNull("the English introduction must be there", english)
        assertEquals(
            "a language with no introduction of its own is shown the English one",
            english,
            content.surahInfo(1, "bn"),
        )
    }

    @Test
    fun theWordListNeverFallsBackToAnotherLanguage() {
        assertEquals("words-en", content.meaningPack("en"))
        assertEquals("words-bn", content.meaningPack("bn"))
        assertNull(
            "a language with no list installed answers nothing",
            content.meaningPack("ar"),
        )
    }
}
