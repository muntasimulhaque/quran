package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The word by word reading aid, end to end: a translation and its word list
 * are installed, the switch is turned on, and the study reading is checked to
 * show a meaning under a word, in the language of the chosen translation.
 *
 * The library is assembled before the reader screen starts, which is the
 * order a real reader creates it in, and development builds carry every pack,
 * so no network is involved.
 */
@RunWith(AndroidJUnit4::class)
class WordByWordTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    @Test
    fun wordMeaningsFollowTheTranslationLanguage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PackStore(context)
        store.install("translation-taisirul-quran-bn")
        store.install("words-bn")
        val settings = SettingsStore(context)
        runBlocking {
            settings.setTranslationPack("translation-taisirul-quran-bn")
            settings.setWordByWord(true)
            settings.setMode(ReadingMode.Study)
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            // The first Bengali meaning of Al-Fatihah 1:1, from the word list.
            rule.waitUntil(timeoutMillis = 25_000) {
                rule.onAllNodesWithText("নামে", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            capture("word-by-word-bengali")
        }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        Thread.sleep(400)
        val bitmap = androidx.test.runner.screenshot.Screenshot.capture().bitmap
        val given = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val directory = if (given.isNullOrBlank()) {
            File(
                InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
                "screenshots",
            )
        } else {
            File(given)
        }
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
