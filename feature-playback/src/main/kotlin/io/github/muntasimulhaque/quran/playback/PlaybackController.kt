package io.github.muntasimulhaque.quran.playback

import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.core.RecitationPlaylist
import io.github.muntasimulhaque.quran.core.RecitationQueue
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PlaybackPosition
import io.github.muntasimulhaque.quran.data.PlaybackPositionStore
import io.github.muntasimulhaque.quran.data.RecitationAyah
import io.github.muntasimulhaque.quran.data.RecitationDownloader
import io.github.muntasimulhaque.quran.data.RecitationManifest
import io.github.muntasimulhaque.quran.data.RecitationStore
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.ui.kit.reciterName
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
 * surah repeat is the one answer the player cannot give by itself
 * (owner decision).
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
    private var segments: List<io.github.muntasimulhaque.quran.data.WordSegment> = emptyList()
    private var segmentedAyah: Int? = null
    private var ticker: Job? = null
    private var checkpoint: Job? = null
    private var downloadJob: Job? = null
    private var requestedAyah: Int? = null

    /**
     * Whether the reading that draws the recited word is on the glass. The
     * tick exists for that wash, and a wash nobody can see is a loop that
     * wakes the processor for nothing (owner decision, this session).
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

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun attach(content: ContentDatabase) {
        this.content = content
        // The tick is for the word being recited, and it runs only while
        // something is being recited.
        syncTicker()
    }

    /**
     * Tells the player whether the reading is on the glass.
     *
     * The wash moves ten times a second for the reader's eyes. When the eyes
     * are not there, the wash is not there either, so the tick stops and the
     * place is written on a ten-second checkpoint instead. That checkpoint is
     * what keeps a process that dies mid-ayah answered by the rest of the ayah
     * rather than by its top (owner decision, this session).
     */
    fun setWordTracking(visible: Boolean) {
        if (tracking == visible) return
        tracking = visible
        if (visible) {
            controller?.let { publish(it) }
        } else {
            checkpointPlace()
        }
        syncTicker()
    }

    /**
     * The reader's Play, from any of its doors. A fresh play is the reader
     * asking for something else, so a package still on its way goes with it
     * rather than surfacing later as a surah that starts playing on its own.
     * The download's own continuation enters at [startPlayback]: it is not a
     * new request, and its job is the one this door would cancel.
     */
    suspend fun play(recitation: String, ayahNumber: Int, resume: Boolean = true) {
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
        )
        startPlayback(recitation, ayahNumber, resume)
    }

    /** Where Play lands once the reader's own request is the only one left. */
    private suspend fun startPlayback(recitation: String, ayahNumber: Int, resume: Boolean) {
        val database = content ?: return
        // A fresh play is the reader asking for the audio back, so the end
        // that Stop after this ayah reached is behind them now.
        stoppedAtAyahEnd = false
        val ayah = withContext(Dispatchers.IO) {
            database.ayah(ayahNumber)
        } ?: return
        recitationId = recitation
        requestedAyah = ayahNumber
        val items = withContext(Dispatchers.IO) { itemsFor(recitation, ayah.surah) }
        if (items.isEmpty()) {
            // Without the reciter's word timings there is no audio to point
            // at, and downloading the surah would not change that: say so
            // rather than offer a download that cannot help.
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
            )
            return
        }
        val index = items.indexOfFirst {
            it.mediaId == RecitationPlaylist.mediaId(ayah.number, ayah.surah)
        }
        if (index < 0) {
            // The surah is not fully on the device; ask before fetching it.
            val packageToFetch = manifest.packageFor(recitation, ayah.surah)
            _state.value = _state.value.copy(
                recitation = recitation,
                unavailable = packageToFetch == null,
                pendingDownloadSurah = if (packageToFetch == null) null else ayah.surah,
                pendingDownloadBytes = packageToFetch?.bytes ?: 0,
                pendingIsContinuation = false,
                downloadProgress = null,
                downloadFailed = false,
            )
            return
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
            return
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
            return
        }
        startTicker()
        _state.value = _state.value.copy(
            recitation = recitation,
            unavailable = false,
            error = false,
            pendingDownloadSurah = null,
            downloadProgress = null,
            downloadFailed = false,
        )
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
    ): Boolean {
        val packageToFetch = manifest.packageFor(recitation, surah) ?: return false
        val folder = withContext(Dispatchers.IO) { folderFor(recitation, surah) } ?: return false
        val result = downloader.download(packageToFetch, folder) { read, total ->
            onProgress(if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f)
        }
        return result.isSuccess
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
                    current.copy(downloadProgress = 0f, downloadFailed = false)
                } else {
                    current
                }
            }
            val folder = withContext(Dispatchers.IO) { folderFor(recitation, surah) }
            if (folder == null) {
                failDownload(surah)
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
                failDownload(surah)
            }
        }
    }

    /** The failure line, for the request that is still on screen. */
    private fun failDownload(surah: Int) {
        _state.update { current ->
            if (current.pendingDownloadSurah == surah) {
                current.copy(downloadProgress = null, downloadFailed = true)
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
            )
        }
    }

    /**
     * The surah ended and the next one is not on the device. The reader's
     * Continue to the next surah choice decides: off, the offer waits with
     * the next surah's name and size, so nothing is fetched behind the
     * reader's back; on, the package is fetched with the reciter being heard
     * and plays on, and the pill carries the size, the progress, and the
     * cancel while it does (owner decision).
     */
    private fun offerNextSurah() {
        // A surah that is repeating has not ended; it has begun again, so
        // there is nothing to offer and no package to fetch. A reading told
        // to stop after each ayah never reaches a surah's end at all. Both
        // are the reader's own word, and neither asks for the next package.
        if (end == EndOfAudio.REPEAT_SURAH || end == EndOfAudio.STOP_AFTER_AYAH) return
        val surah = _state.value.surah ?: return
        val recitation = recitationId ?: return
        val next = surah + 1
        if (next > 114) return
        val packageToFetch = manifest.packageFor(recitation, next) ?: return
        // The first ayah of the next surah is a query, and the end of a surah
        // is not a place for the main thread to wait on the disk for it.
        scope.launch {
            val first = withContext(Dispatchers.IO) {
                content?.ayahsOfSurah(next)?.firstOrNull()?.number
            } ?: return@launch
            requestedAyah = first
            _state.value = _state.value.copy(
                pendingDownloadSurah = next,
                pendingDownloadBytes = packageToFetch.bytes,
                pendingIsContinuation = true,
                downloadProgress = null,
                downloadFailed = false,
            )
            if (end == EndOfAudio.CONTINUE_SURAH) confirmDownload()
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
        return database.recitationAyah(recitation, first.number)?.audioPath?.substringBeforeLast('/')
    }

    fun toggle() {
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
        stoppedAtAyahEnd = false
        controller?.seekToNextMediaItem()
    }

    fun previous() {
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
        // cannot put away at all (owner report, this session: the close on a
        // failed player was a mark that answered nothing).
        val player = controller
        player?.stop()
        player?.clearMediaItems()
        stoppedAtAyahEnd = false
        _state.value = PlaybackUiState(connected = true)
        // The reader put the recitation away themselves, so where it was
        // heard is not a place to offer them again.
        heardAyah = null
        syncTicker()
        scope.launch { positionStore.clear() }
    }

    /** The reading window is gone: let the controller go, not only its state. */
    fun release() {
        downloadJob?.cancel()
        downloadJob = null
        ticker?.cancel()
        ticker = null
        checkpoint?.cancel()
        checkpoint = null
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
                        _state.value = _state.value.copy(connected = true)
                        continuation.resume(player)
                    } catch (error: Throwable) {
                        continuation.resumeWithException(error)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    private fun itemsFor(recitation: String, surah: Int): List<MediaItem> {
        val database = content ?: return emptyList()
        val spoken = spokenContext()
        val surahName = database.surah(surah)?.nameSimple.orEmpty()
        val artist = reciterName(
            spoken,
            recitation,
            database.recitations().firstOrNull { it.id == recitation }?.name.orEmpty(),
        )
        return database.ayahsOfSurah(surah).mapNotNull { ayah ->
            val audio = database.recitationAyah(recitation, ayah.number) ?: return@mapNotNull null
            val uri = store.uri(audio.audioPath) ?: return@mapNotNull null
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
                        .build(),
                )
                .build()
        }
    }

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
                    end,
                    _state.value.surah,
                    mediaItem?.mediaId,
                )
                if (ended != null && restartSurah(player, ended)) {
                    publish(player)
                    return
                }
            }
            publish(controller)
            maybeAppendNextSurah()
        }

        override fun onPlayerError(error: PlaybackException) {
            // One file that will not play is not a reason to take the reading
            // down: the pill says so and offers the one action that can help.
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
                wordPosition = null,
            )
            heardAyah = null
            syncTicker()
            scope.launch { positionStore.clear() }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            val player = controller
            val surah = _state.value.surah
            if (end == EndOfAudio.REPEAT_SURAH &&
                player != null && surah != null && restartSurah(player, surah)
            ) {
                publish(player)
                return
            }
            // The surah ended, so nothing is playing and nothing may stay
            // marked. The player keeps the last ayah as its current item, and
            // the next publish would keep drawing it as the reciting ayah
            // forever; the mark is cleared here, before the next surah is
            // offered, so the page rests unlit while the reader decides.
            _state.value = _state.value.copy(
                isPlaying = false,
                ayahNumber = null,
                wordPosition = null,
            )
            // The surah is over, so the offset inside its last ayah is not a
            // place to come back to.
            heardAyah = null
            syncTicker()
            scope.launch { positionStore.clear() }
            offerNextSurah()
        }
    }

    /**
     * The tick that keeps the recited word marked. It publishes ten times a
     * second while something is being heard and the reading is on the glass,
     * and does not run at all otherwise: a word cannot move inside a paused
     * player, and a wash nobody can see is a loop that wakes four cores to
     * learn nothing. Ten times a second keeps the wash within a tenth of a
     * region boundary, where five left it visibly behind a fast recitation
     * (owner report, this session).
     */
    private fun startTicker() {
        if (!tracking) return
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                controller?.let { publish(it) }
                delay(TICK_MILLIS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    /**
     * The place written while the reading is hidden. The tick is off, so
     * nothing moves the wash; this is only the memory of where the recitation
     * had reached, at a pace that costs the disk almost nothing and still
     * answers a process that died mid-ayah (owner decision, this session).
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
     * The one cadence the player needs: the wash while the reading is on the
     * glass, the place while it is not, and neither while the player is
     * paused (owner decision, this session).
     */
    private fun syncTicker() {
        val cadence = playbackCadence(_state.value.isPlaying, tracking)
        if (cadence.tick) startTicker() else stopTicker()
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
        if (segmentedAyah != ayahNumber) loadSegments(ayahNumber)
        _state.value = _state.value.copy(
            connected = true,
            isPlaying = player.isPlaying,
            recitation = recitationId,
            ayahNumber = ayahNumber,
            surah = surah,
            reference = player.currentMediaItem?.mediaMetadata?.title?.toString(),
            wordPosition = wordAt(at),
        )
        rememberHeard(recitationId, ayahNumber, at, player.isPlaying)
        syncTicker()
    }

    /**
     * Writes the place the recitation was heard down, so a process that dies
     * mid-ayah is answered by the same ayah at the same offset rather than by
     * the top of it.
     *
     * The offset is written on a throttle: it moves ten times a second and
     * the disk does not need to, and a resume a few seconds out is one nobody
     * can tell from the real one. A change of ayah and a pause are each
     * written at once, because the ayah is the part nothing else can put back
     * and the pause is the moment the reader stepped away.
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

    private fun loadSegments(ayahNumber: Int) {
        val recitation = recitationId ?: return
        segmentedAyah = ayahNumber
        segments = emptyList()
        val database = content ?: return
        scope.launch {
            val recitationAyah: RecitationAyah? = withContext(Dispatchers.IO) {
                database.recitationAyah(recitation, ayahNumber)
            }
            if (segmentedAyah == ayahNumber) {
                segments = recitationAyah?.segments.orEmpty()
            }
        }
    }

    private fun wordAt(positionMs: Long): Int? = recitedWordAt(segments, positionMs)

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

        /** How often the recited word is published while something is heard. */
        const val TICK_MILLIS = 100L

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
