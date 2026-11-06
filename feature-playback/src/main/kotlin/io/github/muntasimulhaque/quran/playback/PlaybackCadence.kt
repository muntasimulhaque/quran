package io.github.muntasimulhaque.quran.playback

/**
 * What the player's timer does at one moment.
 *
 * The wash is drawn from the reading's own frame clock now, so the player
 * keeps no wash tick at all: a second clock ten times a second was a source
 * of lag, not precision, and it grew when the reader sped the reciter up. The
 * checkpoint is the only timer left. It writes the heard place for the
 * resume, and it is worth its cost only while the reading is hidden. A paused
 * player needs neither (owner decision, this session).
 */
internal data class PlaybackCadence(
    /** Whether the ten-second place checkpoint should run. */
    val checkpoint: Boolean,
)

/** The one cadence rule: the place while hidden, nothing while paused. */
internal fun playbackCadence(isPlaying: Boolean, visible: Boolean): PlaybackCadence =
    PlaybackCadence(checkpoint = isPlaying && !visible)
