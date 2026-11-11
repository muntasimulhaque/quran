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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
 *
 * The second and third legs pin the two rules a pack's arrival follows after
 * the 14.6 report: a pack the queue fetched is installed and **not** chosen,
 * because the queue is the reader's past ask and not this moment's; and a
 * queue entry for a pack that is already here is done, so it cannot come
 * back the next time an update detaches the pack (owner report, this
 * session).
 */
@RunWith(AndroidJUnit4::class)
class ContentLibraryTest {

    @Test
    fun aPackArrivesAndLeavesThroughTheCallbacks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        // The pack starts off the device, so the install path is the one the
        // test walks. The build carries it as an asset, so no network is used.
        // A queue entry left by another test would install it first, so the
        // queue starts empty for this leg.
        PackStore(context).remove("words-en")
        SettingsStore(context).removePendingPacks(setOf("words-en"))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val opened = CompletableDeferred<ContentLibrary.Opening>()
        val installed = CompletableDeferred<Pair<ContentPack, Boolean>>()
        val removed = CompletableDeferred<ContentPack>()
        val library = ContentLibrary(
            application = context,
            scope = scope,
            settingsStore = SettingsStore(context),
            settings = { AppSettings() },
            onInstalled = { pack, select ->
                if (pack.id == "words-en") installed.complete(pack to select)
            },
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

            // The reader's tap on Add: the arrival is their choice.
            library.install("words-en", select = true)
            val (arrived, select) = withTimeout(60_000) { installed.await() }
            assertTrue("the callback names the pack", arrived.id == "words-en")
            assertTrue("a tap on Add chooses the pack", select)
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

    @Test
    fun theQueueInstallsAPackWithoutChoosingIt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store = PackStore(context)
        val settings = SettingsStore(context)
        // The pack is off the device and on the queue: the state a language
        // choice from an earlier session leaves behind when its download
        // never finished.
        store.remove("words-en")
        settings.removePendingPacks(setOf("words-en"))
        settings.addPendingPacks(setOf("words-en"))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val opened = CompletableDeferred<ContentLibrary.Opening>()
        val installed = CompletableDeferred<Pair<ContentPack, Boolean>>()
        val library = ContentLibrary(
            application = context,
            scope = scope,
            settingsStore = settings,
            settings = { AppSettings() },
            onInstalled = { pack, select ->
                if (pack.id == "words-en") installed.complete(pack to select)
            },
            onOpened = { opening -> opened.complete(opening) },
            onReopened = { },
            onRemoved = { _, _ -> },
        )
        try {
            library.open()
            withTimeout(60_000) { opened.await() }
            val (arrived, select) = withTimeout(60_000) { installed.await() }
            assertTrue("the queue brought the pack in", arrived.id == "words-en")
            assertFalse("an arrival from the queue is not a choice", select)
            awaitInstalled(library, "words-en", present = true)
            assertTrue(
                "the pack is on the device even though it was not chosen",
                library.isInstalled("words-en"),
            )
            // The entry is cleared after the library reopens over the pack,
            // so the test waits for the queue's own state instead of reading
            // it the moment the arrival callback lands.
            withTimeout(60_000) {
                while ("words-en" in settings.pendingPacks()) delay(50)
            }
            assertTrue(
                "its queue entry is done once it is here",
                "words-en" !in settings.pendingPacks(),
            )
        } finally {
            library.retire()
            scope.cancel()
            settings.removePendingPacks(setOf("words-en"))
            store.install("words-en")
        }
    }

    @Test
    fun aQueueEntryForAPackThatIsAlreadyHereIsDropped() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store = PackStore(context)
        val settings = SettingsStore(context)
        // The pack is here, and an old language choice left it queued: the
        // drain used to skip it forever, which is how it could come back
        // after an update detached it.
        store.install("words-en")
        settings.addPendingPacks(setOf("words-en"))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val opened = CompletableDeferred<ContentLibrary.Opening>()
        val library = ContentLibrary(
            application = context,
            scope = scope,
            settingsStore = settings,
            settings = { AppSettings() },
            onInstalled = { _, _ -> },
            onOpened = { opening -> opened.complete(opening) },
            onReopened = { },
            onRemoved = { _, _ -> },
        )
        try {
            library.open()
            withTimeout(60_000) { opened.await() }
            withTimeout(60_000) {
                while ("words-en" in settings.pendingPacks()) delay(50)
            }
            assertFalse(
                "a pack that is already here is done with the queue",
                "words-en" in settings.pendingPacks(),
            )
        } finally {
            library.retire()
            scope.cancel()
            settings.removePendingPacks(setOf("words-en"))
            store.install("words-en")
        }
    }

    @Test
    fun aWordAidWhoseListWasAlreadyHereIsGivenItsRoot() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store = PackStore(context)
        val settings = SettingsStore(context)
        val original = settings.settings.first()
        // The state a reader who turned the aid on before the root existed
        // is in: the list is here, the root is not, and the aid is on. The
        // app has to bring the root without another tap (owner report).
        settings.setUiLanguage("en")
        store.install("words-en")
        store.remove("word-root")
        // Another class in this process may have left a pack on the queue;
        // the drain would stop at a tafsir waiting for Wi-Fi and never reach
        // the root. The test owns the queue while it runs.
        settings.removePendingPacks(settings.pendingPacks())
        settings.setWordByWord(true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val opened = CompletableDeferred<ContentLibrary.Opening>()
        val library = ContentLibrary(
            application = context,
            scope = scope,
            settingsStore = settings,
            settings = { AppSettings(wordByWord = true) },
            onInstalled = { _, _ -> },
            onOpened = { opening -> opened.complete(opening) },
            onReopened = { },
            onRemoved = { _, _ -> },
        )
        try {
            library.open()
            withTimeout(60_000) { opened.await() }
            // The build carries the root, so the queue places it without a
            // network; the wait is for the copy and the reopen, not a
            // download.
            withTimeout(60_000) {
                while (!library.isInstalled("word-root")) delay(50)
            }
            withTimeout(60_000) {
                while ("word-root" in settings.pendingPacks()) delay(50)
            }
            assertTrue(
                "the aid's root follows the list already on the device",
                library.isInstalled("word-root"),
            )
        } finally {
            library.retire()
            scope.cancel()
            settings.setWordByWord(false)
            settings.removePendingPacks(setOf("word-root"))
            original.uiLanguage?.let { settings.setUiLanguage(it) }
            store.remove("word-root")
        }
    }

    /** Waits for the library's own state to catch up with its callback. */
    private suspend fun awaitInstalled(library: ContentLibrary, id: String, present: Boolean) {
        withTimeout(60_000) {
            while (library.isInstalled(id) != present) delay(50)
        }
    }
}
