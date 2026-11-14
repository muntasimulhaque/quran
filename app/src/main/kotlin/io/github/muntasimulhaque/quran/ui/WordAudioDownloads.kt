package io.github.muntasimulhaque.quran.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.github.muntasimulhaque.quran.data.WordAudioDownloader
import io.github.muntasimulhaque.quran.data.WordAudioManifest
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
 * The word meanings toggle is the reader's word for the pair. Turning it on
 * asks for the surah being read on the network the reader is on, because the
 * tap is the ask and the page has shown the size. Every surah the reader
 * reaches afterwards arrives quietly on an unmetered connection, after the
 * page is readable, the way the root pack does, and the moment Wi-Fi arrives
 * a surah that was waiting starts. Turning the aid off cancels a transfer
 * still in flight and keeps the packs already here.
 *
 * The state is the one line the word meanings page reads: the current
 * surah's size, what the whole Book would weigh, and whether the surah is
 * downloaded, bundled, waiting for Wi-Fi, or failed.
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
) {

    private val downloader = WordAudioDownloader(context)
    private val _state = MutableStateFlow(WordAudioState(totalBytes = manifest.totalBytes()))
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
                totalBytes = manifest.totalBytes(),
            )
            val result = downloader.download(pack) { read, total ->
                _state.update {
                    it.copy(
                        surah = surah,
                        bytes = pack.bytes,
                        read = read,
                        installed = false,
                        failed = false,
                        waiting = false,
                        totalBytes = total,
                    )
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

    fun positions(surah: Int): Map<Int, Set<Int>> = store.positions(surah)

    fun remove(surah: Int) {
        store.remove(surah)
        refresh(surah)
    }

    fun clear() {
        job?.cancel()
        job = null
        pendingSurah = null
        store.clear()
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
            totalBytes = manifest.totalBytes(),
            installed = surah in 1..114 && store.isInstalled(surah),
            bundled = surah in 1..114 && store.isBundled(surah),
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
