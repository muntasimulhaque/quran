package io.github.muntasimulhaque.quran

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.WordAudioManifest
import io.github.muntasimulhaque.quran.data.WordAudioStore
import io.github.muntasimulhaque.quran.ui.WordAudioDownloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The speaker's press as the download's ask.
 *
 * The sheet offers the mark on every word, so a press for a surah that is not
 * on the device fetches that surah on whatever connection the reader is on,
 * commits the pack, publishes the state, and hands the surah back so the word
 * can sound when it lands. This class holds that chain with a pack it writes
 * itself: the shipped pack is a network of its own, and the reading never
 * needs one (owner decision, this session).
 *
 * It is also the regression the 18.6 sheet could not answer: the mark was
 * decided from a snapshot taken when the rows were built, so a pack that
 * arrived while the reader stayed in the surah never reached the sheet. Here
 * the arrival is the state, and the state is what the sheet reads.
 */
@RunWith(AndroidJUnit4::class)
class WordAudioDownloadTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val folder = File(context.filesDir, "word-audio/108")

    @After
    fun clean() {
        scope.cancel()
        folder.deleteRecursively()
    }

    @Test
    fun aTapFetchesTheSurahCommitsItAndSaysItArrived() = runBlocking {
        folder.deleteRecursively()
        val store = WordAudioStore(context)
        val manifest = WordAudioManifest.load(context)
        // The asset manifest names every surah, so the tap has a pack to ask
        // for. Al-Kawthar is ten words and the smallest of them.
        assertTrue("the manifest must carry Al-Kawthar", manifest.packageFor(108) != null)
        var arrived = 0
        val downloads = WordAudioDownloads(
            context = context,
            scope = scope,
            store = store,
            manifest = manifest,
            onInstalled = { surah -> arrived = surah },
            fetch = { _, onProgress ->
                folder.mkdirs()
                File(folder, "108_001_001.opus").writeBytes(ByteArray(64))
                onProgress(64, 64)
                Result.success(Unit)
            },
        )
        try {
            assertFalse(store.isInstalled(108))
            downloads.request(108, unmeteredOnly = false)
            withTimeout(10_000) {
                while (downloads.state.value.surah != 108 || !downloads.state.value.installed) {
                    delay(10)
                }
            }
            assertEquals("the surah must be handed back for the word", 108, arrived)
            assertTrue("the pack must be readable by the store", store.isInstalled(108))
            assertTrue(store.uri(108, 1, 1) != null)
            assertFalse("a committed pack is not a failure", downloads.state.value.failed)
        } finally {
            downloads.retire()
        }
    }
}
