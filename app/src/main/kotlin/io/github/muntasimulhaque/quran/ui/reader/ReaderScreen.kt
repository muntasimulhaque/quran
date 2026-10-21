package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.readingMetrics
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import io.github.muntasimulhaque.quran.ui.kit.rememberReducedMotion
import io.github.muntasimulhaque.quran.ui.kit.reciterShortName
import io.github.muntasimulhaque.quran.ui.reading.AyahCard
import io.github.muntasimulhaque.quran.ui.reading.AyahNoteSheet
import io.github.muntasimulhaque.quran.ui.reading.ReadingList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How long the chrome stays after a touch before it steps back. */
private const val CHROME_MILLIS = 7000L

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
 * The reading screen: one reading, and chrome that only appears
 * when it is asked for. Everything else in the app happens in a sheet over
 * this surface or in the ayah card, so the page itself is never crowded.
 */
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    content: ContentDatabase,
    onPlaybackPermission: () -> Unit,
    onReminderPermission: () -> Unit,
    notificationsBlocked: () -> Boolean,
    exactGrantWithheld: () -> Boolean,
    onOpenNotificationSettings: () -> Unit,
    onAskExactAlarm: () -> Unit,
    /** A shortcut or a deep link that asks for the search field once. */
    openSearch: Boolean = false,
) {
    val settings = viewModel.settings
    val reducedMotion by rememberReducedMotion()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val lastRead by viewModel.lastRead.collectAsStateWithLifecycle()

    var chrome by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Ayah?>(null) }
    var cardAyah by remember { mutableStateOf<Ayah?>(null) }
    // The note sheet is keyed by the ayah's number, not by the ayah itself:
    // the pill opens it, and the number is all it needs to write the note and
    // keep the ayah with it.
    var cardNote by remember { mutableStateOf<Int?>(null) }
    // The saved ayah the reader asked to remove while a note is still on it:
    // the confirmation is up until they answer, and the ayah stays saved
    // until they do. Null when nothing is being asked about.
    var pendingRemove by remember { mutableStateOf<Int?>(null) }
    // The open sheet survives the window, not only the composition: a change
    // of language recreates the Activity, and a sheet kept in plain `remember`
    // closed with the old window while the reader was still in it. The reader
    // chose a language from the Language page and was returned to the reading;
    // the sheet is written into the saved state, so the choice is answered
    // where it was made (owner decision, 21).
    var sheet by rememberSaveable { mutableStateOf(ReaderSheet.None) }
    var touch by remember { mutableIntStateOf(0) }
    // The screen is kept awake while the reader is here, but not forever. The
    // window timer releases the flag; a touch takes it back.
    var keepAwake by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    // What a Share is doing: null when nothing, a card waiting for the
    // reader's choice in the share sheet, and the same card with `capture`
    // set once they asked for the picture. One holder rather than two, so a
    // second tap can never leave a stray sheet behind the capture.
    var sharing by remember { mutableStateOf<ShareRequest?>(null) }

    // A sheet, an ayah's actions, a note, or a share is the reader using the
    // app, not listening to it from across the room. Those keep the screen
    // awake; the plain reading and the plain listening are the two that give
    // it back after their own window (owner decision, this session).
    val usingControls = sheet != ReaderSheet.None || selected != null ||
        cardAyah != null || cardNote != null || pendingRemove != null || sharing != null

    // The wash under an ayah: the one the tap chose, or the one whose
    // note is open over it. A note opened from Browse has no pill behind it,
    // but the reader is still looking at the ayah the note belongs to, so it
    // wears the same mark while the sheet is up.
    val washedAyah = selected?.number ?: cardNote

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
    // raised. A reader who tapped an ayah means the pill, and back is the
    // gesture they already use to put a thing away. The sheets (the ayah card,
    // the note, Browse, Search, Settings) are their own windows and take back
    // through their own handlers; this one is for the reading's own foot.
    //
    // Back answers the page's own rule and nothing more: it never silences a
    // recitation that is playing, and it never aborts a package on its way.
    androidx.activity.compose.BackHandler(enabled = selected != null) {
        selected = null
        touch++
    }
    androidx.activity.compose.BackHandler(
        enabled = selected == null && raised != RaisedAnswer.Nothing,
    ) {
        putAwayRaised()
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

    // The chrome steps back on its own; a chosen ayah waits for the reader.
    LaunchedEffect(chrome, touch) {
        if (!chrome) return@LaunchedEffect
        delay(CHROME_MILLIS)
        chrome = false
    }

    // The localized fallback line for a device with no receiving app, read
    // here where stringResource is valid; the share itself runs in a plain
    // lambda where a composable call would not be.
    val shareUnavailable = stringResource(R.string.share_unavailable)

    // A shortcut or a link that asked for the search field opens it once,
    // over the reading it landed on.
    LaunchedEffect(openSearch) {
        if (openSearch) sheet = ReaderSheet.Search
    }

    /**
     * The words alone, wherever a door sends them. The card and the words
     * are two separate intents now: putting the text on the picture's own
     * intent is what made every receiver post the ayah twice.
     */
    fun sharePlainText(text: String) {
        if (!shareAyahText(context, text)) {
            // Nothing can be shared on a device with no receiving app; the
            // reader is told rather than left with a tap that did nothing.
            android.widget.Toast.makeText(
                context,
                shareUnavailable,
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
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
            if (!opened) sharePlainText(text)
        }
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
                            touch++
                            keepAwake = true
                        }
                    }
                }
            },
    ) {
        run {
            val surah = viewModel.surahOf(settings.ayah)
            if (surah != null) {
                ReadingList(
                    content = content,
                    surah = surah,
                    loadRows = {
                        viewModel.readingRows(
                            surah = surah.number,
                            wordByWord = settings.wordByWord,
                        )
                    },
                    // The copy read ahead while the reader was still in the
                    // surah before this one, and the read of the surah after
                    // it. Together they make the turn at the end of a surah
                    // land on text rather than on the paper.
                    cachedRows = { viewModel.cachedRows(it) },
                    onReadAhead = { viewModel.readAheadSurah(it) },
                    metrics = settings.readingMetrics(),
                    place = settings.ayah,
                    placeOffset = settings.ayahOffset,
                    hasTranslation = viewModel.enabledTranslationPacks.isNotEmpty(),
                    contentLanguage = viewModel.readingLanguage,
                    nextSurahName = viewModel.surahs
                        .firstOrNull { it.number == surah.number + 1 }?.nameSimple,
                    selectedAyah = washedAyah,
                    playingAyah = playback.ayahNumber,
                    playingWord = playback.wordPosition,
                    onAyah = { ayah ->
                        selected = ayah
                        chrome = false
                        touch++
                    },
                    onBackgroundTap = {
                        chrome = !chrome
                        selected = null
                        // A tap on the page is how a raised control is put
                        // away: the paused recitation, a failure, the offer.
                        // It never silences a playing recitation, and never
                        // aborts a package on its way (owner decision, this
                        // session).
                        putAwayRaised()
                        touch++
                    },
                    onScrolled = {
                        if (chrome) chrome = false
                        // A scroll is the reader still being here, even when
                        // the chrome is already away and no tap was needed.
                        touch++
                    },
                    onNextSurah = { number -> viewModel.jumpToSurah(number) },
                    onAddContent = { sheet = ReaderSheet.Settings },
                    onPlaceChanged = { ayah, offset -> viewModel.onReadingSettled(ayah, offset) },
                    startAtOpening = viewModel.startAtSurahOpening == surah.number,
                    onOpeningReached = { viewModel.consumeSurahOpening(surah.number) },
                    contentPaddingTop = 64.dp,
                    contentPaddingBottom = 120.dp,
                )
            }
        }

        ReaderTopBar(
            title = surahName(viewModel, settings.ayah),
            detail = stringResource(R.string.juz_label, viewModel.juz),
            visible = chrome,
            reducedMotion = reducedMotion,
            // A tap on the surah's name is the way back to its top, the way
            // a reader flips to the beginning of a surah in a printed copy.
            titleAction = stringResource(R.string.action_surah_top),
            onTitle = { viewModel.jumpToSurahOpening(settings.ayah) },
            onBrowse = { sheet = ReaderSheet.Browse },
            onSearch = { sheet = ReaderSheet.Search },
            onSettings = { sheet = ReaderSheet.Settings },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // The long press is the richest gesture in the app and the least
        // visible, so it is said once, quietly, and then never again.
        if (!settings.longPressHintShown) {
            LongPressHint(
                reducedMotion = reducedMotion,
                onSeen = { viewModel.markLongPressHintShown() },
            )
        }

        val offer = viewModel.listenOffer
        val showPlayback = playback.isAnything() || offer != null
        val bottomItems = selected != null || showPlayback
        AnimatedVisibility(
            visible = bottomItems,
            // A reader who asked the phone to reduce motion gets the appearance
            // and the disappearance without the slide; everyone else keeps the
            // lift that says the control comes from the foot of the page.
            enter = if (reducedMotion) fadeIn() else fadeIn() + slideInVertically { it / 3 },
            exit = if (reducedMotion) fadeOut() else fadeOut() + slideOutVertically { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomStack(
                viewModel = viewModel,
                playback = playback,
                showPlayback = showPlayback,
                saved = saved,
                selected = selected,
                onPlay = { ayah ->
                    onPlaybackPermission()
                    viewModel.playAyah(ayah.number)
                },
                onDeselect = {
                    selected = null
                    touch++
                },
                onMore = { ayah ->
                    cardAyah = ayah
                    selected = null
                },
                onNote = { ayah -> cardNote = ayah.number },
                onRemoveSaved = { ayah -> pendingRemove = ayah.number },
                onShare = { ayah ->
                    scope.launch { sharing = loadShareCard(context, viewModel, ayah) }
                },
                onTouch = { touch++ },
                onReciter = { viewModel.choosePillReciter(it) },
                reciterId = settings.recitation,
                reciterName = reciterShortName(
                    settings.recitation,
                    viewModel.recitations.firstOrNull { it.id == settings.recitation }?.name.orEmpty(),
                ),
                reciterOptions = viewModel.pillReciters,
            )
        }

        // The share card, composed for its capture alone. It never reaches
        // the screen: the layer it records into is read back and then the
        // whole node leaves the composition. This capture runs only after
        // the reader has chosen Share image, so the words-only door never
        // pays for a bitmap.
        sharing?.takeIf { it.capture }?.let { card ->
            ShareCardCapture(
                card = card.card,
                onImage = { image ->
                    sharing = null
                    shareCard(image, card.card.text)
                },
                onFailed = {
                    sharing = null
                    sharePlainText(card.card.text)
                },
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }

    // The share sheet: the real card as a preview, and the two doors.
    sharing?.takeIf { !it.capture }?.let { ready ->
        AyahShareSheet(
            card = ready.card,
            onShareImage = { sharing = ready.copy(capture = true) },
            onShareText = {
                sharing = null
                sharePlainText(ready.card.text)
            },
            onDismiss = { sharing = null },
        )
    }

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
        onDismiss = { sheet = ReaderSheet.None },
        onPendingRemove = { pendingRemove = it },
    )

    cardAyah?.let { ayah ->
        AyahCard(
            content = content,
            ayah = ayah,
            surahName = viewModel.surahs.firstOrNull { it.number == ayah.surah }?.nameSimple
                ?: stringResource(R.string.surah_fallback_name, ayah.surah),
            tafsirPacks = viewModel.enabledTafsirPacks,
            settings = settings,
            onAddContent = {
                cardAyah = null
                sheet = ReaderSheet.Settings
            },
            onDismiss = { cardAyah = null },
        )
    }

    cardNote?.let { ayahNumber ->
        val savedRow = saved.firstOrNull { it.ayahNumber == ayahNumber }
        AyahNoteSheet(
            note = savedRow?.note,
            settings = settings,
            onSave = { note ->
                viewModel.setNote(ayahNumber, note)
                cardNote = null
            },
            onDismiss = { cardNote = null },
        )
    }

    pendingRemove?.let { ayahNumber ->
        RemoveSavedSheet(
            note = saved.firstOrNull { it.ayahNumber == ayahNumber }?.note,
            onConfirm = {
                viewModel.removeSaved(ayahNumber)
                pendingRemove = null
            },
            onDismiss = { pendingRemove = null },
        )
    }
}

@Composable
private fun surahName(viewModel: ReaderViewModel, ayah: Int): String =
    viewModel.surahOf(ayah)?.nameSimple ?: stringResource(R.string.reader_fallback_title)
