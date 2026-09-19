package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * The word by word reading aid, end to end: a translation and its word list
 * are installed, the switch is turned on, and the study reading is checked to
 * show a meaning under a word, in the language of the chosen translation.
 *
 * The library is assembled before the reader screen starts, which is the
 * order a real reader creates it in, and development builds carry every pack,
 * so no network is involved. That is why the preparation is a rule outside
 * the compose rule: the compose rule owns the activity, so the activity
 * launches after the library is in place, and it is the supported pairing for
 * this test (an empty rule beside a separately launched activity could
 * compose on a thread without a looper and take the study list's prefetch
 * scheduler down with it).
 */
@RunWith(AndroidJUnit4::class)
class WordByWordTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    private val library = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val store = PackStore(context)
            store.install("translation-taisirul-quran-bn")
            store.install("words-bn")
            val settings = SettingsStore(context)
            runBlocking {
                settings.setTranslationPacks(setOf("translation-taisirul-quran-bn"))
                settings.setWordByWord(true)
                settings.setMode(ReadingMode.Study)
                for (role in TypeRole.entries) settings.setTypeSize(role, TextSize.DEFAULT)
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun wordMeaningsFollowTheTranslationLanguage() {
        // The first Bengali meaning of Al-Fatihah 1:1, from the word list. The
        // wait is generous on purpose: a software-rendered emulator takes its
        // time opening the content library, and a slow machine is not a
        // failing reading aid.
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithText("নামে", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        capture("word-by-word-bengali")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
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
