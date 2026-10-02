package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState

/**
 * The playback pill. It speaks in four voices: asking to download a surah,
 * reporting progress, reporting a failure, and playing. Downloading only ever
 * starts from the reader's own tap on Download, and only for one surah.
 */
@Composable
fun PlaybackBar(
    state: PlaybackUiState,
    offer: ListenOffer?,
    offerTitle: String,
    reciterName: String,
    /** The reciter in use, by id, so the chooser can put its check on it. */
    reciterId: String? = null,
    /**
     * The reciters the chooser offers, with what each would still need for
     * the ayah the pill is about. Empty where there are none to offer, and
     * [onReciter] null where the choice cannot be made now, which is while a
     * package is on its way.
     */
    reciterOptions: List<ListenOption> = emptyList(),
    reference: String?,
    pendingLabel: String?,
    /**
     * The pending surah's name and size, said while its package downloads
     * on its own: the auto-continue path had no offer, so this is where the
     * reader sees what is arriving (owner decision). The offer states
     * do not need it; they carried the name and the size before the tap.
     */
    pendingAudio: String? = null,
    /** The reader's pace, shown only when it is not the ordinary one. */
    speed: Float = 1f,
    /** What happens at the end of the audio, set from the pill's own menu. */
    end: EndOfAudio = EndOfAudio.OFF,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReciter: (String) -> Unit,
    onDownload: () -> Unit,
    onClose: () -> Unit,
    onOfferConfirm: () -> Unit = {},
    onOfferCancel: () -> Unit = {},
    onOfferReciter: (String) -> Unit = {},
    /** The pace, and the end of the audio, set from the pill exactly as from
     * settings. */
    onSpeed: (Float) -> Unit = {},
    onEndOfAudio: (EndOfAudio) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (offer != null) {
        ListenOfferBar(
            offer = offer,
            title = offerTitle,
            onConfirm = onOfferConfirm,
            onCancel = onOfferCancel,
            onReciter = onOfferReciter,
            modifier = modifier.padding(horizontal = BarGutter),
        )
        return
    }
    val downloading = state.downloadProgress != null
    val needsDownload = state.pendingDownloadSurah != null && !downloading
    val playing = !downloading && !needsDownload && !state.downloadFailed && !state.unavailable
    // The words are what give when the line is long, never the gutter and
    // never the controls: the reciter's name is one line and the state under
    // it one more, and each ellipsizes rather than push the controls off the
    // row. What a row cannot hold at all is [oneRowFloor]'s question, and
    // the second branch below is its answer.
    val status = when {
        state.downloadFailed -> stringResource(R.string.playback_download_failed)
        downloading -> {
            val percent = ((state.downloadProgress ?: 0f) * 100).toInt()
            if (state.pendingIsContinuation && !pendingAudio.isNullOrBlank()) {
                stringResource(R.string.playback_downloading_named, pendingAudio, percent)
            } else {
                stringResource(R.string.playback_downloading, percent)
            }
        }
        needsDownload -> pendingLabel.orEmpty()
        state.unavailable -> stringResource(R.string.playback_unavailable)
        else -> playbackStatus(reference.orEmpty(), speed)
    }
    val controls = pillControls(
        state = state,
        downloading = downloading,
        needsDownload = needsDownload,
        onToggle = onToggle,
        onNext = onNext,
        onPrevious = onPrevious,
        onDownload = onDownload,
        onClose = onClose,
    )
    // The reciter is a choice, not a hop: while a package is on its way there
    // is nothing to choose between, and a list with nothing in it is not a
    // door, so the door is closed rather than open and inert.
    val chooseReciter = if (downloading || reciterOptions.isEmpty()) null else onReciter
    val listening = if (playing) onSpeed to onEndOfAudio else null
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = BarGutter)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            // The playback bar floats over the reading, so it wears the
            // floating tone and a soft lift: the page is visible around it
            // and under it, and it has to read as above the page.
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag("playback-bar"),
    ) {
        val oneRow = maxWidth >= oneRowFloor(controls.size)
        Column {
            if (oneRow) {
                Row(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 6.dp,
                        top = 6.dp,
                        bottom = 6.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaybackWords(
                        reciterName = reciterName,
                        reciterId = reciterId,
                        reciterOptions = reciterOptions,
                        onReciter = chooseReciter,
                        status = status,
                        onListening = listening,
                        speed = speed,
                        end = end,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(WordsGap))
                    PillControls(controls)
                }
            } else {
                // The words keep the whole width of the pill and the controls
                // sit under them. Both lines are centred on the pill,
                // and the words are one line rather than two: the reciter's
                // name stood over the place with the pill's own left edge
                // behind them both, so the reader's eye went to the corner of
                // a capsule to find out who was reading and where they were
                // (owner report). A row that says both, centred, with
                // the controls centred under it, is the shape a hand expects
                // of a control floating over the page.
                //
                // The inset is the capsule's own: the corner of a pill this
                // tall reaches further in than the corner of a 55 dp one, so
                // the lines keep clear of the curve at the top and the foot.
                PlaybackWordsLine(
                    reciterName = reciterName,
                    reciterId = reciterId,
                    reciterOptions = reciterOptions,
                    onReciter = chooseReciter,
                    status = status,
                    onListening = listening,
                    speed = speed,
                    end = end,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
                PillControls(
                    controls = controls,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    arrangement = Arrangement.Center,
                )
            }
            if (downloading) {
                LinearProgressIndicator(
                    progress = { state.downloadProgress ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                )
            }
        }
    }
}
