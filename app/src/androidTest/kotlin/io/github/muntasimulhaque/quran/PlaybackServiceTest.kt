package io.github.muntasimulhaque.quran

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.playback.PlaybackService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The stop answer's one piece of wiring, pinned where it can be exercised: the
 * controller cannot set the player's pause-at-end itself, because the method is
 * an `ExoPlayer` method and not one the `Player` interface carries, so it
 * travels as a custom session command. This connects a real controller to the
 * real service and proves the command is both advertised and answered, which is
 * the half a compose test cannot see and the half that would leave the stop
 * switch a dead control if it broke.
 *
 * The command's own effect on the player is `setPauseAtEndOfMediaItems`, whose
 * behavior belongs to Media3 and is not reimplemented here.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackServiceTest {

    @Test
    fun theStopCommandIsAdvertisedAndAnswered() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val main = ContextCompat.getMainExecutor(context)

        val controllerRef = AtomicReference<MediaController>()
        val connected = CountDownLatch(1)
        val future = AtomicReference<com.google.common.util.concurrent.ListenableFuture<MediaController>>()
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
        val controller = controllerRef.get()

        try {
            val command = SessionCommand(PlaybackService.COMMAND_PAUSE_AT_END, Bundle.EMPTY)
            val advertised = AtomicReference(false)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                advertised.set(controller.availableSessionCommands.contains(command))
            }
            assertTrue(
                "the session must advertise the stop command it is asked for",
                advertised.get(),
            )

            val answered = CountDownLatch(1)
            val result = AtomicReference<SessionResult>()
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
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                future.get()?.let { MediaController.releaseFuture(it) }
            }
        }
    }
}
