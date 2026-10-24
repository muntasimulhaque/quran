package io.github.muntasimulhaque.quran.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.uikit.R as UiKitR

/**
 * The media notification, wearing the app's own hand for the transport it
 * offers.
 *
 * The library draws its play, pause, next, and previous marks unless a
 * provider says otherwise, and stock Material triangles beside the app's
 * drawn capsule read as two hands in one shade. This keeps every other piece
 * of the library's notification: its channel, its small icon, its title and
 * artist, its foreground behavior, and its localized descriptions. Only the
 * four transport marks are replaced, by the same geometry the pill draws in
 * `ui-kit` (owner decision, 11.1).
 *
 * The buttons are the library's own list, mapped one by one, rather than a
 * list built here: a reader's media-button preferences or a future custom
 * action still arrive and still keep their command, their name, and their
 * place. A button that is not one of the four transports is returned
 * untouched.
 */
class AppMediaNotificationProvider(context: Context) :
    DefaultMediaNotificationProvider(context) {

    init {
        // The same silhouette the daily reminder wears in the status bar.
        setSmallIcon(UiKitR.drawable.ic_daily)
    }

    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        showPauseButton: Boolean,
    ): ImmutableList<CommandButton> {
        val buttons = super.getMediaButtons(
            session,
            playerCommands,
            mediaButtonPreferences,
            showPauseButton,
        )
        val dressed = ImmutableList.builder<CommandButton>()
        for (button in buttons) dressed.add(button.withAppMark())
        return dressed.build()
    }

    /**
     * One command button with the app's mark in place of the library's, or
     * the button itself when it is not a transport. Every field that decides
     * what the button does, what it is called, and where it sits is carried
     * over, so the replacement changes the drawing and nothing else.
     */
    private fun CommandButton.withAppMark(): CommandButton {
        val mark = when (icon) {
            CommandButton.ICON_PLAY -> R.drawable.ic_transport_play
            CommandButton.ICON_PAUSE -> R.drawable.ic_transport_pause
            CommandButton.ICON_NEXT -> R.drawable.ic_transport_next
            CommandButton.ICON_PREVIOUS -> R.drawable.ic_transport_previous
            else -> return this
        }
        val builder = CommandButton.Builder(icon)
            .setCustomIconResId(mark)
            .setDisplayName(displayName)
            .setEnabled(isEnabled)
            .setExtras(extras)
            .setSlots(*slots.toArray())
        val command = sessionCommand
        if (command != null) {
            builder.setSessionCommand(command, parameter)
        } else {
            builder.setPlayerCommand(playerCommand, parameter)
        }
        return builder.build()
    }
}
