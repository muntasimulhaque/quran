package io.github.muntasimulhaque.quran.ui

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.PackDownloader
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.PackUpdater
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.RecitationManifest
import io.github.muntasimulhaque.quran.data.RecitationPackage
import io.github.muntasimulhaque.quran.data.RecitationStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The reader's library: the open content database, the packs on the device,
 * and the only two operations that change them, install and remove.
 *
 * The library lives beside the view model rather than inside it because it is
 * a world of its own: files, hashes, a download queue, and an open SQLite
 * connection that is swapped whenever the packs change. The view model's own
 * file is about the reader's choices and their place in the Book, and it
 * talks to this one through the door below: [open], [install], [remove], and
 * the few readers of installed content the listening flow needs. Everything
 * that a pack's arrival or removal means for the reader's choices is told
 * back through [onInstalled] and [onRemoved], so this class never has to
 * know what a translation switch or a reciter choice is (owner decision,
 * this session).
 *
 * A reader's settings are read once, at the first open, and handed to the
 * host with the opening: the store is not read twice for one launch.
 */
class ContentLibrary(
    private val application: Application,
    private val scope: CoroutineScope,
    private val settingsStore: SettingsStore,
    /** The reader's choices, as the view model holds them. */
    private val settings: () -> AppSettings,
    /**
     * A pack arrived, or an installed one was asked for: the host decides
     * what to turn on, because that is the reader's choice and not a file
     * fact.
     */
    private val onInstalled: (ContentPack) -> Unit,
    /** The first open, with the reader's settings and the Book's index. */
    private val onOpened: suspend (Opening) -> Unit,
    /** The library turned over: the host drops what it read from the old one. */
    private val onReopened: (ContentDatabase) -> Unit,
    /** A pack left, with the reciter to fall back to when one was removed. */
    private val onRemoved: (ContentPack, String?) -> Unit,
) {

    /** Everything the first open hands back to the host, once. */
    data class Opening(
        val database: ContentDatabase,
        val settings: AppSettings,
        val surahs: List<Surah>,
        val juzStarts: List<Int>,
    )

    /** A pack the reader asked for, while it downloads. */
    data class PackSetup(
        val pack: ContentPack,
        val progress: Float?,
        val failed: Boolean = false,
    )

    private val store = PackStore(application)
    private val downloader = PackDownloader(application)
    private val packUpdater = PackUpdater(application)
    private val recitationStore = RecitationStore(application)
    private val manifest = RecitationManifest.load(application)

    /**
     * The reader's library. It is Compose state, not a plain field, because a
     * pack arriving, leaving, or the interface language changing the packs it
     * reads replaces it: every screen that holds the open database must be
     * recomposed with the new one, or it keeps reading a connection the swap
     * has retired.
     */
    var database by mutableStateOf<ContentDatabase?>(null)
        private set
    var surahs by mutableStateOf<List<Surah>>(emptyList())
        private set
    var packs by mutableStateOf<List<ContentPack>>(emptyList())
        private set
    var recitations by mutableStateOf<List<Recitation>>(emptyList())
        private set
    var ready by mutableStateOf(false)
        private set

    /** True when the content itself could not be opened on this device. */
    var failure by mutableStateOf(false)
        private set

    var packSetup by mutableStateOf<PackSetup?>(null)
        private set

    private var catalog: PackCatalog = PackCatalog.parse("{}")
    private var installed: Set<String> = emptySet()
    private var draining = false

    /**
     * The one gate on opening and reopening the library. A language choice
     * can land while the first open is still reading, and both runs replace
     * the same [database] and [surahs] state; one at a time, so the last
     * writer is always the newest truth (owner decision, this session).
     */
    private val openMutex = Mutex()

    /**
     * A language's tafsir waits for Wi-Fi when the phone is on cellular; the
     * moment an unmetered network arrives while the app is open, the queue
     * drains. Nothing is listened for when nothing is pending.
     */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                scope.launch { drainPendingPacks() }
            }
        }
    }

    init {
        runCatching {
            application.getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(networkCallback)
        }
    }

    /**
     * Opens the reader's library, or reports that it could not be opened. A
     * failure here is the one thing that would keep the reader from the text,
     * so it is a calm screen with one way forward, never a crash.
     */
    fun open() {
        scope.launch {
            openMutex.withLock {
                openLibrary()
            }
        }
    }

    private suspend fun openLibrary() {
        failure = false
        // Everything that touches the disk stays off the main thread:
        // listing the packs, reading the catalog, and opening the library
        // itself are all work the reader's first frame must not wait on.
        val opened = withContext(Dispatchers.IO) {
            val installedNow = store.installed()
            // The catalog is marked with what this device has, so a pack
            // that is present is never offered as missing and a language
            // switch never re-fetches what is already here.
            val catalogNow = PackCatalog.load(application).withInstalled(installedNow)
            val opened = runCatching {
                ContentDatabase.open(application, catalogNow, installedNow)
            }
            Triple(installedNow, catalogNow, opened)
        }
        installed = opened.first
        catalog = opened.second
        val fresh = opened.third.getOrElse {
            android.util.Log.e(TAG, "the content library could not be opened", it)
            failure = true
            return
        }
        database = fresh
        // The reader's settings are read once, and everything that follows
        // uses that same value: the store is not read twice for one launch.
        val stored = settingsStore.settings.first()
        // The names the reader reads are the names their language says:
        // the Book's index is read with the language set, so Browse, the
        // reading, search, the reminder, and the share card all draw the
        // same name (owner decision, this session).
        fresh.useLanguage(stored.uiLanguage ?: "en")
        // The reader's index of the Book, read on a worker: 114 surahs
        // and the thirty juz boundaries, and the launch frame is not
        // where a query belongs.
        val index = withContext(Dispatchers.IO) {
            fresh.surahs() to fresh.juzStarts().map { it.ayah }
        }
        surahs = index.first
        packs = fresh.packs()
        recitations = fresh.recitations()
        onOpened(
            Opening(
                database = fresh,
                settings = stored,
                surahs = index.first,
                juzStarts = index.second,
            ),
        )
        ready = true
        // A language choice from this launch or an earlier one: whatever
        // is still missing arrives now, without another question.
        scope.launch { drainPendingPacks() }
        // The tafsir index is the one thing a first search would wait for,
        // so it is built now, on a worker, while the reader is reading.
        withContext(Dispatchers.IO) { fresh.prewarmSearch(stored.tafsirPacks.toList()) }
        // A pack the app has updated since this install is replaced
        // quietly, on an unmetered connection, and the library is
        // reopened over it. The reading never waits: this runs after the
        // first page is ready.
        scope.launch {
            if (packUpdater.refresh(catalog).isNotEmpty()) refresh()
        }
        // The same library is read back against the catalog's hashes once
        // a day, on the reader's behalf, so a pack a storage problem or an
        // interrupted copy has damaged is repaired without anyone asking.
        // There is no row for it and nothing is shown: a reader should
        // never meet the word for it (owner decision, this session). It is
        // unmetered, once a day, and after the first page is readable, all
        // of which PackUpdater keeps.
        scope.launch {
            if (packUpdater.dailyIntegrityRefresh(catalog).isNotEmpty()) refresh()
        }
    }

    /** After the content could not be opened, this tries again from the start. */
    fun retry() {
        if (ready) return
        open()
    }

    /**
     * Reopens the library with exactly the packs on the device. Attaching a
     * pack is instant, and the reader's place, notes, and settings are
     * untouched. Also the way an arrival the app already knows about is
     * published: a pack that was just copied or downloaded is attached by
     * this call.
     */
    suspend fun refresh() = openMutex.withLock {
        refreshLibrary()
    }

    private suspend fun refreshLibrary() {
        installed = withContext(Dispatchers.IO) { store.installed() }
        catalog = catalog.withInstalled(installed)
        val fresh = runCatching {
            withContext(Dispatchers.IO) { ContentDatabase.open(application, catalog, installed) }
        }.getOrElse { failure ->
            // A library that cannot be reopened leaves the reader on the one
            // they already have rather than taking the process down: the pack
            // is on the device, and the next open picks it up.
            android.util.Log.e(TAG, "the content library could not be reopened", failure)
            return
        }
        // The new library is published before the old one is retired, so no
        // screen can pick up a database whose turn has passed. The retire is
        // not a close: it waits for the last query still reading the old
        // connection to finish, then closes it, so an in-flight Browse or
        // reading load is never killed under the reader.
        val old = database
        database = fresh
        // The language may have changed since the last open; the names are
        // read again so a reader who just chose Bangla reads Bangla names
        // without a restart (owner decision, this session).
        fresh.useLanguage(settings().uiLanguage ?: "en")
        surahs = withContext(Dispatchers.IO) { fresh.surahs() }
        packs = fresh.packs()
        onReopened(fresh)
        // The old library is retired on a worker, because retiring it waits
        // for the last in-flight reader, and the main thread must never wait.
        if (old != null) withContext(Dispatchers.IO) { old.close() }
        // The tafsir search index changes with the packs, so it is rebuilt.
        withContext(Dispatchers.IO) { fresh.prewarmSearch(settings().tafsirPacks.toList()) }
    }

    /**
     * Brings a pack onto the device, or turns it on when it is already here.
     * The reader's tap is the ask; the pack is verified before it joins the
     * library, and what is left of the queue is written down so an
     * interrupted download continues rather than being forgotten.
     */
    fun install(id: String) {
        scope.launch {
            val pack = catalog.get(id) ?: return@launch
            if (pack.installed) {
                onInstalled(pack)
                settingsStore.removePendingPacks(setOf(id))
                return@launch
            }
            if (installNow(id)) {
                finishInstall(catalog.get(id) ?: pack)
                settingsStore.removePendingPacks(setOf(id))
            }
        }
    }

    /** A pack the reader's language asks for, fetched when it is not here. */
    fun installIfMissing(id: String) {
        val pack = catalog.get(id) ?: return
        if (pack.installed) return
        install(pack.id)
    }

    /**
     * One pack onto the device, from the app's own assets when the build
     * carries it and from its release page when it does not. Answers whether
     * it is here now. It asks nothing of the reader: the tap or the language
     * choice was the ask, and progress is shown while it runs.
     */
    private suspend fun installNow(id: String): Boolean {
        val pack = catalog.get(id) ?: return false
        if (pack.installed) return true
        // A pack already on its way is not asked for twice; a failed one is,
        // because the second attempt is the retry.
        if (packSetup?.let { it.pack.id == id && !it.failed } == true) return false
        packSetup = PackSetup(pack, progress = 0f)
        // The build's own assets are copied on a worker: a development build
        // carries every pack, and a copy is file work the reader's tap must
        // never wait on the main thread for.
        val fromAssets = runCatching {
            withContext(Dispatchers.IO) { store.install(id) }
        }.getOrDefault(false)
        if (fromAssets) return true
        val result = downloader.download(pack) { read, total ->
            packSetup = PackSetup(
                pack,
                progress = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else null,
            )
        }
        if (result.isSuccess) return true
        packSetup = PackSetup(pack, progress = null, failed = true)
        return false
    }

    private suspend fun finishInstall(pack: ContentPack) {
        packSetup = null
        // The pack becomes the reader's choice before the library reopens,
        // so the search index that warms behind the reopen knows about it.
        onInstalled(catalog.get(pack.id)?.copy(installed = true) ?: pack.copy(installed = true))
        refresh()
    }

    /**
     * Brings the packs the reader's language asked for onto the device, one
     * at a time, and remembers what is left: a download the process or the
     * connection interrupts is picked up on the next attempt instead of being
     * forgotten. The tafsir waits for an unmetered connection, so a language
     * choice never spends the reader's data on the big pack without being
     * told; the translation, which is small, arrives on any connection.
     */
    suspend fun drainPending() {
        drainPendingPacks()
    }

    private suspend fun drainPendingPacks() {
        if (draining) return
        draining = true
        try {
            while (true) {
                val next = settingsStore.pendingPacks().firstOrNull { id ->
                    val pack = catalog.get(id) ?: return@firstOrNull false
                    !pack.installed && (pack.type != PackType.Tafsir || packUpdater.canRefresh())
                } ?: break
                if (!installNow(next)) break
                finishInstall(catalog.get(next) ?: break)
                settingsStore.removePendingPacks(setOf(next))
            }
        } finally {
            draining = false
        }
    }

    fun cancelPackSetup() {
        packSetup = null
    }

    /**
     * Takes a pack off the device. A pack the reader removes is not asked for
     * again by the queue, and a removed reciter never stays the default: the
     * fallback is computed here, where the downloaded totals live, and told
     * to the host to write down.
     */
    fun remove(id: String) {
        val pack = catalog.get(id) ?: return
        if (pack.shipped) return
        val reciter = id.removePrefix(ContentDatabase.RECITER_PREFIX)
        scope.launch {
            // A pack the reader removes is not asked for again by the queue.
            settingsStore.removePendingPacks(setOf(id))
            withContext(Dispatchers.IO) {
                store.remove(id)
                if (pack.type == PackType.Recitation) {
                    val folder = database?.recitationAyah(reciter, 1)?.audioPath
                        ?.substringBeforeLast('/')
                    if (folder != null) recitationStore.removeAll(folder)
                }
            }
            val fallback = if (pack.type == PackType.Recitation && settings().recitation == reciter) {
                fallbackReciter(reciter)
            } else {
                null
            }
            onRemoved(pack, fallback)
            refresh()
        }
    }

    /**
     * The reciter the app falls back to when the selected one is removed.
     * The remaining reciter with the most audio on the device wins, because
     * that is the one the reader has actually been listening to; with nothing
     * downloaded anywhere the fallback is Husary, the quiet second voice, and
     * only then the list's own order. A removed reciter is never chosen, even
     * if a file of his lingers.
     */
    private suspend fun fallbackReciter(removed: String): String {
        val candidates = recitations.filter { it.id != removed }
        if (candidates.isEmpty()) return recitations.firstOrNull()?.id ?: removed
        val totals = downloadedTotals()
        candidates.maxByOrNull { totals[it.id]?.first ?: 0 }
            ?.takeIf { (totals[it.id]?.first ?: 0) > 0 }
            ?.let { return it.id }
        return candidates.firstOrNull { it.id == FALLBACK_RECITER }?.id ?: candidates.first().id
    }

    /** A pack brought onto the device, from the assets when it is there. */
    suspend fun installFromAssetsOrNetwork(
        pack: ContentPack,
        onProgress: (Float) -> Unit,
    ): Boolean {
        val fromAssets = runCatching {
            withContext(Dispatchers.IO) { store.install(pack.id) }
        }.getOrDefault(false)
        if (fromAssets) return true
        val result = downloader.download(pack) { read, total ->
            onProgress(if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f)
        }
        return result.isSuccess
    }

    /** Whether a pack is on the device, for the listening flow's price. */
    fun isInstalled(id: String): Boolean = id in installed

    /** The catalog's entry for a pack, for its size and its type. */
    fun pack(id: String): ContentPack? = catalog.get(id)

    /** The published package for one reciter and surah, for its size. */
    fun packageFor(recitation: String, surah: Int): RecitationPackage? =
        manifest.packageFor(recitation, surah)

    /** Surah to bytes on disk for one reciter, among the published packages. */
    suspend fun downloadedSurahs(recitation: String): List<DownloadedSurah> = withContext(Dispatchers.IO) {
        val folder = reciterFolder(recitation) ?: return@withContext emptyList()
        val byNumber = surahs.associateBy { it.number }
        manifest.surahs(recitation).mapNotNull { surah ->
            val bytes = recitationStore.bytesForSurah(folder, surah)
            if (bytes <= 0) {
                null
            } else {
                DownloadedSurah(
                    surah = surah,
                    name = byNumber[surah]?.nameSimple
                        ?: application.getString(R.string.surah_fallback_name, surah),
                    bytes = bytes,
                )
            }
        }.sortedBy { it.surah }
    }

    /** Every reciter's downloaded count and total, newest-fallback order. */
    suspend fun downloadedTotals(): Map<String, Pair<Int, Long>> = withContext(Dispatchers.IO) {
        recitations.associate { recitation ->
            val folder = reciterFolder(recitation.id)
            val found = if (folder == null) {
                emptyList()
            } else {
                manifest.surahs(recitation.id).mapNotNull { surah ->
                    val bytes = recitationStore.bytesForSurah(folder, surah)
                    if (bytes > 0) bytes else null
                }
            }
            recitation.id to (found.size to found.sum())
        }
    }

    suspend fun removeDownloads(recitation: String, surah: Int): Boolean = withContext(Dispatchers.IO) {
        val folder = reciterFolder(recitation) ?: return@withContext false
        recitationStore.removeSurah(folder, surah) > 0
    }

    private fun reciterFolder(recitation: String): String? =
        database?.recitationAyah(recitation, 1)?.audioPath?.substringBeforeLast('/')

    /**
     * The process is going away; the connection is let go without waiting on
     * the main thread for whatever speculative work is still reading it.
     */
    fun retire() {
        runCatching {
            application.getSystemService(ConnectivityManager::class.java)
                ?.unregisterNetworkCallback(networkCallback)
        }
        database?.retire()
        database = null
    }

    private companion object {
        const val TAG = "ContentLibrary"

        /** The reciter the app falls to when nothing is downloaded anywhere. */
        const val FALLBACK_RECITER = "husary"
    }
}
