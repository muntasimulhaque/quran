package io.github.muntasimulhaque.quran.ui.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.Footnote
import io.github.muntasimulhaque.quran.feature.study.R
import io.github.muntasimulhaque.quran.ui.theme.Space

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
 * study reading and the ayah card both open this same sheet, so a translator's
 * note reads the same wherever it was reached from, and no surface prints its
 * footnotes at the foot of the page as a block nobody asked for.
 *
 * The note is set at the size of the text it belongs to, passed in by the
 * caller, so the translation's notes follow the translation size setting and
 * a tafsir's notes will follow the tafsir size the day a tafsir carries one.
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 34.dp),
        ) {
            Text(
                text = stringResource(R.string.study_footnote_title, footnote.number, surahName, reference),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = footnote.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = sizeSp.sp,
                    lineHeight = lineSp.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Space.Block),
            )
        }
    }
}
