package io.github.muntasimulhaque.quran.ui.rich

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
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
 */
@Composable
fun TranslationBody(runs: List<TextRun>, modifier: Modifier = Modifier) {
    Text(
        text = annotated(runs, arabicSize = 18.sp, markerSize = 11.sp, quoteColor = null),
        style = LatinReading,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}

/** The footnote list of one ayah, hung under its translation. */
@Composable
fun FootnoteList(footnotes: List<Footnote>, modifier: Modifier = Modifier) {
    if (footnotes.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val primary = MaterialTheme.colorScheme.primary
        footnotes.forEach { footnote ->
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(color = primary, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                    ) { append(footnote.number.toString()) }
                    append("  ")
                    append(footnote.text)
                },
                style = MaterialTheme.typography.bodySmall
                    .merge(SpanStyle(fontFamily = Inter))
                    .plus(
                        ParagraphStyle(
                            lineHeight = 18.sp,
                            textIndent = TextIndent(firstLine = 0.sp, restLine = 14.sp),
                        ),
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Draws the tafsir's paragraphs and headings. */
@Composable
fun RichBlocks(blocks: List<TextBlock>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        blocks.forEach { block ->
            when (block.kind) {
                TextBlockKind.HEADING -> Text(
                    text = annotated(block.runs, arabicSize = 17.sp, markerSize = 11.sp, quoteColor = null),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextBlockKind.PARAGRAPH -> Text(
                    text = annotated(
                        block.runs,
                        arabicSize = 18.sp,
                        markerSize = 11.sp,
                        quoteColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    style = LatinReading,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Draws the As-Sa'di tafsir: Arabic prose with the Quran quotes set apart. */
@Composable
fun ArabicBody(runs: List<TextRun>, modifier: Modifier = Modifier) {
    Text(
        text = annotated(
            runs,
            arabicSize = 20.sp,
            markerSize = 11.sp,
            quoteColor = MaterialTheme.colorScheme.onBackground,
        ),
        style = TextStyle(fontFamily = Amiri, fontSize = 20.sp, lineHeight = 36.sp),
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
): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val arabicFonts = remember(context) { ArabicFonts(context) }
    return buildAnnotatedString {
        runs.forEach { run ->
            val marker = run.marker
            if (marker != null) {
                withStyle(
                    SpanStyle(
                        color = primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = markerSize,
                        baselineShift = BaselineShift.Superscript,
                    ),
                ) { append(marker.toString()) }
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
