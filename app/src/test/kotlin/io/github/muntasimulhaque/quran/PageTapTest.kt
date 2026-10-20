package io.github.muntasimulhaque.quran

import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.reader.RaisedAnswer
import io.github.muntasimulhaque.quran.ui.reader.raisedAnswer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The pill wears no close any more (owner decision, this session), so a tap on
 * the page and the phone's own Back carry the whole of its dismissal rule.
 * The rule is arithmetic over the playback state, and the state is the only
 * thing it reads, so it is pinned here where a change fails the build instead
 * of at the price of an emulator run:
 *
 * 1. Sound coming out is never silenced by a stray tap: the page is a tap
 *    surface the size of the screen and the reader's thumb rests on it.
 * 2. A package on its way is never aborted by one either: the transfer's own
 *    X is the deliberate cancel, and it is the app's only remaining close.
 * 3. Everything else at the foot is put away together with its state, because
 *    a paused ayah carries the reading's own mark and hiding the capsule
 *    alone would strand that mark with no control left to clear it.
 */
class PageTapTest {

    private fun state(
        isPlaying: Boolean = false,
        ayahNumber: Int? = null,
        unavailable: Boolean = false,
        pendingSurah: Int? = null,
        progress: Float? = null,
        downloadFailed: Boolean = false,
        error: Boolean = false,
    ) = PlaybackUiState(
        connected = true,
        isPlaying = isPlaying,
        ayahNumber = ayahNumber,
        surah = if (ayahNumber == null) null else 1,
        unavailable = unavailable,
        pendingDownloadSurah = pendingSurah,
        downloadProgress = progress,
        downloadFailed = downloadFailed,
        error = error,
    )

    private fun offer(progress: Float?, failed: Boolean = false) = ListenOffer(
        reciter = "husary",
        reciterName = "Husary",
        surah = 2,
        surahName = "Al-Baqarah",
        ayah = 1,
        bytes = 22L * 1024 * 1024,
        options = listOf(ListenOption("husary", "Husary", 0L)),
        progress = progress,
        failed = failed,
    )

    @Test
    fun nothingRaisedAnswersNothing() {
        assertEquals(RaisedAnswer.Nothing, raisedAnswer(PlaybackUiState(), null))
    }

    @Test
    fun aPlayingRecitationIsNeverSilencedByATap() {
        assertEquals(
            RaisedAnswer.Nothing,
            raisedAnswer(state(isPlaying = true, ayahNumber = 2), null),
        )
    }

    @Test
    fun aPausedRecitationIsPutAwayWithItsState() {
        assertEquals(
            RaisedAnswer.StopRecitation,
            raisedAnswer(state(isPlaying = false, ayahNumber = 2), null),
        )
    }

    @Test
    fun aFailedPlayerIsPutAwayToo() {
        assertEquals(
            RaisedAnswer.StopRecitation,
            raisedAnswer(state(error = true, ayahNumber = 2), null),
        )
    }

    @Test
    fun anUnavailableAnswerIsPutAwayToo() {
        assertEquals(
            RaisedAnswer.StopRecitation,
            raisedAnswer(state(unavailable = true, ayahNumber = 2), null),
        )
    }

    @Test
    fun aPackageOnItsWayIsLeftToItsOwnX() {
        assertEquals(
            RaisedAnswer.Nothing,
            raisedAnswer(
                state(pendingSurah = 2, progress = 0.42f),
                null,
            ),
        )
    }

    @Test
    fun anOfferThePillCarriesIsAnsweredNotNow() {
        assertEquals(
            RaisedAnswer.CancelDownload,
            raisedAnswer(state(pendingSurah = 2), null),
        )
    }

    @Test
    fun aFailedDownloadIsPutAwayToo() {
        assertEquals(
            RaisedAnswer.CancelDownload,
            raisedAnswer(state(pendingSurah = 2, downloadFailed = true), null),
        )
    }

    @Test
    fun theOfferBarIsAnsweredNotNow() {
        assertEquals(RaisedAnswer.CancelOffer, raisedAnswer(PlaybackUiState(), offer(null)))
        assertEquals(
            RaisedAnswer.CancelOffer,
            raisedAnswer(PlaybackUiState(), offer(null, failed = true)),
        )
    }

    @Test
    fun theOfferBarsOwnTransferIsLeftToItsOwnX() {
        assertEquals(
            RaisedAnswer.Nothing,
            raisedAnswer(PlaybackUiState(), offer(0.42f)),
        )
    }
}
