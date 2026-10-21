package io.github.muntasimulhaque.quran.playback

/**
 * What the player's timers do at one moment.
 *
 * The tick paints the word wash ten times a second, and it is worth its cost
 * only while the reading that shows the wash is on the glass. The checkpoint
 * writes the heard place for the resume, and it is worth its cost only while
 * the tick is off. A paused player needs neither (owner decision, this
 * session).
 */
internal data class PlaybackCadence(
    /** Whether the ten-times-a-second wash tick should run. */
    val tick: Boolean,
    /** Whether the ten-second place checkpoint should run. */
    val checkpoint: Boolean,
)

/** The one cadence rule: the wash while visible, the place while hidden, neither while paused. */
internal fun playbackCadence(isPlaying: Boolean, visible: Boolean): PlaybackCadence = when {
    !isPlaying -> PlaybackCadence(tick = false, checkpoint = false)
    visible -> PlaybackCadence(tick = true, checkpoint = false)
    else -> PlaybackCadence(tick = false, checkpoint = true)
}
