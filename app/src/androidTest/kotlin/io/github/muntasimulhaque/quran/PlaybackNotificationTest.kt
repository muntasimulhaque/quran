package io.github.muntasimulhaque.quran

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import io.github.muntasimulhaque.quran.feature.playback.R as PlaybackR
import io.github.muntasimulhaque.quran.playback.PlaybackService
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shade wears the app's own transport, not the library's.
 *
 * The media notification is built by Media3 from the session's command
 * buttons, and a stock play triangle beside the app's drawn capsule is the
 * one place a reader sees another hand. This starts the real service, gives
 * it a real (silent) media item, and reads the notification the phone is
 * actually showing: every action icon must be one of the app's own four
 * drawables, and none may be one of the library's stock transport drawables.
 *
 * The paused state shows play, previous, and next; starting the recitation
 * rebuilds the same notification with pause, so both faces of the middle
 * button are read.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackNotificationTest {

    @Test
    fun thePostedNotificationWearsTheAppsOwnTransportMarks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)
        val media = File(context.cacheDir, "notification-silence.wav")
        writeSilence(media)

        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val main = ContextCompat.getMainExecutor(context)
        val futureRef = AtomicReference<ListenableFuture<MediaController>>()
        val controllerRef = AtomicReference<MediaController>()
        val connected = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val future = MediaController.Builder(context, token).buildAsync()
            futureRef.set(future)
            future.addListener(
                {
                    controllerRef.set(future.get())
                    connected.countDown()
                },
                main,
            )
        }
        assertTrue("the controller must connect to the service", connected.await(20, TimeUnit.SECONDS))
        val controller = controllerRef.get()

        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                // Two items, so both seek commands are available: the
                // notification offers previous and next around the play
                // button, which is the shape the reading gives it.
                controller.setMediaItems(
                    listOf("Al-Fatihah", "Al-Baqarah").map { title ->
                        MediaItem.Builder()
                            .setUri(Uri.fromFile(media))
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(title)
                                    .setArtist("Minshawi")
                                    .build(),
                            )
                            .build()
                    },
                )
                controller.prepare()
            }

            val paused = awaitNotification(manager, context) { actions ->
                actions.any { it.getIcon().resId == PlaybackR.drawable.ic_transport_play }
            }
            assertAppMarks(paused.notification.actions.map { it.getIcon().resId })

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.play()
            }
            val playing = awaitNotification(manager, context) { actions ->
                actions.any { it.getIcon().resId == PlaybackR.drawable.ic_transport_pause }
            }
            assertAppMarks(playing.notification.actions.map { it.getIcon().resId })
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.stop()
                controller.clearMediaItems()
                futureRef.get()?.let { MediaController.releaseFuture(it) }
            }
            context.stopService(Intent(context, PlaybackService::class.java))
            media.delete()
        }
    }

    /**
     * The action marks, read from the posted notification: the app's previous,
     * play or pause, and next are all there, and not one stock Media3 mark is.
     */
    private fun assertAppMarks(icons: List<Int>) {
        assertTrue(
            "the notification must offer the app's previous mark: $icons",
            icons.contains(PlaybackR.drawable.ic_transport_previous),
        )
        assertTrue(
            "the notification must offer the app's next mark: $icons",
            icons.contains(PlaybackR.drawable.ic_transport_next),
        )
        assertTrue(
            "the notification must offer the app's play or pause mark: $icons",
            icons.contains(PlaybackR.drawable.ic_transport_play) ||
                icons.contains(PlaybackR.drawable.ic_transport_pause),
        )
        val stock = setOf(
            androidx.media3.session.R.drawable.media3_icon_play,
            androidx.media3.session.R.drawable.media3_icon_pause,
            androidx.media3.session.R.drawable.media3_icon_next,
            androidx.media3.session.R.drawable.media3_icon_previous,
        )
        assertTrue(
            "no action may wear a stock Media3 transport mark: $icons",
            icons.none { it in stock },
        )
        assertFalse("the notification carries actions", icons.isEmpty())
    }

    /**
     * Waits for the media notification to carry the mark a state expects. The
     * notification is rebuilt in place, so the wait is on the action list and
     * not on a new notification id.
     */
    private fun awaitNotification(
        manager: NotificationManager,
        context: Context,
        wanted: (List<android.app.Notification.Action>) -> Boolean,
    ): android.service.notification.StatusBarNotification {
        val deadline = SystemClock.uptimeMillis() + 30_000
        var last: List<Int> = emptyList()
        while (SystemClock.uptimeMillis() < deadline) {
            val posted = manager.activeNotifications.firstOrNull {
                it.id == DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID &&
                    it.packageName == context.packageName
            }
            if (posted != null) {
                val actions = posted.notification.actions.orEmpty().toList()
                last = actions.map { it.getIcon().resId }
                if (wanted(actions)) return posted
            }
            Thread.sleep(100)
        }
        throw AssertionError("the media notification never carried the wanted mark: $last")
    }

    /** A half second of silence, so the player reaches READY without a device sound. */
    private fun writeSilence(file: File) {
        val sampleRate = 8_000
        val samples = sampleRate / 2
        val dataBytes = samples * 2
        val out = ByteArrayOutputStream()
        fun ascii(value: String) = out.write(value.toByteArray(Charsets.US_ASCII))
        fun le32(value: Int) {
            out.write(value and 0xFF)
            out.write((value shr 8) and 0xFF)
            out.write((value shr 16) and 0xFF)
            out.write((value shr 24) and 0xFF)
        }
        fun le16(value: Int) {
            out.write(value and 0xFF)
            out.write((value shr 8) and 0xFF)
        }
        ascii("RIFF")
        le32(36 + dataBytes)
        ascii("WAVE")
        ascii("fmt ")
        le32(16)
        le16(1)
        le16(1)
        le32(sampleRate)
        le32(sampleRate * 2)
        le16(2)
        le16(16)
        ascii("data")
        le32(dataBytes)
        out.write(ByteArray(dataBytes))
        file.writeBytes(out.toByteArray())
    }
}
