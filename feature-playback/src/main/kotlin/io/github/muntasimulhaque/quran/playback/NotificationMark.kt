package io.github.muntasimulhaque.quran.playback

import androidx.media3.session.CommandButton
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.ui.glyph.Icon

/**
 * The four transport marks the media notification draws, each named with the
 * app's own drawing and the vector file that carries it.
 *
 * The shade is the one surface the app cannot draw with Compose, so its marks
 * are vector files beside the media session. They are not a second hand: each
 * file carries the numbers [io.github.muntasimulhaque.quran.ui.glyph.Glyphs]
 * draws, on the same 24 unit square, and this table is where that is said, in
 * the app's own code rather than only in a test. The two shapes had already
 * drifted once, and nothing could see it (owner report, an earlier session).
 *
 * **The two skips live here and not on the pill** (owner decision, this
 * session). The pill became one row of 64 dp with one mark, because three 48 dp
 * targets cut the place the pill never cuts, so next and previous left the
 * glass and are the actions play or pause offers to a screen reader. They are
 * still the app's own drawings, still what the notification shows, and still
 * what the table means: a mark with no surface would be a mark the app does not
 * have.
 */
internal enum class NotificationMark(
    /** The app's own drawing, the one the pill draws at 24 dp. */
    val icon: Icon,
    /** The vector file the shade draws, carrying the same numbers. */
    val markResId: Int,
    /** The file's name, so a test can read it against the set. */
    val fileName: String,
) {
    Play(Icon.Play, R.drawable.ic_transport_play, "ic_transport_play.xml"),
    Pause(Icon.Pause, R.drawable.ic_transport_pause, "ic_transport_pause.xml"),
    Next(Icon.Next, R.drawable.ic_transport_next, "ic_transport_next.xml"),
    Previous(Icon.Previous, R.drawable.ic_transport_previous, "ic_transport_previous.xml"),
    ;

    internal companion object {
        /**
         * The mark a library command button asks for, or null when the button
         * is not one of the four transports. The mapping is the library's own
         * icon vocabulary, so a button the app does not know keeps its own
         * drawing and its own command (owner decision, an earlier session).
         */
        fun of(commandIcon: Int): NotificationMark? = when (commandIcon) {
            CommandButton.ICON_PLAY -> Play
            CommandButton.ICON_PAUSE -> Pause
            CommandButton.ICON_NEXT -> Next
            CommandButton.ICON_PREVIOUS -> Previous
            else -> null
        }
    }
}
