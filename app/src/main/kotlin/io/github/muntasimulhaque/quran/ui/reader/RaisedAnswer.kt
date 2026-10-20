package io.github.muntasimulhaque.quran.ui.reader

import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.PlaybackUiState

/**
 * What the reading's page answers when it is tapped, and what the phone's own
 * Back answers, at the foot of the reading.
 *
 * The pill's close is gone (owner decision, this session), so a raised
 * control is put away the way every other raised control in this app is put
 * away: a tap on the page, or Back. That makes the table below the pill's
 * whole dismissal rule, which is why it is a pure function beside the screen
 * rather than a chain of conditions inside it: the rule is what the
 * [PageTapTest] pins, and a rule that lives in a lambda can only be read.
 *
 * Three lines hold it:
 *
 * 1. **Sound coming out is never silenced by a stray tap.** A playing
 *    recitation answers nothing; the page is a tap surface the size of the
 *    screen, and the reader's thumb rests on it while they read.
 * 2. **A package on its way is left to its own X.** Its abort is the one
 *    thing a stray tap must never do, and the X is the app's only remaining
 *    close, so the page and Back send nothing.
 * 3. **Everything else is put away and its state cleared with it.** A paused
 *    recitation carries the reading's own mark, so hiding the capsule alone
 *    would strand that mark with no control left to clear it; a failure and
 *    an unavailable answer are cleared the same way; the offer is answered
 *    "not now" exactly as its own close used to answer it.
 */
internal enum class RaisedAnswer {

    /** Nothing is raised that a tap or Back should put away. */
    Nothing,

    /** Put the recitation away, whether it stands paused or failed. */
    StopRecitation,

    /** Abort the package on its way, and forget the surah it was for. */
    CancelDownload,

    /** Answer the download offer with its own "not now". */
    CancelOffer,
}

/**
 * The answer the foot of the page gives, in the order the pill wears its
 * voices: the offer first, because it replaces the capsule while it stands,
 * then the failure, the transfer, the offer the pill carries itself, the
 * unavailable answer, and the transport.
 */
internal fun raisedAnswer(
    state: PlaybackUiState,
    offer: ListenOffer?,
): RaisedAnswer = when {
    // The offer bar speaks for the whole foot while it is up.
    offer != null -> if (offer.progress == null) {
        RaisedAnswer.CancelOffer
    } else {
        RaisedAnswer.Nothing
    }
    state.error -> RaisedAnswer.StopRecitation
    state.downloadFailed -> RaisedAnswer.CancelDownload
    state.downloadProgress != null -> RaisedAnswer.Nothing
    state.pendingDownloadSurah != null -> RaisedAnswer.CancelDownload
    state.unavailable -> RaisedAnswer.StopRecitation
    // Nothing was ever asked for: a fresh install, or a stop already taken.
    state.ayahNumber == null -> RaisedAnswer.Nothing
    state.isPlaying -> RaisedAnswer.Nothing
    else -> RaisedAnswer.StopRecitation
}
