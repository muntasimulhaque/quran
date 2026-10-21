package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * Everything that can sit at the foot of the page: the pack on its way in,
 * the ayah's own actions, and the playback pill, stacked in one place.
 *
 * It lives beside the reading screen rather than inside it because the
 * screen's own file is about the page: what the reader is reading, which
 * sheets are over it, and where a tap lands. This file is the one place the
 * foot of the page is built, so a change to the pill's stack is a change
 * here and never a hunt through a thousand lines of page.
 */

/** Everything that can sit at the foot of the page, stacked in one place. */
@Composable
internal fun BottomStack(
    viewModel: ReaderViewModel,
    playback: PlaybackUiState,
    /**
     * Whether the playback pill is on the page at all. A pill that is not
     * playing sound can be put away by a tap on the page or by Back, and that
     * is a stop: the paused ayah's mark comes from the playback state, so the
     * state is cleared with the capsule rather than only hidden. The rule
     * itself is [raisedAnswer], beside the screen.
     */
    showPlayback: Boolean,
    saved: List<SavedAyah>,
    selected: Ayah?,
    onPlay: (Ayah) -> Unit,
    onDeselect: () -> Unit,
    onNote: (Ayah) -> Unit,
    onRemoveSaved: (Ayah) -> Unit,
    onMore: (Ayah) -> Unit,
    onShare: (Ayah) -> Unit,
    onTouch: () -> Unit,
    onReciter: (String) -> Unit,
    reciterId: String,
    reciterName: String,
    reciterOptions: List<ListenOption>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        viewModel.packSetup?.let { setup ->
            Row(
                modifier = Modifier
                    .floatingLift(Radii.Pill)
                    .clip(Radii.Pill)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.padding(end = 12.dp)) {
                    Text(
                        text = contentPackName(setup.pack.id, setup.pack.name),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = when {
                            setup.failed -> stringResource(R.string.pack_download_failed)
                            setup.progress == null -> stringResource(R.string.reader_pack_preparing)
                            else -> stringResource(
                                R.string.reader_pack_downloading,
                                (setup.progress * 100).toInt(),
                                formatBytes(setup.pack.bytes),
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (setup.failed) {
                    TextButton(
                        label = stringResource(R.string.action_retry),
                        onClick = { viewModel.installPack(setup.pack.id) },
                    )
                }
                TextButton(
                    label = stringResource(
                        if (setup.failed) R.string.action_close else R.string.action_cancel,
                    ),
                    onClick = { viewModel.cancelPackSetup() },
                    // Retry and Close are two standalone actions, so they
                    // stand the action gap apart (owner decision, this
                    // session).
                    modifier = Modifier.padding(start = Space.Row),
                    quiet = true,
                )
            }
        }
        selected?.let { ayah ->
            val row = saved.firstOrNull { it.ayahNumber == ayah.number }
            AyahActions(
                // The two marks are the reader's own two hands: the note they
                // wrote, and the bookmark they pressed. A note keeps its ayah
                // in Saved, but it never lights the bookmark, so one tap of a
                // note does not claim to be a save (owner report).
                isSaved = row?.marked == true,
                hasNote = !row?.note.isNullOrBlank(),
                // The deeper door opens tafsir and nothing else, so a reader
                // who hid tafsir has nothing behind it and the action goes
                // with the door.
                showTafsir = viewModel.settings.showTafsir,
                onSave = {
                    // A save of the reader's own, carrying a note, asks
                    // before it goes, so the reader's own words are never
                    // removed by one tap. An ayah that only a note kept has
                    // no save to remove, and one tap of Save makes it.
                    if (row?.marked == true && !row.note.isNullOrBlank()) {
                        onRemoveSaved(ayah)
                    } else {
                        viewModel.toggleSaved(ayah)
                    }
                    onTouch()
                },
                onPlay = {
                    onPlay(ayah)
                    onDeselect()
                },
                onNote = { onNote(ayah) },
                onMore = { onMore(ayah) },
                onShare = { onShare(ayah) },
            )
        }

        val offer = viewModel.listenOffer
        if (showPlayback) {
            val pendingSurahName = playback.pendingDownloadSurah?.let { surah ->
                viewModel.surahs.firstOrNull { it.number == surah }?.nameSimple
                    ?: stringResource(R.string.surah_fallback_name, surah)
            }
            PlaybackBar(
                state = playback,
                offer = offer,
                offerTitle = offer?.let { pending ->
                    stringResource(R.string.playback_offer, pending.surahName, formatBytes(pending.bytes))
                }.orEmpty(),
                onOfferConfirm = { viewModel.confirmListen() },
                onOfferReciter = { viewModel.chooseListenReciter(it) },
                reciterName = reciterName,
                reciterId = reciterId,
                reciterOptions = reciterOptions,
                reference = playback.reference,
                // The button says Continue; the words name the surah and its
                // size, so the offer does not say the verb twice (owner
                // decision). The continuation and the plain offer read the
                // same way here, which is the point.
                pendingLabel = pendingSurahName?.let { name ->
                    stringResource(R.string.playback_offer, name, formatBytes(playback.pendingDownloadBytes))
                },
                pendingAudio = pendingSurahName?.let { name ->
                    stringResource(R.string.playback_offer, name, formatBytes(playback.pendingDownloadBytes))
                },
                onToggle = { viewModel.togglePlayback() },
                onNext = { viewModel.nextAyah() },
                onPrevious = { viewModel.previousAyah() },
                onReciter = onReciter,
                onDownload = {
                    // A failed player and a pending package share the pill's
                    // one Retry; the view model knows which the tap means.
                    if (playback.error) viewModel.retryPlayback() else viewModel.confirmDownload()
                },
                // The recitation's own stop has no shape on the glass: it is
                // the action TalkBack offers on Play or pause, and the page
                // and Back put a non-playing pill away (owner decision, this
                // session). The transfer keeps the app's one X, because a
                // stray tap must never throw a package away.
                onStop = { viewModel.stopPlayback() },
                onCancelDownload = { viewModel.cancelDownload() },
                onOfferCancelDownload = { viewModel.cancelListen() },
                speed = viewModel.settings.playbackSpeed,
                end = viewModel.settings.endOfAudio,
                onSpeed = { viewModel.setPlaybackSpeed(it) },
                onEndOfAudio = { viewModel.setEndOfAudio(it) },
            )
        }
    }
}

/** How anything playing or pending shows at the foot of the page. */
internal fun PlaybackUiState.isAnything(): Boolean =
    ayahNumber != null || unavailable || pendingDownloadSurah != null || downloadFailed || error
