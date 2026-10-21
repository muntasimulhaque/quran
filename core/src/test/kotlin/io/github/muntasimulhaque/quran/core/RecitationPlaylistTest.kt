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

    @Test
    fun anAdvanceInsideTheSurahIsNotItsEnd() {
        // The player reports an automatic advance at every ayah boundary,
        // and every ayah is one media item. Only the advance that arrives in
        // another surah may restart one (owner report, this session).
        assertNull(
            RecitationPlaylist.endedSurah(
                answer = EndOfAudio.REPEAT_SURAH,
                playing = 1,
                arrivingMediaId = RecitationPlaylist.mediaId(2, 1),
            ),
        )
    }

    @Test
    fun theAdvanceIntoTheNextSurahEndsTheOneBefore() {
        assertEquals(
            1,
            RecitationPlaylist.endedSurah(
                answer = EndOfAudio.REPEAT_SURAH,
                playing = 1,
                arrivingMediaId = RecitationPlaylist.mediaId(1, 2),
            ),
        )
    }

    @Test
    fun onlyTheSurahRepeatReadsTheEnd() {
        for (answer in EndOfAudio.entries - EndOfAudio.REPEAT_SURAH) {
            assertNull(
                "$answer must not restart a surah",
                RecitationPlaylist.endedSurah(
                    answer = answer,
                    playing = 1,
                    arrivingMediaId = RecitationPlaylist.mediaId(1, 2),
                ),
            )
        }
    }

    @Test
    fun anAdvanceWithNothingPlayingOrNothingArrivingIsNotAnEnd() {
        assertNull(
            RecitationPlaylist.endedSurah(
                answer = EndOfAudio.REPEAT_SURAH,
                playing = null,
                arrivingMediaId = RecitationPlaylist.mediaId(1, 2),
            ),
        )
        assertNull(
            RecitationPlaylist.endedSurah(
                answer = EndOfAudio.REPEAT_SURAH,
                playing = 1,
                arrivingMediaId = null,
            ),
        )
        assertNull(
            RecitationPlaylist.endedSurah(
                answer = EndOfAudio.REPEAT_SURAH,
                playing = 1,
                arrivingMediaId = "a-file-name.mp3",
            ),
        )
    }
}
