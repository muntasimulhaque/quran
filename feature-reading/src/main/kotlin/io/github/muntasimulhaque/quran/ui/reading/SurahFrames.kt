package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.Arabic
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ReadingMetrics
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.reader.GlyphSize
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash
import io.github.muntasimulhaque.quran.ui.theme.rememberHafs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SurahOpening(
    surah: Surah,
    content: ContentDatabase?,
    /** The reader's language: the introduction must speak it or not appear. */
    language: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPaperTap: () -> Unit,
    metrics: ReadingMetrics,
) {
    // The introduction is read as the surah opens, not when its door is
    // pressed, because the door is drawn only when there is something behind
    // it: a button that opens nothing is worse than no button. A language
    // whose content carries no introduction of its own is shown the English
    // one, because the introduction is the one place the app prefers a
    // language the reader may not read over saying nothing at all (owner
    // decision, this session). The translation, the tafsir, and the word
    // meanings keep the strict one-language rule.
    val info by produceState<String?>(initialValue = null, surah.number, content, language) {
        value = withContext(Dispatchers.IO) { content?.surahInfo(surah.number, language) }
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
            .padding(top = 40.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The opening of a surah is the one arrival in the reading: the Arabic
        // name in the ornament gold, a short gold rule under it, and the name
        // and place in quiet type, so every surah opens the same way.
        Text(
            text = surah.nameArabic,
            style = TextStyle(
                fontFamily = Amiri,
                fontSize = 40.sp,
                color = ornament,
            ),
        )
        // The rule is ornament, never a separator: a short gold rule under the
        // Arabic name, carrying no meaning a reader would lose without it. It
        // is a hairline of the same gold, held well inside the measure.
        Box(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth(0.3f)
                .height(1.dp)
                .background(ornament.copy(alpha = 0.55f)),
        )
        Text(
            text = surah.nameSimple,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = Space.Section),
        )
        Text(
            text = stringResource(
                R.string.reading_surah_meta,
                if (surah.revelationPlace.equals("makkah", true)) {
                    stringResource(R.string.reading_place_makkah)
                } else {
                    stringResource(R.string.reading_place_madinah)
                },
                surah.versesCount,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Tight),
        )
        basmalah?.let { line ->
            // The basmalah is the Quran's own first ayah, so it reads in the
            // Quran's own face and at the reader's own Arabic size, the same
            // as every verse below it. It wore 26 sp Amiri, a fixed hand
            // outside the reader's choice, and the opening read as two
            // different texts stacked (owner decision, the theme and
            // consistency session).
            TajweedText(
                text = AnnotatedString(line.text),
                code = line.tajweed,
                colors = palette.tajweed,
                enabled = metrics.tajweed,
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = metrics.arabicSp.sp,
                    lineHeight = metrics.arabicLineSp.sp,
                    color = ink,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Section)
                    .semantics { contentDescription = Arabic.portableText(line.text) },
            )
        }
        // The introduction is drawn only once the reader opens it: closed,
        // the door is the whole of what stands under the basmalah, so the
        // surah's own first ayah keeps the page. The about is prose the
        // reader asked for, not a preview to scroll past (owner decision,
        // this session: the three-line teaser of 6.6, a side effect of the
        // introduction loading as the surah opens so the door knows whether
        // it has anything behind it, is removed).
        if (expanded && info != null) {
            // Opened, the introduction wears the wash a chosen ayah wears:
            // the reader asked for it, and while it is up it reads as the
            // chosen thing on the page, in the same lapis at the same weight
            // a chosen ayah leaves behind. A press on it answers in the same
            // lapis rather than the gray an unstyled ripple would flash.
            val wash = if (expanded) {
                MaterialTheme.colorScheme.primary.copy(alpha = Wash.Selected)
            } else {
                Color.Transparent
            }
            val press = remember { MutableInteractionSource() }
            Text(
                text = RichText.paragraphs(info.orEmpty()),
                // Opened, the introduction is the reader's prose, and it
                // reads at the reading's own voice and size, not at a
                // caption's: it is the longest text beside the tafsir on
                // this page, so it takes the same room the translation
                // takes (owner decision, this session). The runs are drawn
                // plain by design: [RichText.paragraphs] draws no spans
                // here, so the tags in the source never reach one.
                style = LocalReadingVoice.current.style.copy(
                    fontSize = metrics.translationSp.sp,
                    lineHeight = metrics.translationLineSp.sp,
                    fontFamily = LocalReadingVoice.current.atSize(metrics.translationSp),
                ),
                color = ink.copy(alpha = 0.78f),
                // No cap on the introduction: it was asked for with a press,
                // and it is the reader's to read while the door stays open.
                // The door is the paragraph itself: the reading around it is
                // the paper, and only the part that opens is its own target,
                // so a press shows the boundary of the about and not of the
                // whole opening. The boundary is the ayah's own rounded one,
                // and the ayah's own room sits inside it: the shape wraps the
                // padding, not the letters, so the about answers in the same
                // shape and color as an ayah and no line of text is left
                // touching the edge it is standing on.
                modifier = Modifier
                    .padding(top = Space.Block)
                    .clip(Radii.Card)
                    .background(wash)
                    .clickable(
                        interactionSource = press,
                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                        onClick = onPaperTap,
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
        // The door itself is a button like every other action in the app,
        // so a word that answers a tap never reads as plain type. It sits
        // under the introduction: closed, it is the whole of what is shown;
        // opened, it is the Hide that puts the introduction away again. The
        // door exists only where the introduction does: a language with no
        // introduction is not offered a button that opens nothing.
        if (info != null) {
            TextButton(
                label = stringResource(
                    if (expanded) R.string.reading_hide else R.string.reading_about_surah,
                ),
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** The end of a surah, and the door to the next one. */
@Composable
internal fun SurahEnd(
    surah: Surah,
    nextSurahName: String?,
    onNextSurah: (Int) -> Unit,
) {
    val next = surah.number + 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 30.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.22f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        )
        Text(
            text = stringResource(R.string.reading_surah_complete, surah.nameSimple),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
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
                    .clip(Radii.Card)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = Wash.Door))
                    // A card that opens a surah is a door, and every door says
                    // so to TalkBack; this one answered a tap with the words
                    // alone (owner decision, the theme and consistency
                    // session).
                    .semantics { role = Role.Button }
                    .clickable { onNextSurah(next) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reading_next_surah),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = nextSurahName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = Space.Tight),
                    )
                }
                IconGlyph(
                    icon = Icon.Chevron,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(GlyphSize.ChevronRow)
                        .rotate(-90f),
                )
            }
        } else if (next > 114) {
            Text(
                text = stringResource(R.string.reading_end_of_quran),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

