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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import io.github.muntasimulhaque.quran.feature.playback.R as PlaybackR
import io.github.muntasimulhaque.quran.playback.AppMediaNotificationProvider
import io.github.muntasimulhaque.quran.playback.PlaybackService
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
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
 * The paused notification shows play, previous, and next. The middle
 * button's pause face is read from the same provider with the pause branch
 * chosen, because whether the phone grants audio focus is not the app's
 * decision: a test that starts real playback fails when another app holds
 * the focus, for a reason the app did not choose. The posted notification's
 * three marks are read from the shade itself.
 *
 * One run read a shade carrying the app's previous and play and nothing
 * else for the whole wait, then passed on the retry that named it. The marks
 * were the app's own, so the fault was not the drawing: the player had no
 * next item for the shade to offer. This test now starts from a cleared
 * shade and refuses to read the marks until the player itself holds both
 * items, and a timeout names the marks it saw instead of their ids
 * (owner report, 20.5).
 */
@RunWith(AndroidJUnit4::class)
class PlaybackNotificationTest {

    @Test
    fun thePostedNotificationWearsTheAppsOwnTransportMarks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)
        // A shade left over from an earlier class answers to the same
        // notification id, so the read starts from no media notification at
        // all: the service is stopped and the shade is cleared before the
        // controller asks for a session.
        manager.cancel(DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID)
        context.stopService(Intent(context, PlaybackService::class.java))
        val cleared = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < cleared && mediaNotification(manager, context) != null) {
            Thread.sleep(50)
        }
        assertTrue(
            "an earlier media notification was still in the shade",
            mediaNotification(manager, context) == null,
        )
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

            // The shade can only offer a next mark once the player holds a
            // second item, so that is a fact to wait for and not a guess:
            // a run that read the shade early saw previous and play alone
            // and spent its whole wait on a state the player never reached.
            val ready = SystemClock.uptimeMillis() + 10_000
            var items = 0
            var hasNext = false
            while (SystemClock.uptimeMillis() < ready) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    items = controller.mediaItemCount
                    hasNext = controller.hasNextMediaItem()
                }
                if (items == 2 && hasNext) break
                Thread.sleep(50)
            }
            assertEquals(
                "the player must hold both items before the shade is read",
                2,
                items,
            )
            assertTrue(
                "the first item must have a next item for the shade to offer",
                hasNext,
            )

            // The notification is rebuilt as the player's commands settle.
            // Waiting for the play mark alone read a list that still had only
            // previous and play, so the next mark was asserted before the
            // player had offered it (owner report, 17.9).
            val posted = awaitNotification(manager, context) { actions ->
                val icons = actions.map { it.getIcon().resId }
                PlaybackR.drawable.ic_transport_previous in icons &&
                    PlaybackR.drawable.ic_transport_play in icons &&
                    PlaybackR.drawable.ic_transport_next in icons
            }
            assertTheNotificationCarriesAppMarks(posted.notification.actions.map { it.getIcon().resId })

            assertThePauseMarkIsTheAppsOwn(context)
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
     * The action marks read from the posted notification: the app's previous,
     * play or pause, and next are all there, and every mark is one of the
     * app's own four.
     */
    private fun assertTheNotificationCarriesAppMarks(icons: List<Int>) {
        val appMarks = appMarks()
        assertTrue(
            "the notification must offer the app's previous mark: $icons",
            PlaybackR.drawable.ic_transport_previous in icons,
        )
        assertTrue(
            "the notification must offer the app's next mark: $icons",
            PlaybackR.drawable.ic_transport_next in icons,
        )
        assertTrue(
            "the notification must offer the app's play or pause mark: $icons",
            PlaybackR.drawable.ic_transport_play in icons ||
                PlaybackR.drawable.ic_transport_pause in icons,
        )
        assertTrue(
            "no action may wear a mark that is not the app's own: $icons",
            icons.all { it in appMarks },
        )
        assertFalse(
            "no action may wear a stock Media3 transport mark: $icons",
            icons.any { it in stockMarks() },
        )
    }

    /**
     * The pause face, read from the same provider the service sets, with the
     * pause branch chosen. Building the notification's pause state needs a
     * playing player, and the posting of it is the system's audio focus
     * decision; the provider's button list is the app's own answer and is
     * read here without a device sound.
     */
    private fun assertThePauseMarkIsTheAppsOwn(context: Context) {
        val buttons = AtomicReference<List<CommandButton>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val player = ExoPlayer.Builder(context).build()
            val session = MediaSession.Builder(context, player)
                .setId("playback-notification-test")
                .build()
            try {
                val commands = Player.Commands.Builder()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .build()
                buttons.set(
                    Inspector(context).buttons(session, commands, showPauseButton = true),
                )
            } finally {
                session.release()
                player.release()
            }
        }
        val marks = buttons.get()
        val pause = marks.firstOrNull { it.icon == CommandButton.ICON_PAUSE }
            ?: throw AssertionError("the provider must offer a pause button: $marks")
        assertEquals(
            "the pause mark is the app's own",
            PlaybackR.drawable.ic_transport_pause,
            pause.iconResId,
        )
        assertTrue(
            "every provider button must wear the app's own mark: " +
                marks.map { it.iconResId },
            marks.all { it.iconResId in appMarks() },
        )
    }

    private fun appMarks(): Set<Int> = setOf(
        PlaybackR.drawable.ic_transport_play,
        PlaybackR.drawable.ic_transport_pause,
        PlaybackR.drawable.ic_transport_next,
        PlaybackR.drawable.ic_transport_previous,
    )

    private fun stockMarks(): Set<Int> = setOf(
        androidx.media3.session.R.drawable.media3_icon_play,
        androidx.media3.session.R.drawable.media3_icon_pause,
        androidx.media3.session.R.drawable.media3_icon_next,
        androidx.media3.session.R.drawable.media3_icon_previous,
    )

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
        var last: List<String> = emptyList()
        while (SystemClock.uptimeMillis() < deadline) {
            val posted = mediaNotification(manager, context)
            if (posted != null) {
                val actions = posted.notification.actions.orEmpty().toList()
                last = actions.map { markName(it.getIcon().resId) +
                    ":" + (it.title?.toString() ?: "?") }
                if (wanted(actions)) return posted
            }
            Thread.sleep(100)
        }
        throw AssertionError(
            "the media notification never carried the wanted marks; " +
                "it last carried " + (if (last.isEmpty()) "no action at all" else last.joinToString(", ")),
        )
    }

    /** The app's media notification in the shade, or null when there is none. */
    private fun mediaNotification(
        manager: NotificationManager,
        context: Context,
    ): android.service.notification.StatusBarNotification? =
        manager.activeNotifications.firstOrNull {
            it.id == DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID &&
                it.packageName == context.packageName
        }

    /**
     * A transport mark by name, so a red run says which hand drew the shade
     * and which button was missing instead of printing resource numbers.
     */
    private fun markName(id: Int): String = when (id) {
        PlaybackR.drawable.ic_transport_previous -> "app previous"
        PlaybackR.drawable.ic_transport_play -> "app play"
        PlaybackR.drawable.ic_transport_pause -> "app pause"
        PlaybackR.drawable.ic_transport_next -> "app next"
        androidx.media3.session.R.drawable.media3_icon_previous -> "library previous"
        androidx.media3.session.R.drawable.media3_icon_play -> "library play"
        androidx.media3.session.R.drawable.media3_icon_pause -> "library pause"
        androidx.media3.session.R.drawable.media3_icon_next -> "library next"
        else -> "unknown 0x" + Integer.toHexString(id)
    }

    /** A second of silence, so the player reaches READY without a device sound. */
    private fun writeSilence(file: File) {
        val sampleRate = 8_000
        val samples = sampleRate
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

    /**
     * The provider with its protected button list in reach, so the pause
     * branch can be read without starting the service.
     */
    private class Inspector(context: Context) : AppMediaNotificationProvider(context) {
        fun buttons(
            session: MediaSession,
            commands: Player.Commands,
            showPauseButton: Boolean,
        ): List<CommandButton> =
            getMediaButtons(session, commands, ImmutableList.of(), showPauseButton)
    }
}
