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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.SearchHit
import io.github.muntasimulhaque.quran.data.SearchArabicLine
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.arabicLineFor
import io.github.muntasimulhaque.quran.data.shouldShowWordMeaning
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.rich.HighlightedText
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * The size a line of Arabic in a result is drawn at before the reader's own
 * step: 22 sp, the size both Arabic lines of a result share, the whole ayah
 * and the matched words alone. The base is a step under the page, so a list
 * of results stays a list, and the reader's own Arabic step moves it with the
 * reading: a reader who enlarged the Quran's text for their eyes finds the
 * same preference in the results (owner decision, the theme and consistency
 * session; the step reaches search, this session). The line ratio is the
 * reading's own, from `TextSize.lineRatio`, so the two surfaces cannot drift
 * apart at the hand of whoever edits one of them next.
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
    onAyah: (Ayah) -> Unit,
) {
    // The reader's own Arabic step moves the result's Arabic, so a reader who
    // needs a larger Quran reads a larger result too; the snippets stay on
    // the interface ladder, because a result list is for scanning.
    val arabicSize = SearchArabicSp * TextSize.step(arabicStep)
    val arabicLine = arabicSize * TextSize.lineRatio(TypeRole.Arabic)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = { onAyah(hit.ayah) })
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row),
    ) {
        Text(
            text = localizedDigits(hit.ayah.verseKey),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
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
        when (val line = arabicLineFor(hit)) {
            SearchArabicLine.Ayah -> {
                Spacer(Modifier.height(8.dp))
                TajweedText(
                    text = AnnotatedString(arabicText),
                    style = TextStyle(
                        fontFamily = hafs,
                        fontSize = arabicSize.sp,
                        lineHeight = arabicLine.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Right,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    wash = matches,
                    washColor = wash,
                )
            }

            is SearchArabicLine.Words -> {
                Spacer(Modifier.height(8.dp))
                val joined = line.words.joinToString("  ")
                TajweedText(
                    text = AnnotatedString(joined),
                    style = TextStyle(
                        fontFamily = hafs,
                        // The matched words keep the line's own size: an
                        // emphasis that enlarged them re-laid the line under
                        // the reader while they searched. One size for both
                        // Arabic lines, moved by the reader's own Arabic
                        // step (owner decision, the theme and consistency
                        // session; this session).
                        fontSize = arabicSize.sp,
                        lineHeight = arabicLine.sp,
                        color = MaterialTheme.colorScheme.onSurface,
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
            Spacer(Modifier.height(8.dp))
            HighlightedText(
                text = translation.text,
                ranges = translation.ranges,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
            )
            if (showSource) {
                Text(
                    text = contentPackName(translation.pack, translation.packName),
                    style = MaterialTheme.typography.labelMedium,
                    // A pack's name is read, so it keeps the theme's own
                    // secondary tone; the 0.7 alpha it wore measured 3.3:1,
                    // under the theme's 4.5:1 rule.
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
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Space.Block),
            )
            HighlightedText(
                text = meaning,
                ranges = remember(meaning, terms) {
                    Search.matchRanges(meaning, terms, arabic = false)
                },
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                modifier = Modifier.padding(top = Space.Tight),
            )
        }
    }
}

@Composable
internal fun TafsirRow(hit: SearchHit.TafsirHitResult, onAyah: (Ayah) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = { onAyah(hit.ayah) })
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
            style = MaterialTheme.typography.labelMedium,
            // The pack's name is a name, not an action: the tafsir row's
            // label wears the same secondary tone the translation's pack
            // name wears, where it wore the accent (owner decision, the
            // theme and consistency session).
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        HighlightedText(
            text = hit.text,
            ranges = hit.ranges,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
            maxLines = 5,
        )
    }
}
