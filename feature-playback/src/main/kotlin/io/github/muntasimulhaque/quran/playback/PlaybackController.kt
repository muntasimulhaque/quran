package io.github.muntasimulhaque.quran.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.core.RecitationPlaylist
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.RecitationAyah
import io.github.muntasimulhaque.quran.data.RecitationDownloader
import io.github.muntasimulhaque.quran.data.RecitationManifest
import io.github.muntasimulhaque.quran.data.RecitationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
) {

    private val store = RecitationStore(context)
    private val manifest = RecitationManifest.load(context)
    private val downloader = RecitationDownloader(context)
    private var content: ContentDatabase? = null
    private var controller: MediaController? = null
    private var loadedThroughSurah = 0
    private var recitationId: String? = null
    private var segments: List<io.github.muntasimulhaque.quran.data.WordSegment> = emptyList()
    private var segmentedAyah: Int? = null
    private var ticker: Job? = null
    private var downloadJob: Job? = null
    private var requestedAyah: Int? = null

    /** The reader's pace, repeat, and continuation, applied on connect. */
    private var speed = 1f
    private var end = EndOfAudio.OFF

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun attach(content: ContentDatabase) {
        this.content = content
        startTicker()
    }

    suspend fun play(recitation: String, ayahNumber: Int) {
        val database = content ?: return
        val location = withContext(Dispatchers.IO) {
            database.ayahsWithPages(listOf(ayahNumber)).firstOrNull()
        } ?: return
        recitationId = recitation
        requestedAyah = ayahNumber
        val items = withContext(Dispatchers.IO) { itemsFor(recitation, location.ayah.surah) }
        if (items.isEmpty()) {
            // Without the reciter's word timings there is no audio to point
            // at, and downloading the surah would not change that: say so
            // rather than offer a download that cannot help.
            val timings = withContext(Dispatchers.IO) {
                database.recitationAyah(recitation, location.ayah.number)
            }
            val packageToFetch = manifest.packageFor(recitation, location.ayah.surah)
                ?.takeIf { timings != null }
            _state.value = _state.value.copy(
                recitation = recitation,
                unavailable = packageToFetch == null,
                pendingDownloadSurah = if (packageToFetch == null) null else location.ayah.surah,
                pendingDownloadBytes = packageToFetch?.bytes ?: 0,
                pendingIsContinuation = false,
                downloadProgress = null,
                downloadFailed = false,
            )
            return
        }
        val index = items.indexOfFirst {
            it.mediaId == RecitationPlaylist.mediaId(location.ayah.number, location.ayah.surah)
        }
        if (index < 0) {
            // The surah is not fully on the device; ask before fetching it.
            val packageToFetch = manifest.packageFor(recitation, location.ayah.surah)
            _state.value = _state.value.copy(
                recitation = recitation,
                unavailable = packageToFetch == null,
                pendingDownloadSurah = if (packageToFetch == null) null else location.ayah.surah,
                pendingDownloadBytes = packageToFetch?.bytes ?: 0,
                pendingIsContinuation = false,
                downloadProgress = null,
                downloadFailed = false,
            )
            return
        }
        val player = connect()
        withContext(Dispatchers.Main) {
            player.setMediaItems(items, index, 0L)
            player.prepare()
            player.play()
        }
        loadedThroughSurah = location.ayah.surah
        _state.value = _state.value.copy(
            recitation = recitation,
            unavailable = false,
            pendingDownloadSurah = null,
            downloadProgress = null,
            downloadFailed = false,
        )
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

    /** Downloads the pending surah, verifies it, and starts playing where the reader asked. */
    fun confirmDownload() {
        val recitation = _state.value.recitation ?: recitationId ?: return
        val surah = _state.value.pendingDownloadSurah ?: return
        val ayah = requestedAyah ?: return
        val packageToFetch = manifest.packageFor(recitation, surah) ?: return
        downloadJob?.cancel()
        downloadJob = scope.launch {
            _state.value = _state.value.copy(downloadProgress = 0f, downloadFailed = false)
            val folder = withContext(Dispatchers.IO) { folderFor(recitation, surah) }
            if (folder == null) {
                _state.value = _state.value.copy(downloadProgress = null, downloadFailed = true)
                return@launch
            }
            val result = downloader.download(packageToFetch, folder) { read, total ->
                _state.value = _state.value.copy(
                    downloadProgress = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else null,
                )
            }
            if (result.isSuccess) {
                _state.value = _state.value.copy(pendingDownloadSurah = null, downloadProgress = null)
                play(recitation, ayah)
            } else {
                _state.value = _state.value.copy(downloadProgress = null, downloadFailed = true)
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.value = _state.value.copy(
            pendingDownloadSurah = null,
            pendingIsContinuation = false,
            downloadProgress = null,
            downloadFailed = false,
        )
    }

    /**
     * The surah ended and the next one is not on the device. The reader's
     * Continue choice decides: off, the offer waits with the next surah's
     * name and size, so nothing is fetched behind the reader's back; on, the
     * package is fetched with the reciter being heard and plays on, and the
     * pill carries the size, the progress, and the cancel while it does
     * (owner decision).
     */
    private fun offerNextSurah() {
        // A surah that is repeating has not ended; it has begun again, so
        // there is nothing to offer and no package to fetch. This is the
        // same word the reader gave by turning the repeat on.
        if (end == EndOfAudio.REPEAT_SURAH) return
        val surah = _state.value.surah ?: return
        val recitation = recitationId ?: return
        val next = surah + 1
        if (next > 114) return
        val packageToFetch = manifest.packageFor(recitation, next) ?: return
        val first = content?.ayahsOfSurah(next)?.firstOrNull()?.number ?: return
        requestedAyah = first
        _state.value = _state.value.copy(
            pendingDownloadSurah = next,
            pendingDownloadBytes = packageToFetch.bytes,
            pendingIsContinuation = true,
            downloadProgress = null,
            downloadFailed = false,
        )
        if (end == EndOfAudio.CONTINUE) confirmDownload()
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
     * What happens at the end of the audio: the ayah again, the surah again,
     * or the surah after this one. One value, and the three switches that
     * show it in the pill and on the Listening page are three views of it
     * (owner decision).
     *
     * The ayah repeat is the player's own, so the item itself loops and the
     * surah-end offer never appears while the reader is repeating. The surah
     * repeat cannot be: the playlist grows (the next surah's ayahs are
     * appended while the reader is in this one), so `REPEAT_MODE_ALL` would
     * loop everything that is loaded rather than one surah. It is therefore
     * ours, and [restartSurah] does it from the two events a surah's end
     * arrives through.
     */
    fun setEndOfAudio(value: EndOfAudio) {
        end = value
        controller?.repeatMode =
            if (value == EndOfAudio.REPEAT_AYAH) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun stop() {
        val player = controller ?: return
        player.stop()
        player.clearMediaItems()
        _state.value = PlaybackUiState(connected = true)
    }

    private suspend fun connect(): MediaController {
        controller?.let { return it }
        return suspendCancellableCoroutine { continuation ->
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener(
                {
                    try {
                        val player = future.get().also {
                            it.addListener(listener)
                            // The reader's own pace and repeat choice arrive
                            // with the connection, so the first ayah plays the
                            // way the last session was being heard.
                            it.setPlaybackSpeed(speed)
                            it.repeatMode =
                                if (end == EndOfAudio.REPEAT_AYAH) {
                                    Player.REPEAT_MODE_ONE
                                } else {
                                    Player.REPEAT_MODE_OFF
                                }
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
        val surahName = database.surah(surah)?.nameSimple.orEmpty()
        val reciterName = database.recitations().firstOrNull { it.id == recitation }?.name.orEmpty()
        return database.ayahsOfSurah(surah).mapNotNull { ayah ->
            val audio = database.recitationAyah(recitation, ayah.number) ?: return@mapNotNull null
            val uri = store.uri(audio.audioPath) ?: return@mapNotNull null
            MediaItem.Builder()
                .setMediaId(RecitationPlaylist.mediaId(ayah.number, ayah.surah))
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("$surahName ${ayah.verseKey}")
                        .setArtist(reciterName)
                        .build(),
                )
                .build()
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // A word loop belongs to the ayah it was asked for: when the
            // player leaves that ayah, for any reason, the loop ends with it.
            // A reader who hears the next ayah was never asked to.
            if (_state.value.loopingWord != null &&
                mediaItem?.mediaId != _state.value.ayahNumber?.let {
                    RecitationPlaylist.mediaId(it, _state.value.surah ?: 0)
                }
            ) {
                _state.value = _state.value.copy(loopingWord = null)
            }
            // The surah that was being heard, read before the state moves on:
            // the surah that has just ended is the one the state last
            // published, and it is the one the repeat belongs to.
            val ended = _state.value.surah
            val player = controller
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                end == EndOfAudio.REPEAT_SURAH &&
                player != null && ended != null && restartSurah(player, ended)
            ) {
                publish(player)
                return
            }
            publish(controller)
            maybeAppendNextSurah()
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
                positionMs = 0,
            )
            offerNextSurah()
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                controller?.let { publish(it) }
                delay(200)
            }
        }
    }

    private fun publish(player: Player?) {
        val player = player ?: return
        // An ended player still carries its last media item, and reading it
        // would light the final ayah as if it were being recited. When the
        // surah is over the state says so itself, with no ayah marked.
        if (player.playbackState == Player.STATE_ENDED) return
        val id = player.currentMediaItem?.mediaId ?: return
        val ayahNumber = RecitationPlaylist.ayahOf(id) ?: return
        val surah = RecitationPlaylist.surahOf(id)
        val position = player.currentPosition.coerceAtLeast(0)
        if (segmentedAyah != ayahNumber) loadSegments(ayahNumber)
        _state.value = _state.value.copy(
            connected = true,
            isPlaying = player.isPlaying,
            recitation = recitationId,
            ayahNumber = ayahNumber,
            surah = surah,
            reference = player.currentMediaItem?.mediaMetadata?.title?.toString(),
            positionMs = position,
            wordPosition = wordAt(position),
        )
        enforceWordLoop(player, position)
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

    private fun wordAt(positionMs: Long): Int? {
        val segment = segments.firstOrNull { positionMs in it.startMs until it.endMs } ?: return null
        return segment.wordFrom + 1
    }

    /**
     * The time span of one word of the ayah now loaded, as the word table
     * numbers it, or null when there are no timings for it.
     */
    private fun spanOfWord(word: Int): Pair<Long, Long>? {
        val segment = segments.firstOrNull { it.wordFrom + 1 == word } ?: return null
        return segment.startMs to segment.endMs
    }

    /**
     * Plays the ayah now playing from one word, and loops that word until the
     * reader stops it.
     *
     * Repetition is how a verse is learned: a reader who taps a word wants to
     * hear that word, and to hear it again, without leaving the ayah. The
     * timings are already in the content (a word's start and end in the
     * reciter's own timing data), so this is a seek and a boundary, not a new
     * download and not a new setting: tapping a word and tapping it again is
     * the whole of the interface, and the pill says the word is repeating so
     * a reader is never surprised by audio that will not stop.
     */
    fun loopWord(word: Int) {
        val player = controller ?: return
        val ayah = _state.value.ayahNumber ?: return
        if (segmentedAyah != ayah) loadSegments(ayah)
        val span = spanOfWord(word) ?: return
        scope.launch {
            withContext(Dispatchers.Main) {
                player.seekTo(span.first)
                player.play()
            }
            _state.value = _state.value.copy(loopingWord = word)
        }
    }

    /** Stops the word loop and lets the ayah carry on from where it is. */
    fun clearWordLoop() {
        if (_state.value.loopingWord == null) return
        _state.value = _state.value.copy(loopingWord = null)
    }

    /**
     * When a word is looping, the player is kept inside that word's span.
     *
     * The boundary is the reader's, not the player's: the moment the reciter
     * passes the word's end, playback seeks back to its start, so the word
     * repeats until the reader stops it. The loop is cleared when the ayah
     * ends, when playback stops, and by [clearWordLoop]; it is never a
     * setting the reader has to find and turn off, because a loop that
     * surprises someone is worse than no loop.
     */
    private fun enforceWordLoop(player: Player, positionMs: Long) {
        val word = _state.value.loopingWord ?: return
        if (!player.isPlaying) {
            _state.value = _state.value.copy(loopingWord = null)
            return
        }
        val span = spanOfWord(word) ?: run {
            _state.value = _state.value.copy(loopingWord = null)
            return
        }
        val (start, end) = span
        if (positionMs >= end || positionMs < start - WORD_LOOP_SLACK_MS) {
            player.seekTo(start)
        }
    }

    private fun maybeAppendNextSurah() {        val player = controller ?: return
        val id = player.currentMediaItem?.mediaId ?: return
        val surah = RecitationPlaylist.surahOf(id) ?: return
        if (surah >= 114 || loadedThroughSurah != surah) return
        val next = surah + 1
        loadedThroughSurah = next
        val recitation = recitationId ?: return
        scope.launch {
            val items = withContext(Dispatchers.IO) { itemsFor(recitation, next) }
            if (items.isNotEmpty()) {
                withContext(Dispatchers.Main) { controller?.addMediaItems(items) }
            }
        }
    }

    private companion object {
        /** The pace the reader may choose, and its bounds. */
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 1.5f

        /**
         * How far before a looping word's start playback is allowed to sit
         * before it is drawn back in. The reciter's own timing has a little
         * slack at a word's edge, and without it the loop would catch the
         * word mid-word every time.
         */
        const val WORD_LOOP_SLACK_MS = 120L
    }
}
