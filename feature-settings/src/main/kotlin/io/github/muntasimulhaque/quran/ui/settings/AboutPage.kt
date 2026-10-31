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
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.screenSubtitle

/** How the page joins two facts that belong to one credit, or splits terms. */
private const val SEPARATOR = " · "

/**
 * The app itself: its version and license, then who made the text, the faces,
 * and the voices it carries, and under what terms.
 *
 * The page is an obligation and is written as one. It stands so a reader can
 * see whose book this is: the licenses of the datasets the app draws require
 * their source, their version, and their terms to be readable in the app, and
 * the faces and the recitations are credited by the hand that made them. It
 * says that and stops. The page used to carry a "what each face is for" line
 * under every credit and a group of two label-over-value rows at its head;
 * both are the kind of writing that makes a license page into a brochure
 * (owner report, this session).
 *
 * The shape is the sheet's own, one step quieter than its rows: a name in the
 * reading size, and under it one line that says what the thing is, its maker
 * and its version joined rather than stacked. A pack's terms stand below that
 * line with a line's air between, because terms are a different kind of fact
 * from a credit and must not read as one more sentence about it.
 *
 * The page has no door at all. It used to end the Fonts group with a row that
 * opened the full SIL Open Font License, and the reader tapped a license page
 * that repeated the six credits above it before printing the text (owner
 * report). The faces are credited here, each by the hand that made it; the
 * license texts travel with the files in the package, where every library's
 * license already travels, and the manifest records each dataset's terms. A
 * license screen is not part of reading the Quran, and the app does not teach
 * one.
 *
 * Corrections and rights requests travel through the store listing's contact
 * and the project repository rather than through the app.
 *
 * Content is verified when it is written, trusted by its length and moment
 * afterwards, and re-hashed when the library opens; a damaged pack is
 * repaired from its own row. There is no daily pass and no row for it here,
 * because a reader should never have to know the word for it (owner
 * decision, earlier session).
 */
@Composable
fun AboutPage(
    packs: List<ContentPack>,
    version: String,
) {
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        // The app's own two facts, on the page's first line. A reader who
        // opens About wants the version and the terms, and the two of them in
        // one sentence are shorter than the two rows that said "Version" and
        // "License" above their own values (owner report, this session).
        Text(
            text = stringResource(R.string.about_version_license, localizedDigits(version)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = Space.Gutter,
                end = Space.Gutter,
                top = Space.Block + Space.Tight,
            ),
        )

        Group(stringResource(R.string.about_group_content))
        // What is on the device is what the page names: every license is
        // honored where the reader already meets the content it covers, and
        // a catalog of packs nobody installed is a list of other people's
        // books (owner decision, an earlier session).
        //
        // The reciters are the one kind left to their own group below, where
        // they are named and credited in the reader's own language: naming
        // them here as well printed the same two voices twice, once as raw
        // catalog rows and once as the app's own credits (owner decision, an
        // earlier session).
        packs.filter { (it.shipped || it.installed) && it.type != PackType.Recitation }
            .forEach { pack ->
                val maker = packLine(pack.credit, pack.creditBn)
                val versionLine = pack.version
                    .takeIf { it.isNotBlank() && it != "QUL" }
                    ?.let { stringResource(R.string.settings_version) + " " + it }
                AboutCredit(
                    title = stringResource(
                        R.string.about_pack_title,
                        contentPackName(pack.id, pack.name),
                        packKind(pack),
                    ),
                    line = listOfNotNull(maker, versionLine).joinToString(SEPARATOR),
                    terms = packLine(pack.license, pack.licenseBn)
                        .split(SEPARATOR)
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
                )
            }

        Group(stringResource(R.string.about_group_fonts))
        Face(
            title = stringResource(R.string.credit_page_fonts),
            madeBy = stringResource(R.string.credit_page_fonts_by),
            usedFor = stringResource(R.string.credit_page_fonts_use),
        )
        Face(
            title = stringResource(R.string.credit_amiri),
            madeBy = stringResource(R.string.credit_amiri_by),
            usedFor = stringResource(R.string.credit_amiri_use),
        )
        Face(
            title = stringResource(R.string.credit_amiri_text),
            madeBy = stringResource(R.string.credit_amiri_text_by),
            usedFor = stringResource(R.string.credit_amiri_text_use),
        )
        Face(
            title = stringResource(R.string.credit_literata),
            madeBy = stringResource(R.string.credit_literata_by),
            usedFor = stringResource(R.string.credit_literata_use),
        )
        Face(
            title = stringResource(R.string.credit_noto_serif_bengali),
            madeBy = stringResource(R.string.credit_noto_serif_bengali_by),
            usedFor = stringResource(R.string.credit_noto_serif_bengali_use),
        )
        Face(
            title = stringResource(R.string.credit_inter),
            madeBy = stringResource(R.string.credit_inter_by),
            usedFor = stringResource(R.string.credit_inter_use),
        )
        Face(
            title = stringResource(R.string.credit_noto_sans_bengali),
            madeBy = stringResource(R.string.credit_noto_sans_bengali_by),
            usedFor = stringResource(R.string.credit_noto_sans_bengali_use),
        )

        Group(stringResource(R.string.about_group_recitations))
        Face(
            title = stringResource(R.string.credit_minshawi),
            madeBy = stringResource(R.string.credit_minshawi_by),
            usedFor = stringResource(R.string.credit_minshawi_use),
        )
        Face(
            title = stringResource(R.string.credit_husary),
            madeBy = stringResource(R.string.credit_husary_by),
            usedFor = stringResource(R.string.credit_husary_use),
        )

        Spacer(Modifier.height(Space.Section))
    }
}

/**
 * A pack's own line in the reader's language: the catalog's Bangla line when
 * the interface is Bangla and the catalog carries one, and the English line
 * otherwise, so no pack is ever credited in a language nobody wrote for it
 * (owner decision, an earlier session).
 */
@Composable
private fun packLine(english: String, bangla: String): String {
    val language = LocalConfiguration.current.locales[0].language
    return remember(english, bangla, language) {
        if (language.startsWith("bn") && bangla.isNotBlank()) bangla else english
    }
}

/**
 * What a pack is, in the reader's language: "English translation", "Bangla
 * tafsir". The language alone was the whole of the clue while every pack of
 * one language was a single kind, and with two translations and two tafsirs in
 * the catalog a title that said "· English" left the reader to guess which was
 * which (owner report, this session). The Quran text is named by its language
 * and nothing else, because there is only one of it and its name already says
 * what it is.
 */
@Composable
private fun packKind(pack: ContentPack): String {
    val language = languageName(pack.language)
    return when (pack.type) {
        PackType.Translation -> stringResource(R.string.about_pack_translation, language)
        PackType.Tafsir -> stringResource(R.string.about_pack_tafsir, language)
        PackType.Words -> stringResource(R.string.about_pack_words, language)
        else -> language
    }
}

/** A face or a voice: its name, and the one line that says whose it is. */
@Composable
private fun Face(title: String, madeBy: String, usedFor: String) {
    AboutCredit(title = title, line = madeBy + SEPARATOR + usedFor)
}

/**
 * One credit: its name in the sheet's row size, one line that says what it is,
 * and, when the thing carries terms, those terms under it.
 *
 * The name is the size a settings row's name wears, so the page and the sheet
 * behind it read as one hand; the line under it is the size every row's value
 * wears. Nothing is uppercase, nothing is a badge, and no credit is a door:
 * the page is facts and credits, and a reader reads it once.
 */
@Composable
internal fun AboutCredit(title: String, line: String?, terms: List<String> = emptyList()) {
    Column(
        modifier = Modifier.padding(
            start = Space.Gutter,
            end = Space.Gutter,
            bottom = Space.Row,
        ),
    ) {
        Text(
            text = title,
            style = screenSubtitle(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!line.isNullOrBlank()) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Tight),
            )
        }
        terms.forEachIndexed { index, term ->
            Text(
                text = term,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = if (index == 0) Space.Line else Space.Tight),
            )
        }
    }
}
