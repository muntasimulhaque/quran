package io.github.muntasimulhaque.quran.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PageFontStore
import io.github.muntasimulhaque.quran.data.PagePosition
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.ReadingState
import io.github.muntasimulhaque.quran.data.Surah
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Holds the one decision the reader should never have to make twice: where
 * they are. The database opens in the background; the reader lands on the
 * remembered page in the remembered mode.
 */
class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val readingState = ReadingState(application)
    private val pageFonts = PageFontStore(application)
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

    val content: ContentDatabase? get() = contentDatabase
    val fonts: PageFontStore get() = pageFonts

    init {
        viewModelScope.launch {
            val database = ContentDatabase.open(getApplication())
            contentDatabase = database
            surahs = database.surahs()
            val snapshot = readingState.snapshot.first()
            page = snapshot.page
            mode = snapshot.mode
            position = database.pagePosition(page)
            ready = true
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

    override fun onCleared() {
        contentDatabase?.close()
        contentDatabase = null
        super.onCleared()
    }
}
