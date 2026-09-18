package io.github.muntasimulhaque.quran.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.AyahHeader
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.PackDownloader
import io.github.muntasimulhaque.quran.data.PackStore
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
import io.github.muntasimulhaque.quran.data.TranslationText
import io.github.muntasimulhaque.quran.data.Word
import io.github.muntasimulhaque.quran.data.WordMeaning
import io.github.muntasimulhaque.quran.playback.PlaybackController
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.mushaf.PageRenderer
import kotlinx.coroutines.Dispatchers
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

    /** The Mushaf page of the reader's place, kept in step with the pager. */
    var page by mutableIntStateOf(1)
        private set
    var position by mutableStateOf<PagePosition?>(null)
        private set
    var headers by mutableStateOf<List<AyahHeader>>(emptyList())
        private set

    val content: ContentDatabase? get() = contentDatabase
    val fonts: PageFontStore get() = pageFonts
    val saved: StateFlow<List<SavedAyah>> = savedStore.saved
    val playbackState: StateFlow<PlaybackUiState> = playback.state

    /** Only reciters whose packages are published, so nothing dead is offered. */
    val availableRecitations: List<Recitation>
        get() = recitations.filter { it.id in manifest.publishedRecitations() }

    val translationPacks: List<ContentPack> get() = packs.filter { it.type == PackType.Translation }
    val tafsirPacks: List<ContentPack> get() = packs.filter { it.type == PackType.Tafsir }
    val installedTranslationPacks: List<ContentPack>
        get() = translationPacks.filter { it.installed }
    val installedTafsirPacks: List<ContentPack> get() = tafsirPacks.filter { it.installed }
    val enabledTafsirPacks: List<ContentPack>
        get() = tafsirPacks.filter { it.id in settings.tafsirPacks && it.installed }

    val selectedTranslation: ContentPack?
        get() = translationPacks.firstOrNull { it.id == settings.translationPack && it.installed }

    /** A pack the reader asked for, while it downloads. */
    data class PackSetup(
        val pack: ContentPack,
        val progress: Float?,
        val failed: Boolean = false,
    )

    var packSetup by mutableStateOf<PackSetup?>(null)
        private set

    private val rowCache = object : LinkedHashMap<Int, StudyRow>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, StudyRow>?): Boolean = size > 160
    }

    init {
        viewModelScope.launch {
            catalog = PackCatalog.load(getApplication())
            installed = store.installed()
            val database = ContentDatabase.open(getApplication(), catalog, installed)
            contentDatabase = database
            playback.attach(database)
            surahs = database.surahs()
            packs = database.packs()
            recitations = database.recitations()
            loadHeaders(database)
            applySettings(settingsStore.settings.first(), database)
            ready = true
        }
        viewModelScope.launch { savedStore.load() }
        // Warm the tafsir search index once the reader is reading.
        viewModelScope.launch {
            val database = contentDatabase ?: return@launch
            withContext(Dispatchers.IO) {
                database.prewarmSearch(settingsStore.settings.first().tafsirPacks.toList())
            }
        }
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

    private fun loadHeaders(database: ContentDatabase) {
        headers = database.ayahHeaders()
    }

    /**
     * The surah of an ayah, from the header list already in memory. The list
     * is in ayah order, so this is one array read and never touches disk.
     */
    fun surahOf(ayah: Int): Surah? {
        val number = headers.getOrNull(ayah.coerceIn(1, 6236) - 1)?.surah ?: return null
        return surahByNumber[number]
    }

    /** The ayah numbers of one surah, in order, from the same memory. */
    fun ayahNumbersOfSurah(surah: Int): List<Int> {
        val first = contentDatabase?.firstAyahOfSurah(surah) ?: return emptyList()
        val count = surahByNumber[surah]?.versesCount ?: return emptyList()
        return (first until first + count).toList()
    }

    private val surahByNumber: Map<Int, Surah> get() = surahs.associateBy { it.number }

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

    fun onPageSettled(page: Int) {
        val database = contentDatabase ?: return
        this.page = page
        position = database.pagePosition(page)
        val ayah = database.firstAyahOfPage(page)
        if (settings.ayah != ayah) {
            settings = settings.copy(ayah = ayah)
            viewModelScope.launch { settingsStore.setAyah(ayah) }
        }
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

    fun selectRecitation(id: String) {        if (id == settings.recitation) return
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
        contentDatabase?.close()
        contentDatabase = null
        super.onCleared()
    }
}
