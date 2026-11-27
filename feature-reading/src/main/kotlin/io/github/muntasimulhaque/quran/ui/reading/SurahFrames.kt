package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.ReferenceLink
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.TextBlock
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.AppChevron
import io.github.muntasimulhaque.quran.ui.kit.ChevronRole
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.rich.RichBlocks
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.amiriDisplay
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.TypeLadder
import io.github.muntasimulhaque.quran.ui.theme.title
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash
import io.github.muntasimulhaque.quran.ui.theme.rememberHafs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The air the surah's arrival keeps at its own two ends: more above the
 * Arabic name than under the opening's last line, so the frame reads as the
 * head of the page and the verses below it keep the lesser gap. The last line
 * is the basmalah where the surah has one and the introduction's door where
 * it does not, so the frame ends at the same place either way (owner
 * decision, 23.6). [SurahEnd] holds the arithmetic between this frame, the
 * reading's foot, and the turn of a page.
 */
private val OpeningTop = 40.dp

/** The lesser end of the same frame, under the opening's last line. */
private val OpeningBottom = 14.dp

@Composable
internal fun SurahOpening(
    surah: Surah,
    content: ContentDatabase?,
    /** The reader's language: the introduction must speak it or not appear. */
    language: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    /** Opens an internal reference the introduction links to, when it does. */
    onOpenReference: (surah: Int, ayah: Int?) -> Unit = { _, _ -> },
    metrics: ReadingMetrics,
) {
    // The introduction is read as the surah opens, not when its door is
    // pressed, because the door is drawn only when there is something behind
    // it: a button that opens nothing is worse than no button. A language
    // whose content carries no introduction of its own is shown the English
    // one, because the introduction is the one place the app prefers a
    // language the reader may not read over saying nothing at all (owner
    // decision, 6.6). The translation, the tafsir, and the word
    // meanings keep the strict one-language rule.
    val info by produceState<List<TextBlock>?>(initialValue = null, surah.number, content, language) {
        value = withContext(Dispatchers.IO) {
            content?.surahInfo(surah.number, language)?.let { RichText.parseHtml(it) }
        }
    }
    // The basmalah is the content's own first ayah, read from the same
    // library the surah is read from, so the words at the head of the surah
    // are the words the audit verified. It is asked for only where it is
    // drawn: every surah but Al-Fatiha, whose first ayah it already is, and
    // At-Tawbah, which opens without it.
    val basmalah by produceState<Ayah?>(initialValue = null, surah.number, content) {
        value = if (surah.number != 1 && surah.bismillahPre) {
            withContext(Dispatchers.IO) { content?.basmalah() }
        } else {
            null
        }
    }
    val palette = LocalPagePalette.current
    val ornament = palette.ornament
    val ink = palette.ink
    val hafs = rememberHafs()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("surah-opening")
            .padding(top = OpeningTop, bottom = OpeningBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The opening of a surah is the one arrival in the reading: the
        // Arabic name in the ornament gold, then the name and place in quiet
        // type, so every surah opens the same way. The two names are one
        // header, so the Latin name stands a row's gap under the Arabic; the
        // gold rule that used to divide them was ornament, and it made the
        // arrival read as two separate lines (owner decision, 14.3).
        Text(
            text = surah.nameArabic,
            style = amiriDisplay(TypeLadder.OPENING_ARABIC_SP, ornament),
        )
        Text(
            text = surah.nameSimple,
            style = LocalReadingVoice.current.title(TypeLadder.OPENING_NAME_SP),
            color = MaterialTheme.colorScheme.onBackground,
            // The surah's own name is this page's title, so a reader who
            // cannot see it can jump to it instead of hearing the opening
            // read as one run (owner decision, 13.3).
            modifier = Modifier
                .padding(top = Space.Row)
                .semantics { heading() },
        )
        Text(
            text = stringResource(
                R.string.reading_surah_meta,
                if (surah.revelationPlace.equals("makkah", true)) {
                    stringResource(R.string.reading_revelation_makki)
                } else {
                    stringResource(R.string.reading_revelation_madani)
                },
                surah.versesCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Tight),
        )
        // The surah's head ends with the door of its introduction, and the
        // basmalah opens the text. The introduction is the prose of the name
        // and the place above it, so it stands with them: under the basmalah
        // it split the Quran's own two lines, and opened it stood between
        // them. Al-Fatiha and At-Tawbah, which open without a separate
        // basmalah, already kept the door in the head, so every surah now
        // reads the same way (owner decision, 23.6).
        //
        // The introduction is drawn only once the reader opens it: closed,
        // the door is the whole of what stands there, so the surah's own
        // first ayah keeps the page. The about is prose the reader asked
        // for, not a preview to scroll past (owner decision, 6.7: the
        // three-line teaser of 6.6, a side effect of the introduction
        // loading as the surah opens so the door knows whether it has
        // anything behind it, is removed).
        if (expanded && info != null) {
            // Opened, the introduction wears the wash a chosen ayah wears:
            // the reader asked for it, and while it is up it reads as the
            // chosen thing on the page, on the same ground a chosen ayah
            // leaves behind. It is also its own way out: the door that
            // opened it is gone while it is up, so a tap anywhere on it
            // puts it away, and the accent ripple is the press answering.
            // The click's label is the word a screen reader hears, so the
            // one exit is spoken as Hide (owner decision, 23.2).
            val press = remember { MutableInteractionSource() }
            Box(
                // The introduction is a document, not one paragraph: it is
                // the same blocks the tafsir is drawn from, so its own heads
                // (Name, Period of Revelation, Theme) stand as heads instead
                // of running into the sentence under them. The old shape
                // flattened every block to plain prose and lost the source's
                // structure on the page that explains the surah (owner
                // decision, 13.3).
                //
                // It takes the door's own slot, a row under the place, so
                // the top edge of the page does not move when the door
                // becomes the document.
                modifier = Modifier
                    .padding(top = Space.Row)
                    .clip(Radii.Card)
                    .background(palette.selected)
                    .focusRing(press, Radii.Card)
                    // A tap puts it away and a drag is the list's: the
                    // standard click cancels its press when the scroll takes
                    // over, so a reader who drags the introduction reads on
                    // and does not lose it to a stray tap. The label is what
                    // TalkBack says the double tap does (owner decision,
                    // 23.2).
                    .clickable(
                        interactionSource = press,
                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                        onClickLabel = stringResource(R.string.reading_hide),
                        onClick = { onExpandedChange(false) },
                    )
                    .testTag("surah-about")
                    .padding(horizontal = Space.WashInset, vertical = Space.Row),
            ) {
                RichBlocks(
                    blocks = info.orEmpty(),
                    // Opened, the introduction is the reader's prose, and it
                    // reads at the reading's own voice and size, not at a
                    // caption's: it is the longest text beside the tafsir on
                    // this page, so it takes the same room the translation
                    // takes (owner decision, 13.3).
                    sizeSp = metrics.translationSp,
                    lineSp = metrics.translationLineSp,
                    arabicSp = metrics.arabicInlineSp,
                    color = palette.introInk,
                    onLink = { href ->
                        ReferenceLink.parse(href)?.let { onOpenReference(it.surah, it.ayah) }
                    },
                )
            }
        }
        // The door itself is a button like every other action in the app,
        // so a word that answers a tap never reads as plain type. It is
        // drawn only while the introduction is closed: opened, the
        // introduction takes this slot and is its own close, so there is no
        // second button at the foot to scroll to. The door exists only where
        // the introduction does: a language with no introduction is not
        // offered a button that opens nothing (owner decision, 23.2).
        if (!expanded && info != null) {
            TextButton(
                label = stringResource(R.string.reading_about_surah),
                onClick = { onExpandedChange(true) },
                modifier = Modifier.padding(top = Space.Row),
            )
        }
        basmalah?.let { line ->
            // The basmalah is the Quran's own first ayah, so it reads in the
            // Quran's own face and at the reader's own Arabic size, the same
            // as every verse below it. It wore 26 sp Amiri, a fixed hand
            // outside the reader's choice, and the opening read as two
            // different texts stacked (owner decision, 7.6).
            //
            // It is the text's first line: the head and its door stand above
            // it, so the preface never comes between the basmalah and the
            // first ayah (owner decision, 23.6).
            TajweedText(
                text = AnnotatedString(line.text),
                code = line.tajweed,
                colors = palette.tajweed,
                enabled = metrics.tajweed,
                style = arabicStyle(hafs, metrics.arabicSp, metrics.arabicLineSp, ink),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Section)
                    .semantics { contentDescription = Arabic.portableText(line.text) }
                    .testTag("surah-basmalah"),
            )
        }
    }
}

/**
 * The one-time reminder offer, at the end of the first surah the reader
 * finishes. It is not a gate: the reading is already the gift, and the ask
 * comes after it. Turn on asks the phone for what it needs; Not now moves
 * the ask to the end of the next surah the reader finishes, and the second
 * Not now is the no that never asks again (owner decisions, 15.5 and 21.9).
 *
 * The tile centers its name, its sentence, and its two answers (owner
 * decision, 21.8). It stands under the surah's own centered completion
 * line, and a short invitation on a full-width card reads as one block only
 * when every line in it agrees on one axis, the rule the empty note already
 * keeps (15.0). The reading's own lines start at the leading edge because
 * they are long; the tile's are one short sentence each. The pair keeps its
 * one order and gap (18.8) and stands the way the share sheet's centered
 * pair does. The tafsir offer inside the ayah card keeps its sheet's
 * leading edge, because it is a section of a page that starts there, not a
 * card standing on the glass.
 */
@Composable
internal fun ReadingReminderOffer(
    onTurnOn: () -> Unit,
    onNotNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.Section, bottom = Space.Row)
            .clip(Radii.Tile)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = Wash.Door))
            .padding(horizontal = Space.Block, vertical = Space.Block)
            .testTag("reading-reminder-offer"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.reading_reminder_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.reading_reminder_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Space.Line),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Space.Block),
            horizontalArrangement = Arrangement.spacedBy(Space.Action, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                label = stringResource(R.string.reading_reminder_not_now),
                onClick = onNotNow,
                quiet = true,
            )
            TextButton(
                label = stringResource(R.string.reading_reminder_turn_on),
                onClick = onTurnOn,
                prominent = true,
            )
        }
    }
}

/**
 * The end of a surah, and the door to the next one.
 *
 * The column keeps 20 dp under its own last line, and the reading's foot
 * under the list adds 32 dp above the system's navigation bar, so the card
 * ends 52 dp above the bar: exactly half the air the opening of a surah is
 * given above it (64 dp of page padding and 40 dp of the opening itself).
 * The turn of a page then reads as a shorter breath than the arrival, which
 * is the order the reader meets them in, never as a card left on the edge of
 * the glass (owner decision, 12.2).
 *
 * The surah closes with the completion line alone. The short gray rule
 * that stood over it was a hairline at a share no scale names, too faint
 * to read as a mark and too quiet to close anything the line does not
 * say. A gold closing mark in the ornament's voice was weighed and set
 * aside: the turn stays the quieter breath, plain type and one card
 * (owner decision, 20.1).
 */
/** The turn of a page: the lesser breath, argued against the arrival in
 * [SurahEnd]'s own KDoc. */
private val EndTop = 30.dp

/** The air under the next-surah card, which [SurahEnd]'s KDoc counts. */
private val EndBottom = 20.dp

@Composable
internal fun SurahEnd(
    surah: Surah,
    nextSurahName: String?,
    onNextSurah: (Int) -> Unit,
) {
    val next = surah.number + 1
    val nextPress = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = EndTop, bottom = EndBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.reading_surah_complete, surah.nameSimple),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Block),
        )
        if (next <= 114 && nextSurahName != null) {
            // The next surah is a card, not a bare line of type: the label
            // says what the tap does, the name says where it lands, and the
            // arrow says which way the reading goes, the way the end of a
            // page names the page after it.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Block)
                    .clip(Radii.Tile)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = Wash.Door))
                    // A card that opens a surah is a door, and every door says
                    // so to TalkBack; this one answered a tap with the words
                    // alone (owner decision, 7.6).
                    .semantics { role = Role.Button }
                    .focusRing(nextPress, Radii.Tile)
                    .clickable(
                        interactionSource = nextPress,
                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                        role = Role.Button,
                        onClick = { onNextSurah(next) },
                    )
                    .padding(horizontal = Space.Block, vertical = Space.Row),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reading_next_surah),
                        // A label over the next surah's name, so it wears the
                        // meta line rather than a group head: the card itself
                        // is the door (owner decision, 13.3).
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = nextSurahName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = Space.Tight),
                    )
                }
                AppChevron(
                    role = ChevronRole.Next,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else if (next > 114) {
            Text(
                text = stringResource(R.string.reading_end_of_quran),
                style = MaterialTheme.typography.titleMedium,
                // The end of the Book is a statement, not an action: the
                // accent belongs to what answers a tap (owner decision, 13.3).
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

