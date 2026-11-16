package io.github.muntasimulhaque.quran.playback

import io.github.muntasimulhaque.quran.core.RecitationPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The pill's key, pinned with the numbers that made it wrong: 16:28 is the
 * Book's 1929th ayah, and 2:255 is its 262nd. The key is the verse's own
 * number, never the running one (owner report, this session).
 */
class PlayingPlaceTest {

    @Test
    fun theKeyIsTheVersesOwnNumberNotTheRunningOne() {
        val place = playingPlace(RecitationPlaylist.mediaId(1929, 16), 28)
        assertEquals(16, place?.surah)
        assertEquals(28, place?.ayahInSurah)
        assertEquals("16:28", place?.reference)
    }

    @Test
    fun alBaqarahsTwoHundredFiftyFifthReadsItsOwnNumber() {
        val place = playingPlace(RecitationPlaylist.mediaId(262, 2), 255)
        assertEquals("2:255", place?.reference)
    }

    @Test
    fun alFatihahReadsTheSameInBothNumbers() {
        val place = playingPlace(RecitationPlaylist.mediaId(1, 1), 1)
        assertEquals("1:1", place?.reference)
    }

    @Test
    fun anItemWithoutTheAyahsOwnNumberNamesNothing() {
        assertNull(playingPlace(RecitationPlaylist.mediaId(1929, 16), null))
    }

    @Test
    fun anIdThatIsNotOursNamesNothing() {
        assertNull(playingPlace("a-file-name.mp3", 28))
    }
}
