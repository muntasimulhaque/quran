package io.github.muntasimulhaque.quran

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.PlaybackPosition
import io.github.muntasimulhaque.quran.data.PlaybackPositionStore
import io.github.muntasimulhaque.quran.playback.PlaybackController
import io.github.muntasimulhaque.quran.playback.PlaybackService
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one custom command the player keeps for the reading, pinned where it can
 * be exercised: the controller cannot set the stop after this ayah itself,
 * because `setPauseAtEndOfMediaItems` is an `ExoPlayer` fact and not something
 * the `Player` interface a `MediaController` speaks carries. It travels as a
 * boolean.
 *
 * The word's own voice is not a command any more. A word is one Opus file
 * item on its own player, so its end is the item's end, and the two legs below
 * pin what the reading needs from that: the word ends by itself and leaves the
 * session's playlist and the saved place untouched, and a recitation that was
 * playing waits at the word and is given back when the word ends (owner
 * decision, this session).
 */
@RunWith(AndroidJUnit4::class)
class PlaybackServiceTest {

    @Test
    fun theStopCommandIsAdvertisedAndAnswered() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (controller, future) = connect(context)
        try {
            val command = SessionCommand(PlaybackService.COMMAND_PAUSE_AT_END, Bundle.EMPTY)
            assertTrue(
                "the session must advertise the stop command it is asked for",
                readMain { controller.availableSessionCommands.contains(command) },
            )

            val answered = CountDownLatch(1)
            val result = AtomicReference<SessionResult>()
            val main = ContextCompat.getMainExecutor(context)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val sent = controller.sendCustomCommand(
                    command,
                    Bundle().apply { putBoolean(PlaybackService.ARG_PAUSE_AT_END, true) },
                )
                sent.addListener(
                    {
                        result.set(sent.get())
                        answered.countDown()
                    },
                    main,
                )
            }
            assertTrue("the session must answer the stop command", answered.await(20, TimeUnit.SECONDS))
            assertEquals(SessionResult.RESULT_SUCCESS, result.get().resultCode)
        } finally {
            release(controller, future)
        }
    }

    /**
     * A word's own voice on real audio from the app's own assets: the word ends
     * by itself, and the session's player is never handed the word at all, so
     * the playlist, the capsule, and the saved place stand exactly as they
     * were (owner decision, this session).
     */
    @Test
    fun theWordsOwnFileEndsAndLeavesTheSessionAlone() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reading = ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java),
        )
        val store = PackStore(context)
        store.install("reciter-husary")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val playback = PlaybackController(context, scope)
        val database = ContentDatabase.open(context, PackCatalog.load(context), store.installed())
        playback.attach(database)
        // A place the reader left behind before the word was asked for: the
        // sample must neither write it nor wipe it (owner decision, this
        // session).
        val place = PlaybackPosition("husary", 7, 1_234L)
        withContext(Dispatchers.IO) { PlaybackPositionStore(context).write(place) }
        val (watcher, future) = connect(context)
        try {
            assertTrue(
                "the word must start",
                withContext(Dispatchers.Main) { playback.hearWord(1, 1) },
            )
            assertTrue(
                "the sheet must be told which word sounds",
                awaitState(playback, 5_000) { it.sample != null },
            )
            assertTrue(
                "the word must end by itself",
                awaitState(playback, 15_000) { it.sample == null },
            )
            // A word is not a session: the session's player never held it, so
            // there is no item, no capsule, and no notification left standing
            // for one word (owner decision, this session).
            assertEquals(
                "a word must never stand in the session's playlist",
                0,
                readMain { watcher.mediaItemCount },
            )
            assertTrue(
                "a word must not start the session's player",
                readMain { !watcher.playWhenReady },
            )
            assertNull("a sample must leave no ayah marked", readMain { playback.state.value.ayahNumber })
            assertEquals(
                "a word's end is not the listening place",
                place,
                withContext(Dispatchers.IO) { PlaybackPositionStore(context).read() },
            )
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                watcher.pause()
                watcher.clearMediaItems()
                watcher.stop()
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                playback.release()
            }
            release(watcher, future)
            withContext(Dispatchers.IO) { PlaybackPositionStore(context).clear() }
            scope.cancel()
            reading.close()
        }
    }

    /**
     * A word heard over a recitation: the session waits at the word and is
     * given back at its last millisecond, with the playlist it was playing
     * still whole. The word is the fourth of Al-Fatiha's first ayah, which is
     * the ayah's long one, so the pause has a window wide enough to read.
     */
    @Test
    fun aWordHeardOverARecitationWaitsAndGivesItBack() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reading = ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java),
        )
        val store = PackStore(context)
        store.install("reciter-husary")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val playback = PlaybackController(context, scope)
        val database = ContentDatabase.open(context, PackCatalog.load(context), store.installed())
        playback.attach(database)
        val (watcher, future) = connect(context)
        try {
            withContext(Dispatchers.Main) { playback.play("husary", 1, resume = false) }
            assertTrue(
                "the recitation must start",
                await(watcher, 20_000) { playing, _ -> playing },
            )
            assertTrue(
                "the word must start",
                withContext(Dispatchers.Main) { playback.hearWord(1, 4) },
            )
            assertTrue(
                "the recitation must wait for the word",
                await(watcher, 5_000) { playing, _ -> !playing },
            )
            assertTrue(
                "the word must end by itself",
                awaitState(playback, 15_000) { it.sample == null },
            )
            assertTrue(
                "the recitation must be given back after the word",
                await(watcher, 10_000) { playing, _ -> playing },
            )
            assertEquals(
                "the word must not replace the reader's playlist",
                7,
                readMain { watcher.mediaItemCount },
            )
        } finally {
            withContext(Dispatchers.Main) { playback.stop() }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                watcher.pause()
                watcher.clearMediaItems()
                watcher.stop()
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                playback.release()
            }
            release(watcher, future)
            withContext(Dispatchers.IO) { PlaybackPositionStore(context).clear() }
            scope.cancel()
            reading.close()
        }
    }

    private fun connect(context: Context): Pair<MediaController, ListenableFuture<MediaController>> {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val main = ContextCompat.getMainExecutor(context)
        val controllerRef = AtomicReference<MediaController>()
        val connected = CountDownLatch(1)
        val future = AtomicReference<ListenableFuture<MediaController>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val built = MediaController.Builder(context, token).buildAsync()
            future.set(built)
            built.addListener(
                {
                    controllerRef.set(built.get())
                    connected.countDown()
                },
                main,
            )
        }
        assertTrue("the controller must connect to the service", connected.await(20, TimeUnit.SECONDS))
        return controllerRef.get() to future.get()
    }

    private fun release(controller: MediaController, future: ListenableFuture<MediaController>) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            MediaController.releaseFuture(future)
        }
    }

    /** Waits for the controller's own state to say [condition]. */
    private fun awaitState(
        playback: PlaybackController,
        timeoutMs: Long,
        condition: (PlaybackUiState) -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (readMain { condition(playback.state.value) }) return true
            Thread.sleep(50)
        }
        return false
    }

    /** The player's two facts, read on the main thread the player lives on. */
    private fun <T> readMain(block: () -> T): T {
        val out = AtomicReference<T>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { out.set(block()) }
        return out.get()
    }

    /** Waits for the player to say [condition], handing it its facts each turn. */
    private fun await(
        controller: MediaController,
        timeoutMs: Long,
        condition: (playing: Boolean, position: Long) -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val playing = readMain { controller.playWhenReady }
            val position = readMain { controller.currentPosition }
            if (condition(playing, position)) return true
            Thread.sleep(100)
        }
        return false
    }

}
