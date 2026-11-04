package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.TypeLadder
import io.github.muntasimulhaque.quran.ui.theme.amiriDisplay
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.bookRowName
import io.github.muntasimulhaque.quran.ui.theme.Space

@Composable
internal fun ReferenceRow(
    ayah: Ayah,
    surahName: String?,
    hafs: FontFamily,
    arabicStep: Float,
    onAyah: (Ayah) -> Unit,
) {
    val arabicSize = (TextSize.sp(TypeRole.Arabic, TextSize.DEFAULT) - 2f) * TextSize.step(arabicStep)
    val arabicLine = arabicSize * TextSize.lineRatio(TypeRole.Arabic)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = { onAyah(ayah) })
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = listOfNotNull(surahName, localizedDigits(ayah.verseKey))
                    .joinToString("  ·  "),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TajweedText(
                text = AnnotatedString(ayah.text),
                style = arabicStyle(
                    hafs,
                    arabicSize,
                    arabicLine,
                    MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.End,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Line),
            )
        }
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = surah.nameSimple,
                style = bookRowName(),
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
            style = amiriDisplay(TypeLadder.LIST_ARABIC_SP, MaterialTheme.colorScheme.onSurfaceVariant),
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
internal fun placeName(place: String): String =
    if (place.equals("makkah", true)) {
        stringResource(R.string.search_revelation_makki)
    } else {
        stringResource(R.string.search_revelation_madani)
    }
