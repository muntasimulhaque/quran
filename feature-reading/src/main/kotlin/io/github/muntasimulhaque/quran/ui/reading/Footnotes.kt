package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.Footnote
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Reading
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.kit.SheetHandle
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.kit.rememberSheetWidth

/**
 * A footnote the reader opened, with the ayah it belongs to and the size of
 * the text it annotates. The size travels with the note because a footnote is
 * part of the sentence it explains: a translator's note is read at the
 * translation's size, and a tafsir's note at the tafsir's size, whatever the
 * reader chose for that kind of text.
 */
internal data class OpenFootnote(
    val note: Footnote,
    val reference: String,
    val sizeSp: Float,
    val lineSp: Float,
)

/**
 * The footnote of one ayah, opened from the marker the reader tapped. The
 * reading and the ayah card both open this same sheet, so a translator's
 * note reads the same wherever it was reached from, and no surface prints its
 * footnotes at the foot of the page as a block nobody asked for.
 *
 * The note is set at the size of the text it belongs to, passed in by the
 * caller, so the translation's notes follow the translation size setting and
 * a tafsir's notes will follow the tafsir size the day a tafsir carries one.
 * It speaks the reading's own voice with it: a translator's note is part of
 * the sentence it explains, drawn on the page that sentence is drawn on, so
 * it wears the face the translation wears rather than the interface's, the
 * same voice the reader's own note is written in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FootnoteSheet(
    footnote: Footnote,
    surahName: String,
    reference: String,
    sizeSp: Float,
    lineSp: Float,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // A note is prose, and a prose line has a measure: the footnote keeps its
    // own 65-character column, centered on the app's one page, so a reader
    // who changes the size gets more lines and never a longer one (owner
    // decision, the theme and consistency session; this session).
    val prose = Reading.proseMeasure(LocalReadingVoice.current, sizeSp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { SheetHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = rememberSheetWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                // The gate rides with the scroll, so an upward flick on a
                // short footnote never bounces the sheet on its top anchor.
                .sheetVerticalScroll(rememberScrollState())
                .padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.SheetFoot),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    R.string.reading_footnote_title,
                    footnote.number,
                    surahName,
                    localizedDigits(reference),
                ),
                style = MaterialTheme.typography.labelMedium,
                // The marker that opened this sheet keeps the action color;
                // the sheet's own name is a reference line, and every other
                // reference line on the reading wears the secondary tone
                // (owner decision, the theme and consistency session).
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .widthIn(max = prose)
                    .fillMaxWidth(),
            )
            Text(
                text = footnote.text,
                style = LocalReadingVoice.current.style.copy(
                    fontSize = sizeSp.sp,
                    lineHeight = lineSp.sp,
                    // The note takes the optical cut of the size it is
                    // drawn at, like every other prose surface; without it
                    // the default small-text cut was stretched over the
                    // reader's chosen size (owner decision, the theme and
                    // consistency session).
                    fontFamily = LocalReadingVoice.current.atSize(sizeSp),
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .widthIn(max = prose)
                    .fillMaxWidth()
                    .padding(top = Space.Block),
            )
        }
    }
}
