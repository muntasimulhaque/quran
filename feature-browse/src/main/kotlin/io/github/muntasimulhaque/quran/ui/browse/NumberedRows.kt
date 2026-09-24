package io.github.muntasimulhaque.quran.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.feature.browse.R
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import java.util.Locale

/**
 * The number column Browse's two numbered lists share, and the two rows that
 * wear it.
 *
 * Every number is measured into the width of the widest number its list can
 * hold and set flush to the right edge of that width, so 3, 10, and 114 end
 * their digits at one line and every name after the number starts at the same
 * place whatever the number is. The widest is found by measuring the numbers
 * the list actually draws, never a stand-in: digits are not all alike, and
 * "114" is narrower than "100" in the interface face, so a column sized on
 * the last surah clipped the last digit off the earlier three-digit rows.
 * A column of digits that starts at a different x on every row width is two
 * lists pretending to be one.
 *
 * The width is measured once per list with the reader's own font scale and
 * the interface's own digits, so the column grows with a large system text
 * and a Bangla reading gets a Bangla column.
 */
@Composable
internal fun rememberNumberWidth(numbers: List<Int>): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    val locale = LocalConfiguration.current.locales[0]
    val labels = remember(numbers, locale) { numbers.map { numberLabel(it, locale) } }
    return remember(measurer, style, labels, density) {
        val widest = labels.maxOfOrNull { measurer.measure(it, style).size.width } ?: 0
        with(density) { widest.toDp() }
    }
}

/** One number of a numbered list, in the interface's own digits. */
@Composable
internal fun numberLabel(value: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(value, locale) { numberLabel(value, locale) }
}

private fun numberLabel(value: Int, locale: Locale): String = String.format(locale, "%d", value)

/** One number of a numbered list, right-aligned in the column's width. */
@Composable
internal fun NumberCell(
    text: String,
    width: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(width),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

/** One surah: its number in the shared column, its name, and its own Arabic name. */
@Composable
internal fun SurahRow(surah: Surah, numberWidth: Dp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberCell(
            text = numberLabel(surah.number),
            width = numberWidth,
            // The number is read, so it takes the theme's own secondary tone
            // and not a quiet alpha: onSurfaceVariant measures 6.1:1 and up on
            // every ground, where the 0.7 alpha it wore measured 3.3:1 (D-084).
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = surah.nameSimple,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.surah_meta_place_ayahs,
                    placeName(surah.revelationPlace),
                    surah.versesCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = surah.nameArabic,
            style = TextStyle(
                fontFamily = Amiri,
                fontSize = 23.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

/** One juz: its number in the shared column, its name, and where it begins. */
@Composable
internal fun JuzRow(
    juz: Int,
    numberWidth: Dp,
    reference: String,
    surahName: String,
    onJuz: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onJuz)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberCell(
            text = numberLabel(juz),
            width = numberWidth,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.juz_title, juz),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (reference.isEmpty()) {
                    ""
                } else {
                    stringResource(R.string.juz_starts_at, surahName, reference)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
