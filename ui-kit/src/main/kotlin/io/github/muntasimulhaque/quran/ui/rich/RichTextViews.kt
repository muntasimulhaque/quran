package io.github.muntasimulhaque.quran.ui.rich

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.TextBlock
import io.github.muntasimulhaque.quran.core.TextBlockKind
import io.github.muntasimulhaque.quran.core.TextRun
import io.github.muntasimulhaque.quran.data.Footnote
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.Inter
import io.github.muntasimulhaque.quran.ui.theme.LatinReading

/**
 * Draws parsed runs with the manuscript's two voices: Literata for Latin and
 * Amiri Quran for Arabic, footnote markers as quiet superscripts, and no
 * styling ever inside a word.
 *
 * A paragraph can carry Arabic at a much larger size than its Latin text, and
 * a superscript marker that sits above the line. The line height is therefore
 * taken from the tallest thing on the line, never from the Latin size alone:
 * otherwise the Arabic and the markers collide with the lines around them as
 * soon as the reader turns the text up.
 */
@Composable
fun TranslationBody(
    runs: List<TextRun>,
    modifier: Modifier = Modifier,
    sizeSp: Float? = null,
    lineSp: Float? = null,
    arabicSp: Float = sizeSp ?: 18f,
    onFootnote: ((Int) -> Unit)? = null,
) {
    val latin = sizeSp ?: LatinReading.fontSize.value
    // Only a paragraph that actually carries Arabic needs the taller line;
    // giving every Latin paragraph the Arabic's room leaves a translation
    // floating in white space.
    val line = if (runs.any { it.arabic }) {
        maxOf(lineSp ?: latin * 1.6f, arabicSp * 1.9f)
    } else {
        lineSp ?: latin * 1.6f
    }
    val base = LatinReading.copy(
        fontSize = latin.sp,
        lineHeight = line.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )
    Text(
        text = annotated(
            runs,
            arabicSize = arabicSp.sp,
            markerSize = (latin * 0.65f).sp,
            quoteColor = null,
            onFootnote = onFootnote,
        ),
        style = base,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * A plain sentence with the matched words washed in the accent color. Used by
 * search results, where the text is a snippet rather than rich runs.
 */
@Composable
fun HighlightedText(
    text: String,
    ranges: List<IntRange>,
    modifier: Modifier = Modifier,
    style: TextStyle = LatinReading,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = 4,
) {
    val wash = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
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
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
}

/** The footnote list of one ayah, hung under its translation. */
@Composable
fun FootnoteList(
    footnotes: List<Footnote>,
    modifier: Modifier = Modifier,
    quiet: Boolean = false,
    sizeSp: Float = 13f,
) {
    if (footnotes.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (quiet) 6.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (quiet) 22.dp else 8.dp),
    ) {
        if (quiet) {
            Box(
                Modifier
                    .fillMaxWidth(0.14f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            )
        }
        val primary = MaterialTheme.colorScheme.onSurfaceVariant
        footnotes.forEach { footnote ->
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(color = primary.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = (sizeSp - 2).sp),
                    ) { append(footnote.number.toString()) }
                    append("  ")
                    append(footnote.text)
                },
                style = MaterialTheme.typography.bodySmall
                    .merge(SpanStyle(fontFamily = Inter, fontSize = sizeSp.sp))
                    .plus(
                        ParagraphStyle(
                            lineHeight = (sizeSp * 1.5f).sp,
                            textIndent = TextIndent(firstLine = 0.sp, restLine = 14.sp),
                        ),
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Draws the tafsir's paragraphs and headings. The size the reader chose for
 * the tafsir moves the whole block, headings included, and the line height
 * gives the quoted Arabic inside a Latin paragraph room to breathe.
 */
@Composable
fun RichBlocks(
    blocks: List<TextBlock>,
    modifier: Modifier = Modifier,
    sizeSp: Float = 16f,
    lineSp: Float = 26f,
    arabicSp: Float = 18f,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        blocks.forEach { block ->
            val line = if (block.runs.any { it.arabic }) {
                maxOf(lineSp, arabicSp * 1.9f)
            } else {
                lineSp
            }
            when (block.kind) {
                TextBlockKind.HEADING -> Text(
                    text = annotated(
                        block.runs,
                        arabicSize = arabicSp.sp,
                        markerSize = (sizeSp * 0.65f).sp,
                        quoteColor = null,
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (sizeSp + 1).sp,
                        lineHeight = line.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextBlockKind.PARAGRAPH -> Text(
                    text = annotated(
                        block.runs,
                        arabicSize = arabicSp.sp,
                        markerSize = (sizeSp * 0.65f).sp,
                        quoteColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    style = LatinReading.copy(
                        fontSize = sizeSp.sp,
                        lineHeight = line.sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Draws the As-Sa'di tafsir: Arabic prose with the Quran quotes set apart. */
@Composable
fun ArabicBody(runs: List<TextRun>, modifier: Modifier = Modifier, sizeSp: Float = 20f) {
    Text(
        text = annotated(
            runs,
            arabicSize = sizeSp.sp,
            markerSize = (sizeSp * 0.6f).sp,
            quoteColor = MaterialTheme.colorScheme.onBackground,
        ),
        style = TextStyle(fontFamily = Amiri, fontSize = sizeSp.sp, lineHeight = (sizeSp * 1.9f).sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Right,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun annotated(
    runs: List<TextRun>,
    arabicSize: TextUnit,
    markerSize: TextUnit,
    quoteColor: Color?,
    onFootnote: ((Int) -> Unit)? = null,
): AnnotatedString {
    val markerColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = markerColor
    val context = LocalContext.current
    val arabicFonts = remember(context) { ArabicFonts(context) }
    return buildAnnotatedString {
        runs.forEach { run ->
            val marker = run.marker
            if (marker != null) {
                val style = SpanStyle(
                    color = markerColor.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                    fontSize = markerSize,
                    baselineShift = BaselineShift.Superscript,
                )
                if (onFootnote == null) {
                    withStyle(style) { append(marker.toString()) }
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
                val color = if (run.quote) quoteColor ?: primary else Color.Unspecified
                var index = 0
                while (index < run.text.length) {
                    val codepoint = run.text.codePointAt(index)
                    val family = arabicFonts.family(codepoint)
                    var end = index + Character.charCount(codepoint)
                    while (end < run.text.length) {
                        val next = run.text.codePointAt(end)
                        if (arabicFonts.family(next) != family) break
                        end += Character.charCount(next)
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = family,
                            fontSize = arabicSize,
                            fontWeight = if (run.bold) FontWeight.SemiBold else null,
                            fontStyle = if (run.italic) FontStyle.Italic else null,
                            color = color,
                        ),
                    ) { append(run.text.substring(index, end)) }
                    index = end
                }
            } else {
                withStyle(
                    SpanStyle(
                        fontWeight = if (run.bold) FontWeight.SemiBold else null,
                        fontStyle = if (run.italic) FontStyle.Italic else null,
                        color = if (run.quote) quoteColor ?: primary else Color.Unspecified,
                    ),
                ) { append(run.text) }
            }
        }
    }
}
