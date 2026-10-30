package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.SearchHit
import io.github.muntasimulhaque.quran.data.SearchArabicLine
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.arabicLineFor
import io.github.muntasimulhaque.quran.data.shouldShowWordMeaning
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.ReferenceLabel
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.rich.HighlightedText
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * The size a line of Arabic in a result is drawn at before the reader's own
 * step: 22 sp, the page's own word size, two points under the Quran's 24. The
 * base is a step under the page, so a list of results stays a list, and the
 * reader's own Arabic step moves it with the reading: a reader who enlarged
 * the Quran's text for their eyes finds the same preference in the results.
 * The result line is the aid's size, not the page's, and the line ratio is
 * the reading's own, from `TextSize.lineRatio`, so the two surfaces cannot
 * drift apart at the hand of whoever edits one of them next (owner decision,
 * this session).
 */
private const val SearchArabicSp = 22f


@Composable
internal fun AyahRow(
    hit: SearchHit.AyahHit,
    hafs: FontFamily,
    terms: List<String>,
    showSource: Boolean,
    /** The reader's own Arabic step, so the results follow the reading. */
    arabicStep: Float,
    /** The reader's own translation size, so a snippet is the reading's own text. */
    proseSp: Float,
    proseLineSp: Float,
    onOpen: () -> Unit,
) {
    // The reader's own Arabic step moves the result's Arabic, so a reader who
    // needs a larger Quran reads a larger result too. The snippet follows the
    // translation's step as well: it is the same text the reader reads on the
    // page, and a reader who raised the translation must not meet it small
    // again in the sheet they searched from (owner decision, this session).
    val arabicSize = SearchArabicSp * TextSize.step(arabicStep)
    val arabicLine = arabicSize * TextSize.lineRatio(TypeRole.Arabic)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = onOpen)
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row),
    ) {
        ReferenceLabel(text = localizedDigits(hit.ayah.verseKey))
        // The Arabic is drawn when it is the match, or when the row has
        // nothing else to say; otherwise the row opens with the evidence
        // instead of with a page of text that never matched (owner report,
        // 2.3, and the rule is pure, in `data`, with its own tests).
        //
        // The wash is geometry from the shaped line, not a background span:
        // a span re-shapes the matched run and can move the letters under the
        // reader as they type (owner decision, the QPC rendering audit).
        val arabicText = hit.ayah.text
        val matches = remember(arabicText, terms) {
            Search.matchRanges(arabicText, terms, arabic = true)
        }
        val wash = MaterialTheme.colorScheme.primary.copy(alpha = Wash.Match)
        // The rule colors stay on the reading: a result list is a scan
        // surface, and the matched line's wash is not a reading ground. The
        // result line is ink, on purpose (owner decision, the palette
        // session).
        when (val line = arabicLineFor(hit)) {
            SearchArabicLine.Ayah -> {
                Spacer(Modifier.height(Space.Line))
                TajweedText(
                    text = AnnotatedString(arabicText),
                    style = arabicStyle(
                        hafs,
                        arabicSize,
                        arabicLine,
                        MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Right,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    wash = matches,
                    washColor = wash,
                )
            }

            is SearchArabicLine.Words -> {
                Spacer(Modifier.height(Space.Line))
                val joined = line.words.joinToString("  ")
                TajweedText(
                    text = AnnotatedString(joined),
                    // The matched words keep the line's own size: an
                    // emphasis that enlarged them re-laid the line under the
                    // reader while they searched. One size for both Arabic
                    // lines, moved by the reader's own Arabic step (owner
                    // decision, the theme and consistency session).
                    style = arabicStyle(
                        hafs,
                        arabicSize,
                        arabicLine,
                        MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Right,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    wash = listOf(joined.indices),
                    washColor = wash,
                )
            }

            SearchArabicLine.None -> Unit
        }
        hit.translation?.let { translation ->
            Spacer(Modifier.height(Space.Line))
            HighlightedText(
                text = translation.text,
                ranges = translation.ranges,
                sizeSp = proseSp,
                lineSp = proseLineSp,
            )
            if (showSource) {
                Text(
                    text = contentPackName(translation.pack, translation.packName),
                    style = MaterialTheme.typography.bodySmall,
                    // A pack's name is a source line, so it wears the meta
                    // voice and the theme's own secondary tone; the 0.7 alpha
                    // it once wore measured 3.3:1, under the theme's 4.5:1
                    // rule.
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.Tight),
                )
            }
        }
        // The word meaning is drawn only when it is the row's only evidence
        // of the match; the rule is pure and tested in `data` beside the
        // result ordering. When the translation above already carries the
        // wash, the meaning is a second copy of the same fact and the owner
        // read it as noise (owner report). A row that matched only a
        // meaning keeps it, named: an unlabelled line under the ayah would be
        // read as a translation, and it is not one.
        hit.wordMeaning?.takeIf { shouldShowWordMeaning(hit) }?.let { meaning ->
            Text(
                text = stringResource(R.string.search_word_meaning_label),
                style = MaterialTheme.typography.labelMedium,
                // A label naming the evidence under it is not an action: the
                // accent belongs to what answers a tap (owner decision, this
                // session).
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Block),
            )
            HighlightedText(
                text = meaning,
                ranges = remember(meaning, terms) {
                    Search.matchRanges(meaning, terms, arabic = false)
                },
                sizeSp = proseSp,
                lineSp = proseLineSp,
                modifier = Modifier.padding(top = Space.Tight),
            )
        }
    }
}

@Composable
internal fun TafsirRow(
    hit: SearchHit.TafsirHitResult,
    proseSp: Float,
    proseLineSp: Float,
    onOpen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = onOpen)
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row),
    ) {
        val span = if (hit.fromAyah == hit.toAyah) {
            "${hit.surah}:${hit.fromAyah}"
        } else {
            "${hit.surah}:${hit.fromAyah}-${hit.toAyah}"
        }
        Text(
            text = stringResource(
                R.string.search_tafsir_range,
                contentPackName(hit.pack, hit.packName),
                hit.surahName,
                localizedDigits(span),
            ),
            style = MaterialTheme.typography.bodySmall,
            // The pack's name and the passage's span are a source line, not
            // an action: the meta voice and the secondary tone, where it wore
            // the accent (owner decision, the theme and consistency session).
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.Line))
        HighlightedText(
            text = hit.text,
            ranges = hit.ranges,
            sizeSp = proseSp,
            lineSp = proseLineSp,
            maxLines = 5,
        )
    }
}
