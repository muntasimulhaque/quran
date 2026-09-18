package io.github.muntasimulhaque.quran.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.PackDownloader
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.PackVerifier
import io.github.muntasimulhaque.quran.data.PageFontStore
import io.github.muntasimulhaque.quran.data.PagePosition
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.RecitationManifest
import io.github.muntasimulhaque.quran.data.RecitationStore
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.SavedStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.StudyRow
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.playback.PlaybackController
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.settings.ContentCheck
import io.github.muntasimulhaque.quran.ui.settings.DataNotice
import io.github.muntasimulhaque.quran.ui.mushaf.PageKey
import io.github.muntasimulhaque.quran.ui.mushaf.PageRenderer
import io.github.muntasimulhaque.quran.ui.mushaf.StartupPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

/** One row of the study list: a surah's opening, or an ayah. */
/**
 * Holds the reader's place, their choices, and the content they read.
 *
 * The place is one ayah, not a page: the Mushaf derives its page from it and
 * the study list derives its scroll position from it, so switching modes
 * never loses the reader, and closing the app never loses either.
 */
class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    private val pageFonts = PageFontStore(application)
    private val savedStore = SavedStore(application)
    private val playback = PlaybackController(application, viewModelScope)
    private val manifest = RecitationManifest.load(application)
    private val recitationStore = RecitationStore(application)
    private val store = PackStore(application)
    private val downloader = PackDownloader(application)
    private var contentDatabase: ContentDatabase? = null
    private var catalog: PackCatalog = PackCatalog.parse("{}")
    private var installed: Set<String> = emptySet()

    val renderer = PageRenderer(application)

    var settings by mutableStateOf(AppSettings())
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

    /** The Mushaf page of the reader's place, kept in step with the pager. */
    var page by mutableIntStateOf(1)
        private set
    var position by mutableStateOf<PagePosition?>(null)
        private set

    /**
     * The page picture from the last session. It is loaded before anything
     * else and shown until the reader's page paints, so a launch lands on the
     * page the reader left instead of on a blank sheet.
     */
    var startupPage by mutableStateOf<StartupPage?>(null)
        private set

    private var pageCacheJob: Job? = null

    val content: ContentDatabase? get() = contentDatabase
    val fonts: PageFontStore get() = pageFonts
    val saved: StateFlow<List<SavedAyah>> = savedStore.saved
    val playbackState: StateFlow<PlaybackUiState> = playback.state

    val translationPacks: List<ContentPack> get() = packs.filter { it.type == PackType.Translation }
    val tafsirPacks: List<ContentPack> get() = packs.filter { it.type == PackType.Tafsir }
    val installedTranslationPacks: List<ContentPack>
        get() = translationPacks.filter { it.installed }
    val enabledTafsirPacks: List<ContentPack>
        get() = tafsirPacks.filter { it.id in settings.tafsirPacks && it.installed }

    val selectedTranslation: ContentPack?
        get() = translationPacks.firstOrNull { it.id == settings.translationPack && it.installed }

    /**
     * The language the word by word aid should speak: the language of the
     * reading the reader chose, so the meanings under an ayah and the words in
     * a card match the translation beside them.
     */
    val wordLanguage: String get() = selectedTranslation?.language ?: "en"

    /** A pack the reader asked for, while it downloads. */
    data class PackSetup(
        val pack: ContentPack,
        val progress: Float?,
        val failed: Boolean = false,
    )

    var packSetup by mutableStateOf<PackSetup?>(null)
        private set

    /** What the last export or import did, said once under the rows. */
    var dataNotice by mutableStateOf<DataNotice?>(null)
        private set

    /** What the content self check is doing, or what it found. */
    var contentCheck by mutableStateOf<ContentCheck?>(null)
        private set

    private val rowCache = object : LinkedHashMap<Int, StudyRow>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, StudyRow>?): Boolean = size > 160
    }

    init {
        // The page picture comes first: it is the reader's place, and it must
        // be ready before the content database has even opened.
        viewModelScope.launch { startupPage = renderer.loadStartupPage() }
        openLibrary()
        viewModelScope.launch { savedStore.load() }
        viewModelScope.launch {
            settingsStore.settings.collect { next ->
                val database = contentDatabase
                if (database == null) {
                    settings = next
                } else {
                    applySettings(next, database)
                }
            }
        }
        // The reader follows the recitation when they asked to.
        viewModelScope.launch {
            playback.state
                .map { Triple(it.ayahNumber, it.isPlaying, settings.followReciter) }
                .distinctUntilChanged()
                .collect { (ayahNumber, playing, follow) ->
                    val database = contentDatabase
                    if (!playing || ayahNumber == null || database == null || !follow) return@collect
                    setPlace(ayahNumber, database, persist = true)
                    settings = settings.copy(ayah = ayahNumber)
                }
        }
    }

    /**
     * Opens the reader's library, or reports that it could not be opened. A
     * failure here is the one thing that would keep the reader from the text,
     * so it is a calm screen with one way forward, never a crash.
     */
    private fun openLibrary() {
        viewModelScope.launch {
            failure = false
            val application = getApplication<Application>()
            catalog = PackCatalog.load(application)
            installed = store.installed()
            val opened = runCatching { ContentDatabase.open(application, catalog, installed) }
            val database = opened.getOrElse {
                android.util.Log.e(TAG, "the content library could not be opened", it)
                failure = true
                return@launch
            }
            contentDatabase = database
            playback.attach(database)
            // The reader's settings are read once, and everything that follows
            // uses that same value: the store is not read twice for one launch.
            val stored = settingsStore.settings.first()
            settings = stored
            surahs = database.surahs()
            indexSurahs()
            packs = database.packs()
            recitations = database.recitations()
            setPlace(stored.ayah, database, persist = false)
            ready = true
            // The tafsir index is the one thing a first search would wait for,
            // so it is built now, on a worker, while the reader is reading.
            withContext(Dispatchers.IO) { database.prewarmSearch(stored.tafsirPacks.toList()) }
        }
    }

    /** After the content could not be opened, this tries again from the start. */
    fun retryOpen() {
        if (ready) return
        openLibrary()
    }

    private suspend fun applySettings(next: AppSettings, database: ContentDatabase) {
        val previous = settings
        settings = next
        if (previous.ayah != next.ayah || !ready) {
            setPlace(next.ayah, database, persist = false)
        }
        if (previous.translationPack != next.translationPack ||
            previous.wordByWord != next.wordByWord
        ) {
            clearRowCache()
        }
    }

    /**
     * The surah of every ayah and the first ayah of every surah, computed
     * once from the surah table's own counts. The Quran's ayah numbering is a
     * single ascending run, so this is arithmetic, not a query: the study
     * list asks for it on every scroll and the top bar asks on every tap.
     */
    private fun indexSurahs() {
        val byAyah = IntArray(TOTAL_AYAHS + 1)
        val firstAyah = IntArray(115)
        var next = 1
        for (surah in surahs) {
            firstAyah[surah.number] = next
            val end = (next + surah.versesCount).coerceAtMost(TOTAL_AYAHS + 1)
            for (number in next until end) byAyah[number] = surah.number
            next = end
        }
        surahAt = byAyah
        firstAyahOf = firstAyah
        surahIndexByNumber = null
    }

    private var surahAt: IntArray = IntArray(0)
    private var firstAyahOf: IntArray = IntArray(0)

    /**
     * The surah of an ayah, from the index already in memory. One array read,
     * and never a disk lookup.
     */
    fun surahOf(ayah: Int): Surah? {
        val clamped = ayah.coerceIn(1, TOTAL_AYAHS)
        val number = surahAt.getOrNull(clamped) ?: 0
        return surahByNumber[number]
    }

    /** The ayah numbers of one surah, in order, from the same memory. */
    fun ayahNumbersOfSurah(surah: Int): List<Int> {
        val first = firstAyahOf.getOrNull(surah)?.takeIf { it > 0 } ?: return emptyList()
        val count = surahByNumber[surah]?.versesCount ?: return emptyList()
        return (first until first + count).toList()
    }

    private val surahByNumber: Map<Int, Surah>
        get() = surahIndexByNumber ?: surahs.associateBy { it.number }.also { surahIndexByNumber = it }

    private var surahIndexByNumber: Map<Int, Surah>? = null

    /**
     * Moves the reader's place: the page follows the ayah, and the ayah is
     * written down when the change is the reader's own.
     */
    private suspend fun setPlace(ayah: Int, database: ContentDatabase, persist: Boolean) {
        val clamped = ayah.coerceIn(1, 6236)
        page = database.pageOfAyah(clamped)
        position = database.pagePosition(page)
        if (persist && settings.ayah != clamped) settingsStore.setAyah(clamped)
    }

    fun onPageSettled(page: Int, widthPx: Int, theme: String) {
        val database = contentDatabase ?: return
        this.page = page
        position = database.pagePosition(page)
        val ayah = database.firstAyahOfPage(page)
        if (settings.ayah != ayah) {
            settings = settings.copy(ayah = ayah)
            viewModelScope.launch { settingsStore.setAyah(ayah) }
        }
        // The page picture is written once the reader rests, not while they
        // swipe: a settle that is followed by another cancels the write.
        if (widthPx > 0) {
            pageCacheJob?.cancel()
            pageCacheJob = viewModelScope.launch {
                delay(PAGE_CACHE_DELAY_MS)
                renderer.rememberStartupPage(PageKey(page, widthPx, theme))
            }
        }
    }

    /** The picture has been replaced by the real page; it can be let go. */
    fun releaseStartupPage() {
        startupPage = null
    }

    /** The reader scrolled the study list and stopped. */
    fun onStudySettled(ayah: Int) {
        if (settings.ayah == ayah) return
        settings = settings.copy(ayah = ayah)
        viewModelScope.launch { settingsStore.setAyah(ayah) }
    }

    /** The ayah, its translation, and its reference, for copy and share. */
    suspend fun ayahShareText(ayah: Ayah): String {
        val translation = withContext(Dispatchers.IO) {
            studyRow(ayah.number, settings.translationPack)?.translation?.text
        }
        val surahName = surahs.firstOrNull { it.number == ayah.surah }?.nameSimple ?: "Surah ${ayah.surah}"
        return buildString {
            append(ayah.text)
            translation?.takeIf { it.isNotBlank() }?.let {
                append("\n\n")
                append(io.github.muntasimulhaque.quran.core.RichText.plain(it))
            }
            append("\n\n")
            append(surahName)
            append(' ')
            append(ayah.surah)
            append(':')
            append(ayah.ayah)
        }
    }

    fun jumpToAyah(ayah: Int) {
        val database = contentDatabase ?: return
        val clamped = ayah.coerceIn(1, 6236)
        settings = settings.copy(ayah = clamped)
        viewModelScope.launch { setPlace(clamped, database, persist = true) }
    }

    fun jumpToSurah(surah: Int) {
        jumpToAyah(contentDatabase?.firstAyahOfSurah(surah) ?: return)
    }

    fun switchMode(newMode: ReadingMode) {
        if (newMode == settings.mode) return
        settings = settings.copy(mode = newMode)
        viewModelScope.launch { settingsStore.setMode(newMode) }
    }

    fun setTheme(theme: AppTheme) {
        settings = settings.copy(theme = theme)
        viewModelScope.launch { settingsStore.setTheme(theme) }
    }

    fun setTextSize(size: TextSize) {
        settings = settings.copy(textSize = size)
        viewModelScope.launch { settingsStore.setTextSize(size) }
    }

    fun setKeepAwake(keep: Boolean) {
        settings = settings.copy(keepAwake = keep)
        viewModelScope.launch { settingsStore.setKeepAwake(keep) }
    }

    fun setFollowReciter(follow: Boolean) {
        settings = settings.copy(followReciter = follow)
        viewModelScope.launch { settingsStore.setFollowReciter(follow) }
    }

    fun setShowFootnotes(show: Boolean) {
        settings = settings.copy(showFootnotes = show)
        viewModelScope.launch { settingsStore.setShowFootnotes(show) }
    }

    fun setWordByWord(show: Boolean) {
        settings = settings.copy(wordByWord = show)
        viewModelScope.launch { settingsStore.setWordByWord(show) }
    }

    fun setDimLevel(level: Int) {
        settings = settings.copy(dimLevel = level.coerceIn(0, 2))
        viewModelScope.launch { settingsStore.setDimLevel(level) }
    }

    fun markLongPressHintShown() {
        if (settings.longPressHintShown) return
        settings = settings.copy(longPressHintShown = true)
        viewModelScope.launch { settingsStore.setLongPressHintShown() }
    }

    fun setTranslationPack(pack: String) {
        settings = settings.copy(translationPack = pack)
        viewModelScope.launch { settingsStore.setTranslationPack(pack) }
    }

    fun toggleTafsirPack(pack: String) {
        val next = if (pack in settings.tafsirPacks) {
            settings.tafsirPacks - pack
        } else {
            settings.tafsirPacks + pack
        }
        settings = settings.copy(tafsirPacks = next)
        viewModelScope.launch { settingsStore.setTafsirPacks(next) }
    }

    /**
     * Brings a pack onto the device, or turns it on when it is already here.
     * A download never starts without the reader having seen the size, and
     * the pack is verified before it joins the library.
     */
    fun installPack(id: String) {
        val pack = catalog.get(id) ?: return
        if (pack.installed) {
            selectPack(pack)
            return
        }
        viewModelScope.launch {
            packSetup = PackSetup(pack, progress = 0f)
            if (store.install(id)) {
                finishInstall(pack)
                return@launch
            }
            val result = downloader.download(pack) { read, total ->
                packSetup = PackSetup(
                    pack,
                    progress = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else null,
                )
            }
            if (result.isSuccess) {
                finishInstall(pack)
            } else {
                packSetup = PackSetup(pack, progress = null, failed = true)
            }
        }
    }

    private suspend fun finishInstall(pack: ContentPack) {
        packSetup = null
        reopenLibrary()
        selectPack(catalog.get(pack.id)?.copy(installed = true) ?: pack.copy(installed = true))
        val waiting = pendingPlayAyah
        if (pack.type == PackType.Recitation && waiting != null) {
            pendingPlayAyah = null
            playback.play(settings.recitation, waiting)
        }
    }

    private var pendingPlayAyah: Int? = null

    private fun selectPack(pack: ContentPack) {
        when (pack.type) {
            PackType.Translation -> setTranslationPack(pack.id)
            PackType.Tafsir -> if (pack.id !in settings.tafsirPacks) toggleTafsirPack(pack.id)
            PackType.Recitation -> Unit
            else -> Unit
        }
    }

    fun cancelPackSetup() {
        packSetup = null
    }

    fun removePack(id: String) {
        val pack = catalog.get(id) ?: return
        if (pack.shipped) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                store.remove(id)
                if (pack.type == PackType.Recitation) {
                    val folder = contentDatabase?.recitationAyah(pack.id.removePrefix("reciter-"), 1)?.audioPath
                        ?.substringBeforeLast('/')
                    if (folder != null) recitationStore.removeAll(folder)
                }
            }
            if (settings.translationPack == id) setTranslationPack("")
            if (id in settings.tafsirPacks) toggleTafsirPack(id)
            reopenLibrary()
        }
    }

    /**
     * Reopens the library with exactly the packs on the device. Attaching a
     * pack is instant, and the reader's place, notes, and settings are
     * untouched.
     */
    private suspend fun reopenLibrary() {
        val application = getApplication<Application>()
        installed = withContext(Dispatchers.IO) { store.installed() }
        val fresh = withContext(Dispatchers.IO) { ContentDatabase.open(application, catalog, installed) }
        contentDatabase?.close()
        contentDatabase = fresh
        playback.attach(fresh)
        packs = fresh.packs()
        clearRowCache()
        // The tafsir search index changes with the packs, so it is rebuilt.
        withContext(Dispatchers.IO) { fresh.prewarmSearch(settings.tafsirPacks.toList()) }
    }

    fun selectRecitation(id: String) {
        if (id == settings.recitation) return
        settings = settings.copy(recitation = id)
        viewModelScope.launch { settingsStore.setRecitation(id) }
        val current = playback.state.value.ayahNumber ?: return
        viewModelScope.launch { playback.play(id, current) }
    }

    fun studyRow(ayahNumber: Int, pack: String): StudyRow? {
        val database = contentDatabase ?: return null
        synchronized(rowCache) { rowCache[ayahNumber] }?.let { return it }
        val ayah = database.ayah(ayahNumber) ?: return null
        val words = database.wordsForAyahs(listOf(ayahNumber))[ayahNumber].orEmpty()
        val translation = database.translations(listOf(ayahNumber), pack)[ayahNumber]
        val language = packs.firstOrNull { it.id == pack }?.language ?: "en"
        val meanings = if (settings.wordByWord) {
            database.wordMeanings(ayahNumber, language)
        } else {
            emptyList()
        }
        val row = StudyRow(ayah, words, translation, meanings)
        synchronized(rowCache) { rowCache[ayahNumber] = row }
        return row
    }

    private fun clearRowCache() {
        synchronized(rowCache) { rowCache.clear() }
    }

    fun toggleSaved(ayah: Ayah) {
        viewModelScope.launch { savedStore.toggle(ayah.number) }
    }

    fun hasSavedAyahs(): Boolean = saved.value.isNotEmpty()

    /** The reader's saved work as a document, or null when there is none. */
    suspend fun exportSavedJson(): String? =
        if (saved.value.isEmpty()) null else savedStore.exportJson()

    /** Reads a document the reader chose and merges it into their own rows. */
    fun importSaved(text: String?) {
        if (text == null) {
            dataNotice = DataNotice.ImportFailed
            return
        }
        viewModelScope.launch {
            dataNotice = savedStore.importJson(text).fold(
                onSuccess = { added ->
                    if (added > 0) DataNotice.Imported(added) else DataNotice.NothingImported
                },
                onFailure = { DataNotice.ImportFailed },
            )
        }
    }

    fun reportDataNotice(notice: DataNotice?) {
        dataNotice = notice
    }

    /**
     * Reads every installed pack back and compares it with the fingerprint
     * the catalog recorded. This is the reader's own second look at the
     * library, not something that runs on its own.
     */
    fun checkContent() {
        viewModelScope.launch {
            contentCheck = ContentCheck.Running
            val damaged = PackVerifier(getApplication()).damaged(packs)
            contentCheck = ContentCheck.Done(damaged)
        }
    }

    fun setNote(ayah: Ayah, note: String?) {
        viewModelScope.launch { savedStore.setNote(ayah.number, note) }
    }

    fun removeSaved(ayahNumber: Int) {
        viewModelScope.launch { savedStore.remove(ayahNumber) }
    }

    fun playAyah(ayahNumber: Int) {
        val reciterPack = ContentDatabase.reciterPack(settings.recitation)
        if (reciterPack !in installed) {
            // The reciter's timings are a pack of their own; without it there
            // is nothing to play and nowhere to look for the audio.
            pendingPlayAyah = ayahNumber
            installPack(reciterPack)
            return
        }
        viewModelScope.launch { playback.play(settings.recitation, ayahNumber) }
    }

    fun togglePlayback() = playback.toggle()

    fun nextAyah() = playback.next()

    fun previousAyah() = playback.previous()

    fun stopPlayback() = playback.stop()

    fun confirmDownload() = playback.confirmDownload()

    fun cancelDownload() = playback.cancelDownload()

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
                    name = byNumber[surah]?.nameSimple ?: "Surah $surah",
                    bytes = bytes,
                )
            }
        }.sortedBy { it.surah }
    }

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
        contentDatabase?.recitationAyah(recitation, 1)?.audioPath?.substringBeforeLast('/')

    override fun onCleared() {
        savedStore.close()
        contentDatabase?.close()
        contentDatabase = null
    }

    private companion object {
        const val TAG = "ReaderViewModel"

        /** The Quran's ayah count; the numbering is one ascending run. */
        const val TOTAL_AYAHS = 6236

        /** How long a settled page waits before its picture is written. */
        const val PAGE_CACHE_DELAY_MS = 350L
    }
}
