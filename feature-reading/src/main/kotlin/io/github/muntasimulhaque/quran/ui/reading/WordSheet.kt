package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.WordGrammar
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.WordRoot
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.GlyphButton
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.SheetHead
import io.github.muntasimulhaque.quran.ui.kit.SheetPlace
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
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
 * **The head is every sheet's head** (owner decision, this session). The name
 * at [SheetHead]'s 20 SemiBold, the place 4 dp under it in [SheetPlace]'s meta
 * line, both on the page's 20 dp edge: the footnote's own head, and the note's
 * and the settings page's, drawn by the same rule. It was not: the speaker's
 * 48 dp target made this head 48 dp tall under a 26 dp title, so the place
 * stood 15 dp under the name where the footnote's stood 4, and two sheets that
 * call themselves one page opened two different ways (owner report, this
 * session). [SheetHead] measures the head by its name and lets the mark's
 * target reach outside that line, so no sheet with an action pays for it in
 * the air under its name.
 *
 * The speaker is quiet at rest and wears the accent while the word sounds,
 * which is the rule every door that turns in this app follows: the mark is
 * lit because something is happening, not because it is a control (owner
 * decision, this session).
 *
 * **The entry is centered; the facts are not** (owner decision, this
 * session). The word the reader pressed and the one gloss this ayah gives it
 * are one subject, and a subject is centered as the reading centers a single
 * display line: the Quran's own 26 sp on its 2.0 line, the page ink and the
 * tajweed colors the reader has, then the gloss in the reader's language at
 * the translation's 16 sp, 4 dp under it. Under the entry, the study lines are
 * the app's facts about the word, so they take the app's own shape: the page's
 * left edge, the label at [SectionHead]'s 12, the value 4 dp under it, and a
 * count where the pack carries one, 4 dp under the value. The sheet had them
 * as sentences, each label inline with a 24 sp Arabic run and a count, so the
 * root outshouted the gloss and at the largest text step a label could orphan
 * on its own line (owner report, this session).
 *
 * **A fact's Arabic is 20 of the Quran's 26** (owner decision, this session).
 * The word is the reading's own size and the gloss the translation's; a fact
 * is reference material and may outrank neither. It stood at 24, which is the
 * print's medallion and the reading's inline stand, a measure for Arabic
 * *inside* a verse's line and not for a fact under a gloss. Twenty is the
 * inline stand less a step, and it is the size a 13 sp value would carry with
 * Arabic half again as tall as the prose beside it.
 *
 * The root's Latin rendering belongs to the page that is read in Latin script:
 * a Bangla reader reads the root letters or nothing, and a line of Latin on a
 * Bangla page is a third script the sheet never promised (owner decision, an
 * earlier session).
 *
 * The lines stand directly under the entry and are drawn only when the word
 * pack carries them, so a pack held from before draws the sheet it always
 * drew; a word the corpus gives no lemma says nothing rather than drawing an
 * empty line. The grammar's words are the app's own translated grammar
 * vocabulary, the same in both languages, and never a translation of scripture
 * (owner decision, this session).
 *
 * **While the word sounds, the word is washed** (owner decision, an earlier
 * session). One word is a sample, not a session: it is one clipped item on its
 * own player, so it raises no capsule and leaves no notification behind, and
 * the sheet gives the tap its answer where the finger is. A recitation that
 * was already playing waits at the word and is given back when the clip ends,
 * so the pill behind the sheet stands paused and then goes on. The word wears
 * the reading's own recited-word mark, the same rounded wash the verse uses,
 * and it rests when the word does.
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
    /**
     * The word's grammar and lemma, when the word pack carries them. Null on
     * a held pack from before the study lines, which then draws the word, its
     * meaning, and its root, as it did (owner decision, an earlier session).
     */
    val grammar: WordGrammar? = null,
    /** The ayah the word stands in, for the voice that says it. */
    val ayahNumber: Int = 0,
    /** The word's position in its ayah, which the timings name. */
    val position: Int = 0,
    /** True when the surah's audio is on the device, so the mark can answer. */
    val canHear: Boolean = false,
)

/**
 * A fact's Arabic as a share of the Quran's own size: 20 of 26 at the default.
 *
 * The word is the reading's 26 and the gloss the translation's 16; the facts
 * under them are reference material. The reading's inline stand is 24 of 26,
 * which is Arabic *inside* a verse's line; a fact takes one step further down
 * so it can outrank neither the word nor its gloss (owner decision, this
 * session).
 */
private const val FactArabicOfQuran = 20f / 26f

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
    /** True while this word's own voice is sounding. */
    hearing: Boolean = false,
) {
    val sheetState = rememberExpandedSheetState()
    val palette = LocalPagePalette.current
    // The word's shaped line, so the wash can sit on the letters rather than
    // on the width the sheet gives it (owner decision, an earlier session).
    var wordLayout by remember(word.word) { mutableStateOf<TextLayoutResult?>(null) }
    val ink = palette.ink
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    // A fact's own Arabic, read from the reader's step so the sheet follows
    // the size they chose for the Quran (owner decision, this session).
    val factSp = metrics.arabicSp * FactArabicOfQuran
    // The mark is quiet at rest and lit while the word sounds: a door says its
    // state in the accent, and at rest it asks for nothing (owner decision,
    // this session).
    val markTint = if (hearing) MaterialTheme.colorScheme.primary else quiet
    val hear = onHear
    // The head's one action, drawn only when the word's voice is on the
    // device. The explicit type keeps the lambda composable.
    val action: (@Composable () -> Unit)? = if (hear == null) {
        null
    } else {
        {
            GlyphButton(
                description = stringResource(R.string.reading_hear_word),
                onClick = hear,
                tint = markTint,
                modifier = Modifier.testTag("word-sheet-hear"),
            ) {
                IconGlyph(
                    icon = Icon.Speaker,
                    tint = markTint,
                    modifier = Modifier.size(GlyphSize.Control),
                )
            }
        }
    }
    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        name = stringResource(R.string.reading_word_sheet_name),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The gate rides with the scroll, so a long entry never
                // bounces the sheet on its top anchor.
                .sheetVerticalScroll(rememberScrollState())
                .padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.SheetFoot)
                .testTag("word-sheet"),
        ) {
            SheetHead(
                text = stringResource(R.string.reading_word_sheet_name),
                action = action,
            )
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
            // The entry is the sheet's one subject, so it is centered: the
            // word at the Quran's own size, drawn from the page's palette so
            // the reader meets the letters and the rule colors they pressed
            // and held, in the portable form rather than the print's encoding
            // (owner decision, this session).
            TajweedText(
                text = AnnotatedString(word.word),
                code = word.tajweed,
                colors = palette.tajweed,
                enabled = metrics.tajweed,
                style = arabicStyle(hafs, metrics.arabicSp, metrics.arabicLineSp, ink),
                textAlign = TextAlign.Center,
                maxLines = 1,
                onTextLayout = { wordLayout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Block)
                    .drawWithContent {
                        val layout = wordLayout
                        if (hearing && layout != null) {
                            drawWordWash(
                                layout,
                                layout.layoutInput.text.indices,
                                palette.highlight,
                            )
                        }
                        drawContent()
                    }
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
                color = ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Tight),
            )
            // The root, when the word has one: the letters the page writes,
            // their reading on the page that is read in Latin script, and how
            // much of the Book speaks that root. A bare fact, not a door: it
            // answers no tap (owner decision, an earlier session).
            word.root?.let { root ->
                Fact(
                    label = stringResource(R.string.reading_word_root),
                    tag = "word-sheet-root",
                ) {
                    // The letters wrap rather than clipping at the largest
                    // system scale, and the Latin rides their line where it
                    // fits, under them where it does not (owner decision, this
                    // session).
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.Line),
                        verticalArrangement = Arrangement.spacedBy(Space.Tight),
                    ) {
                        Text(
                            text = root.arabic,
                            style = arabicStyle(hafs, factSp, factSp * 2f, ink),
                        )
                        if (root.latin.isNotBlank() && metrics.readingLanguage == "en") {
                            Text(
                                text = root.latin,
                                style = MaterialTheme.typography.bodySmall,
                                color = quiet,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                    if (root.wordsCount > 0) {
                        Text(
                            text = stringResource(
                                R.string.reading_word_root_count,
                                localizedDigits(root.wordsCount.toString()),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = quiet,
                            modifier = Modifier.padding(top = Space.Tight),
                        )
                    }
                }
            }
            word.grammar?.let { grammar ->
                // The dictionary form, when the corpus gives one: the word's
                // own letters and the Book's two counts for it. A pronoun and
                // a disjoint letter have no lemma, and the sheet then says
                // nothing rather than drawing a line with nothing behind it
                // (owner decision, this session).
                grammar.lemma?.let { lemma ->
                    Fact(
                        label = stringResource(R.string.reading_word_lemma),
                        tag = "word-sheet-lemma",
                    ) {
                        Text(
                            text = lemma,
                            style = arabicStyle(hafs, factSp, factSp * 2f, ink),
                        )
                        Text(
                            text = stringResource(
                                R.string.reading_word_lemma_count,
                                localizedDigits(grammar.lemmaWords.toString()),
                                localizedDigits(grammar.lemmaForms.toString()),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = quiet,
                            modifier = Modifier.padding(top = Space.Tight),
                        )
                    }
                }
                // What the word is, as a grammar reads it. The line is drawn
                // only on a word pack that carries it, so an older pack is the
                // sheet the reader already knows (owner decision, an earlier
                // session).
                grammarPhrase(grammar)?.let { phrase ->
                    Fact(
                        label = stringResource(R.string.reading_word_grammar),
                        tag = "word-sheet-grammar",
                    ) {
                        Text(
                            text = phrase,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ink,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One fact about the word: its label on the page's edge, the value 4 dp under
 * it, and whatever the pack adds about the value (a count) 4 dp under that.
 *
 * The label is [SectionHead]'s voice, so it is a heading a screen reader can
 * jump to and it steps back from the value it names. [Space.Block] separates
 * one fact from the next, because two facts are two thoughts.
 */
@Composable
private fun Fact(
    label: String,
    tag: String,
    value: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.Block)
            .testTag(tag),
    ) {
        SectionHead(text = label)
        Column(
            modifier = Modifier.padding(top = Space.Tight),
            content = value,
        )
    }
}
