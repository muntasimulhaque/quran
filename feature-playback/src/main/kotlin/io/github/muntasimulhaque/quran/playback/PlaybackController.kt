package io.github.muntasimulhaque.quran.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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
 * player always knows where it is without another query.
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
        val index = items.indexOfFirst { it.mediaId == mediaId(location.ayah.number, location.ayah.surah) }
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
     * The surah ended and the next one is not on the device. Offer it, with its
     * size, instead of leaving the reader at a silent stop or fetching it
     * behind their back.
     */
    private fun offerNextSurah() {
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
                        val player = future.get().also { it.addListener(listener) }
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
                .setMediaId(mediaId(ayah.number, ayah.surah))
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
            publish(controller)
            maybeAppendNextSurah()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
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
        val ayahNumber = id.substringBefore(':').toIntOrNull() ?: return
        val surah = id.substringAfter(':').toIntOrNull()
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

    private fun maybeAppendNextSurah() {
        val player = controller ?: return
        val id = player.currentMediaItem?.mediaId ?: return
        val surah = id.substringAfter(':').toIntOrNull() ?: return
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

    private fun mediaId(ayahNumber: Int, surah: Int) = "$ayahNumber:$surah"
}
