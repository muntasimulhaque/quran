package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space


/** How the catalog joins the licenses of one pack's datasets. */
private const val LICENSE_SEPARATOR = " · "

/**
 * The app itself: its version, who made what it carries and under what terms.
 *
 * The credits stand here, on the page, rather than behind a row that opens
 * them (owner decision, this session): every license is honored by being read
 * where a reader already is, not by a door they have to think to open, and a
 * reader who wonders who made the text meets the answer the moment they open
 * About.
 *
 * The page is facts only: every row is read and nothing opens (owner
 * decision). Corrections and rights requests travel through the store
 * listing's contact and the project repository rather than through the app.
 *
 * The content is checked on the reader's behalf, once a day, without being
 * asked; there is no row for it here, because a reader should never have to
 * know the word for it (owner decision, this session).
 */
@Composable
fun AboutPage(
    packs: List<ContentPack>,
    version: String,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        Group(stringResource(R.string.settings_group_about))
        // The version's digits follow the interface's own language, like every
        // other number a reader meets (owner decision, this session).
        ValueRow(stringResource(R.string.settings_version), localizedDigits(version))
        ValueRow(stringResource(R.string.settings_license_label), stringResource(R.string.about_license))

        Group(stringResource(R.string.about_group_content))
        // What is on the device is what the page names: every license is
        // honored where the reader already meets the content it covers, and
        // a catalog of packs nobody installed is a list of other people's
        // books (owner decision, this session).
        //
        // The reciters are the one kind left to their own group below, where
        // they are named and credited in the reader's own language: naming
        // them here as well printed the same two voices twice, once as raw
        // catalog rows and once as the app's own credits (owner decision,
        // this session).
        packs.filter { (it.shipped || it.installed) && it.type != PackType.Recitation }.forEach { pack ->
            AboutCredit(
                title = stringResource(
                    R.string.about_pack_title,
                    contentPackName(pack.id, pack.name),
                    languageName(pack.language),
                ),
                lines = listOf(packLine(pack.credit, pack.creditBn)) +
                    packLine(pack.license, pack.licenseBn).split(LICENSE_SEPARATOR)
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
            )
        }

        Group(stringResource(R.string.about_group_fonts))
        AboutCredit(
            stringResource(R.string.credit_page_fonts),
            listOf(
                stringResource(R.string.credit_page_fonts_by),
                stringResource(R.string.credit_page_fonts_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_amiri),
            listOf(
                stringResource(R.string.credit_amiri_by),
                stringResource(R.string.credit_amiri_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_literata),
            listOf(
                stringResource(R.string.credit_literata_by),
                stringResource(R.string.credit_literata_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_noto_serif_bengali),
            listOf(
                stringResource(R.string.credit_noto_serif_bengali_by),
                stringResource(R.string.credit_noto_serif_bengali_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_inter),
            listOf(
                stringResource(R.string.credit_inter_by),
                stringResource(R.string.credit_inter_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_noto_sans_bengali),
            listOf(
                stringResource(R.string.credit_noto_sans_bengali_by),
                stringResource(R.string.credit_noto_sans_bengali_use),
            ),
        )

        Group(stringResource(R.string.about_group_recitations))
        AboutCredit(
            stringResource(R.string.credit_minshawi),
            listOf(
                stringResource(R.string.credit_minshawi_by),
                stringResource(R.string.credit_minshawi_use),
            ),
        )
        AboutCredit(
            stringResource(R.string.credit_husary),
            listOf(
                stringResource(R.string.credit_husary_by),
                stringResource(R.string.credit_husary_use),
            ),
        )

        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * A pack's own line in the reader's language: the catalog's Bangla line when
 * the interface is Bangla and the catalog carries one, and the English line
 * otherwise, so no pack is ever credited in a language nobody wrote for it
 * (owner decision, this session).
 */
@Composable
private fun packLine(english: String, bangla: String): String {
    val language = LocalConfiguration.current.locales[0].language
    return remember(english, bangla, language) {
        if (language.startsWith("bn") && bangla.isNotBlank()) bangla else english
    }
}

/** One credit: its name, and the lines that say who made it and what for. */
@Composable
private fun AboutCredit(title: String, lines: List<String>) {
    Column(Modifier.padding(start = Space.Gutter, end = Space.Gutter, bottom = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
