package io.github.muntasimulhaque.quran.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.RecitationAyah
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
    private var content: ContentDatabase? = null
    private var controller: MediaController? = null
    private var loadedThroughSurah = 0
    private var recitationId: String? = null
    private var segments: List<io.github.muntasimulhaque.quran.data.WordSegment> = emptyList()
    private var segmentedAyah: Int? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun attach(content: ContentDatabase) {
        this.content = content
        startTicker()
    }

    fun segmentsFor(ayahNumber: Int): List<io.github.muntasimulhaque.quran.data.WordSegment> =
        if (segmentedAyah == ayahNumber) segments else emptyList()

    suspend fun play(recitation: String, ayahNumber: Int) {
        val database = content ?: return
        val location = withContext(Dispatchers.IO) {
            database.ayahsWithPages(listOf(ayahNumber)).firstOrNull()
        } ?: return
        recitationId = recitation
        val items = withContext(Dispatchers.IO) { itemsFor(recitation, location.ayah.surah) }
        if (items.isEmpty()) {
            _state.value = _state.value.copy(recitation = recitation, unavailable = true)
            return
        }
        val index = items.indexOfFirst { it.mediaId == mediaId(location.ayah.number, location.ayah.surah) }
            .coerceAtLeast(0)
        val player = connect()
        withContext(Dispatchers.Main) {
            player.setMediaItems(items, index, 0L)
            player.prepare()
            player.play()
        }
        loadedThroughSurah = location.ayah.surah
        _state.value = _state.value.copy(recitation = recitation, unavailable = false)
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
