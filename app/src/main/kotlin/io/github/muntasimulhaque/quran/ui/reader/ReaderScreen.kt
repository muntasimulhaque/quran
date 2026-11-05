package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.readingMetrics
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import io.github.muntasimulhaque.quran.ui.PermissionNotice
import io.github.muntasimulhaque.quran.ui.kit.AppSnackbar
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.rememberReducedMotion
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.reading.AyahCard
import io.github.muntasimulhaque.quran.ui.reading.ReadingReveal
import io.github.muntasimulhaque.quran.ui.search.SearchArrival
import io.github.muntasimulhaque.quran.ui.reading.AyahNoteSheet
import io.github.muntasimulhaque.quran.ui.reading.ReadingList
import io.github.muntasimulhaque.quran.ui.reading.SelectionClearer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * How long the screen stays awake after the last touch while only the
 * recitation is heard. A minute gives the reader time to settle and to notice
 * the wash, and then the phone's own screen timeout is allowed to do its work.
 * The recitation keeps playing in the foreground service, so the page can go
 * dark without ending the listening (owner decision, this session).
 */
private const val LISTENING_AWAKE_MILLIS = 60_000L

/**
 * How long the screen stays awake after the last touch while the page is being
 * read. Reading needs the page, and a long ayah can hold the eye for a while
 * without a touch, so this window is generous; a reader who is actually
 * reading scrolls far more often than this (owner decision, this session).
 */
private const val READING_AWAKE_MILLIS = 10 * 60_000L

/** Which sheet is over the reader, if any. */
internal enum class ReaderSheet { None, Browse, Search, Settings }

/**
 * The unsaved note drafts, written into the window's saved state as strings.
 * A note is the reader's own writing, and a closed sheet is not a decision to
 * throw it away: a draft survives the sheet, a rotation, and a process death
 * until the note is saved or cleared (owner decision, this session).
 */
private val NoteDraftsSaver = listSaver<Map<Int, String>, String>(
    save = { drafts -> drafts.map { (ayah, text) -> "$ayah\u0000$text" } },
    restore = { entries ->
        entries.mapNotNull { entry ->
            val split = entry.indexOf('\u0000')
            if (split <= 0) return@mapNotNull null
            entry.substring(0, split).toIntOrNull()?.let { ayah ->
                ayah to entry.substring(split + 1)
            }
        }.toMap()
    },
)

/**
 * The reading screen: one reading, and chrome that only appears when it is
 * asked for and leaves when the page moves. Everything else in the app happens
 * in a sheet over this surface or in the ayah card, so the page itself is
 * never crowded.
 */
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    content: ContentDatabase,
    onReminderPermission: () -> Unit,
    notificationsBlocked: () -> Boolean,
    exactGrantWithheld: () -> Boolean,
    onOpenNotificationSettings: () -> Unit,
    onAskExactAlarm: () -> Unit,
    /** The one permission answer the reading still has to say. */
    permissionNotice: PermissionNotice? = null,
    /** The notice has been handed to the Snackbar. */
    onPermissionNoticeShown: () -> Unit = {},
    /** A shortcut or a deep link that asks for the search field once. */
    openSearch: Boolean = false,
) {
    val settings = viewModel.settings
    val reducedMotion by rememberReducedMotion()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    // The recited word travels on a flow of its own and is handed down as a
    // State that this screen never reads, so the tick that moves the wash
    // invalidates the one ayah being heard instead of the page (owner
    // decision, this session).
    val playedWord = viewModel.playedWord.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    // The Save marks, by ayah, so a door can say what the reader kept. A note
    // alone is not a Save: the two marks are the reader's own and the app
    // keeps them apart (owner decision, this session).
    val savedAyahs = remember(saved) {
        saved.filter { it.marked }.map { it.ayahNumber }.toSet()
    }
    val lastRead by viewModel.lastRead.collectAsStateWithLifecycle()

    // The bar is the reader's ask and stays until they put it away or start
    // moving the page. No timer, no first-run reveal.
    var chrome by rememberSaveable { mutableStateOf(false) }
    // The ayah whose own menu is open, and the door it hangs from. One at a
    // time, never across a rotation: a popup is a moment and not a place
    // (owner decision, this session).
    var menu by remember { mutableStateOf<AyahMenuRequest?>(null) }
    // The reading's text selection is a moment, not a place: it is cleared by
    // the next touch anywhere on the page or the chrome, and it is never saved
    // across a rotation (owner decision, this session).
    val selectionClearer = remember { SelectionClearer() }
    var cardAyah by remember { mutableStateOf<Ayah?>(null) }
    // The tafsir pack a search hit opened the card for; null for the normal
    // Tafsir pill, where the single enabled pack (or the door list) decides.
    var cardTafsirPack by remember { mutableStateOf<String?>(null) }
    // The note sheet is keyed by the ayah's number, not by the ayah itself:
    // the pill opens it, and the number is all it needs to write the note and
    // keep the ayah with it.
    var cardNote by rememberSaveable { mutableStateOf<Int?>(null) }
    // The reader's unsaved note text, by ayah. A sheet that closes without
    // saving is not a discarded note: the next opening hands the words back
    // (owner decision, this session).
    var noteDrafts by rememberSaveable(stateSaver = NoteDraftsSaver) {
        mutableStateOf<Map<Int, String>>(emptyMap())
    }
    var noteSaving by remember { mutableStateOf(false) }
    var noteFailed by remember { mutableStateOf(false) }
    // The saved ayah the reader asked to remove while a note is still on it:
    // the confirmation is up until they answer, and the ayah stays saved
    // until they do. Null when nothing is being asked about.
    var pendingRemove by remember { mutableStateOf<Int?>(null) }
    // Whether the remove question's answer belongs to the reading's Snackbar
    // (true) or to Browse's own (false). The question can be raised from
    // either surface, and the reading's Snackbar is behind the Browse sheet
    // when the reader is still standing in it (owner decision, this session).
    var pendingRemoveUndoable by remember { mutableStateOf(true) }
    // The open sheet survives the window, not only the composition: a change
    // of language recreates the Activity, and a sheet kept in plain `remember`
    // closed with the old window while the reader was still in it. The reader
    // chose a language from the Language page and was returned to the reading;
    // the sheet is written into the saved state, so the choice is answered
    // where it was made (owner decision, 21).
    var sheet by rememberSaveable { mutableStateOf(ReaderSheet.None) }
    // The sheet a result came from, one level deep. A search or Browse result
    // closes its sheet, and Back from the reading reopens it at the same
    // query, tab, and place. The state holder keeps the sheet's own saveable
    // state while it is out of composition.
    var returnTo by rememberSaveable { mutableStateOf<ReaderSheet?>(null) }
    val sheetStates = rememberSaveableStateHolder()
    // A sheet the reader closed is forgotten after the provider has left the
    // composition; removing a live provider's state can race its own save.
    var clearSheet by remember { mutableStateOf<ReaderSheet?>(null) }
    // The search arrival to reveal on the page, and the height the foot of the
    // page actually takes, so the reading can keep its last line clear of it.
    var reveal by remember { mutableStateOf<ReadingReveal?>(null) }
    var bottomStackHeight by remember { mutableIntStateOf(0) }
    var touch by remember { mutableIntStateOf(0) }
    // The screen is kept awake while the reader is here, but not forever. The
    // window timer releases the flag; a touch takes it back.
    var keepAwake by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val announceView = LocalView.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val text = resources.getString(message.text)
            if (message.announceOnly) {
                announceView.announceForAccessibility(text)
            } else {
                val result = snackbar.showSnackbar(
                    message = text,
                    actionLabel = message.action?.let { resources.getString(it) },
                    withDismissAction = false,
                    duration = if (message.action != null) {
                        SnackbarDuration.Long
                    } else {
                        SnackbarDuration.Short
                    },
                )
                if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
            }
        }
    }
    // The one permission answer that arrives outside this composition. The
    // notice is cleared before it is drawn, so a recomposition cannot show it
    // twice; the action opens the phone's own page for the thing it names.
    LaunchedEffect(permissionNotice) {
        val notice = permissionNotice ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = resources.getString(notice.text),
            actionLabel = resources.getString(notice.action),
            withDismissAction = false,
            duration = SnackbarDuration.Long,
        )
        onPermissionNoticeShown()
        if (result == SnackbarResult.ActionPerformed) {
            when (notice.kind) {
                PermissionNotice.Kind.Notifications -> onOpenNotificationSettings()
                PermissionNotice.Kind.ExactAlarm -> onAskExactAlarm()
            }
        }
    }
    // What a Share is doing: the card the reader is looking at in the share
    // sheet, and whether its picture is being captured right now. The two are
    // separate because the sheet stays up while the capture runs: a reader who
    // sends the card to one app can send it to another from the same sheet,
    // instead of raising the sheet again for every receiver (owner report,
    // this session). `sharing` is non-null for the whole visit, so the sheet
    // is composed from the first preview until the reader dismisses it.
    var sharing by remember { mutableStateOf<ShareCard?>(null) }
    var capturing by remember { mutableStateOf(false) }
    // The width the share sheet measured and chose for this card. The door
    // hands it back here, and the capture draws that one card: the preview
    // and the picture are never two shapes (owner decision, this version).
    var captureWidth by remember { mutableStateOf(ShareCardWidths.first()) }

    /** Opens a sheet from the reading and forgets any stale state it held. */
    fun openSheet(target: ReaderSheet) {
        if (sheet == target) return
        returnTo = null
        menu = null
        selectionClearer.clear()
        sheetStates.removeState(target.name)
        sheet = target
    }

    /** Closes a sheet the reader closed themselves; a result keeps its state. */
    fun closeSheet() {
        val closing = sheet
        sheet = ReaderSheet.None
        if (returnTo == null) clearSheet = closing
    }

    // A sheet the reader closed is forgotten once its provider has left and
    // its own saveable state has been written back.
    LaunchedEffect(clearSheet, sheet) {
        if (sheet != ReaderSheet.None) return@LaunchedEffect
        clearSheet?.let {
            sheetStates.removeState(it.name)
            clearSheet = null
        }
    }

    /**
     * A result closes its sheet but keeps it as the reading's way back. The
     * arrival is the text, so the bar the reader left through is not waiting
     * over it (owner decision, this session).
     */
    fun resultOpened(from: ReaderSheet) {
        returnTo = from
        sheet = ReaderSheet.None
        chrome = false
    }

    /**
     * A search result arrives with its evidence: the matched Arabic words,
     * the words whose meanings matched, or the tafsir passage that carried
     * the match. The reveal is transient and never changes a saved setting.
     */
    fun openSearchResult(arrival: SearchArrival) {
        returnTo = ReaderSheet.Search
        sheet = ReaderSheet.None
        chrome = false
        menu = null
        cardAyah = null
        cardTafsirPack = null
        when (arrival) {
            is SearchArrival.AyahMatch -> {
                val hasEvidence = arrival.arabicWords.isNotEmpty() ||
                    arrival.meaningWords.isNotEmpty() ||
                    arrival.translationPack != null
                reveal = if (hasEvidence) {
                    ReadingReveal(
                        ayah = arrival.ayah.number,
                        words = arrival.arabicWords,
                        meanings = arrival.meaningWords.toSet(),
                        showTranslation = arrival.translationPack != null,
                        wordByWord = arrival.meaningWords.isNotEmpty(),
                    )
                } else {
                    null
                }
                cardNote = arrival.note?.let { arrival.ayah.number }
                viewModel.jumpToAyah(arrival.ayah.number)
            }
            is SearchArrival.TafsirMatch -> {
                reveal = null
                cardNote = null
                viewModel.jumpToAyah(arrival.ayah.number)
                cardTafsirPack = arrival.pack
                cardAyah = arrival.ayah
            }
        }
    }

    // A sheet, an ayah's actions, a note, or a share is the reader using the
    // app, not listening to it from across the room. Those keep the screen
    // awake; the plain reading and the plain listening are the two that give
    // it back after their own window (owner decision, this session).
    val usingControls = sheet != ReaderSheet.None || menu != null ||
        cardAyah != null || cardNote != null || pendingRemove != null || sharing != null

    // A search reveal belongs to the arrival, not to the page. It leaves on
    // the reader's first touch (the reading's own handlers below) or after a
    // few seconds, whichever comes first.
    LaunchedEffect(reveal) {
        if (reveal == null) return@LaunchedEffect
        delay(8_000)
        reveal = null
    }

    // The wash under an ayah: the one whose menu is open, or the one whose
    // note is open over it. A note opened from Browse has no door behind it,
    // but the reader is still looking at the ayah the note belongs to, so it
    // wears the same mark while the sheet is up.
    val washedAyah = menu?.ayah?.number ?: cardNote

    // What a tap on the page, and the phone's own Back, answer at the foot of
    // the reading. The pill wears no close any more, so this is the one way a
    // raised control is put away (owner decision, this session): a paused or
    // failed recitation goes with its state, a package in flight is left to
    // its own X, and a download offer is answered "not now". The rule lives
    // in [raisedAnswer] so it can be pinned without a screen.
    val raised = raisedAnswer(playback, viewModel.listenOffer)
    // The answer is read at the moment of the tap and never captured: the
    // reading's paper keeps the first tap handler it was built with, so a
    // value captured here would be the foot's answer from the first
    // composition and the tap would answer nothing (found by the offer's own
    // page-tap leg).
    val putAwayRaised: () -> Unit = {
        when (raisedAnswer(viewModel.playbackState.value, viewModel.listenOffer)) {
            RaisedAnswer.Nothing -> Unit
            RaisedAnswer.StopRecitation -> viewModel.stopPlayback()
            RaisedAnswer.CancelDownload -> viewModel.cancelDownload()
            RaisedAnswer.CancelOffer -> viewModel.cancelListen()
        }
    }

    // The phone's back button dismisses the ayah pill first, then whatever the
    // foot of the page has raised, and only closes the app when nothing is
    // raised. A reader who tapped an ayah's door means its menu, and back is
    // the gesture they already use to put a thing away. The sheets (the ayah
    // card, the note, Browse, Search, Settings) are their own windows and take
    // back through their own handlers; this one is for the reading's own
    // raised surfaces.
    //
    // Back answers the page's own rule and nothing more: it never silences a
    // recitation that is playing, and it never aborts a package on its way.
    androidx.activity.compose.BackHandler(enabled = menu != null) {
        menu = null
        touch++
    }
    androidx.activity.compose.BackHandler(
        enabled = menu == null && raised != RaisedAnswer.Nothing,
    ) {
        putAwayRaised()
        touch++
    }
    // A package that is still arriving is left to its own X. Back used to
    // finish the Activity, which cancelled the ViewModel's download; the
    // reader who pressed Back to leave the pill also threw away the transfer
    // they had asked for. The handler consumes Back while a content pack or
    // a recitation package is in flight, and does nothing else.
    androidx.activity.compose.BackHandler(
        enabled = menu == null && raised == RaisedAnswer.Nothing &&
            (viewModel.packSetup != null || playback.downloadProgress != null),
    ) {
        touch++
    }

    // A result is a door the reader came through, and Back returns to it
    // before it closes the reading. This handler is registered last, so it
    // wins over the in-flight guard above: a download may keep running while
    // the reader goes back to the results that started it.
    androidx.activity.compose.BackHandler(
        enabled = menu == null && raised == RaisedAnswer.Nothing && returnTo != null,
    ) {
        sheet = returnTo ?: ReaderSheet.None
        returnTo = null
        touch++
    }

    // Reading with the screen awake is part of reading, and listening with
    // the screen off is part of listening: the link between the two is the
    // reader's own touch. The window timer below releases the flag, and the
    // phone's normal screen timeout takes it from there. The recitation is a
    // foreground service, so it keeps playing when the screen goes dark (owner
    // decision, this session).
    val view = LocalView.current
    DisposableEffect(view) {
        onDispose { view.keepScreenOn = false }
    }
    SideEffect { view.keepScreenOn = keepAwake }

    LaunchedEffect(touch, playback.isPlaying, usingControls, keepAwake) {
        if (!keepAwake || usingControls) return@LaunchedEffect
        delay(if (playback.isPlaying) LISTENING_AWAKE_MILLIS else READING_AWAKE_MILLIS)
        keepAwake = false
    }

    // The recited word is drawn for the reader's eyes, so the ten-times-a-
    // second tick belongs to the reading being on the glass. A screen that has
    // gone dark, a backgrounded app, and a phone in someone else's hand keep
    // the place instead, at a ten-second checkpoint, and the processor is left
    // to sleep (owner decision, this session).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.setWordTracking(true)
                    keepAwake = true
                    touch++
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.setWordTracking(false)
                    keepAwake = false
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        viewModel.setWordTracking(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.setWordTracking(false)
        }
    }

    // The localized fallback line for a device with no receiving app, read
    // here where stringResource is valid; the share itself runs in a plain
    // lambda where a composable call would not be.
    val shareUnavailable = stringResource(R.string.share_unavailable)
    // Said when the picture cannot be made or handed out. The words still
    // go, so the reader leaves with the ayah and knows why it was words.
    val shareCardFailed = stringResource(R.string.share_card_failed)

    // A shortcut or a link that asked for the search field opens it once,
    // over the reading it landed on.
    // A shortcut asks for the search field once. The flag is saved with the
    // window so a theme or language recreation does not reopen a sheet the
    // reader has already closed.
    var searchOpened by rememberSaveable(openSearch) { mutableStateOf(false) }
    LaunchedEffect(openSearch) {
        if (openSearch && !searchOpened) {
            searchOpened = true
            openSheet(ReaderSheet.Search)
        }
    }

    /**
     * The words alone, wherever a door sends them. The card and the words
     * are two separate intents now: putting the text on the picture's own
     * intent is what made every receiver post the ayah twice.
     */
    fun sharePlainText(text: String) {
        if (!shareAyahText(context, text)) {
            // Nothing can be shared on a device with no receiving app. The
            // sheet closes so the snackbar under it is seen: a modal sheet
            // has its own window and would hide the one line that says the
            // tap did nothing. The reader can raise the sheet again.
            sharing = null
            capturing = false
            scope.launch { snackbar.showSnackbar(shareUnavailable) }
        }
    }

    /**
     * The picture could not be made or handed out: the words go instead, and
     * the reader is told why. The sheet closes before the sentence so the
     * snackbar is not behind another window, which is the rule
     * [sharePlainText] already keeps when nothing can receive a share (owner
     * decision, this version: a tap on Share image that silently posted the
     * words looked like the picture had been sent).
     */
    fun shareWordsInsteadOfPicture(text: String) {
        sharing = null
        capturing = false
        sharePlainText(text)
        scope.launch { snackbar.showSnackbar(shareCardFailed) }
    }

    /**
     * The card as a picture, alone. The PNG goes out through the app's own
     * content provider; a full cache or a provider that will not resolve
     * falls back to the plain text, so a Share is never a tap that did
     * nothing.
     */
    fun shareCard(image: ImageBitmap, text: String) {
        scope.launch {
            val file = withContext(Dispatchers.IO) { writeShareCard(context, image) }
            val opened = file != null && startShareCard(context, file)
            if (!opened) shareWordsInsteadOfPicture(text)
        }
    }

    // The reading is edge to edge, so the foot of the page clears the
    // system's navigation bar itself; [readingFoot] owns that rule.
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // A Snackbar stands above whatever the foot of the page has raised, and
    // above the system bar when nothing is raised. The height is the one the
    // stack already measured, so the two cannot overlap (owner decision,
    // this session).
    val snackbarBottom = if (bottomStackHeight > 0) {
        with(LocalDensity.current) { bottomStackHeight.toDp() } + Space.Line
    } else {
        navigationBar + Space.Block
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Every touch is the reader still being here, including a scroll
            // that lands on an ayah's own handler. The initial pass sees it
            // without taking it from the child that owns the gesture (owner
            // decision, this session).
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed && !it.previousPressed }) {
                            selectionClearer.clear()
                            touch++
                            keepAwake = true
                            reveal = null
                        }
                    }
                }
            },
    ) {
        // The bar is composed before the page it floats over, so a screen
        // reader meets the controls first when the reader brings them back:
        // the bar's nodes would otherwise stand after every ayah in the list
        // (owner decision, this session). zIndex keeps it on top of the page.
        ReaderTopBar(
            title = surahName(viewModel, settings.ayah),
            detail = stringResource(R.string.juz_label, viewModel.juz),
            visible = chrome,
            reducedMotion = reducedMotion,
            // A tap on the surah's name is the way back to its top, the way
            // a reader flips to the beginning of a surah in a printed copy.
            titleAction = stringResource(R.string.action_surah_top),
            onTitle = {
                viewModel.jumpToSurahOpening(settings.ayah)
                // The bar must not sit over the name it just went to.
                chrome = false
            },
            onBrowse = { openSheet(ReaderSheet.Browse) },
            onSearch = { openSheet(ReaderSheet.Search) },
            onSettings = { openSheet(ReaderSheet.Settings) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f),
        )

        run {
            val surah = viewModel.surahOf(settings.ayah)
            if (surah != null) {
                val revealForSurah = reveal?.takeIf {
                    viewModel.surahOf(it.ayah)?.number == surah.number
                }
                ReadingList(
                    content = content,
                    selectionClearer = selectionClearer,
                    surah = surah,
                    loadRows = {
                        viewModel.readingRows(
                            surah = surah.number,
                            wordByWord = settings.wordByWord || revealForSurah?.wordByWord == true,
                        )
                    },
                    // The copy read ahead while the reader was still in the
                    // surah before this one, and the read of the surah after
                    // it. Together they make the turn at the end of a surah
                    // land on text rather than on the paper.
                    cachedRows = { viewModel.cachedRows(it) },
                    onReadAhead = { viewModel.readAheadSurah(it) },
                    metrics = settings.readingMetrics(),
                    reveal = revealForSurah,
                    place = settings.ayah,
                    hasTranslation = viewModel.enabledTranslationPacks.isNotEmpty(),
                    contentLanguage = viewModel.readingLanguage,
                    nextSurahName = viewModel.surahs
                        .firstOrNull { it.number == surah.number + 1 }?.nameSimple,
                    selectedAyah = washedAyah,
                    menuAyah = menu?.ayah?.number,
                    savedAyahs = savedAyahs,
                    playingAyah = playback.ayahNumber,
                    playedWord = playedWord,
                    onAyahMenu = { ayah, anchor ->
                        menu = AyahMenuRequest(ayah, anchor)
                        reveal = null
                        chrome = false
                        touch++
                    },
                    onBackgroundTap = {
                        chrome = !chrome
                        menu = null
                        reveal = null
                        // A tap on the page is how a raised control is put
                        // away: the paused recitation, a failure, the offer.
                        // It never silences a playing recitation, and never
                        // aborts a package on its way (owner decision, this
                        // session).
                        putAwayRaised()
                        touch++
                    },
                    onScrolled = {
                        // The menu goes with the page: its door moves under
                        // it, and a popup hung from a moving anchor is worse
                        // than no popup (owner decision, this session).
                        menu = null
                        // A drag of the reader's own is the page moving, and
                        // the bar must not stand over the page the reader is
                        // moving. This is the reader's finger, not a jump the
                        // app started and not a screen reader's scroll action
                        // (owner decision, this session).
                        chrome = false
                        // A scroll is the reader still being here, even when
                        // the chrome is already away and no tap was needed.
                        touch++
                    },
                    onNextSurah = { number ->
                        // The next surah is an arrival at its opening; the
                        // bar has no place over it (owner decision).
                        chrome = false
                        viewModel.jumpToSurah(number)
                    },
                    // The reminder is offered after the reader has read the
                    // surah, never before the text. Turning it on is the act
                    // that asks the phone for the notification and the exact
                    // time; Not now writes the no and the card never returns
                    // (owner decision, this session).
                    reminderOffer = !settings.reminderAnswered,
                    onReminderOn = {
                        viewModel.setDailyAyah(
                            enabled = true,
                            onPermission = onReminderPermission,
                        )
                        viewModel.markReminderAnswered()
                    },
                    onReminderNotNow = {
                        viewModel.setDailyAyah(
                            enabled = false,
                            onPermission = onReminderPermission,
                        )
                        viewModel.markReminderAnswered()
                    },
                    onAddContent = { openSheet(ReaderSheet.Settings) },
                    onPlaceChanged = { ayah -> viewModel.onReadingSettled(ayah) },
                    onJumped = { ayah ->
                        announceView.announceForAccessibility(
                            resources.getString(
                                io.github.muntasimulhaque.quran.feature.reading.R.string.reading_ayah_reference,
                                io.github.muntasimulhaque.quran.core.localizedDigits(
                                    ayah.verseKey,
                                    locale,
                                ),
                            ),
                        )
                    },
                    startAtOpening = viewModel.startAtSurahOpening == surah.number,
                    onOpeningReached = { viewModel.consumeSurahOpening(surah.number) },
                    // The air above a surah's opening, and the whole of the
                    // head's room: the page is drawn edge to edge and keeps no
                    // inset for the system bar, so a page scrolled into the
                    // middle of a surah puts its first line under the clock.
                    // That is the page, not an oversight, and it stays: the
                    // owner weighed an inset against the full-height page and
                    // kept the page (owner decision, this session).
                    contentPaddingTop = 64.dp,
                    contentPaddingBottom = readingFoot(
                        // Nothing is raised until the reader asks for a
                        // recitation or a pack, so the page's own foot is the
                        // page's (owner decision, this session).
                        bottomStack = with(LocalDensity.current) {
                            bottomStackHeight.toDp()
                        },
                        navigationBar = navigationBar,
                    ),
                )
            }
        }

        // A quiet paper scrim under the clock, so a line scrolled to the top
        // never sits under the status bar's icons. It is the status bar's own
        // height plus a breath, not a second bar: the page keeps its full
        // height and the Quran keeps its first line (owner decision, this
        // session).
        if (!chrome) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(statusBar + Space.Line)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.background,
                            0.72f to MaterialTheme.colorScheme.background,
                            1f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                        ),
                    ),
            )
        }

        // The long press and its one-time note are gone with the gesture: the
        // ayah's own number is its door, visible on every ayah, and a
        // signifier that has to be taught is not a signifier (owner decision,
        // this session).

        val offer = viewModel.listenOffer
        val showPlayback = playback.isAnything() || offer != null
        // The foot belongs to the player and the pack alone. The stack is
        // composed only while one of them is raised, so an idle page pays for
        // no bar at all (owner decision, this session).
        BottomStack(
            viewModel = viewModel,
            playback = playback,
            showPlayback = showPlayback,
            onHeight = { bottomStackHeight = it },
            onReciter = { viewModel.choosePillReciter(it) },
            reciterId = settings.recitation,
            reciterName = reciterShortName(
                settings.recitation,
                viewModel.recitations.firstOrNull { it.id == settings.recitation }?.name.orEmpty(),
            ),
            reciterOptions = viewModel.pillReciters,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f),
        )

        AppSnackbar(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
                .padding(horizontal = Space.Gutter)
                .padding(bottom = snackbarBottom),
        )

        // The ayah's own menu, hung from its door. Every answer closes it and
        // then does its work, so the reader is returned to the reading rather
        // than to a panel they have to put away (owner decision, this
        // session). The popup is the app's one popup, and it is the same one
        // the playback pill opens.
        menu?.let { request ->
            val savedRow = saved.firstOrNull { it.ayahNumber == request.ayah.number }
            AyahMenu(
                reference = localizedDigits(request.ayah.verseKey),
                anchor = request.anchor,
                isSaved = savedRow?.marked == true,
                hasNote = !savedRow?.note.isNullOrBlank(),
                // The Tafsir door is always on the menu. A reader who has no
                // pack gets the offer card, not a missing action (owner
                // decision, this session).
                showTafsir = true,
                onDismiss = { menu = null },
                onPlay = {
                    viewModel.playAyah(request.ayah.number)
                },
                onTafsir = {
                    cardTafsirPack = null
                    cardAyah = request.ayah
                },
                onSave = {
                    // A save of the reader's own, carrying a note, asks before
                    // it goes, so the reader's own words are never removed by
                    // one tap. An ayah that only a note kept has no save to
                    // remove, and one tap of Save makes it.
                    if (savedRow?.marked == true && !savedRow.note.isNullOrBlank()) {
                        pendingRemove = request.ayah.number
                        pendingRemoveUndoable = true
                    } else {
                        viewModel.toggleSaved(request.ayah)
                    }
                    touch++
                },
                onNote = {
                    noteSaving = false
                    noteFailed = false
                    cardNote = request.ayah.number
                },
                onShare = {
                    capturing = false
                    scope.launch { sharing = loadShareCard(context, viewModel, request.ayah) }
                },
            )
        }

        // The share card, composed for its capture alone. It never reaches
        // the screen: the layer it records into is read back and then the
        // whole node leaves the composition. This capture runs only while
        // `capturing` is set, so the words-only door never pays for a bitmap,
        // and the share sheet behind it stays composed for the next receiver.
        sharing?.takeIf { capturing }?.let { card ->
            ShareCardCapture(
                card = card,
                width = captureWidth,
                onImage = { image ->
                    capturing = false
                    shareCard(image, card.text)
                },
                onFailed = { shareWordsInsteadOfPicture(card.text) },
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }

    // The share sheet: the real card as a preview, and the two doors. It stays
    // up after either door: the reader may want the same card in another app,
    // and the sheet is the one place that says what the card is. It closes on
    // a dismiss, or on a share that found no receiver (so the snackbar is not
    // hidden behind the sheet's own window).
    sharing?.let { card ->
        AyahShareSheet(
            card = card,
            onShareImage = { width ->
                captureWidth = width
                if (!capturing) capturing = true
            },
            onShareText = { if (!capturing) sharePlainText(card.text) },
            onDismiss = {
                sharing = null
                capturing = false
            },
        )
    }

    if (sheet != ReaderSheet.None) {
        sheetStates.SaveableStateProvider(sheet.name) {
            ReaderSheets(
                sheet = sheet,
                content = content,
                viewModel = viewModel,
                settings = settings,
                saved = saved,
                lastRead = lastRead,
                notificationsBlocked = notificationsBlocked,
                exactGrantWithheld = exactGrantWithheld,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onAskExactAlarm = onAskExactAlarm,
                onReminderPermission = onReminderPermission,
                onDismiss = { closeSheet() },
                onResult = { resultOpened(it) },
                onSearchResult = { openSearchResult(it) },
                onPendingRemove = { ayah ->
                    pendingRemove = ayah
                    // A row removed from Browse asks and is undone over
                    // Browse; the reading's Snackbar is behind that sheet.
                    pendingRemoveUndoable = sheet != ReaderSheet.Browse
                },
                onRestoreSaved = { row -> viewModel.restoreSaved(row) },
            )
        }
    }

    cardAyah?.let { ayah ->
        AyahCard(
            content = content,
            ayah = ayah,
            surahName = viewModel.surahs.firstOrNull { it.number == ayah.surah }?.nameSimple
                ?: stringResource(R.string.surah_fallback_name, ayah.surah),
            tafsirPacks = viewModel.enabledTafsirPacks,
            settings = settings,
            tafsirOffer = viewModel.packs.firstOrNull {
                it.type == PackType.Tafsir &&
                    it.language == viewModel.readingLanguage &&
                    !it.installed
            },
            onInstallTafsir = { viewModel.installPack(it) },
            initialTafsirPack = cardTafsirPack,
            onAddContent = {
                cardAyah = null
                cardTafsirPack = null
                openSheet(ReaderSheet.Settings)
            },
            onDismiss = {
                cardAyah = null
                cardTafsirPack = null
            },
        )
    }

    cardNote?.let { ayahNumber ->
        val savedRow = saved.firstOrNull { it.ayahNumber == ayahNumber }
        AyahNoteSheet(
            note = noteDrafts[ayahNumber] ?: savedRow?.note,
            settings = settings,
            saving = noteSaving,
            failed = noteFailed,
            onSave = { note ->
                if (!noteSaving) {
                    noteSaving = true
                    noteFailed = false
                    viewModel.setNote(ayahNumber, note) { written ->
                        noteSaving = false
                        if (written) {
                            noteDrafts = noteDrafts - ayahNumber
                            cardNote = null
                        } else {
                            noteFailed = true
                        }
                    }
                }
            },
            onDraftChange = { text ->
                // An empty draft is not kept: it would hide the saved note
                // the next time the sheet opens. Clearing a note is the
                // Clear action, which writes the clear.
                noteDrafts = if (text.isBlank()) {
                    noteDrafts - ayahNumber
                } else {
                    noteDrafts + (ayahNumber to text)
                }
            },
            onDismiss = {
                noteSaving = false
                noteFailed = false
                cardNote = null
            },
        )
    }

    pendingRemove?.let { ayahNumber ->
        RemoveSavedSheet(
            note = saved.firstOrNull { it.ayahNumber == ayahNumber }?.note,
            noteSp = settings.translationSp,
            noteLineSp = settings.translationLineSp,
            onConfirm = {
                viewModel.removeSaved(ayahNumber, undoable = pendingRemoveUndoable)
                pendingRemove = null
            },
            onDismiss = { pendingRemove = null },
        )
    }
}

@Composable
private fun surahName(viewModel: ReaderViewModel, ayah: Int): String =
    viewModel.surahOf(ayah)?.nameSimple ?: stringResource(R.string.reader_fallback_title)

/**
 * The room the reading keeps under its own last line, before the system's
 * navigation bar is cleared.
 */
private val PageFoot = 32.dp

/**
 * The reading's foot: the padding the page takes below its last line.
 *
 * The reading is drawn edge to edge, so the bottom of the window is the
 * glass and not the page, and the system's navigation bar is painted over
 * whatever the list draws there. The foot was 32 dp alone, which a three
 * button bar (about 50 dp) swallowed whole: the door to the next surah
 * ended 3 dp above the glass while the design asked for 52, and the last
 * ayah of any surah could be read half hidden under the bar (owner report,
 * this session).
 *
 * The bar is cleared first and the page's own room is added after it, so
 * the air the reader sees is the same on a phone with three buttons, a
 * phone with a gesture bar, and a tablet with a taskbar. A control raised at
 * the foot already measures its height with the inset inside it (BottomStack
 * pads itself with the navigation bar) and covers that ground itself, so it
 * is taken as it stands and the bar is never paid for twice.
 */
internal fun readingFoot(bottomStack: Dp, navigationBar: Dp): Dp =
    maxOf(bottomStack, PageFoot + navigationBar)

/**
 * The ayah whose own menu is open, and the door it hangs from.
 *
 * The door is a place on the glass and not a setting, so the pair lives in a
 * plain `remember`: a rotation closes the menu, which is right, because the
 * door has moved and the reader's attention has too.
 */
private data class AyahMenuRequest(val ayah: Ayah, val anchor: IntRect)
