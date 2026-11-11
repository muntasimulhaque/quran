package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.WordRoot
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.SheetPlace
import io.github.muntasimulhaque.quran.ui.kit.SheetTitle
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.GlyphButton
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.prose

/**
 * A word the reader pressed and held, with the meaning the word list gives it
 * and the reference that says where it stands.
 *
 * The sheet is the footnote's container and the word aid's own page. Its
 * head is the footnote's head: the app's one sheet title at 20 SemiBold and
 * the place at 13, both on the page's 20 dp edge. Under it the card is
 * centered: one Arabic word and one short gloss are one entry, not a
 * paragraph. The word list's meanings are short (median nine characters, 99
 * percent under thirty), so the gloss is one line, and the rare longest is
 * two; the centered stack reads as one entry. The head takes the left edge
 * because every sheet in the app names itself there (owner decision, this
 * session).
 *
 * The word is the Quran's own word: the ayah's 26 sp on its 2.0 line, the
 * page ink, and the tajweed colors when the reader has them on. The meaning
 * is the reader's language in the translation's voice and size, 16 sp on the
 * reading's own line. The place is the app's own meta line under the sheet's
 * title. The sheet is not a second reading of the ayah; it holds the one word
 * the reader asked for and the place it holds (owner decision, this
 * session).
 *
 * The sheet names itself where every sheet does: [SheetTitle]'s 20 at
 * SemiBold, then the place in the same 13 meta line the footnote sheet
 * wears. The word remains the subject under both (owner decision, this
 * session).
 */
internal data class OpenWord(
    val word: String,
    val meaning: String,
    /** The ayah's tajweed code, cut to this word's own characters. */
    val tajweed: String,
    /** The ayah's key, for example 1:1, in the app's own digits. */
    val reference: String,
    /** The word's triliteral root; null for the third of the words with none. */
    val root: WordRoot? = null,
    /** The ayah the word stands in, for the voice that says it. */
    val ayahNumber: Int = 0,
    /** The word's position in its ayah, which the timings name. */
    val position: Int = 0,
    /** True when the surah's audio is on the device, so the mark can answer. */
    val canHear: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun WordSheet(
    word: OpenWord,
    surahName: String,
    metrics: ReadingMetrics,
    hafs: FontFamily,
    onDismiss: () -> Unit,
    /** The word's own voice, when the audio for it is on the device. */
    onHear: (() -> Unit)? = null,
) {
    val sheetState = rememberExpandedSheetState()
    val palette = LocalPagePalette.current
    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        name = stringResource(R.string.reading_word_sheet_name),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The gate rides with the scroll, so a long meaning never
                // bounces the sheet on its top anchor.
                .sheetVerticalScroll(rememberScrollState())
                .padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.SheetFoot)
                .testTag("word-sheet"),
        ) {
            // The sheet names itself where every sheet does, and the one
            // action it owns stands at the head's trailing edge: the title's
            // line is left aligned, so the room beside it is the only room on
            // the sheet that is not the entry's, and a reader looking for
            // "hear it" looks where every other surface in the app keeps its
            // own control. The mark and the title share one center, and the
            // place stays under both on the page's own edge, so the head
            // reads as one line with a door at its end (owner decision, this
            // session).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Space.Block),
                ) {
                    SheetTitle(
                        text = stringResource(R.string.reading_word_sheet_name),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (onHear != null) {
                    GlyphButton(
                        description = stringResource(R.string.reading_hear_word),
                        onClick = onHear,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("word-sheet-hear"),
                    ) {
                        IconGlyph(
                            icon = Icon.Speaker,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(GlyphSize.Control),
                        )
                    }
                }
            }
            SheetPlace(
                text = stringResource(
                    R.string.reading_sheet_where,
                    surahName,
                    localizedDigits(word.reference),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Tight),
            )
            // The word is the one subject on the sheet. It is drawn from the
            // page's palette so the reader meets the same word and the same
            // rule colors they pressed and held, at the ayah's own size, and
            // it speaks the portable form rather than the print's encoding
            // (owner decision, this session).
            TajweedText(
                text = AnnotatedString(word.word),
                code = word.tajweed,
                colors = palette.tajweed,
                enabled = metrics.tajweed,
                style = arabicStyle(hafs, metrics.arabicSp, metrics.arabicLineSp, palette.ink),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Block)
                    .semantics {
                        contentDescription = Arabic.portableText(word.word)
                    },
            )
            Text(
                text = word.meaning,
                style = LocalReadingVoice.current.prose(
                    metrics.translationSp,
                    metrics.translationLineSp,
                ),
                color = palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Tight),
            )
            // The root, when the word has one: the letters the page writes,
            // and the same letters read out for a reader who does not read
            // them. A bare line, not a door: it answers no tap (owner
            // decision, this session).
            //
            // It wraps rather than overflows: three words in a row are one
            // line at the app's own size and two at the largest system scale,
            // and the sheet is the one surface a reader may reach at 200
            // percent. A row would have clipped the letters at the right edge
            // instead of folding them (owner decision, this session).
            word.root?.let { root ->
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.Block)
                        .testTag("word-sheet-root"),
                    horizontalArrangement = Arrangement.spacedBy(
                        Space.Line,
                        Alignment.CenterHorizontally,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.Tight),
                ) {
                    Text(
                        text = stringResource(R.string.reading_word_root),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    Text(
                        text = root.arabic,
                        // The root is a fragment, not a verse: the medallion's
                        // own measure, which is the size the reading already
                        // draws Arabic below the Quran's line (owner
                        // decision, this session).
                        style = arabicStyle(
                            hafs,
                            TextSize.medallionAt(metrics.arabicSp),
                            TextSize.medallionAt(metrics.arabicSp) * 2f,
                            palette.ink,
                        ),
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    // The Latin rendering belongs to the page that is read in
                    // Latin script: a Bangla reader reads the root letters or
                    // nothing, and a line of Latin on a Bangla page is a third
                    // script the sheet never promised (owner decision, this
                    // session).
                    if (root.latin.isNotBlank() && metrics.readingLanguage == "en") {
                        Text(
                            text = root.latin,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }
    }
}
