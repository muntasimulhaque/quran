package io.github.muntasimulhaque.quran

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two custom commands the player keeps for the reading, pinned where they
 * can be exercised: the controller cannot set either itself, because both are
 * `ExoPlayer` facts and not things the `Player` interface a `MediaController`
 * speaks carries.
 *
 * The first is the stop after this ayah, which travels as a boolean. The
 * second is the word's own voice, which travels as a media millisecond and is
 * watched by the service: the sheet asks to hear one word and no more, and the
 * pause must land at the word's end, on the word that was asked for, and never
 * on a verse the reader has moved on to (owner decision, this session).
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
     * The word's stop, end to end, on real audio from the app's own assets:
     * it pauses at the millisecond it was given, it does not pause once it has
     * been taken off, and a pause from the reader's own transport takes it off
     * as well, so a word heard on the sheet can never leave a pause waiting in
     * the middle of a verse the reader is now hearing.
     */
    @Test
    fun theWordStopPausesAtItsOffsetAndAPauseTakesItOff() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audio = cacheAudio(context, "recitation/husary/001001.mp3", "word-stop.mp3")
        // The reading is on the glass before the audio starts: the phone grants
        // an audio focus request to the app that is in front of the reader,
        // and an app in the background is refused it by the audio service's
        // own hardening. Every real word is heard from the open reading, so
        // this is the state the command is asked for in (owner decision, this
        // session).
        val reading = ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java),
        )
        val (controller, future) = connect(context)
        try {
            assertTrue(
                "the session must advertise the word's stop",
                readMain {
                    controller.availableSessionCommands
                        .contains(SessionCommand(PlaybackService.COMMAND_PAUSE_AT, Bundle.EMPTY))
                },
            )
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.setMediaItem(MediaItem.fromUri(Uri.fromFile(audio)))
                controller.prepare()
                controller.play()
            }
            assertTrue(
                "the test audio must start playing",
                await(controller, 20_000) { playing, position -> playing && position > 0 },
            )

            // A pause at one second: the player stops there, not at the end.
            sendPauseAt(controller, context, 1_000L)
            assertTrue(
                "the word's stop must pause the player",
                await(controller, 15_000) { playing, _ -> !playing },
            )
            val stopped = readMain { controller.currentPosition }
            assertTrue(
                "the stop must land at its millisecond, not before it and not at the item's end: $stopped",
                stopped in 800L..3_000L,
            )

            // Taken off, the player runs past the old mark.
            sendPauseAt(controller, context, -1L)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.seekTo(0L)
                controller.play()
            }
            Thread.sleep(2_500)
            assertTrue(
                "a cleared stop must not pause the verse the reader went back to",
                readMain { controller.playWhenReady },
            )

            // Armed and then paused by the reader's own transport: the stop
            // goes with the pause, and a play after it is not cut short.
            sendPauseAt(controller, context, 1_500L)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.pause()
            }
            Thread.sleep(200)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.seekTo(0L)
                controller.play()
            }
            Thread.sleep(3_000)
            assertTrue(
                "a transport pause must take the word's stop off the player",
                readMain { controller.playWhenReady },
            )
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.pause()
                controller.clearMediaItems()
                controller.stop()
            }
            release(controller, future)
            reading.close()
        }
    }

    @Test
    fun theWordsOwnVoiceStopsAtItsEnd() = runBlocking {
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
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val main = ContextCompat.getMainExecutor(context)
        val watcherRef = AtomicReference<MediaController>()
        val connected = CountDownLatch(1)
        val built = MediaController.Builder(context, token).buildAsync()
        built.addListener(
            {
                watcherRef.set(built.get())
                connected.countDown()
            },
            main,
        )
        assertTrue("the watcher must connect", connected.await(20, TimeUnit.SECONDS))
        val watcher = watcherRef.get()
        try {
            assertTrue(
                "the word must start",
                withContext(Dispatchers.Main) { playback.hearWord("husary", 1, 1) },
            )
            assertTrue(
                "the word must start playing",
                await(watcher, 15_000) { playing, _ -> playing },
            )
            assertTrue(
                "the word must stop at its end",
                await(watcher, 5_000) { playing, _ -> !playing },
            )
            // The sample is over, so its player is over: no items, no capsule,
            // and no notification left standing for one word (owner decision,
            // this session).
            assertTrue(
                "the sample must take its player with it",
                await(watcher, 5_000) { _, _ ->
                    readMain { watcher.mediaItemCount == 0 }
                },
            )
            val state = readMain { playback.state.value }
            assertNull("a sample must leave no capsule", state.sample)
            assertNull("a sample must leave no ayah marked", state.ayahNumber)
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
                MediaController.releaseFuture(built)
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                playback.release()
            }
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

    private fun sendPauseAt(controller: MediaController, context: Context, atMs: Long) {
        val main = ContextCompat.getMainExecutor(context)
        val answered = CountDownLatch(1)
        val result = AtomicReference<SessionResult>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val sent = controller.sendCustomCommand(
                SessionCommand(PlaybackService.COMMAND_PAUSE_AT, Bundle.EMPTY),
                Bundle().apply { putLong(PlaybackService.ARG_PAUSE_AT_MS, atMs) },
            )
            sent.addListener(
                {
                    result.set(sent.get())
                    answered.countDown()
                },
                main,
            )
        }
        assertTrue("the session must answer the word's stop", answered.await(20, TimeUnit.SECONDS))
        assertEquals(SessionResult.RESULT_SUCCESS, result.get().resultCode)
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

    private fun cacheAudio(context: Context, asset: String, name: String): File {
        val file = File(context.cacheDir, name)
        if (file.isFile && file.length() > 0L) return file
        context.assets.open(asset).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }
}
