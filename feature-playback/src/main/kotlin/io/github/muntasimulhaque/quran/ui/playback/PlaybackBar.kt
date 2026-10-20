package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
 * **The capsule hugs its words** (owner decision, this session). It used to
 * take the page's whole width, because a close's rail ran along both ends of
 * its words' line; with the close gone, that rail was empty ground inside a
 * shape that had no reason to be that wide. So the capsule is the wider of
 * its words' line and its transport, plus [PillEndAir] at the words' end and
 * [PillAir] at the transport's, and it never takes a pixel the reader is not
 * reading. On a 360 dp phone that is about 252 dp instead of 328, which is
 * also why the place is no longer cut with an ellipsis: the words' line has
 * the capsule's whole measure to itself, and the capsule grows with the words
 * up to the page's own edge when the reader's font scale or the language asks
 * for more room.
 *
 * The shape is measured by the layout and never by a number reserved for it:
 * a surah name, an ayah, and a pace are what the line is, and a capsule
 * fixed to the longest of them would be wider than the reader's own line for
 * almost every ayah. The one thing that would make it breathe pointlessly is
 * a download's percent, so the progress line's own room is added under the
 * row and the words keep their measure. The capsule snaps to a new width and
 * never animates it: a width that slid would turn a one dp change between
 * two ayahs into a performance over the page.
 *
 * The window's own measure still decides the playing shape: where the words
 * cannot keep [WordsMeasure] beside the transport, they take a line of their
 * own and the transport stands centred under them, on the capsule's own axis.
 * No other voice takes a second row: an offer, a failure, and a transfer's own
 * abort are one answer each, and one answer shares the words' line (owner
 * decision, this session: a transfer's capsule stood 130 dp tall to hold one
 * X under its words, where the offer bar beside it held the same shape in
 * 64).
 *
 * One air, [PillAir], is kept between the shapes the reader sees, and the
 * phone's two rows stand 108 dp tall (owner decision, this session): the
 * words' press pill is [WordsFlash] inside its 48 dp target, so the seam
 * between the flash and the transport is that target's own lower rim while
 * the two targets stand against one another. The words' end keeps
 * [PillEndAir], because a capsule's end is a sphere and the words' line sits
 * in its shoulder, where [PillAir] read as a name touching the border, and
 * [WordsCrown] keeps the capsule's crown from being a control.
 *
 * The pill is also where its two popups hang. A menu takes the shape of the
 * anchor it is given, and an anchor on the word the reader touched stood the
 * reciter chooser at one end of the capsule and the listening menu at the other
 * (owner report): two popups in two places on one control. So the capsule
 * holds both, from one anchor on its own centre, and the words only say which
 * of the two was asked for. The anchor stands on the capsule's whole measure
 * without ever deciding it, so a menu may be wider than the pill it answers
 * to and still hang from the pill's own middle.
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
    /**
     * The reader's own way out of a recitation. It has no shape on the glass:
     * it is the action TalkBack offers on Play or pause, where the close used
     * to stand (owner decision, this session).
     */
    onStop: () -> Unit,
    /** The one X the app keeps: abort a package on its way. */
    onCancelDownload: () -> Unit,
    onOfferConfirm: () -> Unit = {},
    onOfferCancelDownload: () -> Unit = {},
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
            onCancelDownload = onOfferCancelDownload,
            onReciter = onOfferReciter,
            modifier = modifier.padding(horizontal = BarGutter),
        )
        return
    }
    val progress = state.downloadProgress
    val downloading = progress != null
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
            val percent = ((progress ?: 0f) * 100).toInt()
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
        onStop = onStop,
        onCancelDownload = onCancelDownload,
    )
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
        // The capsule's own content stands on the capsule's middle. A hugged
        // capsule is its content exactly, and one held to a measure of its own
        // (a test's, or the page's cap at a large font scale) still centres
        // the row it holds rather than hanging it from the leading edge.
        contentAlignment = Alignment.Center,
    ) {
        val oneRow = !playing || maxWidth >= oneRowFloor(controls.size)
        Box {
            Column(
                // The capsule is the wider of its two rows, and neither row
                // touches the other's edges: the words' line and the
                // transport are each centred on the capsule's own axis.
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(
                    bottom = if (progress != null) ProgressRoom else 0.dp,
                ),
            ) {
                if (oneRow) {
                    Row(
                        // The words lead with [PillEndAir]: the capsule's end
                        // is a sphere, and at the words' line, a few dp above
                        // the middle, [PillAir] measured from the widest point
                        // reads as nothing. The transport keeps [PillAir] at
                        // its end, where it stands at the capsule's widest
                        // height and wears its own circles.
                        modifier = Modifier.padding(
                            start = PillEndAir,
                            end = PillAir,
                            top = PillAir,
                            bottom = PillAir,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The words take their own measure and the transport
                        // follows them; a weighted word box would spend the
                        // page's width on a capsule that no longer fills it.
                        PlaybackWordsLine(
                            reciterName = reciterName,
                            onOpenReciters = openReciters,
                            status = status,
                            onOpenListening = openListening,
                        )
                        Spacer(Modifier.width(WordsGap))
                        PillControls(controls)
                    }
                } else {
                    // The words keep the whole measure of the capsule and the
                    // controls stand under them, both centred. The words' flash
                    // is [WordsFlash] tall inside its 48 dp target, so the two
                    // targets stand against one another while the seam the
                    // reader sees still keeps [PillAir]: the air that used to
                    // sit between two 48 dp nodes now lives in the words'
                    // target's own rim, and the phone's rows stand 108 dp where
                    // they were 120 (owner decision, this session). The words'
                    // end keeps [PillEndAir] for the same shoulder the one-row
                    // pill answers to, and [WordsCrown] leaves the capsule's
                    // top edge inert.
                    PlaybackWordsLine(
                        reciterName = reciterName,
                        onOpenReciters = openReciters,
                        status = status,
                        onOpenListening = openListening,
                        modifier = Modifier.padding(
                            start = PillEndAir,
                            end = PillEndAir,
                            top = WordsCrown,
                            bottom = if (controls.isEmpty()) PillAir else 0.dp,
                        ),
                    )
                    // A voice with no answer of its own keeps no empty band:
                    // the notice is the words alone, and its capsule is one
                    // row tall (owner decision, this session).
                    if (controls.isNotEmpty()) {
                        PillControls(
                            controls = controls,
                            modifier = Modifier.padding(
                                start = PillAir,
                                end = PillAir,
                                bottom = PillAir,
                            ),
                        )
                    }
                }
            }
            // The progress is the app's one download line, drawn by hand
            // like every other shape on the page and shared with the offer
            // bar, so one download reads the same whichever pill asked for it
            // (owner decision, this session). It stands in the room the
            // capsule keeps under its row, so it never crosses a control's
            // press shape, and its measure is the capsule's, which the words
            // and not the percent decide.
            if (progress != null) {
                Box(Modifier.matchParentSize()) {
                    DownloadLine(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .height(2.dp)
                            // The line names itself so the air between it and
                            // the row above can be read back by a test: the
                            // promise is that the progress never touches a
                            // control's press shape.
                            .testTag("download-line"),
                    )
                }
            }
            // The two popups' anchor stands on the capsule's measure without
            // ever deciding it: a menu wider than the pill still hangs from
            // the pill's own middle (owner report).
            Box(Modifier.matchParentSize()) {
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
