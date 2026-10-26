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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.TajweedFamily
import io.github.muntasimulhaque.quran.core.TajweedRule
import io.github.muntasimulhaque.quran.core.TajweedSamples
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.rich.ArabicFonts
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.TajweedColors

/**
 * The eighteen rules, in the seven families the reading draws when its
 * switch is on. Each family shows one word from the reading in that family's
 * color, drawn by the reading's own painter at the reading's own size, and
 * the rules it covers under it (owner decision, the legend session).
 *
 * The dot this page used to show was a fat patch of color, and a fat patch
 * reads stronger than the thin stroke a colored letter is drawn in: the
 * legend promised a green the page never showed. A word is the page's own
 * material, so the reader compares the sample to the ayah and sees one
 * color.
 *
 * The words are not typed here. [sample] reads an ayah whose words carry all
 * seven families, and [TajweedSamples] cuts one word per family out of it,
 * so the sample is the audited text and the audited code, and a content
 * update can never leave the legend showing a word the page does not draw.
 *
 * The sentence under each family and rule is UI copy, not a print annotation:
 * it says what the rule asks of the reciter, and it belongs to the same
 * review as the content. A qualified reviewer checks it before a release.
 */
@Composable
fun TajweedPage(
    settings: AppSettings,
    sample: suspend () -> Ayah?,
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
        Text(
            text = stringResource(R.string.settings_tajweed_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = Space.Gutter,
                end = Space.Gutter,
                bottom = Space.Section,
            ),
        )
        TajweedFamily.entries.forEach { family ->
            Family(
                family = family,
                colors = palette.tajweed,
                sample = samples.firstOrNull { it.family == family },
                settings = settings,
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
    settings: AppSettings,
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
                        fontSize = settings.arabicSp.sp,
                        lineHeight = settings.arabicLineSp.sp,
                        color = palette.ink,
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        Text(
            text = stringResource(family.note()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Tight),
        )
        family.rules.forEach { rule ->
            Text(
                text = stringResource(rule.label()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = Space.Row, top = Space.Block),
            )
            Text(
                text = stringResource(rule.note()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Space.Row, top = 2.dp),
            )
        }
    }
}

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

/** One sentence on what the family sounds like, under its name. */
private fun TajweedFamily.note(): Int = when (this) {
    TajweedFamily.SILENT -> R.string.settings_tajweed_family_silent_note
    TajweedFamily.GHUNNAH -> R.string.settings_tajweed_family_ghunnah_note
    TajweedFamily.QALAQAH -> R.string.settings_tajweed_family_qalaqah_note
    TajweedFamily.IKHAFA -> R.string.settings_tajweed_family_ikhafa_note
    TajweedFamily.IQLAB -> R.string.settings_tajweed_family_iqlab_note
    TajweedFamily.IDGHAM -> R.string.settings_tajweed_family_idgham_note
    TajweedFamily.MADDA -> R.string.settings_tajweed_family_madda_note
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

/** One sentence on what the rule asks of the reciter, under its name. */
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
