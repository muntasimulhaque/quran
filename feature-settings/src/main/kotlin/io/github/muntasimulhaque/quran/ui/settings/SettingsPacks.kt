package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * What a list of packs says, in one place.
 *
 * The hub names the pack a row is on in a few words, and the list pages name
 * every pack in a group by the language it speaks. Both are the same question,
 * asked of a pack catalog, and they are answered here so the hub and the
 * pages cannot drift into saying different things about the same pack.
 */

/**
 * The translations the reading draws with, in one line: the pack's own name
 * when there is one, a count when there are several, and an honest nothing
 * when the reader has added none. A pack that is chosen but not yet on the
 * device is not counted, so the row never promises a translation the reading
 * cannot draw.
 */
@Composable
internal fun translationName(settings: AppSettings, packs: List<ContentPack>): String {
    val chosen = packs.filter { it.id in settings.translationPacks && it.installed }
    return when {
        chosen.isEmpty() -> stringResource(R.string.settings_none_yet)
        chosen.size == 1 -> chosen.first().name
        else -> stringResource(R.string.settings_chosen_count, chosen.size)
    }
}

/** The tafsirs the reading draws with, in the same one line as the translations. */
@Composable
internal fun tafsirSummary(settings: AppSettings, packs: List<ContentPack>): String {
    val chosen = packs.filter { it.type == PackType.Tafsir && it.installed && it.id in settings.tafsirPacks }
    return when {
        chosen.isEmpty() -> stringResource(R.string.settings_none_yet)
        chosen.size == 1 -> chosen.first().name
        else -> stringResource(R.string.settings_chosen_count, chosen.size)
    }
}

/**
 * The word list the reading speaks: the one that matches the first chosen
 * translation, and English when that language has no list. The translation
 * counts as chosen even before it is installed, so a Bangla reader is never
 * offered the English word list because the Bangla translation is still on
 * its way. The pack is the one the switch adds when it is missing.
 */
internal fun wantedWordsPack(settings: AppSettings, packs: List<ContentPack>): ContentPack? {
    val language = packs.firstOrNull { it.id in settings.translationPacks }?.language
        ?: UiLanguage.English.tag
    val preferred = ContentDatabase.wordsPackId(language)
    return packs.firstOrNull { it.id == preferred }
        ?: packs.firstOrNull { it.id == ContentDatabase.WORDS_PACK }
}

/**
 * One pack's own line on a list page: the language it speaks and whether it
 * came with the app, so a reader never wonders what a size beside a pack
 * means.
 */
@Composable
internal fun translationSubtitle(pack: ContentPack): String {
    val detail = if (pack.shipped) {
        stringResource(R.string.pack_included_suffix)
    } else {
        formatBytes(pack.bytes)
    }
    return stringResource(R.string.pack_installed, languageName(pack.language), detail)
}

/**
 * The packs of one kind, grouped by the language they speak, in the
 * alphabetical order of those languages, and alphabetical inside each group.
 * A list of choices is read, not searched: the reader looks for a name, so
 * names are in one order everywhere in the app.
 */
@Composable
internal fun LanguageGroups(
    packs: List<ContentPack>,
    type: PackType,
    row: @Composable (ContentPack) -> Unit,
) {
    val groups = packs.filter { it.type == type }.groupBy { it.language }
    val names = HashMap<String, String>()
    for (language in groups.keys) names[language] = languageName(language)
    groups.entries
        .sortedBy { (names[it.key] ?: it.key).lowercase() }
        .forEach { (language, group) ->
            Text(
                text = names[language] ?: language,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = Space.Block, bottom = Space.Tight),
            )
            group.sortedBy { it.name.lowercase() }.forEach { pack -> row(pack) }
        }
}
