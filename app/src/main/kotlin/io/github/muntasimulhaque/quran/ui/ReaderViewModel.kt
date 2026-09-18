package io.github.muntasimulhaque.quran.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PageFontStore
import io.github.muntasimulhaque.quran.data.PagePosition
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.ReadingState
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.RecitationStore
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.SavedStore
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.playback.PlaybackController
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the one decision the reader should never have to make twice: where
 * they are. The database opens in the background; the reader lands on the
 * remembered page in the remembered mode.
 */
class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val readingState = ReadingState(application)
    private val pageFonts = PageFontStore(application)
    private val savedStore = SavedStore(application)
    private val playback = PlaybackController(application, viewModelScope)
    private var contentDatabase: ContentDatabase? = null

    var surahs by mutableStateOf<List<Surah>>(emptyList())
        private set
    var page by mutableIntStateOf(1)
        private set
    var mode by mutableStateOf(ReadingMode.Mushaf)
        private set
    var position by mutableStateOf<PagePosition?>(null)
        private set
    var ready by mutableStateOf(false)
        private set
    var recitation by mutableStateOf("minshawi")
        private set
    var recitations by mutableStateOf<List<Recitation>>(emptyList())
        private set

    val content: ContentDatabase? get() = contentDatabase
    val fonts: PageFontStore get() = pageFonts
    val saved: StateFlow<List<SavedAyah>> = savedStore.saved
    val playbackState: StateFlow<PlaybackUiState> = playback.state

    init {
        viewModelScope.launch {
            val database = ContentDatabase.open(getApplication())
            contentDatabase = database
            playback.attach(database)
            surahs = database.surahs()
            recitations = database.recitations()
            val snapshot = readingState.snapshot.first()
            page = snapshot.page
            mode = snapshot.mode
            recitation = snapshot.recitation
            position = database.pagePosition(page)
            ready = true
        }
        viewModelScope.launch { savedStore.load() }
        // The reader follows the recitation: a new ayah moves the page.
        viewModelScope.launch {
            playback.state
                .map { it.ayahNumber to it.isPlaying }
                .distinctUntilChanged()
                .collect { (ayahNumber, playing) ->
                    val database = contentDatabase
                    if (!playing || ayahNumber == null || database == null) return@collect
                    val target = withContext(Dispatchers.IO) {
                        database.ayahsWithPages(listOf(ayahNumber)).firstOrNull()?.page
                    }
                    if (target != null) goToPage(target)
                }
        }
    }

    fun goToPage(newPage: Int) {
        val clamped = newPage.coerceIn(1, 604)
        if (clamped == page) return
        page = clamped
        position = contentDatabase?.pagePosition(clamped)
        viewModelScope.launch { readingState.setPage(clamped) }
    }

    fun switchMode(newMode: ReadingMode) {
        if (newMode == mode) return
        mode = newMode
        viewModelScope.launch { readingState.setMode(newMode) }
    }

    fun jumpToSurah(surah: Int) {
        val target = contentDatabase?.firstPageOfSurah(surah) ?: return
        goToPage(target)
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
        viewModelScope.launch { playback.play(recitation, ayahNumber) }
    }

    fun togglePlayback() = playback.toggle()

    fun nextAyah() = playback.next()

    fun previousAyah() = playback.previous()

    fun stopPlayback() = playback.stop()

    fun selectRecitation(id: String) {
        if (id == recitation) return
        recitation = id
        viewModelScope.launch { readingState.setRecitation(id) }
        val current = playback.state.value.ayahNumber ?: return
        viewModelScope.launch { playback.play(id, current) }
    }

    /** Which recitations have their audio on this device right now. */
    suspend fun recitationAvailability(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val database = contentDatabase ?: return@withContext emptyMap()
        val store = RecitationStore(getApplication())
        recitations.associate { recitation ->
            val path = database.recitationAyah(recitation.id, 1)?.audioPath
            recitation.id to store.isAvailable(path)
        }
    }

    override fun onCleared() {
        contentDatabase?.close()
        contentDatabase = null
        super.onCleared()
    }
}
