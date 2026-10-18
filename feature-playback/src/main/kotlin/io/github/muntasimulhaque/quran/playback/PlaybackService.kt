package io.github.muntasimulhaque.quran.playback

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.uikit.R as UiKitR
import java.util.Locale

/**
 * Keeps recitation playing with the screen off. Media3 owns the notification
 * and the foreground service; the app only hands it a player and a session.
 *
 * The one thing the controller cannot reach on its own is the player's
 * pause-at-end, which the stop answer needs: `setPauseAtEndOfMediaItems` is an
 * `ExoPlayer` method and not one the `Player` interface a `MediaController`
 * speaks carries. It therefore travels as a custom session command that the
 * session advertises to the controller on connect, and answers in
 * `onCustomCommand` (owner decision).
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    /**
     * The service's own words follow the reader's language too. The media
     * library names the notification's channel and its transport controls,
     * and those strings resolve against the context the service carries; it
     * carries the reader's own locale, the same way the activity does, so a
     * Bangla reading gets a Bangla notification even on a phone whose system
     * language is English (owner decision, this session).
     */
    override fun attachBaseContext(newBase: Context) {
        val tag = LanguagePreference(newBase).tag()
            ?: return super.attachBaseContext(newBase)
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate() {
        super.onCreate()
        // The notification wears the app's own mark in the status bar, the
        // same silhouette the daily reminder wears, instead of the library's
        // generic one (owner decision, this session). The channel is left as
        // the library names it: a new channel id would leave every reader who
        // already has the old one with two entries in the phone's settings.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .build()
                .apply { setSmallIcon(UiKitR.drawable.ic_daily) },
        )
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult {
                // The reader's own app is the only controller, and it is the
                // one command it carries beyond the library's own.
                val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                    .buildUpon()
                    .add(SessionCommand(COMMAND_PAUSE_AT_END, Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(commands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == COMMAND_PAUSE_AT_END) {
                    player.setPauseAtEndOfMediaItems(args.getBoolean(ARG_PAUSE_AT_END))
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }
        session = MediaSession.Builder(this, player).setCallback(callback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        /** The custom action that carries the stop answer to the player. */
        const val COMMAND_PAUSE_AT_END = "io.github.muntasimulhaque.quran.PAUSE_AT_END"

        /** The command's one argument: true to stop at the end of each ayah. */
        const val ARG_PAUSE_AT_END = "pause_at_end"
    }
}
