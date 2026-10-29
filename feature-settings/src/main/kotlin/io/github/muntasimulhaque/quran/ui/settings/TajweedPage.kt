package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.TajweedFamily
import io.github.muntasimulhaque.quran.core.TajweedRule
import io.github.muntasimulhaque.quran.core.TajweedSamples
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.rich.ArabicFonts
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.TajweedColors

/**
 * The legend of the reading's colors: the seven families the page draws when
 * its switch is on, each with one word from the reading in that family's
 * color, and every rule the family covers with one sentence on what it asks
 * of the reciter.
 *
 * The colors are seven, one per family, and that is the frame the page is
 * for: a reader looking at a green letter wants to know which of the seven
 * groups it is. But the group is not the whole answer, because the rules
 * inside a family ask different things of the voice: idgham with a nasal sound
 * and idgham without one are the same color and not the same act. So each
 * family is a head, its name and the sample word that proves the color, and
 * under it every rule it covers, named and explained.
 *
 * A rule is one paragraph, not two lines: the name runs into its sentence in
 * the family's ink and weight, and the sentence follows in the quiet tone, so
 * eighteen rules cost eighteen short paragraphs instead of eighteen headings
 * and eighteen notes with a block of air between each pair. The two spaces
 * that separate the name from its sentence are the whole of the punctuation,
 * because English ends a sentence with a period and Bangla with a danda, and
 * neither belongs between a name and the words that explain it.
 *
 * The page used to print all eighteen names, a sentence for each, and one
 * sentence for the family above them, which said the same thing twice for the
 * four families that hold a single rule; it then went to the seven families
 * with their rule names joined on one line, which was a third of the page and
 * explained none of the words (owner report). What stands here is the
 * explanation back, without the second reading of it.
 *
 * The sample word is drawn by the reading's own painter, in the reading's own
 * palette, so the legend and the page cannot disagree. It is capped at a
 * fixed size of its own: at the reader's largest Arabic step the sample stood
 * taller than the family's own name and the legend read as a second reading
 * (owner report).
 *
 * The words are not typed here. [sample] reads an ayah whose words carry all
 * seven families, and [TajweedSamples] cuts one word per family out of it,
 * so the sample is the audited text and the audited code, and a content
 * update can never leave the legend showing a word the page does not draw.
 *
 * The sentence under each rule is UI copy, not a print annotation: it says
 * what the rule asks of the reciter, and it belongs to the same review as the
 * content. A qualified reviewer checks it before a release.
 *
 * The switch that draws all of this stands at the page's own top, in the same
 * words and the same state as the hub row that opened it. A reader reading the
 * colors and finding them not to their taste turns them off where they are,
 * rather than going back a page for the switch they came from (owner decision,
 * an earlier session).
 */
@Composable
fun TajweedPage(
    settings: AppSettings,
    sample: suspend () -> Ayah?,
    onTajweed: (Boolean) -> Unit,
) {
    val palette = LocalPagePalette.current
    val context = LocalContext.current
    val hafs = remember(context) { ArabicFonts(context).hafsFamily }
    val ayah by produceState<Ayah?>(initialValue = null) { value = sample() }
    val samples = remember(ayah) {
        ayah?.let { TajweedSamples.of(it.text, it.tajweed) }.orEmpty()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sheetVerticalScroll(rememberScrollState()),
    ) {
        ToggleRow(
            title = stringResource(R.string.settings_tajweed_title),
            subtitle = stringResource(R.string.settings_tajweed_summary),
            checked = settings.tajweed,
            onChange = onTajweed,
            switchTag = "switch-tajweed-page",
        )
        Spacer(Modifier.height(Space.Block))
        TajweedFamily.entries.forEach { family ->
            Family(
                family = family,
                colors = palette.tajweed,
                sample = samples.firstOrNull { it.family == family },
                hafs = hafs,
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}

@Composable
private fun Family(
    family: TajweedFamily,
    colors: TajweedColors,
    sample: TajweedSamples.Sample?,
    hafs: FontFamily,
) {
    val palette = LocalPagePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.Section),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(family.title()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (sample != null) {
                // The word is the page's own text and the page's own color,
                // so the legend and the reading can never disagree. A screen
                // reader has nothing to do with a color sample, and an
                // unexplained Arabic word in the middle of the list is noise.
                TajweedText(
                    text = AnnotatedString(sample.text),
                    code = sample.code,
                    colors = colors,
                    enabled = true,
                    style = TextStyle(
                        fontFamily = hafs,
                        fontSize = LegendSampleSp.sp,
                        lineHeight = (LegendSampleSp * TextSize.lineRatio(TypeRole.Arabic)).sp,
                        color = palette.ink,
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        // The rules that wear this color, each named and explained. The family
        // is one head over them and says nothing of its own: four of the seven
        // hold a single rule, and a sentence above them was that rule's own
        // sentence said twice (owner report).
        family.rules.forEachIndexed { index, rule ->
            Rule(
                name = stringResource(rule.label()),
                sentence = stringResource(rule.note()),
                modifier = Modifier.padding(top = if (index == 0) Space.Block else Space.Line),
            )
        }
    }
}

/**
 * One rule: its name in the page's ink and the app's medium weight, its
 * sentence after it in the quiet tone, on one paragraph.
 *
 * The name is a run-in head, so a reader scanning for a word they have heard
 * finds it at the start of a line without the page paying for a line of its
 * own, and the sentence that explains it follows to the end of the same
 * thought.
 */
@Composable
private fun Rule(name: String, sentence: String, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = ink, fontWeight = FontWeight.Medium)) { append(name) }
            append(RuleRunIn)
            withStyle(SpanStyle(color = quiet)) { append(sentence) }
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}

/**
 * The room between a rule's name and its sentence: two spaces, so a run-in
 * head separates from its own sentence without a mark that belongs to one
 * language and not the other.
 */
private const val RuleRunIn = "  "

/**
 * The one size the legend's sample word is drawn at, whatever the reader's
 * own Arabic step: a legend is read, not recited, and at the largest step the
 * sample stood taller than the family it belongs to.
 */
private const val LegendSampleSp = 24f

/** The family's name in the reader's language. */
private fun TajweedFamily.title(): Int = when (this) {
    TajweedFamily.SILENT -> R.string.settings_tajweed_family_silent
    TajweedFamily.GHUNNAH -> R.string.settings_tajweed_family_ghunnah
    TajweedFamily.QALAQAH -> R.string.settings_tajweed_family_qalaqah
    TajweedFamily.IKHAFA -> R.string.settings_tajweed_family_ikhafa
    TajweedFamily.IQLAB -> R.string.settings_tajweed_family_iqlab
    TajweedFamily.IDGHAM -> R.string.settings_tajweed_family_idgham
    TajweedFamily.MADDA -> R.string.settings_tajweed_family_madda
}

/** The rule's name in the reader's language, one string for each rule. */
private fun TajweedRule.label(): Int = when (this) {
    TajweedRule.HAM_WASL -> R.string.settings_tajweed_rule_ham_wasl
    TajweedRule.LAAM_SHAMSIYAH -> R.string.settings_tajweed_rule_laam_shamsiyah
    TajweedRule.SLNT -> R.string.settings_tajweed_rule_slnt
    TajweedRule.GHUNNAH -> R.string.settings_tajweed_rule_ghunnah
    TajweedRule.QALAQAH -> R.string.settings_tajweed_rule_qalaqah
    TajweedRule.IKHAFA -> R.string.settings_tajweed_rule_ikhafa
    TajweedRule.IKHAFA_SHAFAWI -> R.string.settings_tajweed_rule_ikhafa_shafawi
    TajweedRule.IQLAB -> R.string.settings_tajweed_rule_iqlab
    TajweedRule.IDGHAM_GHUNNAH -> R.string.settings_tajweed_rule_idgham_ghunnah
    TajweedRule.IDGHAM_WO_GHUNNAH -> R.string.settings_tajweed_rule_idgham_wo_ghunnah
    TajweedRule.IDGHAM_SHAFAWI -> R.string.settings_tajweed_rule_idgham_shafawi
    TajweedRule.IDGHAM_MUTAJANISAYN -> R.string.settings_tajweed_rule_idgham_mutajanisayn
    TajweedRule.IDGHAM_MUTAQARIBAYN -> R.string.settings_tajweed_rule_idgham_mutaqaribayn
    TajweedRule.MADDA_NORMAL -> R.string.settings_tajweed_rule_madda_normal
    TajweedRule.MADDA_PERMISSIBLE -> R.string.settings_tajweed_rule_madda_permissible
    TajweedRule.MADDA_OBLIGATORY_MONFASEL -> R.string.settings_tajweed_rule_madda_obligatory_monfasel
    TajweedRule.MADDA_OBLIGATORY_MOTTASEL -> R.string.settings_tajweed_rule_madda_obligatory_mottasel
    TajweedRule.MADDA_NECESSARY -> R.string.settings_tajweed_rule_madda_necessary
}

/** One sentence on what the rule asks of the reciter, after its name. */
private fun TajweedRule.note(): Int = when (this) {
    TajweedRule.HAM_WASL -> R.string.settings_tajweed_rule_ham_wasl_note
    TajweedRule.LAAM_SHAMSIYAH -> R.string.settings_tajweed_rule_laam_shamsiyah_note
    TajweedRule.SLNT -> R.string.settings_tajweed_rule_slnt_note
    TajweedRule.GHUNNAH -> R.string.settings_tajweed_rule_ghunnah_note
    TajweedRule.QALAQAH -> R.string.settings_tajweed_rule_qalaqah_note
    TajweedRule.IKHAFA -> R.string.settings_tajweed_rule_ikhafa_note
    TajweedRule.IKHAFA_SHAFAWI -> R.string.settings_tajweed_rule_ikhafa_shafawi_note
    TajweedRule.IQLAB -> R.string.settings_tajweed_rule_iqlab_note
    TajweedRule.IDGHAM_GHUNNAH -> R.string.settings_tajweed_rule_idgham_ghunnah_note
    TajweedRule.IDGHAM_WO_GHUNNAH -> R.string.settings_tajweed_rule_idgham_wo_ghunnah_note
    TajweedRule.IDGHAM_SHAFAWI -> R.string.settings_tajweed_rule_idgham_shafawi_note
    TajweedRule.IDGHAM_MUTAJANISAYN -> R.string.settings_tajweed_rule_idgham_mutajanisayn_note
    TajweedRule.IDGHAM_MUTAQARIBAYN -> R.string.settings_tajweed_rule_idgham_mutaqaribayn_note
    TajweedRule.MADDA_NORMAL -> R.string.settings_tajweed_rule_madda_normal_note
    TajweedRule.MADDA_PERMISSIBLE -> R.string.settings_tajweed_rule_madda_permissible_note
    TajweedRule.MADDA_OBLIGATORY_MONFASEL -> R.string.settings_tajweed_rule_madda_obligatory_monfasel_note
    TajweedRule.MADDA_OBLIGATORY_MOTTASEL -> R.string.settings_tajweed_rule_madda_obligatory_mottasel_note
    TajweedRule.MADDA_NECESSARY -> R.string.settings_tajweed_rule_madda_necessary_note
}
