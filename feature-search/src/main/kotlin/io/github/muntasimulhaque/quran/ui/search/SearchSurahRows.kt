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
import io.github.muntasimulhaque.quran.ui.theme.TypeLadder
import io.github.muntasimulhaque.quran.ui.theme.amiriDisplay
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.bookRowName
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The two rows a search answers with when the query is a place rather than a
 * word: the surah a reference or a name matched, and one reference from the
 * lookup that names its own ayah.
 *
 * Both stand on the sheet's own row, [sheetRow], so a search result and a
 * settings row answer a finger at the same air and the same press, and both
 * draw the Book's letter where they show the Quran's words.
 *
 * A reference the reader typed as a juz says so: they asked for "Juz 30", and
 * the row answers in that word with the ayah the juz opens drawn under it.
 * Naming it by its surah and verse key instead would answer a question the
 * reader did not ask (owner decision, 22.3).
 */
@Composable
internal fun ReferenceRow(
    ayah: Ayah,
    surahName: String?,
    hafs: FontFamily,
    arabicStep: Float,
    /** The juz the reader named, when the reference was one. */
    juz: Int? = null,
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
                text = if (juz != null) {
                    stringResource(R.string.search_reference_juz, juz)
                } else {
                    listOfNotNull(surahName, localizedDigits(ayah.verseKey))
                        .joinToString("  ·  ")
                },
                style = bookRowName(),
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
                // The label names the kind of result; it is not an action, so
                // it wears the group label's voice and the quiet ink (owner
                // decision, the consistency audit).
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                // (owner decision, 10.0).
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
    }
}

@Composable
internal fun placeName(place: String): String =
    if (place.equals("makkah", true)) {
        stringResource(R.string.search_revelation_makki)
    } else {
        stringResource(R.string.search_revelation_madani)
    }
