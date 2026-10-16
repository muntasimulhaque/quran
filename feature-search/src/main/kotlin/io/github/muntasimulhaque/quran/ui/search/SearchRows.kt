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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.SearchHit
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.SearchArabicLine
import io.github.muntasimulhaque.quran.data.arabicLineFor
import io.github.muntasimulhaque.quran.data.shouldShowWordMeaning
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.rich.HighlightedText
import io.github.muntasimulhaque.quran.ui.reader.GlyphSize
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash

/**
 * The air a line of Arabic in a result gives its diacritics, as a share of
 * its size. Both Arabic lines a result can draw (the whole ayah, and the
 * matched words alone) breathe at this one ratio, the one the reading
 * itself sets its Arabic at: a verse in a result reads like the verse on
 * the page, and the two line kinds cannot drift apart at the hand of
 * whoever edits one of them next.
 */
private const val SearchArabicAir = 1.9f


@Composable
internal fun ReferenceRow(ayah: Ayah, onAyah: (Ayah) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = { onAyah(ayah) })
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.search_label_go_to),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = ayah.verseKey,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Space.Tight),
            )
        }
        // The row is the door, and it ends in the one chevron mark every
        // other door ends in. An Open button inside a row that already
        // opened on tap was two controls for one act, and TalkBack met a
        // row and a nested button (owner decision, the theme and consistency
        // session).
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = Space.Tight)
                .size(GlyphSize.ChevronRow),
        )
    }
}

@Composable
internal fun SurahRow(surah: Surah, onSurah: (Surah) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = { onSurah(surah) })
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.search_label_surah),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = surah.nameSimple,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Space.Tight),
            )
            Text(
                // The place first, the count second: the order Browse and
                // the reading use, so one surah reads one way everywhere
                // (owner decision, the theme and consistency session).
                text = stringResource(
                    R.string.search_surah_meta,
                    placeName(surah.revelationPlace),
                    surah.versesCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Tight),
            )
        }
        Text(
            text = surah.nameArabic,
            style = TextStyle(fontFamily = Amiri, fontSize = 23.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = Space.Tight)
                .size(GlyphSize.ChevronRow),
        )
    }
}

@Composable
internal fun AyahRow(
    hit: SearchHit.AyahHit,
    hafs: FontFamily,
    terms: List<String>,
    showSource: Boolean,
    onAyah: (Ayah) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = { onAyah(hit.ayah) })
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row),
    ) {
        Text(
            text = "${hit.ayah.verseKey}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        // The Arabic is drawn when it is the match, or when the row has
        // nothing else to say; otherwise the row opens with the evidence
        // instead of with a page of text that never matched (owner report,
        // 2.3, and the rule is pure, in `data`, with its own tests).
        when (val line = arabicLineFor(hit)) {
            SearchArabicLine.Ayah -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = arabic(hit, hafs, terms),
                    style = TextStyle(
                        fontFamily = hafs,
                        fontSize = 22.sp,
                        lineHeight = (22f * SearchArabicAir).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Right,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is SearchArabicLine.Words -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = washed(line.words.joinToString("  ")),
                    style = TextStyle(
                        fontFamily = hafs,
                        // The matched words keep the line's own size: an
                        // emphasis that enlarged them re-laid the line under
                        // the reader while they searched. 22 sp, one size
                        // for both Arabic lines (owner decision, the theme
                        // and consistency session).
                        fontSize = 22.sp,
                        lineHeight = (22f * SearchArabicAir).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    textAlign = TextAlign.Right,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
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
                    text = translation.packName,
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
        Text(
            text = stringResource(
                R.string.search_tafsir_range,
                hit.packName,
                hit.surahName,
                if (hit.fromAyah == hit.toAyah) {
                    "${hit.surah}:${hit.fromAyah}"
                } else {
                    "${hit.surah}:${hit.fromAyah}-${hit.toAyah}"
                },
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

/** Words in a wash, which is how the ayah's own match is drawn above. */
@Composable
internal fun washed(text: String): AnnotatedString {
    // A match wears the same wash everywhere: the accent at [Wash.Match],
    // never the reciter's highlight, which says a word is being heard
    // (owner decision, the audio and type audit session).
    //
    // Arabic is washed and never weighted: `HighlightedText` adds Medium to
    // a Latin match, and Arabic emphasis must never be synthesized on a
    // calligraphic face (see `RichTextViews`). The two scripts share one
    // wash rule and differ only where the script itself must (owner
    // decision, the theme and consistency session).
    val wash = MaterialTheme.colorScheme.primary.copy(alpha = Wash.Match)
    return buildAnnotatedString { withStyle(SpanStyle(background = wash)) { append(text) } }
}

/** The ayah with the matched words washed; the ranges are exact. */
@Composable
internal fun arabic(hit: SearchHit.AyahHit, hafs: FontFamily, terms: List<String>): AnnotatedString {
    val text = hit.ayah.text
    val ranges = remember(text, terms) { Search.matchRanges(text, terms, arabic = true) }
    if (ranges.isEmpty()) return AnnotatedString(text)
    // A match wears the same wash everywhere: the accent at [Wash.Match],
    // never the reciter's highlight, which says a word is being heard
    // (owner decision, the audio and type audit session).
    val wash = MaterialTheme.colorScheme.primary.copy(alpha = Wash.Match)
    return buildAnnotatedString {
        var index = 0
        for (range in ranges) {
            val from = range.first.coerceIn(index, text.length)
            val to = (range.last + 1).coerceIn(from, text.length)
            if (from > index) append(text.substring(index, from))
            if (to > from) {
                withStyle(SpanStyle(background = wash)) { append(text.substring(from, to)) }
            }
            index = to
        }
        if (index < text.length) append(text.substring(index))
    }
}

@Composable
internal fun placeName(place: String): String =
    if (place.equals("makkah", true)) {
        stringResource(R.string.search_place_makkah)
    } else {
        stringResource(R.string.search_place_madinah)
    }
