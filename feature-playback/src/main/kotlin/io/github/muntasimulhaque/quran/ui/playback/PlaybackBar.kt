package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
 * The playback pill. It speaks in five voices: asking to download a surah,
 * reporting progress, reporting a failure, playing, and answering that the
 * recitation is not there. Downloading only ever starts from the reader's own
 * tap on Download, and only for one surah.
 *
 * **The pill is one row, the offer's shape, in every voice** (owner decision,
 * this session). The words are a line of type over a line: the reciter's name
 * over the place while playing, and over the state's own sentence when there
 * is no recitation. The answer stands at the trailing end, one shape: the
 * transport while playing, Retry on a failure, the app's one X on a transfer,
 * the Download word on a request, and nothing at all on an answer the app
 * cannot give. The capsule is 64 dp tall, or 74 with a download's line under
 * it. Before this the playing pill stacked a words row over a transport row
 * and paid 120 dp for a line of words that fits beside the controls, and the
 * offer carried a second, shorter block, so one control wore two shapes.
 *
 * **The playing words are one block, and one door.** Its two lines are one
 * question, who and where, and the panel it opens is one panel: the voices,
 * the pace, and what happens at the end of the audio. The voices that are not
 * a recitation keep one line, the name beside the state's sentence, and their
 * name opens the voice list alone, because the pace and the end answer a
 * recitation that is not there (owner decision, this session). The offer makes
 * the same choice for the same reason.
 *
 * **The capsule hugs its words** (owner decision, an earlier session). It
 * used to take the page's whole width, because a close's rail ran along both
 * ends of its words' line; with the close gone, that rail was empty ground
 * inside a shape that had no reason to be that wide. So the capsule is the
 * wider of its words and its answer, plus [PillEndAir] at the words' end and
 * [PillAir] at the answer's, and it never takes a pixel the reader is not
 * reading. On a 360 dp phone the playing capsule is about 305 to 325 dp, and
 * the offer about 250. The shape is measured by the layout and never by a
 * number reserved for it; the capsule snaps to a new width and never animates
 * it, because a width that slid would turn a one dp change between two ayahs
 * into a performance over the page.
 *
 * The place is one line and gives way with an ellipsis before the name does.
 * The one-row shape gives it about 120 dp on a 360 dp phone, which holds
 * Al-Fatihah and Al-Mumtahanah and ends the longest Bangla name short (owner
 * decision, this session: the shape was chosen with that cost in view).
 *
 * **Nothing touches.** The words keep [PillEndAir] at their own end, because a
 * capsule's end is a sphere and an [PillAir] measured from its widest point
 * read as a name touching the border; the answer keeps [PillAir] at its end,
 * where it stands at the capsule's widest height or wears a round shape of its
 * own; and [WordsGap] stands between the two. On the unavailable pill, which
 * is words alone, both ends keep [PillEndAir], so the sentence is held as the
 * name is (owner decision, this session).
 *
 * The pill is also where its popup hangs. A menu takes the shape of the anchor
 * it is given, and an anchor on the word the reader touched stood the reciter
 * chooser at one end of the capsule and the listening menu at the other (owner
 * report): two popups in two places on one control. So the capsule holds the
 * menu, from one anchor on its own centre. The merged panel is taller than the
 * two menus were apart, so the popup caps itself to the ground above the
 * capsule and scrolls what does not fit, and the anchor stands on the
 * capsule's whole measure without ever deciding it.
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
     * the voice list is then not a door.
     */
    reciterOptions: List<ListenOption> = emptyList(),
    reference: String?,
    pendingLabel: String?,
    /**
     * The pending surah's name and size, said while its package downloads
     * on its own, whichever door asked for it: a download always names the
     * surah that is arriving. A reader who cannot tell what is being fetched
     * cannot tell whether it is the surah they wanted (owner report).
     */
    pendingAudio: String? = null,
    /** The reader's pace, shown only when it is not the ordinary one. */
    speed: Float = 1f,
    /** What happens as the reading moves on, set from the panel's own list. */
    end: EndOfAudio = EndOfAudio.CONTINUE_AYAH,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReciter: (String) -> Unit,
    onDownload: () -> Unit,
    /**
     * The reader's own way out of a recitation. It has no shape on the glass:
     * it is the action TalkBack offers on Play or pause, where the close used
     * to stand (owner decision, an earlier session).
     */
    onStop: () -> Unit,
    /** The one X the app keeps: abort a package on its way. */
    onCancelDownload: () -> Unit,
    onOfferConfirm: () -> Unit = {},
    onOfferCancelDownload: () -> Unit = {},
    onOfferReciter: (String) -> Unit = {},
    /** The pace, and the end of the audio, set from the panel exactly as from
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
    // The words are what give when the line is long, never the gutters and
    // never the answer: the name is one line, the place or the sentence is one
    // more, and each ellipsizes rather than push the answer off the row.
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
    // The voice list is a choice, not a hop: while a package is on its way
    // there is nothing to choose between, and a list with nothing in it is not
    // a door, so the door is closed rather than open and inert.
    val canChooseReciter = !downloading && reciterOptions.isNotEmpty()
    // The panel belongs to the pill that opened it, so it closes when the pill
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
    val openReciters: (() -> Unit)? = if (canChooseReciter) {
        { openMenu = PillMenu.Reciters }
    } else {
        null
    }
    Box(
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
        // still centres the row it holds rather than hanging it from the
        // leading edge.
        contentAlignment = Alignment.Center,
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(
                    bottom = if (progress != null) ProgressRoom else 0.dp,
                ),
            ) {
                Row(
                    // The words lead with [PillEndAir]: the capsule's end is a
                    // sphere, and at the name's line [PillAir] measured from
                    // the widest point reads as nothing. The words' other end
                    // keeps it too when the words are all there is; once an
                    // answer follows, the answer's own shape keeps [PillAir]
                    // at the capsule's widest height.
                    modifier = Modifier.padding(
                        start = PillEndAir,
                        end = if (controls.isEmpty()) PillEndAir else PillAir,
                        top = PillAir,
                        bottom = PillAir,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (playing) {
                        // The recitation exists, so its words are the offer's
                        // block, and its door is the whole listening panel.
                        WordsBlock(
                            name = reciterName,
                            title = status,
                            onOpen = { openMenu = PillMenu.Listening },
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag("playback-words"),
                        )
                    } else {
                        // No recitation, so one line, and its name opens the
                        // voice list alone: the pace and the end answer a
                        // recitation that is not there.
                        PlaybackWordsLine(
                            reciterName = reciterName,
                            onOpenReciters = openReciters,
                            status = status,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag("playback-words"),
                        )
                    }
                    if (controls.isNotEmpty()) {
                        Spacer(Modifier.width(WordsGap))
                        PillControls(controls)
                    }
                }
            }
            // The progress is the app's one download line, drawn by hand
            // like every other shape on the page and shared with the offer
            // bar, so one download reads the same whichever pill asked for it
            // (owner decision). It stands in the room the capsule keeps under
            // its row, so it never crosses a control's press shape, and its
            // measure is the capsule's, which the words and not the percent
            // decide.
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
            // The popup's anchor stands on the capsule's measure without ever
            // deciding it: a menu wider than the pill still hangs from the
            // pill's own middle (owner report).
            Box(Modifier.matchParentSize()) {
                PillMenuAnchor { anchor ->
                    PillPopup(
                        open = openMenu != null,
                        anchor = anchor,
                        onDismiss = { openMenu = null },
                    ) {
                        when (openMenu) {
                            // The whole of the listening, for a recitation
                            // that exists.
                            PillMenu.Listening -> ListeningPanel(
                                options = reciterOptions,
                                selected = reciterId,
                                onChoose = {
                                    openMenu = null
                                    onReciter(it)
                                },
                                speed = speed,
                                end = end,
                                onSpeed = onSpeed,
                                onEndOfAudio = onEndOfAudio,
                            )
                            // The voices alone, for a voice that has no
                            // recitation to set a pace for.
                            PillMenu.Reciters -> ReciterChoices(
                                options = reciterOptions,
                                selected = reciterId,
                                onChoose = {
                                    openMenu = null
                                    onReciter(it)
                                },
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
 * The anchor the pill's own popup hangs from, on both of the bars it wears:
 * the capsule's own width, with the popup's measure centred on it and its top
 * edge as the line they drop from.
 *
 * The anchor and the popup are one measure on purpose, and the anchor is read
 * back to the caller so the popup is placed from the anchor's own place and
 * not from whatever the library would take from a box it cannot see the
 * bounds of (owner report). It draws nothing and takes one hairline of room,
 * because a box with no height has no bounds to read: a zero-height anchor
 * stands every popup at the window's own edge instead of the capsule's centre.
 * The playing capsule and the offer bar both stand it, so the menu opens from
 * the same place wherever it is asked.
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
