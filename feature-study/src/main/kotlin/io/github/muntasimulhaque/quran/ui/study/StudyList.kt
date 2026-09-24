package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.AyahList
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.StudyRow
import io.github.muntasimulhaque.quran.feature.study.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.rememberReducedMotion
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Reading
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * The study reading of one surah: its opening, then a continuous scroll
 * through every ayah to the end of the surah, and a quiet closing line that
 * offers the next surah.
 *
 * A surah is the unit the Quran itself gives, and readers read it as one:
 * this view ends where the surah ends, instead of sliding into the next one
 * without a word. Tapping the paper brings the chrome; a long press asks
 * about the ayah under the finger.
 */
@Composable
fun StudyList(
    content: ContentDatabase,
    surah: Surah,
    loadRows: suspend () -> List<StudyRow>,
    settings: AppSettings,
    hasTranslation: Boolean,
    nextSurahName: String?,
    selectedAyah: Int?,
    playingAyah: Int?,
    playingWord: Int?,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    onScrolled: () -> Unit,
    onNextSurah: (Int) -> Unit,
    onAddContent: () -> Unit,
    onPlaceChanged: (Int) -> Unit,
    /** True when the reader opened this surah at its top; answered once. */
    startAtOpening: Boolean,
    onOpeningReached: () -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pageDescription = stringResource(R.string.study_page_description)
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
    // The whole surah arrives at once, so every item has its text before the
    // list is measured: the reader's place lands exactly, and nothing grows or
    // shifts under them while they read. Until the rows for *this* surah are
    // in hand the paper is shown, never the surah the reader just left: a
    // stale list would be measured with the new surah's name on it, and a
    // scroll through it would write a place in the wrong surah.
    val loaded by produceState<Pair<Int, List<StudyRow>>?>(
        initialValue = null,
        surah.number,
        settings.translationPacks,
        settings.wordByWord,
    ) { value = surah.number to loadRows() }
    val rows = loaded?.takeIf { it.first == surah.number }?.second.orEmpty()
    val ayahs = remember(rows) { rows.map { it.ayah.number } }
    var footnote by remember { mutableStateOf<OpenFootnote?>(null) }
    if (ayahs.isEmpty()) {
        // The paper is already there; the text is a frame away.
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    StudyRows(
        content = content,
        surah = surah,
        rows = rows,
        ayahs = ayahs,
        settings = settings,
        hasTranslation = hasTranslation,
        nextSurahName = nextSurahName,
        selectedAyah = selectedAyah,
        playingAyah = playingAyah,
        playingWord = playingWord,
        hafs = hafs,
        pageDescription = pageDescription,
        onAyah = onAyah,
        onBackgroundTap = onBackgroundTap,
        onScrolled = onScrolled,
        onNextSurah = onNextSurah,
        onAddContent = onAddContent,
        onPlaceChanged = onPlaceChanged,
        startAtOpening = startAtOpening,
        onOpeningReached = onOpeningReached,
        contentPaddingTop = contentPaddingTop,
        contentPaddingBottom = contentPaddingBottom,
        footnote = footnote,
        onFootnote = { footnote = it },
        modifier = modifier,
    )
}

/** The surah's rows, drawn and driven once the text is on hand. */
@Composable
private fun StudyRows(
    content: ContentDatabase,
    surah: Surah,
    rows: List<StudyRow>,
    ayahs: List<Int>,
    settings: AppSettings,
    hasTranslation: Boolean,
    nextSurahName: String?,
    selectedAyah: Int?,
    playingAyah: Int?,
    playingWord: Int?,
    hafs: FontFamily,
    pageDescription: String,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    onScrolled: () -> Unit,
    onNextSurah: (Int) -> Unit,
    onAddContent: () -> Unit,
    onPlaceChanged: (Int) -> Unit,
    startAtOpening: Boolean,
    onOpeningReached: () -> Unit,
    contentPaddingTop: Dp,
    contentPaddingBottom: Dp,
    footnote: OpenFootnote?,
    onFootnote: (OpenFootnote?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = AyahList.indexOf(ayahs, settings.ayah),
    )

    // The surah's introduction opens from its quiet door; once it is open, a
    // tap anywhere on the paper, the ayahs, or the closing line puts it away
    // again, exactly as the Hide action does, so the reader is never left
    // looking for the way out. The state lives here rather than inside the
    // opening item because the taps that dismiss it land outside that item.
    var aboutOpen by rememberSaveable(surah.number) { mutableStateOf(false) }
    val dismissAbout: () -> Unit = {
        if (aboutOpen) aboutOpen = false else onBackgroundTap()
    }

    // Moving the page means reading, and reading wants the page: the chrome
    // is told to step aside as soon as the list is moving under a finger.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling -> if (scrolling) onScrolled() }
    }

    // The reader's place is written down when a drag of their own ends, and
    // never by a scroll the app started. The flag is cleared on the write, so
    // the jump that follows a drag cannot write a place of its own.
    var dragged by remember(surah.number) { mutableStateOf(false) }
    LaunchedEffect(listState, surah.number) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) dragged = true
        }
    }
    LaunchedEffect(listState, surah.number) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collect { (scrolling, index) ->
                if (scrolling || !dragged) return@collect
                dragged = false
                AyahList.ayahAt(ayahs, index)?.let(onPlaceChanged)
            }
    }

    // The reader's place moves the list, exactly, and the list never moves
    // itself. The place is an ayah, so a round trip through the list has to
    // land on the same item; anything less leaves the place behind.
    LaunchedEffect(settings.ayah, surah.number, ayahs) {
        val target = AyahList.indexOf(ayahs, settings.ayah)
        if (target != listState.firstVisibleItemIndex) listState.scrollToItem(target)
    }

    // Opening a surah from Browse lands on its top: the opening item, which
    // is not an ayah and so has no place of its own. The request is its own
    // effect rather than a branch of the place above, because the place must
    // not re-run when the request is cleared: that is what would pull the
    // reader back down to the first ayah the moment the opening appeared.
    LaunchedEffect(listState, ayahs, startAtOpening) {
        if (!startAtOpening || ayahs.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(0)
        onOpeningReached()
    }

    // The playing ayah comes back into view when the reciter moves on. A
    // reader who asked the system to reduce motion is taken there at once.
    val reducedMotion by rememberReducedMotion()
    LaunchedEffect(playingAyah, ayahs, reducedMotion) {
        val ayahNumber = playingAyah ?: return@LaunchedEffect
        if (!settings.followReciter) return@LaunchedEffect
        val target = AyahList.indexOf(ayahs, ayahNumber)
        val current = listState.firstVisibleItemIndex
        if (target < current || target > current + 3) {
            if (reducedMotion) listState.scrollToItem(target)
            else listState.animateScrollToItem(target)
        }
    }

    // The reading sits in a column of a readable width, centered when the
    // screen is wider than the measure. A translation laid across the whole
    // of a ten inch tablet is over two hundred characters a line, and the eye
    // stops returning to the margin on its own; a book never does that. The
    // list itself stays full width, so the paper around the column still
    // answers a tap with the chrome and a drag with the scroll.
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val side = maxOf(20.dp, (screenWidth - Reading.MaxMeasure) / 2)
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = pageDescription }
            .pointerInput(Unit) {
                // The paper around the text brings the chrome; the text itself
                // belongs to its ayah, and every ayah consumes its own taps.
                detectTapGestures(onTap = { dismissAbout() })
            },
        contentPadding = PaddingValues(
            start = side,
            end = side,
            top = contentPaddingTop,
            bottom = contentPaddingBottom,
        ),
    ) {
        item(key = "surah-${surah.number}") {
            SurahOpening(
                surah = surah,
                content = content,
                expanded = aboutOpen,
                onExpandedChange = { aboutOpen = it },
                onPaperTap = { if (!aboutOpen) onBackgroundTap() },
            )
            if (!hasTranslation) {
                AddContent(
                    text = stringResource(R.string.study_add_translation),
                    onClick = onAddContent,
                )
            }
        }
        items(rows.size, key = { "ayah-${rows[it].ayah.number}" }) { index ->
            val row = rows[index]
            AyahBlock(
                row = row,
                hafs = hafs,
                settings = settings,
                wordByWord = settings.wordByWord,
                isSelected = selectedAyah == row.ayah.number,
                playingAyah = playingAyah,
                playingWord = playingWord,
                onAyah = onAyah,
                onBackgroundTap = dismissAbout,
                onFootnote = { number ->
                    val note = row.translations.asSequence()
                        .flatMap { it.text.footnotes.asSequence() }
                        .firstOrNull { it.number == number }
                    if (note != null) {
                        onFootnote(
                            OpenFootnote(
                                note = note,
                                reference = "${row.ayah.surah}:${row.ayah.ayah}",
                                sizeSp = settings.translationSp,
                                lineSp = settings.translationLineSp,
                            ),
                        )
                    }
                },
            )
        }
        item(key = "end-${surah.number}") {
            SurahEnd(
                surah = surah,
                nextSurahName = nextSurahName,
                onNextSurah = onNextSurah,
                onBackgroundTap = dismissAbout,
            )
        }
    }

    footnote?.let { open ->
        FootnoteSheet(
            footnote = open.note,
            surahName = surah.nameSimple,
            reference = open.reference,
            sizeSp = open.sizeSp,
            lineSp = open.lineSp,
            onDismiss = { onFootnote(null) },
        )
    }
}

/** A quiet door to the packs the reader does not have yet. */
@Composable
private fun AddContent(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.study_action_add),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SurahOpening(
    surah: Surah,
    content: ContentDatabase?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPaperTap: () -> Unit,
) {
    val info by produceState<String?>(initialValue = null, surah.number, expanded) {
        value = if (expanded) {
            withContext(Dispatchers.IO) { content?.surahInfo(surah.number) }
        } else {
            null
        }
    }
    val ornament = LocalPagePalette.current.ornament
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The opening of a surah is the one arrival in the reading, so it is
        // built to the same model as the printed page: the Arabic name in the
        // ornament gold, a short gold rule under it, and the name and place in
        // quiet type. The Mushaf page draws exactly this on its surah line, so
        // the two readings open a surah the same way.
        Text(
            text = surah.nameArabic,
            style = TextStyle(
                fontFamily = Amiri,
                fontSize = 40.sp,
                color = ornament,
            ),
        )
        // The rule is ornament, never a separator: it sits under the Arabic
        // name the way the printed page rules its surah line, and it carries
        // no meaning a reader would lose without it. It is a hairline of the
        // same gold, held well inside the measure.
        Box(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth(0.3f)
                .height(1.dp)
                .background(ornament.copy(alpha = 0.55f)),
        )
        Text(
            text = surah.nameSimple,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = Space.Section),
        )
        Text(
            text = stringResource(
                R.string.study_surah_meta,
                if (surah.revelationPlace.equals("makkah", true)) {
                    stringResource(R.string.study_place_makkah)
                } else {
                    stringResource(R.string.study_place_madinah)
                },
                surah.versesCount,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (surah.number != 1 && surah.bismillahPre) {
            Text(
                text = BASMALLAH,
                style = TextStyle(
                    fontFamily = Amiri,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Section),
            )
        }
        // The door itself is a button like every other action in the app,
        // so a word that answers a tap never reads as plain type.
        TextButton(
            label = stringResource(
                if (expanded) R.string.study_hide else R.string.study_about_surah,
            ),
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.padding(top = Space.Section),
        )
        if (info != null) {
            // Opened, the introduction wears the wash a chosen ayah wears:
            // the reader asked for it, and while it is up it reads as the
            // chosen thing on the page, in the same lapis at the same weight
            // the long press leaves behind. A press on it answers in the same
            // lapis rather than the gray an unstyled ripple would flash.
            val wash = if (expanded) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
            } else {
                Color.Transparent
            }
            val press = remember { MutableInteractionSource() }
            Text(
                text = RichText.paragraphs(info.orEmpty()),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                // Opened, the whole introduction is the reader's; it was
                // asked for with a press, and a cap on it would cut the
                // paragraphs the app only shows on request. Closed, the first
                // lines are a preview, and the ellipsis says so.
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                // The door is the paragraph itself: the reading around it is
                // the paper, and only the part that opens is its own target,
                // so a press shows the boundary of the about and not of the
                // whole opening. The boundary is the ayah's own rounded one,
                // and the ayah's own room sits inside it: the shape wraps the
                // padding, not the letters, so the about answers in the same
                // shape and color as an ayah and no line of text is left
                // touching the edge it is standing on.
                modifier = Modifier
                    .padding(top = Space.Block)
                    .clip(RoundedCornerShape(14.dp))
                    .background(wash)
                    .clickable(
                        interactionSource = press,
                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                        onClick = onPaperTap,
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
        // The door itself is a button like every other action in the app,
        // so a word that answers a tap never reads as plain type.
        TextButton(
            label = stringResource(
                if (expanded) R.string.study_hide else R.string.study_about_surah,
            ),
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** The end of a surah, and the door to the next one. */
@Composable
private fun SurahEnd(
    surah: Surah,
    nextSurahName: String?,
    onNextSurah: (Int) -> Unit,
    onBackgroundTap: () -> Unit,
) {
    val next = surah.number + 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 30.dp, bottom = 20.dp)
            .clickable(onClick = onBackgroundTap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.22f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        )
        Text(
            text = stringResource(R.string.study_surah_complete, surah.nameSimple),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (next <= 114 && nextSurahName != null) {
            // The next surah is a card, not a bare line of type: the label
            // says what the tap does, the name says where it lands, and the
            // arrow says which way the reading goes, the way the end of a
            // page names the page after it.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Block)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .clickable { onNextSurah(next) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.study_next_surah),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = nextSurahName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                IconGlyph(
                    icon = Icon.Chevron,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(-90f),
                )
            }
        } else if (next > 114) {
            Text(
                text = stringResource(R.string.study_end_of_quran),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AyahBlock(
    row: StudyRow,
    hafs: FontFamily,
    settings: AppSettings,
    wordByWord: Boolean,
    isSelected: Boolean,
    playingAyah: Int?,
    playingWord: Int?,
    onAyah: (Ayah) -> Unit,
    onBackgroundTap: () -> Unit,
    onFootnote: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val palette = LocalPagePalette.current
    val playing = row.ayah.number == playingAyah
    val ayahActions = stringResource(R.string.study_ayah_actions)
    val wash = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        playing -> palette.highlight.copy(alpha = palette.highlight.alpha * 0.55f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    // The padding belongs to the paper, so the gaps between ayahs still
    // answer a tap with the reading chrome. The block is set apart by the
    // room around it, not by a rule: the app draws no separators.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(wash)
                .combinedClickable(
                    onClick = onBackgroundTap,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAyah(row.ayah)
                    },
                )
                // The Mushaf gives every ayah a node whose action raises the
                // pill; the study block answered a real long press only, so a
                // screen reader could read an ayah here but not act on it. The
                // same action is now exposed the same way in both readings,
                // which is the one gesture the reading is built around
                // (D-084 named this gap; this closes it).
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(ayahActions) {
                            onAyah(row.ayah)
                            true
                        },
                    )
                }
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            val arabicLine = arabic(row, playing, playingWord)
            var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                text = arabicLine.text,
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = settings.arabicSp.sp,
                    lineHeight = settings.arabicLineSp.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                textAlign = TextAlign.Right,
                onTextLayout = { textLayout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        val range = arabicLine.washRange
                        val layout = textLayout
                        if (layout != null && range != null) {
                            drawPlayingWordWash(layout, range, palette.highlight)
                        }
                        drawContent()
                    },
            )
            if (wordByWord && row.meanings.any { it.meaning != null }) {
                WordByWord(
                    meanings = row.meanings,
                    hafs = hafs,
                    settings = settings,
                    // The aid is an annotation of the ayah above it, not a
                    // second verse: a tighter break than the one before the
                    // translation groups the word list with its line, so the
                    // reader reads the verse first and the meanings as its
                    // gloss.
                    modifier = Modifier.padding(top = Space.Line),
                )
            }
            // More than one translation may be on, and each is drawn in its
            // own column, named, so the reader always knows whose reading
            // they are looking at.
            row.translations.forEach { line ->
                if (row.translations.size > 1) {
                    Text(
                        text = line.packName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = Space.Block),
                    )
                }
                TranslationBody(
                    runs = remember(line.text.text) { RichText.footnotes(line.text.text) },
                    modifier = Modifier.padding(
                        top = if (row.translations.size > 1) Space.Tight else Space.Block,
                    ),
                    sizeSp = settings.translationSp,
                    lineSp = settings.translationLineSp,
                    arabicSp = settings.arabicSp * 0.8f,
                    onFootnote = onFootnote,
                )
            }
            // The reference closes the block the way a printed study Quran
            // numbers its verses: after the reader has read the ayah, not as
            // a heading before it. It takes the theme's secondary tone
            // rather than a quiet alpha, because an alpha that whispers on
            // paper sinks under 4.5:1 on the sepia ground.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Line),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = "${row.ayah.surah}:${row.ayah.ayah}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One Arabic line: its words, and the wash the reciter is on. */
private data class ArabicLine(val text: AnnotatedString, val washRange: IntRange?)

/**
 * The ayah as whole words. The recited word carries its range instead of a
 * style span, so the surface above can wash it in a rounded shape: a style
 * boundary inside an Arabic word breaks its letter joining, and a span is
 * always a bare rectangle behind the glyphs.
 */
private fun arabic(row: StudyRow, playing: Boolean, playingWord: Int?): ArabicLine {
    val words = row.words
    if (words.isEmpty()) return ArabicLine(AnnotatedString(row.ayah.text), null)
    var washRange: IntRange? = null
    val text = buildAnnotatedString {
        words.forEachIndexed { index, word ->
            if (index > 0) append(' ')
            val start = length
            append(word.text)
            if (playing && word.position == playingWord) washRange = start until length
        }
    }
    return ArabicLine(text, washRange)
}

/**
 * The current word under the reciter, drawn as one rounded wash per line it
 * spans. The geometry comes from the text layout itself, so the wash sits
 * exactly on the word the reciter is saying, in the reading direction too.
 */
private fun DrawScope.drawPlayingWordWash(
    layout: TextLayoutResult,
    range: IntRange,
    wash: Color,
) {
    val start = range.first.coerceAtLeast(0)
    val end = range.last.coerceAtMost(layout.layoutInput.text.length - 1)
    if (end < start) return
    var line = layout.getLineForOffset(start)
    var box: Rect? = null
    for (offset in start..end) {
        val nextLine = layout.getLineForOffset(offset)
        if (nextLine != line) {
            box?.let { drawWordWash(it, wash) }
            box = null
            line = nextLine
        }
        val charBox = layout.getBoundingBox(offset)
        box = box?.let { union ->
            Rect(
                left = min(union.left, charBox.left),
                top = min(union.top, charBox.top),
                right = max(union.right, charBox.right),
                bottom = max(union.bottom, charBox.bottom),
            )
        } ?: charBox
    }
    box?.let { drawWordWash(it, wash) }
}

/** One word's wash: rounded, with the room a mark needs around the glyphs. */
private fun DrawScope.drawWordWash(box: Rect, wash: Color) {
    val pad = box.height * 0.08f
    drawRoundRect(
        color = wash,
        topLeft = Offset(box.left - pad, box.top + pad * 0.5f),
        size = Size(box.width + pad * 2, box.height - pad),
        cornerRadius = CornerRadius(box.height * 0.26f),
    )
}

private const val BASMALLAH = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064e\u0647\u0650 " +
    "\u0671\u0644\u0631\u0651\u064e\u062d\u0652\u0645\u064e\u0670\u0646\u0650 \u0671\u0644\u0631\u0651\u064e\u062d\u0650\u064a\u0645\u0650"
