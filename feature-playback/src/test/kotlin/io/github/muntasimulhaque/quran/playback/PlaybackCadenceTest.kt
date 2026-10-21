package io.github.muntasimulhaque.quran.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackCadenceTest {

    @Test
    fun `the wash ticks only while the reading is on the glass`() {
        assertEquals(
            PlaybackCadence(tick = true, checkpoint = false),
            playbackCadence(isPlaying = true, visible = true),
        )
    }

    @Test
    fun `a hidden reading keeps the place without the tick`() {
        assertEquals(
            PlaybackCadence(tick = false, checkpoint = true),
            playbackCadence(isPlaying = true, visible = false),
        )
    }

    @Test
    fun `a paused reading has no timer at all`() {
        assertEquals(
            PlaybackCadence(tick = false, checkpoint = false),
            playbackCadence(isPlaying = false, visible = true),
        )
        assertEquals(
            PlaybackCadence(tick = false, checkpoint = false),
            playbackCadence(isPlaying = false, visible = false),
        )
    }
}
