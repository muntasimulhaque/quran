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
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TranslationText
import io.github.muntasimulhaque.quran.data.Word
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
sealed interface StudyItem {
    data class Header(val surah: Surah, val firstAyah: Int) : StudyItem
    data class AyahItem(val header: AyahHeader) : StudyItem
}

/** Everything one study row needs, loaded off the main thread. */
data class StudyRow(
    val ayah: Ayah,
    val words: List<Word>,
    val translation: TranslationText?,
)

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
    private var contentDatabase: ContentDatabase? = null

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
    var studyItems by mutableStateOf<List<StudyItem>>(emptyList())
        private set
    var headers by mutableStateOf<List<AyahHeader>>(emptyList())
        private set
    private var itemIndexByAyah = IntArray(6237) { 1 }

    val content: ContentDatabase? get() = contentDatabase
    val fonts: PageFontStore get() = pageFonts
    val saved: StateFlow<List<SavedAyah>> = savedStore.saved
    val playbackState: StateFlow<PlaybackUiState> = playback.state

    /** Only reciters whose packages are published, so nothing dead is offered. */
    val availableRecitations: List<Recitation>
        get() = recitations.filter { it.id in manifest.publishedRecitations() }

    val translationPacks: List<ContentPack> get() = packs.filter { it.type == PackType.Translation }
    val tafsirPacks: List<ContentPack> get() = packs.filter { it.type == PackType.Tafsir }
    val enabledTafsirPacks: List<ContentPack>
        get() = tafsirPacks.filter { it.id in settings.tafsirPacks }

    private val rowCache = object : LinkedHashMap<Int, StudyRow>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, StudyRow>?): Boolean = size > 160
    }

    init {
        viewModelScope.launch {
            val database = ContentDatabase.open(getApplication())
            contentDatabase = database
            playback.attach(database)
            surahs = database.surahs()
            packs = database.packs()
            recitations = database.recitations()
            buildStudyList(database)
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
        if (previous.translationPack != next.translationPack) clearRowCache()
    }

    private fun buildStudyList(database: ContentDatabase) {
        val headerRows = database.ayahHeaders()
        headers = headerRows
        val byNumber = surahs.associateBy { it.number }
        val items = ArrayList<StudyItem>(headerRows.size + 114)
        val indexByAyah = IntArray(6237) { 1 }
        var currentSurah = -1
        for (header in headerRows) {
            if (header.surah != currentSurah) {
                currentSurah = header.surah
                byNumber[currentSurah]?.let { items += StudyItem.Header(it, header.number) }
            }
            indexByAyah[header.number] = items.size
            items += StudyItem.AyahItem(header)
        }
        studyItems = items
        itemIndexByAyah = indexByAyah
    }

    fun studyIndexOf(ayah: Int): Int = itemIndexByAyah.getOrElse(ayah.coerceIn(1, 6236)) { 1 }

    /**
     * The surah of an ayah, from the header list already in memory. The list
     * is in ayah order, so this is one array read and never touches disk.
     */
    fun surahOf(ayah: Int): Surah? {
        val number = headers.getOrNull(ayah.coerceIn(1, 6236) - 1)?.surah ?: return null
        return surahByNumber[number]
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
        val row = StudyRow(ayah, words, translation)
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
        viewModelScope.launch { playback.play(settings.recitation, ayahNumber) }
    }

    fun togglePlayback() = playback.toggle()

    fun nextAyah() = playback.next()

    fun previousAyah() = playback.previous()

    fun stopPlayback() = playback.stop()

    fun confirmDownload() = playback.confirmDownload()

    fun cancelDownload() = playback.cancelDownload()

    /** Surah to bytes on disk for one reciter, among the published packages. */
    suspend fun downloadedSurahs(recitation: String): Map<Int, Long> = withContext(Dispatchers.IO) {
        val folder = reciterFolder(recitation) ?: return@withContext emptyMap()
        manifest.surahs(recitation)
            .mapNotNull { surah ->
                val bytes = recitationStore.bytesForSurah(folder, surah)
                if (bytes > 0) surah to bytes else null
            }
            .toMap()
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
