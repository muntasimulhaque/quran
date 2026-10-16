package io.github.muntasimulhaque.quran.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.core.JuzIndex
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.ShareText
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.LastReadStore
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.LAST_MINUTE_OF_DAY
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.ReadPlace
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.data.SavedStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.forLanguage
import io.github.muntasimulhaque.quran.data.withLanguage
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.daily.DailyAyahScheduler
import io.github.muntasimulhaque.quran.playback.PlaybackController
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One row of the reading: a surah's opening, or an ayah. */
/**
 * Holds the reader's place, their choices, and the content they read.
 *
 * The place is one ayah: the reading derives its scroll position from it.
 */
class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    private val languagePreference = LanguagePreference(application)
    private val savedStore = SavedStore(application)
    private val lastReadStore = LastReadStore(application)
    private val playback = PlaybackController(application, viewModelScope)

    /**
     * The reader's library, in its own file: the open content database, the
     * packs on the device, and the downloads that put them there. The reader's
     * choices are the view model's, and the library tells it when a pack
     * arrives or leaves through the callbacks below (owner decision, this
     * session).
     */
    private val library = ContentLibrary(
        application = application,
        scope = viewModelScope,
        settingsStore = settingsStore,
        settings = { settings },
        onInstalled = { pack -> onPackInstalled(pack) },
        onOpened = { opening -> onLibraryOpened(opening) },
        onReopened = { database -> onLibraryReopened(database) },
        onRemoved = { pack, fallback -> onPackRemoved(pack, fallback) },
    )

    /**
     * The reader's choices, seeded from the synchronous mirror the store
     * keeps of the page and its switch: the first frame is composed on the
     * page the reader last chose, and everything else waits for the store's
     * own read in the init below. A plain default would paint a Night
     * reader a frame or two of Paper before the stored page landed.
     */
    var settings by mutableStateOf(settingsStore.snapshot())
        private set
    val surahs: List<Surah> get() = library.surahs
    val packs: List<ContentPack> get() = library.packs
    val recitations: List<Recitation> get() = library.recitations
    val ready: Boolean get() = library.ready

    /** True when the content itself could not be opened on this device. */
    val failure: Boolean get() = library.failure

    /**
     * The juz of every ayah number, built from the content's own boundaries
     * when the library opens. State rather than a plain field because the
     * label has to redraw the moment the index lands, though it is built
     * once per library.
     */
    private var juzAt by mutableStateOf(IntArray(0))

    /**
     * The juz of the reader's place, for reference. It is derived from the
     * place rather than kept beside it: a scroll moves the place and the
     * label in one breath, and a second value cannot fall behind (owner
     * report: the label stayed on Juz 1 while the reader scrolled into
     * Juz 2).
     */
    val juz: Int get() = juzAt.getOrElse(settings.ayah.coerceIn(1, TOTAL_AYAHS)) { 1 }

    val content: ContentDatabase? get() = library.database
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
     * The language the reader chose, which is the language the reading reads
     * and the language search reads. The translation, the tafsir, the word
     * meanings, and the surah introductions all follow it, and nothing falls
     * back to another language: a Bangla reader is never handed an English
     * column, an English meaning, or an English introduction because the
     * Bangla content has not arrived yet (owner decision, this session). An
     * install that has not chosen yet answers English, the same answer the
     * shell gives before the first screen, and the reader never reaches the
     * reading or the search field before that choice is made.
     */
    val readingLanguage: String
        get() = UiLanguage.of(settings.uiLanguage)?.tag ?: "en"

    /**
     * The translations search reads: the reader's own on switches, narrowed to
     * the language they chose. A reader who reads in Bangla searches Bangla
     * only, even when they added an English column by hand, because one search
     * field over two languages answers in two voices and the reader asked for
     * one (owner decision, this session).
     */
    val searchTranslationPacks: List<ContentPack>
        get() = enabledTranslationPacks.forLanguage(readingLanguage)

    /** The tafsirs search reads, narrowed to the reader's own language. */
    val searchTafsirPacks: List<ContentPack>
        get() = enabledTafsirPacks.forLanguage(readingLanguage)

    /** The pack on its way in, owned by the library. */
    val packSetup: ContentLibrary.PackSetup? get() = library.packSetup

    /**
     * The reading's rows, built and kept beside the reader's own state. The
     * cache carries the two caches and the library generation they were read
     * under, so anything that shapes a row clears it in one call:
     * [clearRowCache]. A row cached under one language and read back under
     * another put the Bangla translation in an English Font size sample
     * (owner report).
     */
    private val rowsCache = ReadingRowsCache(
        database = { library.database },
        translationPacks = { enabledTranslationPacks },
        language = { readingLanguage },
    )

    /**
     * The reciters the pill's chooser offers, each with what it would still
     * need for the surah the reader is hearing. It is the same list the
     * offer carries, read once per surah and per reciter rather than per
     * tap: a menu that did its reading as it opened would show the reader
     * an empty list for a frame.
     *
     * This and the ayah below are declared here, with the rest of the state,
     * and not further down where they were written first, because the
     * collector in `init` reads them. `viewModelScope` runs on the main
     * dispatcher immediately, so that collector's first read happens while
     * the view model is still being built, and a property declared after
     * `init` has no backing value yet: reading one is a crash, not a
     * default. The 3.6 capture run found it on all three legs at once.
     */
    var pillReciters by mutableStateOf<List<ListenOption>>(emptyList())
        private set

    /**
     * The ayah the pill is about: the one the reader last asked to hear.
     * The player knows the ayah it is on, but not the one it was asked for
     * while a package is on its way, and that is the one a reciter chosen
     * now has to be asked about.
     */
    private var listenTarget by mutableIntStateOf(0)

    /**
     * What the pill's chooser offers, read for one surah and one reciter. A
     * surah or a reciter the app cannot name is the same answer as a surah
     * with no package published for anyone: nothing to offer, and an empty
     * menu rather than a menu of dead rows.
     */
    private suspend fun readPillReciters(scope: PillScope) {
        // The recitation is asked before the surah is looked up, so the very
        // first reading, which happens while the library is still opening and
        // the surah index is still empty, stops here rather than caching an
        // empty surah map that the library is about to replace.
        if (scope.recitation == null) {
            pillReciters = emptyList()
            return
        }
        val surah = scope.surah ?: surahOf(listenTarget)?.number
        if (surah == null) {
            pillReciters = emptyList()
            return
        }
        pillReciters = listenOptions(surah)
    }

    /** The one reading the chooser's numbers belong to: who, and which surah. */
    private data class PillScope(val recitation: String?, val surah: Int?)

    init {
        // The library opens first: everything the reader is shown comes out
        // of it, and the frame before it is ready is the app's own paper.
        library.open()
        viewModelScope.launch { savedStore.load() }
        viewModelScope.launch { lastReadStore.load() }
        // Settings are read once while the library opens. After that the view
        // model owns them: it writes every change and keeps the in-memory copy
        // in step, so a late echo of an earlier write can never reach back and
        // move the reader's place.
        viewModelScope.launch {
            settingsStore.settings.collect { next ->
                if (library.database == null) settings = next
            }
        }
        // The reader follows the recitation when they asked to.
        viewModelScope.launch {
            playback.state
                .map { Triple(it.ayahNumber, it.isPlaying, settings.followReciter) }
                .distinctUntilChanged()
                .collect { (ayahNumber, playing, follow) ->
                    if (!playing || ayahNumber == null || library.database == null || !follow) return@collect
                    // A repeat of the place the reader is already on is not a
                    // second write. Following the reciter lands on the ayah's
                    // own top, so the depth is cleared with it.
                    if (settings.ayah != ayahNumber) settingsStore.setAyah(ayahNumber)
                    settings = settings.copy(ayah = ayahNumber, ayahOffset = 0)
                }
        }
        // The pill's chooser names every reciter with that reciter's price for
        // the surah under the reader's finger, so the list is read again
        // when the surah or the reciter changes and never on a tick: the
        // state publishes five times a second, and a directory listing that
        // often would be a directory listing the reader never asked for.
        viewModelScope.launch {
            playback.state
                .map { PillScope(it.recitation, it.surah ?: it.pendingDownloadSurah) }
                .distinctUntilChanged()
                .collect { scope -> readPillReciters(scope) }
        }
    }

    /**
     * The library is open: the player attaches to it, the reader's settings
     * are taken from the one read, and the Book's index is built.
     */
    private suspend fun onLibraryOpened(opening: ContentLibrary.Opening) {
        playback.attach(opening.database)
        settings = opening.settings
        // The reader's pace and their answer to what happens at the end of
        // the audio are theirs from the last session, and the player must be
        // told before the first ayah.
        playback.setSpeed(opening.settings.playbackSpeed)
        playback.setEndOfAudio(opening.settings.endOfAudio)
        indexSurahs(opening.surahs)
        juzAt = JuzIndex.of(opening.juzStarts, TOTAL_AYAHS)
        lastReadStore.record(opening.settings.ayah)
    }

    /** The library turned over: the player attaches to the new one. */
    private fun onLibraryReopened(database: ContentDatabase) {
        playback.attach(database)
        clearRowCache()
    }

    /**
     * Adding a pack is also choosing it: a reader who added a reciter wants
     * to hear them, a reader who added a translation wants to read it, and a
     * reader who added a word list wants to see the meanings. Anything else
     * leaves the app showing a pack the reader never asked to use.
     */
    private fun onPackInstalled(pack: ContentPack) {
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

    /**
     * A pack left the device. The reader's own choices that pointed at it go
     * with it, and a removed reciter never stays the default: the library
     * names the fallback, and the choice is written down before the library
     * closes over the new packs.
     */
    private fun onPackRemoved(pack: ContentPack, fallbackReciter: String?) {
        if (pack.id in settings.translationPacks) {
            setTranslationPacks(settings.translationPacks - pack.id)
        }
        if (pack.id in settings.tafsirPacks) toggleTafsirPack(pack.id)
        if (fallbackReciter != null) {
            settings = settings.copy(recitation = fallbackReciter)
            viewModelScope.launch { settingsStore.setRecitation(fallbackReciter) }
        }
    }

    /** After the content could not be opened, this tries again from the start. */
    fun retryOpen() {
        library.retry()
    }

    private fun indexSurahs(index: List<Surah>) {
        val byAyah = IntArray(TOTAL_AYAHS + 1)
        val firstAyah = IntArray(115)
        var next = 1
        for (surah in index) {
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
     * The reader scrolled the reading and stopped: the ayah they stopped on,
     * and the depth into it they stopped at. Both are written together, so
     * the place that comes back is the line they were reading and not the
     * top of a block that may be many screens tall.
     */
    fun onReadingSettled(ayah: Int, offset: Int) {
        val safeOffset = offset.coerceAtLeast(0)
        if (settings.ayah == ayah && settings.ayahOffset == safeOffset) return
        settings = settings.copy(ayah = ayah, ayahOffset = safeOffset)
        viewModelScope.launch { settingsStore.setAyah(ayah, safeOffset) }
        notePlace(ayah)
    }

    /**
     * Notes that the reader rested on this ayah. A scroll writes many of
     * these in a row, so only the last one inside a short window is kept:
     * the list is places the reader stopped at, not a log of every ayah their
     * finger crossed. One place per sitting is the intent, and this is what
     * makes it true when the reader is scrolling.
     */
    private fun notePlace(ayah: Int) {
        placeJob?.cancel()
        placeJob = viewModelScope.launch {
            delay(PLACE_SETTLE_MS)
            lastReadStore.record(ayah)
        }
    }

    private var placeJob: Job? = null

    /**
     * The ayah, its translation, and its reference, for copy and share. The
     * shape and the direction mark are `core/ShareText`, where the reason for
     * the mark lives and its test pins it.
     */
    suspend fun ayahShareText(ayah: Ayah): String {
        val translation = withContext(Dispatchers.IO) {
            readingRow(ayah.number)?.translations?.firstOrNull()?.text?.text
        }
        val surahName = library.surahs.firstOrNull { it.number == ayah.surah }?.nameSimple
            ?: getApplication<Application>().getString(R.string.surah_fallback_name, ayah.surah)
        return ShareText.ayah(
            arabic = ayah.text,
            translation = translation
                ?.takeIf { it.isNotBlank() }
                ?.let { RichText.plain(it) },
            reference = "$surahName ${ayah.surah}:${ayah.ayah}",
        )
    }

    fun jumpToAyah(ayah: Int) {
        if (library.database == null) return
        val clamped = ayah.coerceIn(1, 6236)
        leaveSurah(surahOf(clamped)?.number)
        // A jump lands on the ayah's own top, so the depth the reader had
        // inside another ayah never follows them here.
        settings = settings.copy(ayah = clamped, ayahOffset = 0)
        viewModelScope.launch {
            settingsStore.setAyah(clamped, 0)
            lastReadStore.record(clamped)
        }
    }

    /**
     * Opens one ayah for reading. A tap on the daily reminder asks for a
     * verse to read, and the place is written down the same way any other
     * jump writes it (owner decision, 30).
     */
    fun jumpToAyahInReading(ayah: Int) {
        jumpToAyah(ayah.coerceIn(1, 6236))
    }

    /**
     * Opens one ayah named as a reference, from a link or a shortcut. The
     * reference speaks the reader's own numbers; the running number the
     * reading uses is the surah's first ayah plus the one inside it.
     */
    fun jumpToReference(surah: Int, ayah: Int) {
        if (surah !in 1..TOTAL_SURAHS || ayah < 1) return
        val first = firstAyahOf.getOrNull(surah)?.takeIf { it > 0 } ?: return
        jumpToAyah(first + ayah - 1)
    }

    /** The reader's Retry after a failed playback, on the same ayah. */
    fun retryPlayback() = playback.retryPlayback()

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
        jumpToAyah(library.database?.firstAyahOfSurah(surah) ?: return)
    }

    /**
     * The surah whose opening item the reading should land on, once.
     * Browse asks for the top of a surah and the list answers by scrolling to
     * the surah's opening; the request is then cleared, so neither a later
     * place change nor a recomposition is pulled up with it.
     */
    var startAtSurahOpening by mutableStateOf<Int?>(null)
        private set

    /**
     * Opens a surah from Browse. A reader who has a place in that surah lands
     * on the place; a reader who does not lands on the top of the surah, its
     * opening item. The first ayah is still the place written down, so the next
     * launch returns there.
     */
    fun openSurah(surah: Int) {
        val database = library.database ?: return
        val place = lastRead.value.firstOrNull { surahOf(it.ayahNumber)?.number == surah }
        if (place != null) {
            jumpToAyah(place.ayahNumber)
            return
        }
        startAtSurahOpening = surah
        jumpToAyah(database.firstAyahOfSurah(surah))
    }

    /**
     * The top of the surah the reader is in: its opening, which is where a
     * reader flips to when they want the beginning of a surah rather than the
     * place they had got to in it. It is the same landing [openSurah] gives a
     * surah the reader has never been in, and it never carries them back down
     * to a remembered place first.
     */
    fun jumpToSurahOpening(ayah: Int) {
        val database = library.database ?: return
        val surah = surahOf(ayah)?.number ?: return
        startAtSurahOpening = surah
        jumpToAyah(database.firstAyahOfSurah(surah))
    }

    /** The reading has landed on the surah's opening; the request is done. */
    fun consumeSurahOpening(surah: Int) {
        if (startAtSurahOpening == surah) startAtSurahOpening = null
    }

    fun setTheme(theme: AppTheme) {
        settings = settings.copy(theme = theme)
        viewModelScope.launch { settingsStore.setTheme(theme) }
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

    fun setFollowReciter(follow: Boolean) {
        settings = settings.copy(followReciter = follow)
        viewModelScope.launch { settingsStore.setFollowReciter(follow) }
    }

    /** The reader's pace for the recitation; it is remembered and applied. */
    fun setPlaybackSpeed(speed: Float) {
        settings = settings.copy(playbackSpeed = speed)
        playback.setSpeed(speed)
        viewModelScope.launch { settingsStore.setPlaybackSpeed(speed) }
    }

    /**
     * What happens at the end of the audio: the ayah again, the surah again,
     * or the next surah.
     *
     * One setter for all three, because they are one answer and the plan
     * behind them keeps at most one on (owner decision). Turning one
     * on leaves the other two off, so a reader can never be waiting for a
     * surah that a repeating one will never end. The continuation is the
     * reader's word for the packages that follow, and it still announces
     * itself on the pill with its size and a cancel (owner decision).
     */
    fun setEndOfAudio(choice: EndOfAudio) {
        settings = settings.copy(endOfAudio = choice)
        playback.setEndOfAudio(choice)
        viewModelScope.launch { settingsStore.setEndOfAudio(choice) }
    }

    /** The reading keeps the reader's chosen translation; this only shows or hides it. */
    fun setShowTranslation(show: Boolean) {
        settings = settings.copy(showTranslation = show)
        viewModelScope.launch { settingsStore.setShowTranslation(show) }
    }

    /** The reading keeps the reader's chosen tafsirs; this only shows or hides them. */
    fun setShowTafsir(show: Boolean) {
        settings = settings.copy(showTafsir = show)
        viewModelScope.launch { settingsStore.setShowTafsir(show) }
    }

    /**
     * The daily reminder, armed or cleared where it is chosen. Turning it on
     * also asks for the notification permission, because a switch that
     * cannot speak is not a working switch; the alarm itself is set on the
     * worker, so the main thread never touches the system alarm manager.
     */
    fun setDailyAyah(enabled: Boolean, onPermission: () -> Unit) {
        if (enabled) onPermission()
        settings = settings.copy(dailyAyah = enabled)
        viewModelScope.launch {
            settingsStore.setDailyAyah(enabled)
            withContext(Dispatchers.IO) {
                DailyAyahScheduler.apply(
                    getApplication(),
                    enabled = enabled,
                    minuteOfDay = settings.dailyAyahMinute,
                )
            }
        }
    }

    /**
     * The moment the daily reminder arrives, in the reader's own local time.
     *
     * Moving the moment asks for the notification permission too, on the same
     * terms as the switch: a reader who came to set five in the morning wants
     * five in the morning to arrive, and the phone's answer is worth having
     * before the reminder silently does not. It is not asked when the page is
     * merely opened, only at these two acts of setting (owner decision, 2.3).
     */
    fun setDailyAyahTime(minuteOfDay: Int, onPermission: () -> Unit = {}) {
        val safe = minuteOfDay.coerceIn(0, LAST_MINUTE_OF_DAY)
        settings = settings.copy(dailyAyahMinute = safe)
        if (settings.dailyAyah) onPermission()
        viewModelScope.launch {
            settingsStore.setDailyAyahTime(safe)
            if (!settings.dailyAyah) return@launch
            withContext(Dispatchers.IO) {
                DailyAyahScheduler.apply(getApplication(), enabled = true, minuteOfDay = safe)
            }
        }
    }

    fun setWordByWord(show: Boolean) {
        if (!show) {
            settings = settings.copy(wordByWord = false)
            clearRowCache()
            viewModelScope.launch { settingsStore.setWordByWord(false) }
            return
        }
        // The meanings speak the reader's own language, the same one search
        // reads. When that language's word list is not on the device, turning
        // the aid on is asking for it: the list is fetched through the same
        // sized offer every other pack uses, and the aid turns on when it has
        // landed. A language the catalog offers no list for never turns the
        // aid on, because a switch that shows nothing is not a working switch.
        val wanted = wantedWordsPack()
        val pack = library.pack(wanted) ?: return
        if (pack.installed) {
            settings = settings.copy(wordByWord = true)
            clearRowCache()
            viewModelScope.launch { settingsStore.setWordByWord(true) }
            return
        }
        library.install(pack.id)
    }

    /**
     * The word list the reading speaks: the reader's own language's list, and
     * nothing else. There is no fallback to English, exactly as there is none
     * for the translation, the tafsir, or search (owner decision, this
     * session). The id is answered even when the catalog does not carry the
     * list, so a caller can tell "not here yet" from "not offered".
     */
    fun wantedWordsPack(): String = ContentDatabase.wordsPackId(readingLanguage)

    /**
     * The reader's language: the interface switches at once, and the content
     * follows it. The language's own translation and tafsir replace the other
     * offered language's defaults, so a reader who moves from Bangla to
     * English never keeps two defaults fighting; a pack the reader added by
     * hand, like the second translation, is left exactly where it was.
     *
     * The first choice is the reader's first arrival, so it lands in the reading
     * of Al-Fatiha, where the Arabic and its meaning meet at once
     * (owner decision, 4.5). Later changes only move the content; the place
     * stays the reader's own.
     *
     * The language's reading is fetched without another question: the
     * translation and the tafsir join the pending queue, and the queue brings
     * them one at a time, the tafsir on an unmetered connection. The word
     * list is not fetched here: it is off by default, and the toggle is its
     * own ask when the reader wants it.
     */
    fun chooseLanguage(language: UiLanguage) {
        val firstRun = settings.uiLanguage == null
        val next = settings.withLanguage(language)
        val chosen = if (firstRun) next.copy(ayah = 1, ayahOffset = 0) else next
        settings = chosen
        clearRowCache()
        // The boot-time mirror lands before anything else, so the Activity
        // that recreates for the new locale comes up speaking it.
        languagePreference.set(language.tag)
        viewModelScope.launch {
            settingsStore.setLanguage(language, next.translationPacks, next.tafsirPacks)
            if (firstRun) {
                settingsStore.setAyah(1, 0)
                lastReadStore.record(1)
            }
            settingsStore.addPendingPacks(
                setOf(language.content.translation, language.content.tafsir),
            )
            library.drainPending()
        }
        // A reader who already reads with word meanings is given the word
        // list of the new language, through the same door the toggle uses.
        if (chosen.wordByWord) library.installIfMissing(wantedWordsPack())
    }

    /**
     * The one-time long-press note has been read, so it is never shown again.
     * The reader may dismiss it early; either way the word is written down.
     */
    fun markLongPressHintShown() {
        if (settings.longPressHintShown) return
        settings = settings.copy(longPressHintShown = true)
        viewModelScope.launch { settingsStore.setLongPressHintShown() }
    }

    fun setTranslationPacks(packs: Set<String>) {
        settings = settings.copy(translationPacks = packs)
        clearRowCache()
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

    /** The reader's tap on a pack: the library brings it in and selects it. */
    fun installPack(id: String) = library.install(id)

    fun cancelPackSetup() = library.cancelPackSetup()

    /** The reader's tap to remove a pack: the library takes it off. */
    fun removePack(id: String) = library.remove(id)

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
     * Every row of one surah in a single pass, from [ReadingRowsCache]: the
     * ayahs, their words, every translation the reader turned on, and the
     * word by word aid. The cache is the one place the rows are built, so
     * the reading list, the size sample, and the share card all read the
     * same rows and a change to their shape is a change in one file.
     */
    suspend fun readingRows(surah: Int, wordByWord: Boolean): List<ReadingRow> =
        rowsCache.rows(surah, wordByWord)

    fun readingRow(ayahNumber: Int): ReadingRow? =
        rowsCache.row(ayahNumber, settings.wordByWord)

    /**
     * The ayah the font size page shows above its steps. It is short on
     * purpose: the sample is there to judge a size, and a long ayah would
     * push the steps off the screen before the reader reached them.
     */
    suspend fun sizePreviewRow(): ReadingRow? =
        withContext(Dispatchers.IO) { readingRow(SIZE_PREVIEW_AYAH) }

    /**
     * The read-ahead copy of one surah's rows, when the library has not
     * turned over since they were read.
     *
     * Null on the first arrival at a surah and after any change of packs,
     * which is when the database is the only honest answer. It is read during
     * composition, so it must stay a plain field read and nothing more.
     */
    fun cachedRows(surah: Int): List<ReadingRow>? = rowsCache.cached(surah)

    /**
     * Reads the surah after [surah] while the reader is still in this one, so
     * the turn at the end of a surah lands on text that is already measured
     * instead of on the paper.
     */
    fun readAheadSurah(surah: Int) {
        viewModelScope.launch { rowsCache.readAhead(surah, settings.wordByWord) }
    }

    private fun clearRowCache() {
        rowsCache.clear()
    }

    fun toggleSaved(ayah: Ayah) {
        viewModelScope.launch { savedStore.toggle(ayah.number) }
    }

    /** A place the reader is done with, dropped from Last Read. */
    fun forgetPlace(ayahNumber: Int) {
        viewModelScope.launch { lastReadStore.remove(ayahNumber) }
    }

    /** Writes the reader's note on one ayah. It is keyed by number because
     * the note sheet opens both from the reading and from a Notes row. */
    fun setNote(ayahNumber: Int, note: String?) {
        viewModelScope.launch { savedStore.setNote(ayahNumber, note) }
    }

    /**
     * Removes the reader's save, and the note written on the ayah with it.
     * The screen asks before this when a note exists.
     */
    fun removeSaved(ayahNumber: Int) {
        viewModelScope.launch { savedStore.unsave(ayahNumber) }
    }

    fun playAyah(ayahNumber: Int) {
        listenTarget = ayahNumber
        viewModelScope.launch {
            val database = library.database ?: return@launch
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
        val timingsMissing = !library.isInstalled(timingsId)
        val audioMissing = library.downloadedSurahs(recitation).none { it.surah == ayah.surah }
        val audioBytes = library.packageFor(recitation, ayah.surah)?.bytes
        if (!timingsMissing && !audioMissing) return null
        // A surah with no published package is not a download, it is an
        // answer: playback says so itself, and the offer stays out of it.
        if (audioMissing && audioBytes == null) return null
        val bytes = (if (timingsMissing) library.pack(timingsId)?.bytes ?: 0L else 0L) +
            (if (audioMissing) audioBytes ?: 0L else 0L)
        return bytes.takeIf { it > 0 }
    }

    private suspend fun offerFor(recitation: String, ayah: Ayah, bytes: Long): ListenOffer {
        val options = listenOptions(ayah.surah)
        return ListenOffer(
            reciter = recitation,
            reciterName = library.recitations.firstOrNull { it.id == recitation }?.name ?: recitation,
            surah = ayah.surah,
            surahName = library.surahs.firstOrNull { it.number == ayah.surah }?.nameSimple
                ?: getApplication<Application>().getString(R.string.surah_fallback_name, ayah.surah),
            ayah = ayah.number,
            bytes = bytes,
            options = options,
        )
    }

    /** What one reciter would still need for one surah, in bytes. */
    private suspend fun listenBytes(recitation: String, surah: Int): Long {
        val timingsId = ContentDatabase.reciterPack(recitation)
        val timings = if (library.isInstalled(timingsId)) 0L else library.pack(timingsId)?.bytes ?: 0L
        val audio = if (library.downloadedSurahs(recitation).any { it.surah == surah }) {
            0L
        } else {
            library.packageFor(recitation, surah)?.bytes ?: 0L
        }
        return timings + audio
    }

    /** Every reciter, with what it would still need for one surah. */
    private suspend fun listenOptions(surah: Int): List<ListenOption> =
        library.recitations.map { reciter ->
            ListenOption(
                reciter = reciter.id,
                name = reciter.name,
                bytes = listenBytes(reciter.id, surah),
            )
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
        val timings = library.pack(timingsId)
        val timingsBytes = if (library.isInstalled(timingsId)) 0L else timings?.bytes ?: 0L
        val audioBytes = if (library.downloadedSurahs(offer.reciter).any { it.surah == offer.surah }) {
            0L
        } else {
            library.packageFor(offer.reciter, offer.surah)?.bytes ?: 0L
        }
        val total = (timingsBytes + audioBytes).coerceAtLeast(1L)
        listenOffer = offer.copy(progress = 0f, failed = false)
        if (timingsBytes > 0L && timings != null) {
            val installedTimings = library.installFromAssetsOrNetwork(timings) { fraction ->
                setListenProgress(timingsBytes * fraction / total)
            }
            if (!installedTimings) {
                listenOffer = listenOffer?.copy(progress = null, failed = true)
                return
            }
            library.refresh()
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
            val ayah = library.database?.ayah(offer.ayah) ?: return@launch
            val bytes = listenBytes(reciter, ayah.surah)
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

    /**
     * An audio request waiting for the reader's word. It lives in the view
     * model rather than in the player because it may cover a pack as well as
     * a surah, and the reader should see one offer, not two.
     */
    var listenOffer by mutableStateOf<ListenOffer?>(null)
        private set
    private var listenJob: Job? = null

    /**
     * The reader chose a reciter on the pill rather than in the settings:
     * it becomes their reciter, and the ayah they are hearing moves to it
     * through the same door Play uses, so a reciter whose audio is not on
     * the device is offered with its size instead of reported as
     * unavailable (owner decision).
     */
    fun choosePillReciter(reciter: String) {
        val target = listenTarget
        if (target <= 0 || reciter == settings.recitation) return
        setRecitation(reciter)
        viewModelScope.launch {
            val ayah = library.database?.ayah(target) ?: return@launch
            val bytes = listenBytes(reciter, ayah.surah)
            if (bytes <= 0L) {
                listenOffer = null
                playback.play(reciter, target)
                return@launch
            }
            listenOffer = offerFor(reciter, ayah, bytes)
        }
    }

    fun togglePlayback() = playback.toggle()

    fun nextAyah() = playback.next()

    fun previousAyah() = playback.previous()

    fun stopPlayback() = playback.stop()

    fun confirmDownload() = playback.confirmDownload()

    fun cancelDownload() = playback.cancelDownload()

    /** Surah to bytes on disk for one reciter, among the published packages. */
    suspend fun downloadedSurahs(recitation: String): List<DownloadedSurah> =
        library.downloadedSurahs(recitation)

    /** The reader's tap to remove one surah's audio, for the reciter page. */
    suspend fun removeDownloads(recitation: String, surah: Int): Boolean =
        library.removeDownloads(recitation, surah)

    override fun onCleared() {
        // The reader's own databases are left to the process: a close here
        // can land while a save or a note is still being written on a worker
        // (cancelling the scope does not interrupt a blocking SQLite call),
        // and a closed helper under an in-flight write is a crash. The
        // process closes them when it goes, which is the standard life of a
        // single-activity app's SQLiteOpenHelper. The library retires its own
        // connection without waiting on the main thread.
        library.retire()
    }

    private companion object {
        /** The Quran's ayah count; the numbering is one ascending run. */
        const val TOTAL_AYAHS = 6236

        /** The Quran's surah count. */
        const val TOTAL_SURAHS = 114

        /** Al-Ikhlas 112:1, the short ayah the size page previews. */
        const val SIZE_PREVIEW_AYAH = 6222

        /**
         * How long a place waits before it is written down. A reader who is
         * scrolling passes an ayah every few hundred milliseconds; a place is
         * where they stopped, so the write waits for them to stop.
         */
        const val PLACE_SETTLE_MS = 1_200L
    }
}

