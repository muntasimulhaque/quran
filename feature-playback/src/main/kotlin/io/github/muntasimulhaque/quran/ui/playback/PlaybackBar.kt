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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.data.DownloadError
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.kit.AppPopup
import io.github.muntasimulhaque.quran.ui.kit.PopupAlignment
import io.github.muntasimulhaque.quran.ui.kit.DownloadLine
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.uikit.R as UiKitR

/**
 * The playback pill. It speaks in five voices: asking to download a surah,
 * reporting progress, reporting a failure, playing, and answering that the
 * recitation is not there. Downloading only ever starts from the reader's own
 * tap on Download, and only for one surah.
 *
 * **The playing pill is one row, and the offer is its sibling** (owner
 * decision, this session). A recitation's capsule is one 64 dp row: the words'
 * two-line lockup at the leading end, play or pause at the trailing end. It
 * used to be two rows of 48, the words over a transport row, and the second
 * row was 144 dp of circles centered in 328: the reader saw a capsule twice as
 * tall as the words it carried, with an empty band in it (owner report, this
 * session). 64 is the height the pill already stands at in its one-row voices
 * (8 + the finger's 48 + 8), and the offer is one row at 64 too: the place
 * line gives way at its size before the surah's name, and Download is the one
 * answer. The transfer keeps 10 dp more for its progress line. The capsule is
 * one height in every voice that asks a question, and the only taller state
 * is a transfer with a line to show.
 *
 * **The one row carries one mark, because the place never gives way** (owner
 * decision, this session). Three 48 dp targets take 144 dp of the capsule's
 * 328 and leave the words 148; the longest place with its key and its turn
 * needs 149 dp in Bangla at the default type and 174 at the top step, so three
 * marks cut the place. Play or pause is the mark that stays, and Previous ayah
 * and Next ayah are the actions it offers beside the tap, where Stop already
 * stands: a screen reader keeps all three in the pill, and the media
 * notification keeps them for everyone. See `pillControls`.
 *
 * **The playing words are one block, one door, and two lines.** The voice and
 * its pace stand over the place and its key, in the transfer's own lockup: a
 * name over a state, at the transfer's 2 dp inset. One line cannot hold the
 * four facts at the top step in either language, and the lockup pays nothing for the
 * second line because the block is as wide as its wider line, not the sum of
 * both: one line of the four facts is 247 dp in Bangla at the default type,
 * where 236 are there. The panel it opens is one panel: the voices, the pace,
 * and what happens at the end of the audio. The order is the offer's own grammar, who
 * is reading before where, and now it is the reading order of a lockup too.
 * The voices that are not a recitation keep one line, the name beside the
 * state's sentence, and their name opens the voice list alone, because the
 * pace and the end answer a recitation that is not there (owner decision, this
 * session).
 *
 * **The capsule is one measure, and it never changes it** (owner decision,
 * this session). Its width used to follow its words: about 285 dp at the
 * shortest places, the page's own 328 at the longest, about 230 for an offer,
 * and about 210 for a transfer that widened again as the percent did. A
 * control that widens as a download moves throws its own edge around under
 * the reader's eye, and the progress line at its foot changes length with
 * it. So every voice, the playing capsule, the offer, the transfer, the
 * failure, and the unavailable answer, now stands at [PillMeasure], clamped
 * by the glass, and the words and the answer keep their own ends while the
 * ground between them takes what the words do not need. The capsule has one
 * width to be at, so it snaps to nothing and animates nothing.
 *
 * **The voice leads, the place follows, and the key never gives way** (owner
 * decision, this session). The lockup's first line is the chosen voice at 13
 * in the accent, with the pace at 13 in the quiet ink after it when it is not
 * 1x; its second line is the surah's own name at the card's 14 Medium, the
 * ayah's key at 13 in the ink, and the chevron that closes it. The order is
 * the offer's: who, then which surah, then the ayah. The pill's type follows
 * the system scale only up to the app's largest step, 1.2, and stops there.
 * The place and the key keep their ends on their own line, the pace keeps the
 * voice's, and the voice is the only part that gives way, because the key says
 * which ayah and the name says who. The reading and the panel still follow the
 * reader's scale.
 *
 * **The pace lives in the listening panel, and the pill reads it back**
 * (owner decision, this session). The panel is where the pace is set, and
 * its own step row says the chosen value; the pill says it again beside the
 * voice when it is not 1x, so the reader who closed the panel still knows how
 * fast the recitation is. The pill says who, where, and how fast; the panel
 * also says what happens at the end.
 *
 * **Nothing touches.** The playing words keep [PillEndAir] at the leading end,
 * because a capsule's end is a sphere, and the transport keeps [PillAir] at the
 * trailing end; between them, the words' block takes its own room and the
 * answer's circle keeps [GlyphLead] of target air, which is [WordsGap] between
 * the shapes the reader sees. On the one-row
 * voices the words keep [PillEndAir] at their own end, the answer keeps
 * [PillAir] at its end, and [WordsGap] stands between the two. On the
 * unavailable pill, which is words alone, both ends keep [PillEndAir], so
 * the sentence is held as the name is (owner decision, this session).
 *
 * The pill is also where its popup hangs. A menu takes the shape of the anchor
 * it is given, and an anchor on the word the reader touched stood the reciter
 * chooser at one end of the capsule and the listening menu at the other (owner
 * report): two popups in two places on one control. So the capsule holds the
 * menu, from one anchor on its own centre. The merged panel is taller than the
 * two menus were apart, so the popup caps itself to the window's safe band
 * and scrolls what does not fit. It takes the whole band when the ground
 * above the capsule cannot hold it, so the pace and the ending are not hidden
 * behind a scroll the window did not need, and the anchor stands on the
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
    /**
     * The playing ayah's key, surah:ayah, for the pill's own line. The pill
     * localizes its digits where it draws them.
     */
    reference: String?,
    /** The surah's name in the reader's language; null while it is not known. */
    placeName: String? = null,
    pendingLabel: String?,
    /**
     * The pending surah's name and size, said while its package downloads
     * on its own, whichever door asked for it: a download always names the
     * surah that is arriving. A reader who cannot tell what is being fetched
     * cannot tell whether it is the surah they wanted (owner report).
     */
    pendingAudio: String? = null,
    /** The reader's pace, drawn in the panel and read back on the pill when it is not 1x. */
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
            onReciter = onReciter,
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
        state.downloadFailed -> stringResource(
            if (state.downloadFailure == DownloadError.Storage) {
                UiKitR.string.failure_storage
            } else {
                UiKitR.string.failure_download
            },
        )
        downloading -> {
            val percent = (progress * 100).toInt()
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
        // The playing voice says its place in [PlayingWordsBlock]; this
        // branch is the row the other voices never see.
        else -> ""
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
    // leaving pause, speed, and the switches out of the keys means the panel
    // is not put away by the very state it just changed. The answer itself
    // closes it below, so one tap both changes the thing and returns the
    // reader to the reading (owner decision, this session).
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
            // One measure in every voice, never the words' (owner decision,
            // this session). The capsule stands at the page's own 328 on the
            // app's reference phone and clamps to a narrower glass, so a
            // download's percent and an offer's size can never move its edge.
            .widthIn(max = PillMeasure)
            .fillMaxWidth()
            .floatingLift(Radii.Pill)
            .clip(Radii.Pill)
            // The playback bar floats over the reading, so it wears the
            // floating tone and a soft lift: the page is visible around it
            // and under it, and it has to read as above the page.
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag("playback-bar"),
        // The capsule's own content stands on the capsule's middle. One
        // measure is held whatever the voice, and the row it holds is
        // centered on that measure rather than hanging from its leading edge.
        contentAlignment = Alignment.Center,
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(
                    bottom = if (progress != null) ProgressRoom else 0.dp,
                ),
            ) {
                if (playing) {
                    // The playing capsule is one 64 dp row: the words' lockup
                    // at the leading end, play or pause at the trailing end.
                    // The words keep [PillEndAir] at the leading end, where the
                    // capsule is a sphere, and the answer keeps [PillAir] at
                    // the trailing one. The lockup is content-sized inside the
                    // room that is left, so a long place would ellipsize rather
                    // than push the answer out; the capsule's measure and the
                    // answer's one mark are chosen so that it never has to
                    // (owner decision, this session).
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = PillEndAir,
                                end = PillAir,
                                top = PillAir,
                                bottom = PillAir,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            PlayingWordsBlock(
                                placeName = placeName,
                                reciter = reciterName,
                                reference = playbackPlace(reference.orEmpty()),
                                speed = speed,
                                onOpen = { openMenu = PillMenu.Listening },
                                expanded = openMenu == PillMenu.Listening,
                                modifier = Modifier.testTag("playback-words"),
                            )
                        }
                        if (controls.isNotEmpty()) {
                            // The first circle keeps [WordsGap] from the words'
                            // own shape: a glyph answer draws its circle
                            // [TransportInset] inside its target, so its lead
                            // gives that inset back (owner decision, this
                            // session).
                            Spacer(Modifier.width(GlyphLead))
                            PillControls(controls)
                        }
                    }
                } else {
                    Row(
                        // The whole measure belongs to the row, so the words
                        // lead with [PillEndAir] at one end, the answer keeps
                        // [PillAir] at the other, and the ground the words do
                        // not need stands between them rather than past the
                        // answer (owner decision, this session).
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = PillEndAir,
                                end = if (controls.isEmpty()) PillEndAir else PillAir,
                                top = PillAir,
                                bottom = PillAir,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // No recitation, so one line, and its name opens the
                        // voice list alone: the pace and the end answer a
                        // recitation that is not there.
                        PlaybackWordsLine(
                            reciterName = reciterName,
                            onOpenReciters = openReciters,
                            status = status,
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .testTag("playback-words"),
                            announce = state.error || state.downloadFailed || state.unavailable,
                            expanded = openMenu == PillMenu.Reciters,
                        )
                        if (controls.isNotEmpty()) {
                            // The answer's own shape keeps the app's air. A
                            // glyph answer draws its circle [TransportInset]
                            // inside its target, so its lead gives the inset
                            // back and the reader sees [WordsGap] either way;
                            // a word answer ends at its own edge and takes the
                            // gap whole (owner decision, this session).
                            Spacer(
                                Modifier.width(
                                    if (controls.first() is GlyphControl) GlyphLead else WordsGap,
                                ),
                            )
                            PillControls(controls)
                        }
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
                            .padding(horizontal = Space.Block)
                            .padding(bottom = Space.Line)
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
            // pill's own middle (owner report). The anchor is always on the
            // page, so it is measured before a menu is asked for, and the
            // popup itself is composed only while one is open.
            Box(Modifier.matchParentSize()) {
                PillMenuAnchor { anchor ->
                    val place = placeName?.takeIf { it.isNotBlank() }
                    val fullPlace = when {
                        place == null -> null
                        state.ayahNumber != null -> localizedDigits(
                            stringResource(
                                R.string.playback_now_playing,
                                place,
                                state.ayahNumber,
                            ),
                        )
                        else -> "$place ${playbackPlace(reference.orEmpty())}"
                    }
                    if (openMenu != null) AppPopup(
                        anchor = anchor,
                        onDismiss = { openMenu = null },
                        alignment = PopupAlignment.Center,
                        // The place is the panel's pane title, spoken and
                        // never drawn: the capsule under the panel already
                        // says it, and the voice list keeps no title.
                        name = fullPlace?.takeIf { openMenu == PillMenu.Listening },
                    ) {
                        PillMenuBody(
                            openMenu = openMenu,
                            options = reciterOptions,
                            selected = reciterId,
                            speed = speed,
                            end = end,
                            onDismiss = { openMenu = null },
                            onReciter = onReciter,
                            onSpeed = onSpeed,
                            onEndOfAudio = onEndOfAudio,
                        )
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
