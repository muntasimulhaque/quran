package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.data.TranslationLine
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.rich.drawTajweedColors
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun AyahBlock(
    row: ReadingRow,
    hafs: FontFamily,
    metrics: ReadingMetrics,
    isSelected: Boolean,
    playingAyah: Int?,
    playingWord: Int?,
    /** The search arrival to reveal on this ayah, when it is this one. */
    reveal: ReadingReveal? = null,
    /** True when the reader's own word-by-word switch is on. */
    showWordAid: Boolean = metrics.wordByWord,
    /** True while this ayah's menu is open, so its own door can say so. */
    isMenuOpen: Boolean = false,
    /** True when the reader has pressed Save on this ayah. */
    saved: Boolean = false,
    /** The ayah's door was tapped, with its own place on the glass. */
    onMenu: (Ayah, IntRect) -> Unit,
    onBackgroundTap: () -> Unit,
    onFootnote: (TranslationLine, Int) -> Unit,
) {
    val palette = LocalPagePalette.current
    val playing = row.ayah.number == playingAyah
    val ayahActions = stringResource(R.string.reading_ayah_actions)
    val paperActions = stringResource(R.string.reading_paper_actions)
    // The end of an ayah in an Arabic-only reading: the KFGQPC face's own
    // printed medallion, in the ornament gold, closing the verse it belongs
    // to. The face draws the print's medallion around a run of Arabic-Indic
    // digits, so the reading appends the number alone and the medallion comes
    // with the face (owner decision, this session). The reference line at
    // the head of the block is not drawn then, so the screen holds the Quran
    // and nothing else.
    val arabicOnly = !metrics.showTranslation && !showWordAid
    // The reader's digits, and a spoken name for them: the reference is a
    // number the reader reads, so it follows the interface's digits, and a
    // screen reader hears it named rather than as a bare key (owner decision,
    // this session).
    val reference = localizedDigits(row.ayah.verseKey)
    val referenceLabel = stringResource(R.string.reading_ayah_reference, reference)
    val revealWords = reveal?.words.orEmpty()
    val revealMeanings = reveal?.meanings.orEmpty()
    val matchWash = MaterialTheme.colorScheme.primary.copy(alpha = Wash.Match)
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
            // 0.9 of the ayah: the medallion's own ink is about 0.89 em tall,
            // so this lands it on the line the way the printed page does and
            // keeps the digits inside it legible (owner decision, this
            // session). It replaces the 0.72 the Amiri rosette wore, whose
            // oval openwork read as a bracket and whose digits were tiny.
            fontSize = (metrics.arabicSp * 0.9f).sp,
            color = palette.ornament,
        )
    }
    val wash = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = Wash.Selected)
        playing -> palette.highlight.copy(alpha = palette.highlight.alpha * 0.55f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
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
    // first: one page with two rhythms, which reads as a table of rows. At
    // [Space.Tight] inside and out a verse stands 16 dp from the next, a
    // quarter of a line further than a line break and twice the white of
    // one, so the glass holds four verses in ten more than it did. The
    // face's own medallion still closes the verse and the line still ends
    // ragged at the margin, so the reader loses a blank line and nothing
    // else (owner decision, this session: the verse gap was the room a
    // Browse row keeps, and the Quran was borrowing a list's rhythm).
    //
    // The surah's own opening above the first verse keeps its 14 dp foot, so
    // the surah's name stands 22 dp off that verse, more than two verses
    // stand from each other.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // The door is a 48 dp row, and it stands inside the head the
            // block already kept: the card's own room above and below it is
            // untouched, so two blocks that carry more than the Arabic still
            // stand 24 dp apart (owner decision, this session).
            .padding(vertical = if (arabicOnly) Space.Tight else Space.Row),
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
                    val box = it.boundsInWindow()
                    blockBounds.value = IntRect(
                        box.left.toInt(),
                        box.top.toInt(),
                        box.right.toInt(),
                        box.top.toInt() + 1,
                    )
                }
                .clip(Radii.Card)
                .background(wash)
                // A tap belongs to the page: it brings or puts away the
                // chrome, and it is how a raised control is dismissed. The
                // ayah's own door is its number, at the head of the block,
                // so the tap carries no ripple over the Quran's own letters
                // (owner decision, this session: the one-tap ayah of 6.5 and
                // the long press that followed it are both gone, because a
                // signifier that has to be taught is not a signifier).
                .clickable(
                    interactionSource = press,
                    indication = null,
                    onClick = onBackgroundTap,
                )
                // Every ayah gives a screen reader the same two doors a finger
                // has: the ayah's own actions, and the paper's own tap. The
                // actions are offered here as well as through the door,
                // because a reader who cannot see the number must still have
                // the door.
                .semantics {
                    contentDescription = spokenContent
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
            // The ayah's reference stands at the head of its block, in the
            // reader's own digits, so the reader meets it as the block arrives
            // rather than after the translation's last line: a reader moving
            // through the surah always knows which ayah is on the glass, and
            // the numbers still form one rail down the page (owner decision,
            // this session: it was drawn at the block's foot, which read as a
            // closing note and was off the glass while the ayah itself was
            // read). In an Arabic-only reading the number closes the verse
            // itself, in the face's own printed medallion, so no separate line
            // is drawn.
            if (!arabicOnly) {
                AyahDoor(
                    reference = reference,
                    open = isMenuOpen,
                    marked = saved,
                    onOpen = { onMenu(row.ayah, it) },
                )
            }
            // The roundel's own span is a key as well as the words: the
            // reader's size change and a change of ground both reach the
            // line only through it, and a remembered line that missed it
            // kept the old size and the old gold (owner report, this
            // session).
            val recitedWord = playingWord.takeIf { playing }
            // The code is read by the painter, not by the layout: the line's
            // letters are the same whether the switch is on or off, so the
            // reader's place never moves when the colors do (owner decision,
            // the QPC rendering audit).
            val tajweed = row.ayah.tajweed
            // A screen reader hears the portable form, not the print's
            // encoding: the same words, with the dotless base and the small
            // letters folded to what a speech engine can pronounce (owner
            // decision, the palette session). The visible line is untouched.
            val arabicLine = remember(
                row.ayah.number,
                row.ayah.ayah,
                row.ayah.text,
                row.words,
                playing,
                recitedWord,
                arabicOnly,
                ornamentStyle,
                revealWords,
            ) {
                arabic(row, playing, recitedWord, arabicOnly, ornamentStyle, revealWords)
            }
            var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
            // The Arabic line's own place on the glass, so the medallion's tap
            // can become the rect its menu hangs from.
            val textBounds = remember { mutableStateOf(IntRect.Zero) }
            Text(
                text = arabicLine.text,
                style = arabicStyle(hafs, metrics.arabicSp, metrics.arabicLineSp, palette.ink),
                textAlign = TextAlign.Right,
                onTextLayout = { textLayout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        val box = it.boundsInWindow()
                        textBounds.value = IntRect(
                            box.left.toInt(),
                            box.top.toInt(),
                            box.right.toInt(),
                            box.bottom.toInt(),
                        )
                    }
                    // In an Arabic-only reading there is no reference line, so
                    // the verse's own printed medallion closes it and is the
                    // ayah's door: a tap on the medallion opens the actions,
                    // and a tap anywhere else on the line is the page's own
                    // tap, exactly as it is on the paper around it (owner
                    // decision, this session). The medallion is a few dp of
                    // ink and not a 48 dp target; the block's own custom
                    // action is the screen reader's door and the visible mark
                    // is the sighted reader's, the way a footnote's marker is.
                    .then(
                        if (arabicOnly) {
                            Modifier.pointerInput(arabicLine.ornamentRange) {
                                detectTapGestures { position ->
                                    val layout = textLayout
                                    val range = arabicLine.ornamentRange
                                    if (layout != null && range != null &&
                                        layout.getOffsetForPosition(position) in range
                                    ) {
                                        onMenu(
                                            row.ayah,
                                            ornamentAnchor(layout, range, textBounds.value),
                                        )
                                    } else {
                                        onBackgroundTap()
                                    }
                                }
                            }
                        } else {
                            Modifier
                        },
                    )
                    .drawWithContent {
                        val range = arabicLine.washRange
                        val layout = textLayout
                        if (layout == null) {
                            drawContent()
                            return@drawWithContent
                        }
                        arabicLine.matchRanges.forEach { range ->
                            drawPlayingWordWash(layout, range, matchWash)
                        }
                        if (range != null) drawPlayingWordWash(layout, range, palette.highlight)
                        drawContent()
                        drawTajweedColors(layout, tajweed, palette.tajweed, metrics.tajweed)
                    },
            )
            if (showWordAid && row.meanings.any { it.meaning != null }) {
                WordByWord(
                    meanings = row.meanings,
                    hafs = hafs,
                    metrics = metrics,
                    highlightWords = revealMeanings,
                    highlightColor = matchWash,
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
            // they are looking at. The library still reads the translation
            // for the font size sample; the reading draws it only when the
            // reader asked to see it.
            if (metrics.showTranslation) {
                row.translations.forEach { line ->
                    if (row.translations.size > 1) {
                        Text(
                            text = contentPackName(line.pack, line.packName),
                            style = MaterialTheme.typography.bodySmall,
                            // The pack's name is read, so it keeps the theme's
                            // own secondary tone. The 0.75 alpha it wore
                            // measured 3.3:1, under the 4.5:1 rule the search
                            // and the ayah card already closed this same
                            // label with (owner decision, the theme and
                            // consistency session).
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Space.Block),
                        )
                    }
                    // The translation is the reader's to copy: a line of
                    // Saheeh International can leave the app as words, not
                    // only as a picture of the page. The Quran above it stays
                    // outside the selection, because the page's own touch
                    // belongs to the verse and the verse is not a paragraph
                    // (owner decision, this session).
                    SelectionContainer {
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
}

/** One Arabic line: its words, the wash the reciter is on, and any search match. */
private data class ArabicLine(
    val text: AnnotatedString,
    val washRange: IntRange?,
    val matchRanges: List<IntRange>,
    /** The printed medallion's own span, which is the ayah's door in Arabic only. */
    val ornamentRange: IntRange?,
)

/**
 * The ayah as whole words. The recited word carries its range instead of a
 * style span, so the surface above can wash it in a rounded shape: a style
 * boundary inside an Arabic word breaks its letter joining, and a span is
 * always a bare rectangle behind the glyphs.
 *
 * The tajweed colors are not spans either. The line is shaped once and
 * [drawTajweedColors] paints over that one layout, because a color span would
 * re-shape the run and put a mark at the bottom of its letter (owner decision,
 * the QPC rendering audit).
 *
 * In an Arabic-only reading the verse closes with its own number, drawn in
 * the face's own printed medallion: [ornament] appends the number to the same
 * line, where a printed page puts it, and the KFGQPC face wraps it in the
 * medallion, so the reference at the head of the block is not drawn (owner decision,
 * this session).
 */
private fun arabic(
    row: ReadingRow,
    playing: Boolean,
    playingWord: Int?,
    ornament: Boolean,
    ornamentStyle: SpanStyle,
    revealWords: Set<Int>,
): ArabicLine {
    val words = row.words
    var washRange: IntRange? = null
    val matchRanges = mutableListOf<IntRange>()
    var ornamentRange: IntRange? = null
    val text = buildAnnotatedString {
        if (words.isEmpty()) {
            append(row.ayah.text)
        } else {
            words.forEachIndexed { index, word ->
                if (index > 0) append(' ')
                val start = length
                append(word.text)
                if (playing && word.position == playingWord) washRange = start until length
                if (word.position in revealWords) matchRanges += start until length
            }
        }
        if (ornament) {
            append(' ')
            val start = length
            withStyle(ornamentStyle) { append(Arabic.ayahNumber(row.ayah.ayah)) }
            // The medallion's own span, handed up so the reading can make it
            // the ayah's door on a page that carries no reference line (owner
            // decision, this session).
            ornamentRange = start until length
        }
    }
    return ArabicLine(text, washRange, matchRanges, ornamentRange)
}

/**
 * The medallion's own box on the glass: the rect its menu hangs from.
 *
 * The box is read from the shaped line's own layout, where the medallion's ink
 * stands, and moved by the line's place in the window, because the menu's
 * placement is arithmetic in window coordinates and nothing else can read a
 * text span back through a test.
 */
private fun ornamentAnchor(
    layout: TextLayoutResult,
    range: IntRange,
    text: IntRect,
): IntRect {
    val box = layout.getBoundingBox(range.first)
    return IntRect(
        text.left + box.left.toInt(),
        text.top + box.top.toInt(),
        text.left + box.right.toInt(),
        text.top + box.bottom.toInt(),
    )
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

