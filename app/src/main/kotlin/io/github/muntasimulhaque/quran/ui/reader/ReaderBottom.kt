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
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.theme.Radii

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
                        text = setup.pack.name,
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
                    modifier = Modifier.padding(start = 6.dp),
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
        if (playback.isAnything() || offer != null) {
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
                onOfferCancel = { viewModel.cancelListen() },
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
                onClose = {
                    if (playback.pendingDownloadSurah != null || playback.downloadFailed) {
                        viewModel.cancelDownload()
                    } else {
                        viewModel.stopPlayback()
                    }
                },
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

/**
 * What one of the pill's marks says about itself, in the app's own words. The
 * mark a reader cannot see is a mark they were never told about, and the
 * glyph's colour is not a sentence.
 */
@Composable
private fun markState(on: Boolean): String =
    stringResource(if (on) R.string.action_state_on else R.string.action_state_off)

/**
 * The ayah's own actions, on the pill an ayah raises when the reader taps it:
 * hear it, understand it, keep it, write on it, and send it out.
 *
 * The order is the reader's intent, not the control's kind (owner decision,
 * this session). The two reasons to open an ayah's actions lead: hear it (Play) and
 * understand it (Tafsir), the deep door this reading is built around. Then the
 * two that keep the ayah, Save before Note, because the keep is the lighter
 * tap and the note is the writing: Save lands where it stands, and Note opens
 * a sheet and a keyboard. A note keeps its ayah in Saved and never lights the
 * bookmark (below), so the keep is the fact and the note is what is written on
 * it, and a parent before its child reads in the order the eye lands (owner
 * decision, forty-sixth session). Share stands last: it is the only action
 * that leaves the app, the least common, and the one a stray tap is most costed
 * by, so it takes the far edge, away from where the thumb rests.
 *
 * Tafsir sits in the second slot when the reader shows tafsir and is absent
 * otherwise; Play is always first, Save is always before Note, and Share is
 * always last, so the three fixed actions never move and only the deep door
 * comes and goes with the reader's own choice.
 *
 * Each action carries its own mark, and a mark says the tap that made it: the
 * note glyph is lit by a note and the bookmark by a Save, never the one by the
 * other. The two marked actions also say their state out loud, because a lit
 * glyph is a color and color is not something a reader who cannot see it is
 * told (owner report).
 */
@Composable
private fun AyahActions(
    isSaved: Boolean,
    hasNote: Boolean,
    showTafsir: Boolean,
    onSave: () -> Unit,
    onPlay: () -> Unit,
    onNote: () -> Unit,
    onMore: () -> Unit,
    onShare: () -> Unit,
) {
    val shape = Radii.Pill
    Row(
        modifier = Modifier
            .floatingLift(shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextAction(stringResource(R.string.action_play), Icon.Play, onPlay)
        if (showTafsir) {
            TextAction(
                label = stringResource(R.string.action_tafsir),
                icon = Icon.Tafsir,
                onClick = onMore,
            )
        }
        // The two marked actions say their state; the three that only answer a
        // tap say nothing, so nothing on the pill reads as a switch it is not.
        TextAction(
            label = stringResource(R.string.action_save),
            icon = if (isSaved) Icon.BookmarkFilled else Icon.Bookmark,
            onClick = onSave,
            active = isSaved,
            state = markState(isSaved),
        )
        TextAction(
            label = stringResource(R.string.action_note),
            icon = Icon.Note,
            onClick = onNote,
            active = hasNote,
            state = markState(hasNote),
        )
        TextAction(stringResource(R.string.action_share), Icon.Share, onShare)
    }
}
