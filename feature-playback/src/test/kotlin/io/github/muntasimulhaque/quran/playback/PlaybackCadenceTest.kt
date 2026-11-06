package io.github.muntasimulhaque.quran.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackCadenceTest {

    @Test
    fun `a visible reading keeps no player timer`() {
        assertEquals(
            PlaybackCadence(checkpoint = false),
            playbackCadence(isPlaying = true, visible = true),
        )
    }

    @Test
    fun `a hidden reading keeps the place`() {
        assertEquals(
            PlaybackCadence(checkpoint = true),
            playbackCadence(isPlaying = true, visible = false),
        )
    }

    @Test
    fun `a paused reading has no timer at all`() {
        assertEquals(
            PlaybackCadence(checkpoint = false),
            playbackCadence(isPlaying = false, visible = true),
        )
        assertEquals(
            PlaybackCadence(checkpoint = false),
            playbackCadence(isPlaying = false, visible = false),
        )
    }
}
