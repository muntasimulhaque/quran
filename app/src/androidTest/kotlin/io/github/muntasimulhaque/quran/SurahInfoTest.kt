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
 * English introduction rather than no door at all (owner decision, this
 * session). The word meanings keep the strict rule and never fall back, and
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
        // both word lists are on the device at once.
        for (id in listOf("words-en", "words-bn")) store.install(id)
        content = runBlocking { ContentDatabase.open(context, catalog, store.installed()) }
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
