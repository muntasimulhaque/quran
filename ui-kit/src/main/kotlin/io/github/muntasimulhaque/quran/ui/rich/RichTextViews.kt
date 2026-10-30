package io.github.muntasimulhaque.quran.ui.rich

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.TextBlock
import io.github.muntasimulhaque.quran.core.TextBlockKind
import io.github.muntasimulhaque.quran.core.TextRun
import io.github.muntasimulhaque.quran.core.ScriptMix
import io.github.muntasimulhaque.quran.core.scriptMix
import io.github.muntasimulhaque.quran.ui.theme.AmiriText
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Wash
import io.github.muntasimulhaque.quran.ui.theme.prose

/**
 * Draws parsed runs with the manuscript's two voices: Literata for Latin and
 * Amiri's body face for Arabic. The body face covers every letter the content
 * carries, so no run is handed to the platform to guess at. Footnote markers
 * are accent superscripts, and no styling is ever taken inside a word.
 *
 * A paragraph can carry Arabic at a much larger size than its Latin text, and
 * a superscript marker that sits above the line. The line height is therefore
 * taken from the tallest thing on the line, never from the Latin size alone:
 * otherwise the Arabic and the markers collide with the lines around them as
 * soon as the reader turns the text up. A block with no Arabic keeps the
 * Latin line exactly; see [lineHeightFor] for the two that carry it.
 */
@Composable
fun TranslationBody(
    runs: List<TextRun>,
    modifier: Modifier = Modifier,
    sizeSp: Float? = null,
    lineSp: Float? = null,
    arabicSp: Float = sizeSp ?: 18f,
    onFootnote: ((Int) -> Unit)? = null,
    /** The notes this text actually carries; null means every marker is a door. */
    footnoteNumbers: Set<Int>? = null,
    /**
     * The ink of the words. The reading's own ink is the default; a surface
     * drawn on a ground of its own, such as the share card's paper, passes
     * the ink of that ground instead (owner report, the audio and type audit
     * session).
     */
    color: Color = LocalPagePalette.current.ink,
) {
    val voice = LocalReadingVoice.current
    val body = sizeSp ?: voice.style.fontSize.value
    val mix = scriptMix(runs)
    val line = lineHeightFor(
        mix = mix,
        latinLine = lineSp ?: body * voice.lineRatio,
        arabicSp = arabicSp,
    )
    val base = voice.prose(body, line)
    Text(
        text = annotated(
            runs,
            arabicSize = arabicSp.sp,
            markerSize = markerSp(body, 0.70f),
            // A symbol inside an all-Arabic block is part of that block and
            // takes its size; inside a Latin sentence it takes the
            // sentence's.
            symbolSize = if (mix == ScriptMix.ARABIC) arabicSp.sp else body.sp,
            quoteColor = color,
            onFootnote = onFootnote,
            footnoteNumbers = footnoteNumbers,
        ),
        style = base,
        color = color,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The line room one block gets, from the scripts it actually carries.
 *
 * Arabic draws its diacritics above and its descenders below letters, so an
 * Arabic paragraph needs more air than a Latin one. The mistake this fixes
 * (owner report) was giving a whole Latin paragraph the Arabic
 * paragraph's air because one quotation appeared in it: every line of an
 * English tafsir paragraph breathed at 42.6 px for one inline word.
 *
 * The two ratios are grounded in the sources' own glyphs, not in taste. The
 * Amiri ink of the tafsir's Arabic-only paragraphs needs 1.67 em at the
 * median and 1.84 em at the 90th percentile, so [ArabicAir] stays where it
 * has been at 1.9, just past that tail: the owner's report was about the
 * mixed paragraphs, and the all-Arabic block's own rhythm was not part of
 * it, so its line does not move. The Arabic inside the 4,836 mixed
 * paragraphs needs 0.97 em at the median and 1.32 em at the very worst (the
 * Prophet's ligature under a stack of marks, and one Urdu blessing), so
 * [MixedAir] at 1.35 covers the whole distribution while bringing the mixed
 * line down from the old 1.9: at the default tafsir size that is 30.2 px of
 * line instead of 42.6 for a 16 sp body, which is the change that made the
 * tafsir's gaps even (owner report). A block whose inline Arabic is
 * taller than any the sources carry today would need this constant raised
 * with it; the measurement is written here so the next session can redo it
 * rather than guess.
 *
 * The tafsir briefly read its Arabic in Hafs (owner decision, the audio and
 * type audit session) and is back in Amiri (owner decision, this session):
 * the measurements above were taken in Amiri ink, so they stand as measured.
 */
private fun lineHeightFor(mix: ScriptMix, latinLine: Float, arabicSp: Float): Float = when (mix) {
    ScriptMix.LATIN -> latinLine
    ScriptMix.MIXED -> maxOf(latinLine, arabicSp * MixedAir)
    ScriptMix.ARABIC -> maxOf(latinLine, arabicSp * ArabicAir)
}

/** The air a Latin paragraph with an Arabic quotation gives the quotation. */
private const val MixedAir = 1.35f

/** The air an all-Arabic paragraph gives its diacritics; unchanged at 1.9. */
private const val ArabicAir = 1.9f

/**
 * The floor under a footnote marker's size. The marker is set at a share of
 * the body it annotates, so at the smallest translation step a 0.70 share
 * is about 8 sp: a figure too small to read and too thin to tap (owner
 * decision, forty-seventh session). Ten is the smallest that still reads
 * as a number, holds its own above the line, and gives the finger room.
 */
private const val MarkerFloor = 10f

/** A marker's size: its share of the body, never below [MarkerFloor]. */
private fun markerSp(bodySp: Float, share: Float) = maxOf(bodySp * share, MarkerFloor).sp
/**
 * A plain sentence with the matched words washed in the accent color. Used by
 * search results, where the text is a snippet rather than rich runs.
 */
@Composable
fun HighlightedText(
    text: String,
    ranges: List<IntRange>,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: Color = LocalPagePalette.current.ink,
    maxLines: Int = 4,
) {
    // A snippet is reading, so it is set in the reading's own voice, at the
    // size the voice stands at when a surface has no size of its own: the
    // interface face inside a result list put the book away the moment a
    // reader searched it (owner decision, this session).
    val voice = LocalReadingVoice.current
    val resolvedStyle = style ?: voice.prose(
        sizeSp = voice.style.fontSize.value,
        lineSp = voice.style.lineHeight.value,
    )
    val wash = MaterialTheme.colorScheme.primary.copy(alpha = Wash.Match)
    val weight = FontWeight.Medium
    Text(
        text = buildAnnotatedString {
            var index = 0
            for (range in ranges) {
                val from = range.first.coerceIn(index, text.length)
                val to = (range.last + 1).coerceIn(from, text.length)
                if (from > index) append(text.substring(index, from))
                if (to > from) {
                    withStyle(SpanStyle(background = wash, fontWeight = weight)) {
                        append(text.substring(from, to))
                    }
                }
                index = to
            }
            if (index < text.length) append(text.substring(index))
        },
        style = resolvedStyle,
        color = color,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Draws the tafsir's paragraphs and headings. The size the reader chose for
 * the tafsir moves the whole block, headings included, and the line height
 * gives the quoted Arabic inside a Latin paragraph room to breathe. The
 * defaults stand at the reading's own two sizes a rich block is drawn at:
 * the tafsir's body and line, which TafsirDirectionTest overrides with its
 * own when it pins the direction rule.
 */
@Composable
fun RichBlocks(
    blocks: List<TextBlock>,
    modifier: Modifier = Modifier,
    sizeSp: Float = 17f,
    lineSp: Float = 27f,
    arabicSp: Float = 18f,
) {
    val voice = LocalReadingVoice.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        blocks.forEachIndexed { index, block ->
            val heading = block.kind == TextBlockKind.HEADING
            // A heading is a real step over the prose, a quarter again its
            // size and in the reading's own weight. It stood one point above
            // the body, which the eye read as the same size (owner decision,
            // this session).
            val mix = scriptMix(block.runs)
            val drawn = if (heading) sizeSp * 1.25f else sizeSp
            val line = lineHeightFor(
                mix = mix,
                // A heading is set tighter than a paragraph, but never below
                // the face's own line box: at 20 sp Literata's ink needs
                // 1.485 of the em, and a long heading (some run to six
                // lines) set at 1.3 put one line's descenders into the next
                // line's ascenders.
                latinLine = if (heading) {
                    maxOf(drawn * 1.3f, drawn * voice.minimumLineRatio)
                } else {
                    lineSp
                },
                arabicSp = arabicSp,
            )
            Text(
                text = annotated(
                    block.runs,
                    arabicSize = arabicSp.sp,
                    markerSize = markerSp(sizeSp, 0.70f),
                    symbolSize = if (mix == ScriptMix.ARABIC) arabicSp.sp else drawn.sp,
                    quoteColor = if (heading) null else LocalPagePalette.current.ink,
                ),
                style = voice.prose(
                    sizeSp = drawn,
                    lineSp = line,
                    color = LocalPagePalette.current.ink,
                    weight = if (heading) FontWeight.SemiBold else FontWeight.Normal,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    // A heading opens a part of the prose, so it takes a step
                    // more air above it than the room between two paragraphs:
                    // the sheet reads as sections instead of as one run
                    // (owner decision, this session).
                    .padding(top = if (heading && index > 0) Space.Line else 0.dp),
            )
        }
    }
}

@Composable
private fun annotated(
    runs: List<TextRun>,
    arabicSize: TextUnit,
    markerSize: TextUnit,
    /** The size a symbol in a sentence is drawn at: the sentence's own. */
    symbolSize: TextUnit,
    quoteColor: Color?,
    onFootnote: ((Int) -> Unit)? = null,
    footnoteNumbers: Set<Int>? = null,
): AnnotatedString {
    // The marker is a door to the note, so it wears the theme's one action
    // color, the same ink the reciter's name, the chevrons, and the note's
    // own sheet title wear: at a share of the body's size and in the body's
    // own tone it read as a smaller word and not as something to touch
    // (owner decision, forty-seventh session). The accent clears the 4.5:1
    // the small figure needs on every ground: 8.1:1 on paper and
    // 8.5:1 on night.
    val markerColor = MaterialTheme.colorScheme.primary
    // What a quoted run falls back to where the caller has no quote color of
    // its own: the reading's ink, never the accent, which now
    // belongs to the marker alone.
    val quoteFallback = LocalPagePalette.current.ink
    return buildAnnotatedString {
        runs.forEach { run ->
            val marker = run.marker
            if (marker != null) {
                val style = SpanStyle(
                    // The marker is a door to the note; it wears the action
                    // color rather than the 0.75 alpha that measured 3.7:1 on
                    // paper, and rather than the reading's own
                    // secondary tone, which the sentence around it wears
                    // (owner decision, forty-seventh session).
                    color = markerColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = markerSize,
                    // The shift is measured, not the platform's. Compose's
                    // own superscript lifts a figure a full cap height, which
                    // put the marker above the line it belongs to and, because
                    // a baseline shift counts toward the line's height, grew
                    // the line to make room for it: the marker of one line
                    // then sat in the gap under the line above. A
                    // third of the size is a raised figure that stays on its
                    // own line.
                    //
                    // The value is in **ems**, so it is a third and not a
                    // third of the size: this read `markerSize.value * 0.3f`
                    // and lifted every marker three whole ems, which put the
                    // markers of a paragraph in a band two lines above the
                    // text they belong to. It reached the store because the
                    // ayah card's frame photographs 2:255, whose six markers
                    // are three digits each, and the committed set was captured
                    // before the change.
                    baselineShift = BaselineShift(0.30f),
                    // Literata's own figures are lining (measured: every
                    // digit sits on the baseline at cap height), so the marker
                    // needs no figure feature at all. It wore "lnum", which
                    // asked for what the face already does.
                )
                val linkable = onFootnote != null &&
                    (footnoteNumbers == null || marker in footnoteNumbers)
                if (!linkable) {
                    // A marker with no note in the source is not a door. It
                    // stays a superscript, in the sentence's own ink, so a
                    // tap does not open an empty sheet and the reader is not
                    // told the note is somewhere it is not.
                    withStyle(style.copy(color = quoteFallback)) { append(marker.toString()) }
                } else {
                    // The marker is a door: tapping it opens the translator's
                    // note without moving the reader out of the sentence.
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "footnote-$marker",
                            styles = TextLinkStyles(style = style),
                            linkInteractionListener = { onFootnote(marker) },
                        ),
                    ) { withStyle(style) { append(marker.toString()) } }
                }
            } else if (run.arabic) {
                // One face for the tafsir's Arabic: Amiri's body text covers
                // every letter the content carries, including the Urdu heh and
                // the pre-shaped phrase the source keeps, so the old
                // per-codepoint fallback to the Hafs face is gone (owner
                // decision, this session). No shipped content puts bold or
                // italic on an Arabic run, and synthesis is off, so an
                // emphasis that ever reached here would draw the face as it is
                // rather than a smeared or slanted copy of it.
                //
                // The Prophet's ligature is the one exception to the inline
                // size: it is a symbol inside a sentence, not a quotation, so
                // it takes the sentence's size and sits in the line. At the
                // quotation's size it stood a third above the words around it
                // (owner decision, this session).
                withStyle(
                    SpanStyle(
                        fontFamily = AmiriText,
                        fontSize = if (RichText.isHonorific(run.text)) symbolSize else arabicSize,
                        color = if (run.quote) quoteColor ?: quoteFallback else Color.Unspecified,
                        fontSynthesis = FontSynthesis.None,
                    ),
                ) { append(run.text) }
            } else {
                withStyle(
                    SpanStyle(
                        fontWeight = if (run.bold) FontWeight.SemiBold else null,
                        fontStyle = if (run.italic) FontStyle.Italic else null,
                        color = if (run.quote) quoteColor ?: quoteFallback else Color.Unspecified,
                    ),
                ) { append(run.text) }
            }
        }
    }
}
