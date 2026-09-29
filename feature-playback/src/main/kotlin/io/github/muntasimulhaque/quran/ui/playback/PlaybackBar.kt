package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.speedText
import kotlin.math.abs

/**
 * The gutter a floating control keeps from the glass. A pill that reaches
 * the screen's own edge stops reading as a control over the page and starts
 * reading as a sheet the app forgot to inset (owner report, D-090).
 */
private val BarGutter = 16.dp

/**
 * The measure the pill's words keep, or the words take a line of their own.
 *
 * 300 dp is the whole line the pill can print at its longest: the longest
 * surah name and its ayah, the slowest pace, and the surah repeat. Below
 * that the words share the row with four 48 dp controls and get about ninety
 * dp, which is fourteen characters at the size the status line is set: a
 * surah name and its ayah break across lines, the pace and the repeat are
 * cut with an ellipsis, and the reader is left with a name they cannot read
 * (owner report, D-119).
 *
 * So the pill is two rows wherever the words cannot keep that measure, and
 * one row where they can: a tablet, a landscape phone, and nowhere else. A
 * phone in portrait always takes the two rows, and pays for it in height.
 */
private val WordsMeasure = 300.dp

/**
 * What the pill's own row spends before the words get a pixel of it: the
 * row's padding, the gap, the controls, and the status line's chevron. Four
 * controls, the playing state, is 544 dp, which is a tablet and a landscape
 * phone and no phone in portrait.
 */
private fun oneRowFloor(controls: Int): Dp =
    RowInsets + WordsGap + (TransportSize * controls) + ChevronRoom + WordsMeasure

/** The row's own padding: 16 at the start, 6 at the end. */
private val RowInsets = 22.dp

/** The gap between the words and the controls, in both shapes. */
private val WordsGap = 12.dp

/**
 * One control's own target, which is the app's floor and what
 * `minimumInteractiveComponentSize` gives it. A Material release that widens
 * it has to widen this number with it, or the one-row shape is measured
 * against a row that no longer exists.
 */
private val TransportSize = 48.dp

/** The status line's own chevron: 12 for the mark, 4 before it, 2 after the text. */
private val ChevronRoom = 18.dp

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
    reference: String?,
    pendingLabel: String?,
    /**
     * The pending surah's name and size, said while its package downloads
     * on its own: the auto-continue path had no offer, so this is where the
     * reader sees what is arriving (owner decision, D-105). The offer states
     * do not need it; they carried the name and the size before the tap.
     */
    pendingAudio: String? = null,
    /** The reader's pace, shown only when it is not the ordinary one. */
    speed: Float = 1f,
    /** What happens at the end of the audio, and what the pill therefore says. */
    end: EndOfAudio = EndOfAudio.OFF,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReciter: () -> Unit,
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
        else -> playbackStatus(reference.orEmpty(), speed, end)
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
                        status = status,
                        onReciter = onReciter,
                        onListening = if (playing) onSpeed to onEndOfAudio else null,
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
                // The words keep the whole width of the pill, and the
                // controls sit under them, last mark at the end as they are
                // everywhere else in the app (D-119, and the settings tail in
                // D-120). The inset is the capsule's own: the corner of a
                // 98 dp pill reaches further in than the corner of a 55 dp
                // one, so the first line starts where the curve has cleared.
                PlaybackWords(
                    reciterName = reciterName,
                    status = status,
                    onReciter = onReciter,
                    onListening = if (playing) onSpeed to onEndOfAudio else null,
                    speed = speed,
                    end = end,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
                )
                PillControls(
                    controls = controls,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 6.dp),
                    arrangement = Arrangement.End,
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

/**
 * The pill's words: who is reading, and where the reader is, and, while a
 * recitation plays, the door to the pace and to what happens at the end.
 *
 * The reciter's name is the door to the reciter chooser and the line under
 * it is the door to the listening menu, which wears a chevron so it can be
 * found without a guess. Two doors on two lines, and the second one says its
 * own state to TalkBack whether or not it can fit the words as well.
 */
@Composable
private fun PlaybackWords(
    reciterName: String,
    status: String,
    onReciter: () -> Unit,
    /** The pace and the end of the audio, or null where there is nothing to hear. */
    onListening: Pair<(Float) -> Unit, (EndOfAudio) -> Unit>?,
    speed: Float,
    end: EndOfAudio,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onReciter)
            .testTag("playback-words"),
    ) {
        Text(
            text = reciterName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PlaybackStatusLine(
            text = status,
            onListening = onListening,
            speed = speed,
            end = end,
        )
    }
}

/**
 * The pill's second line: what the reader is hearing, and, while a recitation
 * plays, the door to the pace and the end of the audio. The door is the line
 * itself and it wears a chevron so it can be found without a guess; the
 * chevron is gone when there is nothing behind it.
 */
@Composable
private fun PlaybackStatusLine(
    text: String,
    onListening: Pair<(Float) -> Unit, (EndOfAudio) -> Unit>?,
    speed: Float,
    end: EndOfAudio,
) {
    // Read here, in the composable's own scope: a semantics lambda is not a
    // composable, and a stringResource inside one is a compile error.
    val repetition = repeatWord(end)
    var open by remember { mutableStateOf(false) }
    if (onListening == null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button) { open = true }
                .padding(end = 2.dp)
                .testTag("playback-listening")
                .semantics {
                    contentDescription = text
                    val spoken = buildList {
                        if (abs(speed - 1f) > 0.01f) add(speedText(speed))
                        if (end != EndOfAudio.OFF) add(repetition)
                    }.joinToString(", ")
                    if (spoken.isNotEmpty()) stateDescription = spoken
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(12.dp),
            )
        }
        ListeningMenu(
            open = open,
            onDismiss = { open = false },
            speed = speed,
            end = end,
            onSpeed = onListening.first,
            onEndOfAudio = onListening.second,
        )
    }
}

/**
 * What the pill says under the reciter's name while an ayah plays: where the
 * reader is, and, only when it is not the ordinary answer, the pace and
 * which end of the audio repeats. A reader who set 1.5x a week ago and
 * forgot, or who turned a repeat on and then wondered why the reading would
 * not move on, reads the answer here instead of hunting through settings for
 * it (D-087, widened by D-118).
 */
@Composable
private fun playbackStatus(
    reference: String,
    speed: Float,
    end: EndOfAudio,
    loopingWord: Int? = null,
): String {
    val extras = buildList {
        // a word that is repeating is said first: it is the one thing about
        // the audio the reader did not expect, and the pill's whole job is to
        // say it
        if (loopingWord != null) add(stringResource(R.string.playback_repeating_word))
        if (abs(speed - 1f) > 0.01f) add(speedText(speed))
        if (end != EndOfAudio.OFF) add(repeatWord(end))
    }
    return (listOf(reference) + extras).filter { it.isNotBlank() }.joinToString(" \u00b7 ")
}

/** The word that names the end of the audio, the way the switches name it. */
@Composable
private fun repeatWord(end: EndOfAudio): String = when (end) {
    EndOfAudio.REPEAT_AYAH -> stringResource(R.string.playback_repeating_ayah)
    EndOfAudio.REPEAT_SURAH -> stringResource(R.string.playback_repeating_surah)
    EndOfAudio.CONTINUE, EndOfAudio.OFF -> ""
}
