package io.github.muntasimulhaque.quran.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import io.github.muntasimulhaque.quran.data.LastReadStore
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.ReadPlace
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
import io.github.muntasimulhaque.quran.data.TranslationLine
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.withLanguage
import io.github.muntasimulhaque.quran.playback.PlaybackController
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.settings.ContentCheck
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
    private val languagePreference = LanguagePreference(application)
    private val pageFonts = PageFontStore(application)
    private val savedStore = SavedStore(application)
    private val lastReadStore = LastReadStore(application)
    private val playback = PlaybackController(application, viewModelScope)
    private val manifest = RecitationManifest.load(application)
    private val recitationStore = RecitationStore(application)
    private val store = PackStore(application)
    private val downloader = PackDownloader(application)

    /**
     * The reader's library. It is Compose state, not a plain field, because a
     * pack arriving, leaving, or the interface language changing the packs it
     * reads replaces it: every screen that holds the open database must be
     * recomposed with the new one, or it keeps reading a connection the swap
     * has retired.
     */
    private var contentDatabase by mutableStateOf<ContentDatabase?>(null)
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

    /** Where the reader has been reading, newest first. */
    val lastRead: StateFlow<List<ReadPlace>> = lastReadStore.places
    val playbackState: StateFlow<PlaybackUiState> = playback.state

    val translationPacks: List<ContentPack> get() = packs.filter { it.type == PackType.Translation }
    val tafsirPacks: List<ContentPack> get() = packs.filter { it.type == PackType.Tafsir }

    /**
     * The translations the reader turned on and has on the device, in catalog
     * order. More than one may be on, and each is drawn in its own column
     * under the ayah; the first is the one search and share read.
     */
    val enabledTranslationPacks: List<ContentPack>
        get() = translationPacks.filter { it.installed && it.id in settings.translationPacks }

    val enabledTafsirPacks: List<ContentPack>
        get() = tafsirPacks.filter { it.id in settings.tafsirPacks && it.installed }

    /**
     * The word by word aid should speak the language of the reading the reader
     * chose, so the meanings under an ayah and the words in a card match the
     * translation beside them. With more than one translation on, the first
     * one decides. The choice counts even before its pack is on the device:
     * a reader who chose Bangla wants Bangla meanings, and the word list is
     * what the settings toggle adds for them.
     */
    val wordLanguage: String
        get() = translationPacks.firstOrNull { it.id in settings.translationPacks }?.language ?: "en"

    /** A pack the reader asked for, while it downloads. */
    data class PackSetup(
        val pack: ContentPack,
        val progress: Float?,
        val failed: Boolean = false,
    )

    var packSetup by mutableStateOf<PackSetup?>(null)
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
        viewModelScope.launch { lastReadStore.load() }
        // Settings are read once while the library opens. After that the view
        // model owns them: it writes every change and keeps the in-memory copy
        // in step, so a late echo of an earlier write can never reach back and
        // move the reader's place.
        viewModelScope.launch {
            settingsStore.settings.collect { next ->
                if (contentDatabase == null) settings = next
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
            installed = store.installed()
            // The catalog is marked with what this device has, so a pack that
            // is present is never offered as missing and a language switch
            // never re-fetches what is already here.
            catalog = PackCatalog.load(application).withInstalled(installed)
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
            lastReadStore.record(stored.ayah, stored.mode)
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
        // A page the reader turned is a new place. A page they arrived at by
        // switching modes is not: their ayah is already on it, and the exact
        // ayah is worth keeping, so the study view opens on the same one.
        if (database.pageOfAyah(settings.ayah) != page) {
            val ayah = database.firstAyahOfPage(page)
            if (settings.ayah != ayah) {
                leaveSurah(surahOf(ayah)?.number)
                settings = settings.copy(ayah = ayah)
                viewModelScope.launch { settingsStore.setAyah(ayah) }
                notePlace(ayah, ReadingMode.Mushaf)
            }
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
        notePlace(ayah, ReadingMode.Study)
    }

    /**
     * Notes that the reader rested on this ayah. A scroll writes many of
     * these in a row, so only the last one inside a short window is kept:
     * the list is places the reader stopped at, not a log of every ayah their
     * finger crossed. One place per sitting is the intent, and this is what
     * makes it true when the reader is scrolling.
     */
    private fun notePlace(ayah: Int, mode: ReadingMode) {
        placeJob?.cancel()
        placeJob = viewModelScope.launch {
            delay(PLACE_SETTLE_MS)
            lastReadStore.record(ayah, mode)
        }
    }

    private var placeJob: Job? = null

    /** The ayah, its translation, and its reference, for copy and share. */
    suspend fun ayahShareText(ayah: Ayah): String {
        val translation = withContext(Dispatchers.IO) {
            studyRow(ayah.number)?.translations?.firstOrNull()?.text?.text
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
        val mode = settings.mode
        leaveSurah(surahOf(clamped)?.number)
        settings = settings.copy(ayah = clamped)
        viewModelScope.launch {
            setPlace(clamped, database, persist = true)
            lastReadStore.record(clamped, mode)
        }
    }

    /**
     * A download offer belongs to the surah it was asked for. Moving to
     * another surah takes it away: a request for one surah's audio must never
     * sit over a different surah, waiting for a tap that no longer means what
     * its own text said. The surah is read from the in-memory index, so a
     * page turn pays nothing for the check.
     */
    private fun leaveSurah(surah: Int?) {
        if (surah == null) return
        listenOffer?.takeIf { it.surah != surah }?.let { cancelListen() }
        playback.state.value.pendingDownloadSurah?.takeIf { it != surah }?.let {
            playback.cancelDownload()
        }
    }

    fun jumpToSurah(surah: Int) {
        jumpToAyah(contentDatabase?.firstAyahOfSurah(surah) ?: return)
    }

    /**
     * The surah whose opening item the study reading should land on, once.
     * Browse asks for the top of a surah and the list answers by scrolling to
     * the surah's opening; the request is then cleared, so neither a later
     * place change nor a recomposition is pulled up with it.
     */
    var startAtSurahOpening by mutableStateOf<Int?>(null)
        private set

    /**
     * Opens a surah from Browse. A reader who has a place in that surah lands
     * on the place; a reader who does not lands on the top of the surah, its
     * opening item in study mode and the page of its first ayah in the
     * Mushaf. The first ayah is still the place written down, so the next
     * launch returns there.
     */
    fun openSurah(surah: Int) {
        val database = contentDatabase ?: return
        val place = lastRead.value.firstOrNull { surahOf(it.ayahNumber)?.number == surah }
        if (place != null) {
            jumpToAyah(place.ayahNumber)
            return
        }
        startAtSurahOpening = surah
        jumpToAyah(database.firstAyahOfSurah(surah))
    }

    /** The study list has landed on the surah's opening; the request is done. */
    fun consumeSurahOpening(surah: Int) {
        if (startAtSurahOpening == surah) startAtSurahOpening = null
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

    /**
     * The page follows the system's own day and night. The choice of page is
     * kept, not cleared: turning the switch off returns the reader to the
     * page they had, which is the page the night was standing in for.
     */
    fun setAutoNight(follow: Boolean) {
        settings = settings.copy(autoNight = follow)
        viewModelScope.launch { settingsStore.setAutoNight(follow) }
    }

    /** One kind of text changes size; nothing else moves with it. */
    fun setTypeSize(role: TypeRole, step: Float) {
        val value = TextSize.step(step)
        settings = when (role) {
            TypeRole.Arabic -> settings.copy(arabicSize = value)
            TypeRole.Translation -> settings.copy(translationSize = value)
            TypeRole.Tafsir -> settings.copy(tafsirSize = value)
            TypeRole.Words -> settings.copy(wordsSize = value)
        }
        viewModelScope.launch { settingsStore.setTypeSize(role, value) }
    }

    fun setKeepAwake(keep: Boolean) {
        settings = settings.copy(keepAwake = keep)
        viewModelScope.launch { settingsStore.setKeepAwake(keep) }
    }

    fun setFollowReciter(follow: Boolean) {
        settings = settings.copy(followReciter = follow)
        viewModelScope.launch { settingsStore.setFollowReciter(follow) }
    }

    fun setWordByWord(show: Boolean) {
        if (!show) {
            settings = settings.copy(wordByWord = false)
            viewModelScope.launch { settingsStore.setWordByWord(false) }
            return
        }
        // The meanings speak the language of the first translation on. When
        // that language's word list is not on the device, turning the aid on
        // is asking for it: the list is fetched through the same sized offer
        // every other pack uses, and the aid turns on when it has landed.
        val wanted = wantedWordsPack()
        val pack = catalog.get(wanted)
        if (pack == null || pack.installed) {
            settings = settings.copy(wordByWord = true)
            viewModelScope.launch { settingsStore.setWordByWord(true) }
            return
        }
        installPack(pack.id)
    }

    /** The word list the reading speaks: the translation's language, or English. */
    fun wantedWordsPack(): String =
        ContentDatabase.wordsPackId(wordLanguage)
            .takeIf { catalog.get(it) != null }
            ?: ContentDatabase.WORDS_PACK

    /**
     * The reader's language: the interface switches at once, and the content
     * follows it. The language's own translation and tafsir replace the other
     * offered language's defaults, so a reader who moves from Bangla to
     * English never keeps two defaults fighting; a pack the reader added by
     * hand, like As-Sa'di, is left exactly where it was.
     */
    fun chooseLanguage(language: UiLanguage) {
        val next = settings.withLanguage(language)
        settings = next
        // The boot-time mirror lands before anything else, so the Activity
        // that recreates for the new locale comes up speaking it.
        languagePreference.set(language.tag)
        viewModelScope.launch {
            settingsStore.setLanguage(language, next.translationPacks, next.tafsirPacks)
        }
        // A reader who already reads with word meanings is given the word
        // list of the new language, through the same door the toggle uses.
        if (next.wordByWord) ensureWordsPack()
    }

    /** The active language's word list, fetched when it is not yet here. */
    private fun ensureWordsPack() {
        val pack = catalog.get(wantedWordsPack()) ?: return
        if (pack.installed) return
        installPack(pack.id)
    }

    fun markLongPressHintShown() {
        if (settings.longPressHintShown) return
        settings = settings.copy(longPressHintShown = true)
        viewModelScope.launch { settingsStore.setLongPressHintShown() }
    }

    fun setTranslationPacks(packs: Set<String>) {
        settings = settings.copy(translationPacks = packs)
        viewModelScope.launch { settingsStore.setTranslationPacks(packs) }
    }

    /**
     * Turns one translation on or off. A reader may read more than one; the
     * first one on is the one search, share, and the word by word aid read.
     */
    fun toggleTranslationPack(pack: String) {
        val next = if (pack in settings.translationPacks) {
            settings.translationPacks - pack
        } else {
            settings.translationPacks + pack
        }
        setTranslationPacks(next)
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
        // A pack already on its way is not asked for twice; a failed one is,
        // because the second tap is the reader's retry.
        if (packSetup?.let { it.pack.id == id && !it.failed } == true) return
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
        // The pack becomes the reader's choice before the library reopens,
        // so the search index that warms behind the reopen knows about it.
        selectPack(catalog.get(pack.id)?.copy(installed = true) ?: pack.copy(installed = true))
        reopenLibrary()
    }

    /**
     * Adding a pack is also choosing it: a reader who added a reciter wants
     * to hear them, a reader who added a translation wants to read it, and a
     * reader who added a word list wants to see the meanings. Anything else
     * leaves the app showing a pack the reader never asked to use.
     */
    private fun selectPack(pack: ContentPack) {
        when (pack.type) {
            PackType.Translation -> if (pack.id !in settings.translationPacks) {
                toggleTranslationPack(pack.id)
            }
            PackType.Tafsir -> if (pack.id !in settings.tafsirPacks) toggleTafsirPack(pack.id)
            PackType.Recitation ->
                selectRecitation(pack.id.removePrefix(ContentDatabase.RECITER_PREFIX))
            PackType.Words -> if (!settings.wordByWord) {
                settings = settings.copy(wordByWord = true)
                viewModelScope.launch { settingsStore.setWordByWord(true) }
            }
            else -> Unit
        }
    }

    fun cancelPackSetup() {
        packSetup = null
    }

    fun removePack(id: String) {
        val pack = catalog.get(id) ?: return
        if (pack.shipped) return
        val reciter = id.removePrefix(ContentDatabase.RECITER_PREFIX)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                store.remove(id)
                if (pack.type == PackType.Recitation) {
                    val folder = contentDatabase?.recitationAyah(reciter, 1)?.audioPath
                        ?.substringBeforeLast('/')
                    if (folder != null) recitationStore.removeAll(folder)
                }
            }
            if (id in settings.translationPacks) {
                setTranslationPacks(settings.translationPacks - id)
            }
            if (id in settings.tafsirPacks) toggleTafsirPack(id)
            // A removed reciter cannot stay the default: the reader hears
            // somebody else now, and the choice is written down with the
            // removal, before the library closes over the new packs.
            if (pack.type == PackType.Recitation && settings.recitation == reciter) {
                val fallback = fallbackReciter(reciter)
                settings = settings.copy(recitation = fallback)
                settingsStore.setRecitation(fallback)
            }
            reopenLibrary()
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

    /**
     * Reopens the library with exactly the packs on the device. Attaching a
     * pack is instant, and the reader's place, notes, and settings are
     * untouched.
     */
    private suspend fun reopenLibrary() {
        val application = getApplication<Application>()
        installed = withContext(Dispatchers.IO) { store.installed() }
        catalog = catalog.withInstalled(installed)
        val fresh = withContext(Dispatchers.IO) { ContentDatabase.open(application, catalog, installed) }
        // The new library is published before the old one is retired, so no
        // screen can pick up a database whose turn has passed. The retire is
        // not a close: it waits for the last query still reading the old
        // connection to finish, then closes it, so an in-flight Browse or
        // study load is never killed under the reader.
        val old = contentDatabase
        contentDatabase = fresh
        playback.attach(fresh)
        packs = fresh.packs()
        clearRowCache()
        // The old library is retired on a worker, because retiring it waits
        // for the last in-flight reader, and the main thread must never wait.
        if (old != null) withContext(Dispatchers.IO) { old.close() }
        // The tafsir search index changes with the packs, so it is rebuilt.
        withContext(Dispatchers.IO) { fresh.prewarmSearch(settings.tafsirPacks.toList()) }
    }

    /**
     * The reader's reciter, chosen in settings or on the offer: it becomes
     * the default, and an ayah already playing moves to it. The move goes
     * through the same door Play uses, so a reciter whose timings or audio
     * are not on the device is offered, never reported as unavailable.
     */
    fun selectRecitation(id: String) {
        if (!setRecitation(id)) return
        playback.state.value.ayahNumber?.let { playAyah(it) }
    }

    /**
     * Writes the default reciter down, and answers whether it changed. The
     * offer chooses through this too, so a reader who picks a reciter on the
     * pill is heard from that reciter from then on.
     */
    private fun setRecitation(id: String): Boolean {
        if (id == settings.recitation) return false
        settings = settings.copy(recitation = id)
        viewModelScope.launch { settingsStore.setRecitation(id) }
        return true
    }

    /**
     * Every row of one surah in a single pass: the ayahs, their words, every
     * translation the reader turned on, and the word by word aid. One query
     * per source instead of one per ayah, and the study list is drawn
     * complete, so the reader's place lands exactly and nothing shifts under
     * them while they read.
     */
    suspend fun studyRows(surah: Int, wordByWord: Boolean): List<StudyRow> =
        withContext(Dispatchers.IO) {
            val database = contentDatabase ?: return@withContext emptyList()
            val ayahs = database.ayahsOfSurah(surah)
            if (ayahs.isEmpty()) return@withContext emptyList()
            val numbers = ayahs.map { it.number }
            val words = database.wordsForAyahs(numbers)
            val lines = translationLines(database, numbers)
            val meanings = if (wordByWord) {
                database.wordMeanings(numbers, wordLanguage)
            } else {
                emptyMap()
            }
            ayahs.map { ayah ->
                StudyRow(
                    ayah = ayah,
                    words = words[ayah.number].orEmpty(),
                    translations = lines[ayah.number].orEmpty(),
                    meanings = meanings[ayah.number].orEmpty(),
                )
            }
        }

    /**
     * Every translation the reader turned on for a set of ayahs, each read
     * from its own pack and named, so a row draws its lines without knowing
     * which packs are on.
     */
    private fun translationLines(
        database: ContentDatabase,
        numbers: List<Int>,
    ): Map<Int, List<TranslationLine>> {
        val out = HashMap<Int, MutableList<TranslationLine>>(numbers.size)
        for (pack in enabledTranslationPacks) {
            val found = database.translations(numbers, pack.id)
            for ((number, text) in found) {
                out.getOrPut(number) { mutableListOf() }
                    .add(TranslationLine(pack.id, pack.name, pack.language, text))
            }
        }
        return out
    }

    fun studyRow(ayahNumber: Int): StudyRow? {
        val database = contentDatabase ?: return null
        synchronized(rowCache) { rowCache[ayahNumber] }?.let { return it }
        val ayah = database.ayah(ayahNumber) ?: return null
        val words = database.wordsForAyahs(listOf(ayahNumber))[ayahNumber].orEmpty()
        val translations = translationLines(database, listOf(ayahNumber))[ayahNumber].orEmpty()
        val meanings = if (settings.wordByWord) {
            database.wordMeanings(ayahNumber, wordLanguage)
        } else {
            emptyList()
        }
        val row = StudyRow(ayah, words, translations, meanings)
        synchronized(rowCache) { rowCache[ayahNumber] = row }
        return row
    }

    /**
     * The ayah the font size page shows above its steps. It is short on
     * purpose: the sample is there to judge a size, and a long ayah would
     * push the steps off the screen before the reader reached them.
     */
    suspend fun sizePreviewRow(): StudyRow? =
        withContext(Dispatchers.IO) { studyRow(SIZE_PREVIEW_AYAH) }

    private fun clearRowCache() {
        synchronized(rowCache) { rowCache.clear() }
    }

    fun toggleSaved(ayah: Ayah) {
        viewModelScope.launch { savedStore.toggle(ayah.number) }
    }

    /** A place the reader is done with, dropped from Last Read. */
    fun forgetPlace(ayahNumber: Int) {
        viewModelScope.launch { lastReadStore.remove(ayahNumber) }
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
        viewModelScope.launch {
            val database = contentDatabase ?: return@launch
            val ayah = database.ayah(ayahNumber) ?: return@launch
            val recitation = settings.recitation
            val missing = missingForListen(recitation, ayah)
            if (missing == null) {
                playback.play(recitation, ayahNumber)
                return@launch
            }
            listenOffer = offerFor(recitation, ayah, missing)
        }
    }

    /**
     * Listening needs two things that may both be missing: the reciter's word
     * timings, and the surah's own audio. They are asked for once, named in
     * full, with the reciter changeable in the offer itself. Nothing is
     * fetched until the reader says so.
     */
    private suspend fun missingForListen(recitation: String, ayah: Ayah): Long? {
        val timingsId = ContentDatabase.reciterPack(recitation)
        val timingsMissing = timingsId !in installed
        val audioMissing = downloadedSurahs(recitation).none { it.surah == ayah.surah }
        val audioBytes = manifest.packageFor(recitation, ayah.surah)?.bytes
        if (!timingsMissing && !audioMissing) return null
        // A surah with no published package is not a download, it is an
        // answer: playback says so itself, and the offer stays out of it.
        if (audioMissing && audioBytes == null) return null
        val bytes = (if (timingsMissing) catalog.get(timingsId)?.bytes ?: 0L else 0L) +
            (if (audioMissing) audioBytes ?: 0L else 0L)
        return bytes.takeIf { it > 0 }
    }

    private suspend fun offerFor(recitation: String, ayah: Ayah, bytes: Long): ListenOffer {
        val options = recitations.map { reciter ->
            ListenOption(
                reciter = reciter.id,
                name = reciter.name,
                bytes = listenBytes(reciter.id, ayah),
            )
        }
        return ListenOffer(
            reciter = recitation,
            reciterName = recitations.firstOrNull { it.id == recitation }?.name ?: recitation,
            surah = ayah.surah,
            surahName = surahs.firstOrNull { it.number == ayah.surah }?.nameSimple
                ?: "Surah ${ayah.surah}",
            ayah = ayah.number,
            bytes = bytes,
            options = options,
        )
    }

    /** What one reciter would still need for one ayah, in bytes. */
    private suspend fun listenBytes(recitation: String, ayah: Ayah): Long {
        val timingsId = ContentDatabase.reciterPack(recitation)
        val timings = if (timingsId in installed) 0L else catalog.get(timingsId)?.bytes ?: 0L
        val audio = if (downloadedSurahs(recitation).any { it.surah == ayah.surah }) {
            0L
        } else {
            manifest.packageFor(recitation, ayah.surah)?.bytes ?: 0L
        }
        return timings + audio
    }

    /** The reader approved the offer: fetch the timings and the audio, then play. */
    fun confirmListen() {
        val offer = listenOffer ?: return
        if (offer.progress != null) return
        listenJob?.cancel()
        listenJob = viewModelScope.launch { fetchForListen(offer) }
    }

    private suspend fun fetchForListen(offer: ListenOffer) {
        val timingsId = ContentDatabase.reciterPack(offer.reciter)
        val timings = catalog.get(timingsId)
        val timingsBytes = if (timingsId in installed) 0L else timings?.bytes ?: 0L
        val audioBytes = if (downloadedSurahs(offer.reciter).any { it.surah == offer.surah }) {
            0L
        } else {
            manifest.packageFor(offer.reciter, offer.surah)?.bytes ?: 0L
        }
        val total = (timingsBytes + audioBytes).coerceAtLeast(1L)
        listenOffer = offer.copy(progress = 0f, failed = false)
        if (timingsBytes > 0L && timings != null) {
            val installedTimings = installFromAssetsOrNetwork(timings) { fraction ->
                setListenProgress(timingsBytes * fraction / total)
            }
            if (!installedTimings) {
                listenOffer = listenOffer?.copy(progress = null, failed = true)
                return
            }
            reopenLibrary()
            setRecitation(offer.reciter)
        }
        if (audioBytes > 0L) {
            val fetched = playback.fetchSurahAudio(offer.reciter, offer.surah) { fraction ->
                setListenProgress((timingsBytes + audioBytes * fraction) / total)
            }
            if (!fetched) {
                listenOffer = listenOffer?.copy(progress = null, failed = true)
                return
            }
        }
        listenOffer = null
        playback.play(offer.reciter, offer.ayah)
    }

    private fun setListenProgress(value: Float) {
        listenOffer = listenOffer?.copy(progress = value.coerceIn(0f, 1f))
    }

    /** The reader changed the reciter on the offer: it becomes their reciter. */
    fun chooseListenReciter(reciter: String) {
        val offer = listenOffer ?: return
        if (reciter == offer.reciter || offer.progress != null) return
        setRecitation(reciter)
        viewModelScope.launch {
            val ayah = contentDatabase?.ayah(offer.ayah) ?: return@launch
            val bytes = listenBytes(reciter, ayah)
            // A reciter whose audio is already on the device needs no offer.
            if (bytes <= 0L) {
                listenOffer = null
                playback.play(reciter, offer.ayah)
                return@launch
            }
            listenOffer = offerFor(reciter, ayah, bytes)
        }
    }

    fun cancelListen() {
        listenJob?.cancel()
        listenJob = null
        listenOffer = null
    }

    /** A pack brought onto the device, from the assets when it is there. */
    private suspend fun installFromAssetsOrNetwork(
        pack: ContentPack,
        onProgress: (Float) -> Unit,
    ): Boolean {
        if (store.install(pack.id)) return true
        val result = downloader.download(pack) { read, total ->
            onProgress(if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f)
        }
        return result.isSuccess
    }

    /**
     * An audio request waiting for the reader's word. It lives in the view
     * model rather than in the player because it may cover a pack as well as
     * a surah, and the reader should see one offer, not two.
     */
    var listenOffer by mutableStateOf<ListenOffer?>(null)
        private set
    private var listenJob: Job? = null

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
        lastReadStore.close()
        // The process is going away; the connection is let go without waiting
        // on the main thread for whatever speculative work is still reading it.
        contentDatabase?.retire()
        contentDatabase = null
    }

    private companion object {
        const val TAG = "ReaderViewModel"

        /** The Quran's ayah count; the numbering is one ascending run. */
        const val TOTAL_AYAHS = 6236

        /** Al-Ikhlas 112:1, the short ayah the size page previews. */
        const val SIZE_PREVIEW_AYAH = 6222

        /** The reciter the app falls to when nothing is downloaded anywhere. */
        const val FALLBACK_RECITER = "husary"

        /** How long a settled page waits before its picture is written. */
        const val PAGE_CACHE_DELAY_MS = 350L

        /**
         * How long a place waits before it is written down. A reader who is
         * scrolling passes an ayah every few hundred milliseconds; a place is
         * where they stopped, so the write waits for them to stop.
         */
        const val PLACE_SETTLE_MS = 1_200L
    }
}

