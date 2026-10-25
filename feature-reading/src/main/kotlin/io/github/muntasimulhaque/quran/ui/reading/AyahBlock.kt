package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AyahBlock(
    row: ReadingRow,
    hafs: FontFamily,
    metrics: ReadingMetrics,
    isSelected: Boolean,
    playingAyah: Int?,
    playingWord: Int?,
    onAyah: (Ayah) -> Unit,
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
    val arabicOnly = !metrics.showTranslation && !metrics.wordByWord
    // The reader's digits, and a spoken name for them: the reference is a
    // number the reader reads, so it follows the interface's digits, and a
    // screen reader hears it named rather than as a bare key (owner decision,
    // this session).
    val reference = localizedDigits(row.ayah.verseKey)
    val referenceLabel = stringResource(R.string.reading_ayah_reference, reference)
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
        metrics.wordByWord,
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
            if (metrics.wordByWord) {
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.Row),
    ) {
        val haptics = LocalHapticFeedback.current
        val press = remember { MutableInteractionSource() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radii.Card)
                .background(wash)
                // A tap belongs to the page: it brings or puts away the
                // chrome, and it is how a raised pill is dismissed. An
                // ayah's own actions are one long press, answered by a
                // haptic so the gesture says it landed; the tap carries no
                // ripple over the Quran's own letters (owner decision, this
                // session: the one-tap ayah of 6.5 was reverted, because a
                // stray touch raised the pill and the reading around an ayah
                // gave the reader almost nothing to tap it away with).
                .combinedClickable(
                    interactionSource = press,
                    indication = null,
                    onClick = onBackgroundTap,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAyah(row.ayah)
                    },
                )
                // Every ayah gives a screen reader the same two doors a finger
                // has: the ayah's actions, which is the one gesture the
                // reading is built around, and the paper's own tap.
                .semantics {
                    contentDescription = spokenContent
                    customActions = listOf(
                        CustomAccessibilityAction(ayahActions) {
                            onAyah(row.ayah)
                            true
                        },
                        CustomAccessibilityAction(paperActions) {
                            onBackgroundTap()
                            true
                        },
                    )
                }
                .padding(horizontal = 8.dp, vertical = 10.dp),
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
                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelMedium,
                    // The theme's secondary tone clears 4.5:1; a quiet alpha
                    // did not (owner decision, the theme and consistency
                    // session).
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Space.Tight),
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
            ) {
                arabic(row, playing, recitedWord, arabicOnly, ornamentStyle)
            }
            var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                text = arabicLine.text,
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = metrics.arabicSp.sp,
                    lineHeight = metrics.arabicLineSp.sp,
                    color = palette.ink,
                ),
                textAlign = TextAlign.Right,
                onTextLayout = { textLayout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        val range = arabicLine.washRange
                        val layout = textLayout
                        if (layout == null) {
                            drawContent()
                            return@drawWithContent
                        }
                        if (range != null) drawPlayingWordWash(layout, range, palette.highlight)
                        drawContent()
                        drawTajweedColors(layout, tajweed, palette.tajweed, metrics.tajweed)
                    },
            )
            if (metrics.wordByWord && row.meanings.any { it.meaning != null }) {
                WordByWord(
                    meanings = row.meanings,
                    hafs = hafs,
                    metrics = metrics,
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
                            style = MaterialTheme.typography.labelMedium,
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

/** One Arabic line: its words, and the wash the reciter is on. */
private data class ArabicLine(val text: AnnotatedString, val washRange: IntRange?)

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
): ArabicLine {
    val words = row.words
    val line = if (words.isEmpty()) {
        buildAnnotatedString {
            append(row.ayah.text)
            if (ornament) {
                append(' ')
                withStyle(ornamentStyle) { append(Arabic.ayahNumber(row.ayah.ayah)) }
            }
        } to null
    } else {
        var washRange: IntRange? = null
        buildAnnotatedString {
            words.forEachIndexed { index, word ->
                if (index > 0) append(' ')
                val start = length
                append(word.text)
                if (playing && word.position == playingWord) washRange = start until length
            }
            if (ornament) {
                append(' ')
                withStyle(ornamentStyle) { append(Arabic.ayahNumber(row.ayah.ayah)) }
            }
        } to washRange
    }
    return ArabicLine(line.first, line.second)
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

