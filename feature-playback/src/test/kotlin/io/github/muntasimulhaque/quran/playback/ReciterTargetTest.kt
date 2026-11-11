package io.github.muntasimulhaque.quran.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class ReciterTargetTest {

    @Test
    fun `a voice is heard from the ayah playing now`() {
        assertEquals(
            "the audio moves on by itself, and the pill is a control over what is playing",
            5,
            reciterTarget(playing = 5, requested = 1),
        )
    }

    @Test
    fun `the ayah playing now wins over the one Play was pressed on`() {
        assertEquals(
            "a carried-on surah is still where the reader is, not the surah they started in",
            401,
            reciterTarget(playing = 401, requested = 1),
        )
    }

    @Test
    fun `an ayah asked for but not yet playing is the fallback`() {
        assertEquals(
            "a package on its way belongs to an ayah the player was never handed",
            2552,
            reciterTarget(playing = null, requested = 2552),
        )
    }

    @Test
    fun `nothing playing and nothing asked is nothing`() {
        assertEquals(0, reciterTarget(playing = null, requested = 0))
    }
}
