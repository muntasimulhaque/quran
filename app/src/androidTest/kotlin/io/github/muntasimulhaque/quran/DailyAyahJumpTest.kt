package io.github.muntasimulhaque.quran

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A tap on the daily reminder opens that ayah in the reading.
 *
 * The reminder's whole purpose is to bring the reader to the words, so the
 * jump lands on the day's ayah, where the translation and the meanings are
 * open around it (owner decision, thirty-first session).
 */
@RunWith(AndroidJUnit4::class)
class DailyAyahJumpTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setUp() {
        PackStore(context).install("translation-saheeh-en")
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setAyah(1)
                setTranslationPacks(setOf("translation-saheeh-en"))
                setShowTranslation(true)
            }
        }
        // The view model the activity would create: the factory is the same
        // one the activity's own ViewModelProvider uses, and an
        // AndroidViewModel needs no store owner to exist.
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        @Suppress("UNCHECKED_CAST")
        viewModel = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
            .getInstance(application)
            .create(ReaderViewModel::class.java)
        awaitReady()
    }

    @Test
    fun theReminderSJumpLandsOnTheAyah() {
        val target = 7 + 255
        viewModel.jumpToAyahInStudy(target)
        awaitPlace(target)
        assertEquals("the place must be the ayah the reminder named", target, viewModel.settings.ayah)
    }

    @Test
    fun aJumpWhileReadingStaysOnTheAyah() {
                val target = 7 + 84
        viewModel.jumpToAyahInStudy(target)
        awaitPlace(target)
        assertEquals(target, viewModel.settings.ayah)
            }

    private fun awaitReady() {
        val deadline = System.currentTimeMillis() + 60_000
        while (!viewModel.ready && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
        }
        assertTrue("the library must open", viewModel.ready)
    }

    private fun awaitPlace(ayah: Int) {
        val deadline = System.currentTimeMillis() + 20_000
        while (viewModel.settings.ayah != ayah && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
        }
    }
}
