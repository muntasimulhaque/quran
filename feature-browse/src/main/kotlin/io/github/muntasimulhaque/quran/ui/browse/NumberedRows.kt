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

/**
 * The number column Browse's two numbered lists share, and the two rows that
 * wear it.
 *
 * Every number is measured into the width of the widest number its list can
 * hold and set flush to the right edge of that width, so 3, 10, and 114 end
 * their digits at one line: the0 of 10 starts where the3 of 3 stands, and
 * every name after the number starts at the same place whatever the number
 * is. A column of digits that starts at a different x on every row width is
 * two lists pretending to be one.
 *
 * The width is measured once per list with the reader's own font scale, so
 * the column grows with a large system text instead of clipping the number
 * that no longer fits.
 */
@Composable
internal fun rememberNumberWidth(widest: String): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    return remember(measurer, style, widest, density) {
        with(density) { measurer.measure(widest, style).size.width.toDp() }
    }
}

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
            text = surah.number.toString(),
            width = numberWidth,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
            text = juz.toString(),
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
