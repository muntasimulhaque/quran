package io.github.muntasimulhaque.quran

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.ui.ContentLibrary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The library controller on its own: it opens the content, brings a pack in
 * from the build's own assets, reports it to the host, takes it off again,
 * and reports that too.
 *
 * The split gave the library its own file, and this is the file's own test:
 * the open, install, and remove paths the reader's taps walk are exercised
 * here directly, without the reading screen around them. The build carries
 * every pack, so the install path works with no network (owner decision,
 * this session).
 */
@RunWith(AndroidJUnit4::class)
class ContentLibraryTest {

    @Test
    fun aPackArrivesAndLeavesThroughTheCallbacks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        // The pack starts off the device, so the install path is the one the
        // test walks. The build carries it as an asset, so no network is used.
        PackStore(context).remove("words-en")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val opened = CompletableDeferred<ContentLibrary.Opening>()
        val installed = CompletableDeferred<ContentPack>()
        val removed = CompletableDeferred<ContentPack>()
        val library = ContentLibrary(
            application = context,
            scope = scope,
            settingsStore = SettingsStore(context),
            settings = { AppSettings() },
            onInstalled = { pack -> if (pack.id == "words-en") installed.complete(pack) },
            onOpened = { opening -> opened.complete(opening) },
            onReopened = { },
            onRemoved = { pack, _ -> if (pack.id == "words-en") removed.complete(pack) },
        )
        try {
            library.open()
            val opening = withTimeout(60_000) { opened.await() }
            assertNotNull("the library must hand back its content", opening.database.ayah(1))
            assertTrue("the Book's index arrives with the opening", opening.surahs.size == 114)
            assertFalse("the pack starts off the device", library.isInstalled("words-en"))

            library.install("words-en")
            val arrived = withTimeout(60_000) { installed.await() }
            assertTrue("the callback names the pack", arrived.id == "words-en")
            // The selection callback lands before the reopen finishes, which
            // is the order the reader's tap needs; the state is read back
            // once the library has caught up.
            awaitInstalled(library, "words-en", present = true)
            assertTrue(
                "the library holds the pack after its own reopen",
                library.isInstalled("words-en"),
            )

            library.remove("words-en")
            val left = withTimeout(60_000) { removed.await() }
            assertTrue("the removal names the pack", left.id == "words-en")
            awaitInstalled(library, "words-en", present = false)
            assertFalse("the library lets the pack go", library.isInstalled("words-en"))
        } finally {
            library.retire()
            scope.cancel()
            // Put the pack back for whatever tests follow in this process.
            PackStore(context).install("words-en")
        }
    }

    /** Waits for the library's own state to catch up with its callback. */
    private suspend fun awaitInstalled(library: ContentLibrary, id: String, present: Boolean) {
        withTimeout(60_000) {
            while (library.isInstalled(id) != present) kotlinx.coroutines.delay(50)
        }
    }
}
