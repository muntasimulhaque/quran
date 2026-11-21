package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TranslationLine
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.translationIsNamed
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.rich.TajweedPainter
import io.github.muntasimulhaque.quran.ui.rich.drawTajweed
import io.github.muntasimulhaque.quran.ui.rich.spanInk
import io.github.muntasimulhaque.quran.ui.rich.spanPlace
import io.github.muntasimulhaque.quran.ui.rich.spanTarget
import io.github.muntasimulhaque.quran.ui.theme.ControlMetrics
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Space

@Composable
internal fun AyahBlock(
    row: ReadingRow,
    hafs: FontFamily,
    metrics: ReadingMetrics,
    isSelected: Boolean,
    playingAyah: Int?,
    /**
     * The word the reciter is on, read in the draw and only for the ayah
     * being heard. The null on every other block keeps a word change from
     * rebuilding its line (owner decision, 19.9).
     */
    playedWord: State<Int?>?,
    /** The search arrival to reveal on this ayah, when it is this one. */
    reveal: ReadingReveal? = null,
    /** True when the reader's own word-by-word switch is on. */
    showWordAid: Boolean = metrics.wordByWord,
    /** True when the reader has written a note on this ayah. */
    noted: Boolean = false,
    /** True when the reader has pressed Save on this ayah. It is spoken, and
     * no longer drawn: the save is said where the tap that made it lives. */
    saved: Boolean = false,
    /** True while this ayah's menu is open, so its door can turn. */
    menuOpen: Boolean = false,
    /** The focus target a jump lands on, when this is the ayah it landed on. */
    focusRequester: FocusRequester? = null,
    /** The ayah's door was tapped, with its own place on the glass. */
    onMenu: (Ayah, IntRect) -> Unit,
    onBackgroundTap: () -> Unit,
    onFootnote: (TranslationLine, Int) -> Unit,
    /** The word the reader pressed and held, with its own meaning. */
    onWord: (OpenWord) -> Unit,
) {
    val palette = LocalPagePalette.current
    val playing = row.ayah.number == playingAyah
    val haptics = LocalHapticFeedback.current
    // The gesture's two callbacks are read through their latest value: a
    // pointerInput's lambda is not restarted by a recomposition, and a long
    // press that began before the page rebuilt must still open the current
    // sheet (owner decision, 15.9).
    val currentWord by rememberUpdatedState(onWord)
    val currentVerseTap by rememberUpdatedState(onBackgroundTap)
    val ayahActions = stringResource(R.string.reading_ayah_actions)
    val savedState = stringResource(R.string.reading_door_saved)
    val notedState = stringResource(R.string.reading_door_noted)
    val paperActions = stringResource(R.string.reading_paper_actions)
    // The end of an ayah: the KFGQPC face's own printed medallion, in the
    // ornament gold, closing the verse it belongs to. The face draws the
    // print's medallion around a run of Arabic-Indic digits, so the reading
    // appends the number alone and the medallion comes with the face (owner
    // decision, 19.5). Every verse closes this way now, on the
    // page that carries a translation as well as on the one that draws the
    // Quran alone: the same verse ended two ways depending on a setting, and
    // the difference was the print's own roundel (owner decision, 19.5). On
    // the page that draws the Quran alone the medallion is also
    // the verse's only number and its door, and the reference line at the
    // head is not drawn, so the screen holds the Quran and nothing else.
    val arabicOnly = !metrics.showTranslation && !showWordAid
    // The page that draws the Quran alone keeps its line's rhythm, so the
    // reader's own marks ride the verse's first line in a rail at its leading
    // edge instead of standing in a head above it: the mark adds no height,
    // and the block is the same height whether or not it carries one (owner
    // decision, 16.9). The rail is the Arabic line's own box at the
    // system's scale, the measure the mark is centered on.
    val firstLine = with(LocalDensity.current) {
        (metrics.arabicLineSp * fontScale).dp
    }
    // The reader's digits, and a spoken name for them: the reference is a
    // number the reader reads, so it follows the interface's digits, and a
    // screen reader hears it named rather than as a bare key
    // (owner decision, 9.1).
    val reference = localizedDigits(row.ayah.verseKey)
    val referenceLabel = stringResource(R.string.reading_ayah_reference, reference)
    val revealWords = reveal?.words.orEmpty()
    // A search that matched a word's meaning marks the word it matched, by
    // its text. The reading used to draw the whole aid to show this; the
    // page now carries the same mark as an Arabic hit, on the one word that
    // answered (owner decision, 15.9).
    val revealMeanings = reveal?.meanings.orEmpty()
    // The one spoken shape of the whole ayah. The reference and the Arabic
    // used to carry their own descriptions inside a merged clickable block,
    // and TalkBack preferred those descriptions to the translation and the
    // word meanings, which had no description of their own. The block now
    // says the whole ayah once, in the reader's language.
    val spokenAyah = remember(row.ayah.text) { Arabic.portableText(row.ayah.text) }
    val spokenContent = remember(
        referenceLabel,
        spokenAyah,
        row.translations,
        row.meanings,
        metrics.showTranslation,
        showWordAid,
    ) {
        buildString {
            append(referenceLabel)
            append(". ")
            append(spokenAyah)
            if (metrics.showTranslation) {
                row.translations.forEach { line ->
                    append(". ")
                    append(RichText.plain(line.text.text))
                }
            }
            if (showWordAid) {
                val glosses = row.meanings.mapNotNull { it.meaning }
                if (glosses.isNotEmpty()) {
                    append(". ")
                    append(glosses.joinToString(", "))
                }
            }
        }
    }
    val ornamentStyle = remember(hafs, metrics.arabicSp, palette.ornament) {
        SpanStyle(
            fontFamily = hafs,
            // 24 of the ayah's 26: the medallion's own ink is about 0.89 em
            // tall, so this lands it on the line the way the printed page
            // does and keeps the digits inside it legible (owner decision,
            // 14.0). It replaces the 0.72 the Amiri rosette wore,
            // whose oval openwork read as a bracket and whose digits were
            // tiny.
            fontSize = TextSize.medallionAt(metrics.arabicSp).sp,
            color = palette.ornament,
        )
    }
    // The block's own ground is the selected ayah's wash and nothing else.
    // The reciter's word is marked on the word, not on the whole ayah: the
    // ayah-level recitation wash was removed when the word wash came back,
    // because the two tints together put the seven rule colors under their
    // contrast floor (owner decision, 15.9).
    //
    // The wash is the accent's own family at the lightness the warm tone
    // carried, so the one surface that says "this verse is the one you are
    // acting on" says it in the accent's voice (owner decision, 21.0).
    val wash = if (isSelected) palette.selected else Color.Transparent
    // The padding belongs to the paper, so the gaps between ayahs still
    // answer a tap with the reading chrome. The block is set apart by the
    // room around it, not by a rule: the app draws no separators.
    //
    // How much room is two cases and not one, because the block is two
    // things. Carrying a translation or a word list, it is a card, and two
    // cards need the room the app's rows keep ([Space.Row], with the wash's
    // own 10 dp inside), because the eye has to leave one stack of text and
    // find the head of the next. Arabic only, it is a line of the mushaf,
    // and the verses of a page ride the line's own rhythm, one after
    // another, as the print sets them.
    //
    // They did not. An Arabic-only block kept the card's 44 dp, so a verse
    // stood 1.8 of a line from the next, with about three times the white a
    // line break leaves between one verse's last mark and the next verse's
    // first: one page with two rhythms, which reads as a table of rows. The
    // outer paper gap is gone now, and only the wash's own [Space.Tight] on
    // each side stands between two verses: 8 dp from text to text, so the
    // glass holds more of the surah and the face's own medallion is the
    // separator, not a blank line. The line still ends ragged at the margin,
    // so the reader loses a blank line and nothing else (owner decision, 13.7:
    // the verse gap was the room a Browse row keeps, and the Quran
    // was borrowing a list's rhythm).
    //
    // The surah's own opening above the first verse keeps its 14 dp foot, so
    // the surah's name stands 18 dp off that verse, more than two verses
    // stand from each other.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // The door is a 48 dp row, and it stands inside the head the
            // block already kept: the card's own room above and below it is
            // untouched, so two blocks that carry more than the Arabic still
            // stand 24 dp apart (owner decision, 13.3).
            .padding(vertical = if (arabicOnly) 0.dp else Space.Row),
    ) {
        val press = remember { MutableInteractionSource() }
        // The block's own place on the glass, for the screen reader's action:
        // a reader who cannot reach the door still has the same door offered
        // as an action, and it opens where the block stands.
        val blockBounds = remember { mutableStateOf(IntRect.Zero) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    // The block's place is read only when its door opens, but
                    // the callback runs on every layout that moves it, which
                    // is every frame of a scroll. It is written only when it
                    // has changed (owner decision, 19.8).
                    val box = it.boundsInWindow()
                    val next = IntRect(
                        box.left.toInt(),
                        box.top.toInt(),
                        box.right.toInt(),
                        box.top.toInt() + 1,
                    )
                    if (next != blockBounds.value) blockBounds.value = next
                }
                .testTag("ayah-${row.ayah.verseKey}")
                .clip(Radii.Card)
                .background(wash)
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
                )
                // A tap belongs to the page and only to the page: it brings
                // or puts away the bar, and it never chooses a verse. The
                // ayah's own door is its number, at the head of the block, so
                // the tap carries no ripple over the Quran's own letters (owner
                // decision, 16.7: 16.6's whole-verse tap made the
                // page's own tap impossible, because almost the whole page is
                // verse). The word's own long press is the one hold the
                // reading keeps: it is the word meanings aid, turned on by its
                // own Settings row, and it opens the word's sheet rather than
                // the ayah's actions (owner decision, 16.7).
                .clickable(
                    interactionSource = press,
                    indication = null,
                    onClick = { currentVerseTap.invoke() },
                )
                // Every ayah gives a screen reader the same two doors a finger
                // has: the ayah's own actions, and the paper's own tap. The
                // actions are offered here as well as through the door,
                // because a reader who cannot see the number must still have
                // the door.
                .semantics {
                    contentDescription = spokenContent
                    // The reader's own mark is a state of the verse, not a
                    // drawing: the note is spoken here, where the verse is, as
                    // it is on the door. The save is spoken too, though the
                    // page draws nothing for it: a reader who cannot see the
                    // menu's lit row still learns, while reading, that this
                    // ayah is one of their own (owner decision, 17.7).
                    if (saved || noted) {
                        stateDescription = buildList {
                            if (saved) add(savedState)
                            if (noted) add(notedState)
                        }.joinToString(", ")
                    }
                    customActions = listOf(
                        CustomAccessibilityAction(ayahActions) {
                            onMenu(row.ayah, blockBounds.value)
                            true
                        },
                        CustomAccessibilityAction(paperActions) {
                            onBackgroundTap()
                            true
                        },
                    )
                }
                .padding(
                    horizontal = Space.WashInset,
                    // The wash's own room, and the tighter measure on a page
                    // that draws the Quran alone: see the block above.
                    vertical = if (arabicOnly) Space.Tight else 10.dp,
                ),
        ) {
            // The reader's own marks, and the reader's own door. On a page
            // that carries a translation the head is the reference on the
            // app's quiet pill, in the action color, with the turn that says
            // it opens, and the note inside the pill when the reader wrote
            // one. On a page that draws the Quran alone there is no reference
            // line: the verse's printed medallion is its number and its door,
            // and the reader's note rides the verse's first line in a rail at
            // its leading edge, so nothing stands above the verse and the
            // page keeps its rhythm (owner decision, 19.5).
            if (!arabicOnly) {
                AyahDoor(
                    reference = reference,
                    key = row.ayah.verseKey,
                    noted = noted,
                    open = menuOpen,
                    onOpen = { onMenu(row.ayah, it) },
                )
            }
            // The roundel's own span is a key as well as the words: the
            // reader's size change and a change of ground both reach the line
            // only through it, and a remembered line that missed it kept the
            // old size and the old gold (owner report, 6.6). The word the
            // reciter is on is not a key here. Only the wash over it changes,
            // and the wash is read in the draw below, so a word boundary
            // redraws the line instead of rebuilding its AnnotatedString
            // (owner decision, 19.9). The code is read by the painter, not by
            // the layout: the line's letters are the same whether the switch
            // is on or off, so the reader's place never moves when the colors
            // do (owner decision, the QPC rendering audit).
            val tajweed = row.ayah.tajweed
            // The line's color geometry, kept between draws: the wash moves
            // while the reciter reads and the block redraws for its own
            // reasons, and neither changes the boxes (owner decision, 19.8).
            val tajweedPainter = remember { TajweedPainter() }
            // A screen reader hears the portable form, not the print's
            // encoding: the same words, with the dotless base and the small
            // letters folded to what a speech engine can pronounce (owner
            // decision, 10.6). The visible line is untouched.
            val arabicLine = remember(
                row.ayah.number,
                row.ayah.ayah,
                row.ayah.text,
                row.words,
                ornamentStyle,
                revealWords,
                revealMeanings,
            ) {
                arabic(
                    row,
                    // Every verse closes with the print's own medallion, and
                    // on a page that carries a translation the medallion is a
                    // mark and not a door: the head's pill is the door there,
                    // and the roundel only closes the verse the way the
                    // printed page closes it. On the page that draws the
                    // Quran alone it is the number and the door both (owner
                    // decision, 19.5).
                    ornamentStyle,
                    revealWords,
                    revealMeanings,
                )
            }
            var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
            // The Arabic line's own place on the glass, so the medallion's tap
            // can become the rect its menu hangs from.
            val textBounds = remember { mutableStateOf(IntRect.Zero) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // The rail is reserved on every Arabic-only verse, so a save
                // never rewraps the Arabic: it is the width of a mark and its
                // gap, and the words start after it on every verse alike.
                if (arabicOnly) {
                    MarksRail(
                        noted = noted,
                        lineHeight = firstLine,
                    )
                }
                Text(
                    text = arabicLine.text,
                    style = arabicStyle(hafs, metrics.arabicSp, metrics.arabicLineSp, palette.ink),
                    textAlign = TextAlign.Right,
                    onTextLayout = { textLayout = it },
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned {
                            val box = it.boundsInWindow()
                            val next = IntRect(
                                box.left.toInt(),
                                box.top.toInt(),
                                box.right.toInt(),
                                box.bottom.toInt(),
                            )
                            if (next != textBounds.value) textBounds.value = next
                        }
                        // In an Arabic-only reading there is no reference line, so
                        // the verse's own printed medallion closes it and is the
                        // ayah's only number and its door: a tap on the medallion
                        // opens the actions, and a tap anywhere else on the line
                        // is the page's own tap, exactly as it is on the paper
                        // around it. The medallion is a few dp of ink, so its
                        // target is grown around the ink to the 48 dp every
                        // control owes a finger; the ink and the page do not move
                        // (owner decision, 16.7).
                        .then(
                            if (arabicOnly) {
                                Modifier.pointerInput(arabicLine.ornamentRange) {
                                    detectTapGestures { position ->
                                        val layout = textLayout
                                        val range = arabicLine.ornamentRange
                                        if (layout != null && range != null &&
                                            ornamentTarget(layout, range).contains(position)
                                        ) {
                                            onMenu(
                                                row.ayah,
                                                ornamentAnchor(layout, range, textBounds.value),
                                            )
                                        } else {
                                            currentVerseTap.invoke()
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            },
                        )
                        .then(
                            if (showWordAid) {
                                Modifier.pointerInput(arabicLine.wordRanges, row.meanings) {
                                    detectTapGestures(
                                        // The tap is still the page's: a word is
                                        // asked for by holding it, so the reading
                                        // keeps its one tap everywhere (owner
                                        // decision, 15.9).
                                        onTap = { currentVerseTap() },
                                        onLongPress = { position ->
                                            val layout = textLayout
                                            if (layout != null) {
                                                val offset = layout.getOffsetForPosition(position)
                                                val index = arabicLine.wordRanges
                                                    .indexOfFirst { offset in it }
                                                val word = row.meanings.getOrNull(index)
                                                val meaning = word?.meaning
                                                if (word != null && !meaning.isNullOrBlank()) {
                                                    // The app's one haptic, on
                                                    // purpose (owner decision,
                                                    // 20.5): a word's sheet
                                                    // arrives under the
                                                    // finger, and that arrival
                                                    // is the one touch the app
                                                    // does not draw.
                                                    // Everything else stays
                                                    // silent.
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    currentWord(
                                                        OpenWord(
                                                            word = word.word,
                                                            meaning = meaning,
                                                            tajweed = word.tajweed,
                                                            reference = row.ayah.verseKey,
                                                            root = row.roots.getOrNull(index),
                                                            grammar = row.grammar.getOrNull(index),
                                                            ayahNumber = row.ayah.number,
                                                            position = row.words.getOrNull(index)?.position
                                                                ?: 0,
                                                            surah = row.ayah.surah,
                                                        ),
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            } else {
                                Modifier
                            },
                        )
                        .drawWithContent {
                            // The word is read here, in the draw, so a change
                            // of it invalidates the wash and not the block
                            // (owner decision, 19.9).
                            val recited = if (playing) {
                                playedWord?.value?.let { arabicLine.wordRangeByPosition[it] }
                            } else {
                                null
                            }
                            val layout = textLayout
                            if (layout == null) {
                                drawContent()
                                return@drawWithContent
                            }
                            // The page has one word wash: the word a search
                            // arrived on and the word the reciter is saying
                            // are both a place on the Quran's own line, and
                            // the seven rule colors have to stay readable on
                            // it. The selected ayah is a ground of its own, so
                            // the word wash is not laid over it (owner
                            // decision, 15.9).
                            if (!isSelected) {
                                arabicLine.matchRanges.forEach { range ->
                                    drawWordWash(layout, range, palette.highlight)
                                }
                                if (
                                    recited != null &&
                                    arabicLine.matchRanges.none { it == recited }
                                ) {
                                    drawWordWash(layout, recited, palette.highlight)
                                }
                            }
                            drawContent()
                            // The colors are painted from the geometry the
                            // block already has in hand, not from a fresh walk
                            // of every character: the wash above moves while
                            // the reciter reads, and each of those redraws
                            // used to rebuild the whole line's boxes (owner
                            // decision, 19.8).
                            if (metrics.tajweed) {
                                val paint = tajweedPainter.paint(
                                    layout,
                                    tajweed,
                                    palette.tajweed,
                                )
                                if (paint != null) drawTajweed(layout, paint)
                            }
                        },
                )
            }
            // More than one translation may be on, and each is drawn in its
            // own column; a column is named when there is more than one, and
            // when it is the only one but not in the reader's language, so
            // the reader always knows whose reading they are looking at. The
            // library still reads the translation for the font size sample;
            // the reading draws it only when the reader asked to see it.
            if (metrics.showTranslation) {
                row.translations.forEach { line ->
                    // The name is read when more than one translation is drawn,
                    // and whenever the column is not in the reader's own
                    // language: a line of another language never stands
                    // unannounced under the ayah (owner decision, 15.3).
                    if (
                        translationIsNamed(
                            row.translations.size,
                            line.language,
                            metrics.readingLanguage,
                        )
                    ) {
                        Text(
                            text = contentPackName(line.packName, line.packNameBn),
                            style = MaterialTheme.typography.bodySmall,
                            // The pack's name is read, so it keeps the theme's
                            // own secondary tone. The 0.75 alpha it wore
                            // measured 3.3:1, under the 4.5:1 rule the search
                            // and the ayah card already closed this same label
                            // with (owner decision, 7.6).
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Space.Block),
                        )
                    }
                    // The translation is read, not selected: the line
                    // is drawn as type and nothing else. Every line of
                    // every ayah on the glass used to carry the platform's
                    // own selection machinery, created as the ayah arrived
                    // and thrown away as it left, with a finger watcher of
                    // its own for the long press that opened it. The Quran
                    // above it was never selectable, so the feature copied
                    // the English or the Bangla sentence and never the
                    // verse; the Share action is the one door that leaves
                    // the app with the whole verse, Arabic included (owner
                    // decision, 19.8).
                    TranslationBody(
                        runs = remember(line.text.text) { RichText.footnotes(line.text.text) },
                        modifier = Modifier.padding(
                            top = if (row.translations.size > 1) Space.Tight else Space.Block,
                        ),
                        sizeSp = metrics.translationSp,
                        lineSp = metrics.translationLineSp,
                        arabicSp = metrics.arabicInlineSp,
                        onFootnote = { number -> onFootnote(line, number) },
                        footnoteNumbers = remember(line.text.footnotes) {
                            line.text.footnotes.map { it.number }.toSet()
                        },
                    )
                }
            }
        }
    }
}

/**
 * The reader's own note on a page that draws the Quran alone.
 *
 * The page has no head: the verse's printed medallion is its number and its
 * door, so a note has nothing to sit in. It used to sit in a 48 dp row above
 * the verse, the height the door's finger needs, which made a noted verse
 * stand 48 dp apart from its neighbors on a page whose whole rhythm is
 * [Space.Tight] on each side of the wash. The mark rides the verse's first
 * line in a rail the width of the mark and its gap, reserved on every verse
 * so a note never rewraps the Arabic, and the block keeps the verse's own
 * height whether or not it carries a note (owner decision, 17.7).
 *
 * The rail is reserved on every verse, so it must never widen: it held two
 * marks while the saved ayah wore a bookmark, and it holds the note's one now
 * that the save is said where the tap that made it lives (owner decision,
 * 17.7). The note stands centered on the first line's own box, so it
 * reads as a note in the page's margin and never as part of the Quran.
 */
@Composable
private fun MarksRail(
    noted: Boolean,
    /** The Arabic line's own box at the system's scale. */
    lineHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(MarkRail)
            .height(lineHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (noted) {
            IconGlyph(
                icon = Icon.Note,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(GlyphSize.ChevronInline)
                    .testTag("door-noted"),
            )
        }
    }
}

/** The room the marks' rail keeps on a page that draws the Quran alone. */
private val MarkRail = GlyphSize.ChevronInline + Space.Tight

/** One Arabic line: its words, the reciter's ranges, and any search match. */
private data class ArabicLine(
    val text: AnnotatedString,
    val matchRanges: List<IntRange>,
    /** Each word's own span by its position, so the wash needs no second walk. */
    val wordRangeByPosition: Map<Int, IntRange>,
    /** Each word's own span, in the verse's order, for a long press to find its meaning. */
    val wordRanges: List<IntRange>,
    /**
     * The printed medallion's own span: the verse's close on every page, and
     * the ayah's door on the page that carries no reference line.
     */
    val ornamentRange: IntRange?,
)

/**
 * The ayah as whole words. The recited word carries its range instead of a
 * style span, so the surface above can wash it in the recitation's own color:
 * a style boundary inside an Arabic word breaks its letter joining, and a
 * color span would re-shape the line the reading has already settled.
 *
 * The tajweed colors are not spans either. The line is shaped once and
 * [drawTajweed] paints over that one layout, because a color span would
 * re-shape the run and put a mark at the bottom of its letter (owner decision,
 * the QPC rendering audit).
 *
 * Every verse closes with its own number, drawn in the face's own printed
 * medallion: the number is appended to the same line, where a printed page
 * puts it, and the KFGQPC face wraps it in the medallion. On a page that
 * carries a translation the medallion is only the verse's close, and the
 * reference and its door stand at the head; on the page that draws the Quran
 * alone it is the number and the door both, so nothing is drawn above the
 * verse (owner decision, 19.5).
 */
private fun arabic(
    row: ReadingRow,
    ornamentStyle: SpanStyle,
    revealWords: Set<Int>,
    revealMeanings: Set<String>,
): ArabicLine {
    val words = row.words
    val matchRanges = mutableListOf<IntRange>()
    val wordRanges = mutableListOf<IntRange>()
    val wordRangeByPosition = HashMap<Int, IntRange>(words.size)
    var ornamentRange: IntRange? = null
    val text = buildAnnotatedString {
        if (words.isEmpty()) {
            append(row.ayah.text)
        } else {
            words.forEachIndexed { index, word ->
                if (index > 0) append(' ')
                val start = length
                append(word.text)
                val range = start until length
                wordRanges += range
                wordRangeByPosition[word.position] = range
                if (word.position in revealWords || word.text in revealMeanings) {
                    matchRanges += range
                }
            }
        }
        append(' ')
        val start = length
        withStyle(ornamentStyle) { append(Arabic.ayahNumber(row.ayah.ayah)) }
        // The medallion's own span, handed up so the reading can make it the
        // ayah's door on a page that carries no reference line (owner
        // decision, 19.5).
        ornamentRange = start until length
    }
    return ArabicLine(
        text = text,
        matchRanges = matchRanges,
        wordRangeByPosition = wordRangeByPosition,
        wordRanges = wordRanges,
        ornamentRange = ornamentRange,
    )
}

/**
 * The medallion's own box on the glass: the rect its menu hangs from.
 *
 * The box is read from the shaped line's own layout, where the medallion's ink
 * stands, and moved by the line's place in the window, because the menu's
 * placement is arithmetic in window coordinates and nothing else can read a
 * text span back through a test. The two steps are [spanInk] and [spanPlace]
 * in `ui-kit`, where that test pins them.
 */
private fun ornamentAnchor(
    layout: TextLayoutResult,
    range: IntRange,
    text: IntRect,
): IntRect = spanPlace(text, ornamentInk(layout, range))

/**
 * The medallion's own ink, as one rectangle over every character of its span.
 * The face draws the medallion from several glyphs, so the first character's
 * box alone is narrower than the mark the reader sees; the union is
 * [spanInk]'s rule, pinned in `ui-kit`.
 */
private fun ornamentInk(layout: TextLayoutResult, range: IntRange): Rect =
    spanInk(range.map { offset -> layout.getBoundingBox(offset) })

/**
 * The medallion's touch target: its ink, grown until the shorter side is 48 dp.
 * The growth is even on both sides, so the target stays centered on the mark
 * the reader sees; a tap that lands on the paper beside the medallion is the
 * door, not a miss (owner decision, 13.8).
 */
private fun PointerInputScope.ornamentTarget(
    layout: TextLayoutResult,
    range: IntRange,
): Rect = spanTarget(ornamentInk(layout, range), ControlMetrics.Target.toPx())
