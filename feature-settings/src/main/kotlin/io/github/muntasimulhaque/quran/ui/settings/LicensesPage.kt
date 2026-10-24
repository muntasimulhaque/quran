package io.github.muntasimulhaque.quran.ui.settings

import android.util.Log
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The license the bundled fonts travel under, packaged from `licenses/`. */
private const val LICENSE_ASSET = "OFL-1.1.txt"

/**
 * The fonts' full license, readable inside the app.
 *
 * Every face the app bundles is under the SIL Open Font License 1.1, and the
 * license is honored by being readable where the fonts are, not only by
 * sitting beside the source tree. The page names each face and its copyright
 * holder first, then prints the license whole and in English, because the
 * text is the license and not a summary of it (owner decision, 11.1).
 *
 * The text is read from the packaged asset rather than copied into a string,
 * so the file a reviewer reads in `licenses/OFL-1.1.txt` and the one a reader
 * opens on the phone cannot drift. A read that fails says so in place of the
 * body; the page still opens, and the app does not crash over a license.
 */
@Composable
fun LicensesPage() {
    val context = LocalContext.current
    val license by produceState<Result<String>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(LICENSE_ASSET).bufferedReader().use { it.readText() }
            }.onFailure { error ->
                Log.e("LicensesPage", "reading $LICENSE_ASSET", error)
            }
        }
    }
    Column(Modifier.fillMaxWidth().sheetVerticalScroll(rememberScrollState())) {
        Text(
            text = stringResource(R.string.licenses_opening),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = Space.Gutter,
                end = Space.Gutter,
                top = Space.Tight,
            ),
        )

        Group(stringResource(R.string.about_group_fonts))
        // The copyright lines the repository's licenses/README.md carries, in
        // the same order, and in the reader's own language where the catalog
        // has one. The license body below stays English: a translated
        // license is not the license.
        AboutCredit(
            stringResource(R.string.credit_page_fonts),
            listOf(stringResource(R.string.credit_page_fonts_by)),
        )
        AboutCredit(
            stringResource(R.string.credit_amiri),
            listOf(stringResource(R.string.credit_amiri_by)),
        )
        AboutCredit(
            stringResource(R.string.credit_literata),
            listOf(stringResource(R.string.credit_literata_by)),
        )
        AboutCredit(
            stringResource(R.string.credit_noto_serif_bengali),
            listOf(stringResource(R.string.credit_noto_serif_bengali_by)),
        )
        AboutCredit(
            stringResource(R.string.credit_inter),
            listOf(stringResource(R.string.credit_inter_by)),
        )
        AboutCredit(
            stringResource(R.string.credit_noto_sans_bengali),
            listOf(stringResource(R.string.credit_noto_sans_bengali_by)),
        )

        when (val body = license) {
            null -> Unit
            else -> body.fold(
                onSuccess = { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            start = Space.Gutter,
                            end = Space.Gutter,
                            top = Space.Block,
                        ),
                    )
                },
                onFailure = {
                    Text(
                        text = stringResource(R.string.licenses_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = Space.Gutter,
                            end = Space.Gutter,
                            top = Space.Block,
                        ),
                    )
                },
            )
        }
        Spacer(Modifier.height(Space.Section))
    }
}
