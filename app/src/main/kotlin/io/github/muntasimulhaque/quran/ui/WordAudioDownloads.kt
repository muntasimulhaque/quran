package io.github.muntasimulhaque.quran.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.github.muntasimulhaque.quran.data.WordAudioDownloader
import io.github.muntasimulhaque.quran.data.WordAudioManifest
import io.github.muntasimulhaque.quran.data.WordAudioOnDevice
import io.github.muntasimulhaque.quran.data.WordAudioPackage
import io.github.muntasimulhaque.quran.data.WordAudioState
import io.github.muntasimulhaque.quran.data.WordAudioStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The word pronunciation downloads, one surah at a time.
 *
 * Two acts are the reader's word for the pair. The word meanings toggle asks
 * for the surah being read on the network the reader is on, because the tap is
 * the ask. And the word sheet's speaker may be pressed for any word: the tap
 * is approval enough, so a surah whose voices are not here is fetched then and
 * the word sounds when its pack lands (owner decision, this session).
 *
 * Every surah the reader reaches afterwards arrives quietly on an unmetered
 * connection, after the page is readable, the way the root pack does, and the
 * moment Wi-Fi arrives a surah that was waiting starts. Turning the aid off
 * cancels a transfer still in flight and keeps the packs already here.
 *
 * The state is what the words page and the word sheet read: the current
 * surah's size, the bytes already here, and whether the surah is installed,
 * waiting for Wi-Fi, or failed.
 *
 * Every question about the disk is asked on a worker. A surah's folder holds
 * up to six thousand files, so listing it on the frame the reader turned the
 * page would be a listing on the reading's own path.
 */
internal class WordAudioDownloads(
    private val context: Context,
    private val scope: CoroutineScope,
    private val store: WordAudioStore = WordAudioStore(context),
    private val manifest: WordAudioManifest = WordAudioManifest.load(context),
    private val onInstalled: (Int) -> Unit = {},
    /**
     * The one transfer. It is a value and not a call to the downloader so a
     * test can hand in a pack it writes itself, and the tap's whole path
     * (fetch, commit, publish, hand back) is measured with no network (owner
     * decision, this session).
     */
    private val fetch: suspend (WordAudioPackage, (Long, Long) -> Unit) -> Result<Unit> =
        { pack, onProgress -> WordAudioDownloader(context).download(pack, onProgress) },
) {

    private val _state = MutableStateFlow(WordAudioState())
    val state: StateFlow<WordAudioState> = _state.asStateFlow()

    private var job: Job? = null
    private var pendingSurah: Int? = null
    private var current = 0
    private var enabled = false

    /**
     * The surah that is waiting for an unmetered connection. The line under
     * the switch says it waits for Wi-Fi, so the moment Wi-Fi arrives the
     * pack starts: a promise the page makes and the app does not keep is a
     * dead control in words.
     */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: android.net.Network,
            capabilities: NetworkCapabilities,
        ) {
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) return
            val waiting = _state.value
            if (!enabled || !waiting.waiting || waiting.installed) return
            request(waiting.surah, unmeteredOnly = false)
        }
    }

    init {
        runCatching {
            context.getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(networkCallback)
        }
    }

    /** The window is going away; the network is no longer listened to. */
    fun retire() {
        runCatching {
            context.getSystemService(ConnectivityManager::class.java)
                ?.unregisterNetworkCallback(networkCallback)
        }
    }

    /** The word aid's switch, and the current surah's first ask. */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            job?.cancel()
            job = null
            pendingSurah = null
            refresh(current)
            return
        }
        request(current, unmeteredOnly = false)
    }

    /** The reader moved to another surah; a later pack waits for Wi-Fi. */
    fun onSurah(surah: Int) {
        if (surah == current) return
        current = surah
        refresh(surah)
        if (enabled) request(surah, unmeteredOnly = true)
    }

    fun request(surah: Int, unmeteredOnly: Boolean) {
        if (surah !in 1..114) return
        current = surah
        val pack = manifest.packageFor(surah)
        if (pack == null) {
            refresh(surah)
            return
        }
        if (pendingSurah == surah && job?.isActive == true) return
        if (unmeteredOnly && !isUnmetered()) {
            scope.launch {
                val base = withContext(Dispatchers.IO) { stateFor(surah) }
                if (current == surah && !base.installed) {
                    _state.value = base.copy(waiting = true)
                } else if (current == surah) {
                    _state.value = base
                }
            }
            return
        }
        job?.cancel()
        pendingSurah = surah
        job = scope.launch {
            val installed = withContext(Dispatchers.IO) { store.isInstalled(surah) }
            if (installed) {
                pendingSurah = null
                val state = withContext(Dispatchers.IO) { stateFor(surah) }
                if (current == surah) _state.value = state
                return@launch
            }
            _state.value = WordAudioState(
                surah = surah,
                bytes = pack.bytes,
            )
            // The line under the finger counts in whole kilobytes, and the
            // reading stands behind it, so the transfer publishes one state
            // per percent rather than one per socket read (owner decision,
            // this session).
            val result = fetch(pack) { read, total ->
                val step = (total / 100).coerceAtLeast(1L)
                _state.update { previous ->
                    val counting = previous.surah == surah && previous.read > 0
                    if (counting && read < total && read - previous.read < step) {
                        previous
                    } else {
                        previous.copy(
                            surah = surah,
                            bytes = pack.bytes,
                            read = read,
                            installed = false,
                            failed = false,
                            waiting = false,
                        )
                    }
                }
            }
            pendingSurah = null
            if (result.isSuccess) {
                store.invalidate(surah)
                val state = withContext(Dispatchers.IO) { stateFor(surah) }
                if (current == surah) _state.value = state
                onInstalled(surah)
            } else if (result.exceptionOrNull() !is CancellationException) {
                val state = withContext(Dispatchers.IO) { stateFor(surah) }
                if (current == surah) _state.value = state.copy(failed = true)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        pendingSurah = null
        refresh(current)
    }

    /** True when this word's own file is on the device, bundled or downloaded. */
    fun hasWord(surah: Int, ayah: Int, position: Int): Boolean =
        store.uri(surah, ayah, position) != null

    /** True when the published manifest carries a package for this surah. */
    fun hasPack(surah: Int): Boolean = surah in 1..114 && manifest.packageFor(surah) != null

    /** True when this surah's words are on the device, bundled or downloaded. */
    fun isInstalled(surah: Int): Boolean = store.isInstalled(surah)

    /**
     * Everything downloaded, for the page that offers to remove it, one surah
     * at a time. The bundled Al-Fatiha is the app's own asset and is never
     * counted.
     */
    fun onDevice(): WordAudioOnDevice = store.onDevice()

    /**
     * The reader's tap to take back one surah's voices. The page reads the
     * device again after it, so the row goes when its files are gone; the
     * current surah's state is read again too, because the aid may be looking
     * at the surah the reader just emptied.
     */
    fun remove(surah: Int) {
        store.remove(surah)
        refresh(current)
    }

    /** Reads the current surah's state off the frame and publishes it. */
    private fun refresh(surah: Int) {
        scope.launch {
            val state = withContext(Dispatchers.IO) { stateFor(surah) }
            if (current == surah) _state.value = state
        }
    }

    private fun stateFor(surah: Int): WordAudioState {
        val pack = manifest.packageFor(surah)
        return WordAudioState(
            surah = surah,
            bytes = pack?.bytes ?: 0,
            installed = surah in 1..114 && store.isInstalled(surah),
        )
    }

    private fun isUnmetered(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val active = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(active) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
