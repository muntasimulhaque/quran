package io.github.muntasimulhaque.quran.ui.reading

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.WordGrammar
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.data.WordAudioState
import io.github.muntasimulhaque.quran.data.WordPlace
import io.github.muntasimulhaque.quran.data.WordRoot
import io.github.muntasimulhaque.quran.data.WordVoice
import io.github.muntasimulhaque.quran.data.wordPlaceGroups
import io.github.muntasimulhaque.quran.data.wordPlaceOpenIndex
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.AppChevron
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.ChevronRole
import io.github.muntasimulhaque.quran.ui.kit.GlyphButton
import io.github.muntasimulhaque.quran.ui.kit.PageHead
import io.github.muntasimulhaque.quran.ui.kit.ReferenceLabel
import io.github.muntasimulhaque.quran.ui.kit.SectionHead
import io.github.muntasimulhaque.quran.ui.kit.SheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.SheetHead
import io.github.muntasimulhaque.quran.ui.kit.SheetPlace
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.SheetTitle
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.kit.sheetDragGate
import io.github.muntasimulhaque.quran.ui.kit.sheetHeadActionInset
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.ControlMetrics
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.prose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * the air under its name. The scroll opens with [sheetHeadActionInset], the
 * room that reach needs above the line, so the top edge never clips the
 * target; the place keeps its 4 dp under the name.
 *
 * The speaker is quiet at rest and wears the accent while the word sounds,
 * which is the rule every door that turns in this app follows: the mark is
 * lit because something is happening, not because it is a control (owner
 * decision, this session).
 *
 * **Every word offers its voice, and the tap is the ask** (owner decision,
 * this session). The mark used to be drawn only for a word whose file was
 * already on the device, so a surah whose voices were still on their way drew
 * a sheet with no speaker at all, which reads as a broken sheet rather than a
 * word waiting for its pack. The speaker now stands for every word: when the
 * file is here it sounds, and when the surah's pack is not the tap fetches it
 * without a question, because a press on a speaker is approval enough. The
 * line under the gloss answers the press where the finger still is: the
 * transfer and its two sizes while the pack arrives, the word's own silence
 * when the aligner cut it no clip, and a transfer that did not arrive. It
 * stands centered under the gloss because the entry above it is the sheet's
 * one subject and the answer belongs to the word that was pressed. The
 * speaker is quiet while the pack is on its way: nothing is sounding yet.
 *
 * **The entry is centered; the facts are not** (owner decision, this
 * session). The word the reader pressed and the one gloss this ayah gives it
 * are one subject, and a subject is centered as the reading centers a single
 * display line: the Quran's own 26 sp on its 2.0 line, trimmed to the line's
 * ink, the page ink and the tajweed colors the reader has, then the gloss in
 * the reader's language at the translation's 16 sp, 8 dp under it.
 * [Space.Section] opens every fact, because a fact is a block: its own head,
 * its own value, its own count. It was a Section for the first and a
 * [Space.Block] for the rest, and the owner's frame showed the failure: the
 * count of one fact and the label of the next stood apart by almost the same
 * air as a label and its own value, so the eye could not read where one fact
 * ended and the next began (owner decision, this session). The entry above
 * them is the block before the first. They are the app's facts about the
 * word, so they take the app's own shape: the page's left edge, the label at
 * [SectionHead]'s 12, the value 4 dp under it,
 * and a count where the pack carries one, 4 dp under the value. The sheet had
 * them as sentences, each label inline with a 24 sp Arabic run and a count, so
 * the root outshouted the gloss and at the largest text step a label could
 * orphan on its own line (owner report, this session).
 *
 * **A fact's Arabic is 20 of the Quran's 26** (owner decision, this session).
 * The word is the reading's own size and the gloss the translation's; a fact
 * is reference material and may outrank neither. It stood at 24, which is the
 * print's medallion and the reading's inline stand, a measure for Arabic
 * *inside* a verse's line and not for a fact under a gloss. Twenty is the
 * inline stand less a step, and it is the size a 13 sp value would carry with
 * Arabic half again as tall as the prose beside it. The fact's own line is
 * trimmed at both ends for the same reason: a word, a root, and a lemma are
 * single-line facts, not a page of verses, so the sheet's air is the `Space`
 * around them and never the reading's 2.0 leading. The root's Latin rendering
 * sits on the Arabic's baseline, and the grammar phrase is a value at the
 * 13 sp meta voice, so the facts read as one list.
 *
 * Both halves of that line declare the baseline, because a flow row aligns
 * only the items that ask for it: with the Latin alone the letters kept the
 * line's top and the reading floated above them, a line clear of the letters
 * it reads, and the Root fact read as four lines rather than three (owner
 * report, this session). The root's own air is unchanged either way: the fact
 * is the Arabic's line box, and the Latin rides inside it.
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
 * **The count is a door, and the places are the sheet's second page** (owner
 * decision, this session). A count that names twenty words and cannot show
 * them is a promise the sheet does not keep. The count line is now the row
 * itself: the number at the page's edge, the app's one forward chevron at the
 * row's end, and the row's card under the finger, because a fact is bare type
 * and anything that answers a touch is a shape the finger can see. The line
 * opens a page inside this sheet, never a second sheet stacked on it: the
 * settings sheet already steps from a hub to a page and back, and that is the
 * app's grammar for a page inside a sheet. The page names the fact it came
 * from, draws the fact's value, and lists the places in a lazy list grouped by
 * surah, the Book's own map of where a root or a form lives. Every row is the
 * reference and the word as the page writes it, and nothing else: the gloss
 * belongs to the word sheet the reader left, and a list of thousands of
 * meanings is a word-by-word reading rather than a place list.
 *
 * The reader arrived from one of those places, and the list opens at it: the
 * current row wears the app's quiet selected wash and the accent reference, and
 * says it is the reader's place to a screen reader. The list is lazy and
 * wears the sheet's drag gate, so scrolling a root of two thousand words back
 * to its top never pulls the sheet down. A tap on a place puts this sheet
 * away, moves the reading to that ayah, and washes that word with the same
 * reveal a search arrival uses, so the place the reader chose is the place
 * their eye lands on. The page is never a dead end: its back mark, and the
 * phone's Back, return to the word, and an old root pack without the index
 * answers the same rows through the scan it always did.
 *
 * **While the word sounds, the word is washed** (owner decision, an earlier
 * session). One word is a sample, not a session: it is one word file on its
 * own player, so it raises no capsule and leaves no notification behind, and
 * the sheet gives the tap its answer where the finger is. A recitation that
 * was already playing waits at the word and is given back when the word ends,
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
    /** The surah the word stands in: the pack the speaker asks for. */
    val surah: Int = 0,
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

/** The word sheet's two pages: the word, and the places one of its facts owns. */
private sealed interface WordSheetPage {
    data object Word : WordSheetPage
    data class Places(val subject: PlacesSubject) : WordSheetPage
}

/** The fact whose places the page lists. */
private sealed interface PlacesSubject {
    data class Root(val arabic: String, val latin: String) : PlacesSubject
    data class Lemma(val lemma: String) : PlacesSubject
}

/** What the places read has to say: nothing yet, a failure, or the list. */
private sealed interface PlacesRead {
    data object Loading : PlacesRead
    data object Failed : PlacesRead
    data class Loaded(val places: List<WordPlace>) : PlacesRead
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun WordSheet(
    word: OpenWord,
    surahName: String,
    /** The surah's own name at a reference, for the places page's groups. */
    surahNameOf: (Int) -> String,
    metrics: ReadingMetrics,
    hafs: FontFamily,
    /** The library the places are read from, on the door's tap. */
    content: ContentDatabase,
    onDismiss: () -> Unit,
    /** A place the reader chose: the reading jumps there with its word washed. */
    onOpenPlace: (ayahNumber: Int, position: Int) -> Unit = { _, _ -> },
    /** The word's own voice, which every word offers: the tap is the ask. */
    onHear: () -> Unit = {},
    /** True while this word's own voice is sounding. */
    hearing: Boolean = false,
    /** What the tap has to say, and what the pack it asks for weighs. */
    voice: WordVoice = WordVoice.Ready,
    voiceAudio: WordAudioState? = null,
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
    // The pack this word's voice travels in, when the state is about this
    // surah: a state left over from the surah the reader just left says
    // nothing about this one.
    val pack = voiceAudio?.takeIf { it.surah == word.surah }
    // What the press has to answer with. The transfer's own sizes come from
    // the manifest and the bytes already here, so the line counts up under
    // the finger (owner decision, this session).
    val voiceLine: String? = when {
        voice == WordVoice.Missing -> stringResource(R.string.reading_word_voice_missing)
        voice == WordVoice.Failed -> stringResource(R.string.reading_word_voice_failed)
        voice != WordVoice.Preparing -> null
        pack?.failed == true -> stringResource(R.string.reading_word_voice_failed)
        // The sizes are drawn from the first byte read, so the line never
        // promises a weight before the transfer has one to count.
        pack != null && pack.bytes > 0 && pack.read > 0 -> stringResource(
            R.string.reading_word_voice_waiting,
            formatBytes(pack.read),
            formatBytes(pack.bytes),
        )
        else -> stringResource(R.string.reading_word_voice_asking)
    }
    // The counting line counts in the ear too if it is a live region, so it
    // is not one; the two sentences a reader acts on are (owner decision,
    // this session, accessibility).
    val counting = voice == WordVoice.Preparing && pack?.failed != true
    // The head's one action. Every word offers its voice, so the sheet always
    // owes the mark the room its target reaches above the name.
    val action: @Composable () -> Unit = {
        GlyphButton(
            description = stringResource(R.string.reading_hear_word),
            onClick = onHear,
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
    // The action's 48 dp target reaches above the head's 26 dp line. The
    // scroll's own top edge would clip it; this inset is the room it owes
    // the target, and it follows the reader's type scale (owner decision,
    // this session).
    val headInset = sheetHeadActionInset()
    // The sheet's page is the word until a count opens the places. It is
    // remembered against the word, so a new word never opens on the last
    // word's places (owner decision, this session).
    var page by remember(word) { mutableStateOf<WordSheetPage>(WordSheetPage.Word) }
    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        name = stringResource(R.string.reading_word_sheet_name),
    ) {
        // The page's own back steps out of the places and the sheet's back
        // stays the sheet's: the two are different rooms, and the reader
        // whose last act was opening a list must not be thrown out of the
        // word they were reading (owner decision, this session).
        BackHandler(enabled = page is WordSheetPage.Places) {
            page = WordSheetPage.Word
        }
        when (val shown = page) {
            WordSheetPage.Word -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // The gate rides with the scroll, so a long entry never
                    // bounces the sheet on its top anchor.
                    .sheetVerticalScroll(rememberScrollState())
                    .padding(top = headInset, bottom = Space.SheetFoot)
                    .testTag("word-sheet"),
            ) {
                // The head and the entry keep the page's 20 dp edge; the
                // count rows below are full-width rows, whose cards reach
                // [SheetRowGutter] outside that edge and whose words stand
                // on it (owner decision, this session).
                Box(Modifier.padding(horizontal = Space.Gutter)) {
                    SheetHead(
                        text = stringResource(R.string.reading_word_sheet_name),
                        action = action,
                    )
                }
                SheetPlace(
                    text = stringResource(
                        R.string.reading_sheet_where,
                        surahName,
                        localizedDigits(word.reference),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.Gutter)
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
                    style = arabicStyle(
                        hafs,
                        metrics.arabicSp,
                        metrics.arabicLineSp,
                        ink,
                        trim = LineHeightStyle.Trim.Both,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    onTextLayout = { wordLayout = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.Gutter)
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
                        .padding(horizontal = Space.Gutter)
                        .padding(top = Space.Line),
                )
                // The answer to the speaker's press, under the word it is about.
                voiceLine?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall,
                        color = quiet,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.Gutter)
                            .padding(top = Space.Line)
                            .testTag("word-sheet-voice")
                            .semantics {
                                if (!counting) liveRegion = LiveRegionMode.Polite
                            },
                    )
                }
                // Every fact opens with a section's air: a fact is a block, and
                // the entry above them is the block before the first.
                // The root, when the word has one: the letters the page writes,
                // their reading on the page that is read in Latin script, and a
                // door to every place the root stands in. The count that used
                // to be a bare line is the door's own label (owner decision,
                // this session).
                word.root?.let { root ->
                    Fact(
                        label = stringResource(R.string.reading_word_root),
                        tag = "word-sheet-root",
                        door = if (root.wordsCount > 0) {
                            {
                                CountDoor(
                                    text = pluralStringResource(
                                        R.plurals.reading_word_root_count,
                                        root.wordsCount,
                                        localizedDigits(root.wordsCount.toString()),
                                    ),
                                    tag = "word-sheet-root-door",
                                    onClick = {
                                        page = WordSheetPage.Places(
                                            PlacesSubject.Root(root.arabic, root.latin),
                                        )
                                    },
                                )
                            }
                        } else {
                            null
                        },
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
                                style = arabicStyle(
                                    hafs,
                                    factSp,
                                    factSp * 2f,
                                    ink,
                                    trim = LineHeightStyle.Trim.Both,
                                ),
                                // The letters declare the baseline too: a flow row
                                // aligns only the items that ask, and with the
                                // Latin alone the letters kept the line's top
                                // (owner report, this session).
                                modifier = Modifier
                                    .testTag("word-sheet-root-arabic")
                                    .alignByBaseline(),
                            )
                            if (root.latin.isNotBlank() && metrics.readingLanguage == "en") {
                                Text(
                                    text = root.latin,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = quiet,
                                    modifier = Modifier
                                        .testTag("word-sheet-root-latin")
                                        .alignByBaseline(),
                                )
                            }
                        }
                    }
                }
                word.grammar?.let { grammar ->
                    // The dictionary form, when the corpus gives one: the word's
                    // own letters and the Book's two counts for it, and the
                    // count line is the door to the places it stands in. A
                    // pronoun and a disjoint letter have no lemma, and the sheet
                    // then says nothing rather than drawing a line with nothing
                    // behind it (owner decision, this session).
                    grammar.lemma?.let { lemma ->
                        Fact(
                            label = stringResource(R.string.reading_word_lemma),
                            tag = "word-sheet-lemma",
                            door = {
                                val words = pluralStringResource(
                                    R.plurals.reading_word_lemma_words,
                                    grammar.lemmaWords,
                                    localizedDigits(grammar.lemmaWords.toString()),
                                )
                                val forms = pluralStringResource(
                                    R.plurals.reading_word_lemma_forms,
                                    grammar.lemmaForms,
                                    localizedDigits(grammar.lemmaForms.toString()),
                                )
                                CountDoor(
                                    text = stringResource(
                                        R.string.reading_word_lemma_count,
                                        words,
                                        forms,
                                    ),
                                    tag = "word-sheet-lemma-door",
                                    onClick = {
                                        page = WordSheetPage.Places(PlacesSubject.Lemma(lemma))
                                    },
                                )
                            },
                        ) {
                            Text(
                                text = lemma,
                                style = arabicStyle(
                                    hafs,
                                    factSp,
                                    factSp * 2f,
                                    ink,
                                    trim = LineHeightStyle.Trim.Both,
                                ),
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
                                style = MaterialTheme.typography.bodySmall,
                                color = ink,
                            )
                        }
                    }
                }
            }
            is WordSheetPage.Places -> PlacesPage(
                subject = shown.subject,
                currentAyah = word.ayahNumber,
                currentPosition = word.position,
                metrics = metrics,
                hafs = hafs,
                factSp = factSp,
                content = content,
                surahNameOf = surahNameOf,
                onBack = { page = WordSheetPage.Word },
                onOpenPlace = { ayah, position ->
                    // The reader's choice is the reading's next place: the
                    // sheet goes first, so the word's own voice and the
                    // sheet's state are put away before the page moves.
                    onDismiss()
                    onOpenPlace(ayah, position)
                },
            )
        }
    }
}

/**
 * The count line that opens a fact's places.
 *
 * The count is the fact's own number and the row's own label; the app's one
 * forward chevron says it opens a page rather than turning in place. The row
 * owns the 48 dp target and the card under the finger, and its words stand on
 * the page's 20 dp edge while the card reaches [SheetRowGutter] outside it,
 * the same row every list in a sheet wears (owner decision, this session).
 */
@Composable
private fun CountDoor(
    text: String,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(
                start = Space.Gutter - SheetRowGutter,
                end = Space.Row - SheetRowGutter,
                top = Space.Line,
                bottom = Space.Line,
            )
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        AppChevron(
            role = ChevronRole.Next,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = ControlMetrics.ChevronGap),
        )
    }
}

/**
 * The places of one fact, in the sheet's own window: the page's head, the
 * fact's value, and the lazy list of where it stands.
 *
 * The read starts when the door is pressed, never when the sheet opens: a
 * long press is the reader asking what a word means, and the thousand places
 * of a common root are a second question (owner decision, this session). It
 * runs on a worker, and the page's head and value stand while the list is
 * still on its way. A failed read or an empty one says so in a sentence; the
 * reader's library is local and a failure is a broken pack, not a network
 * that might come back, so the failure offers one quiet Try again and the
 * empty line offers none (owner decision, this session).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlacesPage(
    subject: PlacesSubject,
    currentAyah: Int,
    currentPosition: Int,
    metrics: ReadingMetrics,
    hafs: FontFamily,
    factSp: Float,
    content: ContentDatabase,
    surahNameOf: (Int) -> String,
    onBack: () -> Unit,
    onOpenPlace: (ayahNumber: Int, position: Int) -> Unit,
) {
    var attempt by remember(subject) { mutableIntStateOf(0) }
    val read by produceState<PlacesRead>(
        initialValue = PlacesRead.Loading,
        subject,
        content,
        metrics.readingLanguage,
        attempt,
    ) {
        value = PlacesRead.Loading
        val found = withContext(Dispatchers.IO) {
            runCatching {
                when (subject) {
                    is PlacesSubject.Root -> content.wordRootPlaces(subject.arabic)
                    is PlacesSubject.Lemma -> content.wordLemmaPlaces(
                        subject.lemma,
                        metrics.readingLanguage,
                    )
                }
            }
        }
        value = found.fold(
            onSuccess = { PlacesRead.Loaded(it) },
            onFailure = { PlacesRead.Failed },
        )
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("word-sheet-places"),
        ) {
            PageHead(
                onBack = onBack,
                backDescription = stringResource(R.string.reading_word_places_back),
            ) {
                SheetTitle(
                    text = when (subject) {
                        is PlacesSubject.Root -> stringResource(R.string.reading_word_root)
                        is PlacesSubject.Lemma -> stringResource(R.string.reading_word_lemma)
                    },
                    modifier = Modifier.padding(start = Space.Tight).weight(1f),
                )
            }
            PlacesEntry(subject = subject, metrics = metrics, hafs = hafs)
            when (val places = read) {
                PlacesRead.Loading -> Spacer(Modifier.weight(1f))
                PlacesRead.Failed -> PlacesNote(
                    text = stringResource(R.string.reading_word_places_failed),
                    retry = stringResource(R.string.reading_word_places_retry),
                    onRetry = { attempt++ },
                    modifier = Modifier.weight(1f),
                )
                is PlacesRead.Loaded -> if (places.places.isEmpty()) {
                    PlacesNote(
                        text = stringResource(R.string.reading_word_places_empty),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    PlacesList(
                        places = places.places,
                        currentAyah = currentAyah,
                        currentPosition = currentPosition,
                        factSp = factSp,
                        hafs = hafs,
                        surahNameOf = surahNameOf,
                        onOpenPlace = onOpenPlace,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The page's subject: the root with its reading on a Latin page, or the
 * dictionary form, at the reading's own size, centered as the entry it came
 * from was (owner decision, this session).
 *
 * It is the value the page's title names, not a second heading: the root's
 * letters and the Latin ride one baseline, as the fact's did, and the lemma is
 * one centered line. The count is not repeated here; the count is the door
 * the reader just pressed, and the page's groups count their own places.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlacesEntry(
    subject: PlacesSubject,
    metrics: ReadingMetrics,
    hafs: FontFamily,
) {
    val ink = LocalPagePalette.current.ink
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.Gutter)
            .padding(top = Space.Block),
    ) {
        when (subject) {
            is PlacesSubject.Root -> FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    Space.Line,
                    Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.Tight),
            ) {
                Text(
                    text = subject.arabic,
                    style = arabicStyle(
                        hafs,
                        metrics.arabicSp,
                        metrics.arabicSp * 2f,
                        ink,
                        trim = LineHeightStyle.Trim.Both,
                    ),
                    modifier = Modifier
                        .testTag("word-sheet-places-root")
                        .alignByBaseline(),
                )
                if (subject.latin.isNotBlank() && metrics.readingLanguage == "en") {
                    Text(
                        text = subject.latin,
                        style = MaterialTheme.typography.bodySmall,
                        color = quiet,
                        modifier = Modifier
                            .testTag("word-sheet-places-latin")
                            .alignByBaseline(),
                    )
                }
            }
            is PlacesSubject.Lemma -> Text(
                text = subject.lemma,
                style = arabicStyle(
                    hafs,
                    metrics.arabicSp,
                    metrics.arabicSp * 2f,
                    ink,
                    trim = LineHeightStyle.Trim.Both,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("word-sheet-places-lemma"),
            )
        }
    }
}

/**
 * A sentence where the list would stand: a failed read has one answer, and an
 * empty one has nothing to offer but the truth (owner decision, this
 * session).
 */
@Composable
private fun PlacesNote(
    text: String,
    modifier: Modifier = Modifier,
    retry: String? = null,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.Gutter)
            .padding(top = Space.Section),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (retry != null) {
            TextButton(
                label = retry,
                onClick = onRetry,
                modifier = Modifier.padding(top = Space.Block),
            )
        }
    }
}

/**
 * The places, grouped by surah, opened at the reader's own place.
 *
 * The list is lazy because a common root stands in thousands of places, and
 * the gate is the one every scrolling sheet page wears: a drag that reaches
 * this list's top belongs to the list, not to the sheet (owner decision, this
 * session). The initial item is the head of the surah the reader's place
 * stands in, so the list opens under the group's name with the reader's own
 * row marked one line below it, rather than on the row alone with its surah
 * scrolled away.
 */
@Composable
private fun PlacesList(
    places: List<WordPlace>,
    currentAyah: Int,
    currentPosition: Int,
    factSp: Float,
    hafs: FontFamily,
    surahNameOf: (Int) -> String,
    onOpenPlace: (ayahNumber: Int, position: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(places) { wordPlaceGroups(places) }
    val open = remember(groups) {
        wordPlaceOpenIndex(groups, currentAyah, currentPosition)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = open)
    val gate = remember(listState) { SheetDragGate(listState) }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .sheetDragGate(gate)
            .testTag("word-sheet-places-list"),
        contentPadding = PaddingValues(bottom = Space.SheetFoot),
    ) {
        groups.forEach { group ->
            item(key = "surah-${group.surah}") {
                SurahHead(
                    surah = group.surah,
                    name = surahNameOf(group.surah),
                    count = group.places.size,
                )
            }
            items(
                items = group.places,
                key = { "place-${it.ayahNumber}-${it.position}" },
            ) { place ->
                PlaceRow(
                    place = place,
                    current = place.ayahNumber == currentAyah &&
                        place.position == currentPosition,
                    factSp = factSp,
                    hafs = hafs,
                    onOpenPlace = onOpenPlace,
                )
            }
        }
    }
}

/** One surah's head: its name and how many of the places stand in it. */
@Composable
private fun SurahHead(surah: Int, name: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Space.Gutter,
                end = Space.Gutter,
                top = Space.Section,
                bottom = Space.Tight,
            )
            .testTag("word-sheet-surah-$surah"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHead(text = name, modifier = Modifier.weight(1f))
        Text(
            text = pluralStringResource(
                R.plurals.reading_word_places_count,
                count,
                localizedDigits(count.toString()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One place: the reference and the word as the page writes it, and nothing
 * else.
 *
 * The word is the study content of the row: the root's letters wear many
 * shapes across the Book, and the dictionary form's forms are the words
 * themselves, so the row shows the reader the word they will meet when the
 * row is pressed. The current place wears the app's quiet selected wash and
 * the accent reference, and says it out loud; the tap still answers, and a
 * tap on the reader's own place returns the reading to it (owner decision,
 * this session).
 */
@Composable
private fun PlaceRow(
    place: WordPlace,
    current: Boolean,
    factSp: Float,
    hafs: FontFamily,
    onOpenPlace: (ayahNumber: Int, position: Int) -> Unit,
) {
    val palette = LocalPagePalette.current
    val ink = palette.ink
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val currentState = stringResource(R.string.reading_word_places_current)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = { onOpenPlace(place.ayahNumber, place.position) })
            .then(
                if (current) {
                    Modifier.background(primary.copy(alpha = Wash.Selected))
                } else {
                    Modifier
                },
            )
            .minimumInteractiveComponentSize()
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row)
            .semantics { if (current) stateDescription = currentState }
            .testTag("word-sheet-place-${place.ayahNumber}-${place.position}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReferenceLabel(
            text = localizedDigits(place.reference),
            color = if (current) primary else quiet,
        )
        Text(
            text = place.word,
            style = arabicStyle(
                hafs,
                factSp,
                factSp * 2f,
                ink,
                trim = LineHeightStyle.Trim.Both,
            ),
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier
                .weight(1f)
                .padding(start = Space.Block)
                .semantics {
                    contentDescription = Arabic.portableText(place.word)
                },
        )
    }
}

/**
 * One fact about the word: its label on the page's edge, the value 4 dp under
 * it, and the count's door where the pack carries one (owner decision, this
 * session).
 *
 * The label is [SectionHead]'s voice, so it is a heading a screen reader can
 * jump to and it steps back from the value it names. [Space.Section] opens
 * every fact, the first and the rest alike: a fact has its own head, so it is
 * a block and it stands a block's break from the one before it, never the 16
 * that separates two thoughts inside one block (owner decision, this
 * session). The label carries a tag of its own beside the fact's, so a test
 * can measure the room the fact opens with against the fact itself.
 *
 * The value keeps the page's 20 dp edge; the door is a full-width row, so its
 * card may reach outside that edge and its words still stand on it. The fact
 * is the same block either way: the door is the last line of the fact, not a
 * block after it.
 */
@Composable
private fun Fact(
    label: String,
    tag: String,
    door: (@Composable () -> Unit)? = null,
    value: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // The tag rides outside the room the fact opens with, so the
            // fact's own box carries it and a test can measure the label
            // against it.
            .testTag(tag)
            .padding(top = Space.Section),
    ) {
        Column(Modifier.padding(horizontal = Space.Gutter)) {
            SectionHead(
                text = label,
                modifier = Modifier.testTag("$tag-label"),
            )
            Column(
                modifier = Modifier.padding(top = Space.Tight),
                content = value,
            )
        }
        door?.invoke()
    }
}
