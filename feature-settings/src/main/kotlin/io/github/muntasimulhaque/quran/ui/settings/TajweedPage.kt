package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.TajweedRule
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.TajweedColors

/**
 * The eighteen rules, in the seven families the reading draws. Each row is a
 * swatch and the rule's own name, so a reader who wants to know what the
 * color on the page means finds the answer without leaving the app. The
 * swatch is the theme's color for the current ground; the name is the
 * reader's language (owner decision, this session).
 */
@Composable
fun TajweedPage() {
    val colors = LocalPagePalette.current.tajweed
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
        Family(colors, R.string.settings_tajweed_family_silent, TajweedRule.HAM_WASL, TajweedRule.LAAM_SHAMSIYAH, TajweedRule.SLNT)
        Family(colors, R.string.settings_tajweed_family_ghunnah, TajweedRule.GHUNNAH)
        Family(colors, R.string.settings_tajweed_family_qalaqah, TajweedRule.QALAQAH)
        Family(colors, R.string.settings_tajweed_family_ikhafa, TajweedRule.IKHAFA, TajweedRule.IKHAFA_SHAFAWI)
        Family(colors, R.string.settings_tajweed_family_iqlab, TajweedRule.IQLAB)
        Family(
            colors,
            R.string.settings_tajweed_family_idgham,
            TajweedRule.IDGHAM_GHUNNAH,
            TajweedRule.IDGHAM_WO_GHUNNAH,
            TajweedRule.IDGHAM_SHAFAWI,
            TajweedRule.IDGHAM_MUTAJANISAYN,
            TajweedRule.IDGHAM_MUTAQARIBAYN,
        )
        Family(
            colors,
            R.string.settings_tajweed_family_madda,
            TajweedRule.MADDA_NORMAL,
            TajweedRule.MADDA_PERMISSIBLE,
            TajweedRule.MADDA_OBLIGATORY_MONFASEL,
            TajweedRule.MADDA_OBLIGATORY_MOTTASEL,
            TajweedRule.MADDA_NECESSARY,
        )
        Spacer(Modifier.height(Space.Section))
    }
}

@Composable
private fun Family(colors: TajweedColors, title: Int, vararg rules: TajweedRule) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.Section),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        rules.forEach { rule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Row),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colors.of(rule)),
                )
                Text(
                    text = stringResource(rule.label()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = Space.Row),
                )
            }
        }
    }
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
