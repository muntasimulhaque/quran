package io.github.muntasimulhaque.quran.playback

import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.core.RecitationPlaylist
import io.github.muntasimulhaque.quran.core.RecitationQueue
import io.github.muntasimulhaque.quran.core.SurahEnding
import io.github.muntasimulhaque.quran.core.WordClip
import io.github.muntasimulhaque.quran.core.surahEnding
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.DownloadError
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PlaybackPosition
import io.github.muntasimulhaque.quran.data.PlaybackPositionStore
import io.github.muntasimulhaque.quran.data.RecitationAyah
import io.github.muntasimulhaque.quran.data.RecitationDownloader
import io.github.muntasimulhaque.quran.data.RecitationManifest
import io.github.muntasimulhaque.quran.data.RecitationStore
import io.github.muntasimulhaque.quran.data.SafePaths
import io.github.muntasimulhaque.quran.data.WordSegment
import io.github.muntasimulhaque.quran.data.downloadError
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.ui.kit.reciterName
import io.github.muntasimulhaque.quran.ui.theme.PaperBackground
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The reader's connection to the recitation service.
 *
 * The playlist is built one surah at a time, from the ayah files that are
 * actually on the device, and the next surah is appended as the reader
 * reaches the end of the current one. A media id is "ayah:surah", so the
 * player always knows where it is without another query, and the shape that
 * id gives the playlist lives in [RecitationPlaylist] where it can be
 * tested.
 *
 * What happens at the end of the audio is [EndOfAudio], one value, and the
 * surah repeat is the one answer the player cannot give by itself (owner
 * decision). The playlist holds one surah past the one being heard, because a
 * deliberate Next and Previous cross surahs; the answer is read at the
 * boundary, where [SurahEnding] says whether the reading rests there, begins
 * the surah again, or carries on into the next one.
 */
class PlaybackController(
    private val context: Context,
    private val scope: CoroutineScope,
    /**
     * Where the recitation was last heard. It is written as it is heard, so a
     * process that dies mid-ayah is answered by the rest of that ayah rather
     * than by its top.
     */
    private val positionStore: PlaybackPositionStore = PlaybackPositionStore(context),
) {

    private val store = RecitationStore(context)
    private val manifest = RecitationManifest.load(context)
    private val downloader = RecitationDownloader(context)
    private var content: ContentDatabase? = null
    private var controller: MediaController? = null
    private val queue = RecitationQueue()
    private var recitationId: String? = null

    /**
     * Every ayah's word regions, read once when the surah's playlist is
     * built and kept until the recitation or the reading ends. [itemsFor]
     * already asks the database for each ayah's [RecitationAyah], which
     * parses the regions, so keeping them costs a map insertion instead of a
     * second disk read and a blank wash at every ayah turn.
     */
    private val segmentsByAyah = java.util.concurrent.ConcurrentHashMap<Int, List<WordSegment>>()

    /** The voice edges of every shipped file, or null until they are read. */
    @Volatile
    private var edges: AudioEdges? = null

    private var checkpoint: Job? = null
    private var downloadJob: Job? = null
    private var requestedAyah: Int? = null

    /**
     * Whether the reading that draws the recited word is on the glass. The
     * wash is scheduled by the reading now, so the player keeps no
     * wash timer. This flag is what keeps the place checkpoint off while the
     * eyes are there (owner decision, this session).
     */
    private var tracking = false

    /**
     * The ayah the last written position belonged to, and when it was
     * written. They live here rather than being read back out of the store,
     * because the throttle has to know both without a disk read.
     */
    private var heardAyah: Int? = null
    private var heardWrittenAt = 0L

    /** The reader's pace, their answer as the reading moves on, applied on connect. */
    private var speed = 1f
    private var end = EndOfAudio.CONTINUE_AYAH

    /**
     * True while the reader's Stop after this ayah has parked the player at
     * the end of the item. A pause at the end is not [Player.STATE_ENDED], so
     * the player keeps its current item and would light the last ayah again on
     * the next event. This flag is what keeps the mark released and the pill
     * away until the reader asks for the audio back (owner decision).
     */
    private var stoppedAtAyahEnd = false

    /**
     * The word's own player: one clipped word, and nothing else.
     *
     * A word is not a session, so it does not borrow the session's player. The
     * one player carries the reader's playlist, its pill, its notification, and
     * the place it was heard at, and a glance at a word must not take any of
     * them away: a clip handed to that player would have to replace its items,
     * and the reader's recitation would be gone with the word (owner report,
     * this session).
     */
    private var sample: ExoPlayer? = null

    /**
     * True when the session was playing as the word was asked for, so a word
     * heard over a recitation gives it back at the word's last millisecond.
     * A recitation that was already paused was paused by the reader, and stays
     * as they left it (owner decision, this session).
     */
    private var resumeAfterSample = false

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    /**
     * The surahs' names in the reader's language, kept as each playlist is
     * built, so the pill's place line does not query the content database on
     * every player event. A name is read once per surah, and the map holds at
     * most the 114 names (owner decision, this version).
     */
    private val placeNames = mutableMapOf<Int, String>()

    fun attach(content: ContentDatabase) {
        this.content = content
        if (edges == null) {
            scope.launch {
                val loaded = withContext(Dispatchers.IO) { AudioEdges.load(context) }
                edges = loaded
            }
        }
        syncTicker()
    }

    /**
     * Tells the player whether the reading is on the glass.
     *
     * The wash is scheduled by the reading, so this flag is
     * only about the place: while the eyes are not there, the heard offset is
     * written on a ten-second checkpoint. That checkpoint is what keeps a
     * process that dies mid-ayah answered by the rest of the ayah rather than
     * by its top (owner decision, this session).
     */
    fun setWordTracking(visible: Boolean) {
        if (tracking == visible) return
        tracking = visible
        if (!visible) checkpointPlace()
        syncTicker()
    }

    /**
     * The word under the reciter at the player's own position, read at the
     * moment the frame is drawn.
     *
     * The reading calls this at each scheduled change, so the wash is as
     * fresh as the glass, and a speed change needs no correction: the position
     * is in media time and the player has already scaled it. The segments were
     * read with the playlist, so this is a map lookup and an arithmetic walk,
     * not a disk read on the frame.
     */
    fun heardWordNow(): Int? {
        val player = controller ?: return null
        if (player.playbackState == Player.STATE_ENDED) return null
        if (stoppedAtAyahEnd && !player.playWhenReady) return null
        val id = player.currentMediaItem?.mediaId ?: return null
        val ayahNumber = RecitationPlaylist.ayahOf(id) ?: return null
        val segments = segmentsByAyah[ayahNumber] ?: return null
        val recitation = recitationId ?: return null
        val edges = edges
        return recitedWordAt(
            segments = segments,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            firstSpeechMs = edges?.firstMs(recitation, ayahNumber) ?: NO_EDGE_MS,
            lastSpeechMs = edges?.lastMs(recitation, ayahNumber) ?: NO_EDGE_MS,
        )
    }

    /**
     * How long the wash may sleep before its next change, in wall
     * milliseconds.
     *
     * The reading schedules its next word instead of polling for it: the wash
     * changes only at a boundary, and every boundary is in the same table the
     * word itself comes from. The wait is media time divided by the reader's
     * pace, and a change of pace restarts the sleep, so 0.5x and 1.5x are both
     * on time (owner decision, this session).
     */
    fun millisToNextWord(): Long? {
        val player = controller ?: return null
        if (player.playbackState == Player.STATE_ENDED) return null
        if (stoppedAtAyahEnd && !player.playWhenReady) return null
        val id = player.currentMediaItem?.mediaId ?: return null
        val ayahNumber = RecitationPlaylist.ayahOf(id) ?: return null
        val segments = segmentsByAyah[ayahNumber] ?: return null
        val recitation = recitationId ?: return null
        val edges = edges
        val wait = nextWordChangeMs(
            segments = segments,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            firstSpeechMs = edges?.firstMs(recitation, ayahNumber) ?: NO_EDGE_MS,
            lastSpeechMs = edges?.lastMs(recitation, ayahNumber) ?: NO_EDGE_MS,
        ) ?: return null
        return (wait / speed.coerceAtLeast(0.1f)).roundToLong().coerceAtLeast(1L)
    }

    /**
     * The reader's Play, from any of its doors. A fresh play is the reader
     * asking for something else, so a package still on its way goes with it
     * rather than surfacing later as a surah that starts playing on its own.
     * The download's own continuation enters at [startPlayback]: it is not a
     * new request, and its job is the one this door would cancel.
     */
    suspend fun play(recitation: String, ayahNumber: Int, resume: Boolean = true) {
        // A real Play is a session, and a word sample is not one: the word's
        // clip goes first, and it does not give the old session back, because
        // the reader has just asked for this one (owner decision, this
        // session).
        finishSample(resume = false)
        downloadJob?.cancel()
        downloadJob = null
        // The pill's download voice goes at once, not at the end of the
        // connect: the progress writes are gated on the pending surah, so
        // clearing it here is what keeps a chunk from painting over the ayah
        // the reader has just asked for.
        _state.value = _state.value.copy(
            pendingDownloadSurah = null,
            pendingIsContinuation = false,
            downloadProgress = null,
            downloadFailed = false,
            downloadFailure = null,
        )
        startPlayback(recitation, ayahNumber, resume)
    }

    /**
     * Where Play lands once the reader's own request is the only one left.
     *
     * True when the player was handed the playlist and told to play. A surah
     * that is not here is a question for the reader, and this is the door that
     * asks it (owner decision, this session).
     */
    private suspend fun startPlayback(
        recitation: String,
        ayahNumber: Int,
        resume: Boolean,
    ): Boolean {
        val database = content ?: return false
        // A fresh play is the reader asking for the audio back, so the end
        // that Stop after this ayah reached is behind them now.
        stoppedAtAyahEnd = false
        val ayah = withContext(Dispatchers.IO) {
            database.ayah(ayahNumber)
        } ?: return false
        if (recitationId != recitation) segmentsByAyah.clear()
        recitationId = recitation
        requestedAyah = ayahNumber
        val items = withContext(Dispatchers.IO) { itemsFor(recitation, ayah.surah) }
        if (items.isEmpty()) {
            // Without the reciter's word timings there is no audio to point
            // at, and downloading the surah would not change that: say so
            // rather than offer a download that cannot help. A partial surah
            // lands here too, and that is a package the reader has not got:
            // it is offered rather than played in pieces (owner decision,
            // this session).
            val timings = withContext(Dispatchers.IO) {
                database.recitationAyah(recitation, ayah.number)
            }
            val packageToFetch = manifest.packageFor(recitation, ayah.surah)
                ?.takeIf { timings != null }
            _state.value = _state.value.copy(
                recitation = recitation,
                unavailable = packageToFetch == null,
                pendingDownloadSurah = if (packageToFetch == null) null else ayah.surah,
                pendingDownloadBytes = packageToFetch?.bytes ?: 0,
                pendingIsContinuation = false,
                downloadProgress = null,
                downloadFailed = false,
                downloadFailure = null,
            )
            return false
        }
        val index = items.indexOfFirst {
            it.mediaId == RecitationPlaylist.mediaId(ayah.number, ayah.surah)
        }
        if (index < 0) {
            // The surah is not fully on the device; ask before fetching it
            // (owner decision, this session).
            val packageToFetch = manifest.packageFor(recitation, ayah.surah)
            _state.value = _state.value.copy(
                recitation = recitation,
                unavailable = packageToFetch == null,
                pendingDownloadSurah = if (packageToFetch == null) null else ayah.surah,
                pendingDownloadBytes = packageToFetch?.bytes ?: 0,
                pendingIsContinuation = false,
                downloadProgress = null,
                downloadFailed = false,
                downloadFailure = null,
            )
            return false
        }
        val player = runCatching { connect() }.getOrElse { failure ->
            // A service the phone will not bind, or a session the controller
            // refuses, is the pill's own failed state with a Retry, never an
            // exception on its way out of the view model.
            android.util.Log.w(TAG, "the player could not be connected", failure)
            _state.value = _state.value.copy(
                connected = false,
                isPlaying = false,
                error = true,
            )
            return false
        }
        // The surah the playlist is loaded through is recorded before the
        // player is handed the playlist: a new playlist is reported to its
        // listeners before setMediaItems returns, and that report appends the
        // next surah from this value. Recording it after the call measured
        // the report from the old surah, which missed the next surah at a
        // start on a surah's last ayah and appended it twice at a start
        // inside the surah (owner report, this session).
        queue.load(ayah.surah)
        // The offset this ayah was last heard at, under this reciter. A
        // process that died mid-ayah is answered by the rest of it. A
        // different ayah, a different reciter, and a surah whose audio has
        // just been fetched all start at the beginning, because none of them
        // is the place the reader left.
        val startAt = if (resume) resumeOffset(recitation, ayahNumber) else 0L
        val started = runCatching {
            withContext(Dispatchers.Main) {
                player.setMediaItems(items, index, startAt)
                player.prepare()
                player.play()
            }
        }
        if (started.isFailure) {
            android.util.Log.w(TAG, "the recitation could not start", started.exceptionOrNull())
            _state.value = _state.value.copy(isPlaying = false, error = true)
            return false
        }
        _state.value = _state.value.copy(
            recitation = recitation,
            unavailable = false,
            error = false,
            pendingDownloadSurah = null,
            downloadProgress = null,
            downloadFailed = false,
            downloadFailure = null,
        )
        return true
    }

    /**
     * How far into [ayahNumber] the recitation was last heard, for this
     * reciter and no other.
     *
     * Zero when nothing is remembered, when the reader has since been heard
     * somewhere else, or when a different reciter was last heard: none of
     * those is the place the reader left, and a resume that drops them into
     * the middle of an ayah they never started is worse than no resume.
     */
    private suspend fun resumeOffset(recitation: String, ayahNumber: Int): Long {
        val remembered = withContext(Dispatchers.IO) { positionStore.read() } ?: return 0L
        if (remembered.recitation != recitation) return 0L
        if (remembered.ayahNumber != ayahNumber) return 0L
        return remembered.positionMs.coerceAtLeast(0L)
    }

    /**
     * Fetches one surah's audio package, verified, reporting the bytes as
     * they land. The app uses this when it is fetching a reciter's timings
     * too, so one tap can cover both.
     */
    suspend fun fetchSurahAudio(
        recitation: String,
        surah: Int,
        onProgress: (Float) -> Unit,
    ): DownloadError? {
        val packageToFetch = manifest.packageFor(recitation, surah) ?: return DownloadError.Other
        val folder = withContext(Dispatchers.IO) { folderFor(recitation, surah) } ?: return DownloadError.Other
        val result = downloader.download(packageToFetch, folder) { read, total ->
            onProgress(if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f)
        }
        return result.exceptionOrNull()?.downloadError()
    }

    /**
     * Downloads the pending surah, verifies it, and starts playing where the
     * reader asked.
     *
     * The progress belongs to the request the reader still holds. Cancel
     * clears the pending surah, and every write below is gated on it, so a
     * chunk that lands while the cancel is being answered cannot paint the
     * pill again with a transfer that is no longer anyone's (owner report,
     * this session).
     */
    fun confirmDownload() {
        val recitation = _state.value.recitation ?: recitationId ?: return
        val surah = _state.value.pendingDownloadSurah ?: return
        val ayah = requestedAyah ?: return
        val packageToFetch = manifest.packageFor(recitation, surah) ?: return
        downloadJob?.cancel()
        downloadJob = scope.launch {
            _state.update { current ->
                if (current.pendingDownloadSurah == surah) {
                    current.copy(downloadProgress = 0f, downloadFailed = false, downloadFailure = null)
                } else {
                    current
                }
            }
            val folder = withContext(Dispatchers.IO) { folderFor(recitation, surah) }
            if (folder == null) {
                failDownload(surah, DownloadError.Other)
                return@launch
            }
            val result = downloader.download(packageToFetch, folder) { read, total ->
                val progress = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else null
                _state.update { current ->
                    if (current.pendingDownloadSurah == surah) {
                        current.copy(downloadProgress = progress)
                    } else {
                        current
                    }
                }
            }
            if (result.isSuccess) {
                // The reader's Cancel cancels this coroutine and the
                // downloader passes that on, so this branch is only reached
                // while the request is still the one on screen. The check is
                // belt and braces for the one frame between a cancel and the
                // job noticing it.
                if (_state.value.pendingDownloadSurah != surah) return@launch
                _state.update {
                    it.copy(pendingDownloadSurah = null, downloadProgress = null)
                }
                // The audio has just been fetched, so the reader hears it from
                // its beginning and not from wherever they were last heard.
                startPlayback(recitation, ayah, resume = false)
            } else {
                failDownload(surah, result.exceptionOrNull()?.downloadError() ?: DownloadError.Other)
            }
        }
    }

    /** The failure line, for the request that is still on screen. */
    private fun failDownload(surah: Int, error: DownloadError) {
        _state.update { current ->
            if (current.pendingDownloadSurah == surah) {
                current.copy(downloadProgress = null, downloadFailed = true, downloadFailure = error)
            } else {
                current
            }
        }
    }

    /**
     * The reader's Retry after a failed playback. The same ayah goes through
     * the same door Play uses, so a file that is missing is offered or
     * fetched again rather than reported as a failure that cannot be
     * answered.
     */
    fun retryPlayback() {
        val ayah = requestedAyah ?: return
        val recitation = recitationId ?: return
        scope.launch { play(recitation, ayah) }
    }

    /**
     * The reader's Cancel. The job goes first, so the downloader's next chunk
     * sees the cancellation and the transfer stops; the state is cleared after
     * it, and every progress write is gated on the pending surah, so a chunk
     * already in flight cannot put the pill back (owner report, this session).
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.update { current ->
            current.copy(
                pendingDownloadSurah = null,
                pendingIsContinuation = false,
                downloadProgress = null,
                downloadFailed = false,
                downloadFailure = null,
            )
        }
    }

    /**
     * The surah ended, and the reader's answer carries on into the next one.
     *
     * The surah after this one is played when its audio is already on the
     * device, whether the playlist held it or not, and fetched only when it is
     * not. While a package is on its way the pill carries the surah's name,
     * its size, the progress, and the cancel, so nothing is fetched without a
     * line that says what it is (owner decision).
     *
     * This is reached only for an answer that carries on
     * ([SurahEnding.CARRY_ON]): a reading that rests at the surah's end never
     * offers the surah after it for a download it did not ask for (owner
     * report, this session).
     */
    private fun carryOnToTheNextSurah() {
        val surah = _state.value.surah
        val recitation = recitationId
        val next = surah?.plus(1)
        if (recitation == null || next == null || next > 114) {
            // Nothing follows the surah's end, so the recitation is over.
            clearRecitation()
            return
        }
        val packageToFetch = manifest.packageFor(recitation, next)
        if (packageToFetch == null) {
            // The reciter has no published package for that surah, so there is
            // nothing to offer and nothing to play.
            clearRecitation()
            return
        }
        // The surah is over, so nothing is playing and nothing may stay
        // marked while the next one is found.
        restTheMark()
        // The first ayah of the next surah is a query, and the end of a surah
        // is not a place for the main thread to wait on the disk for it.
        scope.launch {
            val first = withContext(Dispatchers.IO) {
                content?.ayahsOfSurah(next)?.firstOrNull()?.number
            }
            if (first == null) {
                clearRecitation()
                return@launch
            }
            val onTheDevice = withContext(Dispatchers.IO) {
                itemsFor(recitation, next).isNotEmpty()
            }
            if (onTheDevice) {
                // The audio is here but the playlist had already ended, which
                // is the answer changing to carry on after the boundary had
                // passed: play the surah rather than offer a package the
                // reader already holds.
                startPlayback(recitation, first, resume = false)
                return@launch
            }
            requestedAyah = first
            _state.value = _state.value.copy(
                pendingDownloadSurah = next,
                pendingDownloadBytes = packageToFetch.bytes,
                pendingIsContinuation = true,
                downloadProgress = null,
                downloadFailed = false,
                downloadFailure = null,
            )
            confirmDownload()
        }
    }

    /**
     * The surah begins again, from the first ayah that is on the device.
     *
     * A surah's end arrives through one of two events, and both have to be
     * here or the repeat works only half the time: the playlist running out,
     * which is what happens when the next surah is not on the device, and an
     * automatic transition into the next surah, which is what happens when it
     * is. A deliberate step is a seek and never an automatic transition, so
     * Next and Previous still carry the reader out of the surah, and the
     * repeat then follows the surah they moved to.
     *
     * The answer is read when the boundary arrives, not when the playlist was
     * grown, so a reader who changes the answer mid-surah is answered at that
     * surah's end and not the next one's (owner decision, this session).
     *
     * False when that surah is not in the playlist, which is the one case
     * the caller has to answer for itself.
     */
    private fun restartSurah(player: Player, surah: Int): Boolean {
        val ids = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        val first = RecitationPlaylist.startOf(ids, surah)
        if (first < 0) return false
        player.seekTo(first, 0L)
        player.play()
        return true
    }

    private fun folderFor(recitation: String, surah: Int): String? {
        val database = content ?: return null
        val first = database.ayahsOfSurah(surah).firstOrNull() ?: return null
        return database.recitationAyah(recitation, first.number)?.audioPath?.let(SafePaths::folderOf)
    }

    /**
     * The word's own voice: one word of the verse, in the reader's reciter,
     * from the word's first sound to its last.
     *
     * The reader pressed and held a word and then asked to hear it, so the
     * answer is that word and not the verse. It is one media item whose own
     * end is the word's end, not a verse with a stop armed against it: nothing
     * has to watch the position from another process, and no pause, focus
     * change, or item transition can cancel the stop and leave the verse
     * playing on (owner report, this session).
     *
     * A word is a sample, not a session: it sounds on its own player, so the
     * reader's playlist, pill, notification, and saved place are not touched.
     * A recitation that was playing waits at the word and is given back at its
     * last millisecond; one that was already paused was paused by the reader
     * and stays as they left it (owner decision, this session).
     *
     * False when the audio or the word's own region is not on the device: the
     * sheet only offers the mark for a surah it can hear, so this is the
     * guard behind that mark rather than a state a reader meets.
     */
    suspend fun hearWord(recitation: String, ayahNumber: Int, position: Int): Boolean {
        val database = content ?: return false
        // The regions are half open and counted from the ayah's first drawn
        // word: the word at position p is the region [p - 1, p). A region that
        // spans two words (a joined pair) answers for both of them.
        val index = position - 1
        if (index < 0) return false
        // The timings and the file are both disk work: the tap answers on the
        // frame, so neither is read there (owner decision, this session).
        val found = withContext(Dispatchers.IO) {
            val audio = database.recitationAyah(recitation, ayahNumber) ?: return@withContext null
            val segment = audio.segments.firstOrNull {
                index >= it.wordFrom && index < it.wordTo
            } ?: return@withContext null
            val uri = store.uri(audio.audioPath) ?: return@withContext null
            segment to uri
        } ?: return false
        val (segment, uri) = found
        val clip = WordClip.of(segment.startMs, segment.endMs)
        // A word heard while another sounds replaces it, and the session keeps
        // waiting: the pause for the first word is still the reader's one.
        val replacing = _state.value.sample != null
        _state.value = _state.value.copy(sample = WordSample(ayahNumber, position))
        val started = runCatching {
            withContext(Dispatchers.Main) {
                if (!replacing) {
                    resumeAfterSample = controller?.let {
                        it.playWhenReady && it.playbackState != Player.STATE_ENDED
                    } == true
                    if (resumeAfterSample) controller?.pause()
                }
                val player = samplePlayer()
                player.setMediaItem(
                    MediaItem.Builder()
                        .setUri(uri)
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(clip.startMs)
                                .setEndPositionMs(clip.endMs)
                                .build(),
                        )
                        .build(),
                )
                player.prepare()
                player.play()
            }
        }
        if (started.isFailure) {
            android.util.Log.w(TAG, "the word could not be started", started.exceptionOrNull())
            finishSample(resume = true)
            return false
        }
        return true
    }

    /**
     * The word's own player, built on the first word and kept for the next.
     *
     * It asks for no audio focus of its own: the recitation holds the reader's
     * focus, and a word is heard inside a reading rather than beside it. The
     * session is paused for the word's length, so the two never sound at once
     * (owner decision, this session).
     *
     * It is built on the main thread, where every call to it and to the
     * session's controller is made: the reading's doors and its word sheet all
     * answer on that thread.
     */
    private fun samplePlayer(): ExoPlayer {
        sample?.let { return it }
        val created = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            .setWakeMode(C.WAKE_MODE_NONE)
            .build()
        created.addListener(sampleListener)
        sample = created
        return created
    }

    /**
     * The word's end is its item's end, so the player says so by itself. A
     * word that will not sound ends the sample too: a wash with no voice
     * behind it would sit on the sheet until the reader pressed something
     * else (owner decision, this session).
     */
    private val sampleListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) finishSample(resume = true)
        }

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.w(TAG, "the word could not be played", error)
            finishSample(resume = true)
        }
    }

    /**
     * The reader's own play and pause. A word's voice is not the session, so
     * this is only ever the recitation's transport: a tap here while a word
     * sounds is the reader reaching for their recitation, and the word ends
     * with its own clip rather than answering for it (owner decision, this
     * session).
     */
    fun toggle() {
        // The reader's own transport ends a word that is still sounding: they
        // asked for the recitation, and two voices must not answer them.
        finishSample(resume = false)
        val player = controller ?: return
        stoppedAtAyahEnd = false
        if (player.isPlaying) player.pause() else player.play()
    }

    /**
     * The reader's pace. It is their own choice and it is remembered, so it
     * is applied to the player the moment it is set and to every player the
     * app connects afterwards. ExoPlayer keeps the pitch, so a slower
     * recitation is slower, not deeper.
     */
    fun setSpeed(value: Float) {
        speed = value.coerceIn(MIN_SPEED, MAX_SPEED)
        controller?.setPlaybackSpeed(speed)
        _state.update { it.copy(speed = speed) }
    }

    /**
     * What happens as the reading moves on: the next ayah, the stop, the ayah
     * again, the surah again, or the surah after this one. One value, and the
     * answer rows that show it in the pill's own menu are views of it (owner
     * decision).
     *
     * The ayah repeat is the player's own, so the item itself loops and the
     * surah-end offer never appears while the reader is repeating. The surah
     * repeat cannot be: the playlist grows (the next surah's ayahs are
     * appended while the reader is in this one), so `REPEAT_MODE_ALL` would
     * loop everything that is loaded rather than one surah. It is therefore
     * ours, and [restartSurah] does it from the two events a surah's end
     * arrives through. The stop is the player's own pause-at-end, which a
     * controller cannot set and so is sent to the service.
     */
    fun setEndOfAudio(value: EndOfAudio) {
        end = value
        controller?.let { applyEndOfAudio(it, value) }
    }

    /** The answer, on the player: the ayah repeat, the stop, or neither. */
    private fun applyEndOfAudio(player: MediaController, value: EndOfAudio) {
        player.repeatMode =
            if (value == EndOfAudio.REPEAT_AYAH) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        setPauseAtEnd(player, value == EndOfAudio.STOP_AFTER_AYAH)
    }

    /**
     * The reader's stop, sent to the one player that can keep it. The command
     * is advertised by [PlaybackService] on connect; a controller that somehow
     * did not get it is left as it is rather than crashed.
     */
    private fun setPauseAtEnd(player: MediaController, pause: Boolean) {
        val command = SessionCommand(PlaybackService.COMMAND_PAUSE_AT_END, Bundle.EMPTY)
        if (!player.availableSessionCommands.contains(command)) return
        player.sendCustomCommand(
            command,
            Bundle().apply { putBoolean(PlaybackService.ARG_PAUSE_AT_END, pause) },
        )
    }

    fun next() {
        finishSample(resume = false)
        stoppedAtAyahEnd = false
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        finishSample(resume = false)
        stoppedAtAyahEnd = false
        controller?.seekToPreviousMediaItem()
    }

    fun stop() {
        // A package on its way is part of the pill's own control: a stop puts
        // the whole of it away, so the transfer goes with it instead of
        // surfacing later as a surah that starts playing on its own.
        downloadJob?.cancel()
        downloadJob = null
        // A player that never connected is a pill with no transport to stop,
        // and the pill still has to go: the state is cleared whatever the
        // player is, because a failed connect is otherwise a line the reader
        // cannot put away at all (owner report: the close on a failed player
        // was a mark that answered nothing).
        clearRecitation()
    }

    /**
     * Nothing is left to control: the player lets its items go, and the pill
     * and the phone's own media notification go with them.
     *
     * The reader's Stop and the end of the last surah arrive here together. An
     * ended player is not an empty one: it keeps its last ayah as the current
     * item, so the media notification wore a Play that answers nothing, and an
     * ended player never restarts (owner decision, this session). An ended
     * recitation and a stopped one are one state: nothing is loaded, nothing
     * is marked, and the next Play starts from the reader's place.
     */
    private fun clearRecitation() {
        // A word still sounding goes with the recitation: the reader put the
        // whole of it away, and a word has nothing to give back to a session
        // that is no longer here (owner decision, this session).
        finishSample(resume = false)
        releaseItems()
        _state.value = PlaybackUiState(connected = true, speed = speed)
        // Where the recitation was heard is not a place to offer the reader
        // again: they put it away, or it reached the end they asked for.
        heardAyah = null
        syncTicker()
        scope.launch { positionStore.clear() }
    }

    /**
     * Takes everything the player holds away, so the pill, the phone's own
     * notification, and the reading's mark have nothing left to stand on.
     *
     * The place is the caller's to keep or clear. A word never stood in this
     * player, so its end takes nothing here (owner decision, this session).
     */
    private fun releaseItems() {
        val player = controller
        // Paused first, so a player that was ready to play is not left armed
        // with nothing to play, and every later event sees one state.
        player?.pause()
        player?.stop()
        player?.clearMediaItems()
        stoppedAtAyahEnd = false
        segmentsByAyah.clear()
    }

    /**
     * The word's own voice is over, so its clip goes with it.
     *
     * A word is heard, not kept: its item is cleared from the sample's own
     * player, and the state that washes the word on the sheet goes at the same
     * moment. The recitation that waited for the word is given back when it was
     * playing, and left alone when it was not, because a pause the reader made
     * is theirs to undo. The saved listening place was never the word's, so it
     * is not touched here either (owner decision, this session).
     *
     * Like the sample player itself, this is called on the main thread: by the
     * clip's own listener or by a door the reader pressed.
     */
    private fun finishSample(resume: Boolean) {
        sample?.let { player ->
            player.pause()
            player.clearMediaItems()
        }
        if (_state.value.sample != null) {
            _state.value = _state.value.copy(sample = null)
        }
        val giveBack = resume && resumeAfterSample
        resumeAfterSample = false
        if (giveBack) {
            // A session that was put away while the word sounded has nothing
            // left to give back.
            controller?.takeIf { it.mediaItemCount > 0 }?.play()
        }
    }

    /**
     * The surah is over, so nothing is playing and nothing may stay marked.
     *
     * The player keeps the last ayah as its current item, and the next publish
     * would keep drawing it as the reciting ayah forever; the mark is released
     * before anything follows it. The offset inside that last ayah is not a
     * place to come back to either.
     */
    private fun restTheMark() {
        segmentsByAyah.clear()
        _state.value = _state.value.copy(
            isPlaying = false,
            ayahNumber = null,
        )
        heardAyah = null
        syncTicker()
        scope.launch { positionStore.clear() }
    }

    /** The reading window is gone: let the controller go, not only its state. */
    fun release() {
        downloadJob?.cancel()
        downloadJob = null
        checkpoint?.cancel()
        checkpoint = null
        segmentsByAyah.clear()
        // The word's own player is not the session's, so it is let go here
        // too: a window that is gone has no sheet to wash and no word to say
        // (owner decision, this session).
        sample?.release()
        sample = null
        resumeAfterSample = false
        controller?.release()
        controller = null
    }

    private suspend fun connect(): MediaController {
        controller?.takeIf { it.isConnected }?.let { return it }
        controller = null
        return suspendCancellableCoroutine { continuation ->
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, token)
                .setListener(
                    object : MediaController.Listener {
                        override fun onDisconnected(disconnected: MediaController) {
                            // The service put a paused player away after its
                            // timeout. The reading keeps its place, and the
                            // pill goes with the player rather than staying
                            // as a control that answers nothing (owner
                            // decision, this session).
                            if (controller !== disconnected) return
                            // A package that was in flight belongs to the
                            // player that has just gone; it must not surface
                            // later as a surah that starts on its own (owner
                            // decision, this session).
                            downloadJob?.cancel()
                            downloadJob = null
                            controller = null
                            _state.value = PlaybackUiState()
                            syncTicker()
                        }
                    },
                )
                .buildAsync()
            future.addListener(
                {
                    try {
                        val player = future.get().also {
                            it.addListener(listener)
                            // The reader's own pace and answer arrive with
                            // the connection, so the first ayah plays the way
                            // the last session was being heard.
                            it.setPlaybackSpeed(speed)
                            applyEndOfAudio(it, end)
                        }
                        controller = player
                        _state.value = _state.value.copy(connected = true, speed = speed)
                        continuation.resume(player)
                    } catch (error: Throwable) {
                        continuation.resumeWithException(error)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    /**
     * The one picture the lock screen and the shade carry: the app's own
     * book mark on the reading's paper, drawn once from the launcher icon so
     * the media surface wears the same hand as the home screen. A reader who
     * has no launcher icon (an OEM that refuses one) gets no artwork rather
     * than a broken image (owner decision, this session).
     */
    private val artwork: ByteArray? by lazy { mediaArtwork(context) }

    /**
     * The playlist for one surah, or nothing when any of its ayahs has no
     * audio.
     *
     * A surah is installed whole and a bundled surah is whole, so a partial
     * set is a missing download, not a surah to play in pieces. The debug
     * sample carries three Al-Baqarah ayahs; before this rule the end of
     * Al-Fatiha could auto-play that fragment instead of asking for the surah
     * (owner decision, this session).
     */
    private fun itemsFor(recitation: String, surah: Int): List<MediaItem> {
        val database = content ?: return emptyList()
        val spoken = spokenContext()
        val surahName = database.surah(surah)?.nameSimple.orEmpty()
        if (surahName.isNotBlank()) placeNames[surah] = surahName
        val artist = reciterName(
            spoken,
            recitation,
            database.recitations().firstOrNull { it.id == recitation }?.name.orEmpty(),
        )
        val ayahs = database.ayahsOfSurah(surah)
        val segmentsForSurah = HashMap<Int, List<WordSegment>>(ayahs.size)
        val items = ayahs.mapNotNull { ayah ->
            val audio = database.recitationAyah(recitation, ayah.number) ?: return@mapNotNull null
            val uri = store.uri(audio.audioPath) ?: return@mapNotNull null
            segmentsForSurah[ayah.number] = audio.segments
            MediaItem.Builder()
                .setMediaId(RecitationPlaylist.mediaId(ayah.number, ayah.surah))
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(
                            spoken.getString(
                                R.string.playback_now_playing,
                                surahName,
                                ayah.ayah,
                            ),
                        )
                        .setArtist(artist)
                        .apply {
                            artwork?.let {
                                setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            }
                        }
                        .build(),
                )
                .build()
        }
        if (items.size != ayahs.size) return emptyList()
        if (recitationId == recitation) segmentsByAyah.putAll(segmentsForSurah)
        return items
    }

    /**
     * The app's icon, drawn at 512 on the reading's paper. The icon is the
     * same drawing the launcher and the store carry, and the media surface
     * is the one place a reader hears the app without the page in front of
     * them, so it should still see the app. The icon's own adaptive
     * background is the same paper, so the mark sits on a seamless ground.
     */
    private fun mediaArtwork(context: Context): ByteArray? = runCatching {
        val size = 512
        val inset = size / 8
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(PaperBackground.toArgb())
            val icon = context.packageManager.getApplicationIcon(context.packageName)
            icon.setBounds(inset, inset, size - inset, size - inset)
            icon.draw(canvas)
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }.getOrNull()

    /**
     * A context wearing the reader's own language, for the one surface that
     * has no composition at all: the media notification the phone draws from
     * this playlist. The locale is read from the synchronous mirror the
     * language choice writes, and the reciter's name and the ayah's own
     * number come from it, so the lock screen says the place in the language
     * the reader is reading in, whatever the phone's own language is (owner
     * decision, this session).
     */
    private fun spokenContext(): Context {
        val locale = Locale.forLanguageTag(LanguagePreference(context).tag() ?: "en")
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // A surah's end is the one automatic advance that arrives in
            // another surah, and the surah it ends is the one the state last
            // published. A deliberate step is a seek and is never read here,
            // so Next and Previous still carry the reader out of the surah.
            val player = controller
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && player != null) {
                val ended = RecitationPlaylist.endedSurah(
                    _state.value.surah,
                    mediaItem?.mediaId,
                )
                if (ended != null) {
                    // The boundary is where the reader's one answer is read. The
                    // next surah is in the playlist because a deliberate Next
                    // may cross into it; the answer, and not the playlist,
                    // decides whether the audio does (owner decision, this
                    // session).
                    when (end.surahEnding) {
                        SurahEnding.REPEAT -> if (restartSurah(player, ended)) {
                            publish(player)
                            return
                        }
                        // The surah is over and the reader asked for nothing
                        // after it, so the audio stops here rather than in the
                        // next surah. This is the owner's report: "continue to
                        // the next ayah" is about the ayah, and the end of the
                        // surah is where the reading rests.
                        SurahEnding.REST -> {
                            clearRecitation()
                            return
                        }
                        SurahEnding.CARRY_ON -> Unit
                    }
                }
            }
            publish(controller)
            maybeAppendNextSurah()
        }

        override fun onPlayerError(error: PlaybackException) {
            // One file that will not play is not a reason to take the reading
            // down: the pill says so and offers the one action that can help.
            // A word that will not sound is over the moment it failed, and it
            // is heard on its own player, whose listener says so.
            android.util.Log.w(TAG, "the recitation could not be played", error)
            _state.value = _state.value.copy(isPlaying = false, error = true)
            heardAyah = null
            syncTicker()
        }

        /**
         * A pause at the end of the item is the reader's Stop after this ayah
         * arriving, not a pause: the audio is over, so the recited mark is
         * released the way it is when a surah ends, and the pill has nothing
         * left to control. The reason is what tells the two apart; a manual
         * pause is [Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST] and
         * keeps its place and its transport (owner decision).
         */
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (playWhenReady) {
                stoppedAtAyahEnd = false
                return
            }
            if (reason != Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) return
            stoppedAtAyahEnd = true
            _state.value = _state.value.copy(
                isPlaying = false,
                ayahNumber = null,
            )
            heardAyah = null
            syncTicker()
            scope.launch { positionStore.clear() }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            val player = controller
            val surah = _state.value.surah
            // The playlist ran out, so the surah is over whatever the answer
            // says: a repeat begins this surah again, an answer that carries on
            // has nowhere to go and finds or offers the next surah, and a
            // reading asked to rest is over.
            when (end.surahEnding) {
                SurahEnding.REPEAT -> if (player != null && surah != null && restartSurah(player, surah)) {
                    publish(player)
                    return
                }
                SurahEnding.CARRY_ON -> {
                    carryOnToTheNextSurah()
                    return
                }
                SurahEnding.REST -> {
                    clearRecitation()
                    return
                }
            }
            // A repeat the playlist cannot make is a surah with nowhere to
            // begin again: nothing follows it either.
            clearRecitation()
        }
    }

    /**
     * The place written while the reading is hidden. The wash is scheduled
     * by the reading, so the player has no wash timer at all; this is only
     * the memory of where the recitation had reached, at a pace that costs
     * the disk almost nothing and still answers a process that died mid-ayah
     * (owner decision, this session).
     */
    private fun startCheckpoint() {
        if (tracking) return
        if (checkpoint?.isActive == true) return
        checkpoint = scope.launch {
            while (isActive) {
                delay(POSITION_WRITE_MS)
                checkpointPlace()
            }
        }
    }

    private fun stopCheckpoint() {
        checkpoint?.cancel()
        checkpoint = null
    }

    /** The place, written once at the pace the hidden reading needs and no faster. */
    private fun checkpointPlace() {
        val player = controller ?: return
        if (player.playbackState == Player.STATE_ENDED) return
        if (stoppedAtAyahEnd && !player.playWhenReady) return
        val id = player.currentMediaItem?.mediaId ?: return
        val ayahNumber = RecitationPlaylist.ayahOf(id) ?: return
        rememberHeard(
            recitationId,
            ayahNumber,
            player.currentPosition.coerceAtLeast(0),
            playing = false,
        )
    }

    /**
     * The one cadence the player needs: the place while the reading is hidden,
     * and neither while the player is paused. The wash is not the player's
     * timer any more; the reading schedules it (owner decision,
     * this session).
     */
    private fun syncTicker() {
        val cadence = playbackCadence(_state.value.isPlaying, tracking)
        if (cadence.checkpoint) startCheckpoint() else stopCheckpoint()
    }

    private fun publish(player: Player?) {
        val player = player ?: return
        // An ended player still carries its last media item, and reading it
        // would light the final ayah as if it were being recited. When the
        // surah is over the state says so itself, with no ayah marked.
        if (player.playbackState == Player.STATE_ENDED) return
        // A stop after this ayah parks the player at the end of its item
        // rather than ending it, so the item is still current and a later
        // event would light the ayah again. The mark stays released until the
        // reader asks for the audio back (owner decision).
        if (stoppedAtAyahEnd && !player.playWhenReady) return
        val id = player.currentMediaItem?.mediaId ?: return
        val ayahNumber = RecitationPlaylist.ayahOf(id) ?: return
        val surah = RecitationPlaylist.surahOf(id)
        val at = player.currentPosition.coerceAtLeast(0)
        _state.value = _state.value.copy(
            connected = true,
            isPlaying = player.isPlaying,
            recitation = recitationId,
            ayahNumber = ayahNumber,
            surah = surah,
            reference = surah?.let { "$it:$ayahNumber" },
            placeName = surah?.let { placeNames[it] },
        )
        rememberHeard(recitationId, ayahNumber, at, player.isPlaying)
        syncTicker()
    }

    /**
     * Writes the place the recitation was heard down, so a process that dies
     * mid-ayah is answered by the same ayah at the same offset rather than by
     * the top of it.
     *
     * The offset is written on a throttle: it moves with every frame and the
     * disk does not need to, and a resume a few seconds out is one nobody can
     * tell from the real one. A change of ayah and a pause are each written at
     * once, because the ayah is the part nothing else can put back and the
     * pause is the moment the reader stepped away.
     */
    private fun rememberHeard(recitation: String?, ayahNumber: Int, at: Long, playing: Boolean) {
        if (recitation == null) return
        val moved = heardAyah != ayahNumber
        heardAyah = ayahNumber
        val now = System.currentTimeMillis()
        if (!moved && playing && now - heardWrittenAt < POSITION_WRITE_MS) return
        heardWrittenAt = now
        scope.launch { positionStore.write(PlaybackPosition(recitation, ayahNumber, at)) }
    }

    /**
     * Keeps one surah loaded after the one being heard, whatever the answer
     * says.
     *
     * The playlist is a place the reader may go, not a promise of where the
     * audio goes. Next and Previous cross surahs deliberately (owner decision,
     * 9.9), so the surah after this one is loaded while the reader is in this
     * one; whether the audio follows it is the answer's own rule at the
     * boundary ([surahEnding]), so a change of answer mid-surah is honoured at
     * that surah's end (owner decision, this session).
     *
     * [RecitationQueue] is what keeps one transition from appending the same
     * surah twice.
     */
    private fun maybeAppendNextSurah() {
        val player = controller ?: return
        val id = player.currentMediaItem?.mediaId ?: return
        val surah = RecitationPlaylist.surahOf(id) ?: return
        val recitation = recitationId ?: return
        val next = queue.appendAt(surah) ?: return
        scope.launch {
            val items = withContext(Dispatchers.IO) { itemsFor(recitation, next) }
            if (items.isNotEmpty()) {
                withContext(Dispatchers.Main) { controller?.addMediaItems(items) }
            }
        }
    }

    private companion object {
        /** The one name a failure is logged under. */
        const val TAG = "PlaybackController"

        /** The pace the reader may choose, and its bounds. */
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 1.5f

        /**
         * How far apart two writes of the heard offset may be. The ayah it
         * belongs to is written the moment it changes; this is the offset
         * inside it, which moves constantly and costs a disk write to keep
         * exact. Ten seconds is also the hidden reading's checkpoint, and a
         * resume a few seconds out is one nobody can hear (owner decision,
         * this session).
         */
        const val POSITION_WRITE_MS = 10_000L
    }
}
