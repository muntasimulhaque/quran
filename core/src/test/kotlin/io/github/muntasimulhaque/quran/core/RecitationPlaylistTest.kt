package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The playlist geometry the surah repeat is built on, pinned here because
 * the surah repeat is the one thing the player cannot do by itself and the
 * arithmetic behind it is the whole of what could be wrong with it.
 */
class RecitationPlaylistTest {

    private val fatihah = (1..7).map { RecitationPlaylist.mediaId(it, 1) }
    private val baqarah = (8..286).map { RecitationPlaylist.mediaId(it, 2) }
    private val playlist = fatihah + baqarah

    @Test
    fun anIdNamesBothItsAyahAndItsSurah() {
        val id = RecitationPlaylist.mediaId(255, 2)
        assertEquals(255, RecitationPlaylist.ayahOf(id))
        assertEquals(2, RecitationPlaylist.surahOf(id))
    }

    @Test
    fun anIdThatIsNotOursIsReadAsNothing() {
        assertNull(RecitationPlaylist.surahOf("a-file-name.mp3"))
        assertNull(RecitationPlaylist.ayahOf("a-file-name.mp3"))
        assertNull(RecitationPlaylist.ayahOf(""))
    }

    @Test
    fun theSurahStartsAtItsFirstAyah() {
        assertEquals(0, RecitationPlaylist.startOf(playlist, 1))
        assertEquals(7, RecitationPlaylist.startOf(playlist, 2))
    }

    @Test
    fun aSurahThePlaylistDoesNotHoldStartsNowhere() {
        assertEquals(-1, RecitationPlaylist.startOf(fatihah, 2))
        assertEquals(-1, RecitationPlaylist.startOf(emptyList(), 1))
    }

    @Test
    fun theStartIsTheFirstIndexNotTheShortest() {
        // A partial package leaves gaps, and a surah still begins at the
        // first ayah it has, never at a later one it happens to hold.
        val partial = listOf(
            RecitationPlaylist.mediaId(1, 1),
            RecitationPlaylist.mediaId(5, 1),
            RecitationPlaylist.mediaId(8, 2),
        )
        assertEquals(0, RecitationPlaylist.startOf(partial, 1))
        assertEquals(2, RecitationPlaylist.startOf(partial, 2))
    }
}
