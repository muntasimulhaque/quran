package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.Footnote
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.prose
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll

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
    val sheetState = rememberExpandedSheetState()
    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                // The gate rides with the scroll, so an upward flick on a
                // short footnote never bounces the sheet on its top anchor.
                .sheetVerticalScroll(rememberScrollState())
                // A note is prose on the app's one page: its words stop at
                // Space.Gutter, the line the ayah it explains stands on, and
                // a reader who changes the size gets more lines and never a
                // narrower column (owner decision, this session).
                .padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.SheetFoot)
                .testTag("footnote-sheet"),
        ) {
            Text(
                text = stringResource(
                    R.string.reading_footnote_title,
                    footnote.number,
                    surahName,
                    localizedDigits(reference),
                ),
                style = MaterialTheme.typography.titleLarge,
                // The name of the sheet, in the app's one sheet-title voice:
                // the footnote's own reference is what names it, so a reader
                // who opened the wrong marker knows it before reading a line.
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )
            Text(
                text = footnote.text,
                style = LocalReadingVoice.current.prose(
                    sizeSp,
                    lineSp,
                    MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Block),
            )
        }
    }
}
