package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.core.localizedDigits
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.feature.search.R as SearchR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * A search for a surah's name draws the surah's own row, whole.
 *
 * The row's meta line (the place and the ayah count) is the string a reader
 * never saw because the app died composing it: `SurahRow` was changed to pass
 * the place first in 7.6 and the format string kept the old argument order, so
 * a search that matched a surah name threw
 * `IllegalFormatConversionException: d != java.lang.String` on the main
 * thread. The data was never the problem (SearchCheck and ContentSearchTest
 * passed); the row's own rendering was, which is why this pins the surface a
 * reader sees and the string contract under it in every language.
 */
@RunWith(AndroidJUnit4::class)
class SurahSearchTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /**
     * The library a search opens on: the reader's language chosen and the
     * packs the app installs for it, so the sheet is the one a reader meets.
     */
    private fun prepare() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = PackStore(context)
        for (id in listOf("translation-saheeh-en", "tafsir-ibn-kathir-en", "words-en")) {
            store.install(id)
        }
        val settings = SettingsStore(context)
        runBlocking {
            settings.setUiLanguage(UiLanguage.English.tag)
            settings.setAyah(1)
            settings.setTranslationPacks(setOf("translation-saheeh-en"))
            settings.setTafsirPacks(setOf("tafsir-ibn-kathir-en"))
            settings.setShowTranslation(true)
            settings.setWordByWord(false)
            settings.setLongPressHintShown()
        }
        Thread.sleep(500)
    }

    /**
     * The exact act the owner reported: a short query that matches a surah by
     * name, so the first row is the surah's, and the search sheet must stay up
     * with the row drawn whole. "kahf" is Al-Kahf: Makkah, 110 ayahs.
     */
    @Test
    fun aSurahNameInSearchDrawsItsWholeRow() {
        prepare()
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_SEARCH)
        ActivityScenario.launch<MainActivity>(intent).use {
            rule.waitUntil(timeoutMillis = 60_000) {
                rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNode(hasSetTextAction()).performTextInput("kahf")
            rule.waitUntil(timeoutMillis = 60_000) {
                rule.onAllNodesWithText("Al-Kahf", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            // The meta line is the part the crash took down: the place first,
            // the count second, as the row draws it (owner decision, the theme
            // and consistency session).
            rule.waitUntil(timeoutMillis = 60_000) {
                rule.onAllNodesWithText("110", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    /**
     * And the string contract itself, in every language the app ships: the
     * same two arguments the row passes (`place`, `ayahs`) resolve without a
     * formatter mismatch. The crash was one language copy away from the call
     * site, and both copies carried the old order; a reader in either
     * language met the same death.
     */
    @Test
    fun theSurahMetaRendersInEveryLanguage() {
        for (language in UiLanguage.entries) {
            val tag = language.tag
            val locale = Locale.forLanguageTag(tag)
            val context = localized(tag)
            val place = context.getString(SearchR.string.search_place_makkah)
            val meta = context.getString(SearchR.string.search_surah_meta, place, 110)
            assertTrue(
                "the $tag surah meta must name the place",
                meta.contains(place),
            )
            // The count wears the interface's own digits, the rule every
            // number the reader meets follows: 110 in English, ১১০ in Bangla
            // (owner decision, 8.3).
            assertTrue(
                "the $tag surah meta must count the ayahs",
                meta.contains(localizedDigits("110", locale)),
            )
        }
    }

    /** The app's own resources under one of its languages. */
    private fun localized(tag: String): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
            setLayoutDirection(Locale.forLanguageTag(tag))
        }
        return base.createConfigurationContext(configuration)
    }
}
