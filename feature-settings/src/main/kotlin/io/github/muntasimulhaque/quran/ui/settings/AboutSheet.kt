package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.languageName

/** How the catalog joins the licenses of one pack's datasets. */
private const val LICENSE_SEPARATOR = " · "

/**
 * About and credits in one place: what the app is, who made the text, the
 * translations, the tafsirs, the fonts, and the recitations it carries, and
 * under what terms. Every pack in the catalog appears with its own credit and
 * license, so the page cannot drift from what is installed.
 *
 * The three doors at the foot open the browser only when the reader asks for
 * one; nothing here needs a connection to read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(
    packs: List<ContentPack>,
    version: String,
    onOpenLink: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp),
        ) {
            Text(
                text = stringResource(R.string.about_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp),
            )
            Text(
                text = stringResource(R.string.about_subtitle, version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 4.dp),
            )

            Group(stringResource(R.string.about_group_content))
            packs.forEach { pack ->
                Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.about_pack_title,
                            pack.name,
                            languageName(pack.language),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = pack.credit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // One license per line: a pack can carry several, and a
                    // run of them in one paragraph is unreadable.
                    pack.license.split(LICENSE_SEPARATOR).forEach { license ->
                        if (license.isNotBlank()) {
                            Text(
                                text = license.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Group(stringResource(R.string.about_group_fonts))
            Credit(
                stringResource(R.string.credit_page_fonts),
                stringResource(R.string.credit_page_fonts_by),
                stringResource(R.string.credit_page_fonts_use),
            )
            Credit(
                stringResource(R.string.credit_amiri),
                stringResource(R.string.credit_amiri_by),
                stringResource(R.string.credit_amiri_use),
            )
            Credit(
                stringResource(R.string.credit_literata),
                stringResource(R.string.credit_literata_by),
                stringResource(R.string.credit_literata_use),
            )
            Credit(
                stringResource(R.string.credit_inter),
                stringResource(R.string.credit_inter_by),
                stringResource(R.string.credit_inter_use),
            )

            Group(stringResource(R.string.about_group_recitations))
            Credit(
                stringResource(R.string.credit_minshawi),
                stringResource(R.string.credit_minshawi_by),
                stringResource(R.string.credit_minshawi_use),
            )
            Credit(
                stringResource(R.string.credit_husary),
                stringResource(R.string.credit_husary_by),
                stringResource(R.string.credit_husary_use),
            )

            Group(stringResource(R.string.about_group_app))
            AboutRow(stringResource(R.string.settings_version), version)
            AboutRow(
                stringResource(R.string.settings_ads),
                stringResource(R.string.settings_ads_none),
            )
            AboutRow(stringResource(R.string.settings_license_label), stringResource(R.string.about_license))
            TextRow(title = stringResource(R.string.settings_privacy)) { onOpenLink(PRIVACY_URL) }
            TextRow(title = stringResource(R.string.settings_source)) { onOpenLink(SOURCE_URL) }
            TextRow(title = stringResource(R.string.settings_rights)) { onOpenLink(RIGHTS_URL) }
        }
    }
}

@Composable
private fun Credit(title: String, credit: String, use: String) {
    Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = credit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = use,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
