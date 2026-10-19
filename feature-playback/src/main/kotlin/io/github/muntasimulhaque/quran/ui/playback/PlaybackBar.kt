package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.kit.DownloadLine
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The playback pill. It speaks in four voices: asking to download a surah,
 * reporting progress, reporting a failure, and playing. Downloading only ever
 * starts from the reader's own tap on Download, and only for one surah.
 *
 * The pill uses its width: the words lead and the state's own answers hold
 * the capsule's trailing rail, the close outermost, so the far end of the
 * capsule is not left empty and the dismiss never reads as another transport
 * control (owner decision, this session). On the phone's two rows the close
 * stands on the capsule's own middle, the place it wears in the offer and in
 * the wide pill's one row (owner report, this session). The phone's words
 * stand on that same middle: the close's rail is kept on both sides of them,
 * so the line is centred on the capsule the reader sees rather than in the
 * room left over beside the close (owner report, this session).
 *
 * The pill is also where its two popups hang. A menu takes the shape of the
 * anchor it is given, and an anchor on the word the reader touched stood the
 * reciter chooser at one end of the capsule and the listening menu at the other
 * (owner report): two popups in two places on one control. So the capsule
 * holds both, from one anchor on its own centre, and the words only say which
 * of the two was asked for.
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
     * on its own, whichever door asked for it: a download always names the
     * surah that is arriving. A reader who cannot tell what is being fetched
     * cannot tell whether it is the surah they wanted (owner report, this
     * session).
     */
    pendingAudio: String? = null,
    /** The reader's pace, shown only when it is not the ordinary one. */
    speed: Float = 1f,
    /** What happens as the reading moves on, set from the pill's own menu. */
    end: EndOfAudio = EndOfAudio.CONTINUE_AYAH,
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
    val playing = !downloading && !needsDownload && !state.downloadFailed && !state.error &&
        !state.unavailable
    // The words are what give when the line is long, never the gutter and
    // never the controls: the reciter's name is one line and the state under
    // it one more, and each ellipsizes rather than push the controls off the
    // row. What a row cannot hold at all is [oneRowFloor]'s question, and
    // the second branch below is its answer.
    val status = when {
        state.error -> stringResource(R.string.playback_failed)
        state.downloadFailed -> stringResource(R.string.playback_download_failed)
        downloading -> {
            val percent = (state.downloadProgress * 100).toInt()
            // The surah is always named while its package is on its way, not
            // only on the auto-continue path: a download the reader cannot
            // name is a download they cannot judge (owner report, this
            // session). The bare line is the fallback for a state that
            // carries no name to give.
            if (!pendingAudio.isNullOrBlank()) {
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
    // The close is the pill's trailing mark, not a member of the state's own
    // group: the bar lays it out at the capsule's own trailing edge in every
    // state and both shapes, so a dismiss keeps one place on the glass and
    // never reads as another transport control (owner decision, this
    // session).
    val close = controls.closeControl()
    val leadingControls = controls.filterNot { it is GlyphControl && it.kind == Transport.Close }
    // The reciter is a choice, not a hop: while a package is on its way there
    // is nothing to choose between, and a list with nothing in it is not a
    // door, so the door is closed rather than open and inert.
    val chooseReciter = if (downloading || reciterOptions.isEmpty()) null else onReciter
    // The capsule holds the two menus, so the words answer a tap with the
    // name of one rather than each raising a sheet of its own.
    //
    // The menu belongs to the pill that opened it, so it closes when the pill
    // becomes a different pill: when its voice changes from playing to an
    // offer or a download, and when the reading moves to another ayah or
    // another surah. Keying the remember on those is what closes it, and
    // leaving pause, speed, and the switches out of the keys is what keeps it
    // open while the reader is using it (owner report: a menu stayed over the
    // continuation pill that had replaced the one it belonged to).
    val voice = when {
        state.error -> PillVoice.Failed
        state.downloadFailed -> PillVoice.Failed
        downloading -> PillVoice.Downloading
        needsDownload -> PillVoice.Offer
        state.unavailable -> PillVoice.Unavailable
        else -> PillVoice.Playing
    }
    var openMenu by remember(voice, state.ayahNumber, state.surah) {
        mutableStateOf<PillMenu?>(null)
    }
    val openReciters: (() -> Unit)? = if (chooseReciter == null) {
        null
    } else {
        { openMenu = PillMenu.Reciters }
    }
    val openListening: (() -> Unit)? = if (playing) {
        { openMenu = PillMenu.Listening }
    } else {
        null
    }
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = BarGutter)
            .floatingLift(Radii.Pill)
            .clip(Radii.Pill)
            // The playback bar floats over the reading, so it wears the
            // floating tone and a soft lift: the page is visible around it
            // and under it, and it has to read as above the page.
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag("playback-bar"),
    ) {
        val oneRow = maxWidth >= oneRowFloor(controls.size)
        Column {
            // Both popups hang from the capsule's own centre, so the reader's
            // eye is sent to one place on the control rather than to two. The
            // anchor is a box of the popup's own measure in the middle of the
            // pill, and the place it stands on the screen is read from there,
            // once, while the pill is laid out.
            PillMenuAnchor { anchor ->
                PillPopup(
                    open = openMenu != null,
                    anchor = anchor,
                    onDismiss = { openMenu = null },
                ) {
                    when (openMenu) {
                        PillMenu.Reciters -> ReciterChoices(
                            options = reciterOptions,
                            selected = reciterId,
                            onChoose = {
                                openMenu = null
                                onReciter(it)
                            },
                        )
                        PillMenu.Listening -> ListeningMenu(
                            speed = speed,
                            end = end,
                            onSpeed = onSpeed,
                            onEndOfAudio = onEndOfAudio,
                        )
                        null -> Unit
                    }
                }
            }
            if (oneRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = BarGutter,
                            end = CloseInset,
                            top = RowVertical,
                            bottom = RowVertical,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The words take the leading side and give way with an
                    // ellipsis; the state's own answers and the close hold
                    // the trailing rail, the close outermost (owner decision,
                    // this session: the wide pill used to hug its leading
                    // corner and leave the capsule's far end empty).
                    Box(modifier = Modifier.weight(1f)) {
                        PlaybackWordsLine(
                            reciterName = reciterName,
                            onOpenReciters = openReciters,
                            status = status,
                            onOpenListening = openListening,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(WordsGap))
                    PillControls(leadingControls)
                    close?.let { mark ->
                        PillControls(listOf(mark))
                    }
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
                // the line keeps clear of the curve at the top and the foot.
                // No vertical inset is kept over it: the doors' own 48 dp
                // targets give the line its height (owner decision, this
                // session).
                //
                // The close is the capsule's own trailing mark on its own
                // middle, the place it wears in the offer and in the wide
                // pill's one row: it belongs to the capsule, not to the
                // transport line, so it is drawn over the two rows rather
                // than sitting at the foot of the lower one. The lower row
                // keeps its own measure even where the state has no answer
                // to stand in it, so the pill is one shape in every voice
                // and the close one place (owner report, this session).
                //
                // The words stand on the capsule's own middle, the axis the
                // transport under them stands on, and the close's rail is
                // kept on both sides of the line. The rail is what keeps the
                // words' two 48 dp doors, and the highlights they draw, clear
                // of the close: with the close at the capsule's middle and no
                // rail on the leading side, the line was centred in the room
                // left over beside the close, which put it 24 dp off the axis
                // the reader reads it against (owner report, this session).
                // The rail is one measure on the two sides, so the line is
                // centred on the capsule and the close keeps its ground on
                // the longest reference the pill prints.
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        PlaybackWordsLine(
                            reciterName = reciterName,
                            onOpenReciters = openReciters,
                            status = status,
                            onOpenListening = openListening,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CloseRail),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = TransportSize + 6.dp)
                                .padding(bottom = 6.dp),
                        ) {
                            PillControls(
                                controls = leadingControls,
                                modifier = Modifier.align(Alignment.Center),
                                arrangement = Arrangement.Center,
                            )
                        }
                    }
                    close?.let { mark ->
                        PillControls(
                            controls = listOf(mark),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = CloseInset),
                        )
                    }
                }
            }
            if (downloading) {
                // The progress is the app's one download line, drawn by hand
                // like every other shape on the page and shared with the
                // offer bar, so one download reads the same whichever pill
                // asked for it (owner decision, this session).
                DownloadLine(
                    progress = state.downloadProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .height(2.dp),
                )
            }
        }
    }
}

/**
 * The face the pill is wearing, so a menu can tell when it has become another
 * pill. Playing is the reading itself; the rest are the states a pill takes
 * while a package is offered, fetched, or refused, each with its own words and
 * its own controls.
 */
private enum class PillVoice { Playing, Offer, Downloading, Failed, Unavailable }

/**
 * The anchor the pill's own popups hang from, on both of the bars it wears:
 * the capsule's own width, with the popup's measure centred on it and its top
 * edge as the line they drop from.
 *
 * The anchor and the popup are one measure on purpose, and the anchor is read
 * back to the caller so the popup is placed from the anchor's own place and
 * not from whatever the library would take from a box it cannot see the
 * bounds of (owner report). It draws nothing and takes one hairline of room,
 * because a box with no height has no bounds to read: a zero-height anchor
 * stands every popup at the window's own edge instead of the capsule's centre.
 * The playing capsule and the offer bar both stand it, so the one chooser
 * opens from the same place wherever it is asked.
 */
@Composable
internal fun PillMenuAnchor(content: @Composable (anchor: IntRect) -> Unit) {
    var anchor by remember { mutableStateOf(IntRect.Zero) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(PillMenuMeasure)
                .height(AnchorLine)
                .testTag("pill-menu-anchor")
                .onGloballyPositioned {
                    val box = it.boundsInWindow()
                    anchor = IntRect(box.left.toInt(), box.top.toInt(), box.right.toInt(), box.bottom.toInt())
                },
        ) { content(anchor) }
    }
}
